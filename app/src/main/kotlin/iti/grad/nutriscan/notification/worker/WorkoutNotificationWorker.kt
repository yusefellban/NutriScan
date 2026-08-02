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
import iti.grad.nutriscan.domain.notification.usecase.ObserveNotificationPrefsUseCase
import iti.grad.nutriscan.domain.notification.usecase.ShouldNotifyWorkoutUseCase
import iti.grad.nutriscan.domain.workout.usecase.ObserveWorkoutStatusUseCase
import iti.grad.nutriscan.notification.NotificationChannels
import iti.grad.nutriscan.notification.NutriScanNotificationBuilder
import iti.grad.presentation.R
import kotlinx.coroutines.flow.first
import java.time.LocalTime

@HiltWorker
class WorkoutNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observePrefs: ObserveNotificationPrefsUseCase,
    private val observeWorkoutStatus: ObserveWorkoutStatusUseCase,
    private val shouldNotifyWorkout: ShouldNotifyWorkoutUseCase,
    private val historyRecorder: INotificationHistoryRecorder,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = observePrefs().first()
        val done = observeWorkoutStatus().first()
        if (!shouldNotifyWorkout(prefs, done, LocalTime.now())) return Result.success()

        val title = applicationContext.getString(R.string.notification_push_workout_title)
        val body = applicationContext.getString(R.string.notification_push_workout_body)
        val notification = NutriScanNotificationBuilder.build(
            context = applicationContext,
            type = NotificationType.WORKOUT,
            title = title,
            body = body,
        )
        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationChannels.channelId(NotificationType.WORKOUT).hashCode(), notification)
        historyRecorder.record(NotificationType.WORKOUT, title, body)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "workout_notification_worker"
    }
}
