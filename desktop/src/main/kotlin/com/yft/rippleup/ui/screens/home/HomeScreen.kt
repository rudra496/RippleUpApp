package com.yft.rippleup.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yft.rippleup.resources.Res
import com.yft.rippleup.resources.drop
import com.yft.rippleup.data.remote.CloudEvent
import com.yft.rippleup.data.remote.CloudRipple
import com.yft.rippleup.ui.AppViewModel
import com.yft.rippleup.ui.components.CircleIconButton
import com.yft.rippleup.ui.components.PillTag
import com.yft.rippleup.ui.components.RippleLogo
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.theme.*

/** p03/p17/p68 — HOME, fully driven by real cloud data. */
@Composable
fun HomeScreen(
    vm: AppViewModel,
    onOpenNotifications: () -> Unit,
    onOpenEvent: () -> Unit,
    onLogAction: () -> Unit,
) {
    val ripples by vm.ripples.collectAsState()
    val stats by vm.stats.collectAsState()
    val events by vm.events.collectAsState()
    var showEdit by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(BgMain)) {
        Column(
            Modifier
                .fillMaxSize()
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
                CircleIconButton(bg = White, onClick = onOpenNotifications, badge = true) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = Ink, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            CalendarStrip(verifiedDays(vm.ripples.value))
            Spacer(Modifier.height(18.dp))
            Text(
                "Hey ${vm.displayName}!",
                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                color = Ink,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(12.dp))
            StreakCard(stats.streak, stats.longest, stats.points)
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Today's Ripples list", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = Ink)
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Edit list",
                    tint = Ink,
                    modifier = Modifier.noRippleClickable { showEdit = true }.size(20.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            TimelineCard(ripples.filter { it.created_at?.startsWith(todayKey()) == true }, onLogAction)
            Spacer(Modifier.height(22.dp))
            if (events.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Upcoming Events", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Ink)
                    Text(
                        "See all", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.noRippleClickable { onOpenEvent() },
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    events.take(2).forEach { ev -> EventCard(ev) { onOpenEvent() } }
                    if (events.size > 2) {
                        Box(Modifier.width(120.dp)) {
                            EventCard(events[2]) { onOpenEvent() }
                        }
                    }
                }
            }
        }

        if (showEdit) {
            EditListSheet(vm, ripples, onClose = { showEdit = false })
        }
    }
}

private fun todayKey(): String =
    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

/** Days THIS week (Mon..today) that have at least one verified ripple. */
private fun verifiedDays(ripples: List<CloudRipple>): Set<String> {
    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    val cal = java.util.Calendar.getInstance()
    val todayIdx = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
    val days = mutableSetOf<String>()
    for (off in 0..todayIdx) {
        val c = cal.clone() as java.util.Calendar
        c.add(java.util.Calendar.DAY_OF_YEAR, off - todayIdx)
        days.add(fmt.format(c.time))
    }
    return ripples.filter { it.status == "self" || it.status == "qr" }
        .mapNotNull { it.created_at?.take(10) }
        .toSet()
        .intersect(days)
}

/** Mon–Sun white r8 cards: gold checks for verified days, glowing today, gray drops. */
@Composable
fun CalendarStrip(verifiedDays: Set<String> = emptySet()) {
    val cal = java.util.Calendar.getInstance()
    val todayIdx = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 // Mon=0
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        labels.forEachIndexed { idx, label ->
            val state = when {
                idx == todayIdx -> 2
                verifiedDays.contains(dayKeyFor(cal, idx - todayIdx)) -> 1
                else -> 0
            }
            Column(
                Modifier
                    .weight(1f)
                    .height(86.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(top = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, fontSize = 12.sp, color = Color(0xFF3E4A48))
                Box(
                    Modifier
                        .padding(bottom = 12.dp)
                        .size(30.dp)
                        .drawBehind {
                            when (state) {
                                1 -> drawCircle(Color(0x40F9D14C))
                                2 -> drawCircle(Color(0x668FFBE6))
                                else -> drawCircle(Color(0x14000000))
                            }
                        }
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state == 1) {
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Gold),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        androidx.compose.foundation.Image(
                            painterResource(Res.drawable.drop),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            colorFilter = ColorFilter.tint(if (state == 2) Teal else GrayPending),
                        )
                    }
                }
            }
        }
    }
}

private fun dayKeyFor(base: java.util.Calendar, offsetDays: Int): String {
    val c = base.clone() as java.util.Calendar
    c.add(java.util.Calendar.DAY_OF_YEAR, offsetDays)
    return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(c.time)
}

/** Teal gradient streak card with progress ring + pills — REAL numbers only. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StreakCard(streak: Int, longest: Int, totalPoints: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(StreakGrad)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            StreakRing(fraction = streak.toFloat() / longest.coerceAtLeast(7))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$streak", style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold), color = InkSoft)
                Text("Days", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF96BAB0))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Your Ripples are adding up!",
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
                color = InkSoft,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "Every verified action avoids real CO₂e. Keep the streak going!",
                style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
                color = Secondary,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PillTag("Longest streak", Color.White, TealPill)
                PillTag("$longest Days", TealPill, TealPaleText, bold = true)
                Spacer(Modifier.width(2.dp))
                PillTag("Ripple Points", Color.White, TealPill)
                PillTag("${com.yft.rippleup.util.Fmt.grouped(totalPoints)} pts", TealPill, TealPaleText, bold = true)
            }
        }
    }
}

/** Ring: track circle + teal arc. */
@Composable
fun StreakRing(fraction: Float, modifier: Modifier = Modifier, tint: Color = Teal, sizeDp: androidx.compose.ui.unit.Dp = 86.dp) {
    Box(modifier.size(sizeDp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(sizeDp)) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2 + 2.dp.toPx()
            drawArc(
                color = RingTrack,
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(this.size.width - inset * 2, this.size.height - inset * 2),
                style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
            drawArc(
                color = tint,
                startAngle = 0f, sweepAngle = 360f * fraction.coerceIn(0f, 1f), useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(this.size.width - inset * 2, this.size.height - inset * 2),
                style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
        }
    }
}

/** White card with the dashed timeline — real logged ripples only, honest states. */
@Composable
fun TimelineCard(ripples: List<CloudRipple>, onLogAction: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text(
            java.text.SimpleDateFormat("EEEE, MMM d", java.util.Locale.US)
                .format(java.util.Date()),
            style = TextStyle(fontSize = 12.sp),
            color = Secondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        if (ripples.isEmpty()) {
            Text(
                "No ripples yet today.\nLog your first action below!",
                style = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
                color = Secondary,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                textAlign = TextAlign.Center,
            )
        } else {
            ripples.forEachIndexed { i, r ->
                TimelineRow(r, isLast = i == ripples.lastIndex)
                if (i != ripples.lastIndex) Spacer(Modifier.height(4.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.align(Alignment.CenterHorizontally)) {
            PendingSelfReportBox(onTap = onLogAction)
        }
    }
}

@Composable
fun TimelineRow(r: CloudRipple, isLast: Boolean) {
    Row {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(34.dp)) {
            val done = r.status == "self" || r.status == "qr"
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (done) Teal else Color.White)
                    .border(2.dp, Teal, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (done) Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            if (!isLast) {
                Canvas(
                    Modifier
                        .width(2.dp)
                        .height(64.dp)
                ) {
                    drawLine(
                        color = Color(0x590D9488),
                        start = androidx.compose.ui.geometry.Offset(1f, 0f),
                        end = androidx.compose.ui.geometry.Offset(1f, size.height),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f)),
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(r.title, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Ink)
            Spacer(Modifier.height(2.dp))
            Text(r.subtitle ?: "", style = TextStyle(fontSize = 12.sp), color = Color(0xFF4B5563))
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                when (r.status) {
                    "self" -> { PillTag("Self Reported", TagBg, GrayTag); PillTag("+${r.points} pts", Orange, Color.White, bold = true) }
                    "qr" -> { PillTag("QR Verified", TagBg, GrayTag); PillTag("+${r.points} pts", Orange, Color.White, bold = true) }
                    "pending_review" -> { PillTag("Pending review", TagBg, GrayTag); PillTag("+${r.points} pts", Gray, Color(0xFFFFFDF7), bold = true) }
                    else -> PillTag(r.status, TagBg, GrayTag)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

/** Dashed "+ Log an action" box (p03/p68 look). */
@Composable
fun PendingSelfReportBox(onTap: () -> Unit) {
    Box(
        Modifier
            .noRippleClickable { onTap() }
            .size(width = 110.dp, height = 58.dp)
            .background(Mint, RoundedCornerShape(10.dp))
            .dashedBorder(1.5.dp, Teal, 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("+", color = Teal, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(5.dp))
            Text("Log an action", color = Teal, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Dashed stroke modifier (PDF capture area + self-report box). */
fun Modifier.dashedBorder(strokeWidth: androidx.compose.ui.unit.Dp, color: Color, cornerRadius: androidx.compose.ui.unit.Dp): Modifier =
    this.drawBehind {
        val stroke = strokeWidth.toPx()
        val radius = cornerRadius.toPx()
        val path = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                )
            )
        }
        drawPath(
            path, color,
            style = Stroke(
                width = stroke,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            ),
        )
    }

/** Upcoming event card (real events, register state). */
@Composable
fun EventCard(ev: CloudEvent, onClick: () -> Unit) {
    var registered by remember { mutableStateOf(false) }
    Column(
        Modifier
            .width(158.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .noRippleClickable { onClick() }
            .padding(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(ev.emoji?.ifBlank { "🌍" } ?: "🌍", fontSize = 22.sp)
            Text("${ev.going} going", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Secondary)
        }
        Spacer(Modifier.height(8.dp))
        Text(ev.title, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = Ink, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(ev.date ?: "", fontSize = 10.sp, color = Secondary)
        Spacer(Modifier.height(2.dp))
        Text("📍 ${ev.place}", fontSize = 10.sp, color = Secondary, maxLines = 1)
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (registered) Mint else Teal)
                .noRippleClickable { registered = true }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (registered) "Registered ✓" else "Register",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (registered) Teal else Color.White,
            )
        }
    }
}
