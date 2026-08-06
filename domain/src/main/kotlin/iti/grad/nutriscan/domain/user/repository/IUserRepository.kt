package iti.grad.nutriscan.domain.user.repository

import iti.grad.nutriscan.domain.user.model.AccountDeletionInfo
import iti.grad.nutriscan.domain.user.model.ProfileUpdate
import iti.grad.nutriscan.domain.user.model.User
import kotlinx.coroutines.flow.Flow
import java.io.File

interface IUserRepository {
    /**
     * Gets the current user data as a continuous flow from the local database.
     * Represents the Single Source of Truth for the user profile.
     */
    fun getUserData(): Flow<User?>

    /**
     * Emits the scheduled deletion date when a 409 ACCOUNT_PENDING_DELETION is encountered.
     */
    val accountPendingDeletionEvent: Flow<String>

    /**
     * Fetches the latest profile from the backend and syncs it with the local database.
     */
    suspend fun fetchAndSyncProfile(): Result<Unit>

    /**
     * Updates the user's profile on the backend. 
     * If successful, the local database is immediately updated.
     */
    suspend fun updateProfile(profileUpdate: ProfileUpdate): Result<Unit>

    /**
     * Uploads a new avatar image to the backend. On success, the backend's
     * returned profile (including the new permanent image URL) is persisted
     * to the local database, exactly like [fetchAndSyncProfile].
     */
    suspend fun uploadAvatar(imageFile: File): Result<Unit>

    /**
     * Schedules the authenticated account for permanent deletion.
     *
     * The backend does NOT delete immediately — it returns a [AccountDeletionInfo]
     * containing the date the account will be erased and the grace period in days.
     * The caller is responsible for logging the user out after this call succeeds.
     */
    suspend fun deleteAccount(): Result<AccountDeletionInfo>

    /**
     * Cancels a pending account deletion and fully restores the account.
     *
     * Must be called while the account is still within its grace period
     * (i.e. before [AccountDeletionInfo.scheduledDeletionAt]).
     */
    suspend fun restoreAccount(): Result<Unit>
}
