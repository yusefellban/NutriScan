package iti.grad.nutriscan.notification.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import android.annotation.SuppressLint
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import iti.grad.nutriscan.domain.auth.usecase.CheckIfUserIsLoggedInUseCase
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.repository.INotificationHistoryRecorder
import iti.grad.nutriscan.domain.notification.usecase.ObserveNotificationPrefsUseCase
import iti.grad.nutriscan.domain.notification.usecase.ShouldNotifyWaterUseCase
import iti.grad.nutriscan.domain.dailytracking.usecase.ObserveTodayDailyTrackingUseCase
import iti.grad.nutriscan.notification.NotificationChannels
import iti.grad.nutriscan.notification.NutriScanNotificationBuilder
import iti.grad.presentation.R
import kotlinx.coroutines.flow.first
import java.time.LocalTime

@HiltWorker
class WaterNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observePrefs: ObserveNotificationPrefsUseCase,
    private val observeTodayDailyTracking: ObserveTodayDailyTrackingUseCase,
    private val shouldNotifyWater: ShouldNotifyWaterUseCase,
    private val historyRecorder: INotificationHistoryRecorder,
    private val checkIfUserIsLoggedIn: CheckIfUserIsLoggedInUseCase,
) : CoroutineWorker(context, params) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        // Belt and braces: the scheduler cancels on logout, but work enqueued by an older
        // build — or a session ended by a failed token refresh — can still fire.
        if (!checkIfUserIsLoggedIn()) return Result.success()
        val prefs = observePrefs().first()
        // daily_tracking is what the Calories screen shows. The old water_log table this used
        // to read was orphaned and always reported 0/8.
        val tracking = observeTodayDailyTracking().first()
        if (!shouldNotifyWater(prefs, tracking.waterCnt, tracking.targetWaterCnt, LocalTime.now())) {
            return Result.success()
        }

        val title = applicationContext.getString(R.string.notification_push_water_title)
        val body = applicationContext.getString(
            R.string.notification_push_water_body,
            tracking.waterCnt,
            tracking.targetWaterCnt,
        )
        val notification = NutriScanNotificationBuilder.build(
            context = applicationContext,
            type = NotificationType.WATER,
            title = title,
            body = body,
        )
        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationChannels.channelId(NotificationType.WATER).hashCode(), notification)
        historyRecorder.record(NotificationType.WATER, title, body)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "water_notification_worker"
    }
}
