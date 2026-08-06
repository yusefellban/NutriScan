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
import iti.grad.nutriscan.domain.foodlog.usecase.ObserveTodayFoodLogUseCase
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.repository.INotificationHistoryRecorder
import iti.grad.nutriscan.domain.notification.usecase.ObserveNotificationPrefsUseCase
import iti.grad.nutriscan.domain.notification.usecase.ShouldNotifyStreakUseCase
import iti.grad.nutriscan.domain.streak.usecase.ObserveStreakUseCase
import iti.grad.nutriscan.notification.NotificationChannels
import iti.grad.nutriscan.notification.NotificationSlots
import iti.grad.nutriscan.notification.NutriScanNotificationBuilder
import iti.grad.presentation.R
import kotlinx.coroutines.flow.first
import java.time.LocalTime

@HiltWorker
class StreakNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observePrefs: ObserveNotificationPrefsUseCase,
    private val observeTodayFoodLog: ObserveTodayFoodLogUseCase,
    private val observeStreak: ObserveStreakUseCase,
    private val shouldNotifyStreak: ShouldNotifyStreakUseCase,
    private val historyRecorder: INotificationHistoryRecorder,
    private val checkIfUserIsLoggedIn: CheckIfUserIsLoggedInUseCase,
) : CoroutineWorker(context, params) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        // Belt and braces: the scheduler cancels on logout, but work enqueued by an older
        // build — or a session ended by a failed token refresh — can still fire.
        if (!checkIfUserIsLoggedIn()) return Result.success()
        // WorkManager is best-effort — Doze can defer a slot for hours, and a reminder for a
        // moment that has passed is just noise.
        if (NotificationSlots.isTooLate(
                inputData.getInt(NotificationSlots.KEY_SLOT_MINUTE_OF_DAY, -1),
                LocalTime.now(),
            )
        ) {
            return Result.success()
        }
        val prefs = observePrefs().first()
        val loggedToday = observeTodayFoodLog().first().isNotEmpty()
        val streak = observeStreak().first()
        if (!shouldNotifyStreak(prefs, loggedToday, streak.currentStreak > 0, LocalTime.now())) {
            return Result.success()
        }

        val title = applicationContext.getString(R.string.notification_push_streak_title)
        val body = applicationContext.getString(R.string.notification_push_streak_body, streak.currentStreak)
        val notification = NutriScanNotificationBuilder.build(
            context = applicationContext,
            type = NotificationType.STREAK,
            title = title,
            body = body,
        )
        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationChannels.channelId(NotificationType.STREAK).hashCode(), notification)
        historyRecorder.record(NotificationType.STREAK, title, body)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "streak_notification_worker"
    }
}
