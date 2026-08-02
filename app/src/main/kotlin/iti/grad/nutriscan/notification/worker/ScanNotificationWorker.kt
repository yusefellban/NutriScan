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
import iti.grad.nutriscan.domain.scan.repository.IScanRepository
import iti.grad.nutriscan.notification.NotificationChannels
import iti.grad.nutriscan.notification.NutriScanNotificationBuilder
import iti.grad.presentation.R
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@HiltWorker
class ScanNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observePrefs: ObserveNotificationPrefsUseCase,
    private val scanRepository: IScanRepository,
    private val isWithinQuietHours: IsWithinQuietHoursUseCase,
    private val historyRecorder: INotificationHistoryRecorder,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = observePrefs().first()
        if (!prefs.isEnabled(NotificationType.SCAN)) return Result.success()
        if (isWithinQuietHours(prefs, LocalTime.now())) return Result.success()

        // ponytail: getLastScanDate() always returns null until scan history is persisted,
        // so this reminds the user once per day (subject to quiet hours), not just once.
        val lastScanDate = scanRepository.getLastScanDate() ?: return postReminder()
        val daysSinceLastScan = ChronoUnit.DAYS.between(lastScanDate, LocalDate.now())
        if (daysSinceLastScan < INACTIVITY_THRESHOLD_DAYS) return Result.success()

        return postReminder()
    }

    private suspend fun postReminder(): Result {
        val title = applicationContext.getString(R.string.notification_push_scan_title)
        val body = applicationContext.getString(R.string.notification_push_scan_body)
        val notification = NutriScanNotificationBuilder.build(
            context = applicationContext,
            type = NotificationType.SCAN,
            title = title,
            body = body,
        )
        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationChannels.channelId(NotificationType.SCAN).hashCode(), notification)
        historyRecorder.record(NotificationType.SCAN, title, body)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "scan_notification_worker"
        private const val INACTIVITY_THRESHOLD_DAYS = 3
    }
}
