package iti.grad.nutriscan.domain.streak.usecase

import iti.grad.nutriscan.domain.streak.repository.IStreakRepository
import javax.inject.Inject

class SyncDailyStreakUseCase @Inject constructor(
    private val streakRepository: IStreakRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return streakRepository.syncDailyStreak()
    }
}
