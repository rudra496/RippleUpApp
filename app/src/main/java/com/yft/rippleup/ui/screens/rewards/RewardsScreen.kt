package com.yft.rippleup.ui.screens.rewards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yft.rippleup.data.Content
import com.yft.rippleup.ui.components.PillTag
import com.yft.rippleup.ui.components.RippleLogo
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.screens.onboarding.RewardTabs
import com.yft.rippleup.ui.theme.*

/** p08/p39 — Rewards + Badges tabs. */
@Composable
fun RewardsScreen(onOpenNotifications: () -> Unit, snackbar: (String) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    Column(
        Modifier
            .fillMaxSize()
            .background(BgMain)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp),
    ) {
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RippleLogo(size = 42.dp)
            Box(Modifier.weight(1f))
            CircleBell(onOpenNotifications)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Rewards",
            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
            color = Ink,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(14.dp))
        RewardTabs(tab) { tab = it }
        Spacer(Modifier.height(14.dp))
        Text(
            if (tab == 0) "Unlock partner rewards by completing actions at their location."
            else "Turn every ripple into a milestone worth celebrating.",
            color = Secondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(14.dp))
        if (tab == 0) Content.rewards.forEach { r -> RewardCard(r, snackbar) }
        else Content.badges.forEach { b -> BadgeCard(b) }
    }
}

@Composable
private fun CircleBell(onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White)
            .noRippleClickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = Ink, modifier = Modifier.size(19.dp))
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(OrangeLight)
        )
    }
}

@Composable
private fun RewardCard(r: com.yft.rippleup.data.RewardPartner, snackbar: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 7.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Mint),
                contentAlignment = Alignment.Center,
            ) { Text(r.emoji, fontSize = 20.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(r.name, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Ink)
                    if (r.ready) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(GreenReadyBg)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) { Text("Ready!", color = GreenReady, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            ProgressRing(r.progress)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(PromoBg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🎁", fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
            Text(r.promo, color = PromoText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .clip(RoundedCornerShape(15.dp))
                    .background(if (r.ready) Teal else Orange)
                    .noRippleClickable(enabled = r.ready) {
                        snackbar("Partner redemption is coming soon — show your streak at the counter!")
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    (if (r.ready) "✓  " else "🔒 ") + if (r.ready) "Claim!" else "Locked",
                    color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ProgressRing(done: Int) {
    Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(52.dp)) {
            val stroke = 5.dp.toPx()
            drawArc(
                color = RingTrack,
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = Teal,
                startAngle = -90f, sweepAngle = 360f * (done / 5f), useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$done/5", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkSoft)
            Text("Complete", fontSize = 8.sp, color = Color(0xFF96BAB0))
        }
    }
}

@Composable
private fun BadgeCard(b: com.yft.rippleup.data.Badge) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 7.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (b.earned) Color.White else Color(0xFFEFF4F2))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (b.earned) Mint else Color(0xFFE3ECE9)),
            contentAlignment = Alignment.Center,
        ) { Text(b.emoji, fontSize = 20.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                b.name,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                color = if (b.earned) Ink else Color(0xFF7A8B87),
            )
            Text(
                b.desc,
                style = TextStyle(fontSize = 12.sp),
                color = if (b.earned) Secondary else Color(0xFF9AAAA6),
            )
        }
        if (b.earned) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(1.6.dp, Teal, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("✓", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        } else {
            Text(
                b.remaining.replace(" remaining", "\nremaining"),
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp),
                color = Color(0xFF8A9B97),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }
    }
}
