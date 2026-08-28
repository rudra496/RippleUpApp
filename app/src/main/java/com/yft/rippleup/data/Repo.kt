package com.yft.rippleup.data

import com.yft.rippleup.data.db.PrefEntity
import com.yft.rippleup.data.db.RippleEntity
import com.yft.rippleup.data.db.UserEntity
import com.yft.rippleup.util.Guard
import java.security.MessageDigest

/** Small static helpers around the Room DAOs. */
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

    fun pref(key: String, value: String) = PrefEntity(key, value)

    fun tamperTag(r: RippleEntity): String =
        Guard.tag(r.userEmail, r.title, r.points, r.status, r.createdAt)

    fun verifyTag(r: RippleEntity): Boolean = Guard.verify(r.tamperTag, r.userEmail, r.title, r.points, r.status, r.createdAt)

    suspend fun seedIfEmpty(
        userDao: com.yft.rippleup.data.db.UserDao,
        rippleDao: com.yft.rippleup.data.db.RippleDao,
        prefDao: com.yft.rippleup.data.db.PrefDao,
    ) {
        if (userDao.count() > 0) return
        userDao.insert(
            UserEntity(
                email = TEST_USER,
                firstName = TEST_FIRST,
                lastName = TEST_LAST,
                passwordHash = hash(TEST_USER, TEST_PASS),
                createdAt = System.currentTimeMillis(),
            )
        )
        val now = System.currentTimeMillis()
        fun ripple(title: String, subtitle: String, pts: Int, kg: Float, key: String, status: Int, art: String) =
            RippleEntity(
                userEmail = TEST_USER, title = title, subtitle = subtitle, points = pts,
                co2eKg = kg, actionKey = key, status = status, art = art,
                createdAt = now, demo = true, tamperTag = "",
            ).let { it.copy(tamperTag = tamperTag(it)) }

        // Home timeline exactly as in the PDF (p03):
        // done +20 self (photo), done +340 QR (balloon monster), pending +30 (self-report box)
        rippleDao.insert(ripple("Bring your reusables", "Carried your jute grocery bag today!", 20, 0.05f, "refill", 1, "veg"))
        rippleDao.insert(ripple("Donated clothes @ Thrifty", "Contributing to fabric circularity!", 340, 1.8f, "donate", 2, "balloon"))
        rippleDao.insert(ripple("Weekly meal prep", "Reduce food waste!", 30, 0.4f, "food", 0, "none"))
    }

    /** Display baseline so the demo account matches the PDF numbers on first launch. */
    const val BASE_POINTS = 12_090          // + seeded 360 earned = 12,450 RP
    const val BASE_CO2_KG = 17.9f           // + seeded 2.25 kg = ~18.6 kg CO₂e (rounded)
    const val DEMO_STREAK_DAYS = 6
    const val DEMO_LONGEST_DAYS = 14
}
