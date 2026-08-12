package iti.grad.nutriscan.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for locally persisted notification history.
 *
 * Each row represents one notification that was shown to the user.
 * [type] stores the [NotificationType.name] string so no type converter is needed.
 * [timestamp] is epoch millis (System.currentTimeMillis()) for ordering and relative-time display.
 */
@Entity(tableName = "notification_history")
data class NotificationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val type: String,
    val timestamp: Long,
    val isRead: Boolean = false,
)
