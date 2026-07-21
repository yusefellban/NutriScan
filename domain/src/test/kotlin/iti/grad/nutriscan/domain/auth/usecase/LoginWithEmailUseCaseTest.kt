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

class LoginWithEmailUseCaseTest {

    private lateinit var authRepository: IAuthRepository
    private lateinit var useCase: LoginWithEmailUseCase

    @BeforeEach
    fun setup() {
        authRepository = mockk()
        useCase = LoginWithEmailUseCase(authRepository)
    }

    @Test
    fun `invoke with valid credentials should return success`() = runTest {
        val email = "test@example.com"
        val password = "Password123"
        coEvery { authRepository.loginWithEmail(email, password) } returns Result.success(Unit)

        val result = useCase(email, password)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { authRepository.loginWithEmail(email, password) }
    }

    @Test
    fun `invoke with failure from repository should return failure`() = runTest {
        val email = "test@example.com"
        val password = "wrong_password"
        val exception = Exception("Invalid credentials")
        coEvery { authRepository.loginWithEmail(email, password) } returns Result.failure(exception)

        val result = useCase(email, password)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { authRepository.loginWithEmail(email, password) }
    }
}
