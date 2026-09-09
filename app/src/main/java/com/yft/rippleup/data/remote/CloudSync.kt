package com.yft.rippleup.data.remote

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** High-level cloud operations (all suspend, all guarded by runCatching). */
class CloudSync(private val sessions: SessionManager) {

    private val listJson = Json { ignoreUnknownKeys = true }

    private fun token(): String? = sessions.accessToken.ifBlank { null }

    private suspend fun <T> get(path: String, table: String, deserializer: kotlinx.serialization.KSerializer<T>): T? =
        runCatching {
            val res = SupaClient.rest("GET", table, path, token = token())
            if (res.ok) listJson.decodeFromString(ListSerializer(deserializer), res.body ?: "[]").firstOrNull() else null
        }.getOrNull()

    suspend fun loginWithPassword(email: String, password: String): Pair<AuthResponse?, String?> {
        val res = SupaClient.authPost("token?grant_type=password", SupaClient.obj("email" to email, "password" to password))
        val parsed = res.parse<AuthResponse>()
        if (res.ok && parsed?.access_token != null) {
            sessions.accessToken = parsed.access_token
            parsed.refresh_token?.let { sessions.refreshToken = it }
            return parsed to null
        }
        return null to (parsed?.msg ?: res.error())
    }

    suspend fun signUp(email: String, password: String, fullName: String): Pair<AuthResponse?, String?> {
        val meta = buildJsonObject { put("full_name", fullName) }
        val res = SupaClient.authPost("signup", SupaClient.obj("email" to email, "password" to password).let {
            JsonObject(it.toMap() + mapOf("data" to meta))
        })
        val parsed = res.parse<AuthResponse>()
        if (res.ok && parsed?.access_token != null) {
            sessions.accessToken = parsed.access_token
            parsed.refresh_token?.let { sessions.refreshToken = it }
            return parsed to null
        }
        // session may be null when email confirmation is required — treat as "check email"
        return null to (parsed?.msg ?: if (res.ok) "CHECK_EMAIL" else res.error())
    }

    /** Google sign-in: exchange the Credential Manager ID token for a Supabase session. */
    suspend fun loginWithGoogleIdToken(idToken: String): Pair<AuthResponse?, String?> {
        val res = SupaClient.authPost(
            "token?grant_type=id_token",
            SupaClient.obj("provider" to "google", "id_token" to idToken),
        )
        val parsed = res.parse<AuthResponse>()
        if (res.ok && parsed?.access_token != null) {
            sessions.accessToken = parsed.access_token
            parsed.refresh_token?.let { sessions.refreshToken = it }
            return parsed to null
        }
        return null to (parsed?.msg ?: res.error())
    }

    suspend fun fetchProfile(): CloudProfile? =
        runCatching { get("id=eq.${currentUserId()}", "profiles", CloudProfile.serializer()) }.getOrNull()

    suspend fun currentUserId(): String? =
        runCatching {
            val res = SupaClient.authGet("user", sessions.accessToken)
            if (res.ok) res.parse<AuthResponse>()?.user?.id else null
        }.getOrNull()

    suspend fun fetchLocations(): List<CloudLocation> = runCatching {
        val res = SupaClient.rest("GET", "partner_locations", "active=eq.true&select=*", token = token())
        if (res.ok) listJson.decodeFromString(ListSerializer(CloudLocation.serializer()), res.body ?: "[]") else emptyList()
    }.getOrDefault(emptyList())

    suspend fun fetchLocation(id: String): CloudLocation? = runCatching {
        val res = SupaClient.rest("GET", "partner_locations", "id=eq.$id&select=*", token = token())
        if (res.ok) listJson.decodeFromString(ListSerializer(CloudLocation.serializer()), res.body ?: "[]").firstOrNull() else null
    }.getOrNull()

    suspend fun pushRipple(r: CloudRipple): CloudRipple? = runCatching {
        val res = SupaClient.rest("POST", "ripples", body = Json.encodeToString(CloudRipple.serializer(), r),
            token = token(), prefer = "return=representation")
        if (res.ok) listJson.decodeFromString(ListSerializer(CloudRipple.serializer()), res.body ?: "[]").firstOrNull() else null
    }.getOrNull()

    suspend fun pushVerification(v: CloudVerification): CloudVerification? = runCatching {
        val res = SupaClient.rest("POST", "verifications", body = Json.encodeToString(CloudVerification.serializer(), v),
            token = token(), prefer = "return=representation")
        if (res.ok) listJson.decodeFromString(ListSerializer(CloudVerification.serializer()), res.body ?: "[]").firstOrNull() else null
    }.getOrNull()

    suspend fun fetchMyVerifications(): List<VerificationReceipt> = runCatching {
        val uid = currentUserId() ?: return emptyList()
        val res = SupaClient.rest("GET", "verifications",
            "user_id=eq.$uid&select=*,partner_locations(*),ripples(*),profiles(*)&order=created_at.desc&limit=50",
            token = token())
        if (res.ok) listJson.decodeFromString(ListSerializer(VerificationReceipt.serializer()), res.body ?: "[]") else emptyList()
    }.getOrDefault(emptyList())

    suspend fun fetchReceiptById(id: Long): VerificationReceipt? = runCatching {
        val res = SupaClient.rest("GET", "verifications",
            "id=eq.$id&select=*,partner_locations(*),ripples(*),profiles(*)",
            token = token())
        if (res.ok) listJson.decodeFromString(ListSerializer(VerificationReceipt.serializer()), res.body ?: "[]").firstOrNull() else null
    }.getOrNull()

    suspend fun fetchAllVerifications(): List<VerificationReceipt> = runCatching {
        val res = SupaClient.rest("GET", "verifications",
            "select=*,partner_locations(*),ripples(*),profiles(*)&order=created_at.desc&limit=100",
            token = token())
        if (res.ok) listJson.decodeFromString(ListSerializer(VerificationReceipt.serializer()), res.body ?: "[]") else emptyList()
    }.getOrDefault(emptyList())

    suspend fun approveVerification(id: Long, badgeResult: (List<String>) -> Unit): String? = runCatching {
        val res = SupaClient.rpc("approve_verification", SupaClient.obj("p_verification" to id), token())
        if (!res.ok) return res.error()
        val earned = SupaClient.rpc("sync_badges", SupaClient.obj("p_user" to (currentUserId() ?: "")), token())
        if (earned.ok) badgeResult(parseStringList(earned.body))
        null
    }.getOrElse { it.message }

    suspend fun rejectVerification(id: Long, note: String): String? = runCatching {
        val args = if (note.isBlank()) {
            kotlinx.serialization.json.JsonObject(mapOf("p_verification" to kotlinx.serialization.json.JsonPrimitive(id)))
        } else {
            kotlinx.serialization.json.JsonObject(mapOf(
                "p_verification" to kotlinx.serialization.json.JsonPrimitive(id),
                "p_note" to kotlinx.serialization.json.JsonPrimitive(note),
            ))
        }
        val res = SupaClient.rpc("reject_verification", args, token())
        if (res.ok) null else res.error()
    }.getOrElse { it.message }

    suspend fun fetchNotifications(): List<CloudNotification> = runCatching {
        val res = SupaClient.rest("GET", "notifications", "select=*&order=created_at.desc&limit=20", token = token())
        if (res.ok) listJson.decodeFromString(ListSerializer(CloudNotification.serializer()), res.body ?: "[]") else emptyList()
    }.getOrDefault(emptyList())

    suspend fun syncBadges(): List<String> = runCatching {
        val res = SupaClient.rpc("sync_badges",
            SupaClient.obj("p_user" to (currentUserId() ?: "")), token())
        if (res.ok) parseStringList(res.body) else emptyList()
    }.getOrDefault(emptyList())

    suspend fun registerForEvent(eventKey: String): Boolean = runCatching {
        val res = SupaClient.rest("POST", "event_registrations",
            body = Json.encodeToString(EventRegistration.serializer(), EventRegistration(event_key = eventKey)),
            token = token(), prefer = "return=minimal")
        res.ok
    }.getOrDefault(false)

    private fun parseStringList(body: String?): List<String> = runCatching {
        listJson.decodeFromString<List<String>>(body ?: "[]")
    }.getOrDefault(emptyList())

    fun signOut() = sessions.clear()
}

@kotlinx.serialization.Serializable
private data class EventRegistration(val event_key: String)
