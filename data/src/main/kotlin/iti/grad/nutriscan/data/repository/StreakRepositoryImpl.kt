package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.DailyTrackingDao
import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.data.db.dao.StreakDao
import iti.grad.nutriscan.data.db.entity.StreakEntity
import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.common.runCatchingCancellable
import iti.grad.nutriscan.data.remote.datasource.IUserRemoteDataSource
import iti.grad.nutriscan.domain.streak.model.StreakInfo
import iti.grad.nutriscan.domain.streak.repository.IStreakRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import timber.log.Timber

class StreakRepositoryImpl @Inject constructor(
    private val streakDao: StreakDao,
    private val foodLogDao: FoodLogDao,
    private val dailyTrackingDao: DailyTrackingDao,
    private val authRepository: IAuthRepository,
    private val userRemoteDataSource: IUserRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : IStreakRepository {

    override fun observeStreak(): Flow<StreakInfo> = flow {
        emitAll(
            streakDao.observe(resolveUserId()).map { entity ->
                StreakInfo(
                    currentStreak = entity?.currentStreak ?: 0,
                    longestStreak = entity?.longestStreak ?: 0,
                )
            }
        )
    }.flowOn(ioDispatcher)

    override suspend fun syncDailyStreak(): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            userRemoteDataSource.updateDailyStreak()
        }
    }

    /** Fails rather than using a shared device-local id — that fallback is how one account's
     * streak became visible to the next. Callers wrap this in runCatchingCancellable. */
    private suspend fun resolveUserId(): String = authRepository.getCurrentUserId()
        ?: error("No authenticated user - per-user data is unavailable until sign-in completes")

}
