package iti.grad.nutriscan.notification.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import iti.grad.nutriscan.domain.notification.model.NotificationType
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
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = observePrefs().first()
        val water = observeTodayWater().first()
        if (!shouldNotifyWater(prefs, water, LocalTime.now())) return Result.success()

        val notification = NutriScanNotificationBuilder.build(
            context = applicationContext,
            type = NotificationType.WATER,
            title = applicationContext.getString(R.string.notification_push_water_title),
            body = applicationContext.getString(
                R.string.notification_push_water_body,
                water.glassCount,
                water.goalGlasses,
            ),
        )
        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationChannels.channelId(NotificationType.WATER).hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "water_notification_worker"
    }
}
