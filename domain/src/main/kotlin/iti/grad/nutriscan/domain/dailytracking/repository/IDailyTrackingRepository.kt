package iti.grad.nutriscan.domain.dailytracking.repository

import iti.grad.nutriscan.domain.dailytracking.model.DailyTracking
import iti.grad.nutriscan.domain.dailytracking.model.DailyTrackingHistoryPage
import iti.grad.nutriscan.domain.dailytracking.model.DailyTrackingRemoteSnapshot
import iti.grad.nutriscan.domain.dailytracking.model.DailyTrackingSummary
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface IDailyTrackingRepository {
    /** Local Room state for today, Cairo-anchored. Never null — a missing row maps to defaults. */
    fun observeToday(): Flow<DailyTracking>

    suspend fun getByDate(date: LocalDate): Result<DailyTracking>

    suspend fun getHistoryPage(page: Int, size: Int): Result<DailyTrackingHistoryPage>

    /** Fetches a single day's summary from the backend — used by the date picker in CaloriesHistory. */
    suspend fun getRemoteDaySummary(date: LocalDate): Result<DailyTrackingSummary>

    /** Updates local Room only (`syncedToBackend = false`) — no live network call. */
    suspend fun updateWaterCnt(waterCnt: Int): Result<Unit>

    suspend fun updateTargetWaterCnt(targetWaterCnt: Int): Result<Unit>

    /** Updates stepsCnt and derives+persists caloriesBurnedSteps from the user's weightKg. */
    suspend fun updateStepsCnt(stepsCnt: Int): Result<Unit>

    /** Adds a finished workout's kcal/minutes onto today's running total. Local-only — no
     * backend field exists for exercise yet, so this never marks the row unsynced. */
    suspend fun addExerciseWorkout(kcalBurned: Int, minutes: Int): Result<Unit>

    suspend fun deleteDay(date: LocalDate): Result<Unit>

    /** Live, best-effort backend call — no local state of its own (FoodLogEntity owns that). */
    suspend fun pushMeal(date: LocalDate, scanId: String, mealCnt: Int): Result<Unit>

    suspend fun updateMeal(date: LocalDate, scanId: String, mealCnt: Int): Result<Unit>

    suspend fun deleteMeal(date: LocalDate, scanId: String): Result<Unit>

    /** Nightly job entry point: PATCHes [date]'s water/steps if unsynced. No-op if already synced. */
    suspend fun syncPendingDay(date: LocalDate): Result<Unit>

    /** Flushes every day still owing a push, not just today — a day that ended unsynced (logged
     * late at night, or pushed while offline) is otherwise never retried and never reaches the
     * backend, leaving a permanent hole in the history the backend can serve back. Returns failure
     * if any day failed, so the worker retries. */
    suspend fun syncAllPendingDays(): Result<Unit>

    /** Login/app-start entry point: fetches GET /daily-tracking/today, seeds local Room water/steps
     * only if no row exists yet for today. Always returns the fetched snapshot on success so the
     * caller (ReconcileTodayUseCase, Task 4) can separately reconcile meals against FoodLogEntity. */
    suspend fun fetchAndSeedToday(): Result<DailyTrackingRemoteSnapshot>
}
