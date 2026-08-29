package com.yft.rippleup.ui.screens.verify

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.theme.Teal
import kotlinx.coroutines.delay

/**
 * p49 — Scan QR Code (desktop). The phone build scans with the real camera + MLKit;
 * on desktop there is no camera pipeline, so the viewfinder keeps the exact PDF
 * visuals and a click simulates scanning a partner's Ripple QR.
 */
@Composable
fun QrScanScreen(
    onDetected: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF05070A))
    ) {
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A2F33))
                    .noRippleClickable { onClose() },
                contentAlignment = Alignment.Center,
            ) { Text("✕", color = Color.White, fontSize = 13.sp) }
            Text("Scan QR Code", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Color.White)
            Spacer(Modifier.size(40.dp))
        }
        Spacer(Modifier.height(18.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0A0E12))
                .noRippleClickable { onDetected() },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            ViewfinderOverlay()
            Spacer(Modifier.weight(1f))
            Text(
                "Desktop build — click the viewfinder to scan a partner's Ripple QR",
                color = Color(0xFF5F7B75),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 18.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ViewfinderOverlay() {
    var lineT by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            lineT = (lineT + 0.012f) % 1f
            delay(16)
        }
    }
    Canvas(Modifier.size(240.dp)) {
        val bracket = 46.dp.toPx()
        val stroke = 4.dp.toPx()
        val w = size.width
        val h = size.height
        val c = Teal
        fun tl() = listOf(Offset(0f, bracket) to Offset(0f, stroke / 2), Offset(0f, 0f) to Offset(bracket, 0f))
        fun bl() = listOf(Offset(0f, h - bracket) to Offset(0f, h - stroke / 2), Offset(0f, h) to Offset(bracket, h))
        fun tr() = listOf(Offset(w - bracket, 0f) to Offset(w, 0f), Offset(w, stroke / 2) to Offset(w, bracket))
        fun br() = listOf(Offset(w - bracket, h) to Offset(w, h), Offset(w, h - bracket) to Offset(w, h - stroke / 2))
        listOf(tl(), bl(), tr(), br()).flatten().forEach { (a, b) ->
            drawLine(c, a, b, strokeWidth = stroke, cap = StrokeCap.Round)
        }
        val y = h * (0.25f + lineT * 0.5f)
        drawLine(
            Color(0xFF35D0C0),
            Offset(w * 0.08f, y), Offset(w * 0.92f, y),
            strokeWidth = 2.dp.toPx(),
        )
    }
}
