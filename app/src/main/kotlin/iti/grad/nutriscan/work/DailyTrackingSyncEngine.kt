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
        // Then every *other* day still owing a push. A day that ended unsynced — logged just
        // before midnight, or pushed while offline — was previously never retried, because this
        // job only ever asked for the current date. That left a permanent hole in the history the
        // backend can serve back for past days.
        val backlogResult = dailyTrackingRepository.syncAllPendingDays()
        // No signed-in user means there is nothing of anyone's to push. Previously this fell back
        // to a shared device-local id and synced whatever sat in that bucket, which is exactly how
        // one account's rows reached another's.
        val userId = authRepository.getCurrentUserId() ?: return true

        var anyMealRetryFailed = false
        for (entry in foodLogDao.getPendingSyncEntries(userId)) {
            val loggedDate = LocalDate.parse(entry.loggedDate)
            val scanId = entry.productId ?: entry.id
            val syncResult = when {
                entry.deleted -> dailyTrackingRepository.deleteMeal(loggedDate, scanId)
                entry.backendCreated -> dailyTrackingRepository.updateMeal(loggedDate, scanId, entry.mealCnt)
                else -> dailyTrackingRepository.pushMeal(loggedDate, scanId, entry.mealCnt)
            }
            if (syncResult.isSuccess) {
                when {
                    entry.deleted -> foodLogDao.hardDelete(entry.id)
                    entry.backendCreated -> foodLogDao.clearPendingSync(entry.id)
                    else -> {
                        foodLogDao.markBackendCreated(entry.id)
                        foodLogDao.clearPendingSync(entry.id)
                    }
                }
            } else {
                anyMealRetryFailed = true
            }
        }

        val savedScanRetryResult = savedScanRepository.retryPendingSync()

        return daySyncResult.isSuccess &&
            backlogResult.isSuccess &&
            !anyMealRetryFailed &&
            savedScanRetryResult.isSuccess
    }

}
