package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.DailyTrackingDao
import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.data.db.dao.StreakDao
import iti.grad.nutriscan.data.db.entity.StreakEntity
import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.common.runCatchingCancellable
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

class StreakRepositoryImpl @Inject constructor(
    private val streakDao: StreakDao,
    private val foodLogDao: FoodLogDao,
    private val dailyTrackingDao: DailyTrackingDao,
    private val authRepository: IAuthRepository,
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

    override suspend fun recomputeStreak(): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val today = LocalDate.now()
            val userId = resolveUserId()
            val loggedFood = foodLogDao.observeByUserAndDate(userId, today.toString())
                .first().isNotEmpty()
            val tracking = dailyTrackingDao.getByUserAndDate(userId, today.toString())
            val trackedActivity = tracking != null &&
                (tracking.waterCnt > 0 || tracking.stepsCnt > 0 || tracking.exerciseMinutes > 0)
            if (!loggedFood && !trackedActivity) return@runCatchingCancellable

            val existing = streakDao.get(userId)
            val lastActive = existing?.lastActiveDate?.let(LocalDate::parse)
            val newStreak = when {
                lastActive == today -> existing?.currentStreak ?: 1
                lastActive == today.minusDays(1) -> (existing?.currentStreak ?: 0) + 1
                else -> 1
            }
            streakDao.upsert(
                StreakEntity(
                    userId = userId,
                    currentStreak = newStreak,
                    longestStreak = maxOf(newStreak, existing?.longestStreak ?: 0),
                    lastActiveDate = today.toString(),
                )
            )
        }
    }

    /** Fails rather than using a shared device-local id — that fallback is how one account's
     * streak became visible to the next. Callers wrap this in runCatchingCancellable. */
    private suspend fun resolveUserId(): String = authRepository.getCurrentUserId()
        ?: error("No authenticated user - per-user data is unavailable until sign-in completes")

}
