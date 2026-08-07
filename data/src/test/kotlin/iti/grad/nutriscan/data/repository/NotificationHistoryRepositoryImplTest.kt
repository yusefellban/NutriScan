package iti.grad.nutriscan.data.repository

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import iti.grad.nutriscan.data.db.dao.NotificationHistoryDao
import iti.grad.nutriscan.data.db.entity.NotificationHistoryEntity
import iti.grad.nutriscan.domain.notification.model.NotificationHistoryItem
import iti.grad.nutriscan.domain.notification.model.NotificationType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationHistoryRepositoryImplTest {

    private val dao: NotificationHistoryDao = mockk()
    private val repository = NotificationHistoryRepositoryImpl(dao)

    @Test
    fun `getNotifications maps entities to domain models correctly`() = runTest {
        val entities = listOf(
            NotificationHistoryEntity(1L, "T1", "B1", "WATER", 1000L, false),
            NotificationHistoryEntity(2L, "T2", "B2", "UNKNOWN_TYPE", 2000L, true) // Testing fallback to QUOTE
        )
        every { dao.getAll() } returns flowOf(entities)

        repository.getNotifications().test {
            val items = awaitItem()
            assertEquals(2, items.size)
            
            assertEquals(1L, items[0].id)
            assertEquals(NotificationType.WATER, items[0].type)
            assertEquals(false, items[0].isRead)

            assertEquals(2L, items[1].id)
            assertEquals(NotificationType.QUOTE, items[1].type) // Fallback for invalid type
            assertEquals(true, items[1].isRead)
            
            awaitComplete()
        }
    }

    @Test
    fun `saveNotification maps domain model to entity and delegates to dao`() = runTest {
        val item = NotificationHistoryItem(1L, "T", "B", NotificationType.SCAN, 1000L, true)
        val entitySlot = slot<NotificationHistoryEntity>()
        coEvery { dao.insert(capture(entitySlot)) } returns Unit

        repository.saveNotification(item)

        coVerify(exactly = 1) { dao.insert(any()) }
        val captured = entitySlot.captured
        assertEquals(1L, captured.id)
        assertEquals("T", captured.title)
        assertEquals("B", captured.body)
        assertEquals("SCAN", captured.type)
        assertEquals(1000L, captured.timestamp)
        assertEquals(true, captured.isRead)
    }

    @Test
    fun `deleteNotification delegates to dao`() = runTest {
        coEvery { dao.deleteById(1L) } returns Unit
        
        repository.deleteNotification(1L)
        
        coVerify(exactly = 1) { dao.deleteById(1L) }
    }

    @Test
    fun `clearAllNotifications delegates to dao`() = runTest {
        coEvery { dao.deleteAll() } returns Unit
        
        repository.clearAllNotifications()
        
        coVerify(exactly = 1) { dao.deleteAll() }
    }

    @Test
    fun `markAsRead delegates to dao`() = runTest {
        coEvery { dao.markAsRead(1L) } returns Unit
        
        repository.markAsRead(1L)
        
        coVerify(exactly = 1) { dao.markAsRead(1L) }
    }

    @Test
    fun `getUnreadCount delegates to dao`() = runTest {
        every { dao.getUnreadCount() } returns flowOf(5)
        
        repository.getUnreadCount().test {
            assertEquals(5, awaitItem())
            awaitComplete()
        }
    }
}
