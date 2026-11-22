package com.amwolfstein.sistrum.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import com.amwolfstein.sistrum.R
import com.amwolfstein.sistrum.ui.components.GenreContextMenu
import com.amwolfstein.sistrum.ui.components.HeroCard
import com.amwolfstein.sistrum.ui.components.MaterialSymbols
import com.amwolfstein.sistrum.ui.components.MaterialSymbol
import com.amwolfstein.sistrum.ui.components.PlaylistArtwork
import com.amwolfstein.sistrum.ui.components.TrackRow
import com.amwolfstein.sistrum.ui.components.TrackContextMenu
import com.amwolfstein.sistrum.ui.components.TrackContextMenuActions
import com.amwolfstein.sistrum.ui.components.TrackContextArtist
import com.amwolfstein.sistrum.ui.components.TrackContextBottomSheetRequest
import com.amwolfstein.sistrum.ui.components.PlaylistContextMenu
import com.amwolfstein.sistrum.ui.components.SistrumDialog
import com.amwolfstein.sistrum.ui.components.SistrumPillButtonVariant
import com.amwolfstein.sistrum.ui.components.liquidGlassBackground
import com.amwolfstein.sistrum.player.PlaybackQueueSnapshot
import com.amwolfstein.sistrum.ui.theme.LocalSistrumTokens
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import com.amwolfstein.sistrum.ui.components.MornyeDetailScaffold
import com.amwolfstein.sistrum.ui.components.MornyeActionRow
import com.amwolfstein.sistrum.ui.components.MornyeArtworkOverlay
import com.amwolfstein.sistrum.sync.isExplicit
import com.amwolfstein.sistrum.ui.components.MornyeDetailFooter

@Composable
internal fun PlaylistDetailsContent(
    uiState: PlaylistDetailsUiState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    hazeState: HazeState? = null,
    onHeroColorChanged: (Color) -> Unit = {},
    onBack: () -> Unit = {},
    onPlay: () -> Unit = {},
    onShuffle: () -> Unit = {},
    onTrackClick: (String) -> Unit = {},
    playbackQueue: PlaybackQueueSnapshot = PlaybackQueueSnapshot(),
    onTrackPlayNext: (String) -> Unit = {},
    onTrackAddToQueue: (String) -> Unit = {},
    onTrackFavoriteToggle: (String, Boolean) -> Unit = { _, _ -> },
    onTrackArtistClick: (TrackContextArtist) -> Unit = {},
    onTrackRemoveFromPlaylist: (String) -> Unit = {},
    onTrackMove: (String, String?, String?) -> Unit = { _, _, _ -> },
    onTrackContextBottomSheet: (TrackContextBottomSheetRequest) -> Unit = {},
    onPlaylistPlayNext: (List<String>) -> Unit = {},
    onPlaylistAddToQueue: (List<String>) -> Unit = {},
    onPlaylistUpdate: (String, String, android.net.Uri?, Boolean) -> Unit = { _, _, _, _ -> },
    onPlaylistDelete: (String) -> Unit = {},
    isGeneratedMix: Boolean = false,
) {
    val tokens = LocalSistrumTokens.current
    val playlist = uiState.playlist
    if (playlist == null) {
        MornyeDetailScaffold(
            title = stringResource(R.string.playlist_details_empty_title),
            onBack = onBack,
            modifier = modifier,
            bottomNavigationPadding = contentPadding.calculateBottomPadding(),
        ) {
            item("unavailable") {
                HeroCard(
                    symbol = MaterialSymbols.QueueMusic,
                    title = stringResource(R.string.playlist_details_empty_title),
                    description = stringResource(R.string.playlist_details_empty_description),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        return
    }
    var contextTrack by remember(playlist.id) { mutableStateOf<String?>(null) }
    var playlistMenuExpanded by remember(playlist.id) { mutableStateOf(false) }
    var showPlaylistEditor by remember(playlist.id) { mutableStateOf(false) }
    var showDeleteConfirmation by remember(playlist.id) { mutableStateOf(false) }
    var isReordering by remember(playlist.id) { mutableStateOf(false) }
    var orderedTrackIds by remember(playlist.id) { mutableStateOf(uiState.tracks.map { it.id }) }
    var draggedTrackId by remember { mutableStateOf<String?>(null) }
    var draggedTrackInitialIndex by remember { mutableStateOf(-1) }
    val latestOrderedTrackIds = rememberUpdatedState(orderedTrackIds)
    val latestOnTrackMove = rememberUpdatedState(onTrackMove)
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        if (from.index != to.index) {
            orderedTrackIds = movePlaylistTrack(orderedTrackIds, playlistTrackIndex(from.index), playlistTrackIndex(to.index))
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        }
    }
    val count = pluralStringResource(R.plurals.mornye_songs, uiState.tracks.size, uiState.tracks.size)
    val totalDurationSeconds = remember(uiState.tracks) { playlistTotalDurationSeconds(uiState.tracks) }
    val coverPaths = remember(uiState.customArtworkPath, uiState.artworkPaths) {
        playlistDetailCoverPaths(uiState.customArtworkPath, uiState.artworkPaths)
    }
    val name = if (playlist.id == FavoritesPlaylistId) stringResource(R.string.library_favorites) else playlist.name
    val trackIds = remember(uiState.tracks) { uiState.tracks.map { it.id } }
    val tracksById = remember(uiState.tracks) { uiState.tracks.associateBy { it.id } }
    androidx.compose.runtime.LaunchedEffect(uiState.tracks, isReordering) {
        if (!isReordering) orderedTrackIds = uiState.tracks.map { it.id }
    }
    MornyeDetailScaffold(
        title = name,
        titleStyle = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
        titleMaxLines = Int.MAX_VALUE,
        titleBottomSpacing = 5.dp,
        onBack = onBack,
        artworkPath = coverPaths.firstOrNull(),
        onHeroColorChanged = onHeroColorChanged,
        listState = listState,
        modifier = modifier,
        bottomNavigationPadding = contentPadding.calculateBottomPadding(),
        artworkOverlay = MornyeArtworkOverlay.Title,
        artworkContent = if (uiState.customArtworkPath == null) {
            {
                PlaylistArtwork(
                    playlist.id, coverPaths, modifier = Modifier.fillMaxSize(), isHeader = true,
                    preserveAspectRatio = isGeneratedMix,
                )
            }
        } else null,
        header = {
            Text(count, Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
            MornyeActionRow(
                topPadding = 16.dp,
                onPlay = onPlay,
                onShuffle = onShuffle,
                moreLabel = stringResource(if (isReordering) R.string.playlist_reorder_done else R.string.album_row_more_options),
                onMore = { if (isReordering) isReordering = false else playlistMenuExpanded = true },
                menu = { action ->
                    if (isReordering) action()
                    else if (isGeneratedMix) {
                        GenreContextMenu(
                            trackIds = trackIds,
                            expanded = playlistMenuExpanded,
                            onDismiss = { playlistMenuExpanded = false },
                            hazeState = hazeState,
                            playbackQueue = playbackQueue,
                            onPlayNext = onPlaylistPlayNext,
                            onAddToQueue = onPlaylistAddToQueue,
                            onBottomSheetRequested = onTrackContextBottomSheet,
                            anchor = action,
                        )
                    } else PlaylistContextMenu(
                        playlistId = playlist.id,
                        trackIds = trackIds,
                        expanded = playlistMenuExpanded,
                        onDismiss = { playlistMenuExpanded = false },
                        hazeState = hazeState,
                        onPlayNext = onPlaylistPlayNext,
                        onAddToQueue = onPlaylistAddToQueue,
                        onReorder = { isReordering = true },
                        onEdit = { showPlaylistEditor = true },
                        onDelete = { showDeleteConfirmation = true },
                        anchor = action,
                    )
                },
            )
        },
    ) {
        itemsIndexed(orderedTrackIds, key = { _, trackId -> trackId }) { index, trackId ->
            val track = tracksById[trackId] ?: return@itemsIndexed
            val explicit = track.isExplicit()
            val secondaryTextColor = Color.White.copy(alpha = 0.72f)
            if (isReordering) {
                ReorderableItem(reorderableState, key = trackId) { isDragging ->
                    TrackRow(
                        title = track.title,
                        artist = track.artists,
                        trackNumber = index + 1,
                        explicit = explicit,
                        secondaryTextColor = secondaryTextColor,
                        modifier = Modifier.fillMaxWidth().then(
                            if (isDragging) Modifier.clip(tokens.shapes.medium).liquidGlassBackground(
                                hazeState, glassTint = LocalSistrumTokens.current.colors.glassTint, borderShape = tokens.shapes.medium,
                            ) else Modifier,
                        ),
                        trailingContent = {
                            PlaylistDragHandle(
                                isDragged = isDragging,
                                modifier = playlistDragHandleModifier(
                                    onStart = {
                                        draggedTrackId = trackId
                                        draggedTrackInitialIndex = orderedTrackIds.indexOf(trackId)
                                    },
                                    onStop = {
                                        draggedTrackId?.let { movedTrackId ->
                                            if (latestOrderedTrackIds.value.indexOf(movedTrackId) != draggedTrackInitialIndex) {
                                                val (previous, next) = playlistMoveAnchors(latestOrderedTrackIds.value, movedTrackId)
                                                latestOnTrackMove.value(movedTrackId, previous, next)
                                            }
                                        }
                                        draggedTrackId = null
                                        draggedTrackInitialIndex = -1
                                    },
                                ),
                            )
                        },
                    )
                }
            } else {
                TrackContextMenu(
                    track = track,
                    expanded = contextTrack == track.id,
                    onDismiss = { if (contextTrack == track.id) contextTrack = null },
                    actions = TrackContextMenuActions(removeFromPlaylist = playlist.id != FavoritesPlaylistId && !isGeneratedMix),
                    hazeState = hazeState,
                    playbackQueue = playbackQueue,
                    onPlayNext = { onTrackPlayNext(it.id) },
                    onAddToQueue = { onTrackAddToQueue(it.id) },
                    onFavoriteChange = { item, favorite -> onTrackFavoriteToggle(item.id, favorite) },
                    onGoToArtist = onTrackArtistClick,
                    onRemoveFromPlaylist = { onTrackRemoveFromPlaylist(it.id) },
                    onBottomSheetRequested = onTrackContextBottomSheet,
                ) {
                    TrackRow(
                        title = track.title,
                        artist = track.artists,
                        trackNumber = index + 1,
                        explicit = explicit,
                        secondaryTextColor = secondaryTextColor,
                        onClick = { onTrackClick(track.id) },
                        onMoreClick = { contextTrack = track.id },
                        onLongClick = { contextTrack = track.id },
                    )
                }
            }
            if (index < orderedTrackIds.lastIndex) {
                HorizontalDivider(
                    Modifier.padding(horizontal = 24.dp),
                    color = Color.White.copy(alpha = 0.14f),
                )
            }
        }
        item("footer") {
            MornyeDetailFooter(uiState.tracks.size, (totalDurationSeconds / 60).toInt())
        }
    }
    if (showPlaylistEditor) {
        EditPlaylistBottomSheet(
            initialName = playlist.name,
            artworkPath = uiState.customArtworkPath,
            showNameInput = playlist.id != FavoritesPlaylistId,
            onDismiss = { showPlaylistEditor = false },
            onSave = { name, artwork, clearArtwork -> onPlaylistUpdate(playlist.id, name, artwork, clearArtwork); showPlaylistEditor = false },
        )
    }
    if (showDeleteConfirmation) {
        SistrumDialog(
            title = stringResource(R.string.playlist_delete_confirm_title),
            description = stringResource(R.string.playlist_delete_confirm_description),
            dismissLabel = stringResource(R.string.cancel),
            onDismiss = { showDeleteConfirmation = false },
            confirmLabel = stringResource(R.string.playlist_delete),
            onConfirm = { onPlaylistDelete(playlist.id); showDeleteConfirmation = false },
            confirmVariant = SistrumPillButtonVariant.Destructive,
        )
    }
}

@Composable
private fun PlaylistDragHandle(isDragged: Boolean, modifier: Modifier) {
    val colors = LocalSistrumTokens.current.colors
    val label = stringResource(R.string.playlist_reorder_drag_handle)
    Box(
        Modifier.size(48.dp).semantics(mergeDescendants = true) { contentDescription = label }.then(modifier),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        MaterialSymbol(MaterialSymbols.Menu, null, size = 24.dp, tint = if (isDragged) colors.text else colors.mutedText)
    }
}

private fun ReorderableCollectionItemScope.playlistDragHandleModifier(onStart: () -> Unit, onStop: () -> Unit) =
    Modifier.longPressDraggableHandle(onDragStarted = { onStart() }, onDragStopped = onStop)

internal fun movePlaylistTrack(trackIds: List<String>, fromIndex: Int, toIndex: Int): List<String> =
    trackIds.toMutableList().apply { if (fromIndex in indices && toIndex in indices && fromIndex != toIndex) add(toIndex, removeAt(fromIndex)) }

// The playlist hero occupies LazyColumn index 0; track rows begin immediately after it.
internal fun playlistTrackIndex(lazyListIndex: Int): Int = lazyListIndex - 1

/** Custom artwork wins; repeat up to four distinct song covers to fill the 2×2 header. */
internal fun playlistDetailCoverPaths(customArtworkPath: String?, artworkPaths: List<String>): List<String> =
    if (customArtworkPath != null) listOf(customArtworkPath)
    else if (artworkPaths.isEmpty()) emptyList()
    else List(4) { artworkPaths[it % artworkPaths.size] }
