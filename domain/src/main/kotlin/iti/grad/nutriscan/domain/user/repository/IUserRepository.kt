package iti.grad.nutriscan.domain.user.repository

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
}
