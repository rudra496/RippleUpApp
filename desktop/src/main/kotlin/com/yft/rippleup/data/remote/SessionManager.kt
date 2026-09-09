package com.yft.rippleup.data.remote

import java.util.prefs.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Desktop session storage: java.util.prefs (per-user, local machine). */
class SessionManager {

    private val prefs: Preferences = Preferences.userRoot().node("rippleup-session")

    var accessToken: String
        get() = prefs.get("access", "")
        set(v) = prefs.put("access", v)

    var refreshToken: String
        get() = prefs.get("refresh", "")
        set(v) = prefs.put("refresh", v)

    val hasSession: Boolean get() = accessToken.isNotBlank()

    fun clear() = prefs.clear()

    suspend fun refreshIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        val rt = refreshToken
        if (rt.isBlank()) return@withContext false
        val res = SupaClient.authPost("token?grant_type=refresh_token", SupaClient.obj("refresh_token" to rt))
        if (res.ok) {
            val parsed = res.parse<AuthResponse>()
            accessToken = parsed?.access_token ?: return@withContext false
            parsed.refresh_token?.let { refreshToken = it }
            true
        } else {
            clear()
            false
        }
    }
}
