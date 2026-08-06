package iti.grad.nutriscan.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import iti.grad.nutriscan.domain.notification.repository.INotificationScheduler
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : INotificationScheduler {

    override fun scheduleAll() {
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

    override fun cancelAll() {
        val workManager = WorkManager.getInstance(context)
        LEGACY_WORK_NAMES.forEach(workManager::cancelUniqueWork)
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

    private companion object {
        /** The pre-slot work names. Task 10 replaces the schedule wholesale; these stay listed
         * so already-installed apps get their old works cancelled rather than orphaned. */
        val LEGACY_WORK_NAMES = listOf(
            StepsNotificationWorker.WORK_NAME,
            WaterNotificationWorker.WORK_NAME,
            WorkoutNotificationWorker.WORK_NAME,
            FoodNotificationWorker.WORK_NAME,
            NewsNotificationWorker.WORK_NAME,
            QuoteNotificationWorker.WORK_NAME,
            ScanNotificationWorker.WORK_NAME,
            StreakNotificationWorker.WORK_NAME,
            BreakNotificationWorker.WORK_NAME,
        )
    }
}
