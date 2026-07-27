package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.DailyTrackingDao
import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.data.remote.api.DailyTrackingApiService
import iti.grad.nutriscan.data.remote.dto.DailyTrackingMealRequestDto
import iti.grad.nutriscan.data.remote.dto.DailyTrackingRequestDto
import iti.grad.nutriscan.data.remote.dto.UpdateMealRequestDto
import iti.grad.nutriscan.data.repository.mapper.toDomain
import iti.grad.nutriscan.data.repository.mapper.toEntity
import iti.grad.nutriscan.data.repository.mapper.toRemoteSnapshot
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.common.CairoDateProvider
import iti.grad.nutriscan.domain.common.runCatchingCancellable
import iti.grad.nutriscan.domain.dailytracking.model.DailyTracking
import iti.grad.nutriscan.domain.dailytracking.model.DailyTrackingRemoteSnapshot
import iti.grad.nutriscan.domain.dailytracking.model.DailyTrackingSummary
import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import iti.grad.nutriscan.domain.streak.repository.IStreakRepository
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlin.math.roundToInt

private const val DEFAULT_TARGET_WATER_CNT = 8
private const val DEFAULT_WEIGHT_KG = 70.0
private const val STEP_KCAL_FACTOR = 0.0005
private const val RECONCILE_INTERVAL_MS = 20_000L

class DailyTrackingRepositoryImpl @Inject constructor(
    private val dao: DailyTrackingDao,
    private val api: DailyTrackingApiService,
    private val authRepository: IAuthRepository,
    private val userRepository: IUserRepository,
    private val streakRepository: IStreakRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : IDailyTrackingRepository {

    /** Polls [fetchAndSeedToday] every [RECONCILE_INTERVAL_MS] for as long as this flow is
     * collected, so water/steps logged on another device show up here without an app restart —
     * see [fetchAndSeedToday] for why an unsynced local edit is never clobbered by this. */
    override fun observeToday(): Flow<DailyTracking> = channelFlow {
        val userId = resolveUserId()
        fetchAndSeedToday()
        launch {
            while (isActive) {
                delay(RECONCILE_INTERVAL_MS)
                fetchAndSeedToday()
            }
        }
        todayTicker().flatMapLatest { date ->
            dao.observeByUserAndDate(userId, date.toString()).map { it?.toDomain() ?: defaultDailyTracking(date) }
        }.collect { send(it) }
    }.flowOn(ioDispatcher)

    /** Re-emits [CairoDateProvider.today()] immediately, then again at every Cairo-midnight
     * boundary, so [observeToday]'s [flatMapLatest] re-subscribes to the new day's row instead of
     * observing a fixed date string pinned at collection start — otherwise a Calories screen left
     * open across midnight would keep watching yesterday's now-stale row. */
    private fun todayTicker(): Flow<LocalDate> = flow {
        while (true) {
            val today = CairoDateProvider.today()
            emit(today)
            val now = ZonedDateTime.now(CairoDateProvider.ZONE)
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(CairoDateProvider.ZONE)
            delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(0))
        }
    }

    override suspend fun getByDate(date: LocalDate): Result<DailyTracking> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val userId = resolveUserId()
            dao.getByUserAndDate(userId, date.toString())?.toDomain() ?: defaultDailyTracking(date)
        }
    }

    override suspend fun getHistoryPage(page: Int, size: Int): Result<List<DailyTrackingSummary>> =
        withContext(ioDispatcher) {
            runCatchingCancellable { api.getHistoryPage(page, size).content.map { it.toDomain() } }
        }

    override suspend fun updateWaterCnt(waterCnt: Int): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val current = currentOrDefault()
            dao.upsert(current.copy(waterCnt = waterCnt, syncedToBackend = false).toEntity(resolveUserId()))
            if (waterCnt > 0) streakRepository.recomputeStreak()
        }
    }

    override suspend fun updateTargetWaterCnt(targetWaterCnt: Int): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val current = currentOrDefault()
            dao.upsert(current.copy(targetWaterCnt = targetWaterCnt, syncedToBackend = false).toEntity(resolveUserId()))
        }
    }

    override suspend fun updateStepsCnt(stepsCnt: Int): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val current = currentOrDefault()
            val weightKg = userRepository.getUserData().first()?.weightKg ?: DEFAULT_WEIGHT_KG
            val caloriesBurnedSteps = (stepsCnt * weightKg * STEP_KCAL_FACTOR).roundToInt()
            dao.upsert(
                current.copy(
                    stepsCnt = stepsCnt,
                    caloriesBurnedSteps = caloriesBurnedSteps,
                    syncedToBackend = false,
                ).toEntity(resolveUserId())
            )
            if (stepsCnt > 0) streakRepository.recomputeStreak()
        }
    }

    override suspend fun addExerciseWorkout(kcalBurned: Int, minutes: Int): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val current = currentOrDefault()
            dao.upsert(
                current.copy(
                    exerciseKcal = current.exerciseKcal + kcalBurned,
                    exerciseMinutes = current.exerciseMinutes + minutes,
                ).toEntity(resolveUserId())
            )
            streakRepository.recomputeStreak()
            Unit
        }
    }

    override suspend fun deleteDay(date: LocalDate): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            api.deleteDay(date.toString())
            dao.deleteByUserAndDate(resolveUserId(), date.toString())
        }
    }

    override suspend fun pushMeal(date: LocalDate, scanId: String, mealCnt: Int): Result<Unit> =
        withContext(ioDispatcher) {
            runCatchingCancellable {
                api.addMeal(date.toString(), DailyTrackingMealRequestDto(scanId = scanId, mealCnt = mealCnt))
            }
        }

    override suspend fun updateMeal(date: LocalDate, scanId: String, mealCnt: Int): Result<Unit> =
        withContext(ioDispatcher) {
            runCatchingCancellable {
                api.updateMeal(date.toString(), scanId, UpdateMealRequestDto(mealCnt = mealCnt))
            }
        }

    override suspend fun deleteMeal(date: LocalDate, scanId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable { api.deleteMeal(date.toString(), scanId) }
    }

    override suspend fun syncPendingDay(date: LocalDate): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val userId = resolveUserId()
            val entity = dao.getByUserAndDate(userId, date.toString()) ?: return@runCatchingCancellable
            if (entity.syncedToBackend) return@runCatchingCancellable

            api.updateDay(
                date.toString(),
                DailyTrackingRequestDto(
                    date = date.toString(),
                    targetWaterCnt = entity.targetWaterCnt,
                    waterCnt = entity.waterCnt,
                    stepsCnt = entity.stepsCnt,
                ),
            )
            dao.markSynced(userId, date.toString())
        }
    }

    /** Seeds/refreshes today's row from the backend — called once at login/cold-start, and
     * periodically by [observeToday] while it's collected so water/steps logged on another device
     * show up here without an app restart. Only overwrites Room when there's no local row yet, or
     * the local row is already [DailyTracking.syncedToBackend] — an unsynced local edit (not yet
     * pushed by the nightly worker) always wins until it syncs, so a pull never loses it. */
    override suspend fun fetchAndSeedToday(): Result<DailyTrackingRemoteSnapshot> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val userId = resolveUserId()
            val response = api.getToday()
            val snapshot = response.toRemoteSnapshot()
            val existing = dao.getByUserAndDate(userId, snapshot.date.toString())

            if (existing == null || existing.syncedToBackend) {
                val weightKg = userRepository.getUserData().first()?.weightKg ?: DEFAULT_WEIGHT_KG
                val caloriesBurnedSteps = (snapshot.stepsCnt * weightKg * STEP_KCAL_FACTOR).roundToInt()
                dao.upsert(
                    DailyTracking(
                        date = snapshot.date,
                        targetWaterCnt = snapshot.targetWaterCnt.takeIf { it > 0 } ?: DEFAULT_TARGET_WATER_CNT,
                        waterCnt = snapshot.waterCnt,
                        stepsCnt = snapshot.stepsCnt,
                        caloriesBurnedSteps = caloriesBurnedSteps,
                        exerciseKcal = existing?.exerciseKcal ?: 0,
                        exerciseMinutes = existing?.exerciseMinutes ?: 0,
                        syncedToBackend = true,
                    ).toEntity(userId)
                )
            }

            snapshot
        }
    }

    private suspend fun currentOrDefault(): DailyTracking =
        dao.getByUserAndDate(resolveUserId(), CairoDateProvider.today().toString())?.toDomain()
            ?: defaultDailyTracking(CairoDateProvider.today())

    private fun defaultDailyTracking(date: LocalDate) = DailyTracking(
        date = date,
        targetWaterCnt = DEFAULT_TARGET_WATER_CNT,
        waterCnt = 0,
        stepsCnt = 0,
        caloriesBurnedSteps = 0,
        exerciseKcal = 0,
        exerciseMinutes = 0,
        syncedToBackend = false,
    )

    private suspend fun resolveUserId(): String = authRepository.getCurrentUserId() ?: LOCAL_USER_ID

    private companion object {
        const val LOCAL_USER_ID = "local_device_user"
    }
}
