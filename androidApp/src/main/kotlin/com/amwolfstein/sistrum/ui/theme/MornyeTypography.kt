package com.amwolfstein.sistrum.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.amwolfstein.sistrum.R

private val InterFontFamily = FontFamily(
    (100..900 step 100).map { weight ->
        Font(
            resId = R.font.inter_variable,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
        )
    },
)

private val MornyeBody = TextStyle(
    fontFamily = InterFontFamily,
    fontSize = 17.sp,
    letterSpacing = (-0.41).sp,
    lineHeight = 1.25.em,
    fontWeight = FontWeight.Normal,
)
private val MornyeDisplay = MornyeBody.copy(
    fontSize = 34.sp,
    letterSpacing = 0.38.sp,
    fontWeight = FontWeight.Bold,
)

/** Android text roles from Mornye's Inter theme, with line boxes that scale with text. */
internal val MornyeTypography = Typography(
    displayLarge = MornyeDisplay.copy(fontSize = 57.sp),
    displayMedium = MornyeDisplay.copy(fontSize = 45.sp),
    displaySmall = MornyeDisplay.copy(fontSize = 36.sp),
    headlineLarge = MornyeDisplay,
    headlineMedium = MornyeDisplay.copy(fontSize = 28.sp, letterSpacing = 0.36.sp),
    headlineSmall = MornyeDisplay.copy(fontSize = 22.sp, letterSpacing = 0.35.sp),
    titleLarge = MornyeDisplay.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = MornyeBody.copy(fontWeight = FontWeight.Medium),
    titleSmall = MornyeBody.copy(fontSize = 15.sp, letterSpacing = (-0.23).sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = MornyeBody,
    bodyMedium = MornyeBody.copy(fontSize = 15.sp, letterSpacing = (-0.23).sp),
    bodySmall = MornyeBody.copy(fontSize = 13.sp, letterSpacing = (-0.08).sp),
    labelLarge = MornyeBody.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = MornyeBody.copy(fontSize = 13.sp, letterSpacing = (-0.08).sp, fontWeight = FontWeight.Medium),
    labelSmall = MornyeBody.copy(fontSize = 11.sp, letterSpacing = (-0.24).sp, fontWeight = FontWeight.Medium),
)
