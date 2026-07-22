package iti.grad.nutriscan.domain.auth.usecase

import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ForgotPasswordUseCaseTest {

    private lateinit var authRepository: IAuthRepository
    private lateinit var useCase: ForgotPasswordUseCase

    @BeforeEach
    fun setup() {
        authRepository = mockk()
        useCase = ForgotPasswordUseCase(authRepository)
    }

    @Test
    fun `invoke with valid email should return success`() = runTest {
        val email = "test@example.com"
        coEvery { authRepository.forgotPassword(email) } returns Result.success(Unit)

        val result = useCase(email)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { authRepository.forgotPassword(email) }
    }

    @Test
    fun `invoke with failure from repository should return failure`() = runTest {
        val email = "test@example.com"
        val exception = Exception("Network error")
        coEvery { authRepository.forgotPassword(email) } returns Result.failure(exception)

        val result = useCase(email)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { authRepository.forgotPassword(email) }
    }
}
