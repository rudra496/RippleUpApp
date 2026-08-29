package com.yft.rippleup.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yft.rippleup.resources.Res
import com.yft.rippleup.resources.drop
import com.yft.rippleup.resources.logo
import com.yft.rippleup.ui.theme.OrangeLight
import com.yft.rippleup.ui.theme.Teal
import com.yft.rippleup.ui.theme.White
import org.jetbrains.compose.resources.painterResource

@Composable
fun RippleLogo(size: Dp = 40.dp, useEmblem: Boolean = false) {
    Image(
        painter = painterResource(if (useEmblem) Res.drawable.drop else Res.drawable.logo),
        contentDescription = "RippleUp",
        modifier = Modifier.size(size),
    )
}

/** Teal gradient pill button (Next / Continue / Submit…). */
@Composable
fun GradientButton(
    label: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .noRippleClickable(enabled) { onClick() }
            .clip(RoundedCornerShape(28.dp))
            .background(
                if (enabled) Brush.horizontalGradient(listOf(Color(0xFF14B8A6), Teal))
                else Brush.horizontalGradient(listOf(com.yft.rippleup.ui.theme.MintInput, com.yft.rippleup.ui.theme.MintInput))
            )
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            color = if (enabled) White else Teal,
        )
    }
}

/** Circle icon button on mint (back, close, bell). Content lambda is the last param. */
@Composable
fun CircleIconButton(
    bg: Color = com.yft.rippleup.ui.theme.Mint,
    size: Dp = 36.dp,
    badge: Boolean = false,
    onClick: () -> Unit = {},
    icon: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .noRippleClickable { onClick() }
            .size(size)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) icon()
        if (badge) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(OrangeLight)
            )
        }
    }
}

/** Small pill tag: Self Reported / +20 pts / Easy / Medium… */
@Composable
fun PillTag(text: String, bg: Color, fg: Color, bold: Boolean = false) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text,
            style = TextStyle(fontSize = 10.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal),
            color = fg,
        )
    }
}
