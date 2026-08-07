package iti.grad.nutriscan.domain.streak.usecase

import iti.grad.nutriscan.domain.streak.repository.IStreakRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveStreakUseCase @Inject constructor(
    private val streakRepository: IStreakRepository
) {
    operator fun invoke(): Flow<Int> = streakRepository.observeStreak()
}
