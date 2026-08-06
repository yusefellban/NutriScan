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
import iti.grad.nutriscan.domain.notification.usecase.ShouldNotifyStepsUseCase
import iti.grad.nutriscan.domain.dailytracking.usecase.ObserveTodayDailyTrackingUseCase
import iti.grad.nutriscan.notification.NotificationChannels
import iti.grad.nutriscan.notification.NutriScanNotificationBuilder
import iti.grad.presentation.R
import kotlinx.coroutines.flow.first
import java.time.LocalTime

@HiltWorker
class StepsNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observePrefs: ObserveNotificationPrefsUseCase,
    private val observeTodayDailyTracking: ObserveTodayDailyTrackingUseCase,
    private val shouldNotifySteps: ShouldNotifyStepsUseCase,
    private val historyRecorder: INotificationHistoryRecorder,
    private val checkIfUserIsLoggedIn: CheckIfUserIsLoggedInUseCase,
) : CoroutineWorker(context, params) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        // Belt and braces: the scheduler cancels on logout, but work enqueued by an older
        // build — or a session ended by a failed token refresh — can still fire.
        if (!checkIfUserIsLoggedIn()) return Result.success()
        val prefs = observePrefs().first()
        // daily_tracking is the shared source of truth — the Calories screen shows this exact
        // number, and StepsSyncWorker keeps it fresh every 15 minutes.
        val steps = observeTodayDailyTracking().first().stepsCnt
        if (!shouldNotifySteps(prefs, steps, DAILY_GOAL, LocalTime.now())) return Result.success()

        val title = applicationContext.getString(R.string.notification_push_steps_title)
        val body = applicationContext.getString(R.string.notification_push_steps_body, steps, DAILY_GOAL)
        val notification = NutriScanNotificationBuilder.build(
            context = applicationContext,
            type = NotificationType.STEPS,
            title = title,
            body = body,
        )
        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationChannels.channelId(NotificationType.STEPS).hashCode(), notification)
        historyRecorder.record(NotificationType.STEPS, title, body)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "steps_notification_worker"
        private const val DAILY_GOAL = 10000
    }
}
