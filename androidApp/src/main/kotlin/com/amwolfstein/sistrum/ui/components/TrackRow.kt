package com.amwolfstein.sistrum.ui.components

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.amwolfstein.sistrum.R
import com.amwolfstein.sistrum.sync.decodeArtworkBitmaps
import com.amwolfstein.sistrum.ui.theme.LocalSistrumTokens

private val artworkCache = LruCache<String, ImageBitmap>(250)

/**
 * Resolves artwork for display. The persisted file (written only during a Library
 * scan) is the fast path; when it is missing the embedded picture is decoded
 * directly from the audio file so artwork renders immediately after an update
 * even before the user re-scans, matching how embedded lyrics are read lazily.
 */
@Composable
internal fun rememberArtworkThumbnail(
    artworkPath: String?,
    audioPath: String? = null,
    targetPx: Int = 120,
): ImageBitmap? {
    val context = LocalContext.current
    val absolutePath = remember(artworkPath, context) {
        val path = artworkPath
        if (path.isNullOrBlank()) {
            null
        } else {
            val file = File(path)
            if (file.isAbsolute) file.absolutePath else File(context.filesDir, path).absolutePath
        }
    }
    val audioSource = audioPath?.takeIf(String::isNotBlank)
    val cacheKey = if (absolutePath != null) "file:$absolutePath:$targetPx" else "embed:${audioSource.orEmpty()}:$targetPx"

    var bitmap by remember(cacheKey) { mutableStateOf(artworkCache.get(cacheKey)) }

    LaunchedEffect(cacheKey) {
        if (bitmap == null) {
            val loaded = withContext(Dispatchers.IO) {
                decodeArtworkBitmaps(absolutePath, audioSource, targetPx, Bitmap.Config.RGB_565)?.asImageBitmap()
            }
            if (loaded != null) {
                artworkCache.put(cacheKey, loaded)
                bitmap = loaded
            }
        }
    }
    return bitmap
}

@Composable
fun TrackRow(
    title: String,
    artist: String,
    modifier: Modifier = Modifier,
    artworkPath: String? = null,
    audioPath: String? = null,
    contentPadding: PaddingValues = PaddingValues(
        start = LocalSistrumTokens.current.spacing.extraLarge,
        top = LocalSistrumTokens.current.spacing.small,
        end = LocalSistrumTokens.current.spacing.extraLarge,
        bottom = LocalSistrumTokens.current.spacing.small,
    ),
    onClick: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    trackNumber: Int? = null,
    explicit: Boolean = false,
    secondaryTextColor: Color = LocalSistrumTokens.current.colors.mutedText,
) {
    val tokens = LocalSistrumTokens.current
    val clickModifier = remember(onClick, onLongClick) {
        if (onClick != null || onLongClick != null) {
            Modifier.combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = onLongClick,
            )
        } else {
            Modifier
        }
    }
    val handleMoreClick = remember(onMoreClick) {
        { onMoreClick?.invoke(); Unit }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (trackNumber != null) {
            Text(
                text = trackNumber.toString(),
                modifier = Modifier.widthIn(min = 28.dp),
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor,
            )
        } else {
            val bitmap = rememberArtworkThumbnail(artworkPath, audioPath)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(tokens.shapes.small)
                    .background(tokens.colors.opaqueFill)
                    .border(1.dp, tokens.colors.divider, tokens.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                } else {
                    MaterialSymbol(
                        symbol = MaterialSymbols.MusicNote,
                        contentDescription = null,
                        size = 22.dp,
                        tint = secondaryTextColor,
                    )
                }
            }
        }

        // 2. Title & Artist Column
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = tokens.spacing.medium, end = tokens.spacing.small),
            verticalArrangement = Arrangement.Center,
        ) {
            val explicitLabel = if (explicit) stringResource(R.string.track_explicit) else ""
            val displayTitle = buildAnnotatedString {
                append(title)
                if (explicit) appendInlineContent(ExplicitBadgeId, " $explicitLabel")
            }
            Text(
                text = displayTitle,
                inlineContent = if (explicit) explicitBadgeInlineContent(isRtlText(title, LocalLayoutDirection.current))
                    else emptyMap(),
                style = MaterialTheme.typography.titleMedium,
                color = tokens.colors.text,
                maxLines = 1,
                overflow = if (explicit) TextOverflow.MiddleEllipsis else TextOverflow.Ellipsis,
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        trailingContent?.invoke(this) ?: run {
            IconButton(
                onClick = handleMoreClick,
                modifier = Modifier.size(48.dp),
            ) {
                MaterialSymbol(
                    symbol = MaterialSymbols.MoreVert,
                    contentDescription = stringResource(R.string.track_row_more_options),
                    size = 20.dp,
                    tint = secondaryTextColor,
                )
            }
        }
    }
}
