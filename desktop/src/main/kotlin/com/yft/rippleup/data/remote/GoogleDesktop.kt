package com.yft.rippleup.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

data class GoogleResult(val access: String?, val refresh: String?, val error: String?)

/**
 * Google sign-in for desktop: Supabase OAuth (PKCE) in the default browser with a
 * loopback HTTP callback. redirect_to http://localhost:3000 is in the project's
 * allowed redirect list, so the browser comes back with ?code=... after consent.
 */
object GoogleDesktop {

    private const val CALLBACK_PORT = 3000

    suspend fun signIn(): GoogleResult = withContext(Dispatchers.IO) {
        runCatching {
            val verifier = newVerifier()
            val redirect = "http://localhost:$CALLBACK_PORT/callback"
            val url = "${Config.SUPABASE_URL}/auth/v1/authorize?provider=google" +
                "&redirect_to=" + enc(redirect) +
                "&code_challenge=" + enc(s256(verifier)) +
                "&code_challenge_method=S256"

            ServerSocket(CALLBACK_PORT, 5, InetAddress.getLoopbackAddress()).use { server ->
                server.soTimeout = 5 * 60_000
                Desktop.getDesktop().browse(URI(url))

                val code = waitForCode(server) ?: return@use null
                val res = SupaClient.authPost(
                    "token?grant_type=pkce",
                    SupaClient.obj("auth_code" to code, "code_verifier" to verifier),
                )
                val parsed = res.parse<AuthResponse>()
                if (res.ok && parsed?.access_token != null) {
                    GoogleResult(parsed.access_token, parsed.refresh_token, null)
                } else {
                    GoogleResult(null, null, parsed?.msg ?: res.error())
                }
            } ?: GoogleResult(null, null, "Google sign-in was cancelled or timed out.")
        }.getOrElse { GoogleResult(null, null, it.message ?: "Google sign-in failed.") }
    }

    /** Accepts loopback connections until one is the /callback redirect. */
    private fun waitForCode(server: ServerSocket): String? {
        repeat(5) {
            val socket = runCatching { server.accept() }.getOrNull() ?: return null
            socket.use { s ->
                s.soTimeout = 10_000
                val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
                val requestLine = reader.readLine() ?: return@use
                val path = requestLine.split(" ").getOrNull(1) ?: return@use
                val okPage = "<html><body style='font-family:sans-serif;background:#F5FFFC;" +
                    "color:#0C2620;display:grid;place-items:center;height:95vh'>" +
                    "<div style='text-align:center'><h2>✓ Signed in</h2>" +
                    "<p>You can close this window and return to RippleUp.</p></div></body></html>"
                respond(s.getOutputStream(), if (path.startsWith("/callback")) 200 else 404, okPage)
                if (!path.startsWith("/callback")) return@use
                val query = path.substringAfter('?', "")
                val params = query.split('&').filter { it.contains('=') }.associate {
                    val (k, v) = it.split('=', limit = 2)
                    URLDecoder.decode(k, "UTF-8") to URLDecoder.decode(v, "UTF-8")
                }
                val err = params["error_description"] ?: params["error"]
                return params["code"] ?: throw IllegalStateException(err ?: "No auth code in callback.")
            }
        }
        return null
    }

    private fun respond(out: java.io.OutputStream, status: Int, html: String) {
        val head = "HTTP/1.1 ${if (status == 200) "200 OK" else "404 Not Found"}\r\n" +
            "Content-Type: text/html; charset=utf-8\r\n" +
            "Content-Length: ${html.toByteArray(Charsets.UTF_8).size}\r\n" +
            "Connection: close\r\n\r\n"
        out.use { it.write((head + html).toByteArray(Charsets.UTF_8)); it.flush() }
    }

    private fun newVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun s256(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private fun enc(v: String) = URLEncoder.encode(v, "UTF-8")
}
