package com.yft.rippleup.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val access_token: String? = null,
    val refresh_token: String? = null,
    val expires_in: Int = 0,
    val user: CloudUser? = null,
    val msg: String? = null,
    val error_description: String? = null,
)

@Serializable
data class CloudUser(
    val id: String,
    val email: String? = null,
    val user_metadata: kotlinx.serialization.json.JsonObject? = null,
)

@Serializable
data class CloudProfile(
    val id: String,
    val email: String? = null,
    val full_name: String? = null,
    val is_admin: Boolean = false,
    val total_points: Int = 0,
    val co2e_grams: Int = 0,
)

@Serializable
data class CloudLocation(
    val id: String,
    val name: String,
    val address: String? = null,
    val emoji: String? = null,
    val lat: Double,
    val lng: Double,
    val radius_m: Int = 150,
    val qr_sig: String = "",
    val active: Boolean = true,
)

@Serializable
data class CloudRipple(
    val id: Long = 0,
    val user_id: String = "",
    val title: String,
    val subtitle: String? = null,
    val points: Int = 0,
    val co2e_grams: Int = 0,
    val status: String = "self",
    val action_key: String = "custom",
    val location_id: String? = null,
    val created_at: String? = null,
)

@Serializable
data class CloudVerification(
    val id: Long = 0,
    val user_id: String = "",
    val ripple_id: Long? = null,
    val location_id: String? = null,
    val method: String = "qr",
    val user_lat: Double? = null,
    val user_lng: Double? = null,
    val user_address: String? = null,
    val accuracy_m: Int? = null,
    val distance_m: Int? = null,
    val device: String? = null,
    val status: String = "pending",
    val review_note: String? = null,
    val created_at: String? = null,
)

/** One receipt entry: verification + joined location + profile (who/where/what). */
@Serializable
data class VerificationReceipt(
    val id: Long,
    val user_id: String,
    val method: String = "qr",
    val user_lat: Double? = null,
    val user_lng: Double? = null,
    val user_address: String? = null,
    val accuracy_m: Int? = null,
    val distance_m: Int? = null,
    val device: String? = null,
    val status: String = "pending",
    val review_note: String? = null,
    val created_at: String? = null,
    val ripple_id: Long? = null,
    val location_id: String? = null,
    val partner_locations: CloudLocation? = null,
    val profiles: CloudProfile? = null,
    val ripples: CloudRipple? = null,
)

@Serializable
data class CloudNotification(
    val id: Long,
    val title: String,
    val body: String? = null,
    val tone: String? = "mint",
    val read: Boolean = false,
    val created_at: String? = null,
)
