package com.amwolfstein.sistrum.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.chrisbanes.haze.HazeState
import com.amwolfstein.sistrum.R
import com.amwolfstein.sistrum.player.PlaybackQueueSnapshot
import com.amwolfstein.sistrum.sync.LibraryAlbum
import com.amwolfstein.sistrum.ui.components.AlbumRow
import com.amwolfstein.sistrum.ui.components.HeroCard
import com.amwolfstein.sistrum.ui.components.MornyeActionRow
import com.amwolfstein.sistrum.ui.components.MornyeArtworkOverlay
import com.amwolfstein.sistrum.ui.components.MornyeDetailScaffold
import com.amwolfstein.sistrum.ui.components.MornyePlayShape
import com.amwolfstein.sistrum.ui.components.MaterialSymbols
import androidx.compose.ui.platform.testTag
import com.amwolfstein.sistrum.ui.components.InsetListDivider
import com.amwolfstein.sistrum.ui.components.GenreContextMenu
import com.amwolfstein.sistrum.ui.components.TrackContextBottomSheetRequest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private const val GenreAlbumDividerTag = "genre-detail-album-divider"

@Composable
internal fun GenreDetailsContent(
    uiState: GenreDetailsUiState,
    modifier: Modifier = Modifier,
    listState: LazyListState,
    contentPadding: PaddingValues = PaddingValues(),
    hazeState: HazeState? = null,
    onHeroColorChanged: (Color) -> Unit = {},
    onBack: () -> Unit = {},
    onPlay: () -> Unit = {},
    onShuffle: () -> Unit = {},
    onPlayNext: (List<String>) -> Unit = {},
    onAddToQueue: (List<String>) -> Unit = {},
    onTrackContextBottomSheet: (TrackContextBottomSheetRequest) -> Unit = {},
    onAlbumClick: (LibraryAlbum) -> Unit = {},
    playbackQueue: PlaybackQueueSnapshot = PlaybackQueueSnapshot(),
) {
    val genre = uiState.genre
    if (genre == null) {
        MornyeDetailScaffold(
            title = stringResource(R.string.genre_details_empty_title),
            onBack = onBack,
            modifier = modifier,
            listState = listState,
            bottomNavigationPadding = contentPadding.calculateBottomPadding(),
            artworkOverlay = MornyeArtworkOverlay.Header,
        ) {
            item("unavailable") {
                HeroCard(
                    symbol = MaterialSymbols.Label,
                    title = stringResource(R.string.genre_details_empty_title),
                    description = stringResource(R.string.genre_details_empty_description),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        return
    }
    var menuExpanded by remember(genre.id) { mutableStateOf(false) }
    val artworkAlbum = remember(uiState.albums) {
        uiState.albums.filter { !it.artworkPath.isNullOrBlank() || !it.audioPath.isNullOrBlank() }
            .maxByOrNull { it.year }
    }
    MornyeDetailScaffold(
        title = genre.name,
        titleStyle = MaterialTheme.typography.displayLarge,
        onBack = onBack,
        modifier = modifier,
        listState = listState,
        artworkPath = artworkAlbum?.artworkPath,
        audioPath = artworkAlbum?.audioPath,
        artworkOverlay = MornyeArtworkOverlay.Header,
        bottomNavigationPadding = contentPadding.calculateBottomPadding(),
        onHeroColorChanged = onHeroColorChanged,
        header = {
            Text(
                stringResource(
                    R.string.genre_details_summary,
                    pluralStringResource(R.plurals.genre_details_album_count, uiState.albums.size, uiState.albums.size),
                    pluralStringResource(R.plurals.genre_details_track_count, uiState.tracks.size, uiState.tracks.size),
                ),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            MornyeActionRow(
                onShuffle = onShuffle,
                onPlay = onPlay,
                playShape = MornyePlayShape.Circle,
                onMore = { menuExpanded = true },
                moreLabel = stringResource(R.string.genre_row_more_options),
                menu = { action ->
                    GenreContextMenu(
                        trackIds = uiState.tracks.map { it.id },
                        expanded = menuExpanded,
                        onDismiss = { menuExpanded = false },
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onBottomSheetRequested = onTrackContextBottomSheet,
                        hazeState = hazeState,
                        playbackQueue = playbackQueue,
                        anchor = action,
                    )
                },
            )
        },
    ) {
        itemsIndexed(uiState.albums, key = { _, album -> album.id }, contentType = { _, _ -> "genre_album_row" }) { index, album ->
            if (index == 0) InsetListDivider(Modifier.testTag(GenreAlbumDividerTag))
            AlbumRow(
                title = album.title,
                artist = album.artist.ifBlank { stringResource(R.string.album_unknown_artist) },
                artworkPath = album.artworkPath,
                audioPath = album.audioPath,
                onClick = { onAlbumClick(album) },
            )
            InsetListDivider(Modifier.testTag(GenreAlbumDividerTag))
        }
    }
}
