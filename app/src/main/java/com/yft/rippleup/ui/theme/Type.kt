package com.yft.rippleup.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.yft.rippleup.R

val Montserrat = FontFamily(
    Font(R.font.montserrat_400, FontWeight.Normal),
    Font(R.font.montserrat_500, FontWeight.Medium),
    Font(R.font.montserrat_600, FontWeight.SemiBold),
    Font(R.font.montserrat_700, FontWeight.Bold),
    Font(R.font.montserrat_800, FontWeight.ExtraBold),
)

private val base = TextStyle(fontFamily = Montserrat)

val RippleTypography = Typography(
    displayMedium = base.copy(fontSize = 36.sp, fontWeight = FontWeight.Bold),
    headlineMedium = base.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    headlineSmall = base.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleLarge = base.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
    titleMedium = base.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
    titleSmall = base.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = base.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodyMedium = base.copy(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    bodySmall = base.copy(fontSize = 10.sp, fontWeight = FontWeight.Normal),
    labelLarge = base.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
    labelMedium = base.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = base.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
)
