package iti.grad.nutriscan.notification

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.repository.ITestNotificationSender
import iti.grad.presentation.R
import javax.inject.Inject

class TestNotificationSenderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ITestNotificationSender {

    override fun sendNow() {
        val notification = NutriScanNotificationBuilder.build(
            context = context,
            type = NotificationType.QUOTE,
            title = context.getString(R.string.notification_push_test_title),
            body = context.getString(R.string.notification_push_test_body),
        )
        NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
    }

    override fun sendAllTypesNow() {
        val manager = NotificationManagerCompat.from(context)
        contentFor(NotificationType.entries).forEach { (type, title, body) ->
            manager.notify(TEST_NOTIFICATION_ID + type.ordinal, NutriScanNotificationBuilder.build(context, type, title, body))
        }
    }

    private fun contentFor(types: List<NotificationType>) = types.map { type ->
        when (type) {
            NotificationType.STEPS -> Triple(
                type,
                context.getString(R.string.notification_push_steps_title),
                context.getString(R.string.notification_push_steps_body, 4200, 10000),
            )
            NotificationType.WATER -> Triple(
                type,
                context.getString(R.string.notification_push_water_title),
                context.getString(R.string.notification_push_water_body, 4, 8),
            )
            NotificationType.WORKOUT -> Triple(
                type,
                context.getString(R.string.notification_push_workout_title),
                context.getString(R.string.notification_push_workout_body),
            )
            NotificationType.FOOD -> Triple(
                type,
                context.getString(R.string.notification_push_food_title),
                context.getString(R.string.notification_push_food_body),
            )
            NotificationType.NEWS -> Triple(
                type,
                context.getString(R.string.notification_push_news_title),
                context.getString(R.string.notification_push_news_body),
            )
            NotificationType.QUOTE -> Triple(
                type,
                context.getString(R.string.notification_push_quote_title),
                context.getString(R.string.notification_push_test_body),
            )
            NotificationType.SCAN -> Triple(
                type,
                context.getString(R.string.notification_push_scan_title),
                context.getString(R.string.notification_push_scan_body),
            )
            NotificationType.STREAK -> Triple(
                type,
                context.getString(R.string.notification_push_streak_title),
                context.getString(R.string.notification_push_streak_body, 3),
            )
            NotificationType.BREAK -> Triple(
                type,
                context.getString(R.string.notification_push_break_title),
                context.getString(R.string.notification_push_break_body),
            )
        }
    }

    private companion object {
        const val TEST_NOTIFICATION_ID = 999_001
    }
}
