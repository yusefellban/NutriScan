package iti.grad.nutriscan.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import iti.grad.nutriscan.data.db.NutriScanDatabase
import iti.grad.nutriscan.data.db.entity.NotificationHistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationHistoryDaoTest {

    private lateinit var db: NutriScanDatabase
    private lateinit var dao: NotificationHistoryDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NutriScanDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.notificationHistoryDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetAll() = runBlocking {
        val entity1 = NotificationHistoryEntity(
            id = 1,
            title = "Title 1",
            body = "Body 1",
            type = "SCAN",
            timestamp = 1000L,
            isRead = false
        )
        val entity2 = NotificationHistoryEntity(
            id = 2,
            title = "Title 2",
            body = "Body 2",
            type = "WATER",
            timestamp = 2000L, // Newer
            isRead = true
        )

        dao.insert(entity1)
        dao.insert(entity2)

        val list = dao.getAll().first()
        assertEquals(2, list.size)
        // Should be ordered by timestamp DESC
        assertEquals(2L, list[0].id)
        assertEquals(1L, list[1].id)
    }

    @Test
    fun deleteById() = runBlocking {
        val entity = NotificationHistoryEntity(id = 1, title = "T", body = "B", type = "T", timestamp = 1)
        dao.insert(entity)
        
        dao.deleteById(1)

        val list = dao.getAll().first()
        assertEquals(0, list.size)
    }

    @Test
    fun deleteAll() = runBlocking {
        dao.insert(NotificationHistoryEntity(id = 1, title = "T", body = "B", type = "T", timestamp = 1))
        dao.insert(NotificationHistoryEntity(id = 2, title = "T", body = "B", type = "T", timestamp = 2))
        
        dao.deleteAll()

        val list = dao.getAll().first()
        assertEquals(0, list.size)
    }

    @Test
    fun markAsRead() = runBlocking {
        val entity = NotificationHistoryEntity(id = 1, title = "T", body = "B", type = "T", timestamp = 1, isRead = false)
        dao.insert(entity)
        
        dao.markAsRead(1)

        val list = dao.getAll().first()
        assertEquals(true, list[0].isRead)
    }

    @Test
    fun getUnreadCount() = runBlocking {
        dao.insert(NotificationHistoryEntity(id = 1, title = "T", body = "B", type = "T", timestamp = 1, isRead = false))
        dao.insert(NotificationHistoryEntity(id = 2, title = "T", body = "B", type = "T", timestamp = 2, isRead = false))
        dao.insert(NotificationHistoryEntity(id = 3, title = "T", body = "B", type = "T", timestamp = 3, isRead = true))
        
        val count = dao.getUnreadCount().first()
        assertEquals(2, count)
    }
}
