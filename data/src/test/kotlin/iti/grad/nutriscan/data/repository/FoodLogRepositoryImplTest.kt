package iti.grad.nutriscan.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.data.db.entity.FoodLogEntity
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import iti.grad.nutriscan.domain.streak.repository.IStreakRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

private class FakeFoodLogDao : FoodLogDao {
    private val entries = MutableStateFlow<List<FoodLogEntity>>(emptyList())

    override fun observeByUserAndDate(userId: String, date: String): Flow<List<FoodLogEntity>> =
        MutableStateFlow(entries.value.filter { it.userId == userId && it.loggedDate == date && !it.deleted })

    override suspend fun insert(entity: FoodLogEntity) {
        entries.value = entries.value.filterNot { it.id == entity.id } + entity
    }

    override suspend fun getByIdForUser(id: String, userId: String): FoodLogEntity? =
        entries.value.find { it.id == id && it.userId == userId }

    override suspend fun markDeletedForUser(id: String, userId: String) {
        entries.value = entries.value.map {
            if (it.id == id && it.userId == userId) it.copy(deleted = true, pendingSync = true) else it
        }
    }

    override suspend fun getPendingSyncEntries(userId: String): List<FoodLogEntity> =
        entries.value.filter { it.pendingSync && it.userId == userId }

    override suspend fun clearPendingSync(id: String) {
        entries.value = entries.value.map {
            if (it.id == id) it.copy(pendingSync = false) else it
        }
    }

    override suspend fun hardDelete(id: String) {
        entries.value = entries.value.filterNot { it.id == id }
    }
}

class FoodLogRepositoryImplTest {

    private lateinit var dao: FakeFoodLogDao
    private lateinit var authRepository: IAuthRepository
    private lateinit var streakRepository: IStreakRepository
    private lateinit var dailyTrackingRepository: IDailyTrackingRepository
    private lateinit var repository: FoodLogRepositoryImpl

    private fun entry(id: String = "entry-1") = FoodLogEntry(
        id = id,
        productId = "product-1",
        name = "Apple",
        calories = 95,
        imageUrl = null,
        verdict = ProductVerdict.SAFE,
        loggedDate = LocalDate.now(),
        addedAt = Instant.now(),
    )

    @BeforeEach
    fun setup() {
        dao = FakeFoodLogDao()
        authRepository = mockk()
        streakRepository = mockk(relaxed = true)
        dailyTrackingRepository = mockk()
        coEvery { dailyTrackingRepository.pushMeal(any(), any(), any()) } returns Result.success(Unit)
        coEvery { dailyTrackingRepository.deleteMeal(any(), any()) } returns Result.success(Unit)
        repository = FoodLogRepositoryImpl(
            dao,
            authRepository,
            streakRepository,
            dailyTrackingRepository,
            UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `addFoodEntry falls back to the local device user when logged out`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns null

        val result = repository.addFoodEntry(entry())

        assertTrue(result.isSuccess)
    }

    @Test
    fun `observeTodayFoodLog returns entries added while logged out via the local device user`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns null
        repository.addFoodEntry(entry())

        val entries = repository.observeTodayFoodLog().first()

        assertEquals(1, entries.size)
    }

    @Test
    fun `removeFoodEntry succeeds when logged out via the local device user`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns null
        repository.addFoodEntry(entry())

        val result = repository.removeFoodEntry("entry-1")

        assertTrue(result.isSuccess)
        assertTrue(repository.observeTodayFoodLog().first().isEmpty())
    }

    @Test
    fun `addFoodEntry then observeTodayFoodLog returns the added entry for the current user`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"

        val addResult = repository.addFoodEntry(entry())
        assertTrue(addResult.isSuccess)

        val entries = repository.observeTodayFoodLog().first()

        assertEquals(1, entries.size)
        assertEquals("entry-1", entries.first().id)
    }

    @Test
    fun `removeFoodEntry soft-deletes then hard-deletes once the backend confirms`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { dailyTrackingRepository.deleteMeal(any(), "product-1") } returns Result.success(Unit)
        repository.addFoodEntry(entry())

        val removeResult = repository.removeFoodEntry("entry-1")
        assertTrue(removeResult.isSuccess)

        val entries = repository.observeTodayFoodLog().first()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `addFoodEntry sets pendingSync when the backend push fails`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { dailyTrackingRepository.pushMeal(any(), "product-1", any()) } returns Result.failure(RuntimeException("offline"))

        repository.addFoodEntry(entry())

        assertEquals(1, dao.getPendingSyncEntries("user-1").size)
    }
}
