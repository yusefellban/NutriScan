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

class ResendVerificationEmailUseCaseTest {

    private lateinit var authRepository: IAuthRepository
    private lateinit var useCase: ResendVerificationEmailUseCase

    @BeforeEach
    fun setup() {
        authRepository = mockk()
        useCase = ResendVerificationEmailUseCase(authRepository)
    }

    @Test
    fun `invoke with valid email should return success`() = runTest {
        val email = "test@example.com"
        coEvery { authRepository.resendVerificationEmail(email) } returns Result.success(Unit)

        val result = useCase(email)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { authRepository.resendVerificationEmail(email) }
    }

    @Test
    fun `invoke with failure from repository should return failure`() = runTest {
        val email = "test@example.com"
        val exception = Exception("User not found")
        coEvery { authRepository.resendVerificationEmail(email) } returns Result.failure(exception)

        val result = useCase(email)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { authRepository.resendVerificationEmail(email) }
    }
}
