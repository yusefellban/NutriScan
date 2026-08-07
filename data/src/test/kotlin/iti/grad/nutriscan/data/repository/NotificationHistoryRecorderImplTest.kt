package iti.grad.nutriscan.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import iti.grad.nutriscan.data.db.dao.NotificationHistoryDao
import iti.grad.nutriscan.data.db.entity.NotificationHistoryEntity
import iti.grad.nutriscan.domain.notification.model.NotificationType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationHistoryRecorderImplTest {

    private val dao: NotificationHistoryDao = mockk()
    private val recorder = NotificationHistoryRecorderImpl(dao, UnconfinedTestDispatcher())

    @Test
    fun `record inserts into dao with correct mapping`() = runTest {
        val entitySlot = slot<NotificationHistoryEntity>()
        coEvery { dao.insert(capture(entitySlot)) } returns Unit

        recorder.record(NotificationType.WATER, "Drink Water", "Body text")

        coVerify(exactly = 1) { dao.insert(any()) }
        val captured = entitySlot.captured
        assertEquals("Drink Water", captured.title)
        assertEquals("Body text", captured.body)
        assertEquals(NotificationType.WATER.name, captured.type)
        assertEquals(false, captured.isRead) // Default is false
    }
}
