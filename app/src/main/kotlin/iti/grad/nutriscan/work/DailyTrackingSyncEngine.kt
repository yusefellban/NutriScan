package iti.grad.nutriscan.work

import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import iti.grad.nutriscan.domain.dailytracking.usecase.SyncPendingDailyTrackingUseCase
import iti.grad.nutriscan.domain.scan.repository.ISavedScanRepository
import java.time.LocalDate

/**
 * Core logic of the periodic (every 6h) sync job, pulled out of [DailyTrackingSyncWorker] so it
 * can be unit tested with plain fakes/mocks — [androidx.work.CoroutineWorker] needs a real
 * Android [Context] to construct, which a JVM unit test can't cheaply provide without Robolectric.
 */
class DailyTrackingSyncEngine(
    private val syncPendingDailyTracking: SyncPendingDailyTrackingUseCase,
    private val dailyTrackingRepository: IDailyTrackingRepository,
    private val foodLogDao: FoodLogDao,
    private val authRepository: IAuthRepository,
    private val savedScanRepository: ISavedScanRepository,
) {
    /** Pushes [date]'s daily-tracking (today's in-progress water/steps, so a reinstall never loses
     * more than one run's worth of data) and retries any pending meal/saved-scan push-or-delete,
     * scoped to the currently authenticated user (or the local-device fallback) so a stale
     * login/logout never pushes or deletes another account's pending rows. Returns true only if
     * everything succeeded (worker should retry otherwise). */
    suspend fun sync(date: LocalDate): Boolean {
        val daySyncResult = syncPendingDailyTracking(date)
        val userId = authRepository.getCurrentUserId() ?: LOCAL_USER_ID

        var anyMealRetryFailed = false
        for (entry in foodLogDao.getPendingSyncEntries(userId)) {
            val loggedDate = LocalDate.parse(entry.loggedDate)
            val scanId = entry.productId ?: entry.id
            val syncResult = if (entry.deleted) {
                dailyTrackingRepository.deleteMeal(loggedDate, scanId)
            } else {
                dailyTrackingRepository.pushMeal(loggedDate, scanId, mealCnt = 1)
            }
            if (syncResult.isSuccess) {
                if (entry.deleted) foodLogDao.hardDelete(entry.id) else foodLogDao.clearPendingSync(entry.id)
            } else {
                anyMealRetryFailed = true
            }
        }

        val savedScanRetryResult = savedScanRepository.retryPendingSync()

        return daySyncResult.isSuccess && !anyMealRetryFailed && savedScanRetryResult.isSuccess
    }

    private companion object {
        const val LOCAL_USER_ID = "local_device_user"
    }
}
