package iti.grad.nutriscan.data.local.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.Base64

class JwtDecoderTest {

    private fun encode(json: String): String = Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())

    @Test
    fun `extracts the sub claim from a valid token`() {
        val token = "${encode("""{"alg":"none"}""")}.${encode("""{"sub":"user-42"}""")}.sig"

        assertEquals("user-42", JwtDecoder.extractSubjectClaim(token))
    }

    @Test
    fun `returns null for a null token`() {
        assertNull(JwtDecoder.extractSubjectClaim(null))
    }

    @Test
    fun `returns null for a blank token`() {
        assertNull(JwtDecoder.extractSubjectClaim(""))
    }

    @Test
    fun `returns null for a token with only one segment`() {
        assertNull(JwtDecoder.extractSubjectClaim("nodots"))
    }

    @Test
    fun `returns null when the payload segment is not valid base64`() {
        assertNull(JwtDecoder.extractSubjectClaim("header.###not-base64###.sig"))
    }

    @Test
    fun `returns null when the decoded payload is not valid JSON`() {
        val token = "${encode("""{"alg":"none"}""")}.${encode("not json")}.sig"

        assertNull(JwtDecoder.extractSubjectClaim(token))
    }

    @Test
    fun `returns null when the payload has no sub claim`() {
        val token = "${encode("""{"alg":"none"}""")}.${encode("""{"name":"someone"}""")}.sig"

        assertNull(JwtDecoder.extractSubjectClaim(token))
    }
}
