package iti.grad.nutriscan.domain.auth.usecase

import iti.grad.nutriscan.domain.auth.model.OidcAuthConfig
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import javax.inject.Inject

class GetOidcAuthConfigUseCase @Inject constructor(
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(): OidcAuthConfig {
        return authRepository.getOidcConfig()
    }
}
