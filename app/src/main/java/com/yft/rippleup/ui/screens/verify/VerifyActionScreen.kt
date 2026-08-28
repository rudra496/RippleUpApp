package com.yft.rippleup.ui.screens.verify

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.yft.rippleup.R
import com.yft.rippleup.ui.AppViewModel
import com.yft.rippleup.ui.PendingVerify
import com.yft.rippleup.ui.components.GradientButton
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.screens.home.dashedBorder
import com.yft.rippleup.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/** p11/p55 — teal countdown ring, running monster, capture area, submit. */
@Composable
fun VerifyActionScreen(
    vm: AppViewModel,
    pending: PendingVerify,
    onVerified: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var secondsLeft by remember { mutableIntStateOf(5 * 60) }
    var photoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var rejectMsg by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val photoFile = remember {
        File(context.cacheDir, "ripple_photos").apply { mkdirs() }
            .resolve("ripple_${System.currentTimeMillis()}.jpg")
    }
    val photoUriPending = remember {
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", photoFile)
    }
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { ok -> if (ok) photoUri = photoUriPending }

    // countdown
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BgMain)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            CircleIcon { Text("‹", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.noRippleClickable { onCancel() }) }
            Text("Verify Action", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Ink)
            CircleIcon { Text("✕", color = Ink, fontSize = 12.sp, modifier = Modifier.noRippleClickable { onCancel() }) }
        }
        Spacer(Modifier.height(16.dp))
        // timer ring
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(150.dp)) {
                val stroke = 9.dp.toPx()
                drawArc(Color.White, 0f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(
                    Teal, -90f, 360f * (secondsLeft / 300f), false,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(com.yft.rippleup.util.Fmt.mmss(secondsLeft), style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold), color = Ink)
                Text("remaining", fontSize = 10.sp, color = Secondary)
            }
        }
        Spacer(Modifier.height(10.dp))
        Image(
            painterResource(R.drawable.running),
            contentDescription = null,
            modifier = Modifier.height(110.dp),
        )
        Text("5 minute window to verify", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Secondary)
        Spacer(Modifier.height(12.dp))

        // action card
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CreamCard)
                .border(1.2.dp, OrangeLight, RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("✏️", fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(pending.title, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Ink, maxLines = 2)
                Text(
                    if (pending.viaQr) "Partner-verified action" else "Self-reported custom action",
                    style = TextStyle(fontSize = 12.sp), color = Secondary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("+${pending.points}", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Teal)
                Text("pts", fontSize = 10.sp, color = Secondary)
            }
        }
        Spacer(Modifier.height(14.dp))

        // capture area
        val captured = photoUri != null
        Box(
            Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (captured) Mint else Color(0xFFF0F6F4))
                .dashedBorder(1.5.dp, Teal, 16.dp)
                .noRippleClickable {
                    if (captured) photoUri = null else cameraLauncher.launch(photoUriPending)
                },
            contentAlignment = Alignment.Center,
        ) {
            if (captured) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val bmp = remember(photoUri) {
                        photoUri?.let { uri ->
                            runCatching {
                                val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }
                                context.contentResolver.openInputStream(uri)?.use {
                                    android.graphics.BitmapFactory.decodeStream(it, null, opts)
                                }
                            }.getOrNull()
                        }
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Captured photo",
                            modifier = Modifier
                                .size(width = 150.dp, height = 140.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Photo added ✓", color = Teal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Tap to remove", color = Body, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📷", fontSize = 34.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap to Capture your Ripple", color = Teal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Your action surroundings help us confirm the action",
                        color = Secondary, fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        if (rejectMsg.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(rejectMsg, color = DangerRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        GradientButton(
            label = if (captured) "Submit & Verify!" else "Add a photo to submit",
            enabled = captured && !submitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            submitting = true
            scope.launch {
                val rejection = vm.guardReject(pending.actionKey)
                if (rejection != null) {
                    rejectMsg = rejection
                    submitting = false
                } else {
                    vm.commitVerified(pending)
                    onVerified()
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Cancel", color = Secondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.noRippleClickable { onCancel() }.padding(bottom = 26.dp),
        )
    }
}

@Composable
private fun CircleIcon(content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Mint),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** p12/p49 — monster + "Verifying Action…" + animated dots (~2.5s). */
@Composable
fun VerifyingScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2500)
        onDone()
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(BgOnboarding)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            CircleIcon { Text("✕", color = Ink, fontSize = 12.sp) }
        }
        Spacer(Modifier.weight(0.9f))
        Image(painterResource(R.drawable.party), contentDescription = null, modifier = Modifier.height(220.dp))
        Spacer(Modifier.height(30.dp))
        Text("Verifying Action…", style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold), color = Ink)
        Spacer(Modifier.height(8.dp))
        Text("RippleUp is validating your action", style = TextStyle(fontSize = 14.sp), color = Secondary)
        Spacer(Modifier.height(22.dp))
        AnimatedDots()
        Spacer(Modifier.weight(1.4f))
    }
}

@Composable
private fun AnimatedDots() {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(350)
            step = (step + 1) % 3
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) { i ->
            Box(
                Modifier
                    .size(if (i == step) 14.dp else 11.dp)
                    .clip(CircleShape)
                    .background(if (i == step) Teal else Color(0xFFB9DED6))
            )
        }
    }
}

/**
 * p13/14/72 — ACTION VERIFIED! with confetti + pts card variants.
 * QR variant shows "@ThriftUp!" title +500 and the gift strip; self-report +50/+30/+20 etc.
 */
@Composable
fun VerifiedScreen(
    pending: PendingVerify,
    onContinue: () -> Unit,
) {
    val qr = pending.viaQr
    Column(
        Modifier
            .fillMaxSize()
            .background(BgMain),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.75f))
        Box {
            Image(painterResource(R.drawable.yay), contentDescription = null, modifier = Modifier.height(240.dp))
            Confetti()
        }
        Spacer(Modifier.height(26.dp))
        Text(
            if (qr) "Action Verified @ThriftUp!" else "Action Verified !",
            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
            color = Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (qr) "Your Action was successfully verified!"
            else "Your Self-Reported action was successfully verified",
            style = TextStyle(fontSize = 14.sp),
            color = Color(0xFF5E6B68),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 30.dp),
        )
        Spacer(Modifier.height(26.dp))
        // +pts card
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 42.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFE8F3F1))
                .padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "+ ${pending.points} pts",
                style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold),
                color = Teal,
            )
            Spacer(Modifier.height(4.dp))
            Text("Verified · Added to balance", fontSize = 12.sp, color = Secondary)
        }
        if (qr) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Mint)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🎁", fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                Text("Claim 10% off next purchase", color = PromoText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.weight(1f))
        GradientButton(
            "Continue  →",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) { onContinue() }
        Spacer(Modifier.height(30.dp))
    }
}

/** Lightweight falling confetti — PDF p13. */
@Composable
private fun Confetti() {
    val colors = listOf(Color(0xFFF472B6), Color(0xFF60A5FA), Color(0xFFF9D14C), Color(0xFF34D399), Color(0xFFFB923C), Color(0xFFA78BFA))
    var t by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (true) {
            t = ((System.currentTimeMillis() - start) % 2600f) / 2600f
            delay(33)
        }
    }
    Canvas(Modifier.size(300.dp)) {
        val n = 26
        repeat(n) { i ->
            val seed = i * 97.31f
            val x = ((seed * 13.7f) % 300f)
            val fall = ((t + (i % 7) / 7f) % 1f) * 300f
            val drift = kotlin.math.sin((t * 6.28f) + seed) * 14f
            drawRoundRect(
                color = colors[i % colors.size],
                topLeft = androidx.compose.ui.geometry.Offset((x + drift).toFloat(), fall),
                size = androidx.compose.ui.geometry.Size(9f, 5f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
            )
        }
    }
}
