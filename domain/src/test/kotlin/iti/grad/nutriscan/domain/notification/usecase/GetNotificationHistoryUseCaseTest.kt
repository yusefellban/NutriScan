package iti.grad.nutriscan.domain.notification.usecase

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import iti.grad.nutriscan.domain.notification.model.NotificationHistoryItem
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.repository.INotificationHistoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetNotificationHistoryUseCaseTest {

    private val repository: INotificationHistoryRepository = mockk()
    private val useCase = GetNotificationHistoryUseCase(repository)

    @Test
    fun `invoke delegates to repository and returns flow`() = runTest {
        val dummyList = listOf(
            NotificationHistoryItem(1L, "T1", "B1", NotificationType.WATER, 1000L, false)
        )
        every { repository.getNotifications() } returns flowOf(dummyList)

        val result = useCase().first()
        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)

        verify(exactly = 1) { repository.getNotifications() }
    }
}
