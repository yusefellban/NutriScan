package iti.grad.nutriscan.domain.onboarding.usecase

import iti.grad.nutriscan.domain.onboarding.repository.IOnboardingRepository
import javax.inject.Inject

/**
 * Marks the profile setup flow as completed by the user.
 *
 * This flag is stored locally in DataStore and is the primary gate used by
 * [CheckIfProfileSetupUseCase]. It is set only once — after the user successfully
 * submits the Profile Setup screens — ensuring that new users with placeholder
 * registration data are never silently routed past Profile Setup.
 */
class MarkProfileSetupCompletedUseCase @Inject constructor(
    private val onboardingRepository: IOnboardingRepository
) {
    suspend operator fun invoke() {
        onboardingRepository.completeProfileSetup()
    }
}
