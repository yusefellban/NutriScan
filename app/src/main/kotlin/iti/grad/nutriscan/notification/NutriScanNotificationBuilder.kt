package iti.grad.nutriscan.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import iti.grad.nutriscan.MainActivity
import iti.grad.nutriscan.R
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.presentation.R as PresentationR

object NutriScanNotificationBuilder {

    fun build(context: Context, type: NotificationType, title: String, body: String): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_TYPE, type.name)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            type.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, NotificationChannels.channelId(type))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(iconFor(type))
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    /** Each type gets a small icon that matches its content instead of the generic bell,
     * so the status bar / notification shade signal what kind of nudge just arrived. */
    private fun iconFor(type: NotificationType): Int = when (type) {
        NotificationType.STEPS -> PresentationR.drawable.steps
        NotificationType.WATER -> PresentationR.drawable.ic_water_dot
        NotificationType.WORKOUT -> PresentationR.drawable.ic_notification_workout
        NotificationType.FOOD -> PresentationR.drawable.calorie
        NotificationType.NEWS -> PresentationR.drawable.ic_health_news
        NotificationType.QUOTE -> PresentationR.drawable.ic_notification_quote
        NotificationType.SCAN -> PresentationR.drawable.ic_scan
        NotificationType.STREAK -> PresentationR.drawable.ic_fire
        NotificationType.BREAK -> PresentationR.drawable.hour
    }

    const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
}
