package com.yft.rippleup.util

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Client-side guard rails for action logging (same rules as the Android build):
 *  - per-action cooldowns
 *  - daily rewarded-action cap + daily points cap
 *  - HMAC-SHA256 integrity tag over persisted stats (tamper detection)
 */
object Guard {

    val COOLDOWN_MS: Map<String, Long> = mapOf(
        "refill" to 45L * 60_000,
        "recycle" to 30L * 60_000,
        "food" to 90L * 60_000,
        "transit" to 120L * 60_000,
        "custom" to 45L * 60_000,
        "compost" to 90L * 60_000,
        "donate" to 120L * 60_000,
        "cleanup" to 120L * 60_000,
    )

    const val DAILY_ACTION_CAP = 12
    const val DAILY_POINTS_CAP = 300

    sealed class Verdict {
        data object Allowed : Verdict()
        data class Cooldown(val remainingMs: Long) : Verdict()
        data object DailyActionCap : Verdict()
        data class DailyPointsCap(val earnedToday: Int) : Verdict()
    }

    fun check(
        actionKey: String,
        lastActionAtMs: Long,
        actionsToday: Int,
        pointsToday: Int,
        nowMs: Long = System.currentTimeMillis(),
    ): Verdict {
        if (actionsToday >= DAILY_ACTION_CAP) return Verdict.DailyActionCap
        if (pointsToday >= DAILY_POINTS_CAP) return Verdict.DailyPointsCap(pointsToday)
        val cooldown = COOLDOWN_MS[actionKey] ?: return Verdict.Allowed
        val elapsed = nowMs - lastActionAtMs
        return if (lastActionAtMs <= 0 || elapsed >= cooldown) Verdict.Allowed
        else Verdict.Cooldown(cooldown - elapsed)
    }

    fun rejectionMessage(verdict: Verdict): String = when (verdict) {
        is Verdict.Cooldown ->
            "Slow down! You can log this action again in ${formatRemaining(verdict.remainingMs)}."
        Verdict.DailyActionCap ->
            "Daily limit reached — you've logged 12 ripples today. Come back tomorrow!"
        is Verdict.DailyPointsCap ->
            "Daily point cap of ${DAILY_POINTS_CAP} pts reached (earned today: ${verdict.earnedToday}). Rest up!"
        Verdict.Allowed -> ""
    }

    fun formatRemaining(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return when {
            h > 0 -> "${h}h ${m}m"
            m > 0 -> "${m}m ${s}s"
            else -> "${s}s"
        }
    }

    // --- Tamper-evident storage -------------------------------------------------

    private val secret: ByteArray by lazy {
        val host = runCatching { System.getProperty("os.name") + "|" + System.getProperty("user.name") }
            .getOrDefault("desktop")
        ("ripplup::" + host.reversed() + "::" + longSeed()).toByteArray()
    }

    private fun longSeed(): String = java.lang.Long.toString(0x524950504C5550L, 36) // "ripplup" magic

    fun tag(vararg fields: Any?): String = runCatching {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret, "HmacSHA256"))
        val payload = fields.joinToString("|") { it.toString() }
        mac.doFinal(payload.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(40)
    }.getOrDefault("")

    fun verify(tag: String?, vararg fields: Any?): Boolean =
        !tag.isNullOrBlank() && tag == Guard.tag(*fields)
}
