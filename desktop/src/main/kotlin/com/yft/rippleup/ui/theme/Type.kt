package com.yft.rippleup.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.yft.rippleup.resources.Res
import com.yft.rippleup.resources.montserrat_400
import com.yft.rippleup.resources.montserrat_500
import com.yft.rippleup.resources.montserrat_600
import com.yft.rippleup.resources.montserrat_700
import com.yft.rippleup.resources.montserrat_800
import org.jetbrains.compose.resources.Font

@Composable
fun Montserrat(): FontFamily = FontFamily(
    Font(Res.font.montserrat_400, FontWeight.Normal),
    Font(Res.font.montserrat_500, FontWeight.Medium),
    Font(Res.font.montserrat_600, FontWeight.SemiBold),
    Font(Res.font.montserrat_700, FontWeight.Bold),
    Font(Res.font.montserrat_800, FontWeight.ExtraBold),
)

@Composable
fun RippleTypography(): Typography {
    val fam = Montserrat()
    val base = TextStyle(fontFamily = fam)
    return Typography(
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
}
