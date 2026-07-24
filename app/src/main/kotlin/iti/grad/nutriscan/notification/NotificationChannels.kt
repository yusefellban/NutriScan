package iti.grad.nutriscan.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import iti.grad.nutriscan.domain.notification.model.NotificationType

object NotificationChannels {

    fun channelId(type: NotificationType): String = "channel_${type.name.lowercase()}"

    private fun channelName(type: NotificationType): String = when (type) {
        NotificationType.STEPS -> "Steps"
        NotificationType.WATER -> "Water"
        NotificationType.WORKOUT -> "Workout"
        NotificationType.FOOD -> "Food Log"
        NotificationType.NEWS -> "Health News"
        NotificationType.QUOTE -> "Health Quotes"
        NotificationType.SCAN -> "Scan Reminders"
        NotificationType.STREAK -> "Streak"
    }

    fun createAll(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        NotificationType.entries.forEach { type ->
            val existing = manager.getNotificationChannel(channelId(type))
            if (existing != null && existing.importance != NotificationManager.IMPORTANCE_HIGH) {
                manager.deleteNotificationChannel(channelId(type))
            }
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId(type),
                    channelName(type),
                    NotificationManager.IMPORTANCE_HIGH,
                )
            )
        }
    }
}
