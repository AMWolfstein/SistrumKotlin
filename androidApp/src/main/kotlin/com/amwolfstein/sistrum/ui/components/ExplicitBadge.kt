package com.amwolfstein.sistrum.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amwolfstein.sistrum.ui.theme.LocalSistrumColors

internal const val ExplicitBadgeId = "explicit"

/**
 * An "E" in a small rounded square, placed 6dp after the title. The badge follows the title's
 * own direction, so it sits to the left of an Arabic title and to the right of a Latin one.
 */
@Composable
internal fun explicitBadgeInlineContent(titleIsRtl: Boolean, badgeTestTag: String? = null): Map<String, InlineTextContent> {
    val colors = LocalSistrumColors.current
    val density = LocalDensity.current
    val placeholder = with(density) { Placeholder(24.dp.toSp(), 18.dp.toSp(), PlaceholderVerticalAlign.TextCenter) }
    return mapOf(
        ExplicitBadgeId to InlineTextContent(placeholder) {
            Box(Modifier.fillMaxSize(), contentAlignment = if (titleIsRtl) AbsoluteAlignment.CenterLeft else AbsoluteAlignment.CenterRight) {
                Box(
                    Modifier.size(18.dp).clip(RoundedCornerShape(4.dp))
                        .background(colors.foregroundSubtle.copy(alpha = 0.85f))
                        .clearAndSetSemantics { if (badgeTestTag != null) testTag = badgeTestTag },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("E", color = colors.playerBackdrop, fontSize = with(density) { 12.dp.toSp() }, fontWeight = FontWeight.Bold, lineHeight = with(density) { 12.dp.toSp() })
                }
            }
        },
    )
}
