@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.yft.rippleup.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yft.rippleup.data.Repo
import com.yft.rippleup.data.db.AppDatabase
import com.yft.rippleup.data.db.RippleEntity
import com.yft.rippleup.data.db.UserEntity
import com.yft.rippleup.data.remote.CloudLocation
import com.yft.rippleup.data.remote.CloudRipple
import com.yft.rippleup.data.remote.CloudSync
import com.yft.rippleup.data.remote.Config
import com.yft.rippleup.data.remote.SessionManager
import com.yft.rippleup.util.Guard
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class Session(
    val email: String,
    val firstName: String,
    val lastName: String,
    val cloudId: String? = null,      // Supabase auth.users.id when signed in to the cloud
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

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val db by lazy { AppDatabase.get(app) }
    val sessions by lazy { SessionManager(app) }
    val cloud by lazy { CloudSync(sessions) }

    init {
        viewModelScope.launch {
            runCatching { Repo.seedIfEmpty(db.userDao(), db.rippleDao(), db.prefDao()) }
            // restore a persisted cloud session
            if (Config.cloudConfigured && sessions.hasSession) {
                if (sessions.refreshIfNeeded()) {
                    resumeCloudSession()
                }
            }
        }
    }

    // ---- session --------------------------------------------------------------

    private val sessionInternal = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = sessionInternal

    private val onboardedPrefs =
        app.getSharedPreferences("rippleup_state", android.content.Context.MODE_PRIVATE)

    /** Synchronous one-shot start route (SharedPreferences — never main-thread Room). */
    fun computeStart(): String {
        if (sessionInternal.value != null) return "home"
        return if (onboardedPrefs.getBoolean("onboarded", false)) "auth" else "splash"
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
        pushUnsynced()
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
        pushUnsynced()
    }

    fun login(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            // 1) cloud-first when configured
            if (Config.cloudConfigured) {
                val (auth, err) = cloud.loginWithPassword(email.trim().lowercase(), password)
                if (auth?.access_token != null) {
                    activateCloudSession(auth.user?.email ?: email, auth.user?.id, auth.user?.user_metadata?.get("full_name").toString().trim('"'))
                    onResult(true, "")
                    return@launch
                }
                if (err != null && !err.contains("Invalid login", true)) {
                    onResult(false, err)   // cloud reachable but rejected (e.g. email not confirmed)
                    return@launch
                }
                // fall through to the local demo account so offline testing still works
            }
            // 2) local demo auth
            runCatching {
                val user = db.userDao().byEmail(email.trim().lowercase())
                when {
                    user == null -> onResult(false, "No account found for that email.")
                    user.passwordHash != Repo.hash(user.email, password) ->
                        onResult(false, "Incorrect password. Try again.")
                    else -> {
                        sessionInternal.value = Session(user.email, user.firstName, user.lastName)
                        onResult(true, "")
                    }
                }
            }.onFailure { onResult(false, "Something went wrong. Please try again.") }
        }
    }

    fun signUp(first: String, last: String, email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (first.isBlank() || last.isBlank()) return@launch onResult(false, "Please enter your name.")
            if (!email.contains("@") || !email.contains(".")) return@launch onResult(false, "Please enter a valid email.")
            if (password.length < 6) return@launch onResult(false, "Password must be at least 6 characters.")
            val clean = email.trim().lowercase()

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
            // local demo sign-up
            runCatching {
                if (db.userDao().byEmail(clean) != null) return@runCatching onResult(false, "An account with that email already exists.")
                db.userDao().insert(UserEntity(clean, first.trim(), last.trim(), Repo.hash(clean, password), System.currentTimeMillis()))
                sessionInternal.value = Session(clean, first.trim(), last.trim())
                onResult(true, "")
            }.onFailure { onResult(false, "Something went wrong. Please try again.") }
        }
    }

    /** Google sign-in via Credential Manager ID token -> Supabase. */
    fun loginWithGoogle(idToken: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val (auth, err) = cloud.loginWithGoogleIdToken(idToken)
            if (auth?.access_token != null) {
                activateCloudSession(auth.user?.email ?: "", auth.user?.id, auth.user?.user_metadata?.get("full_name").toString().trim('"'))
                onResult(true, "")
            } else {
                onResult(false, err ?: "Google sign-in failed.")
            }
        }
    }

    fun logout() {
        if (Config.cloudConfigured) cloud.signOut()
        sessionInternal.value = null
    }

    fun markOnboarded() {
        onboardedPrefs.edit().putBoolean("onboarded", true).apply()
    }

    // ---- ripples ----------------------------------------------------------------

    private val ripplesInternal = MutableStateFlow<List<RippleEntity>>(emptyList())
    val ripples: StateFlow<List<RippleEntity>> = ripplesInternal

    init {
        viewModelScope.launch {
            sessionInternal.flatMapLatest { s ->
                if (s == null) kotlinx.coroutines.flow.flowOf(emptyList())
                else kotlinx.coroutines.flow.flow {
                    db.rippleDao().forUser(s.email).collect { emit(it) }
                }
            }.collect { ripplesInternal.value = it }
        }
    }

    private val statsInternal = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> = statsInternal

    init {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(ripplesInternal, sessionInternal) { ripples, s -> ripples to s }
                .collect { (list, _) ->
                    val earned = list.filter { it.status > 0 }.sumOf { it.points }
                    val co2 = list.filter { it.status > 0 }.sumOf { it.co2eKg.toDouble() }.toFloat()
                    val runtimeEarned = (earned - 360).coerceAtLeast(0) // demo seed rows = 360 pts
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

    // ---- cloud sync pipelines -------------------------------------------------

    private fun parseIsoToMillis(iso: String?): Long = runCatching {
        val clean = iso?.substringBefore('.')?.replace("Z", "") ?: return System.currentTimeMillis()
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse(clean)?.time ?: System.currentTimeMillis()
    }.getOrDefault(System.currentTimeMillis())

    private suspend fun pullCloudRipples() {
        val s = sessionInternal.value ?: return
        val uid = s.cloudId ?: return
        val res = withContext(Dispatchers.IO) {
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
        val dao = db.rippleDao()
        withContext(Dispatchers.IO) {
            rows.forEach { cr ->
                val existing = dao.byCloudId(cr.id)
                val ent = RippleEntity(
                    id = existing?.id ?: 0,
                    userEmail = s.email,
                    title = cr.title,
                    subtitle = cr.subtitle ?: "",
                    points = cr.points,
                    co2eKg = cr.co2e_grams / 1000f,
                    actionKey = cr.action_key,
                    status = if (cr.status == "qr") 2 else 1,
                    art = existing?.art ?: "none",
                    createdAt = parseIsoToMillis(cr.created_at),
                    demo = false,
                    cloudId = cr.id,
                    synced = true,
                )
                if (existing != null) dao.update(ent) else dao.insert(ent)
            }
        }
    }

    private suspend fun pushUnsynced() {
        val s = sessionInternal.value ?: return
        val uid = s.cloudId ?: return
        val dao = db.rippleDao()
        dao.unsynced(s.email).forEach { r ->
            val pushed = cloud.pushRipple(
                CloudRipple(
                    user_id = uid, title = r.title, subtitle = r.subtitle, points = r.points,
                    co2e_grams = (r.co2eKg * 1000).toInt(), status = if (r.status == 2) "qr" else "self",
                    action_key = r.actionKey,
                )
            )
            if (pushed != null) dao.markSynced(r.id, pushed.id)
        }
    }

    // ---- actions ----------------------------------------------------------------

    /** Add a pending ripple to today's list (from Discover ADD / Edit sheet). */
    fun addPending(title: String, subtitle: String, points: Int, key: String, kg: Float, art: String = "none") {
        val s = sessionInternal.value ?: return
        viewModelScope.launch {
            runCatching {
                val r = RippleEntity(
                    userEmail = s.email, title = title, subtitle = subtitle, points = points,
                    co2eKg = kg, actionKey = key, status = 0, art = art, createdAt = System.currentTimeMillis(),
                )
                val id = db.rippleDao().insert(r.copy(tamperTag = Repo.tamperTag(r)))
                if (s.cloudId != null) {
                    val pushed = cloud.pushRipple(
                        CloudRipple(user_id = s.cloudId, title = title, subtitle = subtitle, points = points,
                            co2e_grams = (kg * 1000).toInt(), status = "self", action_key = key)
                    )
                    if (pushed != null) db.rippleDao().markSynced(id, pushed.id)
                }
            }
        }
    }

    fun removeRipple(id: Long) {
        viewModelScope.launch { runCatching { db.rippleDao().delete(id) } }
    }

    fun setStatus(id: Long, status: Int) {
        viewModelScope.launch {
            runCatching {
                ripplesInternal.value.find { it.id == id }?.let {
                    val updated = it.copy(status = status)
                    db.rippleDao().update(updated.copy(tamperTag = Repo.tamperTag(updated)))
                }
            }
        }
    }

    /** Runs the guard checks for a fresh verified action. Returns null when allowed. */
    suspend fun guardReject(key: String): String? {
        val s = sessionInternal.value ?: return "Not signed in."
        val dayStart = com.yft.rippleup.util.dayStartMs()
        val logged = runCatching { db.rippleDao().loggedSince(s.email, dayStart) }.getOrDefault(emptyList())
        val actionsToday = logged.size
        val pointsToday = logged.sumOf { it.points }
        val last = logged.maxOfOrNull { it.createdAt } ?: 0L
        return when (val v = Guard.check(key, last, actionsToday, pointsToday)) {
            is Guard.Verdict.Allowed -> null
            else -> Guard.rejectionMessage(v)
        }
    }

    /** Persist a verified self-reported action and award points locally + cloud. */
    fun commitVerified(pending: PendingVerify) {
        val s = sessionInternal.value ?: return
        viewModelScope.launch {
            runCatching {
                val r = RippleEntity(
                    userEmail = s.email, title = pending.title, subtitle = pending.subtitle,
                    points = pending.points, co2eKg = pending.co2eKg, actionKey = pending.actionKey,
                    status = if (pending.viaQr) 2 else 1, art = "none", createdAt = System.currentTimeMillis(),
                )
                val id = db.rippleDao().insert(r.copy(tamperTag = Repo.tamperTag(r)))
                if (s.cloudId != null) {
                    val pushed = cloud.pushRipple(
                        CloudRipple(
                            user_id = s.cloudId, title = pending.title, subtitle = pending.subtitle,
                            points = pending.points, co2e_grams = (pending.co2eKg * 1000).toInt(),
                            status = if (pending.viaQr) "pending_review" else "self", action_key = pending.actionKey,
                        )
                    )
                    if (pushed != null) db.rippleDao().markSynced(id, pushed.id)
                }
            }
        }
    }

    /**
     * Full QR verification pipeline: cloud ripple + verification row (who/where) +
     * local mirror + badge sync. Returns the cloud verification (receipt) or an error.
     */
    suspend fun recordQrVerification(
        location: CloudLocation,
        userLat: Double?,
        userLng: Double?,
        userAddress: String?,
        accuracyM: Int?,
        device: String,
        title: String,
        subtitle: String,
        points: Int,
        actionKey: String,
        co2eGrams: Int,
    ): Pair<com.yft.rippleup.data.remote.CloudVerification?, String?> {
        val s = sessionInternal.value ?: return null to "Sign in first."
        val uid = s.cloudId ?: return null to "Cloud session required for QR verification."
        guardReject(actionKey)?.let { return null to it }

        val verdict = com.yft.rippleup.util.evaluateGeofence(userLat, userLng, location.lat, location.lng, location.radius_m)
        val distance = when (verdict) {
            is com.yft.rippleup.util.GeoVerdict.Pass -> verdict.distanceM
            is com.yft.rippleup.util.GeoVerdict.OutOfRange -> verdict.distanceM
            else -> null
        }
        val verStatus = if (verdict is com.yft.rippleup.util.GeoVerdict.Pass) "pending" else "flagged"

        val cr = cloud.pushRipple(
            CloudRipple(
                user_id = uid, title = title, subtitle = subtitle, points = points,
                co2e_grams = co2eGrams, status = "pending_review", action_key = actionKey,
                location_id = location.id,
            )
        ) ?: return null to "Could not reach the cloud — try again."

        val cv = cloud.pushVerification(
            com.yft.rippleup.data.remote.CloudVerification(
                user_id = uid, ripple_id = cr.id, location_id = location.id, method = "qr",
                user_lat = userLat, user_lng = userLng, user_address = userAddress,
                accuracy_m = accuracyM, distance_m = distance, device = device,
                status = verStatus,
            )
        )

        // local mirror (visible immediately on Home)
        val local = RippleEntity(
            userEmail = s.email, title = title, subtitle = subtitle, points = points,
            co2eKg = co2eGrams / 1000f, actionKey = actionKey, status = 2, art = "none",
            createdAt = System.currentTimeMillis(),
        )
        val localId = db.rippleDao().insert(local.copy(tamperTag = Repo.tamperTag(local)))
        db.rippleDao().markSynced(localId, cr.id)

        cloud.syncBadges()
        return cv to null
    }
}
