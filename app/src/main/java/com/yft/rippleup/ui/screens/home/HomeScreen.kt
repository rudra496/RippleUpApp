package com.yft.rippleup.ui.screens.home

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke as DsStroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yft.rippleup.R
import com.yft.rippleup.data.Content
import com.yft.rippleup.data.Event
import com.yft.rippleup.data.db.RippleEntity
import com.yft.rippleup.ui.AppViewModel
import com.yft.rippleup.ui.components.CircleIconButton
import com.yft.rippleup.ui.components.PillTag
import com.yft.rippleup.ui.components.RippleLogo
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.theme.*

/** p03/p17/p68 — HOME. */
@Composable
fun HomeScreen(
    vm: AppViewModel,
    onOpenNotifications: () -> Unit,
    onOpenEvent: () -> Unit,
    onStartVerify: (RippleEntity?) -> Unit,
) {
    val ripples by vm.ripples.collectAsState()
    val stats by vm.stats.collectAsState()
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
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = Ink,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            CalendarStrip()
            Spacer(Modifier.height(18.dp))
            Text(
                "Hey ${vm.displayName.substringBefore(' ')}!",
                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                color = Ink,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(12.dp))
            StreakCard(stats.streak, stats.longest, stats.pointsPill)
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Today's Ripples list", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = Ink)
                EditSquareIcon(onClick = { showEdit = true })
            }
            Spacer(Modifier.height(10.dp))
            TimelineCard(ripples, onStartVerify = { onStartVerify(it) })
            Spacer(Modifier.height(22.dp))
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
                Content.events.take(2).forEach { ev -> EventCard(ev) { onOpenEvent() } }
                Box(Modifier.width(120.dp)) {
                    EventCard(Content.events[2]) { onOpenEvent() }
                }
            }
        }

        if (showEdit) {
            EditListSheet(vm, ripples, onClose = { showEdit = false })
        }
    }
}

/** Pencil-in-rounded-square edit icon (p01 asset redrawn as icons). */
@Composable
fun EditSquareIcon(onClick: () -> Unit) {
    Box(
        Modifier
            .noRippleClickable { onClick() }
            .padding(3.dp)
    ) {
        Box(
            Modifier
                .size(22.dp)
                .border(1.6.dp, Ink, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Text("✎", fontSize = 11.sp, color = Ink)
        }
    }
}

/** Mon–Sun white r8 cards: gold checks, glowing today, gray drops. */
@Composable
fun CalendarStrip() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        val days = listOf("Mon" to 1, "Tue" to 1, "Wed" to 1, "Thu" to 2, "Fri" to 0, "Sat" to 0, "Sun" to 0)
        days.forEach { (label, state) ->
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
                            painterResource(R.drawable.drop),
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

/** Teal gradient streak card with progress ring + pills (p03). */
@Composable
fun StreakCard(streak: Int, longest: Int, pointsPill: Int) {
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
            StreakRing(fraction = streak.toFloat() / longest.coerceAtLeast(1))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$streak", style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold), color = InkSoft)
                Text("Days", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF96BAB0))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Your Ripples have been adding up!",
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
                color = InkSoft,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "You've helped avoid an estimated 20 g CO₂e this week.",
                style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
                color = Secondary,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                PillTag("Longest streak", Color.White, TealPill)
                PillTag("$longest Days", TealPill, TealPaleText, bold = true)
                Spacer(Modifier.width(2.dp))
                PillTag("Ripple Points", Color.White, TealPill)
                PillTag("${pointsPill}+ pts", TealPill, TealPaleText, bold = true)
            }
        }
    }
}

/** Ring: track circle + teal arc + small drop at the arc tail. */
@Composable
fun StreakRing(fraction: Float, modifier: Modifier = Modifier, tint: Color = Teal, sizeDp: Dp = 86.dp) {
    Box(modifier.size(sizeDp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(sizeDp)) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2 + 2.dp.toPx()
            drawArc(
                color = RingTrack,
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(this.size.width - inset * 2, this.size.height - inset * 2),
                style = DsStroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
            drawArc(
                color = tint,
                startAngle = 0f, sweepAngle = 360f * fraction.coerceIn(0.05f, 1f), useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(this.size.width - inset * 2, this.size.height - inset * 2),
                style = DsStroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
        }
    }
}

/** Big white card with the dashed timeline. */
@Composable
fun TimelineCard(ripples: List<RippleEntity>, onStartVerify: (RippleEntity) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text(
            "Thursday, Sept 19",
            style = TextStyle(fontSize = 12.sp),
            color = Secondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        ripples.forEachIndexed { i, r ->
            TimelineRow(r, isLast = i == ripples.lastIndex, onStartVerify = { onStartVerify(r) })
            if (i != ripples.lastIndex) Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
fun TimelineRow(r: RippleEntity, isLast: Boolean, onStartVerify: () -> Unit) {
    Row {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(34.dp)) {
            val done = r.status > 0
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
                androidx.compose.foundation.Canvas(
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
            Text(r.subtitle, style = TextStyle(fontSize = 12.sp), color = Color(0xFF4B5563))
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                when (r.status) {
                    1 -> { PillTag("Self Reported", TagBg, GrayTag); PillTag("+${r.points} pts", Orange, Color.White, bold = true) }
                    2 -> { PillTag("QR Reported", TagBg, GrayTag); PillTag("+${r.points} pts", Orange, Color.White, bold = true) }
                    else -> { PillTag("Self report", TagBg, GrayTag); PillTag("+${r.points} pts", Gray, Color(0xFFFFFDF7), bold = true) }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        when {
            r.art == "veg" -> RoundedThumb(R.drawable.veg)
            r.art == "balloon" -> RoundedThumb(R.drawable.balloon)
            r.status == 0 -> PendingSelfReportBox(onStartVerify)
        }
    }
}

@Composable
private fun RoundedThumb(res: Int) {
    androidx.compose.foundation.Image(
        painterResource(res),
        contentDescription = null,
        modifier = Modifier
            .size(width = 62.dp, height = 58.dp)
            .clip(RoundedCornerShape(6.dp)),
        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
    )
}

/** Dashed "+ Self Report" box on pending rows (p03/p68). */
@Composable
fun PendingSelfReportBox(onTap: () -> Unit) {
    Box(
        Modifier
            .noRippleClickable { onTap() }
            .size(width = 78.dp, height = 72.dp)
            .background(Mint, RoundedCornerShape(10.dp))
            .dashedBorder(1.5.dp, Teal, 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("+", color = Teal, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Self Report", color = Teal, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Dashed stroke modifier (PDF capture area + self-report box). */
fun Modifier.dashedBorder(strokeWidth: Dp, color: Color, cornerRadius: Dp): Modifier =
    this.drawBehind {
        val stroke = strokeWidth.toPx()
        val radius = cornerRadius.toPx()
        val path = Path().apply {
            addRoundRect(RoundRect(rect = Rect(0f, 0f, size.width, size.height), cornerRadius = CornerRadius(radius, radius)))
        }
        drawPath(
            path, color,
            style = DsStroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
        )
    }

/** Upcoming event card (white r16, emoji, going count, register state). */
@Composable
fun EventCard(ev: Event, onClick: () -> Unit) {
    var registered by remember { mutableStateOf(ev.registered) }
    Column(
        Modifier
            .width(158.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .noRippleClickable { onClick() }
            .padding(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(ev.emoji, fontSize = 22.sp)
            Text("${ev.going} going", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Secondary)
        }
        Spacer(Modifier.height(8.dp))
        Text(ev.name, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = Ink, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(ev.date, fontSize = 10.sp, color = Secondary)
        Spacer(Modifier.height(2.dp))
        Text("📍 ${ev.place}", fontSize = 10.sp, color = Secondary, maxLines = 1)
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (registered) Mint else Teal)
                .noRippleClickable { registered = !registered }
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
