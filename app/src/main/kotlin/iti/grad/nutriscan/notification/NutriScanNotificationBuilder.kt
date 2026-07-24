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
            .setSmallIcon(PresentationR.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
}
