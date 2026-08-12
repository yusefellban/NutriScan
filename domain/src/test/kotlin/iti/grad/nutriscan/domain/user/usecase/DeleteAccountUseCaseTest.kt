package iti.grad.nutriscan.domain.user.usecase

import iti.grad.nutriscan.domain.user.model.AccountDeletionInfo
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DeleteAccountUseCaseTest {

    private lateinit var userRepository: IUserRepository
    private lateinit var useCase: DeleteAccountUseCase

    @BeforeEach
    fun setup() {
        userRepository = mockk()
        useCase = DeleteAccountUseCase(userRepository)
    }

    @Test
    fun `invoke should call deleteAccount on repository and return success`() = runTest {
        val expectedInfo = AccountDeletionInfo("2026-08-22", 15)
        coEvery { userRepository.deleteAccount() } returns Result.success(expectedInfo)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(expectedInfo, result.getOrNull())
        coVerify(exactly = 1) { userRepository.deleteAccount() }
    }

    @Test
    fun `invoke should return failure when repository fails`() = runTest {
        val exception = Exception("Network error")
        coEvery { userRepository.deleteAccount() } returns Result.failure(exception)

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { userRepository.deleteAccount() }
    }
}
