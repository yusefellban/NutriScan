package iti.grad.nutriscan.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import iti.grad.nutriscan.data.db.entity.NotificationHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `notification_history` table.
 *
 * All reads that back the UI are reactive [Flow] queries — Room automatically
 * re-emits when the underlying table changes.  Writes are one-shot [suspend] funs.
 */
@Dao
interface NotificationHistoryDao {

    /** Observe all notifications, newest first. */
    @Query("SELECT * FROM notification_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<NotificationHistoryEntity>>

    /** Insert a new notification record. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: NotificationHistoryEntity)

    /** Delete a single notification by its ID. */
    @Query("DELETE FROM notification_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Delete all notification history. */
    @Query("DELETE FROM notification_history")
    suspend fun deleteAll()

    /** Mark a single notification as read. */
    @Query("UPDATE notification_history SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    /** Observe the count of unread notifications (for potential badge display). */
    @Query("SELECT COUNT(*) FROM notification_history WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>
}
