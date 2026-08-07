package iti.grad.nutriscan.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.data.db.entity.FoodLogEntity
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import iti.grad.nutriscan.domain.streak.repository.IStreakRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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

    override suspend fun getByUserProductAndDate(userId: String, productId: String, date: String): FoodLogEntity? =
        entries.value.find {
            it.userId == userId && it.productId == productId && it.loggedDate == date && !it.deleted
        }

    override suspend fun updateMealCnt(id: String, mealCnt: Int) {
        entries.value = entries.value.map {
            if (it.id == id) it.copy(mealCnt = mealCnt, pendingSync = true) else it
        }
    }

    override suspend fun markBackendCreated(id: String) {
        entries.value = entries.value.map {
            if (it.id == id) it.copy(backendCreated = true) else it
        }
    }

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
        stubAndroidLog()
        dao = FakeFoodLogDao()
        authRepository = mockk()
        streakRepository = mockk(relaxed = true)
        dailyTrackingRepository = mockk()
        coEvery { dailyTrackingRepository.pushMeal(any(), any(), any()) } returns Result.success(Unit)
        coEvery { dailyTrackingRepository.deleteMeal(any(), any()) } returns Result.success(Unit)
        coEvery { dailyTrackingRepository.updateMeal(any(), any(), any()) } returns Result.success(Unit)
        repository = FoodLogRepositoryImpl(
            dao,
            authRepository,
            streakRepository,
            dailyTrackingRepository,
            UnconfinedTestDispatcher()
        )
    }

    // These three used to assert the opposite — that a logged-out caller silently fell through to a
    // shared "local_device_user" bucket. That bucket was visible to every account on the device, so
    // the old behaviour was the cross-account data leak, not a feature worth preserving.

    @Test
    fun `addFoodEntry fails instead of writing to a shared bucket when logged out`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns null

        val result = repository.addFoodEntry(entry())

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { dailyTrackingRepository.pushMeal(any(), any(), any()) }
    }

    @Test
    fun `observeTodayFoodLog exposes no rows when logged out`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns null

        assertThrows(IllegalStateException::class.java) {
            runBlocking { repository.observeTodayFoodLog().first() }
        }
    }

    @Test
    fun `removeFoodEntry fails instead of touching a shared bucket when logged out`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns null

        val result = repository.removeFoodEntry("entry-1")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { dailyTrackingRepository.deleteMeal(any(), any()) }
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

    @Test
    fun `adding the same product twice increments mealCnt and calls updateMeal, not pushMeal again`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"

        repository.addFoodEntry(entry(id = "entry-1"))
        repository.addFoodEntry(entry(id = "entry-2"))

        val entries = repository.observeTodayFoodLog().first()
        assertEquals(1, entries.size)
        assertEquals(2, entries.first().mealCnt)
        coVerify(exactly = 1) { dailyTrackingRepository.pushMeal(any(), "product-1", 1) }
        coVerify(exactly = 1) { dailyTrackingRepository.updateMeal(any(), "product-1", 2) }
    }

    @Test
    fun `removeFoodEntry decrements mealCnt via updateMeal when the count stays above zero`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        repository.addFoodEntry(entry(id = "entry-1"))
        repository.addFoodEntry(entry(id = "entry-2"))

        val result = repository.removeFoodEntry("entry-1")

        assertTrue(result.isSuccess)
        val entries = repository.observeTodayFoodLog().first()
        assertEquals(1, entries.size)
        assertEquals(1, entries.first().mealCnt)
        coVerify(exactly = 1) { dailyTrackingRepository.updateMeal(any(), "product-1", 1) }
        coVerify(exactly = 0) { dailyTrackingRepository.deleteMeal(any(), any()) }
    }

    @Test
    fun `removeFoodEntry deletes and calls deleteMeal only once mealCnt reaches zero`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        repository.addFoodEntry(entry(id = "entry-1"))

        val result = repository.removeFoodEntry("entry-1")

        assertTrue(result.isSuccess)
        assertTrue(repository.observeTodayFoodLog().first().isEmpty())
        coVerify(exactly = 1) { dailyTrackingRepository.deleteMeal(any(), "product-1") }
    }
}
