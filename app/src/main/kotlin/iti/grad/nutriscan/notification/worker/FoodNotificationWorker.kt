package iti.grad.nutriscan.notification.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import android.annotation.SuppressLint
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import iti.grad.nutriscan.domain.foodlog.usecase.ObserveTodayFoodLogUseCase
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
class FoodNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observePrefs: ObserveNotificationPrefsUseCase,
    private val observeTodayFoodLog: ObserveTodayFoodLogUseCase,
    private val isWithinQuietHours: IsWithinQuietHoursUseCase,
    private val historyRecorder: INotificationHistoryRecorder,
) : CoroutineWorker(context, params) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val prefs = observePrefs().first()
        if (!prefs.isEnabled(NotificationType.FOOD)) return Result.success()
        if (isWithinQuietHours(prefs, LocalTime.now())) return Result.success()
        if (LocalTime.now() < EVENING_NUDGE_START) return Result.success()

        val loggedToday = observeTodayFoodLog().first().isNotEmpty()
        if (loggedToday) return Result.success()

        val title = applicationContext.getString(R.string.notification_push_food_title)
        val body = applicationContext.getString(R.string.notification_push_food_body)
        val notification = NutriScanNotificationBuilder.build(
            context = applicationContext,
            type = NotificationType.FOOD,
            title = title,
            body = body,
        )
        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationChannels.channelId(NotificationType.FOOD).hashCode(), notification)
        historyRecorder.record(NotificationType.FOOD, title, body)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "food_notification_worker"
        private val EVENING_NUDGE_START: LocalTime = LocalTime.of(18, 0)
    }
}
