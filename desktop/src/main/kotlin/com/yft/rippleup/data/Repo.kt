package com.yft.rippleup.data

import java.security.MessageDigest

/** Static helpers shared with the Android build (auth hashing + demo constants). */
object Repo {

    fun hash(email: String, password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val salted = "ripplup::$email::$password"
        return md.digest(salted.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    const val TEST_USER = "admin"
    const val TEST_PASS = "rudra"
    const val TEST_FIRST = "Saara"
    const val TEST_LAST = "Rodriguez"

    /** Display baseline so the demo account matches the PDF numbers on first launch. */
    const val BASE_POINTS = 12_090          // + seeded 360 earned = 12,450 RP
    const val BASE_CO2_KG = 17.9f           // + seeded 2.25 kg = ~18.6 kg CO₂e (rounded)
    const val DEMO_STREAK_DAYS = 6
    const val DEMO_LONGEST_DAYS = 14
}
