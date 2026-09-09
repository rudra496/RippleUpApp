package com.yft.rippleup.ui

import com.yft.rippleup.data.Repo
import com.yft.rippleup.data.RippleStore
import com.yft.rippleup.data.db.RippleEntity
import com.yft.rippleup.data.db.UserEntity
import com.yft.rippleup.data.remote.CloudLocation
import com.yft.rippleup.data.remote.CloudRipple
import com.yft.rippleup.data.remote.CloudSync
import com.yft.rippleup.data.remote.CloudVerification
import com.yft.rippleup.data.remote.Config
import com.yft.rippleup.data.remote.SessionManager
import com.yft.rippleup.util.Guard
import com.yft.rippleup.util.dayStartMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.prefs.Preferences

data class Session(
    val email: String,
    val firstName: String,
    val lastName: String,
    val cloudId: String? = null,
    val isAdmin: Boolean = false,
)

data class Stats(
    val points: Int = 0,
    val co2Kg: Float = 0f,
    val streak: Int = Repo.DEMO_STREAK_DAYS,
    val longest: Int = Repo.DEMO_LONGEST_DAYS,
    val pointsPill: Int = 208,
)

data class PendingVerify(
    val title: String,
    val subtitle: String,
    val points: Int,
    val actionKey: String,
    val co2eKg: Float,
    val viaQr: Boolean,
)

/** Desktop model: cloud-first auth + local JSON cache (mirrors the Android VM). */
class AppViewModel(private val store: RippleStore) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val prefs: Preferences = Preferences.userRoot().node("rippleup")
    val sessions = SessionManager()
    val cloud = CloudSync(sessions)

    private val sessionInternal = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = sessionInternal

    private val ripplesInternal = MutableStateFlow<List<RippleEntity>>(emptyList())
    val ripples: StateFlow<List<RippleEntity>> = ripplesInternal

    private val statsInternal = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> = statsInternal

    init {
        scope.launch {
            if (Config.cloudConfigured && sessions.hasSession && sessions.refreshIfNeeded()) {
                resumeCloudSession()
            }
            kotlinx.coroutines.flow.combine(store.state, sessionInternal) { s, ses -> s to ses }
                .collect { (s, ses) ->
                    val email = ses?.email
                    val mine = s.ripples.filter { email != null && it.userEmail == email }.map { store.entityOf(it) }
                    ripplesInternal.value = mine.sortedBy { it.createdAt }
                    val earned = mine.filter { it.status > 0 }.sumOf { it.points }
                    val co2 = mine.filter { it.status > 0 }.sumOf { it.co2eKg.toDouble() }.toFloat()
                    val runtimeEarned = (earned - 360).coerceAtLeast(0)
                    statsInternal.value = Stats(
                        points = Repo.BASE_POINTS + earned,
                        co2Kg = Repo.BASE_CO2_KG + co2,
                        pointsPill = 208 + runtimeEarned,
                    )
                }
        }
    }

    val displayName: String
        get() = session.value?.let { "${it.firstName} ${it.lastName}".trim() } ?: "Saara Rodriguez"

    fun computeStart(): String {
        if (sessionInternal.value != null) return "home"
        return if (prefs.getBoolean("onboarded", false)) "auth" else "splash"
    }

    fun markOnboarded() {
        prefs.putBoolean("onboarded", true)
    }

    private suspend fun resumeCloudSession() {
        val profile = cloud.fetchProfile() ?: return
        sessionInternal.value = Session(
            email = profile.email ?: "cloud-user",
            firstName = (profile.full_name ?: "Ripple").toString().substringBefore(" ").replaceFirstChar { it.uppercase() },
            lastName = "",
            cloudId = profile.id,
            isAdmin = profile.is_admin,
        )
        pullCloudRipples()
    }

    private suspend fun activateCloudSession(authEmail: String, cloudUserId: String?, name: String?) {
        val profile = cloud.fetchProfile()
        sessionInternal.value = Session(
            email = profile?.email ?: authEmail,
            firstName = (name ?: profile?.full_name ?: authEmail).toString().substringBefore(" ").substringBefore("@")
                .replaceFirstChar { it.uppercase() },
            lastName = "",
            cloudId = cloudUserId ?: profile?.id,
            isAdmin = profile?.is_admin ?: false,
        )
        pullCloudRipples()
    }

    fun login(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        scope.launch {
            if (Config.cloudConfigured) {
                val (auth, err) = cloud.loginWithPassword(email.trim().lowercase(), password)
                if (auth?.access_token != null) {
                    activateCloudSession(auth.user?.email ?: email, auth.user?.id, auth.user?.user_metadata?.get("full_name").toString().trim('"'))
                    onResult(true, "")
                    return@launch
                }
                if (err != null && !err.contains("Invalid login", true)) {
                    onResult(false, err)
                    return@launch
                }
            }
            val clean = email.trim().lowercase()
            val user = runCatching { store.findUser(clean) }.getOrNull()
            when {
                user == null -> onResult(false, "No account found for that email.")
                user.passwordHash != Repo.hash(user.email, password) ->
                    onResult(false, "Incorrect password. Try again.")
                else -> {
                    sessionInternal.value = Session(user.email, user.firstName, user.lastName)
                    onResult(true, "")
                }
            }
        }
    }

    fun signUp(first: String, last: String, email: String, password: String, onResult: (Boolean, String) -> Unit) {
        scope.launch {
            val clean = email.trim().lowercase()
            if (first.isBlank() || last.isBlank()) return@launch onResult(false, "Please enter your name.")
            if (!clean.contains("@") || !clean.contains(".")) return@launch onResult(false, "Please enter a valid email.")
            if (password.length < 6) return@launch onResult(false, "Password must be at least 6 characters.")

            if (Config.cloudConfigured) {
                val (auth, err) = cloud.signUp(clean, password, "${first.trim()} ${last.trim()}")
                if (auth?.access_token != null) {
                    activateCloudSession(clean, auth.user?.id, "${first.trim()} ${last.trim()}")
                    onResult(true, "")
                    return@launch
                }
                onResult(false, err ?: "Sign-up failed. Please try again.")
                return@launch
            }
            if (store.findUser(clean) != null) return@launch onResult(false, "An account with that email already exists.")
            store.addUser(
                RippleStore.UserDto(clean, first.trim(), last.trim(), Repo.hash(clean, password), System.currentTimeMillis())
            )
            sessionInternal.value = Session(clean, first.trim(), last.trim())
            onResult(true, "")
        }
    }

    fun logout() {
        if (Config.cloudConfigured) cloud.signOut()
        sessionInternal.value = null
    }

    // ---- ripples ----------------------------------------------------------------

    private suspend fun pullCloudRipples() {
        val s = sessionInternal.value ?: return
        val uid = s.cloudId ?: return
        val res = kotlinx.coroutines.withContext(Dispatchers.IO) {
            com.yft.rippleup.data.remote.SupaClient.rest(
                "GET", "ripples", "user_id=eq.$uid&select=*&order=created_at.desc&limit=200",
                token = sessions.accessToken,
            )
        }
        if (!res.ok) return
        val rows = runCatching {
            kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                .decodeFromString<List<CloudRipple>>(res.body ?: "[]")
        }.getOrDefault(emptyList())
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            rows.forEach { cr ->
                val existing = store.state.value.ripples.firstOrNull { it.tamperTag == "cloud:${cr.id}" }
                val dto = RippleStore.RippleDto(
                    id = existing?.id ?: 0,
                    userEmail = s.email, title = cr.title, subtitle = cr.subtitle ?: "",
                    points = cr.points, co2eKg = cr.co2e_grams / 1000f, actionKey = cr.action_key,
                    status = if (cr.status == "qr") 2 else 1, art = existing?.art ?: "none",
                    createdAt = parseIso(cr.created_at), demo = false, tamperTag = "cloud:${cr.id}",
                )
                if (existing != null) store.updateRippleLocal(dto) else store.insertLocal(dto)
            }
        }
    }

    private fun parseIso(iso: String?): Long = runCatching {
        val clean = iso?.substringBefore('.')?.replace("Z", "") ?: return System.currentTimeMillis()
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .parse(clean)?.time ?: System.currentTimeMillis()
    }.getOrDefault(System.currentTimeMillis())

    // ---- actions ----------------------------------------------------------------

    fun addPending(title: String, subtitle: String, points: Int, key: String, kg: Float, art: String = "none") {
        val s = sessionInternal.value ?: return
        scope.launch {
            val r = RippleEntity(
                userEmail = s.email, title = title, subtitle = subtitle, points = points,
                co2eKg = kg, actionKey = key, status = 0, art = art, createdAt = System.currentTimeMillis(),
            )
            store.insertRipple(r.copy(tamperTag = Repo.tamperTag(r)))
            if (s.cloudId != null) {
                cloud.pushRipple(
                    CloudRipple(user_id = s.cloudId, title = title, subtitle = subtitle, points = points,
                        co2e_grams = (kg * 1000).toInt(), status = "self", action_key = key)
                )
            }
        }
    }

    fun removeRipple(id: Long) {
        scope.launch { store.deleteRipple(id) }
    }

    fun setStatus(id: Long, status: Int) {
        scope.launch {
            ripplesInternal.value.find { it.id == id }?.let {
                val updated = it.copy(status = status)
                store.updateRipple(updated.copy(tamperTag = Repo.tamperTag(updated)))
            }
        }
    }

    suspend fun guardReject(key: String): String? {
        val s = sessionInternal.value ?: return "Not signed in."
        val dayStart = dayStartMs()
        val logged = ripplesInternal.value.filter { !it.demo && it.createdAt >= dayStart }
        val actionsToday = logged.size
        val pointsToday = logged.sumOf { it.points }
        val last = logged.maxOfOrNull { it.createdAt } ?: 0L
        return when (val v = Guard.check(key, last, actionsToday, pointsToday)) {
            is Guard.Verdict.Allowed -> null
            else -> Guard.rejectionMessage(v)
        }
    }

    fun commitVerified(pending: PendingVerify) {
        val s = sessionInternal.value ?: return
        scope.launch {
            val r = RippleEntity(
                userEmail = s.email, title = pending.title, subtitle = pending.subtitle,
                points = pending.points, co2eKg = pending.co2eKg, actionKey = pending.actionKey,
                status = if (pending.viaQr) 2 else 1, art = "none", createdAt = System.currentTimeMillis(),
            )
            store.insertRipple(r.copy(tamperTag = Repo.tamperTag(r)))
            if (s.cloudId != null) {
                cloud.pushRipple(
                    CloudRipple(
                        user_id = s.cloudId, title = pending.title, subtitle = pending.subtitle,
                        points = pending.points, co2e_grams = (pending.co2eKg * 1000).toInt(),
                        status = if (pending.viaQr) "pending_review" else "self", action_key = pending.actionKey,
                    )
                )
            }
        }
    }

    /** Desktop QR verification: the user picks the partner location (no GPS on desktop). */
    suspend fun recordDesktopVerification(location: CloudLocation): Pair<CloudVerification?, String?> {
        val s = sessionInternal.value ?: return null to "Sign in first."
        val uid = s.cloudId ?: return null to "Cloud session required for QR verification."
        guardReject("refill")?.let { return null to it }

        val cr = cloud.pushRipple(
            CloudRipple(
                user_id = uid, title = "Partner action @ ${location.name}",
                subtitle = "QR-verified at ${location.name}", points = 500, co2e_grams = 1200,
                status = "pending_review", action_key = "refill", location_id = location.id,
            )
        ) ?: return null to "Could not reach the cloud — try again."

        val cv = cloud.pushVerification(
            CloudVerification(
                user_id = uid, ripple_id = cr.id, location_id = location.id, method = "desktop",
                user_lat = location.lat, user_lng = location.lng,
                user_address = listOfNotNull(location.name, location.address).joinToString(", "),
                accuracy_m = null, distance_m = 0,
                device = "Desktop", status = "flagged",
            )
        )

        store.insertRipple(
            RippleEntity(
                userEmail = s.email, title = "Partner action @ ${location.name}",
                subtitle = "QR-verified at ${location.name}", points = 500, co2eKg = 1.2f,
                actionKey = "refill", status = 2, art = "none", createdAt = System.currentTimeMillis(),
                tamperTag = "cloud:${cr.id}",
            )
        )
        cloud.syncBadges()
        return cv to null
    }
}
