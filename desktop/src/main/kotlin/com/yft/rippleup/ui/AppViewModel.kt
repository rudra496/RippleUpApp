package com.yft.rippleup.ui

import com.yft.rippleup.data.Repo
import com.yft.rippleup.data.RippleStore
import com.yft.rippleup.data.db.RippleEntity
import com.yft.rippleup.data.db.UserEntity
import com.yft.rippleup.util.Guard
import com.yft.rippleup.util.dayStartMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.prefs.Preferences

data class Session(val email: String, val firstName: String, val lastName: String)

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

/** Desktop build: plain observable model (no Android ViewModel dependency). */
class AppViewModel(private val store: RippleStore) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val prefs: Preferences = Preferences.userRoot().node("rippleup")

    private val sessionInternal = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = sessionInternal

    private val ripplesInternal = MutableStateFlow<List<RippleEntity>>(emptyList())
    val ripples: StateFlow<List<RippleEntity>> = ripplesInternal

    private val statsInternal = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> = statsInternal

    init {
        // refresh derived state whenever the store changes or the session switches
        scope.launch {
            kotlinx.coroutines.flow.combine(store.state, sessionInternal) { s, ses -> s to ses }
                .collect { (s, ses) ->
                    val email = ses?.email
                    val mine = s.ripples.filter { email != null && it.userEmail == email }.map { store.entityOf(it) }
                    ripplesInternal.value = mine.sortedBy { it.createdAt }
                    val earned = mine.filter { it.status > 0 }.sumOf { it.points }
                    val co2 = mine.filter { it.status > 0 }.sumOf { it.co2eKg.toDouble() }.toFloat()
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
        get() = session.value?.let { "${it.firstName} ${it.lastName}" } ?: "Saara Rodriguez"

    /** Synchronous one-shot start route (java.util.prefs — no blocking IO in composables). */
    fun computeStart(): String {
        if (sessionInternal.value != null) return "home"
        return if (prefs.getBoolean("onboarded", false)) "auth" else "splash"
    }

    fun markOnboarded() {
        prefs.putBoolean("onboarded", true)
    }

    fun login(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        scope.launch {
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
            if (store.findUser(clean) != null) return@launch onResult(false, "An account with that email already exists.")
            store.addUser(
                RippleStore.UserDto(clean, first.trim(), last.trim(), Repo.hash(clean, password), System.currentTimeMillis())
            )
            sessionInternal.value = Session(clean, first.trim(), last.trim())
            onResult(true, "")
        }
    }

    fun logout() {
        sessionInternal.value = null
    }

    // ---- ripples ----------------------------------------------------------------

    fun addPending(title: String, subtitle: String, points: Int, key: String, kg: Float, art: String = "none") {
        val s = session.value ?: return
        scope.launch {
            val r = RippleEntity(
                userEmail = s.email, title = title, subtitle = subtitle, points = points,
                co2eKg = kg, actionKey = key, status = 0, art = art, createdAt = System.currentTimeMillis(),
            )
            store.insertRipple(r.copy(tamperTag = tagOf(r)))
        }
    }

    fun removeRipple(id: Long) {
        scope.launch { store.deleteRipple(id) }
    }

    fun setStatus(id: Long, status: Int) {
        scope.launch {
            ripplesInternal.value.find { it.id == id }?.let {
                val updated = it.copy(status = status)
                store.updateRipple(updated.copy(tamperTag = tagOf(updated)))
            }
        }
    }

    /** Runs the guard checks for a fresh verified action. Returns null when allowed. */
    suspend fun guardReject(key: String): String? {
        val s = session.value ?: return "Not signed in."
        val dayStart = dayStartMs()
        val logged = ripplesInternal.value
            .filter { !it.demo && it.createdAt >= dayStart }
        val actionsToday = logged.size
        val pointsToday = logged.sumOf { it.points }
        val last = logged.maxOfOrNull { it.createdAt } ?: 0L
        return when (val v = Guard.check(key, last, actionsToday, pointsToday)) {
            is Guard.Verdict.Allowed -> null
            else -> Guard.rejectionMessage(v)
        }
    }

    /** Persist a verified action and award points. */
    fun commitVerified(pending: PendingVerify) {
        val s = session.value ?: return
        scope.launch {
            val r = RippleEntity(
                userEmail = s.email, title = pending.title, subtitle = pending.subtitle,
                points = pending.points, co2eKg = pending.co2eKg, actionKey = pending.actionKey,
                status = if (pending.viaQr) 2 else 1, art = "none", createdAt = System.currentTimeMillis(),
            )
            store.insertRipple(r.copy(tamperTag = tagOf(r)))
        }
    }

    private fun tagOf(r: RippleEntity): String =
        Guard.tag(r.userEmail, r.title, r.points, r.status, r.createdAt)
}
