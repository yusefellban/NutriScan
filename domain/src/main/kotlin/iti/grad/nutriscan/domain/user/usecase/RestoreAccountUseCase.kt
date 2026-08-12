package iti.grad.nutriscan.domain.user.usecase

import iti.grad.nutriscan.domain.user.repository.IUserRepository
import javax.inject.Inject

/**
 * Cancels a pending account deletion and fully restores the account.
 *
 * Must be invoked while the account is still within its grace period —
 * i.e. before the date returned in [AccountDeletionInfo.scheduledDeletionAt].
 * On success the app navigates back to the Home screen as normal.
 */
class RestoreAccountUseCase @Inject constructor(
    private val userRepository: IUserRepository,
) {
    suspend operator fun invoke(): Result<Unit> =
        userRepository.restoreAccount()
}
