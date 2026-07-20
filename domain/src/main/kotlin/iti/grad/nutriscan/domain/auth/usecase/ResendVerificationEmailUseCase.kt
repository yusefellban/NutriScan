package iti.grad.nutriscan.domain.auth.usecase

import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import javax.inject.Inject

class ResendVerificationEmailUseCase @Inject constructor(
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        return authRepository.resendVerificationEmail(email)
    }
}
