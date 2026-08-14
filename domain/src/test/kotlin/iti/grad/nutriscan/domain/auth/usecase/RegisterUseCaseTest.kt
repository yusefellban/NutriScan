package iti.grad.nutriscan.domain.auth.usecase

import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.onboarding.repository.IOnboardingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RegisterUseCaseTest {

    private lateinit var authRepository: IAuthRepository
    private lateinit var onboardingRepository: IOnboardingRepository
    private lateinit var useCase: RegisterUseCase

    @BeforeEach
    fun setup() {
        authRepository = mockk()
        onboardingRepository = mockk(relaxed = true)
        useCase = RegisterUseCase(authRepository, onboardingRepository)
    }

    @Test
    fun `invoke with valid details should return success and clear preferences`() = runTest {
        val firstName = "Test"
        val lastName = "User"
        val email = "test@example.com"
        val password = "Password123"
        coEvery { authRepository.register(firstName, lastName, email, password) } returns Result.success(Unit)

        val result = useCase(firstName, lastName, email, password)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { authRepository.register(firstName, lastName, email, password) }
        coVerify(exactly = 1) { onboardingRepository.clearUserSpecificPreferences() }
    }

    @Test
    fun `invoke with failure from repository should return failure and not clear preferences`() = runTest {
        val firstName = "Test"
        val lastName = "User"
        val email = "test@example.com"
        val password = "Password123"
        val exception = Exception("Email already exists")
        coEvery { authRepository.register(firstName, lastName, email, password) } returns Result.failure(exception)

        val result = useCase(firstName, lastName, email, password)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { authRepository.register(firstName, lastName, email, password) }
        coVerify(exactly = 0) { onboardingRepository.clearUserSpecificPreferences() }
    }
}
