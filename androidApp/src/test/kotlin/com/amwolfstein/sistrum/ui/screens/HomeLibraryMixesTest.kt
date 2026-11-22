package com.amwolfstein.sistrum.ui.screens

import com.amwolfstein.sistrum.sync.LibraryTrack
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLibraryMixesTest {
    private val day = LocalDate.of(2026, 11, 21)

    @Test
    fun generatesStableMixesFromPlayableTracksAndGenreTags() {
        val tracks = List(96) { index ->
            val genre = if (index < 48) "Egyptian Arabic Pop" else "Jazz"
            LibraryTrack(
                id = "track-$index",
                title = "Song $index",
                artists = "Artist ${index % 8}",
                metadataJson = """{"genres":[{"name":"$genre"}]}""",
                audioPath = "/music/$index.flac",
            )
        }
        val unavailable = tracks.first().copy(id = "unavailable", audioPath = null)
        val mixes = buildHomeLibraryMixes(tracks + unavailable, emptyList(), day)
        val arabic = mixes.single { it.id == "genre:Arabic" }
        assertEquals(40, arabic.tracks.size)
        assertTrue(arabic.tracks.all { it in tracks.take(48) })
        assertEquals(mixes, buildHomeLibraryMixes(tracks.reversed(), emptyList(), day))
        assertTrue(arabic.tracks.zipWithNext().none { (first, second) -> first.artists == second.artists })
    }

    @Test
    fun favoritesUseListeningAndLovedTracksWhenHistoryIsSparse() {
        val tracks = List(40) { index ->
            LibraryTrack(
                id = "$index",
                title = "Song $index",
                artists = "Artist $index",
                playCount = if (index == 0) 14 else 0,
                metadataJson = """{"is_favorite":${index in 1..7}}""",
                audioPath = "/music/$index.flac",
            )
        }
        val favorites = buildHomeLibraryMixes(tracks, emptyList(), day).first()
        assertEquals("favorites", favorites.id)
        assertEquals(tracks.first(), favorites.tracks.first())
        assertTrue(favorites.tracks.take(8).containsAll(tracks.take(8)))
    }
}
