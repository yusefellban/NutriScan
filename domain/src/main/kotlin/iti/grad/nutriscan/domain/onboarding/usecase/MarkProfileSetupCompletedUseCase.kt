package iti.grad.nutriscan.domain.onboarding.usecase

import javax.inject.Inject

/**
 * Obsolete.
 */
class MarkProfileSetupCompletedUseCase @Inject constructor() {
    suspend operator fun invoke() {
        // No longer needed: Profile setup completion is purely driven by DB data
    }
}
