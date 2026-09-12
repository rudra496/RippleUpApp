package com.yft.rippleup.ui.screens.admin

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yft.rippleup.data.remote.CloudProfile
import com.yft.rippleup.data.remote.VerificationReceipt
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.screens.verify.VerificationReceiptScreen
import com.yft.rippleup.ui.screens.verify.VerificationRow
import com.yft.rippleup.ui.theme.*
import kotlinx.coroutines.launch

/**
 * ADMIN — manual contribution review queue.
 * Every QR scan (and its who/where proof) lands here; admins approve or reject,
 * which runs the cloud pipelines (points + notifications + badges).
 */
@Composable
fun AdminScreen(vm: com.yft.rippleup.ui.AppViewModel, onBack: () -> Unit) {
    val cloud = vm.cloud
    val scope = rememberCoroutineScope()
    var receipts by remember { mutableStateOf<List<VerificationReceipt>>(emptyList()) }
    var users by remember { mutableStateOf<List<CloudProfile>>(emptyList()) }
    var tab by remember { mutableStateOf(0) }   // 0 verifications, 1 users
    var loading by remember { mutableStateOf(true) }
    var open by remember { mutableStateOf<VerificationReceipt?>(null) }
    var message by remember { mutableStateOf("") }

    fun refresh() {
        scope.launch {
            loading = true
            receipts = cloud.fetchAllVerifications()
            users = cloud.fetchPendingProfiles()
            loading = false
        }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) { refresh() }

    if (open != null) {
        VerificationReceiptScreen(receipt = open, onClose = { open = null; refresh() })
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BgMain)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Admin Review", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = Ink)
            Spacer(Modifier.weight(1f))
            Text(
                "Refresh",
                color = Teal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.noRippleClickable { refresh() },
            )
        }
        Text(
            "Verify contributions manually — approve to award points + badges.",
            color = Secondary, fontSize = 12.sp,
        )
        if (message.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(message, color = DangerRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Verifications", "New users").forEachIndexed { i, label ->
                val active = tab == i
                Box(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) Teal else Color(0xFFE9E9E9))
                        .noRippleClickable { tab = i }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) { Text(label, color = if (active) Color.White else Secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(12.dp))
        // promote by email — owner feature
        var promoteEmail by remember { mutableStateOf("") }
        var promoteMsg by remember { mutableStateOf("") }
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = promoteEmail,
                onValueChange = { promoteEmail = it },
                placeholder = { Text("gmail of new admin", fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Orange)
                    .noRippleClickable {
                        if (promoteEmail.contains("@")) {
                            scope.launch {
                                promoteMsg = vm.cloud.setAdminByEmail(promoteEmail, true)
                                    ?: ("Admin added: " + promoteEmail.trim())
                            }
                        } else promoteMsg = "Enter a valid email"
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) { Text("Make admin", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
        if (promoteMsg.isNotBlank()) {
            Text(promoteMsg, color = Teal, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
        }

        if (loading) {
            Text("Loading verifications…", color = Secondary, fontSize = 14.sp)
            return@Column
        }
        if (tab == 1) {
            val pendingUsers = users.sortedBy { if (it.approval_status == "rejected") 1 else 0 }
            if (pendingUsers.isEmpty()) {
                Text("No accounts awaiting verification.", color = Secondary, fontSize = 14.sp)
            } else LazyColumn {
                items(pendingUsers, key = { it.id }) { u ->
                    var busyU by remember(u.id) { mutableStateOf(false) }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(u.full_name ?: "Member", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Ink)
                            Text(u.email ?: "", color = Secondary, fontSize = 11.sp)
                            Text(
                                when (u.approval_status) {
                                    "pending" -> "Awaiting verification"
                                    "rejected" -> "Rejected"
                                    else -> u.approval_status
                                },
                                color = Orange, fontSize = 11.sp,
                            )
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Teal)
                                .noRippleClickable(enabled = !busyU) {
                                    busyU = true
                                    scope.launch {
                                        vm.setUserApproval(u.id, "approved")
                                        users = users.filterNot { it.id == u.id }
                                        busyU = false
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) { Text("Approve", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFDE8E8))
                                .noRippleClickable(enabled = !busyU) {
                                    busyU = true
                                    scope.launch {
                                        vm.setUserApproval(u.id, "rejected")
                                        users = users.filterNot { it.id == u.id }
                                        busyU = false
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) { Text("Reject", color = DangerRed, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            return@Column
        }

        if (receipts.isEmpty()) {
            Text("No verifications yet. Scan a partner QR to create one.", color = Secondary, fontSize = 14.sp)
            return@Column
        }

        val ordered = receipts.sortedBy { if (it.status == "pending" || it.status == "flagged") 0 else 1 }
        LazyColumn {
            items(ordered, key = { it.id }) { r ->
                var busy by remember(r.id) { mutableStateOf(false) }
                Column {
                    VerificationRow(
                        name = r.profiles?.full_name ?: r.profiles?.email ?: "Member",
                        place = r.partner_locations?.name ?: r.location_id ?: "—",
                        time = r.created_at?.take(16)?.replace('T', ' ') ?: "",
                        status = r.status,
                        distance = r.distance_m,
                        address = r.user_address,
                        photoPath = r.photos.firstOrNull(),
                        onClick = { open = r },
                    )
                    if (r.status == "pending" || r.status == "flagged") {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ActionPill("Approve", Teal, Color.White, busy) {
                                scope.launch {
                                    val err = cloud.approveVerification(r.id) { }
                                    if (err == null) {
                                        receipts = receipts.map { if (it.id == r.id) it.copy(status = "approved") else it }
                                        message = ""
                                    } else message = err
                                    busy = false
                                }
                            }
                            ActionPill("Reject", Color(0xFFFDE8E8), DangerRed, busy) {
                                scope.launch {
                                    val err = cloud.rejectVerification(r.id, "Location or details unclear")
                                    if (err == null) {
                                        receipts = receipts.map { if (it.id == r.id) it.copy(status = "rejected") else it }
                                        message = ""
                                    } else message = err
                                    busy = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionPill(label: String, bg: Color, fg: Color, busy: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .noRippleClickable(enabled = !busy) { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) { Text(if (busy) "…" else label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}
