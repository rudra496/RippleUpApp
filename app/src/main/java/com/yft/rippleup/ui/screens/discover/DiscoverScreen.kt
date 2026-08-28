package com.yft.rippleup.ui.screens.discover

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yft.rippleup.data.Content
import com.yft.rippleup.data.DiscoverAction
import com.yft.rippleup.data.db.RippleEntity
import com.yft.rippleup.ui.AppViewModel
import com.yft.rippleup.ui.components.CircleIconButton
import com.yft.rippleup.ui.components.PillTag
import com.yft.rippleup.ui.components.RippleLogo
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.screens.home.dashedBorder
import com.yft.rippleup.ui.theme.*

/** p05-07 — Discover with self-report / partner-verified / partners chips. */
@Composable
fun DiscoverScreen(
    vm: AppViewModel,
    onOpenNotifications: () -> Unit,
    onStartVerifyFor: (DiscoverAction) -> Unit,
) {
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
            CircleIconButton(onClick = onOpenNotifications, badge = true) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = Ink, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Discover ",
            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
            color = Ink,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(14.dp))
        // search bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🔍", fontSize = 14.sp)
            Spacer(Modifier.width(10.dp))
            Text("Search actions, partners, events…", color = Secondary, fontSize = 14.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("self-report", "partner-verified", "partners").forEachIndexed { i, label ->
                val active = i == tab
                Box(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) Teal else InactiveChip)
                        .noRippleClickable { tab = i }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(label, color = if (active) Color.White else Secondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        when (tab) {
            0 -> {
                Text(
                    "Self-report by logging your action with an image",
                    color = Secondary, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(10.dp))
                Content.selfReport.forEach { a -> ActionCard(a) { onStartVerifyFor(a) } }
            }
            1 -> {
                Text(
                    "Visit a partner location and scan the QR to earn",
                    color = Secondary, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(10.dp))
                Content.partnerVerified.forEach { a -> ActionCard(a) { onStartVerifyFor(a) } }
            }
            else -> {
                Spacer(Modifier.height(2.dp))
                Content.partners.forEach { p -> PartnerCard(p) }
            }
        }
    }
}

@Composable
private fun ActionCard(a: DiscoverAction, onAdd: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(a.emoji, fontSize = 18.sp)
                Spacer(Modifier.width(2.dp))
                Text(a.title, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Ink)
                Spacer(Modifier.width(6.dp))
                PillTag(a.difficulty, Color(0xFFEFF1F1), Secondary)
                Spacer(Modifier.width(2.dp))
                val (bg, fg) = when (a.ptsTone) {
                    0 -> Color(0xFFD8F5A3) to Color(0xFF3A6B00)
                    1 -> GoldChip to Color(0xFF6B4E00)
                    else -> Color(0xFFE4D5FA) to Color(0xFF5B2E91)
                }
                PillTag("+${a.points} pts", bg, fg, bold = true)
            }
            Spacer(Modifier.height(4.dp))
            if (a.partner.isNotEmpty()) {
                Text(
                    "📍 " + a.partner,
                    color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                )
            }
            Text(a.impact, color = TealDeep, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(a.note, color = Secondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (a.done) Orange else Teal)
                .noRippleClickable(enabled = !a.done) { onAdd() },
            contentAlignment = Alignment.Center,
        ) {
            Text(if (a.done) "✓" else "＋", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PartnerCard(p: com.yft.rippleup.data.Partner) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Mint),
                contentAlignment = Alignment.Center,
            ) { Text(p.emoji, fontSize = 22.sp) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(p.name, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Ink)
                Text(p.offer, color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
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
            Text(p.promo, color = PromoText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (p.chipOrange) Orange else Teal)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    (if (p.chipOrange) "✓" else "＋") + p.chip,
                    color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
