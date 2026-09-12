package com.yft.rippleup.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL

data class RestResult(val code: Int, val body: String?) {
    val ok: Boolean get() = code in 200..299
    inline fun <reified T> parse(): T? = runCatching { Json { ignoreUnknownKeys = true }.decodeFromString<T>(body ?: "") }.getOrNull()
    fun error(): String = runCatching {
        Json.parseToJsonElement(body ?: "").let { (it as? JsonObject)?.get("msg")?.toString()?.trim('"') ?: body ?: "HTTP $code" }
    }.getOrDefault("HTTP $code")
}

/**
 * Minimal Supabase REST + Auth client over HttpURLConnection (no SDK, free-tier friendly).
 * The publishable key identifies the project; user JWTs travel as Bearer.
 */
object SupaClient {

    private val json = Json { ignoreUnknownKeys = true }

    private fun http(url: String, method: String, headers: Map<String, String>, body: String?): RestResult {
        var con: HttpURLConnection? = null
        return runCatching {
            con = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15_000
                readTimeout = 20_000
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
            }
            val code = con!!.responseCode
            val stream = if (code in 200..299) con!!.inputStream else con!!.errorStream
            RestResult(code, stream?.bufferedReader()?.readText())
        }.getOrElse { RestResult(-1, it.message) }.also { con?.disconnect() }
    }

    private fun baseHeaders(token: String?): Map<String, String> = buildMap {
        put("apikey", Config.SUPABASE_PUBLISHABLE_KEY)
        if (!token.isNullOrBlank()) put("Authorization", "Bearer $token")
    }

    suspend fun rest(
        method: String,
        table: String,
        query: String = "",
        body: String? = null,
        token: String? = null,
        prefer: String? = null,
    ): RestResult = withContext(Dispatchers.IO) {
        val url = "${Config.SUPABASE_URL}/rest/v1/$table${if (query.isBlank()) "" else "?$query"}"
        val headers = baseHeaders(token).toMutableMap()
        prefer?.let { headers["Prefer"] = it }
        http(url, method, headers, body)
    }

    suspend fun rpc(fn: String, args: JsonObject, token: String?): RestResult =
        withContext(Dispatchers.IO) {
            val url = "${Config.SUPABASE_URL}/rest/v1/rpc/$fn"
            http(url, "POST", baseHeaders(token), json.encodeToString(JsonObject.serializer(), args))
        }

    suspend fun authPost(path: String, body: JsonObject, token: String? = null): RestResult =
        withContext(Dispatchers.IO) {
            val url = "${Config.SUPABASE_URL}/auth/v1/$path"
            http(url, "POST", baseHeaders(token), json.encodeToString(JsonObject.serializer(), body))
        }

    suspend fun authGet(path: String, token: String): RestResult = withContext(Dispatchers.IO) {
        val url = "${Config.SUPABASE_URL}/auth/v1/$path"
        http(url, "GET", baseHeaders(token), null)
    }

    suspend fun authPut(path: String, body: JsonObject, token: String): RestResult =
        withContext(Dispatchers.IO) {
            val url = "${Config.SUPABASE_URL}/auth/v1/$path"
            http(url, "PUT", baseHeaders(token), json.encodeToString(JsonObject.serializer(), body))
        }

    /** Raw upload to Supabase Storage. Returns the storage object path on success. */
    suspend fun storageUpload(
        bucket: String,
        objectPath: String,
        bytes: ByteArray,
        contentType: String,
        token: String,
    ): RestResult = withContext(Dispatchers.IO) {
        val url = "${Config.SUPABASE_URL}/storage/v1/object/$bucket/$objectPath"
        var con: HttpURLConnection? = null
        runCatching {
            con = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 30_000
                doOutput = true
                setRequestProperty("apikey", Config.SUPABASE_PUBLISHABLE_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", contentType)
                setRequestProperty("x-upsert", "true")
                outputStream.use { it.write(bytes) }
            }
            val code = con!!.responseCode
            val stream = if (code in 200..299) con!!.inputStream else con!!.errorStream
            RestResult(code, stream?.bufferedReader()?.readText())
        }.getOrElse { RestResult(-1, it.message) }.also { con?.disconnect() }
    }

    fun obj(vararg pairs: Pair<String, Any?>): JsonObject = buildJsonObject {
        pairs.forEach { (k, v) ->
            when (v) {
                null -> {}
                is String -> put(k, v)
                is Number -> put(k, v)
                is Boolean -> put(k, v)
                else -> put(k, v.toString())
            }
        }
    }
}
