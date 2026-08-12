package iti.grad.nutriscan.domain.notification.model

/**
 * Domain model for a single notification history entry.
 *
 * Pure Kotlin — no Android/Room dependencies.
 * [type] maps to [NotificationType] for icon/label resolution in the presentation layer.
 * [timestamp] is epoch millis for sorting and relative-time display.
 */
data class NotificationHistoryItem(
    val id: Long = 0,
    val title: String,
    val body: String,
    val type: NotificationType,
    val timestamp: Long,
    val isRead: Boolean = false,
)
