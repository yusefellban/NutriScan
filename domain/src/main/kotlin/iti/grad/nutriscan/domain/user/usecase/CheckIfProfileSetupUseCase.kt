package iti.grad.nutriscan.domain.user.usecase

import iti.grad.nutriscan.domain.onboarding.repository.IOnboardingRepository
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Determines whether the user has completed the Profile Setup flow.
 *
 * ## Decision logic (in order):
 * 1. **Local flag first (fast-path):** If `isProfileSetupCompleted` is `true` in DataStore,
 *    return `true` immediately — no network call needed. This handles the common case where
 *    the user has already completed setup on this device.
 *
 * 2. **Server-data fallback (migration / new-install):** If the flag is `false` but the locally
 *    cached server profile has all required fields (gender, DOB, height, weight), the user must
 *    have completed setup on a previous install or another device. Auto-heal the flag and
 *    return `true`.
 *
 * 3. **Otherwise → `false`:** Send the user to Profile Setup.
 *
 * ## Why not just check server fields directly?
 * The registration call sends placeholder values (`gender = "MALE"`, `heightCm = 170.0`, etc.)
 * to satisfy the required backend schema. Those placeholders make the server-field check return
 * `true` for brand-new users, silently skipping Profile Setup. The local flag is the only
 * source of truth for "has this user explicitly submitted their real profile data".
 */
class CheckIfProfileSetupUseCase @Inject constructor(
    private val onboardingRepository: IOnboardingRepository,
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(): Boolean {
        // 1. Fast-path: local DataStore flag
        if (onboardingRepository.isProfileSetupCompleted()) return true

        // 2. Fallback: cached server data has real fields (migration / fresh install)
        val user = userRepository.getUserData().firstOrNull()
        val hasRealServerData = user != null &&
                user.gender != null &&
                user.dateOfBirth != null &&
                user.heightCm != null &&
                user.weightKg != null

        if (hasRealServerData) {
            // Auto-heal: persist the flag so future checks skip the DB read
            onboardingRepository.completeProfileSetup()
            return true
        }

        return false
    }
}
