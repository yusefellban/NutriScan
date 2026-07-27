package iti.grad.nutriscan.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import iti.grad.nutriscan.notification.worker.BreakNotificationWorker
import iti.grad.nutriscan.notification.worker.FoodNotificationWorker
import iti.grad.nutriscan.notification.worker.NewsNotificationWorker
import iti.grad.nutriscan.notification.worker.QuoteNotificationWorker
import iti.grad.nutriscan.notification.worker.ScanNotificationWorker
import iti.grad.nutriscan.notification.worker.StepsNotificationWorker
import iti.grad.nutriscan.notification.worker.StreakNotificationWorker
import iti.grad.nutriscan.notification.worker.WaterNotificationWorker
import iti.grad.nutriscan.notification.worker.WorkoutNotificationWorker
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun scheduleAll(context: Context) {
        val workManager = WorkManager.getInstance(context)
        schedule<StepsNotificationWorker>(workManager, StepsNotificationWorker.WORK_NAME, 8, TimeUnit.HOURS) // ~3x/day
        schedule<WaterNotificationWorker>(workManager, WaterNotificationWorker.WORK_NAME, 2, TimeUnit.HOURS)
        schedule<WorkoutNotificationWorker>(workManager, WorkoutNotificationWorker.WORK_NAME, 1, TimeUnit.DAYS)
        schedule<FoodNotificationWorker>(workManager, FoodNotificationWorker.WORK_NAME, 1, TimeUnit.DAYS)
        schedule<NewsNotificationWorker>(workManager, NewsNotificationWorker.WORK_NAME, 1, TimeUnit.DAYS)
        schedule<QuoteNotificationWorker>(workManager, QuoteNotificationWorker.WORK_NAME, 1, TimeUnit.DAYS)
        schedule<ScanNotificationWorker>(workManager, ScanNotificationWorker.WORK_NAME, 1, TimeUnit.DAYS)
        schedule<StreakNotificationWorker>(workManager, StreakNotificationWorker.WORK_NAME, 1, TimeUnit.DAYS)
        schedule<BreakNotificationWorker>(workManager, BreakNotificationWorker.WORK_NAME, 2, TimeUnit.HOURS)
    }

    private inline fun <reified W : ListenableWorker> schedule(
        workManager: WorkManager,
        workName: String,
        interval: Long,
        unit: TimeUnit,
    ) {
        val request = PeriodicWorkRequestBuilder<W>(interval, unit).build()
        workManager.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
