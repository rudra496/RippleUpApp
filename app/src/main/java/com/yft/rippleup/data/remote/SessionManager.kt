package com.yft.rippleup.data.remote

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Persisted Supabase session (encrypted on disk when the Keystore is available). */
class SessionManager(context: Context) {

    private val prefs by lazy {
        runCatching {
            val master = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            EncryptedSharedPreferences.create(context, "rippleup_session", master,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
        }.getOrElse {
            // Keystore can be broken on some devices — never crash the app for cache.
            context.getSharedPreferences("rippleup_session_plain", Context.MODE_PRIVATE)
        }
    }

    var accessToken: String
        get() = prefs.getString("access", "") ?: ""
        set(v) = prefs.edit().putString("access", v).apply()

    var refreshToken: String
        get() = prefs.getString("refresh", "") ?: ""
        set(v) = prefs.edit().putString("refresh", v).apply()

    val hasSession: Boolean get() = accessToken.isNotBlank()

    fun clear() = prefs.edit().clear().apply()

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
