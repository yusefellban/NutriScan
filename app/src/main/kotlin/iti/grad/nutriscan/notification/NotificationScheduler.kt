package iti.grad.nutriscan.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
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
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Every reminder is a 24-hour periodic work anchored to a wall-clock slot via an initial delay,
 * not an interval counted from whenever the app happened to start. That is what stops a fresh
 * install from firing all of them at once, and what keeps Water and Break from colliding.
 *
 * Slots are device-local: "9 AM" means 9 AM where the user is. Day-boundary logic elsewhere
 * stays Cairo-anchored via CairoDateProvider — these are different questions.
 */
@Singleton
class NotificationSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : INotificationScheduler {

    override fun scheduleAll() {
        val workManager = WorkManager.getInstance(context)
        // Installs from before the slot schedule have works under the bare type names. They are
        // never re-enqueued, so cancel them or they keep firing on the old two-hour interval.
        LEGACY_WORK_NAMES.forEach(workManager::cancelUniqueWork)

        val now = LocalDateTime.now()
        SLOTS.forEach { slot ->
            val request = PeriodicWorkRequest.Builder(slot.worker, 1, TimeUnit.DAYS)
                .setInitialDelay(NotificationSlots.minutesUntilNext(slot.time, now), TimeUnit.MINUTES)
                .setInputData(
                    workDataOf(NotificationSlots.KEY_SLOT_MINUTE_OF_DAY to slot.time.toSecondOfDay() / 60)
                )
                .build()
            // KEEP, not UPDATE: re-anchoring on every cold start would push the next run forward
            // for anyone who opens the app daily, and they would never get a reminder at all.
            workManager.enqueueUniquePeriodicWork(slot.workName, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }

    override fun cancelAll() {
        val workManager = WorkManager.getInstance(context)
        (LEGACY_WORK_NAMES + SLOTS.map { it.workName }).forEach(workManager::cancelUniqueWork)
    }

    private data class Slot(
        val workName: String,
        val worker: Class<out ListenableWorker>,
        val time: LocalTime,
    )

    private companion object {

        private fun slot(
            worker: Class<out ListenableWorker>,
            baseName: String,
            hour: Int,
            minute: Int,
        ) = Slot(
            workName = "%s_%02d%02d".format(baseName, hour, minute),
            worker = worker,
            time = LocalTime.of(hour, minute),
        )

        /**
         * Water runs four times at exactly four-hour spacing; Break three times at four-hour
         * spacing offset two hours from Water, so the two can never land together. Everything
         * else is once a day and mostly conditional, so a typical day is 7-9 actual posts.
         * Quiet hours (22:00-07:00) sit outside every slot and are now a backstop, not the
         * main defence.
         */
        val SLOTS = listOf(
            slot(QuoteNotificationWorker::class.java, QuoteNotificationWorker.WORK_NAME, 8, 30),
            slot(WaterNotificationWorker::class.java, WaterNotificationWorker.WORK_NAME, 9, 0),
            slot(BreakNotificationWorker::class.java, BreakNotificationWorker.WORK_NAME, 11, 0),
            slot(WaterNotificationWorker::class.java, WaterNotificationWorker.WORK_NAME, 13, 0),
            slot(BreakNotificationWorker::class.java, BreakNotificationWorker.WORK_NAME, 15, 0),
            slot(WaterNotificationWorker::class.java, WaterNotificationWorker.WORK_NAME, 17, 0),
            slot(NewsNotificationWorker::class.java, NewsNotificationWorker.WORK_NAME, 17, 30),
            slot(WorkoutNotificationWorker::class.java, WorkoutNotificationWorker.WORK_NAME, 18, 30),
            slot(BreakNotificationWorker::class.java, BreakNotificationWorker.WORK_NAME, 19, 0),
            slot(FoodNotificationWorker::class.java, FoodNotificationWorker.WORK_NAME, 19, 30),
            slot(StepsNotificationWorker::class.java, StepsNotificationWorker.WORK_NAME, 20, 30),
            slot(WaterNotificationWorker::class.java, WaterNotificationWorker.WORK_NAME, 21, 0),
            slot(ScanNotificationWorker::class.java, ScanNotificationWorker.WORK_NAME, 21, 15),
            slot(StreakNotificationWorker::class.java, StreakNotificationWorker.WORK_NAME, 21, 30),
        )

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
