@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.yft.rippleup.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yft.rippleup.data.remote.CloudEvent
import com.yft.rippleup.data.remote.CloudRipple
import com.yft.rippleup.data.remote.CloudSync
import com.yft.rippleup.data.remote.CloudVerification
import com.yft.rippleup.data.remote.Config
import com.yft.rippleup.data.remote.SessionManager
import com.yft.rippleup.data.remote.SupaClient
import com.yft.rippleup.util.Guard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Where the app should be right now (single source of truth for the router). */
enum class Boot { LOADING, ONBOARDING, AUTH, PENDING_APPROVAL, HOME }

data class Session(
    val email: String,
    val firstName: String,
    val cloudId: String? = null,
    val isAdmin: Boolean = false,
    val approvalStatus: String = "approved",
)

data class Stats(
    val points: Int = 0,
    val co2Kg: Float = 0f,
    val streak: Int = 0,
    val longest: Int = 0,
)

data class PendingVerify(
    val title: String,
    val subtitle: String,
    val points: Int,
    val actionKey: String,
    val co2eKg: Float,
    val viaQr: Boolean,
)

/** PRODUCTION model: cloud-only. No demo accounts, no local fallback. */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    val sessions = SessionManager(app)
    val cloud = CloudSync(sessions)

    private val bootInternal = MutableStateFlow(Boot.LOADING)
    val boot: StateFlow<Boot> = bootInternal

    private val sessionInternal = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = sessionInternal

    private val ripplesInternal = MutableStateFlow<List<CloudRipple>>(emptyList())
    val ripples: StateFlow<List<CloudRipple>> = ripplesInternal

    private val eventsInternal = MutableStateFlow<List<CloudEvent>>(emptyList())
    val events: StateFlow<List<CloudEvent>> = eventsInternal

    private val statsInternal = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> = statsInternal

    private val onboardedPrefs =
        app.getSharedPreferences("rippleup_state", android.content.Context.MODE_PRIVATE)

    init {
        viewModelScope.launch {
            if (Config.cloudConfigured && sessions.hasSession) {
                val refreshed = withContext(Dispatchers.IO) { sessions.refreshIfNeeded() }
                if (refreshed) {
                    val profile = cloud.fetchProfile()
                    if (profile != null) {
                        enterSession(profile)
                        bootInternal.value = Boot.HOME
                        return@launch
                    }
                }
                sessions.clear()
            }
            // no live session
            sessionInternal.value = null
            ripplesInternal.value = emptyList()
            bootInternal.value =
                if (onboardedPrefs.getBoolean("onboarded", false)) Boot.AUTH else Boot.ONBOARDING
        }
    }

    private suspend fun enterSession(profile: com.yft.rippleup.data.remote.CloudProfile) {
        sessionInternal.value = Session(
            email = profile.email ?: "member",
            firstName = (profile.full_name ?: profile.email ?: "Ripple")
                .substringBefore(" ").substringBefore("@").replaceFirstChar { it.uppercase() },
            cloudId = profile.id,
            isAdmin = profile.is_admin,
            approvalStatus = profile.approval_status,
        )
        ripplesInternal.value = cloud.fetchMyRipples()
        eventsInternal.value = cloud.fetchEvents()
        recomputeStats()
        if (profile.approval_status != "approved") bootInternal.value = Boot.PENDING_APPROVAL
    }

    private fun recomputeStats() {
        val s = sessionInternal.value ?: return
        viewModelScope.launch {
            val profile = cloud.fetchProfile() ?: return@launch
            val streak = streakFromRipples(ripplesInternal.value)
            statsInternal.value = Stats(
                points = profile.total_points,
                co2Kg = profile.co2e_grams / 1000f,
                streak = streak,
                longest = maxOf(profile.streak_days, streak),
            )
        }
    }

    val displayName: String
        get() = session.value?.let { it.firstName } ?: "Ripple member"

    // ---- AUTH (OTP) ------------------------------------------------------------

    // ---- AUTH (email + password; requires Confirm-email OFF while on the free tier) ----

    suspend fun signUpWithPassword(
        first: String, last: String, email: String, password: String,
    ): Pair<String?, String?> {
        val clean = email.trim().lowercase()
        if (first.isBlank() || last.isBlank()) return Pair("Please enter your name.", null)
        if (!clean.contains("@") || !clean.contains(".")) return Pair("Please enter a valid email.", null)
        if (password.length < 6) return Pair("Password must be at least 6 characters.", null)
        val res = SupaClient.authPost(
            "signup",
            kotlinx.serialization.json.buildJsonObject {
                put("email", kotlinx.serialization.json.JsonPrimitive(clean))
                put("password", kotlinx.serialization.json.JsonPrimitive(password))
                put("data", kotlinx.serialization.json.buildJsonObject {
                    put("full_name", kotlinx.serialization.json.JsonPrimitive(first.trim() + " " + last.trim()))
                })
            },
        )
        val parsed = res.parse<com.yft.rippleup.data.remote.AuthResponse>()
        if (res.ok && parsed?.access_token != null) {
            sessions.accessToken = parsed.access_token
            parsed.refresh_token?.let { sessions.refreshToken = it }
            return Pair(null, null)
        }
        if (res.ok && parsed?.access_token == null) return Pair("CONFIRM_EMAIL_ON", null)
        return Pair(parsed?.msg, res.error())
    }

    suspend fun loginWithPassword(email: String, password: String): Pair<String?, String?> {
        val res = SupaClient.authPost(
            "token?grant_type=password",
            SupaClient.obj("email" to email.trim().lowercase(), "password" to password),
        )
        val parsed = res.parse<com.yft.rippleup.data.remote.AuthResponse>()
        if (res.ok && parsed?.access_token != null) {
            sessions.accessToken = parsed.access_token
            parsed.refresh_token?.let { sessions.refreshToken = it }
            return Pair(null, null)
        }
        return Pair(parsed?.msg, res.error())
    }

    suspend fun requestOtp(email: String): String? {
        if (!email.contains("@") || !email.contains(".")) return "Please enter a valid email."
        return cloud.sendOtp(email)
    }

    fun verifyCode(email: String, code: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val (auth, err) = cloud.verifyOtp(email, code)
            if (auth?.access_token == null) {
                onResult(false, err ?: "Invalid or expired code.")
                return@launch
            }
            val profile = cloud.fetchProfile()
            if (profile == null) {
                onResult(false, "Could not load your profile — try again.")
                return@launch
            }
            onboardedPrefs.edit().putBoolean("onboarded", true).apply()
            enterSession(profile)
            bootInternal.value =
                if (profile.approval_status == "approved") Boot.HOME else Boot.PENDING_APPROVAL
            onResult(true, "")
        }
    }

    fun loginWithGoogle(idToken: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val (auth, err) = cloud.loginWithGoogleIdToken(idToken)
            if (auth?.access_token == null) {
                onResult(false, err ?: "Google sign-in failed.")
                return@launch
            }
            val profile = cloud.fetchProfile()
            if (profile == null) {
                onResult(false, "Could not load your profile — try again.")
                return@launch
            }
            onboardedPrefs.edit().putBoolean("onboarded", true).apply()
            enterSession(profile)
            bootInternal.value =
                if (profile.approval_status == "approved") Boot.HOME else Boot.PENDING_APPROVAL
            onResult(true, "")
        }
    }

    /** Called after a successful password auth: loads the profile and routes. */
    fun activatePasswordSession(authEmail: String, cloudUserId: String?, name: String?, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val profile = cloud.fetchProfile()
            if (profile == null) {
                onResult(false, "Could not load your profile - try again.")
                return@launch
            }
            onboardedPrefs.edit().putBoolean("onboarded", true).apply()
            enterSession(profile)
            bootInternal.value =
                if (profile.approval_status == "approved") Boot.HOME else Boot.PENDING_APPROVAL
            onResult(true, "")
        }
    }

    fun logout() {
        cloud.signOut()
        sessionInternal.value = null
        ripplesInternal.value = emptyList()
        bootInternal.value = Boot.AUTH
    }

    fun markOnboarded() {
        onboardedPrefs.edit().putBoolean("onboarded", true).apply()
    }

    fun goToAuth() {
        bootInternal.value = Boot.AUTH
    }

    fun refreshAll() {
        viewModelScope.launch {
            val profile = cloud.fetchProfile() ?: return@launch
            if (profile.approval_status == "approved") {
                ripplesInternal.value = cloud.fetchMyRipples()
                eventsInternal.value = cloud.fetchEvents()
            }
            recomputeStats()
        }
    }

    // ---- ACTIONS ----------------------------------------------------------------

    /** Self-reported action with photo -> cloud instantly, points on admin view or instantly? Self counts. */
    fun commitSelfReported(pending: PendingVerify, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val uid = sessionInternal.value?.cloudId
                ?: return@launch onDone(false, "Sign in first.")
            val pushed = cloud.pushRipple(
                CloudRipple(
                    user_id = uid,
                    title = pending.title,
                    subtitle = pending.subtitle,
                    points = pending.points,
                    co2e_grams = (pending.co2eKg * 1000).toInt(),
                    status = "self",
                    action_key = pending.actionKey,
                )
            )
            if (pushed == null) {
                onDone(false, "Could not reach the cloud — try again.")
            } else {
                ripplesInternal.value = cloud.fetchMyRipples()
                recomputeStats()
                cloud.syncBadges()
                onDone(true, "")
            }
        }
    }

    fun removeRipple(cloudId: Long, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = cloud.deleteRipple(cloudId)
            if (ok) ripplesInternal.value = cloud.fetchMyRipples()
            onDone(ok)
        }
    }

    /** QR verification via the server-enforced RPC. Returns the verification receipt id. */
    suspend fun submitQrScan(
        location: com.yft.rippleup.data.remote.CloudLocation,
        userLat: Double?,
        userLng: Double?,
        accuracyM: Double?,
        device: String,
    ): Pair<Long?, String?> {
        val result = cloud.submitQrScan(
            locationId = location.id,
            userLat = userLat,
            userLng = userLng,
            accuracyM = accuracyM,
            device = device,
            actionKey = "refill",
        )
        if (result.first != null) {
            ripplesInternal.value = cloud.fetchMyRipples()
            recomputeStats()
            cloud.syncBadges()
        }
        return result
    }

    suspend fun approveVerification(id: Long, onBadges: (List<String>) -> Unit = {}): String? =
        cloud.approveVerification(id, onBadges)

    suspend fun rejectVerification(id: Long, note: String): String? =
        cloud.rejectVerification(id, note)

    suspend fun setUserApproval(userId: String, status: String): String? =
        cloud.setUserApproval(userId, status)

    suspend fun fetchPendingUsers(): List<com.yft.rippleup.data.remote.CloudProfile> =
        cloud.fetchPendingProfiles()

    suspend fun fetchMyVerifications(): List<com.yft.rippleup.data.remote.VerificationReceipt> =
        cloud.fetchMyVerifications()

    suspend fun fetchAllVerifications(): List<com.yft.rippleup.data.remote.VerificationReceipt> =
        cloud.fetchAllVerifications()

    suspend fun fetchReceiptById(id: Long): com.yft.rippleup.data.remote.VerificationReceipt? =
        cloud.fetchReceiptById(id)

    suspend fun registerForEvent(eventKey: String): Boolean = cloud.registerForEvent(eventKey)

    // ---- helpers ----------------------------------------------------------------

    /** Friendly client-side cooldown/cap check (the cloud enforces the same rules). */
    suspend fun guardReject(key: String): String? {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = fmt.format(System.currentTimeMillis())
        val todays = ripplesInternal.value.filter { it.created_at?.startsWith(today) == true }
        if (todays.size >= Guard.DAILY_ACTION_CAP) return Guard.rejectionMessage(Guard.Verdict.DailyActionCap)
        val ptsToday = todays.sumOf { it.points }
        if (ptsToday >= Guard.DAILY_POINTS_CAP) return Guard.rejectionMessage(Guard.Verdict.DailyPointsCap(ptsToday))
        val lastForAction = ripplesInternal.value
            .filter { it.action_key == key }
            .maxOfOrNull { parseIso(it.created_at) } ?: 0L
        return when (val v = Guard.check(key, lastForAction, todays.size, ptsToday)) {
            is Guard.Verdict.Allowed -> null
            else -> Guard.rejectionMessage(v)
        }
    }

    private fun parseIso(iso: String?): Long = runCatching {
        val clean = iso?.substringBefore('.')?.replace("Z", "") ?: return System.currentTimeMillis()
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse(clean)?.time ?: System.currentTimeMillis()
    }.getOrDefault(System.currentTimeMillis())


    /** Consecutive-day streak ending today/yesterday, from verified ripples. */
    fun streakFromRipples(rows: List<CloudRipple>): Int {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val today = fmt.format(System.currentTimeMillis())
        val days = rows.filter { it.status == "self" || it.status == "qr" }
            .mapNotNull { it.created_at?.take(10) }
            .toSet()
        if (days.isEmpty()) return 0
        var streak = 0
        var cursor = System.currentTimeMillis()
        // allow the streak to be "alive" if today or yesterday has an action
        if (!days.contains(fmt.format(cursor)) && !days.contains(fmt.format(cursor - 86_400_000L))) return 0
        while (days.contains(fmt.format(cursor))) {
            streak += 1
            cursor -= 86_400_000L
        }
        if (streak == 0 && days.contains(fmt.format(cursor - 86_400_000L))) {
            cursor -= 86_400_000L
            while (days.contains(fmt.format(cursor))) {
                streak += 1
                cursor -= 86_400_000L
            }
        }
        return streak
    }
}
