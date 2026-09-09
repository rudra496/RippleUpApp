package com.yft.rippleup.util

import com.yft.rippleup.data.remote.Config
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Location QR payload: https://rippleup.app/v/<locId>?s=<hmac16> */
object QrPayload {

    data class Parsed(val locationId: String, val sig: String)

    fun sigFor(locationId: String, secret: String = Config.QR_SECRET): String =
        hmacSha256(secret, locationId).joinToString("") { "%02x".format(it) }.take(16)

    fun parse(raw: String): Parsed? = runCatching {
        val marker = "/v/"
        val idx = raw.indexOf(marker)
        if (idx < 0) return null
        val rest = raw.substring(idx + marker.length)
        val locId = rest.substringBefore("?")
        val sig = rest.substringAfter("?s=", "").substringBefore("&")
        if (locId.isBlank() || sig.isBlank()) null else Parsed(locId, sig)
    }.getOrNull()

    fun isValid(p: Parsed): Boolean = p.sig == sigFor(p.locationId)

    /** Distance in metres between two coordinates (haversine). */
    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Int {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        return (r * 2 * atan2(sqrt(a), sqrt(1 - a))).toInt()
    }

    private fun hmacSha256(key: String, data: String): ByteArray =
        Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
            doFinal(data.toByteArray())
        }
}

/** Geofence verdict for a scan. */
sealed class GeoVerdict {
    data class Pass(val distanceM: Int) : GeoVerdict()
    data class OutOfRange(val distanceM: Int) : GeoVerdict()
    data object NoLocation : GeoVerdict()
}

fun evaluateGeofence(userLat: Double?, userLng: Double?, locLat: Double, locLng: Double, radiusM: Int): GeoVerdict =
    if (userLat == null || userLng == null) GeoVerdict.NoLocation
    else {
        val d = QrPayload.distanceMeters(userLat, userLng, locLat, locLng)
        if (d <= radiusM.coerceAtLeast(50)) GeoVerdict.Pass(d) else GeoVerdict.OutOfRange(d)
    }
