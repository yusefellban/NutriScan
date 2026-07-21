package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.local.datasource.IAuthTokenLocalDataSource
import iti.grad.nutriscan.data.remote.api.KeycloakApiService
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
    private lateinit var authTokenLocalDataSource: IAuthTokenLocalDataSource
    private lateinit var json: Json
    private lateinit var repository: AuthRepositoryImpl

    @BeforeEach
    fun setup() {
        remoteDataSource = mockk()
        keycloakApiService = mockk()
        authTokenLocalDataSource = mockk()
        json = Json { ignoreUnknownKeys = true }
        repository = AuthRepositoryImpl(
            remoteDataSource,
            keycloakApiService,
            authTokenLocalDataSource,
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
}
