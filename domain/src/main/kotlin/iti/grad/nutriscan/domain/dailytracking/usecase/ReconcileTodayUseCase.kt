package iti.grad.nutriscan.domain.dailytracking.usecase

import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import iti.grad.nutriscan.domain.foodlog.repository.IFoodLogRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

/**
 * Login/app-start reconciliation: pulls today's backend state once, seeds local
 * water/steps if Room has no row yet, and inserts any backend-known meal not already
 * present locally (matched by productId == scanId) — see
 * docs/plans/2026-07-27-daily-tracking-sync.md §"Startup/login reconciliation".
 */
class ReconcileTodayUseCase @Inject constructor(
    private val dailyTrackingRepository: IDailyTrackingRepository,
    private val foodLogRepository: IFoodLogRepository,
) {
    suspend operator fun invoke(): Result<Unit> {
        val snapshot = dailyTrackingRepository.fetchAndSeedToday().getOrElse { return Result.failure(it) }

        val localScanIds = foodLogRepository.observeTodayFoodLog().first()
            .mapNotNull { it.productId }
            .toSet()

        snapshot.meals
            .filter { it.scanId !in localScanIds }
            .forEach { meal ->
                foodLogRepository.addFoodEntryLocalOnly(
                    FoodLogEntry(
                        id = "remote-${meal.scanId}",
                        productId = meal.scanId,
                        name = meal.productName.orEmpty(),
                        calories = meal.calories,
                        imageUrl = meal.imageUrl,
                        verdict = ProductVerdict.SAFE,
                        loggedDate = snapshot.date,
                        addedAt = Instant.now(),
                    )
                )
            }

        return Result.success(Unit)
    }
}
