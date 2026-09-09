package com.yft.rippleup.ui.screens.verify

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yft.rippleup.data.remote.CloudLocation
import com.yft.rippleup.ui.components.MiniMap
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.theme.BgMain
import com.yft.rippleup.ui.theme.Ink
import com.yft.rippleup.ui.theme.Mint
import com.yft.rippleup.ui.theme.Secondary
import com.yft.rippleup.ui.theme.Teal
import kotlinx.coroutines.launch

/**
 * Desktop QR flow: phones scan the physical QR (with GPS geofence); on desktop
 * there is no camera/GPS, so pick the partner location you are at — the verification
 * still lands in the admin review queue, flagged as desktop-origin.
 */
@Composable
fun QrScanScreen(
    vm: com.yft.rippleup.ui.AppViewModel,
    onVerified: (com.yft.rippleup.data.remote.CloudVerification?) -> Unit,
    onError: (String) -> Unit,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var locations by remember { mutableStateOf<List<CloudLocation>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<CloudLocation?>(null) }

    LaunchedEffect(Unit) {
        locations = vm.cloud.fetchLocations()
        loading = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BgMain)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A2F33))
                    .noRippleClickable { onClose() },
                contentAlignment = Alignment.Center,
            ) { Text("X", color = Color.White, fontSize = 13.sp) }
            Text("Scan QR Code", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Ink)
            Spacer(Modifier.size(40.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Desktop verification — choose the partner location you are visiting.",
            style = TextStyle(fontSize = 13.sp, lineHeight = 19.sp), color = Secondary,
        )
        Spacer(Modifier.height(14.dp))

        if (loading) {
            Text("Loading partner locations…", color = Secondary, fontSize = 14.sp)
        } else if (locations.isEmpty()) {
            Text(
                "No partner locations found. Make sure the Supabase URL is configured and schema.sql was run.",
                color = Secondary, fontSize = 13.sp,
            )
        } else {
            locations.forEach { loc ->
                val isSel = selected?.id == loc.id
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSel) Mint else androidx.compose.ui.graphics.Color.White)
                        .noRippleClickable { selected = loc }
                        .padding(12.dp),
                ) {
                    Text("${loc.emoji ?: "P"}  ${loc.name}", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Ink)
                    Text(loc.address ?: "", color = Secondary, fontSize = 12.sp)
                    MiniMap(lat = loc.lat, lng = loc.lng, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (selected != null) Teal else Color(0xFFB7D6D0))
                    .noRippleClickable(enabled = selected != null) {
                        scope.launch {
                            message = "Submitting verification…"
                            val (ver, err) = vm.recordDesktopVerification(selected!!)
                            if (ver != null) onVerified(ver) else {
                                message = err ?: "Verification failed."
                                onError(message)
                            }
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Verify me at this location", color = androidx.compose.ui.graphics.Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            if (message.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(message, color = Secondary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}
