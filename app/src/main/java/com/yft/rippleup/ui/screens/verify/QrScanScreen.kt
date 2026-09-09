package com.yft.rippleup.ui.screens.verify

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.theme.*
import com.yft.rippleup.util.GeoHelper
import com.yft.rippleup.util.QrPayload
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * p49 — Scan QR Code: real camera + MLKit; the payload must be a signed RippleUp
 * location QR. On detection: GPS fix -> geofence check -> cloud verification
 * (who + where) -> receipt.
 */
@Composable
fun QrScanScreen(
    vm: com.yft.rippleup.ui.AppViewModel,
    onVerified: (com.yft.rippleup.data.remote.CloudVerification?) -> Unit,
    onError: (String) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPermission = it }
    LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    var status by remember { mutableStateOf("Point the camera at a RippleUp QR") }
    var processing by remember { mutableStateOf(false) }
    var detected by remember { mutableStateOf(false) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF05070A))
    ) {
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A2F33))
                    .noRippleClickable { onClose() },
                contentAlignment = Alignment.Center,
            ) { Text("✕", color = Color.White, fontSize = 13.sp) }
            Text("Scan QR Code", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Color.White)
            Spacer(Modifier.size(40.dp))
        }
        Spacer(Modifier.height(18.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0A0E12)),
            contentAlignment = Alignment.Center,
        ) {
            if (hasPermission) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val providerFuture = ProcessCameraProvider.getInstance(ctx)
                        providerFuture.addListener({
                            val provider = providerFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            val scanner = BarcodeScanning.getClient()
                            analysis.setAnalyzer(executor) { proxy ->
                                if (detected || processing) { proxy.close(); return@setAnalyzer }
                                val media = proxy.image
                                if (media == null) { proxy.close(); return@setAnalyzer }
                                val img = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                                scanner.process(img)
                                    .addOnSuccessListener { codes ->
                                        val raw = codes.firstOrNull()?.rawValue
                                        if (raw != null && !detected && !processing) {
                                            detected = true
                                            processing = true
                                            status = "QR found — checking location…"
                                            scope.launch {
                                                val parsed = QrPayload.parse(raw)
                                                when {
                                                    parsed == null -> {
                                                        detected = false; processing = false
                                                        status = "Not a RippleUp QR — keep scanning"
                                                    }
                                                    !QrPayload.isValid(parsed) -> {
                                                        detected = false; processing = false
                                                        status = "Invalid QR signature — report this poster"
                                                    }
                                                    else -> {
                                                        val location = vm.cloud.fetchLocation(parsed.locationId)
                                                        if (location == null) {
                                                            detected = false; processing = false
                                                            status = "Unknown location — is the cloud configured?"
                                                        } else {
                                                            status = "Getting your GPS position and address…"
                                                            val fix = GeoHelper.fullFix(context)
                                                            val (ver, err) = vm.recordQrVerification(
                                                                location = location,
                                                                userLat = fix.lat,
                                                                userLng = fix.lng,
                                                                userAddress = fix.address,
                                                                accuracyM = fix.accuracyM,
                                                                device = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
                                                                title = "Partner action @ ${location.name}",
                                                                subtitle = "QR-verified at ${location.name}",
                                                                points = 500,
                                                                actionKey = "refill",
                                                                co2eGrams = 1200,
                                                            )
                                                            if (ver != null) onVerified(ver)
                                                            else {
                                                                detected = false; processing = false
                                                                status = err ?: "Verification failed — try again"
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .addOnCompleteListener { proxy.close() }
                            }
                            runCatching {
                                provider.unbindAll()
                                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    "Camera permission is needed to scan Ripple QR codes.",
                    color = Color(0xFF9FB3AE),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp),
                )
            }
            ViewfinderOverlay()
        }
        // live status strip
        Text(
            status,
            color = if (processing) Color(0xFF35D0C0) else Color(0xFF9FB3AE),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        )
    }
}

@Composable
private fun ViewfinderOverlay() {
    var lineT by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            lineT = (lineT + 0.012f) % 1f
            delay(16)
        }
    }
    Canvas(Modifier.size(240.dp)) {
        val bracket = 46.dp.toPx()
        val stroke = 4.dp.toPx()
        val w = size.width
        val h = size.height
        val c = Teal
        fun tl() = listOf(Offset(0f, bracket) to Offset(0f, stroke / 2), Offset(0f, 0f) to Offset(bracket, 0f))
        fun bl() = listOf(Offset(0f, h - bracket) to Offset(0f, h - stroke / 2), Offset(0f, h) to Offset(bracket, h))
        fun tr() = listOf(Offset(w - bracket, 0f) to Offset(w, 0f), Offset(w, stroke / 2) to Offset(w, bracket))
        fun br() = listOf(Offset(w - bracket, h) to Offset(w, h), Offset(w, h - bracket) to Offset(w, h - stroke / 2))
        listOf(tl(), bl(), tr(), br()).flatten().forEach { (a, b) ->
            drawLine(c, a, b, strokeWidth = stroke, cap = StrokeCap.Round)
        }
        val y = h * (0.25f + lineT * 0.5f)
        drawLine(
            Color(0xFF35D0C0),
            Offset(w * 0.08f, y), Offset(w * 0.92f, y),
            strokeWidth = 2.dp.toPx(),
        )
    }
}
