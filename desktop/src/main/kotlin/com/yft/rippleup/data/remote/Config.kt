package com.yft.rippleup.data.remote

/**
 * RippleUp cloud configuration (Supabase, free tier).
 *
 * SETUP (one-time):
 *  1. Supabase Dashboard -> Settings -> API -> copy "Project URL" -> paste below.
 *  2. Run supabase/schema.sql in the SQL Editor (creates tables + pipelines).
 *  3. For Google sign-in: create an OAuth **Web** client in Google Cloud console,
 *     enable the Google provider in Supabase Auth with that client ID, paste it in
 *     GOOGLE_WEB_CLIENT_ID below.
 *  4. Make yourself admin: update public.profiles set is_admin = true where email = '...';
 *
 * The publishable key is SAFE to ship in a client app (that is its purpose).
 */
object Config {
    const val SUPABASE_URL = "https://YOUR-PROJECT-REF.supabase.co"
    const val SUPABASE_PUBLISHABLE_KEY = "sb_publishable_XV7yrMaCtCjKH9UDI0gXug_yNgb8JD5"

    /** Google OAuth **Web** client ID (Credential Manager + Supabase Google provider). */
    const val GOOGLE_WEB_CLIENT_ID = ""

    /** Shared secret for location QR payloads (printed QR sheets). Rotate for production. */
    const val QR_SECRET = "ripplup-qr-2026"

    val cloudConfigured: Boolean get() = !SUPABASE_URL.contains("YOUR-PROJECT")
    val googleConfigured: Boolean get() = GOOGLE_WEB_CLIENT_ID.isNotBlank()
}
