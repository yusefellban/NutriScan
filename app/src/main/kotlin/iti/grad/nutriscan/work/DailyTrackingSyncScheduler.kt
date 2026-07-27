package iti.grad.nutriscan.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import iti.grad.nutriscan.domain.common.CairoDateProvider
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object DailyTrackingSyncScheduler {
    private const val WORK_NAME = "daily_tracking_sync"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<DailyTrackingSyncWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntilNextCairoMidnight(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun millisUntilNextCairoMidnight(): Long {
        val now = ZonedDateTime.now(CairoDateProvider.ZONE)
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(CairoDateProvider.ZONE)
        return java.time.Duration.between(now, nextMidnight).toMillis()
    }
}
