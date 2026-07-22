package iti.grad.nutriscan.data.remote.datasource

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.data.remote.api.AuthApiService
import iti.grad.nutriscan.data.remote.dto.ForgotPasswordRequestDto
import iti.grad.nutriscan.data.remote.dto.MessageResponseDto
import iti.grad.nutriscan.data.remote.dto.RegisterRequestDto
import iti.grad.nutriscan.data.remote.dto.RegisterResponseDto
import iti.grad.nutriscan.data.remote.dto.ResendVerificationRequestDto
import iti.grad.nutriscan.data.remote.dto.ResendVerificationResponseDto
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Response

class AuthRemoteDataSourceImplTest {

    private lateinit var authApiService: AuthApiService
    private lateinit var dataSource: AuthRemoteDataSourceImpl

    @BeforeEach
    fun setup() {
        authApiService = mockk()
        dataSource = AuthRemoteDataSourceImpl(authApiService)
    }

    @Test
    fun `register should call api service and return response`() = runTest {
        val request = RegisterRequestDto(
            firstName = "test",
            lastName = "test",
            email = "test@test.com",
            username = "test",
            password = "pwd",
            dateOfBirth = "2000-01-01",
            gender = "MALE",
            heightCm = 180.0,
            weightKg = 80.0,
            allergies = emptyList(),
            diseases = emptyList()
        )
        val expectedResponse = Response.success(RegisterResponseDto("msg", false))
        
        coEvery { authApiService.register(request) } returns expectedResponse

        val result = dataSource.register(request)

        assertEquals(expectedResponse, result)
        coVerify(exactly = 1) { authApiService.register(request) }
    }

    @Test
    fun `forgotPassword should call api service and return response`() = runTest {
        val request = ForgotPasswordRequestDto("test@test.com")
        val expectedResponse = Response.success(MessageResponseDto("msg"))
        
        coEvery { authApiService.forgotPassword(request) } returns expectedResponse

        val result = dataSource.forgotPassword(request)

        assertEquals(expectedResponse, result)
        coVerify(exactly = 1) { authApiService.forgotPassword(request) }
    }

    @Test
    fun `resendVerification should call api service and return response`() = runTest {
        val request = ResendVerificationRequestDto("test@test.com")
        val expectedResponse = Response.success(ResendVerificationResponseDto("msg"))
        
        coEvery { authApiService.resendVerification(request) } returns expectedResponse

        val result = dataSource.resendVerification(request)

        assertEquals(expectedResponse, result)
        coVerify(exactly = 1) { authApiService.resendVerification(request) }
    }
}
