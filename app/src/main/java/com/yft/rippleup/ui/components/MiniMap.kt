package com.yft.rippleup.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln

/**
 * Tiny static map from OpenStreetMap raster tiles (free, no API key).
 * 3x3 tile grid around the point + a teal marker pin. Offline-safe fallback.
 */
@Composable
fun MiniMap(
    lat: Double,
    lng: Double,
    modifier: Modifier = Modifier,
    zoom: Int = 15,
) {
    val tileUrls = remember(lat, lng, zoom) { osmTileUrls(lat, lng, zoom) }
    var tiles by remember { mutableStateOf<List<Bitmap>?>(null) }

    LaunchedEffect(tileUrls) {
        tiles = withContext(Dispatchers.IO) {
            runCatching {
                tileUrls.map { url ->
                    val con = URL(url).openConnection() as HttpURLConnection
                    con.connectTimeout = 8000
                    con.readTimeout = 8000
                    con.setRequestProperty("User-Agent", "RippleUp/5.1 (Android)")
                    BitmapFactory.decodeStream(con.inputStream).also { con.disconnect() }
                }
            }.getOrNull()
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
    ) {
        val loaded = tiles
        if (loaded != null && loaded.size == 9) {
            Column(Modifier.fillMaxSize()) {
                for (row in 0..2) {
                    Row(Modifier.weight(1f)) {
                        for (col in 0..2) {
                            androidx.compose.foundation.Image(
                                bitmap = loaded[row * 3 + col].asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.weight(1f).fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize().background(Color(0xFFE4EFEC)))
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = center
            drawCircle(Color(0x330D9488), radius = size.minDimension * 0.14f, center = c)
            drawCircle(Color(0xFF0D9488), radius = 22f, center = c)
            drawCircle(Color.White, radius = 9f, center = c)
        }
        Text(
            "© OpenStreetMap",
            style = androidx.compose.ui.text.TextStyle(fontSize = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp)),
            color = Color(0xFF5A6B67),
            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
        )
    }
}

/** WGS-84 -> OSM tile math; returns the 3x3 grid of tile URLs centred on (lat, lng). */
private fun osmTileUrls(lat: Double, lng: Double, zoom: Int): List<String> {
    val n = Math.pow(2.0, zoom.toDouble()).toInt()
    val xf = (lng + 180.0) / 360.0 * n
    val latRad = Math.toRadians(lat)
    val yf = (1.0 - ln(cos(latRad)) / PI) / 2.0 * n
    val x = xf.toInt().coerceIn(0, n - 1)
    val y = yf.toInt().coerceIn(0, n - 1)
    val urls = mutableListOf<String>()
    for (dy in -1..1) {
        for (dx in -1..1) {
            val tx = ((x + dx) % n + n) % n
            val ty = (y + dy).coerceIn(0, n - 1)
            urls.add("https://tile.openstreetmap.org/$zoom/$tx/$ty.png")
        }
    }
    return urls
}
