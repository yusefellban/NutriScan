package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.UserDao
import iti.grad.nutriscan.data.db.entity.UserEntity
import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.data.remote.datasource.IUserRemoteDataSource
import iti.grad.nutriscan.data.remote.dto.ApiErrorDto
import iti.grad.nutriscan.data.remote.dto.UpdateUserProfileRequestDto
import iti.grad.nutriscan.data.remote.dto.toEntity
import iti.grad.nutriscan.domain.user.model.ProfileUpdate
import iti.grad.nutriscan.domain.user.model.User
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val remoteDataSource: IUserRemoteDataSource,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : IUserRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    /** Polls the backend every [SYNC_INTERVAL_MS] for as long as at least one collector is
     * subscribed, so a profile edit made on another device shows up here without an app restart.
     * [shareIn] multicasts this single poll loop to every collector — [UserRepositoryImpl] is a
     * singleton, so without it, two screens observing the profile at once would each spin up their
     * own independent poller. */
    private val userDataFlow: Flow<User?> = channelFlow {
        fetchAndSyncProfile()
        launch {
            while (isActive) {
                delay(SYNC_INTERVAL_MS)
                fetchAndSyncProfile()
            }
        }
        userDao.getUserFlow().map { entity -> entity?.toDomain() }.collect { send(it) }
    }.shareIn(repositoryScope, SharingStarted.WhileSubscribed(SHARE_STOP_TIMEOUT_MS), replay = 1)

    override fun getUserData(): Flow<User?> = userDataFlow

    private fun UserEntity.toDomain() = User(
        id = id,
        firstName = firstName,
        lastName = lastName,
        email = email,
        gender = gender,
        dateOfBirth = dateOfBirth,
        heightCm = heightCm,
        weightKg = weightKg,
        diseaseIds = diseaseIds,
        allergyIds = allergyIds,
        avatarUrl = avatarUrl,
        bmi = bmi,
        tdee = tdee,
    )

    override suspend fun fetchAndSyncProfile(): Result<Unit> {
        return try {
            val dto = remoteDataSource.getProfile()
            val localUser = userDao.getUserFlow().firstOrNull()
            
            val entity = UserEntity(
                id = dto.id.takeIf { it.isNotBlank() } ?: localUser?.id ?: "unknown",
                firstName = dto.firstName ?: dto.name ?: localUser?.firstName ?: "",
                lastName = dto.lastName ?: localUser?.lastName,
                email = dto.email.takeIf { it.isNotBlank() } ?: localUser?.email ?: "",
                gender = dto.gender,
                dateOfBirth = dto.dateOfBirth,
                heightCm = dto.heightCm,
                weightKg = dto.weightKg,
                diseaseIds = dto.diseases?.map { it.id } ?: localUser?.diseaseIds ?: emptyList(),
                allergyIds = dto.allergies?.map { it.id } ?: localUser?.allergyIds ?: emptyList(),
                // If backend returns null, preserve our local offline avatar
                avatarUrl = dto.avatarUrl ?: localUser?.avatarUrl,
                // Server-computed: always take the latest value from the backend;
                // preserve local if the backend omits them (null-coalescing).
                bmi = dto.bmi ?: localUser?.bmi,
                tdee = dto.tdee ?: localUser?.tdee,
                familyMembers = dto.familyMembers.orEmpty().map { it.toEntity() }
            )
            userDao.insertOrUpdateUser(entity)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(profileUpdate: ProfileUpdate): Result<Unit> {
        val currentUser = userDao.getUserFlow().firstOrNull()
        return try {
            // OPTIMISTIC UPDATE: Update local database first
            val updatedUser = currentUser?.copy(
                firstName = profileUpdate.firstName ?: currentUser.firstName,
                lastName = profileUpdate.lastName ?: currentUser.lastName,
                gender = profileUpdate.gender ?: currentUser.gender,
                dateOfBirth = profileUpdate.dateOfBirth ?: currentUser.dateOfBirth,
                heightCm = profileUpdate.heightCm ?: currentUser.heightCm,
                weightKg = profileUpdate.weightKg ?: currentUser.weightKg,
                diseaseIds = profileUpdate.diseaseIds ?: currentUser.diseaseIds,
                allergyIds = profileUpdate.allergyIds ?: currentUser.allergyIds,
                avatarUrl = profileUpdate.avatarUrl ?: currentUser.avatarUrl,
                // bmi and tdee are server-computed — never overwrite with null on optimistic update.
                bmi = currentUser.bmi,
                tdee = currentUser.tdee,
            ) ?: UserEntity(
                id = "local_temp_id",
                firstName = profileUpdate.firstName ?: "",
                lastName = profileUpdate.lastName ?: "",
                email = "",
                gender = profileUpdate.gender,
                dateOfBirth = profileUpdate.dateOfBirth,
                heightCm = profileUpdate.heightCm,
                weightKg = profileUpdate.weightKg,
                diseaseIds = profileUpdate.diseaseIds ?: emptyList(),
                allergyIds = profileUpdate.allergyIds ?: emptyList(),
                avatarUrl = profileUpdate.avatarUrl,
                // No bmi/tdee yet — will be populated on next fetchAndSyncProfile()
                bmi = null,
                tdee = null,
            )
            userDao.insertOrUpdateUser(updatedUser)

            val request = UpdateUserProfileRequestDto(
                firstName = profileUpdate.firstName,
                lastName = profileUpdate.lastName,
                gender = profileUpdate.gender,
                dateOfBirth = profileUpdate.dateOfBirth,
                heightCm = profileUpdate.heightCm,
                weightKg = profileUpdate.weightKg,
                diseaseIds = profileUpdate.diseaseIds,
                allergyIds = profileUpdate.allergyIds,
                avatarUrl = profileUpdate.avatarUrl
            )
            val response = remoteDataSource.updateProfile(request)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val rawError = response.errorBody()?.string()
                val parsedMessage = parseErrorMessage(rawError)
                Timber.e("Update profile failed with code: ${response.code()}, errorBody: $rawError")
                // ROLLBACK
                if (currentUser != null) {
                    userDao.insertOrUpdateUser(currentUser)
                } else {
                    userDao.deleteUser()
                }
                Result.failure(Exception(parsedMessage))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating profile")
            // ROLLBACK
            if (currentUser != null) {
                userDao.insertOrUpdateUser(currentUser)
            } else {
                userDao.deleteUser()
            }
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "An unexpected error occurred."
        return try {
            val apiError = json.decodeFromString<ApiErrorDto>(errorBody)
            apiError.message
        } catch (_: Exception) {
            "An unexpected error occurred."
        }
    }

    private companion object {
        const val SYNC_INTERVAL_MS = 15_000L
        const val SHARE_STOP_TIMEOUT_MS = 5_000L
    }
}
