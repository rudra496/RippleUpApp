package com.yft.rippleup.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Framework GPS/network location (no Play Services dependency).
 * Tries last-known fixes first, then requests one fresh fix with a 10s budget.
 */
object GeoHelper {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    suspend fun currentLocation(context: Context): Location? {
        if (!hasPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        // 1) recent cached fix (< 2 min old)
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        val now = System.currentTimeMillis()
        providers.mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            .filter { now - it.time < 120_000 }
            .maxByOrNull { it.time }
            ?.let { return it }

        // 2) fresh fix with timeout
        return withTimeoutOrNull(10_000) {
            suspendCancellableCoroutine { cont ->
                val listener = LocationListener { loc -> if (cont.isActive) cont.resume(loc) }
                var started = false
                for (p in providers) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    ) continue
                    runCatching {
                        lm.requestLocationUpdates(p, 0L, 0f, listener, Looper.getMainLooper())
                        started = true
                    }
                }
                if (!started) cont.resume(null)
                cont.invokeOnCancellation { lm.removeUpdates(listener) }
            }
        }
    }
}
