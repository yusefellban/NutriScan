package iti.grad.nutriscan.notification.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
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

/** Periodic wellness nudge (move around / drink water / rest your eyes) — unlike Water/Steps,
 * this isn't gated on being behind a goal, it's just a standing reminder every couple hours. */
@HiltWorker
class BreakNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observePrefs: ObserveNotificationPrefsUseCase,
    private val isWithinQuietHours: IsWithinQuietHoursUseCase,
    private val historyRecorder: INotificationHistoryRecorder,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = observePrefs().first()
        if (!prefs.isEnabled(NotificationType.BREAK)) return Result.success()
        if (isWithinQuietHours(prefs, LocalTime.now())) return Result.success()

        val title = applicationContext.getString(R.string.notification_push_break_title)
        val body = applicationContext.getString(R.string.notification_push_break_body)
        val notification = NutriScanNotificationBuilder.build(
            context = applicationContext,
            type = NotificationType.BREAK,
            title = title,
            body = body,
        )
        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationChannels.channelId(NotificationType.BREAK).hashCode(), notification)
        historyRecorder.record(NotificationType.BREAK, title, body)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "break_notification_worker"
    }
}
