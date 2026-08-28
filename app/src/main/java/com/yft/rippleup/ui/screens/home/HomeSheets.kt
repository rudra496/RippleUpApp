package com.yft.rippleup.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yft.rippleup.data.Content
import com.yft.rippleup.data.db.RippleEntity
import com.yft.rippleup.ui.AppViewModel
import com.yft.rippleup.ui.components.PillTag
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.theme.*

/** Shared sheet chrome: drag handle + title row with X. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetScaffold(
    title: String,
    trailing: @Composable (() -> Unit)? = null,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = Color.White,
        dragHandle = null,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFD6E4E0))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = Ink)
                if (trailing != null) trailing() else CloseX(onClose)
            }
            Spacer(Modifier.height(14.dp))
        }
        content()
    }
}

@Composable
fun CloseX(onClose: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFFEFF4F3))
            .noRippleClickable { onClose() },
        contentAlignment = Alignment.Center,
    ) {
        Text("✕", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** p15/p33 — Edit Today's Ripples List (strike-through remove, + add, Discover more). */
@Composable
fun EditListSheet(vm: AppViewModel, ripples: List<RippleEntity>, onClose: () -> Unit) {
    val removedIds = remember { mutableStateListOf<Long>() }
    var saved by remember { mutableStateOf(false) }

    SheetScaffold(
        title = "Edit Today's Ripples List",
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Teal)
                        .noRippleClickable {
                            removedIds.forEach { vm.removeRipple(it) }
                            saved = true
                            onClose()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Save", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                CloseX(onClose)
            }
        },
        onClose = onClose,
    ) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(
                "${ripples.size - removedIds.size} actions in today's list",
                color = Secondary,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                ripples.forEach { r ->
                    val removed = r.id in removedIds
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0x14000000), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Mint),
                            contentAlignment = Alignment.Center,
                        ) { Text(emojiFor(r), fontSize = 18.sp) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                r.title,
                                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                color = if (removed) Gray else Ink,
                                textDecoration = if (removed) TextDecoration.LineThrough else null,
                            )
                            Text(
                                r.subtitle,
                                style = TextStyle(fontSize = 12.sp),
                                color = if (removed) Color(0xFFB6C2BF) else Teal,
                            )
                        }
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (removed) Mint else Color(0xFFFDE8E8))
                                .noRippleClickable {
                                    if (removed) removedIds.remove(r.id) else removedIds.add(r.id)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (removed) "↺" else "🗑",
                                fontSize = 13.sp,
                                color = if (removed) Teal else DangerRed,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(10.dp))
                // Add Action dashed container
                Column(
                    Modifier
                        .fillMaxWidth()
                        .dashedBorder(1.5.dp, Teal, 24.dp)
                        .background(Color(0xFFF6FBFA), RoundedCornerShape(24.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        "Add Action",
                        color = Teal,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Content.addableActions.forEach { (emoji, title, pts) ->
                        val inList = ripples.any { it.title == title }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(Mint),
                                contentAlignment = Alignment.Center,
                            ) { Text(emoji, fontSize = 16.sp) }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(title, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Ink)
                                Row {
                                    PillTag("Self report", TagBg, GrayTag)
                                    Spacer(Modifier.width(5.dp))
                                    PillTag("$pts pts", Gray, Color(0xFFFFFDF7), bold = true)
                                }
                            }
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(if (inList) Mint else Teal)
                                    .noRippleClickable(enabled = !inList) {
                                        val sub = if (title.contains("Compost")) "Make your food scraps count" else "Skip single-use cups"
                                        val key = if (title.contains("Compost")) "compost" else "refill"
                                        val kg = if (title.contains("Compost")) 0.3f else 0.05f
                                        vm.addPending(title, sub, pts.removePrefix("+").toInt(), key, kg)
                                    },
                                contentAlignment = Alignment.Center,
                            ) { Text(if (inList) "✓" else "+", color = if (inList) Teal else Color.White, fontWeight = FontWeight.Bold) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Teal)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🍃  Discover more", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(26.dp))
            }
        }
    }
}

private fun emojiFor(r: RippleEntity): String = when (r.art) {
    "veg" -> "🥬"
    "balloon" -> "👕"
    else -> if (r.title.contains("meal", true) || r.title.contains("food", true)) "🍱" else "🌿"
}

/** p34 — Notifications sheet with colored cards. */
@Composable
fun NotificationsSheet(onClose: () -> Unit, onLogAction: () -> Unit) {
    val items = remember { mutableStateListOf<com.yft.rippleup.data.Notif>(*Content.notifications.toTypedArray()) }
    var cleared by remember { mutableStateOf(false) }
    SheetScaffold(
        title = "Notifications",
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Clear all",
                    color = Teal,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.noRippleClickable { items.clear(); cleared = true },
                )
                CloseX(onClose)
            }
        },
        onClose = onClose,
    ) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(
                if (cleared || items.isEmpty()) "You're all caught up 🎉" else "${items.size} unread",
                color = Secondary,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items.toList().forEach { n ->
                    val bg = when (n.bgTone) {
                        0 -> NotifOrange; 1 -> NotifMint; 2 -> NotifLavender; 3 -> NotifGreen; else -> NotifCream
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(bg)
                            .padding(16.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(n.emoji, fontSize = 20.sp)
                                Spacer(Modifier.width(10.dp))
                                Text(n.title, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = Ink)
                            }
                            Text(
                                "✕", fontSize = 12.sp, color = Ink,
                                modifier = Modifier.noRippleClickable { items.remove(n) },
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(n.body, style = TextStyle(fontSize = 14.sp, lineHeight = 21.sp), color = Color(0xFF334441))
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (n.button.isNotEmpty()) {
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(15.dp))
                                        .background(if (n.buttonOrange) Orange else Teal)
                                        .noRippleClickable {
                                            if (n.button == "Log an action") { onClose(); onLogAction() }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(n.button, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (n.time.isNotEmpty()) Text(n.time, color = Secondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/** p31/32 — Event detail bottom sheet with Register → Registered state. */
@Composable
fun EventDetailSheet(onClose: () -> Unit) {
    var registered by remember { mutableStateOf(false) }
    val ev = Content.eventDetail
    SheetScaffold(title = "", trailing = {}, onClose = onClose) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 30.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEAF3F8)),
                    contentAlignment = Alignment.Center,
                ) { Text(ev.emoji, fontSize = 30.sp) }
                CloseX(onClose)
            }
            Spacer(Modifier.height(14.dp))
            Text(ev.name, style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold), color = Ink)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(Mint)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) { Text("+500 pts", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0xFFFDF4DC))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) { Text("🏅 Community Champion", color = Orange, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            }
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0x14000000), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                DetailRow("📅", "Sat, Jun 22")
                DetailRow("🕗", "8:00 AM")
                DetailRow("📍", "Narendra Park")
                DetailRow("👥", "${ev.going} going")
            }
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE9F6F1))
                    .padding(16.dp)
            ) {
                Text("HOW TO PARTICIPATE", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(12.dp))
                Content.eventDetailSteps.forEachIndexed { i, step ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Teal),
                            contentAlignment = Alignment.Center,
                        ) { Text("${i + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(10.dp))
                        Text(step, style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp), color = Color(0xFF2F4440))
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("${ev.going}", "Going", Modifier.weight(1f))
                StatCard("+500", "Ripple Points", Modifier.weight(1f), valueTeal = true)
            }
            Spacer(Modifier.height(18.dp))
            if (!registered) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Teal)
                        .noRippleClickable { registered = true }
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Register Now", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Mint)
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Registered!", color = Teal, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Tap to cancel",
                        color = Secondary,
                        fontSize = 11.sp,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.noRippleClickable { registered = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(emoji: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Mint),
            contentAlignment = Alignment.Center,
        ) { Text(emoji, fontSize = 15.sp) }
        Spacer(Modifier.width(12.dp))
        Text(text, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Ink)
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier, valueTeal: Boolean = false) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0x14000000), RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold), color = if (valueTeal) Teal else Ink)
        Text(label, color = Secondary, fontSize = 12.sp)
    }
}

/** p10/p47 — "How do you wish to you verify your Ripple?" dialog. */
@Composable
fun VerifyChoiceDialog(
    onSelfReport: () -> Unit,
    onQr: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth()) {
                Text(
                    "How do you wish to you\nverify your Ripple?",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp),
                    color = Ink,
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFECECEC))
                        .noRippleClickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) { Text("✕", fontSize = 12.sp, color = Color(0xFF444444)) }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ChoiceTile("Self - Report", com.yft.rippleup.R.drawable.monster_selfreport, onSelfReport, Modifier.weight(1f))
                ChoiceTile("RippleUp QR", com.yft.rippleup.R.drawable.monster_qr, onQr, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "scan Ripple QR at partner location or events to earn more points + verified badge",
                style = TextStyle(fontSize = 9.sp, lineHeight = 13.sp),
                color = HintGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ChoiceTile(label: String, artRes: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Mint)
            .border(1.5.dp, Teal, RoundedCornerShape(16.dp))
            .noRippleClickable { onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.foundation.Image(
            androidx.compose.ui.res.painterResource(artRes),
            contentDescription = label,
            modifier = Modifier.size(width = 96.dp, height = 110.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(label, color = Teal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
