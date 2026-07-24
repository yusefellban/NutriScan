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

    private companion object {
        const val TEST_NOTIFICATION_ID = 999_001
    }
}
