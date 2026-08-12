package iti.grad.nutriscan.domain.notification.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.domain.notification.repository.INotificationHistoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MarkNotificationReadUseCaseTest {

    private val repository: INotificationHistoryRepository = mockk()
    private val useCase = MarkNotificationReadUseCase(repository)

    @Test
    fun `invoke delegates to repository`() = runTest {
        coEvery { repository.markAsRead(1L) } returns Unit

        useCase(1L)

        coVerify(exactly = 1) { repository.markAsRead(1L) }
    }
}
