package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.UserDao
import iti.grad.nutriscan.data.db.dao.FamilyMemberDao
import iti.grad.nutriscan.data.db.entity.UserEntity
import iti.grad.nutriscan.data.db.entity.FamilyMemberEntity
import iti.grad.nutriscan.data.remote.datasource.IUserRemoteDataSource
import iti.grad.nutriscan.data.remote.dto.ApiErrorDto
import iti.grad.nutriscan.data.remote.dto.UpdateUserProfileRequestDto
import iti.grad.nutriscan.data.remote.dto.toEntity
import iti.grad.nutriscan.domain.user.model.ProfileUpdate
import iti.grad.nutriscan.domain.user.model.User
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val familyMemberDao: FamilyMemberDao,
    private val remoteDataSource: IUserRemoteDataSource,
    private val json: Json
) : IUserRepository {

    override fun getUserData(): Flow<User?> {
        return userDao.getUserFlow().map { entity ->
            entity?.let {
                User(
                    id = it.id,
                    firstName = it.firstName,
                    lastName = it.lastName,
                    email = it.email,
                    gender = it.gender,
                    dateOfBirth = it.dateOfBirth,
                    heightCm = it.heightCm,
                    weightKg = it.weightKg,
                    diseaseIds = it.diseaseIds,
                    allergyIds = it.allergyIds,
                    avatarUrl = it.avatarUrl
                )
            }
        }
    }

    override suspend fun fetchAndSyncProfile(): Result<Unit> {
        return try {
            val dto = remoteDataSource.getProfile()
            val localUser = userDao.getUserFlow().firstOrNull()
            
            val entity = UserEntity(
                id = dto.id,
                firstName = dto.firstName ?: "",
                lastName = dto.lastName,
                email = dto.email,
                gender = dto.gender,
                dateOfBirth = dto.dateOfBirth,
                heightCm = dto.heightCm,
                weightKg = dto.weightKg,
                diseaseIds = if (!dto.diseaseIds.isNullOrEmpty()) dto.diseaseIds else dto.diseases.map { it.id },
                allergyIds = if (!dto.allergyIds.isNullOrEmpty()) dto.allergyIds else dto.allergies.map { it.id },
                // If backend returns null, preserve our local offline avatar
                avatarUrl = dto.avatarUrl ?: localUser?.avatarUrl
            )
            userDao.insertOrUpdateUser(entity)

            // Keep the family-member cache in sync with every profile refresh too,
            // not just the add/remove flows in FamilyMemberRepositoryImpl.
            val familyMemberEntities = dto.familyMembers.orEmpty().map { memberDto ->
                memberDto.toEntity(dto.id)
            }
            familyMemberDao.replaceAllForUser(dto.id, familyMemberEntities)

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
                avatarUrl = profileUpdate.avatarUrl ?: currentUser.avatarUrl
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
                avatarUrl = profileUpdate.avatarUrl
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
}
