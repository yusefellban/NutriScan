package iti.grad.nutriscan.notification.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import android.annotation.SuppressLint
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.repository.INotificationHistoryRecorder
import iti.grad.nutriscan.domain.notification.usecase.IsWithinQuietHoursUseCase
import iti.grad.nutriscan.domain.notification.usecase.ObserveNotificationPrefsUseCase
import iti.grad.nutriscan.notification.NotificationChannels
import iti.grad.nutriscan.notification.NutriScanNotificationBuilder
import iti.grad.presentation.R
import kotlinx.coroutines.flow.first
import java.time.LocalTime

@HiltWorker
class NewsNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observePrefs: ObserveNotificationPrefsUseCase,
    private val isWithinQuietHours: IsWithinQuietHoursUseCase,
    private val historyRecorder: INotificationHistoryRecorder,
) : CoroutineWorker(context, params) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val prefs = observePrefs().first()
        if (!prefs.isEnabled(NotificationType.NEWS)) return Result.success()
        if (isWithinQuietHours(prefs, LocalTime.now())) return Result.success()

        val title = applicationContext.getString(R.string.notification_push_news_title)
        val body = applicationContext.getString(R.string.notification_push_news_body)
        val notification = NutriScanNotificationBuilder.build(
            context = applicationContext,
            type = NotificationType.NEWS,
            title = title,
            body = body,
        )
        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationChannels.channelId(NotificationType.NEWS).hashCode(), notification)
        historyRecorder.record(NotificationType.NEWS, title, body)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "news_notification_worker"
    }
}
