package iti.grad.nutriscan.notification.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import iti.grad.nutriscan.domain.foodlog.usecase.ObserveTodayFoodLogUseCase
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.usecase.ObserveNotificationPrefsUseCase
import iti.grad.nutriscan.domain.notification.usecase.ShouldNotifyStreakUseCase
import iti.grad.nutriscan.domain.streak.usecase.ObserveStreakUseCase
import iti.grad.nutriscan.notification.NotificationChannels
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
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = observePrefs().first()
        val loggedToday = observeTodayFoodLog().first().isNotEmpty()
        if (!shouldNotifyStreak(prefs, loggedToday, LocalTime.now())) return Result.success()

        val streak = observeStreak().first()
        val notification = NutriScanNotificationBuilder.build(
            context = applicationContext,
            type = NotificationType.STREAK,
            title = applicationContext.getString(R.string.notification_push_streak_title),
            body = applicationContext.getString(R.string.notification_push_streak_body, streak.currentStreak),
        )
        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationChannels.channelId(NotificationType.STREAK).hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "streak_notification_worker"
    }
}
