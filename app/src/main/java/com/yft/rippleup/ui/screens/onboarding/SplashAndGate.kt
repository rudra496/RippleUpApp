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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yft.rippleup.R
import com.yft.rippleup.ui.components.GradientButton
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.theme.*

/** p19/p02 — splash: emblem circles + teal wordmark on mint-white. */
@Composable
fun SplashScreen() {
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
    }
}

/** Concentric mint circles + drop — splash/onboarding emblem. */
@Composable
fun EmblemArt(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(250.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(250.dp).clip(CircleShape).background(Color(0xFFE2F2ED)))
        Box(Modifier.size(195.dp).clip(CircleShape).background(Color(0xFFD2EBE4)))
        Image(
            painter = painterResource(R.drawable.drop),
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
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            pages[page].second,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp),
            color = Secondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(26.dp))
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

/** Approval gate: new accounts wait for an admin to verify them. */
@Composable
fun PendingApprovalScreen(vm: com.yft.rippleup.ui.AppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgOnboarding)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(170.dp).clip(CircleShape).background(Color(0xFFE2F2ED)))
            Box(Modifier.size(130.dp).clip(CircleShape).background(Color(0xFFD2EBE4)))
            Image(
                painter = painterResource(R.drawable.drop),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
            )
        }
        Spacer(Modifier.height(26.dp))
        Text(
            "You're in! One last step",
            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
            color = Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Your account is being verified by the RippleUp team. " +
                "You'll get a notification the moment it's approved — usually within a day.",
            style = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
            color = Secondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            vm.session.value?.email ?: "",
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = Teal,
        )
        Spacer(Modifier.height(30.dp))
        GradientButton("Sign out", modifier = Modifier.fillMaxWidth()) { vm.logout() }
    }
}
