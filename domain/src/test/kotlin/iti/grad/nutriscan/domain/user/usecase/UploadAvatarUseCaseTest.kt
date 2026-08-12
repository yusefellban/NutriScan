package iti.grad.nutriscan.domain.user.usecase

import iti.grad.nutriscan.domain.user.repository.IUserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class UploadAvatarUseCaseTest {

    private lateinit var userRepository: IUserRepository
    private lateinit var useCase: UploadAvatarUseCase

    @BeforeEach
    fun setup() {
        userRepository = mockk()
        useCase = UploadAvatarUseCase(userRepository)
    }

    @Test
    fun `invoke delegates to repository and forwards success`() = runTest {
        val file = File("avatar.jpg")
        coEvery { userRepository.uploadAvatar(file) } returns Result.success(Unit)

        val result = useCase(file)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { userRepository.uploadAvatar(file) }
    }

    @Test
    fun `invoke forwards repository failure unchanged`() = runTest {
        val file = File("avatar.jpg")
        val error = Exception("Upload failed")
        coEvery { userRepository.uploadAvatar(file) } returns Result.failure(error)

        val result = useCase(file)

        assertTrue(result.isFailure)
        assert(result.exceptionOrNull() === error)
    }
}
