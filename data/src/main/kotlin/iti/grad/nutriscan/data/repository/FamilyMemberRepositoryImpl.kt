package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.FamilyMemberDao
import iti.grad.nutriscan.data.db.dao.UserDao
import iti.grad.nutriscan.data.db.entity.FamilyMemberEntity
import iti.grad.nutriscan.data.db.entity.UserEntity
import iti.grad.nutriscan.data.remote.datasource.IUserRemoteDataSource
import iti.grad.nutriscan.data.remote.dto.ApiErrorDto
import iti.grad.nutriscan.data.remote.dto.FamilyMemberDto
import iti.grad.nutriscan.data.remote.dto.toEntity
import iti.grad.nutriscan.data.remote.dto.UpdateUserProfileRequestDto
import iti.grad.nutriscan.domain.family.model.FamilyMember
import iti.grad.nutriscan.domain.family.model.FamilyMemberInput
import iti.grad.nutriscan.domain.family.repository.IFamilyMemberRepository
import iti.grad.nutriscan.data.local.datasource.TokenManager
import iti.grad.nutriscan.data.local.util.JwtDecoder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

/**
 * Offline-first, optimistic-update repository for family members — mirrors
 * [UserRepositoryImpl.updateProfile]'s optimistic-update-then-rollback
 * pattern.
 */
class FamilyMemberRepositoryImpl @Inject constructor(
    private val familyMemberDao: FamilyMemberDao,
    private val userDao: UserDao,
    private val remoteDataSource: IUserRemoteDataSource,
    private val json: Json,
    private val tokenManager: TokenManager,
) : IFamilyMemberRepository {

    private suspend fun getActiveUserId(): String? {
        val dbUser = userDao.getUserFlow().firstOrNull()
        if (dbUser != null) return dbUser.id

        val token = tokenManager.getIdToken() ?: tokenManager.getAccessToken()
        val tokenUserId = JwtDecoder.extractSubjectClaim(token)
        if (tokenUserId != null) {
            val email = JwtDecoder.extractClaim(token, "email") ?: ""
            val name = JwtDecoder.extractClaim(token, "name")
            val givenName = JwtDecoder.extractClaim(token, "given_name")
            val familyName = JwtDecoder.extractClaim(token, "family_name")

            val finalFirstName = givenName ?: name ?: "User"
            val finalLastName = familyName ?: ""

            // Seed a local placeholder user so database relational integrity is preserved offline.
            val placeholder = UserEntity(
                id = tokenUserId,
                firstName = finalFirstName,
                lastName = finalLastName,
                email = email
            )
            userDao.insertOrUpdateUser(placeholder)
            return tokenUserId
        }
        return null
    }

    override fun getFamilyMembers(): Flow<List<FamilyMember>> =
        userDao.getUserFlow().flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                familyMemberDao.getFamilyMembersFlow(user.id).map { entities ->
                    entities.map { it.toDomain() }
                }
            }
        }

    override suspend fun addFamilyMember(input: FamilyMemberInput): Result<Unit> {
        val userId = getActiveUserId()
            ?: return Result.failure(IllegalStateException("No local user; cannot add family member"))

        val existing = familyMemberDao.getFamilyMembersOnce(userId)

        // OPTIMISTIC INSERT with a temporary client-side id so the UI updates instantly.
        val tempId = "local_${System.currentTimeMillis()}"
        val optimisticEntity = FamilyMemberEntity(
            id = tempId,
            ownerUserId = userId,
            name = input.name,
            allergyIds = input.allergyIds,
            diseaseIds = input.diseaseIds,
        )
        familyMemberDao.insertOrUpdateMember(optimisticEntity)

        val fullList = existing + optimisticEntity
        return syncListToBackend(userId = userId, fullList = fullList, rollbackTo = existing)
    }

    override suspend fun removeFamilyMember(memberId: String): Result<Unit> {
        val userId = getActiveUserId()
            ?: return Result.failure(IllegalStateException("No local user; cannot remove family member"))

        val existing = familyMemberDao.getFamilyMembersOnce(userId)
        familyMemberDao.deleteById(memberId) // optimistic removal

        val fullList = existing.filterNot { it.id == memberId }
        return syncListToBackend(userId = userId, fullList = fullList, rollbackTo = existing)
    }

    /**
     * Sends the full family-member list via `PATCH /v1/users/profile`, then reconciles
     * Room with the server's response so temp ids get replaced with real server ids.
     * Rolls back to [rollbackTo] on any failure (matching [UserRepositoryImpl]'s pattern).
     */
    private suspend fun syncListToBackend(
        userId: String,
        fullList: List<FamilyMemberEntity>,
        rollbackTo: List<FamilyMemberEntity>,
    ): Result<Unit> {
        return try {
            val request = UpdateUserProfileRequestDto(
                familyMembers = fullList.map { entity ->
                    FamilyMemberDto(
                        id = entity.id.takeUnless { it.startsWith("local_") },
                        name = entity.name,
                        allergyIds = entity.allergyIds,
                        diseaseIds = entity.diseaseIds,
                    )
                }
            )
            val response = remoteDataSource.updateProfile(request)

            if (response.isSuccessful) {
                val refreshed = remoteDataSource.getProfile()
                val entities = refreshed.familyMembers.orEmpty().map { dto ->
                    dto.toEntity(userId)
                }
                familyMemberDao.replaceAllForUser(userId, entities)
                Result.success(Unit)
            } else {
                val rawError = response.errorBody()?.string()
                Timber.e("Sync family members failed with code: ${response.code()}, errorBody: $rawError")
                familyMemberDao.replaceAllForUser(userId, rollbackTo) // ROLLBACK
                Result.failure(Exception(parseErrorMessage(rawError)))
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while syncing family members")
            familyMemberDao.replaceAllForUser(userId, rollbackTo) // ROLLBACK (offline, timeout, etc.)
            Result.failure(e)
        }
    }

    private fun FamilyMemberEntity.toDomain() = FamilyMember(
        id = id,
        name = name,
        allergyIds = allergyIds,
        diseaseIds = diseaseIds,
    )

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
