package iti.grad.nutriscan.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object DailyTrackingSyncScheduler {
    private const val WORK_NAME = "daily_tracking_sync"
    private const val SYNC_INTERVAL_HOURS = 6L

    /** Backs up today's in-progress water/steps/saved-scans every 6h, so a reinstall never loses
     * more than one interval's worth of data — not anchored to Cairo midnight since it now syncs
     * the current (not the prior finalized) day. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<DailyTrackingSyncWorker>(SYNC_INTERVAL_HOURS, TimeUnit.HOURS).build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
