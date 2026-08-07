package iti.grad.nutriscan.domain.user.usecase

import iti.grad.nutriscan.domain.user.model.AccountDeletionInfo
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import javax.inject.Inject

/**
 * Schedules the current user's account for permanent deletion.
 *
 * On success, returns [AccountDeletionInfo] containing the scheduled deletion date
 * and grace period. The caller (ViewModel) must call [LogoutUseCase] immediately
 * after to clear local state and navigate to the Auth screen.
 */
class DeleteAccountUseCase @Inject constructor(
    private val userRepository: IUserRepository,
) {
    suspend operator fun invoke(): Result<AccountDeletionInfo> =
        userRepository.deleteAccount()
}
