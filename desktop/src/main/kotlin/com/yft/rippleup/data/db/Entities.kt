package com.yft.rippleup.data.db

/** Plain data classes — the desktop build persists these via JSON (see RippleStore). */

data class UserEntity(
    val email: String,                 // "admin" allowed for the test account
    val firstName: String,
    val lastName: String,
    val passwordHash: String,          // salted SHA-256, never plaintext
    val createdAt: Long,
)

/** One timeline entry on the Home "Today's Ripples list". */
data class RippleEntity(
    val id: Long = 0,
    val userEmail: String,
    val title: String,
    val subtitle: String,
    val points: Int,
    val co2eKg: Float,
    val actionKey: String,             // Guard cooldown key
    val status: Int,                   // 0 pending, 1 done-self, 2 done-qr
    val art: String,                   // art key: veg|balloon|none
    val createdAt: Long,
    val demo: Boolean = false,         // seeded demo rows don't count toward caps
    val tamperTag: String = "",        // HMAC over the tamper-relevant fields
)

data class PrefEntity(
    val key: String,
    val value: String,
)
