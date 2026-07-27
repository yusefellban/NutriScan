package iti.grad.nutriscan.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.common.CairoDateProvider
import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import iti.grad.nutriscan.domain.dailytracking.usecase.SyncPendingDailyTrackingUseCase

@HiltWorker
class DailyTrackingSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncPendingDailyTracking: SyncPendingDailyTrackingUseCase,
    private val dailyTrackingRepository: IDailyTrackingRepository,
    private val foodLogDao: FoodLogDao,
    private val authRepository: IAuthRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val engine = DailyTrackingSyncEngine(syncPendingDailyTracking, dailyTrackingRepository, foodLogDao, authRepository)
        val succeeded = engine.sync(CairoDateProvider.today().minusDays(1))
        return if (succeeded) Result.success() else Result.retry()
    }
}
