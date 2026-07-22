package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.local.datasource.TokenManager
import iti.grad.nutriscan.data.remote.api.KeycloakApiService
import iti.grad.nutriscan.data.remote.api.TokenRefreshApiService
import iti.grad.nutriscan.data.remote.datasource.IAuthRemoteDataSource
import iti.grad.nutriscan.data.remote.dto.ForgotPasswordRequestDto
import iti.grad.nutriscan.data.remote.dto.MessageResponseDto
import iti.grad.nutriscan.data.remote.dto.RegisterRequestDto
import iti.grad.nutriscan.data.remote.dto.RegisterResponseDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Response

class AuthRepositoryImplTest {

    private lateinit var remoteDataSource: IAuthRemoteDataSource
    private lateinit var keycloakApiService: KeycloakApiService
    private lateinit var tokenRefreshApiService: TokenRefreshApiService
    private lateinit var tokenManager: TokenManager
    private lateinit var json: Json
    private lateinit var repository: AuthRepositoryImpl

    @BeforeEach
    fun setup() {
        remoteDataSource = mockk()
        keycloakApiService = mockk()
        tokenRefreshApiService = mockk(relaxed = true)
        tokenManager = mockk(relaxed = true)
        json = Json { ignoreUnknownKeys = true }
        repository = AuthRepositoryImpl(
            remoteDataSource,
            keycloakApiService,
            tokenRefreshApiService,
            tokenManager,
            json
        )
    }

    @Test
    fun `forgotPassword should return success on successful api call`() = runTest {
        val email = "test@example.com"
        val request = ForgotPasswordRequestDto(email)
        val apiResponse = Response.success(MessageResponseDto("success"))
        
        coEvery { remoteDataSource.forgotPassword(request) } returns apiResponse

        val result = repository.forgotPassword(email)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { remoteDataSource.forgotPassword(request) }
    }

    @Test
    fun `register should return success on successful api call`() = runTest {
        val email = "test@example.com"
        val password = "pwd"
        val request = RegisterRequestDto(
            firstName = "string",
            lastName = "string",
            email = email,
            username = email,
            password = password,
            dateOfBirth = "2000-01-01",
            gender = "MALE",
            heightCm = 170.0,
            weightKg = 70.0,
            allergies = emptyList(),
            diseases = emptyList()
        )
        val apiResponse = Response.success(RegisterResponseDto("success", false))
        
        coEvery { remoteDataSource.register(request) } returns apiResponse

        val result = repository.register(email, password)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { remoteDataSource.register(request) }
    }

    @Test
    fun `logout should call tokenRefreshApiService and clear tokens`() = runTest {
        val refreshToken = "dummy_refresh_token"
        coEvery { tokenManager.getRefreshToken() } returns refreshToken
        coEvery { tokenRefreshApiService.logout(refreshToken = refreshToken) } returns Response.success(Unit)

        val result = repository.logout()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { tokenRefreshApiService.logout(refreshToken = refreshToken) }
        coVerify(exactly = 1) { tokenManager.clearTokens() }
    }

    @Test
    fun `getCurrentUserId decodes the sub claim from the stored id token`() = runTest {
        val token = fakeJwtWithSubject("user-123")
        coEvery { tokenManager.getIdToken() } returns token

        val userId = repository.getCurrentUserId()

        org.junit.jupiter.api.Assertions.assertEquals("user-123", userId)
    }

    @Test
    fun `getCurrentUserId returns null when there is no stored id token`() = runTest {
        coEvery { tokenManager.getIdToken() } returns null

        val userId = repository.getCurrentUserId()

        org.junit.jupiter.api.Assertions.assertNull(userId)
    }

    private fun fakeJwtWithSubject(subject: String): String {
        val encoder = java.util.Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("""{"alg":"none"}""".toByteArray())
        val payload = encoder.encodeToString("""{"sub":"$subject"}""".toByteArray())
        return "$header.$payload.fake-signature"
    }
}
