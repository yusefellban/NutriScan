package iti.grad.nutriscan.domain.auth.usecase

import iti.grad.nutriscan.domain.auth.model.AuthTokens
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import javax.inject.Inject

class SaveGoogleLoginTokensUseCase @Inject constructor(
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(authTokens: AuthTokens): Result<Unit> {
        return authRepository.saveTokens(authTokens)
    }
}
