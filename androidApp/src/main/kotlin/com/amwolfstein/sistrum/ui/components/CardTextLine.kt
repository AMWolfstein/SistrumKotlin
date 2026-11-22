package com.amwolfstein.sistrum.ui.components

import androidx.compose.foundation.layout.paddingFrom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import kotlin.math.ceil

internal data class CardTextLine(val aboveBaseline: Dp, val belowBaseline: Dp) {
    val height: Dp get() = aboveBaseline + belowBaseline
    val modifier: Modifier
        get() = Modifier.paddingFrom(FirstBaseline, before = aboveBaseline, after = belowBaseline)
}

/** Reserves the same baseline and line height for Latin and Arabic labels in a card. */
@Composable
internal fun rememberCardTextLine(style: TextStyle, lines: Int = 1): CardTextLine {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(measurer, density, style, lines) {
        // Android may add fallback-font padding outside Compose's fixed line height.
        val latin = measurer.measure(List(lines) { "Ágy 2026" }.joinToString("\n"), style = style, maxLines = lines)
        val arabic = measurer.measure(List(lines) { "لِينَا مَعَادٌ" }.joinToString("\n"), style = style, maxLines = lines)
        val above = ceil(maxOf(latin.firstBaseline, arabic.firstBaseline)).toInt()
        val below = ceil(maxOf(
            latin.size.height - latin.firstBaseline,
            arabic.size.height - arabic.firstBaseline,
        )).toInt()
        with(density) { CardTextLine(above.toDp(), below.toDp()) }
    }
}
