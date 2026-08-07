package iti.grad.nutriscan.domain.user.usecase

import iti.grad.nutriscan.domain.user.repository.IUserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RestoreAccountUseCaseTest {

    private lateinit var userRepository: IUserRepository
    private lateinit var useCase: RestoreAccountUseCase

    @BeforeEach
    fun setup() {
        userRepository = mockk()
        useCase = RestoreAccountUseCase(userRepository)
    }

    @Test
    fun `invoke should call restoreAccount on repository and return success`() = runTest {
        coEvery { userRepository.restoreAccount() } returns Result.success(Unit)

        val result = useCase()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { userRepository.restoreAccount() }
    }

    @Test
    fun `invoke should return failure when repository fails`() = runTest {
        val exception = Exception("Network error")
        coEvery { userRepository.restoreAccount() } returns Result.failure(exception)

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { userRepository.restoreAccount() }
    }
}
