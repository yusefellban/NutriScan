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
import iti.grad.nutriscan.domain.notification.usecase.ObserveNotificationPrefsUseCase
import iti.grad.nutriscan.domain.notification.usecase.ShouldNotifyWaterUseCase
import iti.grad.nutriscan.domain.water.usecase.ObserveTodayWaterUseCase
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
    private val observeTodayWater: ObserveTodayWaterUseCase,
    private val shouldNotifyWater: ShouldNotifyWaterUseCase,
    private val historyRecorder: INotificationHistoryRecorder,
) : CoroutineWorker(context, params) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val prefs = observePrefs().first()
        val water = observeTodayWater().first()
        if (!shouldNotifyWater(prefs, water, LocalTime.now())) return Result.success()

        val title = applicationContext.getString(R.string.notification_push_water_title)
        val body = applicationContext.getString(
            R.string.notification_push_water_body,
            water.glassCount,
            water.goalGlasses,
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
