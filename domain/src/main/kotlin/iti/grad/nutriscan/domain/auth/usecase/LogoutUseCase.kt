package iti.grad.nutriscan.domain.auth.usecase

import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.notification.repository.INotificationScheduler
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: IAuthRepository,
    private val notificationScheduler: INotificationScheduler,
) {
    suspend operator fun invoke(): Result<Unit> {
        // Cancelled unconditionally: AuthRepositoryImpl.logout() clears tokens even when the
        // server call fails, so the user is signed out either way.
        notificationScheduler.cancelAll()
        return authRepository.logout()
    }
}
