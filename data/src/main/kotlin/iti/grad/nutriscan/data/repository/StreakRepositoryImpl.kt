package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.DailyTrackingDao
import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.data.db.dao.StreakDao
import iti.grad.nutriscan.data.db.entity.StreakEntity
import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.common.runCatchingCancellable
import iti.grad.nutriscan.data.remote.datasource.IUserRemoteDataSource

import iti.grad.nutriscan.domain.streak.repository.IStreakRepository
import iti.grad.nutriscan.domain.user.repository.IUserRepository
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
    private val authRepository: IAuthRepository,
    private val userRemoteDataSource: IUserRemoteDataSource,
    private val userRepository: IUserRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : IStreakRepository {

    override fun observeStreak(): Flow<Int> = flow {
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            emit(0)
            return@flow
        }
        emitAll(
            streakDao.observe(userId).map { entity ->
                entity?.currentStreak ?: 0
            }
        )
    }.flowOn(ioDispatcher)

    override suspend fun syncDailyStreak(): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            userRemoteDataSource.updateDailyStreak()
            userRepository.fetchAndSyncProfile().getOrThrow()
        }
    }

    /** Fails rather than using a shared device-local id — that fallback is how one account's
     * streak became visible to the next. Callers wrap this in runCatchingCancellable. */
    private suspend fun resolveUserId(): String = authRepository.getCurrentUserId()
        ?: error("No authenticated user - per-user data is unavailable until sign-in completes")

}
