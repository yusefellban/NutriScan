package iti.grad.nutriscan.data.remote.interceptor

import iti.grad.nutriscan.data.local.datasource.TokenManager
import iti.grad.nutriscan.data.remote.api.TokenRefreshApiService
import iti.grad.nutriscan.data.remote.api.TokenRefreshResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NutriScanAuthenticatorTest {

    private lateinit var tokenManager: TokenManager
    private lateinit var tokenRefreshApiService: TokenRefreshApiService
    private lateinit var authenticator: NutriScanAuthenticator

    @BeforeEach
    fun setup() {
        tokenManager = mockk(relaxed = true)
        tokenRefreshApiService = mockk(relaxed = true)
        authenticator = NutriScanAuthenticator(
            tokenManager = tokenManager,
            tokenRefreshApiServiceProvider = { tokenRefreshApiService }
        )
    }

    private fun createDummyResponse(): Response {
        val request = Request.Builder()
            .url("https://api.nutriscan.dev/test")
            .build()
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .build()
    }

    @Test
    fun `authenticate should return null if no refresh token exists`() = runBlocking {
        every { tokenManager.getAccessToken() } returns "old_access"
        every { tokenManager.getRefreshToken() } returns null

        val response = createDummyResponse()
        val result = authenticator.authenticate(null, response)

        assertNull(result)
        coVerify(exactly = 0) { tokenRefreshApiService.refreshToken(any(), any(), any()) }
    }

    @Test
    fun `authenticate should refresh token and return new request if successful`() = runBlocking {
        every { tokenManager.getAccessToken() } returns "old_access"
        every { tokenManager.getRefreshToken() } returns "old_refresh"

        val newAccess = "new_access"
        val newRefresh = "new_refresh"
        val refreshResponse = retrofit2.Response.success(
            TokenRefreshResponse(newAccess, newRefresh, 3600)
        )

        coEvery { tokenRefreshApiService.refreshToken(refreshToken = "old_refresh") } returns refreshResponse

        val response = createDummyResponse()
        val result = authenticator.authenticate(null, response)

        assertNotNull(result)
        assertEquals("Bearer $newAccess", result?.header("Authorization"))
        
        coVerify(exactly = 1) { tokenManager.saveTokens(newAccess, newRefresh) }
    }

    @Test
    fun `authenticate should logout and clear tokens if refresh fails`() = runBlocking {
        every { tokenManager.getAccessToken() } returns "old_access"
        every { tokenManager.getRefreshToken() } returns "old_refresh"

        val errorResponse = retrofit2.Response.error<TokenRefreshResponse>(
            400,
            okhttp3.ResponseBody.create(null, "invalid_grant")
        )

        coEvery { tokenRefreshApiService.refreshToken(refreshToken = "old_refresh") } returns errorResponse

        val response = createDummyResponse()
        val result = authenticator.authenticate(null, response)

        assertNull(result)
        
        coVerify(exactly = 1) { tokenRefreshApiService.logout(refreshToken = "old_refresh") }
        coVerify(exactly = 1) { tokenManager.clearTokens() }
    }
}
