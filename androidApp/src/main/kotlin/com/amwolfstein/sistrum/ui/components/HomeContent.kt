package com.amwolfstein.sistrum.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.amwolfstein.sistrum.R
import com.amwolfstein.sistrum.player.PlaybackQueueSnapshot
import com.amwolfstein.sistrum.sync.LibraryTrack
import com.amwolfstein.sistrum.ui.theme.LocalSistrumTokens
import com.amwolfstein.sistrum.ui.screens.HomeLibraryMix
import com.amwolfstein.sistrum.ui.screens.localizedTitle
import com.amwolfstein.sistrum.ui.screens.playlistDetailCoverPaths

@Composable
internal fun HomeContent(
    isLoaded: Boolean,
    recentTracks: List<LibraryTrack>,
    keepListeningTracks: List<LibraryTrack>,
    mostPlayedTracks: List<LibraryTrack>,
    forgottenTracks: List<LibraryTrack>,
    onTrackClick: (List<LibraryTrack>, String) -> Unit,
    playbackQueue: PlaybackQueueSnapshot,
    onTrackPlayNext: (LibraryTrack) -> Unit,
    onTrackAddToQueue: (LibraryTrack) -> Unit,
    onTrackFavoriteToggle: (LibraryTrack, Boolean) -> Unit,
    onTrackAlbumClick: (LibraryTrack) -> Unit,
    onTrackArtistClick: (TrackContextArtist) -> Unit,
    onTrackContextBottomSheet: (TrackContextBottomSheetRequest) -> Unit,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    recentlyPlayedTracks: List<LibraryTrack> = emptyList(),
    mixes: List<HomeLibraryMix> = emptyList(),
    onOpenMix: (String) -> Unit = {},
) {
    val tokens = LocalSistrumTokens.current
    val albums = remember(recentTracks) { recentHomeAlbums(recentTracks) }
    val hasHomeContent = recentTracks.isNotEmpty() || recentlyPlayedTracks.isNotEmpty() ||
        mixes.isNotEmpty() || keepListeningTracks.isNotEmpty() ||
        mostPlayedTracks.isNotEmpty() || forgottenTracks.isNotEmpty()
    val recentRowState = rememberLazyListState()
    Box(modifier) {
        val layoutDirection = LocalLayoutDirection.current
        val titleHorizontalPadding = contentPadding.calculateStartPadding(layoutDirection)
        LazyColumn(
            modifier = Modifier.testTag("home-list"),
            state = listState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.extraExtraLarge),
        ) {
            if (!isLoaded && !hasHomeContent) return@LazyColumn
            if (!hasHomeContent) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        HeroCard(
                            symbol = MaterialSymbols.MusicNote,
                            title = stringResource(R.string.library_empty_title),
                            description = stringResource(R.string.library_empty_description),
                        )
                    }
                }
            } else {
                if (mixes.isNotEmpty()) item(key = "mixes") {
                    HomeMixes(mixes, titleHorizontalPadding, onOpenMix)
                }
                if (recentlyPlayedTracks.isNotEmpty()) item(key = "recently-played") {
                    HomeTrackSection(
                        titleRes = R.string.home_recently_played,
                        tracks = recentlyPlayedTracks,
                        onTrackClick = onTrackClick,
                        playbackQueue = playbackQueue,
                        onTrackPlayNext = onTrackPlayNext,
                        onTrackAddToQueue = onTrackAddToQueue,
                        onTrackFavoriteToggle = onTrackFavoriteToggle,
                        onTrackAlbumClick = onTrackAlbumClick,
                        onTrackArtistClick = onTrackArtistClick,
                        onTrackContextBottomSheet = onTrackContextBottomSheet,
                        titleHorizontalPadding = titleHorizontalPadding,
                    )
                }
                if (albums.isNotEmpty()) item(key = "recent") {
                    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.large)) {
                        Text(
                            stringResource(R.string.library_recently_added),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = tokens.colors.text,
                            modifier = Modifier.padding(horizontal = titleHorizontalPadding),
                        )
                        LazyRow(state = recentRowState,
                            flingBehavior = rememberSnapFlingBehavior(recentRowState, snapPosition = SnapPosition.Start),
                            contentPadding = PaddingValues(horizontal = titleHorizontalPadding),
                            horizontalArrangement = Arrangement.spacedBy(HomeShelfGap),
                            modifier = Modifier.fillMaxWidth().testTag("home-recent-carousel")) {
                            items(albums, key = { it.first().albumId.ifBlank { it.first().id } }) { album ->
                                val track = album.first()
                                var expanded by remember { mutableStateOf(false) }
                                TrackContextMenu(track = track, expanded = expanded, onDismiss = { expanded = false },
                                    modifier = Modifier.width(HomeShelfCoverSize), playbackQueue = playbackQueue,
                                    onPlayNext = onTrackPlayNext, onAddToQueue = onTrackAddToQueue,
                                    onFavoriteChange = onTrackFavoriteToggle, onGoToAlbum = onTrackAlbumClick,
                                    onGoToArtist = onTrackArtistClick, onBottomSheetRequested = onTrackContextBottomSheet) {
                                    DiscCard(
                                        title = track.album.ifBlank { track.title },
                                        subtitle = track.artists,
                                        artworkPath = track.artworkPath,
                                        audioPath = track.audioPath,
                                        artworkShape = HomeShelfArtworkShape,
                                        titleStyle = MaterialTheme.typography.labelLarge,
                                        subtitleStyle = MaterialTheme.typography.bodySmall,
                                        subtitleGap = 0.dp,
                                        modifier = Modifier.fillMaxWidth().testTag("home-hero-${track.id}"),
                                        titleModifier = Modifier.testTag("home-recent-title-${track.id}"),
                                        onClick = { onTrackClick(recentTracks, track.id) },
                                        onLongClick = { expanded = true },
                                    )
                                }
                            }
                        }
                    }
                }
                if (keepListeningTracks.isNotEmpty()) item {
                    HomeTrackSection(
                        titleRes = R.string.home_keep_listening,
                        tracks = keepListeningTracks,
                        onTrackClick = onTrackClick,
                        playbackQueue = playbackQueue,
                        onTrackPlayNext = onTrackPlayNext,
                        onTrackAddToQueue = onTrackAddToQueue,
                        onTrackFavoriteToggle = onTrackFavoriteToggle,
                        onTrackAlbumClick = onTrackAlbumClick,
                        onTrackArtistClick = onTrackArtistClick,
                        onTrackContextBottomSheet = onTrackContextBottomSheet,
                        titleHorizontalPadding = titleHorizontalPadding,
                    )
                }
                if (mostPlayedTracks.isNotEmpty()) item {
                    HomeTrackSection(
                        titleRes = R.string.home_most_played,
                        tracks = mostPlayedTracks,
                        onTrackClick = onTrackClick,
                        playbackQueue = playbackQueue,
                        onTrackPlayNext = onTrackPlayNext,
                        onTrackAddToQueue = onTrackAddToQueue,
                        onTrackFavoriteToggle = onTrackFavoriteToggle,
                        onTrackAlbumClick = onTrackAlbumClick,
                        onTrackArtistClick = onTrackArtistClick,
                        onTrackContextBottomSheet = onTrackContextBottomSheet,
                        titleHorizontalPadding = titleHorizontalPadding,
                    )
                }
                if (forgottenTracks.isNotEmpty()) item {
                    HomeTrackSection(
                        titleRes = R.string.home_forgotten,
                        tracks = forgottenTracks,
                        onTrackClick = onTrackClick,
                        playbackQueue = playbackQueue,
                        onTrackPlayNext = onTrackPlayNext,
                        onTrackAddToQueue = onTrackAddToQueue,
                        onTrackFavoriteToggle = onTrackFavoriteToggle,
                        onTrackAlbumClick = onTrackAlbumClick,
                        onTrackArtistClick = onTrackArtistClick,
                        onTrackContextBottomSheet = onTrackContextBottomSheet,
                        titleHorizontalPadding = titleHorizontalPadding,
                    )
                }
            }
        }
    }
}

/** Input is the ViewModel's newest-first recent queue; group without changing its order. */
internal fun recentHomeAlbums(tracks: List<LibraryTrack>): List<List<LibraryTrack>> =
    tracks.groupBy { it.albumId.ifBlank { it.id } }.values.toList()

private val HomeShelfCoverSize = 160.dp
private val HomeShelfGap = 16.dp
private val HomeMixWidth = 228.dp
private val HomeShelfArtworkShape = RoundedCornerShape(10.dp)

@Composable
private fun HomeMixes(
    mixes: List<HomeLibraryMix>,
    horizontalPadding: Dp,
    onOpenMix: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(HomeShelfGap),
        modifier = Modifier.testTag("home-mixes"),
    ) {
        items(mixes, key = { it.id }) { mix ->
            val title = mix.localizedTitle()
            val coverPaths = remember(mix.artworkPaths) { playlistDetailCoverPaths(null, mix.artworkPaths) }
            val longestWord = title.split(Regex("\\s+")).maxOf { it.length }.coerceAtLeast(1)
            val titleWidth = (HomeMixWidth.value - 40f) / 0.58f
            val titleSize = minOf(HomeMixWidth.value * 0.17f, titleWidth / longestWord,
                titleWidth * 2.4f / title.length.coerceAtLeast(1))
            Box(
                modifier = Modifier.width(HomeMixWidth).height(286.dp).clip(HomeShelfArtworkShape)
                    .background(Color(0xFF183A6C))
                    .clickable(role = Role.Button) { onOpenMix(mix.id) },
            ) {
                if (coverPaths.isNotEmpty()) {
                    PlaylistArtwork(
                        playlistId = "mix:${mix.id}",
                        artworkPaths = coverPaths,
                        modifier = Modifier.matchParentSize(),
                        isHeader = true,
                    )
                }
                Box(
                    modifier = Modifier.matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x66257BA9), Color(0xA8144F84), Color(0xDE102E56)),
                            ),
                        ),
                ) {
                    Box(Modifier.align(Alignment.TopStart).padding(16.dp)) {
                        MaterialSymbol(MaterialSymbols.QueueMusic, null, tint = Color.White)
                    }
                    val density = LocalDensity.current
                    CompositionLocalProvider(
                        LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(1.2f)),
                    ) {
                        Text(
                            title,
                            modifier = Modifier.align(Alignment.Center).padding(horizontal = 20.dp),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontSize = titleSize.sp,
                                lineHeight = 1.04.em,
                                letterSpacing = (-0.6).sp,
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 3, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            mix.tracks.asSequence().map { it.artists }.filter { it.isNotBlank() }.distinct().take(3).joinToString(" · "),
                            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTrackSection(
    titleRes: Int,
    tracks: List<LibraryTrack>,
    onTrackClick: (List<LibraryTrack>, String) -> Unit,
    playbackQueue: PlaybackQueueSnapshot,
    onTrackPlayNext: (LibraryTrack) -> Unit,
    onTrackAddToQueue: (LibraryTrack) -> Unit,
    onTrackFavoriteToggle: (LibraryTrack, Boolean) -> Unit,
    onTrackAlbumClick: (LibraryTrack) -> Unit,
    onTrackArtistClick: (TrackContextArtist) -> Unit,
    onTrackContextBottomSheet: (TrackContextBottomSheetRequest) -> Unit,
    titleHorizontalPadding: Dp,
) {
    if (tracks.isEmpty()) return

    val tokens = LocalSistrumTokens.current
    var contextTrack by remember { mutableStateOf<LibraryTrack?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.large)) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = tokens.colors.text,
            modifier = Modifier.padding(horizontal = titleHorizontalPadding, vertical = 4.dp),
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth().testTag("home-shelf-$titleRes"),
            contentPadding = PaddingValues(horizontal = titleHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(HomeShelfGap),
        ) {
            items(tracks, key = { it.id }) { track ->
                TrackContextMenu(
                    track = track,
                    expanded = contextTrack?.id == track.id,
                    onDismiss = { if (contextTrack?.id == track.id) contextTrack = null },
                    modifier = Modifier.width(HomeShelfCoverSize),
                    playbackQueue = playbackQueue,
                    onPlayNext = onTrackPlayNext,
                    onAddToQueue = onTrackAddToQueue,
                    onFavoriteChange = onTrackFavoriteToggle,
                    onGoToAlbum = onTrackAlbumClick,
                    onGoToArtist = onTrackArtistClick,
                    onBottomSheetRequested = onTrackContextBottomSheet,
                ) {
                    DiscCard(
                        title = track.title,
                        subtitle = track.artists,
                        artworkPath = track.artworkPath,
                        audioPath = track.audioPath,
                        artworkShape = HomeShelfArtworkShape,
                        titleStyle = MaterialTheme.typography.labelLarge,
                        subtitleStyle = MaterialTheme.typography.bodySmall,
                        subtitleGap = 0.dp,
                        modifier = Modifier.fillMaxWidth().testTag("home-track-${track.id}"),
                        onClick = { onTrackClick(tracks, track.id) },
                        onLongClick = { contextTrack = track },
                    )
                }
            }
        }
    }
}
private val HomeCardSpacing = 20.dp

internal fun homeTrackSectionRows(trackCount: Int) = minOf(trackCount, 2)

/**
 * The grid is as tall as its rows of cards. [cardHeight] comes from the card's text metrics at
 * the current font scale; a fixed height clipped the artist line of larger or Arabic text.
 */
internal fun homeTrackSectionHeight(trackCount: Int, cardHeight: Dp): Dp {
    val rows = homeTrackSectionRows(trackCount)
    return cardHeight * rows + HomeCardSpacing * (rows - 1).coerceAtLeast(0)
}
