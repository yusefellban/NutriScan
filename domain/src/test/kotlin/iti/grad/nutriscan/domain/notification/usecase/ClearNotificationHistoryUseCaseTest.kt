package iti.grad.nutriscan.domain.notification.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.domain.notification.repository.INotificationHistoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClearNotificationHistoryUseCaseTest {

    private val repository: INotificationHistoryRepository = mockk()
    private val useCase = ClearNotificationHistoryUseCase(repository)

    @Test
    fun `invoke delegates to repository`() = runTest {
        coEvery { repository.clearAllNotifications() } returns Unit

        useCase()

        coVerify(exactly = 1) { repository.clearAllNotifications() }
    }
}
