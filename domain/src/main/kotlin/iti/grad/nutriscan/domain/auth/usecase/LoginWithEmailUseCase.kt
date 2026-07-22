package iti.grad.nutriscan.domain.auth.usecase

import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import javax.inject.Inject

class LoginWithEmailUseCase @Inject constructor(
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        return authRepository.loginWithEmail(email, password)
    }
}
