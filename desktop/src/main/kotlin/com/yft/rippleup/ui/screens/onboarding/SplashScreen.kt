package com.yft.rippleup.ui.screens.onboarding

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yft.rippleup.resources.Res
import com.yft.rippleup.resources.drop
import org.jetbrains.compose.resources.painterResource
import com.yft.rippleup.ui.components.GradientButton
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.theme.*
import kotlinx.coroutines.delay

/** p19/p02 — splash: emblem circles + teal wordmark on mint-white. */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1400)
        onDone()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgOnboarding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmblemArt()
        Spacer(Modifier.height(28.dp))
        Text(
            "RippleUp",
            style = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
            color = TealDeep,
        )
        Spacer(Modifier.height(120.dp))
    }
}

/** Concentric mint circles + drop — shared by splash and onboarding. */
@Composable
fun EmblemArt(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(250.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(250.dp)
                .clip(CircleShape)
                .background(Color(0xFFE2F2ED))
        )
        Box(
            Modifier
                .size(195.dp)
                .clip(CircleShape)
                .background(Color(0xFFD2EBE4))
        )
        Image(
            painter = painterResource(Res.drawable.drop),
            contentDescription = null,
            modifier = Modifier.size(74.dp),
        )
    }
}

/** p20-22 — three onboarding pages with fact pill, dots, Next/Get Started, Skip. */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pages = com.yft.rippleup.data.Content.onboarding
    var page by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgOnboarding)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            Text(
                "Skip",
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = TealDeep,
                modifier = Modifier.noRippleClickable { onFinished() },
            )
        }
        Spacer(Modifier.height(70.dp))
        EmblemArt(modifier = Modifier.size(280.dp))
        Spacer(Modifier.height(54.dp))
        Text(
            pages[page].first,
            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp),
            color = Ink,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            pages[page].second,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp),
            color = Secondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(26.dp))
        // fact pill
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFEFF9F6))
                .padding(horizontal = 14.dp, vertical = 13.dp)
        ) {
            Text(
                pages[page].third,
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                color = Teal,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        Spacer(Modifier.weight(1f))
        // dots
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { i ->
                val active = i == page
                Box(
                    Modifier
                        .size(width = if (active) 24.dp else 8.dp, height = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (active) Teal else Color(0xFFB9DED6))
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        GradientButton(
            label = if (page == 2) "Get Started" else "Next",
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (page == 2) onFinished() else page += 1
        }
        Spacer(Modifier.height(28.dp))
    }
}
