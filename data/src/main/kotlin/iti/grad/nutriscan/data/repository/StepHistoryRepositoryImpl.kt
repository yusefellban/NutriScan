package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.DailyTrackingDao
import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.common.runCatchingCancellable
import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import iti.grad.nutriscan.domain.steps.history.model.MonthlyStepData
import iti.grad.nutriscan.domain.steps.history.model.StepHistoryPeriod
import iti.grad.nutriscan.domain.steps.history.model.StepHistorySummary
import iti.grad.nutriscan.domain.steps.history.repository.IStepHistoryRepository
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

private const val STEP_GOAL = 10_000
private const val DEFAULT_WEIGHT_KG = 70.0
private const val DEFAULT_HEIGHT_CM = 170.0
private const val STEP_KCAL_FACTOR = 0.0005

/** Stride length as a fraction of height — the standard walking estimate. */
private const val STRIDE_TO_HEIGHT_RATIO = 0.415

/** Average walking cadence, used to turn a step count into active minutes. */
private const val STEPS_PER_MINUTE = 100

class StepHistoryRepositoryImpl @Inject constructor(
    private val dao: DailyTrackingDao,
    private val dailyTrackingRepository: IDailyTrackingRepository,
    private val authRepository: IAuthRepository,
    private val userRepository: IUserRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : IStepHistoryRepository {

    override suspend fun getStepHistory(period: StepHistoryPeriod): Result<StepHistorySummary> =
        withContext(ioDispatcher) {
            runCatchingCancellable {
                val userId = authRepository.getCurrentUserId()
                    ?: error("No authenticated user - step history is unavailable until sign-in completes")

                val endDate = LocalDate.now()
                val startDate = endDate.minusDays(period.days - 1L)
                val stepsByDate = loadStepsByDate(userId, startDate, endDate)

                val user = userRepository.getUserData().first()
                val heightCm = user?.heightCm ?: DEFAULT_HEIGHT_CM
                val weightKg = user?.weightKg ?: DEFAULT_WEIGHT_KG

                val totalSteps = stepsByDate.values.sum()
                val strideMeters = heightCm * STRIDE_TO_HEIGHT_RATIO / 100

                StepHistorySummary(
                    periodAverage = totalSteps / period.days,
                    stepGoal = STEP_GOAL,
                    startDate = startDate,
                    endDate = endDate,
                    monthlyData = bucket(period, startDate, endDate, stepsByDate),
                    totalCaloriesBurned = (totalSteps * weightKg * STEP_KCAL_FACTOR).roundToInt(),
                    totalDistanceKm = totalSteps * strideMeters / 1000,
                    totalActiveMinutes = totalSteps / STEPS_PER_MINUTE,
                )
            }
        }

    /**
     * Backend history first so the chart still covers days recorded on another device, with local
     * Room layered on top: today's steps are counted by this device's sensor and only pushed a
     * couple of seconds later, so the local row is the fresher of the two.
     *
     * Fetches each day individually via [IDailyTrackingRepository.getRemoteDaySummary] (`GET
     * /daily-tracking/{date}`) rather than [IDailyTrackingRepository.getHistoryPage] (`GET
     * /daily-tracking`) — the paged endpoint isn't returning history, so it silently produced an
     * empty result here (`runCatchingCancellable` + `getOrNull()` swallowed the failure). The
     * per-date endpoint is the one already proven working (it backs the Calories History
     * date-picker), so this reuses it instead of debugging the paged one.
     */
    private suspend fun loadStepsByDate(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Map<LocalDate, Int> = coroutineScope {
        val remote = eachDay(startDate, endDate)
            .map { date -> async { date to dailyTrackingRepository.getRemoteDaySummary(date).getOrNull()?.stepsCnt } }
            .awaitAll()
            .mapNotNull { (date, steps) -> steps?.let { date to it } }
            .toMap()

        val local = dao.getRange(userId, startDate.toString(), endDate.toString())
            .associate { LocalDate.parse(it.date) to it.stepsCnt }

        remote + local
    }

    private fun bucket(
        period: StepHistoryPeriod,
        startDate: LocalDate,
        endDate: LocalDate,
        stepsByDate: Map<LocalDate, Int>,
    ): List<MonthlyStepData> = when (period) {
        StepHistoryPeriod.WEEK -> eachDay(startDate, endDate).map { date ->
            MonthlyStepData(
                monthLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                totalSteps = stepsByDate[date] ?: 0,
            )
        }

        StepHistoryPeriod.MONTH -> eachDay(startDate, endDate)
            .chunked(7)
            .mapIndexed { index, week ->
                MonthlyStepData(
                    monthLabel = "Week ${index + 1}",
                    totalSteps = week.sumOf { stepsByDate[it] ?: 0 },
                )
            }

        StepHistoryPeriod.THREE_MONTHS, StepHistoryPeriod.SIX_MONTHS ->
            eachDay(startDate, endDate)
                .groupBy { it.month }
                .map { (month, days) ->
                    MonthlyStepData(
                        monthLabel = month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        totalSteps = days.sumOf { stepsByDate[it] ?: 0 },
                    )
                }
    }

    private fun eachDay(startDate: LocalDate, endDate: LocalDate): List<LocalDate> =
        generateSequence(startDate) { it.plusDays(1) }
            .takeWhile { !it.isAfter(endDate) }
            .toList()
}

private val StepHistoryPeriod.days: Int
    get() = when (this) {
        StepHistoryPeriod.WEEK -> 7
        StepHistoryPeriod.MONTH -> 30
        StepHistoryPeriod.THREE_MONTHS -> 90
        StepHistoryPeriod.SIX_MONTHS -> 180
    }
