package iti.grad.nutriscan.data.local.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

/**
 * Minimal, pure-JVM JWT payload reader — no signature verification (the token is only ever
 * read back from our own encrypted local storage, never trusted from an external source here).
 */
object JwtDecoder {

    fun extractClaim(jwt: String?, claimKey: String): String? {
        if (jwt.isNullOrBlank()) return null
        val parts = jwt.split(".")
        if (parts.size < 2) return null
        return try {
            val payloadJson = decodeBase64Url(parts[1])
            Json.parseToJsonElement(payloadJson).jsonObject[claimKey]?.jsonPrimitive?.content
        } catch (_: Exception) {
            null
        }
    }

    fun extractSubjectClaim(jwt: String?): String? = extractClaim(jwt, "sub")

    private fun decodeBase64Url(segment: String): String {
        val padded = when (segment.length % 4) {
            2 -> "$segment=="
            3 -> "$segment="
            else -> segment
        }
        return String(Base64.getUrlDecoder().decode(padded))
    }
}
