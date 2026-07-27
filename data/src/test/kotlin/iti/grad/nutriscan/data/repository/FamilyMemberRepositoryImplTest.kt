package iti.grad.nutriscan.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.data.db.dao.UserDao
import iti.grad.nutriscan.data.db.entity.FamilyMemberEntity
import iti.grad.nutriscan.data.db.entity.UserEntity
import iti.grad.nutriscan.data.local.datasource.TokenManager
import iti.grad.nutriscan.data.remote.datasource.IUserRemoteDataSource
import iti.grad.nutriscan.data.remote.dto.FamilyMemberDto
import iti.grad.nutriscan.data.remote.dto.toEntity
import iti.grad.nutriscan.data.remote.dto.UpdateUserProfileRequestDto
import iti.grad.nutriscan.data.remote.dto.UserDto
import iti.grad.nutriscan.domain.family.model.FamilyMemberInput
import iti.grad.nutriscan.domain.user.repository.IUserRepository
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

class FamilyMemberRepositoryImplTest {

    private val userId = "user-1"

    private lateinit var userDao: UserDao
    private lateinit var remoteDataSource: IUserRemoteDataSource
    private lateinit var tokenManager: TokenManager
    private lateinit var json: Json
    private lateinit var userRepository: IUserRepository
    private lateinit var repository: FamilyMemberRepositoryImpl

    private var currentUserEntity = UserEntity(
        id = userId,
        firstName = "Ibrahim",
        lastName = "Soliman",
        email = "ibrahim@example.com",
    )

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
        userDao = mockk()
        remoteDataSource = mockk()
        tokenManager = mockk(relaxed = true)
        json = Json { ignoreUnknownKeys = true }
        userRepository = mockk()
        coEvery { userRepository.fetchAndSyncProfile() } returns Result.success(Unit)

        currentUserEntity = userEntity()
        coEvery { userDao.getUserFlow() } answers { MutableStateFlow(currentUserEntity) }
        coEvery { userDao.insertOrUpdateUser(any()) } answers {
            currentUserEntity = firstArg()
            Unit
        }

        repository = FamilyMemberRepositoryImpl(
            userDao = userDao,
            remoteDataSource = remoteDataSource,
            json = json,
            tokenManager = tokenManager,
            userRepository = userRepository,
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
        val stored = currentUserEntity.familyMembers
        assertEquals(1, stored.size)
        assertEquals("server-1", stored.first().id) // temp local_ id replaced by server id
        assertEquals("Mother", stored.first().name)
    }

    @Test
    fun `addFamilyMember sends the full existing list plus the new member to the backend`() = runTest {
        currentUserEntity = currentUserEntity.copy(
            familyMembers = listOf(
                FamilyMemberEntity(id = "existing-1", name = "Father", allergyIds = emptyList(), diseaseIds = emptyList())
            )
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
        currentUserEntity = currentUserEntity.copy(
            familyMembers = listOf(
                FamilyMemberEntity(id = "existing-1", name = "Father", allergyIds = emptyList(), diseaseIds = emptyList())
            )
        )
        coEvery { remoteDataSource.updateProfile(any()) } throws RuntimeException("network error")

        val result = repository.addFamilyMember(FamilyMemberInput(name = "Mother"))

        assertTrue(result.isFailure)
        val stored = currentUserEntity.familyMembers
        assertEquals(1, stored.size)
        assertEquals("existing-1", stored.first().id) // optimistic "Mother" insert was rolled back
    }

    @Test
    fun `removeFamilyMember success path deletes the member and reconciles with server response`() = runTest {
        currentUserEntity = currentUserEntity.copy(
            familyMembers = listOf(
                FamilyMemberEntity(id = "existing-1", name = "Father", allergyIds = emptyList(), diseaseIds = emptyList())
            )
        )
        coEvery { remoteDataSource.updateProfile(any()) } returns successResponse()
        coEvery { remoteDataSource.getProfile() } returns userDto(familyMembers = emptyList())

        val result = repository.removeFamilyMember("existing-1")

        assertTrue(result.isSuccess)
        assertTrue(currentUserEntity.familyMembers.isEmpty())
    }

    @Test
    fun `removeFamilyMember failure path rolls back Room so the member reappears`() = runTest {
        currentUserEntity = currentUserEntity.copy(
            familyMembers = listOf(
                FamilyMemberEntity(id = "existing-1", name = "Father", allergyIds = emptyList(), diseaseIds = emptyList())
            )
        )
        coEvery { remoteDataSource.updateProfile(any()) } throws RuntimeException("network error")

        val result = repository.removeFamilyMember("existing-1")

        assertTrue(result.isFailure)
        val stored = currentUserEntity.familyMembers
        assertEquals(1, stored.size)
        assertEquals("existing-1", stored.first().id) // optimistic delete was rolled back
    }

    @Test
    fun `getFamilyMembers reflects the current Room-backed list for the active user`() = runTest {
        currentUserEntity = currentUserEntity.copy(
            familyMembers = listOf(
                FamilyMemberEntity(id = "existing-1", name = "Father", allergyIds = listOf(1), diseaseIds = listOf(2))
            )
        )

        val members = repository.getFamilyMembers().first()
        assertEquals(1, members.size)
        assertEquals("Father", members.first().name)
    }

    @Test
    fun `addFamilyMember when database has no user fallback retrieves from tokenManager and seeds placeholder user`() = runTest {
        val userFlow = MutableStateFlow<UserEntity?>(null)
        coEvery { userDao.getUserFlow() } returns userFlow

        val fakeIdToken = "header.eyJzdWIiOiJzZWVkZWQtdXNlci0xMjMiLCJlbWFpbCI6InNlZWRlZEBleGFtcGxlLmNvbSIsImdpdmVuX25hbWUiOiJBaG1lZCIsImZhbWlseV9uYW1lIjoiVGF5c2VlciJ9.sig"
        coEvery { tokenManager.getIdToken() } returns fakeIdToken
        coEvery { tokenManager.getAccessToken() } returns null

        val insertedUser = io.mockk.slot<UserEntity>()
        coEvery { userDao.insertOrUpdateUser(capture(insertedUser)) } returns Unit
        coEvery { remoteDataSource.updateProfile(any()) } returns successResponse()
        coEvery { remoteDataSource.getProfile() } returns userDto(emptyList())

        val result = repository.addFamilyMember(FamilyMemberInput(name = "Mother"))

        assertTrue(result.isSuccess)
        coVerify { userDao.insertOrUpdateUser(any()) }
        assertEquals("seeded-user-123", insertedUser.captured.id)
        assertEquals("Ahmed", insertedUser.captured.firstName)
        assertEquals("Tayseer", insertedUser.captured.lastName)
        assertEquals("seeded@example.com", insertedUser.captured.email)
    }
}