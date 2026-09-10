package com.yft.rippleup.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yft.rippleup.data.remote.VerificationReceipt
import com.yft.rippleup.ui.screens.verify.VerificationReceiptScreen
import com.yft.rippleup.ui.screens.verify.VerificationRow
import com.yft.rippleup.ui.theme.BgMain
import com.yft.rippleup.ui.theme.Ink
import com.yft.rippleup.ui.theme.Secondary

/** Profile -> My Verifications: every scan receipt (who/where/map) for the member. */
@Composable
fun MyVerificationsScreen(vm: com.yft.rippleup.ui.AppViewModel, onBack: () -> Unit) {
    var receipts by remember { mutableStateOf<List<VerificationReceipt>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var open by remember { mutableStateOf<VerificationReceipt?>(null) }

    LaunchedEffect(Unit) {
        receipts = vm.cloud.fetchMyVerifications()
        loading = false
    }

    if (open != null) {
        VerificationReceiptScreen(receipt = open, onClose = { open = null })
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BgMain)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("My Verifications", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = Ink)
        Text("Who scanned, from where — your contribution proofs.", color = Secondary, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        if (loading) {
            Text("Loading…", color = Secondary, fontSize = 14.sp)
        } else if (receipts.isEmpty()) {
            Text("No verifications yet — scan a partner QR code to earn your first proof.", color = Secondary, fontSize = 14.sp)
        } else {
            LazyColumn {
                items(receipts, key = { it.id }) { r ->
                    VerificationRow(
                        name = r.ripples?.title ?: "Partner action",
                        place = r.partner_locations?.name ?: r.location_id ?: "—",
                        time = r.created_at?.take(16)?.replace('T', ' ') ?: "",
                        status = r.status,
                        distance = r.distance_m,
                        address = r.user_address,
                        onClick = { open = r },
                    )
                }
            }
        }
    }
}
