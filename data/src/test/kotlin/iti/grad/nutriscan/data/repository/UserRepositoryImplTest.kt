package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.UserDao
import iti.grad.nutriscan.data.db.entity.UserEntity
import iti.grad.nutriscan.data.remote.datasource.IUserRemoteDataSource
import iti.grad.nutriscan.data.remote.dto.UserDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Covers the shared `persistProfileDto` merge logic (exercised via
 * [UserRepositoryImpl.fetchAndSyncProfile], the same path [UserRepositoryImpl.uploadAvatar]
 * writes through once the backend responds) and the avatar-upload error path.
 *
 * Note: a full success-path test of [UserRepositoryImpl.uploadAvatar] would additionally
 * need to exercise [iti.grad.nutriscan.data.util.ImageCompressor], which calls into
 * `android.graphics.BitmapFactory` — that requires Robolectric (not currently part of this
 * module's JVM unit test setup) and is intentionally left to instrumented/manual QA per the
 * verification plan.
 */
class UserRepositoryImplTest {

    private lateinit var userDao: UserDao
    private lateinit var remoteDataSource: IUserRemoteDataSource
    private lateinit var repository: UserRepositoryImpl

    @BeforeEach
    fun setup() {
        userDao = mockk(relaxed = true)
        remoteDataSource = mockk()
        every { userDao.getUserFlow() } returns flowOf(null)

        repository = UserRepositoryImpl(
            userDao = userDao,
            remoteDataSource = remoteDataSource,
            json = Json { ignoreUnknownKeys = true },
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `fetchAndSyncProfile persists the server's avatarUrl and updatedAt`() = runTest {
        val dto = UserDto(
            id = "1",
            email = "ahmed@example.com",
            firstName = "Ahmed",
            lastName = "Ali",
            avatarUrl = "https://cdn.example.com/avatars/1.jpg",
            updatedAt = "2026-07-29T10:00:00Z",
        )
        coEvery { remoteDataSource.getProfile() } returns dto

        val result = repository.fetchAndSyncProfile()

        assertTrue(result.isSuccess)
        val savedEntity = slot<UserEntity>()
        coVerify(exactly = 1) { userDao.insertOrUpdateUser(capture(savedEntity)) }
        assertEquals("https://cdn.example.com/avatars/1.jpg", savedEntity.captured.avatarUrl)
        assertEquals("2026-07-29T10:00:00Z", savedEntity.captured.updatedAt)
    }

    @Test
    fun `fetchAndSyncProfile preserves the local avatarUrl when the server omits it`() = runTest {
        every { userDao.getUserFlow() } returns flowOf(
            UserEntity(
                id = "1",
                firstName = "Ahmed",
                lastName = "Ali",
                email = "ahmed@example.com",
                avatarUrl = "https://cdn.example.com/avatars/old.jpg",
                updatedAt = "2026-07-01T00:00:00Z",
            )
        )
        val dto = UserDto(id = "1", email = "ahmed@example.com", avatarUrl = null, updatedAt = null)
        coEvery { remoteDataSource.getProfile() } returns dto

        repository.fetchAndSyncProfile()

        val savedEntity = slot<UserEntity>()
        coVerify(exactly = 1) { userDao.insertOrUpdateUser(capture(savedEntity)) }
        assertEquals("https://cdn.example.com/avatars/old.jpg", savedEntity.captured.avatarUrl)
        assertEquals("2026-07-01T00:00:00Z", savedEntity.captured.updatedAt)
    }

    @Test
    fun `fetchAndSyncProfile failure returns Result failure without touching the DAO`() = runTest {
        coEvery { remoteDataSource.getProfile() } throws Exception("Network error")

        val result = repository.fetchAndSyncProfile()

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { userDao.insertOrUpdateUser(any()) }
    }

    @Test
    fun `uploadAvatar returns failure when the image cannot be processed`() = runTest {
        // No android.graphics stub is available in this plain JVM test, so
        // ImageCompressor.compress() will throw — uploadAvatar() must catch that and
        // surface Result.failure instead of propagating, and must never call the
        // network or touch the DAO in that case.
        val result = repository.uploadAvatar(File("does-not-exist.jpg"))

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { remoteDataSource.uploadProfileImage(any()) }
        coVerify(exactly = 0) { userDao.insertOrUpdateUser(any()) }
    }
}
