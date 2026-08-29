package com.yft.rippleup.ui.screens.profile

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yft.rippleup.resources.Res
import com.yft.rippleup.resources.drop
import com.yft.rippleup.resources.cal3
import com.yft.rippleup.resources.cal18
import com.yft.rippleup.resources.cal17
import com.yft.rippleup.resources.cal16
import com.yft.rippleup.resources.cal15
import com.yft.rippleup.resources.cal14
import com.yft.rippleup.resources.cal13
import com.yft.rippleup.resources.avatar
import org.jetbrains.compose.resources.painterResource
import com.yft.rippleup.ui.AppViewModel
import com.yft.rippleup.ui.components.CircleIconButton
import com.yft.rippleup.ui.components.RippleLogo
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.theme.*

/** p09/p40 — Profile with stats, Sept 2026 photo calendar and settings rows. */
@Composable
fun ProfileScreen(
    vm: AppViewModel,
    onOpenAbout: () -> Unit,
    onOpenNotifSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val stats by vm.stats.collectAsState()
    var showAbout by remember { mutableStateOf(false) }
    var showNotif by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp),
    ) {
        // mint hero
        Column(
            Modifier
                .fillMaxWidth()
                .background(BgOnboarding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RippleLogo(size = 42.dp)
                CircleIconButton(bg = White, onClick = {}, badge = true) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = Ink, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Box {
                Box(
                    Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .border(6.dp, Color(0xFFEAF6F2), CircleShape)
                ) {
                    Image(
                        painterResource(Res.drawable.avatar),
                        contentDescription = "Profile photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-6).dp, y = (-10).dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(OrangeLight),
                    contentAlignment = Alignment.Center,
                ) { Text("✎", fontSize = 13.sp, color = Color.White) }
            }
            Spacer(Modifier.height(12.dp))
            Text(vm.displayName, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = Ink)
            Text("Ripple Ambassador", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium), color = Secondary)
            Spacer(Modifier.height(16.dp))
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Ripple Points", com.yft.rippleup.util.Fmt.grouped(stats.points) + " RP", Modifier.weight(1f), sub = null)
                StatTile("CO²e Saved", com.yft.rippleup.util.Fmt.co2(stats.co2Kg), Modifier.weight(1f), sub = "Estimated")
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Current Streak", color = Teal, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("${stats.streak} Days", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            MonthCalendar()
            Spacer(Modifier.height(18.dp))
            Text("Settings", color = Teal, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            SettingRow(icon = { androidx.compose.foundation.Image(painterResource(Res.drawable.drop), contentDescription = null, modifier = Modifier.size(18.dp)) }, label = "About Ripple Up") { showAbout = true }
            SettingRow(icon = { Icon(Icons.Outlined.Notifications, null, tint = Teal, modifier = Modifier.size(18.dp)) }, label = "Notifications") { showNotif = true }
            SettingRow(icon = { Text("🎧", fontSize = 14.sp) }, label = "Help & Support") { showHelp = true }
            SettingRow(icon = { Icon(Icons.Outlined.Lock, null, tint = Teal, modifier = Modifier.size(18.dp)) }, label = "Privacy and Data") { showPrivacy = true }
        }
    }

    if (showAbout) AboutSheet { showAbout = false }
    if (showNotif) NotificationSettingsSheet { showNotif = false }
    if (showHelp) HelpSupportSheet { showHelp = false }
    if (showPrivacy) PrivacySheet { showPrivacy = false }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier, sub: String?) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = Teal, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        if (sub != null) Text(sub, color = Color(0xFFB9C4C1), fontSize = 8.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = Ink)
    }
}

/** Sept 2026 calendar with photo days + teal today (p09/p40). */
@Composable
fun MonthCalendar() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(14.dp),
    ) {
        Text("Sept 2026", color = Color(0xFF8F8F8F), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, color = Color(0xFF403C42), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(6.dp))
        val photoDays = mapOf(
            3 to Res.drawable.cal3, 13 to Res.drawable.cal13, 14 to Res.drawable.cal14,
            15 to Res.drawable.cal15, 16 to Res.drawable.cal16, 17 to Res.drawable.cal17, 18 to Res.drawable.cal18,
        )
        val weeks = listOf(
            listOf(1, 2, 3, 4, 5, 6, 7),
            listOf(8, 9, 10, 11, 12, 13, null),
            listOf(14, 15, 16, 17, 18, 19, 20),
            listOf(21, 22, 23, 24, 25, 26, 27),
            listOf(28, 29, 30, null, null, null, null),
        )
        weeks.forEach { week ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                week.forEach { day ->
                    Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                        when {
                            day == null -> {}
                            day == 19 -> Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Teal),
                                contentAlignment = Alignment.Center,
                            ) { Text("19", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            day in photoDays -> Image(
                                painterResource(photoDays[day]!!),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop,
                            )
                            else -> Text(
                                "$day",
                                color = Color(0xFF7FA39B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingRow(icon: @Composable () -> Unit, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .noRippleClickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Mint),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Spacer(Modifier.width(12.dp))
        Text(label, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium), color = LabelDark, modifier = Modifier.weight(1f))
        Text("›", color = Color(0xFF9AA6A3), fontSize = 18.sp)
    }
}
