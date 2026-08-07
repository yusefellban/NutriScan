package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.UserDao
import iti.grad.nutriscan.data.db.entity.FamilyMemberEntity
import iti.grad.nutriscan.data.db.entity.UserEntity
import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.data.remote.datasource.IUserRemoteDataSource
import iti.grad.nutriscan.data.remote.dto.ApiErrorDto
import iti.grad.nutriscan.data.remote.dto.FamilyMemberDto
import iti.grad.nutriscan.data.remote.dto.toEntity
import iti.grad.nutriscan.data.remote.dto.UpdateUserProfileRequestDto
import iti.grad.nutriscan.domain.family.model.FamilyMember
import iti.grad.nutriscan.domain.family.model.FamilyMemberInput
import iti.grad.nutriscan.domain.family.repository.IFamilyMemberRepository
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import iti.grad.nutriscan.data.local.datasource.TokenManager
import iti.grad.nutriscan.data.util.ImageCompressor
import iti.grad.nutriscan.data.local.util.JwtDecoder
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Offline-first, optimistic-update repository for family members — mirrors
 * [UserRepositoryImpl.updateProfile]'s optimistic-update-then-rollback
 * pattern.
 */
class FamilyMemberRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val remoteDataSource: IUserRemoteDataSource,
    private val json: Json,
    private val tokenManager: TokenManager,
    private val userRepository: IUserRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : IFamilyMemberRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)

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

    /** Polls the backend every [SYNC_INTERVAL_MS] for as long as at least one collector is
     * subscribed, so a family member added by another account holder on their own device shows up
     * here without an app restart. Reuses [IUserRepository.fetchAndSyncProfile] — family members
     * are part of the same `/profile` payload, no separate endpoint. [shareIn] multicasts this
     * single poll loop to every collector — [FamilyMemberRepositoryImpl] is a singleton, so
     * without it, two screens observing family members at once would each spin up their own
     * independent poller. */
    private val familyMembersFlow: Flow<List<FamilyMember>> = channelFlow {
        userRepository.fetchAndSyncProfile()
        launch {
            while (isActive) {
                delay(SYNC_INTERVAL_MS)
                userRepository.fetchAndSyncProfile()
            }
        }
        userDao.getUserFlow()
            .map { user -> user?.familyMembers.orEmpty().map { it.toDomain() } }
            .collect { send(it) }
    }.shareIn(repositoryScope, SharingStarted.WhileSubscribed(SHARE_STOP_TIMEOUT_MS), replay = 1)

    override fun getFamilyMembers(): Flow<List<FamilyMember>> = familyMembersFlow

    override suspend fun addFamilyMember(input: FamilyMemberInput): Result<Unit> {
        val userId = getActiveUserId()
            ?: return Result.failure(IllegalStateException("No local user; cannot add family member"))

        val user = userDao.getUserFlow().firstOrNull()
            ?: return Result.failure(IllegalStateException("No local user; cannot add family member"))
        val existing = user.familyMembers

        // OPTIMISTIC INSERT with a temporary client-side id so the UI updates instantly.
        val tempId = "local_${System.currentTimeMillis()}"
        val optimisticEntity = FamilyMemberEntity(
            id = tempId,
            name = input.name,
            relation = input.relation,
            allergyIds = input.allergyIds,
            diseaseIds = input.diseaseIds,
        )
        userDao.insertOrUpdateUser(user.copy(familyMembers = existing + optimisticEntity))

        val fullList = existing + optimisticEntity
        return syncListToBackend(userId = userId, fullList = fullList, rollbackTo = existing)
    }

    override suspend fun removeFamilyMember(memberId: String): Result<Unit> {
        val userId = getActiveUserId()
            ?: return Result.failure(IllegalStateException("No local user; cannot remove family member"))

        val user = userDao.getUserFlow().firstOrNull()
            ?: return Result.failure(IllegalStateException("No local user; cannot remove family member"))
        val existing = user.familyMembers
        val updatedList = existing.filterNot { it.id == memberId }

        userDao.insertOrUpdateUser(user.copy(familyMembers = updatedList))

        return syncListToBackend(userId = userId, fullList = updatedList, rollbackTo = existing)
    }

    override suspend fun updateFamilyMember(memberId: String, input: FamilyMemberInput): Result<Unit> {
        val userId = getActiveUserId()
            ?: return Result.failure(IllegalStateException("No local user; cannot update family member"))

        val user = userDao.getUserFlow().firstOrNull()
            ?: return Result.failure(IllegalStateException("No local user; cannot update family member"))
        val existing = user.familyMembers

        val updatedList = existing.map { entity ->
            if (entity.id == memberId) {
                entity.copy(
                    name = input.name,
                    relation = input.relation,
                    allergyIds = input.allergyIds,
                    diseaseIds = input.diseaseIds
                )
            } else {
                entity
            }
        }

        userDao.insertOrUpdateUser(user.copy(familyMembers = updatedList))

        return syncListToBackend(userId = userId, fullList = updatedList, rollbackTo = existing)
    }

    override suspend fun uploadFamilyMemberImage(memberId: String, imageFile: File): Result<Unit> {
        if (memberId.isBlank()) {
            return Result.failure(IllegalArgumentException("Family member id is required"))
        }

        return withContext(ioDispatcher) {
            val user = userDao.getUserFlow().firstOrNull()
            val existing = user?.familyMembers.orEmpty()

            try {
                val compressedFile = ImageCompressor.compress(imageFile)
                val requestFile = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData(
                    "image",
                    compressedFile.name,
                    requestFile
                )

                val updatedMemberDto = remoteDataSource.uploadFamilyMemberImage(memberId, part)

                if (compressedFile.absolutePath != imageFile.absolutePath) {
                    compressedFile.delete()
                }

                val updatedEntity = updatedMemberDto.toEntity()
                if (user != null) {
                    val updatedList = user.familyMembers.map { member ->
                        if (member.id == memberId) updatedEntity else member
                    }
                    userDao.insertOrUpdateUser(user.copy(familyMembers = updatedList))
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Exception while uploading family member image")
                if (user != null) {
                    userDao.insertOrUpdateUser(user.copy(familyMembers = existing))
                }
                Result.failure(e)
            }
        }
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
                        relation = entity.relation,
                        allergyIds = entity.allergyIds,
                        diseaseIds = entity.diseaseIds,
                    )
                }
            )
            val response = remoteDataSource.updateProfile(request)

            if (response.isSuccessful) {
                val refreshed = remoteDataSource.getProfile()
                val entities = refreshed.familyMembers.orEmpty().map { dto ->
                    dto.toEntity()
                }
                val user = userDao.getUserFlow().firstOrNull()
                if (user != null) {
                    userDao.insertOrUpdateUser(user.copy(familyMembers = entities))
                }
                Result.success(Unit)
            } else {
                val rawError = response.errorBody()?.string()
                Timber.e("Sync family members failed with code: ${response.code()}, errorBody: $rawError")
                val user = userDao.getUserFlow().firstOrNull()
                if (user != null) {
                    userDao.insertOrUpdateUser(user.copy(familyMembers = rollbackTo))
                }
                Result.failure(Exception(parseErrorMessage(rawError)))
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while syncing family members")
            val user = userDao.getUserFlow().firstOrNull()
            if (user != null) {
                userDao.insertOrUpdateUser(user.copy(familyMembers = rollbackTo))
            }
            Result.failure(e)
        }
    }

    private fun FamilyMemberEntity.toDomain() = FamilyMember(
        id = id,
        name = name,
        relation = relation,
        imageUrl = imageUrl,
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

    private companion object {
        const val SYNC_INTERVAL_MS = 15_000L
        const val SHARE_STOP_TIMEOUT_MS = 5_000L
    }
}
