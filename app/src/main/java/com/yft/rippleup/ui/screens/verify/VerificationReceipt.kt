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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yft.rippleup.data.remote.VerificationReceipt as Receipt
import com.yft.rippleup.ui.components.CircleIconButton
import com.yft.rippleup.ui.components.MiniMap
import com.yft.rippleup.ui.components.PillTag
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.theme.*

/** The proof card Rudra Sir asked for: WHO scanned, WHICH place, and the MAP position. */
@Composable
fun VerificationReceiptScreen(
    receipt: Receipt? = null,
    receiptId: Long? = null,
    vm: com.yft.rippleup.ui.AppViewModel? = null,
    onClose: () -> Unit = {},
) {
    var loaded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Receipt?>(receipt) }
    androidx.compose.runtime.LaunchedEffect(receiptId) {
        if (loaded == null && receiptId != null && vm != null) {
            loaded = vm.cloud.fetchReceiptById(receiptId)
        }
    }
    val shown: Receipt? = loaded ?: receipt.takeIf { receiptId == null }
    Column(
        Modifier
            .fillMaxSize()
            .background(BgMain)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Verification Receipt", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = Ink)
            CircleIconButton(onClick = onClose) {
                Text("✕", color = Ink, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(14.dp))

        val receipt = shown
        if (receipt == null) {
            Text(if (vm != null) "Loading receipt…" else "Receipt unavailable.", color = Secondary, fontSize = 14.sp)
            return@Column
        }

        // status chip
        val (statusText, statusBg, statusFg) = when (receipt.status) {
            "approved" -> Triple("Approved ✓", Color(0xFFDFF5E7), GreenReady)
            "rejected" -> Triple("Rejected", Color(0xFFFDE8E8), DangerRed)
            "flagged" -> Triple("Flagged for review", Color(0xFFFDF4DC), Orange)
            else -> Triple("Pending admin review", Mint, Teal)
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(statusBg)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) { Text(statusText, color = statusFg, fontSize = 13.sp, fontWeight = FontWeight.Bold) }

        Spacer(Modifier.height(14.dp))

        // WHO
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(14.dp)
        ) {
            ReceiptRow("👤", "Scanned by", receipt.profiles?.full_name ?: "Member")
            Spacer(Modifier.height(8.dp))
            ReceiptRow("✉️", "Account", receipt.profiles?.email ?: "—")
            Spacer(Modifier.height(8.dp))
            ReceiptRow("💧", "Action", receipt.ripples?.title ?: "Partner action")
            Spacer(Modifier.height(8.dp))
            ReceiptRow("🏅", "Points", "+${receipt.ripples?.points ?: 0} pts (on approval)")
            Spacer(Modifier.height(8.dp))
            ReceiptRow("📱", "Device", receipt.device ?: "—")
        }
        Spacer(Modifier.height(12.dp))

        // WHERE
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(14.dp)
        ) {
            ReceiptRow("📍", "Place", receipt.partner_locations?.name ?: receipt.location_id ?: "—")
            Spacer(Modifier.height(8.dp))
            ReceiptRow("🏠", "Address", receipt.partner_locations?.address ?: "—")
            Spacer(Modifier.height(8.dp))
            ReceiptRow("⏰", "Scanned at", receipt.created_at?.take(16)?.replace('T', ' ') ?: "—")
            Spacer(Modifier.height(8.dp))
            val d = receipt.distance_m
            ReceiptRow("📏", "Distance", if (d != null) "${d} m from partner point" else "no GPS fix")
        }
        Spacer(Modifier.height(12.dp))

        // MAP (where exactly)
        if (receipt.user_lat != null && receipt.user_lng != null) {
            Text("Where it happened", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Ink)
            Spacer(Modifier.height(8.dp))
            if (!receipt.user_address.isNullOrBlank()) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(14.dp)) {
                    Text("USER SCANNED FROM", color = Teal, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Sp))
                    Spacer(Modifier.height(4.dp))
                    Text(receipt.user_address, style = TextStyle(fontSize = 13.sp, lineHeight = 19.sp), color = Ink)
                    receipt.accuracy_m?.let {
                        Text("GPS accuracy ±${it} m", color = Secondary, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            MiniMap(lat = receipt.user_lat, lng = receipt.user_lng, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text(
                "GPS %.5f, %.5f".format(receipt.user_lat, receipt.user_lng),
                style = TextStyle(fontSize = 11.sp), color = Secondary,
            )
        } else {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFFDF4DC)).padding(14.dp)
            ) {
                Text("No GPS fix was captured for this scan.", color = PromoText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Location permission and GPS help verify contributions.", color = PromoText, fontSize = 12.sp)
            }
        }
        if (!receipt.review_note.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFFDE8E8)).padding(14.dp)) {
                Text("Reviewer note", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = DangerRed)
                Text(receipt.review_note, color = Color(0xFF6B2B2B), fontSize = 13.sp)
            }
        }

        // PROOF PHOTOS
        if (receipt.photos.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Proof photos", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Ink)
            Spacer(Modifier.height(8.dp))
            receipt.photos.forEach { path ->
                com.yft.rippleup.ui.components.UrlImage(
                    url = com.yft.rippleup.data.remote.Config.verificationPhotoUrl(path),
                    contentDescription = "Proof photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    sample = 1,
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun ReceiptRow(emoji: String, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = 13.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(label, color = Secondary, fontSize = 12.sp, modifier = Modifier.width(78.dp))
        Text(value, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
}

/** Compact row used in the admin queue + My Verifications list. */
@Composable
fun VerificationRow(
    name: String,
    place: String,
    time: String,
    status: String,
    distance: Int?,
    address: String? = null,
    photoPath: String? = null,
    onClick: () -> Unit,
) {
    val (statusText, statusBg, statusFg) = when (status) {
        "approved" -> Triple("Approved", Color(0xFFDFF5E7), GreenReady)
        "rejected" -> Triple("Rejected", Color(0xFFFDE8E8), DangerRed)
        "flagged" -> Triple("Flagged", Color(0xFFFDF4DC), Orange)
        else -> Triple("Pending", Mint, Teal)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .noRippleClickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (photoPath != null) {
            com.yft.rippleup.ui.components.UrlImage(
                url = com.yft.rippleup.data.remote.Config.verificationPhotoUrl(photoPath),
                contentDescription = "Proof photo",
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(name, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Ink)
            Text("$place · $time", color = Secondary, fontSize = 11.sp)
            if (!address.isNullOrBlank()) Text(address, color = Color(0xFF4B5B57), fontSize = 11.sp, maxLines = 2)
            if (distance != null) Text("${distance} m from partner point", color = Teal, fontSize = 11.sp)
        }
        Spacer(Modifier.width(8.dp))
        PillTag(statusText, statusBg, statusFg, bold = true)
        Text("›", color = Color(0xFF9AA6A3), fontSize = 16.sp)
    }
}
