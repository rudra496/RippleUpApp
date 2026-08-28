package com.yft.rippleup.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yft.rippleup.data.Repo
import com.yft.rippleup.data.db.AppDatabase
import com.yft.rippleup.data.db.RippleEntity
import com.yft.rippleup.data.db.UserEntity
import com.yft.rippleup.util.Guard
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val db by lazy { AppDatabase.get(app) }

    init {
        viewModelScope.launch {
            runCatching { Repo.seedIfEmpty(db.userDao(), db.rippleDao(), db.prefDao()) }
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

    fun login(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
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
            runCatching {
                val clean = email.trim().lowercase()
                if (first.isBlank() || last.isBlank()) return@runCatching onResult(false, "Please enter your name.")
                if (!clean.contains("@") || !clean.contains(".")) return@runCatching onResult(false, "Please enter a valid email.")
                if (password.length < 6) return@runCatching onResult(false, "Password must be at least 6 characters.")
                if (db.userDao().byEmail(clean) != null) return@runCatching onResult(false, "An account with that email already exists.")
                db.userDao().insert(UserEntity(clean, first.trim(), last.trim(), Repo.hash(clean, password), System.currentTimeMillis()))
                sessionInternal.value = Session(clean, first.trim(), last.trim())
                onResult(true, "")
            }.onFailure { onResult(false, "Something went wrong. Please try again.") }
        }
    }

    fun logout() {
        sessionInternal.value = null
    }

    fun markOnboarded() {
        onboardedPrefs.edit().putBoolean("onboarded", true).apply()
    }

    // ---- ripples ----------------------------------------------------------------

    @OptIn(ExperimentalCoroutinesApi::class)
    val ripples: StateFlow<List<RippleEntity>> = session.flatMapLatest { s ->
        s?.let { db.rippleDao().forUser(it.email) } ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val stats: StateFlow<Stats> = session.flatMapLatest { s ->
        if (s == null) return@flatMapLatest flowOf(Stats())
        val email = s.email
        kotlinx.coroutines.flow.combine(
            db.rippleDao().earnedPoints(email),
            db.rippleDao().earnedCo2(email),
        ) { pts, co2 ->
            val earned = pts ?: 0
            val runtimeEarned = (earned - 360).coerceAtLeast(0) // demo seed rows = 360 pts
            Stats(
                points = Repo.BASE_POINTS + earned,
                co2Kg = Repo.BASE_CO2_KG + (co2 ?: 0f),
                pointsPill = 208 + runtimeEarned,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Stats())

    val displayName: String
        get() = session.value?.let { "${it.firstName} ${it.lastName}" } ?: "Saara Rodriguez"

    /** Add a pending ripple to today's list (from Discover ADD / Edit sheet). */
    fun addPending(title: String, subtitle: String, points: Int, key: String, kg: Float, art: String = "none") {
        val s = session.value ?: return
        viewModelScope.launch {
            runCatching {
                val r = RippleEntity(
                    userEmail = s.email, title = title, subtitle = subtitle, points = points,
                    co2eKg = kg, actionKey = key, status = 0, art = art, createdAt = System.currentTimeMillis(),
                )
                db.rippleDao().insert(r.copy(tamperTag = Repo.tamperTag(r)))
            }
        }
    }

    fun removeRipple(id: Long) {
        viewModelScope.launch { runCatching { db.rippleDao().delete(id) } }
    }

    fun setStatus(id: Long, status: Int) {
        viewModelScope.launch {
            runCatching {
                ripples.value.find { it.id == id }?.let {
                    val updated = it.copy(status = status)
                    db.rippleDao().update(updated.copy(tamperTag = Repo.tamperTag(updated)))
                }
            }
        }
    }

    /**
     * Runs the guard checks for a fresh verified action. Returns null when allowed,
     * otherwise a human-readable rejection message.
     */
    suspend fun guardReject(key: String): String? {
        val s = session.value ?: return "Not signed in."
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

    /** Persist a verified action and award points. */
    fun commitVerified(pending: PendingVerify) {
        val s = session.value ?: return
        viewModelScope.launch {
            runCatching {
                val r = RippleEntity(
                    userEmail = s.email, title = pending.title, subtitle = pending.subtitle,
                    points = pending.points, co2eKg = pending.co2eKg, actionKey = pending.actionKey,
                    status = if (pending.viaQr) 2 else 1, art = "none", createdAt = System.currentTimeMillis(),
                )
                db.rippleDao().insert(r.copy(tamperTag = Repo.tamperTag(r)))
            }
        }
    }

    // ---- profile ------------------------------------------------------------

    fun authErrorHint() = "Tip: test account is admin / rudra"
}
