package com.yft.rippleup.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.yft.rippleup.data.Content
import com.yft.rippleup.ui.components.GradientButton
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.screens.home.SheetScaffold
import com.yft.rippleup.ui.theme.*

/** p46 — About Ripple Up sheet. */
@Composable
fun AboutSheet(onClose: () -> Unit) {
    SheetScaffold(title = "About Ripple Up", onClose = onClose) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            // wordmark: text + drop over the p
            Box {
                Text(
                    "RippleUp",
                    style = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
                    color = TealDeep,
                )
                Image(
                    painterResource(R.drawable.drop),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = (-6).dp)
                        .size(22.dp),
                )
            }
            Text("RippleUp", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = Ink)
            Text("Version 1.0.0 Beta", color = Secondary, fontSize = 14.sp)
            Spacer(Modifier.height(18.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE9F6F1))
                    .padding(16.dp),
            ) {
                Text(
                    "Making sustainability fun, social, and rewarding for the next generation. Every ripple counts !",
                    style = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
                    color = Secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AboutStat("12,450", "Actions logged", Modifier.weight(1f))
                AboutStat("2,340", "Users this month", Modifier.weight(1f))
                AboutStat("48 kg", "Plastic avoided", Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0x14000000), RoundedCornerShape(16.dp)),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .noRippleClickable { }
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Terms of Service", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Ink)
                    Text("⧉", color = Secondary, fontSize = 14.sp)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0F000000)))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .noRippleClickable { }
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Privacy Policy", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Ink)
                    Text("⧉", color = Secondary, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(22.dp))
            GradientButton("⭐  Rate RippleUp on the App Store", modifier = Modifier.fillMaxWidth()) { }
            Spacer(Modifier.height(14.dp))
            Text("RippleUp Team · © 2026", color = Secondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.AboutStat(value: String, label: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0x14000000), RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = Ink)
        Text(label, color = Secondary, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

/** p45 — Notification settings sheet. */
@Composable
fun NotificationSettingsSheet(onClose: () -> Unit) {
    val types = remember {
        mutableStateOf(
            listOf(
                Triple("Action Reminders", "Daily nudges to complete your Ripple list", true),
                Triple("Streak Alerts", "Before midnight if your streak is at risk", true),
                Triple("Challenge Updates", "New challenges and progress milestones", true),
                Triple("Community Activity", "Friends and leaderboard changes", false),
                Triple("Partner Offers", "Deals and reward unlocks from partners", true),
            )
        )
    }
    var push by remember { mutableStateOf(true) }
    SheetScaffold(title = "Notifications", onClose = onClose) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 30.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE4F2EE))
                    .border(1.dp, Color(0x260D9488), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Push Notifications", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = Ink)
                    Text("Allow RippleUp to send notifications", color = Secondary, fontSize = 12.sp)
                }
                Toggle(push) { push = it }
            }
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0x14000000), RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Text("NOTIFICATION TYPES", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(6.dp))
                types.value.forEachIndexed { i, (title, sub, on) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(title, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = Ink)
                            Text(sub, color = Secondary, fontSize = 12.sp)
                        }
                        Toggle(on) { nv ->
                            types.value = types.value.toMutableList().also {
                                it[i] = Triple(title, sub, nv)
                            }
                        }
                    }
                    if (i != types.value.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0F000000)))
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0x14000000), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Quiet Hours", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = Ink)
                    Text("No notifications during this window", color = Secondary, fontSize = 12.sp)
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Mint)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) { Text("10 PM – 8 AM", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
fun Toggle(on: Boolean, onChange: (Boolean) -> Unit) {
    Box(
        Modifier
            .width(46.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (on) Teal else Color(0xFFE2E8E6))
            .noRippleClickable { onChange(!on) }
            .padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color.White)
        )
    }
}

/** p43 — Help & Support sheet with FAQ accordions. */
@Composable
fun HelpSupportSheet(onClose: () -> Unit) {
    var openIdx by remember { mutableStateOf(0) }
    SheetScaffold(title = "Help & Support", onClose = onClose) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 30.dp),
        ) {
            Text("Find answers or reach our team directly.", color = Secondary, fontSize = 14.sp)
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0x14000000), RoundedCornerShape(16.dp)),
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text("FREQUENTLY ASKED", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                }
                Content.faqs.forEachIndexed { i, faq ->
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0F000000)))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .noRippleClickable { openIdx = if (openIdx == i) -1 else i }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(faq.q, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = Ink, modifier = Modifier.weight(1f))
                            Text(if (openIdx == i) "⌄" else "›", color = Color(0xFF7C8B88), fontSize = 14.sp)
                        }
                        if (openIdx == i) {
                            Spacer(Modifier.height(8.dp))
                            Text(faq.a, style = TextStyle(fontSize = 13.sp, lineHeight = 20.sp), color = Color(0xFF5E6B68))
                        }
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
            GradientButton("?  Chat with Support", modifier = Modifier.fillMaxWidth()) { }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0x14000000), RoundedCornerShape(24.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("ⓘ  Report Bug", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0x14000000), RoundedCornerShape(24.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("☆  Send Feedback", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

/** p42 — Privacy and Data sheet. */
@Composable
fun PrivacySheet(onClose: () -> Unit) {
    var personalised by remember { mutableStateOf(true) }
    var anonData by remember { mutableStateOf(true) }
    SheetScaffold(title = "Privacy and Data", onClose = onClose) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 30.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0x14000000), RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Text("DATA & PERSONALIZATION", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Personalized Recommendations", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = Ink)
                        Text("Tailor action suggestions to your habits", color = Secondary, fontSize = 12.sp)
                    }
                    Toggle(personalised) { personalised = it }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0F000000)))
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Share Anonymous Usage Data", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = Ink)
                        Text("Helps improve the app, no personal info shared", color = Secondary, fontSize = 12.sp)
                    }
                    Toggle(anonData) { anonData = it }
                }
            }
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0x14000000), RoundedCornerShape(16.dp)),
            ) {
                Box(Modifier.fillMaxWidth().background(Color(0xFFE9F6F1)).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("CONSENT", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                }
                ConsentRow("Location Access", "Used only when scanning QR at partner locations")
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0F000000)))
                ConsentRow("Camera Access", "Used for QR scanning and action photo verification")
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0F000000)))
                ConsentRow("Notification Permission", "Managed in your Notifications settings above")
            }
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DangerBg)
                    .padding(16.dp),
            ) {
                Text("DANGER ZONE", color = Color(0xFFE4796B), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .noRippleClickable { }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🗑", fontSize = 16.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Delete Account", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = DangerText)
                        Text("Permanently removes all your data · coming soon", color = Color(0xFFE08A80), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsentRow(title: String, sub: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = Ink)
            Text(sub, color = Teal, fontSize = 12.sp)
        }
        Text("›", color = Color(0xFF9AA6A3), fontSize = 16.sp)
    }
}
