package iti.grad.nutriscan.domain.auth.usecase

import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.notification.repository.INotificationScheduler
import javax.inject.Inject

class LoginWithEmailUseCase @Inject constructor(
    private val authRepository: IAuthRepository,
    private val notificationScheduler: INotificationScheduler,
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        return authRepository.loginWithEmail(email, password)
            .onSuccess { notificationScheduler.scheduleAll() }
    }
}
