package com.amwolfstein.sistrum.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.amwolfstein.sistrum.R
import com.amwolfstein.sistrum.player.DailyTrackListeningStat
import com.amwolfstein.sistrum.sync.LibraryTrack
import com.amwolfstein.sistrum.sync.metadataObject
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlin.random.Random

data class HomeLibraryMix(
    val id: String,
    val titleRes: Int,
    val tracks: List<LibraryTrack>,
    val genre: String? = null,
) {
    val artworkPaths: List<String> = tracks.mapNotNull { it.artworkPath }.distinct().take(4)
}

@Composable
internal fun HomeLibraryMix.localizedTitle(): String =
    if (titleRes == R.string.home_genre_mix) stringResource(titleRes, genre.orEmpty()) else stringResource(titleRes)

internal fun homeMixDays() = flow {
    while (true) {
        val now = ZonedDateTime.now()
        emit(now.toLocalDate())
        val nextDay = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        delay(Duration.between(now, nextDay).toMillis().coerceAtLeast(1))
    }
}

internal fun buildHomeLibraryMixes(
    library: List<LibraryTrack>,
    stats: List<DailyTrackListeningStat>,
    day: LocalDate,
): List<HomeLibraryMix> {
    val tracks = library.filter { !it.audioPath.isNullOrBlank() }.distinctBy { it.id }
    if (tracks.size < 30) return emptyList()

    val statsByTrack = stats.groupBy { it.trackId }
    val metadata = tracks.associate { it.id to it.metadataObject() }
    val genresByTrack = tracks.associate { track ->
        track.id to (metadata[track.id]?.get("genres") as? JsonArray).orEmpty().mapNotNull { value ->
            ((value as? JsonObject)?.get("name") as? JsonPrimitive)?.contentOrNull?.trim()
                ?.takeIf(String::isNotEmpty)?.let { name ->
                    if (name.contains("arab", ignoreCase = true) || name.contains("عرب")) "Arabic" else name
                }
        }.distinct()
    }
    val playCounts = tracks.associate { track ->
        track.id to maxOf(track.playCount, statsByTrack[track.id].orEmpty().sumOf { it.playCount })
    }
    val fewPlays = playCounts.values.count { it > 0 } < 8
    val taste = tracks.associate { track ->
        val loved = (metadata[track.id]?.get("is_favorite") as? JsonPrimitive)?.booleanOrNull == true
        track.id to (playCounts.getValue(track.id) + if (fewPlays && loved) 3 else 0)
    }
    val byTaste = tracks.sortedWith(compareByDescending<LibraryTrack> { taste.getValue(it.id) }.thenBy { it.id })
    val groups = mutableMapOf<String, MutableList<LibraryTrack>>()
    tracks.forEach { track -> genresByTrack.getValue(track.id).forEach { groups.getOrPut(it) { mutableListOf() }.add(track) } }
    val genres = groups.entries.sortedWith(
        compareByDescending<Map.Entry<String, List<LibraryTrack>>> { entry ->
            entry.value.sumOf { taste.getValue(it.id) }
        }.thenByDescending { it.value.size }.thenBy { it.key },
    )

    val mixes = mutableListOf<HomeLibraryMix>()
    fun add(id: String, titleRes: Int, candidates: List<LibraryTrack>, seeds: List<LibraryTrack> = emptyList(), genre: String? = null) {
        val pool = candidates.distinctBy { it.id }.sortedBy { it.id }
        if (pool.size < 20) return
        val poolIds = pool.mapTo(hashSetOf()) { it.id }
        val order = (seeds.filter { it.id in poolIds } + pool.shuffled(Random("$day/$id".hashCode()))).distinctBy { it.id }.toMutableList()
        val picked = buildList<LibraryTrack> {
            while (size < 40 && order.isNotEmpty()) {
                val previousArtist = lastOrNull()?.artists?.substringBefore(',')?.trim()
                val next = order.indexOfFirst { !it.artists.substringBefore(',').trim().equals(previousArtist, ignoreCase = true) }
                add(order.removeAt(next.coerceAtLeast(0)))
            }
        }
        val ids = picked.mapTo(hashSetOf()) { it.id }
        if (mixes.any { mix -> mix.tracks.count { it.id in ids } * 5 > minOf(mix.tracks.size, ids.size) * 3 }) return
        mixes += HomeLibraryMix(id, titleRes, picked, genre)
    }

    val favorites = byTaste.filter { taste.getValue(it.id) > 0 }
    if (favorites.size >= 8) {
        val genresOfFavorites = favorites.take(50).flatMap { genresByTrack.getValue(it.id) }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(3)
        val related = genresOfFavorites.flatMap { groups[it.key].orEmpty() }
        add("favorites", R.string.home_favorites_mix, favorites.take(100) + related.ifEmpty { tracks }, favorites.take(12))
    }
    for ((genre, core) in genres) {
        if (mixes.count { it.genre != null } == 5) break
        if (core.size < 12) continue
        val artists = core.mapTo(hashSetOf()) { it.artists }
        val candidates = if (core.size >= 20) core else tracks.filter { it in core || it.artists in artists }
        add("genre:$genre", if (genre == "Arabic") R.string.home_arabic_mix else R.string.home_genre_mix,
            candidates, genre = genre)
    }
    val recentCutoff = day.minusDays(30).toString()
    val fresh = tracks.filter { it.createdAt.take(10) in recentCutoff..day.toString() }
    if (fresh.size * 2 <= tracks.size) add("fresh", R.string.home_fresh_mix, fresh)
    val rediscoverCutoff = day.minusDays(90).toString()
    val forgotten = byTaste.filter { track ->
        val history = statsByTrack[track.id].orEmpty().filter { it.playCount > 0 || it.listenedSeconds > 0 }
        history.isNotEmpty() && history.none { it.localDate >= rediscoverCutoff }
    }
    add("rediscover", R.string.home_rediscover_mix, forgotten, forgotten.take(12))
    val discovery = genres.take(3).flatMap { it.value }.filter { taste.getValue(it.id) == 0 }
    add("discover", R.string.home_discover_mix, discovery)
    return mixes
}
