package iti.grad.nutriscan.domain.auth.usecase

import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(firstName: String, lastName: String, email: String, password: String): Result<Unit> {
        return authRepository.register(firstName, lastName, email, password)
    }
}
