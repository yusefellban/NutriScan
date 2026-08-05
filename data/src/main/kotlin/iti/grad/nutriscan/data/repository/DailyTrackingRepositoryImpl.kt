package iti.grad.nutriscan.data.repository

import android.util.Log
import iti.grad.nutriscan.data.db.dao.DailyTrackingDao
import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.data.remote.api.DailyTrackingApiService
import iti.grad.nutriscan.data.remote.dto.DailyTrackingMealRequestDto
import iti.grad.nutriscan.data.remote.dto.DailyTrackingRequestDto
import iti.grad.nutriscan.data.remote.dto.UpdateMealRequestDto
import iti.grad.nutriscan.data.repository.mapper.toDomain
import iti.grad.nutriscan.data.repository.mapper.toEntity
import iti.grad.nutriscan.data.repository.mapper.toDaySummary
import iti.grad.nutriscan.data.repository.mapper.toRemoteSnapshot
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.common.CairoDateProvider
import iti.grad.nutriscan.domain.common.runCatchingCancellable
import iti.grad.nutriscan.domain.dailytracking.model.DailyTracking
import iti.grad.nutriscan.domain.dailytracking.model.DailyTrackingHistoryPage
import iti.grad.nutriscan.domain.dailytracking.model.DailyTrackingRemoteSnapshot
import iti.grad.nutriscan.domain.dailytracking.model.DailyTrackingSummary
import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import iti.grad.nutriscan.domain.streak.repository.IStreakRepository
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
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
private const val RECONCILE_INTERVAL_MS = 60_000L

/** Coalesces a burst of water/steps writes into one PATCH — long enough to absorb tapping several
 * cups in a row, short enough that a second device sees the change quickly. */
private const val PUSH_DEBOUNCE_MS = 2_000L
private const val SHARE_STOP_TIMEOUT_MS = 5_000L

class DailyTrackingRepositoryImpl @Inject constructor(
    private val dao: DailyTrackingDao,
    private val api: DailyTrackingApiService,
    private val authRepository: IAuthRepository,
    private val userRepository: IUserRepository,
    private val streakRepository: IStreakRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : IDailyTrackingRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    /** Latest debounced PATCH. Filling 8 water cups fires 8 local writes but should cost one
     * request, so each new write cancels the previous pending push — see [pushDayInBackground].
     * Nothing is lost if it's cancelled: the row stays `syncedToBackend = false` and the next
     * write, [syncAllPendingDays], or the periodic worker picks it up. */
    private var pendingPushJob: Job? = null

    /** Polls [fetchAndSeedToday] every [RECONCILE_INTERVAL_MS] for as long as at least one
     * collector is subscribed, so water/steps logged on another device show up here without an
     * app restart — see [fetchAndSeedToday] for why an unsynced local edit is never clobbered by
     * this. [shareIn] multicasts this single poll loop to every collector — [DailyTrackingRepositoryImpl]
     * is a singleton, so without it, two screens observing today's tracking at once would each
     * spin up their own independent poller. */
    private val todayFlow: Flow<DailyTracking> = channelFlow {
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
    }.flowOn(ioDispatcher).shareIn(repositoryScope, SharingStarted.WhileSubscribed(SHARE_STOP_TIMEOUT_MS), replay = 1)

    override fun observeToday(): Flow<DailyTracking> = todayFlow

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

    override suspend fun getHistoryPage(page: Int, size: Int): Result<DailyTrackingHistoryPage> =
        withContext(ioDispatcher) {
            runCatchingCancellable {
                val response = api.getHistoryPage(page, size)
                DailyTrackingHistoryPage(
                    entries = response.content.map { it.toDomain() },
                    isLastPage = response.last,
                    currentPage = response.number,
                )
            }
        }

    override suspend fun getRemoteDaySummary(date: LocalDate): Result<DailyTrackingSummary> =
        withContext(ioDispatcher) {
            runCatchingCancellable { api.getByDate(date.toString()).toDaySummary() }
        }

    /** Fires [syncPendingDay] in the background right after a local water/steps/target write,
     * instead of waiting for [DailyTrackingSyncScheduler]'s next 6h run — otherwise a second
     * device on the same account could show stale data for up to 6h. Runs on [repositoryScope]
     * (not the caller's coroutine) so a slow/failed push never blocks or fails the local write;
     * [syncPendingDay] already wraps its own network call in [runCatchingCancellable], so a
     * failure here is silently retried by the nightly worker as before. */
    private fun pushDayInBackground(date: LocalDate) {
        pendingPushJob?.cancel()
        pendingPushJob = repositoryScope.launch {
            delay(PUSH_DEBOUNCE_MS)
            syncPendingDay(date)
        }
    }

    override suspend fun updateWaterCnt(waterCnt: Int): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val current = currentOrDefault()
            dao.upsert(current.copy(waterCnt = waterCnt, syncedToBackend = false).toEntity(resolveUserId()))
            if (waterCnt > 0) streakRepository.recomputeStreak()
            pushDayInBackground(CairoDateProvider.today())
        }
    }

    override suspend fun updateTargetWaterCnt(targetWaterCnt: Int): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val current = currentOrDefault()
            dao.upsert(current.copy(targetWaterCnt = targetWaterCnt, syncedToBackend = false).toEntity(resolveUserId()))
            pushDayInBackground(CairoDateProvider.today())
        }
    }

    /** A day's step count only ever grows until midnight, so a lower reading is never newer truth —
     * it means this device isn't the one that took the steps. Sensor.TYPE_STEP_COUNTER is
     * device-local (cumulative since *that* device's reboot, offset by a per-device baseline — see
     * StepsRepositoryImpl), so a second phone, a reinstall, or an emulator all start reporting from
     * 0 for the same account. Without this guard that 0 is pushed straight to the backend and wipes
     * the real count. Dropping the smaller value keeps the highest reading any device has seen,
     * which is the closest thing to the truth available without per-device step storage. A genuine
     * reset at midnight is unaffected: the new day is a different row, starting from 0 again. */
    override suspend fun updateStepsCnt(stepsCnt: Int): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val current = currentOrDefault()
            if (stepsCnt <= current.stepsCnt) return@runCatchingCancellable
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
            pushDayInBackground(CairoDateProvider.today())
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
            Log.d(TAG, "POST /daily-tracking/$date/meals scanId=$scanId mealCnt=$mealCnt")
            runCatchingCancellable {
                api.addMeal(date.toString(), DailyTrackingMealRequestDto(scanId = scanId, mealCnt = mealCnt))
            }.onFailure { Log.e(TAG, "POST /daily-tracking/$date/meals FAILED scanId=$scanId", it) }
        }

    override suspend fun updateMeal(date: LocalDate, scanId: String, mealCnt: Int): Result<Unit> =
        withContext(ioDispatcher) {
            Log.d(TAG, "PUT /daily-tracking/$date/meals/$scanId mealCnt=$mealCnt")
            runCatchingCancellable {
                api.updateMeal(date.toString(), scanId, UpdateMealRequestDto(mealCnt = mealCnt))
            }.onFailure { Log.e(TAG, "PUT /daily-tracking/$date/meals/$scanId FAILED", it) }
        }

    override suspend fun deleteMeal(date: LocalDate, scanId: String): Result<Unit> = withContext(ioDispatcher) {
        Log.d(TAG, "DELETE /daily-tracking/$date/meals/$scanId")
        runCatchingCancellable { api.deleteMeal(date.toString(), scanId) }
            .onFailure { Log.e(TAG, "DELETE /daily-tracking/$date/meals/$scanId FAILED", it) }
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

    override suspend fun syncAllPendingDays(): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val pending = dao.getUnsyncedDays(resolveUserId())
            Log.d(TAG, "syncAllPendingDays: ${pending.size} unsynced day(s) ${pending.map { it.date }}")
            val failures = pending.count { syncPendingDay(LocalDate.parse(it.date)).isFailure }
            if (failures > 0) error("$failures of ${pending.size} pending day(s) failed to sync")
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
            Log.d(
                TAG,
                "GET /daily-tracking/today -> date=${snapshot.date} water=${snapshot.waterCnt}/${snapshot.targetWaterCnt} " +
                    "steps=${snapshot.stepsCnt} meals=${snapshot.meals.size} " +
                    snapshot.meals.joinToString(prefix = "[", postfix = "]") { "${it.scanId}x${it.mealCnt}" }
            )

            if (existing == null || existing.syncedToBackend) {
                val weightKg = userRepository.getUserData().first()?.weightKg ?: DEFAULT_WEIGHT_KG
                // Same monotonic rule as updateStepsCnt, in the pull direction: if this device's
                // sensor is already ahead of the backend, seeding the lower remote value would
                // walk the gauge backwards until the next push caught up.
                val stepsCnt = maxOf(snapshot.stepsCnt, existing?.stepsCnt ?: 0)
                val caloriesBurnedSteps = (stepsCnt * weightKg * STEP_KCAL_FACTOR).roundToInt()
                dao.upsert(
                    DailyTracking(
                        date = snapshot.date,
                        targetWaterCnt = snapshot.targetWaterCnt.takeIf { it > 0 } ?: DEFAULT_TARGET_WATER_CNT,
                        waterCnt = snapshot.waterCnt,
                        stepsCnt = stepsCnt,
                        caloriesBurnedSteps = caloriesBurnedSteps,
                        exerciseKcal = existing?.exerciseKcal ?: 0,
                        exerciseMinutes = existing?.exerciseMinutes ?: 0,
                        // Keeping a locally-higher step count means the backend is now behind, so
                        // the row still owes a push — marking it synced here would strand the
                        // higher value on this device forever.
                        syncedToBackend = stepsCnt == snapshot.stepsCnt,
                    ).toEntity(userId)
                )
                Log.d(TAG, "seeded Room from backend (local row was ${if (existing == null) "absent" else "already synced"})")
            } else {
                Log.d(TAG, "kept local unsynced row (water=${existing.waterCnt} steps=${existing.stepsCnt}) — local edit wins until it pushes")
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

    private suspend fun resolveUserId(): String = authRepository.getCurrentUserId()
        ?: error("No authenticated user - per-user data is unavailable until sign-in completes")

    private companion object {

        /** Grep logcat for this to trace the daily-tracking backend calls. Raw HTTP bodies are
         * logged separately by OkHttp's BODY-level interceptor (debug only, see NetworkModule). */
        const val TAG = "DailyTrackingSync"
    }
}
