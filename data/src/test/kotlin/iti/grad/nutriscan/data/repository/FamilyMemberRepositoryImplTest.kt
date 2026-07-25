package iti.grad.nutriscan.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.data.db.dao.FamilyMemberDao
import iti.grad.nutriscan.data.db.dao.UserDao
import iti.grad.nutriscan.data.db.entity.FamilyMemberEntity
import iti.grad.nutriscan.data.db.entity.UserEntity
import iti.grad.nutriscan.data.local.datasource.TokenManager
import iti.grad.nutriscan.data.remote.datasource.IUserRemoteDataSource
import iti.grad.nutriscan.data.remote.dto.FamilyMemberDto
import iti.grad.nutriscan.data.remote.dto.UpdateUserProfileRequestDto
import iti.grad.nutriscan.data.remote.dto.UserDto
import iti.grad.nutriscan.domain.family.model.FamilyMemberInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Response

/**
 * In-memory fake mirroring the real [FamilyMemberDao] contract (including
 * [FamilyMemberDao.replaceAllForUser]'s clear-then-insert semantics) so tests
 * exercise real rollback/reconciliation behavior rather than mocked
 * interactions.
 */
private class FakeFamilyMemberDao : FamilyMemberDao {
    private val membersByUser = MutableStateFlow<Map<String, List<FamilyMemberEntity>>>(emptyMap())

    override fun getFamilyMembersFlow(ownerUserId: String): Flow<List<FamilyMemberEntity>> =
        MutableStateFlow(membersByUser.value[ownerUserId].orEmpty())

    override suspend fun getFamilyMembersOnce(ownerUserId: String): List<FamilyMemberEntity> =
        membersByUser.value[ownerUserId].orEmpty()

    override suspend fun insertOrUpdateMember(member: FamilyMemberEntity) {
        val current = membersByUser.value[member.ownerUserId].orEmpty()
        membersByUser.value = membersByUser.value + (
            member.ownerUserId to (current.filterNot { it.id == member.id } + member)
        )
    }

    override suspend fun insertOrUpdateMembers(members: List<FamilyMemberEntity>) {
        members.forEach { insertOrUpdateMember(it) }
    }

    override suspend fun clearForUser(ownerUserId: String) {
        membersByUser.value = membersByUser.value + (ownerUserId to emptyList())
    }

    override suspend fun deleteById(memberId: String) {
        membersByUser.value = membersByUser.value.mapValues { (_, members) ->
            members.filterNot { it.id == memberId }
        }
    }

    override suspend fun replaceAllForUser(ownerUserId: String, members: List<FamilyMemberEntity>) {
        clearForUser(ownerUserId)
        insertOrUpdateMembers(members)
    }
}

class FamilyMemberRepositoryImplTest {

    private val userId = "user-1"

    private lateinit var familyMemberDao: FakeFamilyMemberDao
    private lateinit var userDao: UserDao
    private lateinit var remoteDataSource: IUserRemoteDataSource
    private lateinit var tokenManager: TokenManager
    private lateinit var json: Json
    private lateinit var repository: FamilyMemberRepositoryImpl

    private fun userEntity() = UserEntity(
        id = userId,
        firstName = "Ibrahim",
        lastName = "Soliman",
        email = "ibrahim@example.com",
    )

    private fun userDto(familyMembers: List<FamilyMemberDto>) = UserDto(
        id = userId,
        firstName = "Ibrahim",
        lastName = "Soliman",
        email = "ibrahim@example.com",
        familyMembers = familyMembers,
    )

    private fun successResponse(): Response<ResponseBody> =
        Response.success(ResponseBody.create(null, ""))

    @BeforeEach
    fun setup() {
        familyMemberDao = FakeFamilyMemberDao()
        userDao = mockk()
        remoteDataSource = mockk()
        tokenManager = mockk(relaxed = true)
        json = Json { ignoreUnknownKeys = true }

        coEvery { userDao.getUserFlow() } returns MutableStateFlow(userEntity())

        repository = FamilyMemberRepositoryImpl(
            familyMemberDao = familyMemberDao,
            userDao = userDao,
            remoteDataSource = remoteDataSource,
            json = json,
            tokenManager = tokenManager,
        )
    }

    @Test
    fun `addFamilyMember success path persists optimistic entity then reconciles with server ids`() = runTest {
        coEvery { remoteDataSource.updateProfile(any()) } returns successResponse()
        coEvery { remoteDataSource.getProfile() } returns userDto(
            familyMembers = listOf(
                FamilyMemberDto(id = "server-1", name = "Mother", allergyIds = listOf(1), diseaseIds = emptyList())
            )
        )

        val result = repository.addFamilyMember(
            FamilyMemberInput(name = "Mother", allergyIds = listOf(1), diseaseIds = emptyList())
        )

        assertTrue(result.isSuccess)
        val stored = familyMemberDao.getFamilyMembersOnce(userId)
        assertEquals(1, stored.size)
        assertEquals("server-1", stored.first().id) // temp local_ id replaced by server id
        assertEquals("Mother", stored.first().name)
    }

    @Test
    fun `addFamilyMember sends the full existing list plus the new member to the backend`() = runTest {
        familyMemberDao.insertOrUpdateMember(
            FamilyMemberEntity(id = "existing-1", ownerUserId = userId, name = "Father", allergyIds = emptyList(), diseaseIds = emptyList())
        )
        coEvery { remoteDataSource.updateProfile(any()) } returns successResponse()
        coEvery { remoteDataSource.getProfile() } returns userDto(
            familyMembers = listOf(
                FamilyMemberDto(id = "existing-1", name = "Father"),
                FamilyMemberDto(id = "server-2", name = "Mother"),
            )
        )

        repository.addFamilyMember(FamilyMemberInput(name = "Mother"))

        coVerify {
            remoteDataSource.updateProfile(
                withArg { request: UpdateUserProfileRequestDto ->
                    assertEquals(2, request.familyMembers?.size)
                    assertEquals("existing-1", request.familyMembers?.get(0)?.id)
                    assertEquals(null, request.familyMembers?.get(1)?.id) // new member has no id yet
                    assertEquals("Mother", request.familyMembers?.get(1)?.name)
                }
            )
        }
    }

    @Test
    fun `addFamilyMember failure path rolls back Room to the pre-add list`() = runTest {
        familyMemberDao.insertOrUpdateMember(
            FamilyMemberEntity(id = "existing-1", ownerUserId = userId, name = "Father", allergyIds = emptyList(), diseaseIds = emptyList())
        )
        coEvery { remoteDataSource.updateProfile(any()) } throws RuntimeException("network error")

        val result = repository.addFamilyMember(FamilyMemberInput(name = "Mother"))

        assertTrue(result.isFailure)
        val stored = familyMemberDao.getFamilyMembersOnce(userId)
        assertEquals(1, stored.size)
        assertEquals("existing-1", stored.first().id) // optimistic "Mother" insert was rolled back
    }

    @Test
    fun `removeFamilyMember success path deletes the member and reconciles with server response`() = runTest {
        familyMemberDao.insertOrUpdateMember(
            FamilyMemberEntity(id = "existing-1", ownerUserId = userId, name = "Father", allergyIds = emptyList(), diseaseIds = emptyList())
        )
        coEvery { remoteDataSource.updateProfile(any()) } returns successResponse()
        coEvery { remoteDataSource.getProfile() } returns userDto(familyMembers = emptyList())

        val result = repository.removeFamilyMember("existing-1")

        assertTrue(result.isSuccess)
        assertTrue(familyMemberDao.getFamilyMembersOnce(userId).isEmpty())
    }

    @Test
    fun `removeFamilyMember failure path rolls back Room so the member reappears`() = runTest {
        familyMemberDao.insertOrUpdateMember(
            FamilyMemberEntity(id = "existing-1", ownerUserId = userId, name = "Father", allergyIds = emptyList(), diseaseIds = emptyList())
        )
        coEvery { remoteDataSource.updateProfile(any()) } throws RuntimeException("network error")

        val result = repository.removeFamilyMember("existing-1")

        assertTrue(result.isFailure)
        val stored = familyMemberDao.getFamilyMembersOnce(userId)
        assertEquals(1, stored.size)
        assertEquals("existing-1", stored.first().id) // optimistic delete was rolled back
    }

    @Test
    fun `getFamilyMembers reflects the current Room-backed list for the active user`() = runTest {
        familyMemberDao.insertOrUpdateMember(
            FamilyMemberEntity(id = "existing-1", ownerUserId = userId, name = "Father", allergyIds = listOf(1), diseaseIds = listOf(2))
        )

        val members = repository.getFamilyMembers().first()

        assertEquals(1, members.size)
        assertEquals("Father", members.first().name)
    }
}