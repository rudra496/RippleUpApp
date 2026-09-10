package com.yft.rippleup.data.remote

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
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

    // ---- AUTH: passwordless email OTP (verification code) + Google id_token ----

    /** Sends a 6-digit verification code to the email. Creates the account if new. */
    suspend fun sendOtp(email: String): String? {
        val res = SupaClient.authPost(
            "otp",
            buildJsonObject {
                put("email", email.trim().lowercase())
                put("create_user", true)
            },
        )
        return if (res.ok) null else res.error()
    }

    /** Verifies the 6-digit code and stores the session. */
    suspend fun verifyOtp(email: String, code: String): Pair<AuthResponse?, String?> {
        val res = SupaClient.authPost(
            "verify",
            buildJsonObject {
                put("type", "email")
                put("email", email.trim().lowercase())
                put("token", code.trim())
            },
        )
        val parsed = res.parse<AuthResponse>()
        if (res.ok && parsed?.access_token != null) {
            sessions.accessToken = parsed.access_token
            parsed.refresh_token?.let { sessions.refreshToken = it }
            return parsed to null
        }
        return null to (parsed?.msg ?: res.error())
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

    suspend fun currentUserId(): String? = runCatching {
        val res = SupaClient.authGet("user", sessions.accessToken)
        if (res.ok) res.parse<AuthResponse>()?.user?.id else null
    }.getOrNull()

    suspend fun fetchProfile(): CloudProfile? =
        runCatching { get("id=eq.${currentUserId()}", "profiles", CloudProfile.serializer()) }.getOrNull()

    fun signOut() = sessions.clear()

    // ---- LOCATIONS / EVENTS ----

    suspend fun fetchLocations(): List<CloudLocation> = runCatching {
        val res = SupaClient.rest("GET", "partner_locations", "active=eq.true&select=*", token = token())
        if (res.ok) listJson.decodeFromString(ListSerializer(CloudLocation.serializer()), res.body ?: "[]") else emptyList()
    }.getOrDefault(emptyList())

    suspend fun fetchLocation(id: String): CloudLocation? = runCatching {
        val res = SupaClient.rest("GET", "partner_locations", "id=eq.$id&select=*", token = token())
        if (res.ok) listJson.decodeFromString(ListSerializer(CloudLocation.serializer()), res.body ?: "[]").firstOrNull() else null
    }.getOrNull()

    suspend fun fetchEvents(): List<CloudEvent> = runCatching {
        val res = SupaClient.rest("GET", "events", "active=eq.true&select=*", token = token())
        if (res.ok) listJson.decodeFromString(ListSerializer(CloudEvent.serializer()), res.body ?: "[]") else emptyList()
    }.getOrDefault(emptyList())

    // ---- RIPPLES ----

    suspend fun fetchMyRipples(): List<CloudRipple> = runCatching {
        val uid = currentUserId() ?: return emptyList()
        val res = SupaClient.rest("GET", "ripples",
            "user_id=eq.$uid&select=*&order=created_at.desc&limit=200", token = token())
        if (res.ok) listJson.decodeFromString(ListSerializer(CloudRipple.serializer()), res.body ?: "[]") else emptyList()
    }.getOrDefault(emptyList())

    suspend fun pushRipple(r: CloudRipple): CloudRipple? = runCatching {
        val res = SupaClient.rest("POST", "ripples", body = Json.encodeToString(CloudRipple.serializer(), r),
            token = token(), prefer = "return=representation")
        if (res.ok) listJson.decodeFromString(ListSerializer(CloudRipple.serializer()), res.body ?: "[]").firstOrNull() else null
    }.getOrNull()

    suspend fun deleteRipple(id: Long): Boolean = runCatching {
        SupaClient.rest("DELETE", "ripples", "id=eq.$id", token = token()).ok
    }.getOrDefault(false)

    // ---- QR VERIFICATION (server-enforced: geofence, 1/day, device binding, approval) ----

    /** Returns the new verification id, or an error message from the server. */
    suspend fun submitQrScan(
        locationId: String,
        userLat: Double?,
        userLng: Double?,
        accuracyM: Double?,
        device: String,
        actionKey: String,
    ): Pair<Long?, String?> {
        val res = SupaClient.rpc(
            "submit_qr_verification",
            buildJsonObject {
                put("p_location", locationId)
                if (userLat != null) put("p_user_lat", userLat)
                if (userLng != null) put("p_user_lng", userLng)
                if (accuracyM != null) put("p_accuracy", accuracyM)
                if (device.isNotBlank()) put("p_device", device)
                put("p_action_key", actionKey)
            },
            token(),
        )
        if (!res.ok) return null to res.error()
        val id = res.body?.trim()?.removePrefix("\"")?.removeSuffix("\"")?.toLongOrNull()
        return (id ?: -1L) to null
    }

    // ---- VERIFICATION RECEIPTS ----

    suspend fun fetchMyVerifications(): List<VerificationReceipt> = runCatching {
        val uid = currentUserId() ?: return emptyList()
        val res = SupaClient.rest("GET", "verifications",
            "user_id=eq.$uid&select=*,partner_locations(*),ripples(*),profiles(*)&order=created_at.desc&limit=50",
            token = token())
        if (res.ok) listJson.decodeFromString(ListSerializer(VerificationReceipt.serializer()), res.body ?: "[]") else emptyList()
    }.getOrDefault(emptyList())

    suspend fun fetchAllVerifications(): List<VerificationReceipt> = runCatching {
        val res = SupaClient.rest("GET", "verifications",
            "select=*,partner_locations(*),ripples(*),profiles(*)&order=created_at.desc&limit=100",
            token = token())
        if (res.ok) listJson.decodeFromString(ListSerializer(VerificationReceipt.serializer()), res.body ?: "[]") else emptyList()
    }.getOrDefault(emptyList())

    suspend fun fetchReceiptById(id: Long): VerificationReceipt? = runCatching {
        val res = SupaClient.rest("GET", "verifications",
            "id=eq.$id&select=*,partner_locations(*),ripples(*),profiles(*)",
            token = token())
        if (res.ok) listJson.decodeFromString(ListSerializer(VerificationReceipt.serializer()), res.body ?: "[]").firstOrNull() else null
    }.getOrNull()

    // ---- ADMIN ----

    suspend fun fetchPendingProfiles(): List<CloudProfile> = runCatching {
        val res = SupaClient.rest("GET", "profiles",
            "approval_status=neq.approved&select=*&order=created_at.desc&limit=100",
            token = token())
        if (res.ok) listJson.decodeFromString(ListSerializer(CloudProfile.serializer()), res.body ?: "[]") else emptyList()
    }.getOrDefault(emptyList())

    suspend fun setUserApproval(userId: String, status: String): String? {
        val res = SupaClient.rpc(
            "set_user_approval",
            buildJsonObject { put("p_user", userId); put("p_status", status) },
            token(),
        )
        return if (res.ok) null else res.error()
    }

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

    // ---- NOTIFICATIONS / REGISTRATIONS / BADGES ----

    suspend fun fetchNotifications(): List<CloudNotification> = runCatching {
        val res = SupaClient.rest("GET", "notifications", "select=*&order=created_at.desc&limit=20", token = token())
        if (res.ok) listJson.decodeFromString(ListSerializer(CloudNotification.serializer()), res.body ?: "[]") else emptyList()
    }.getOrDefault(emptyList())

    suspend fun registerForEvent(eventKey: String): Boolean = runCatching {
        val res = SupaClient.rest("POST", "event_registrations",
            body = Json.encodeToString(EventRegistration.serializer(), EventRegistration(event_key = eventKey)),
            token = token(), prefer = "return=minimal")
        res.ok
    }.getOrDefault(false)

    suspend fun syncBadges(): List<String> = runCatching {
        val res = SupaClient.rpc("sync_badges",
            SupaClient.obj("p_user" to (currentUserId() ?: "")), token())
        if (res.ok) parseStringList(res.body) else emptyList()
    }.getOrDefault(emptyList())

    private fun parseStringList(body: String?): List<String> = runCatching {
        listJson.decodeFromString<List<String>>(body ?: "[]")
    }.getOrDefault(emptyList())
}

@kotlinx.serialization.Serializable
private data class EventRegistration(val event_key: String)
