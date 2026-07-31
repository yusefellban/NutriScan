package iti.grad.nutriscan.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import iti.grad.nutriscan.data.db.dao.DailyTrackingDao
import iti.grad.nutriscan.data.db.entity.DailyTrackingEntity
import iti.grad.nutriscan.data.remote.api.DailyTrackingApiService
import iti.grad.nutriscan.data.remote.dto.DailyTrackingMealResponseDto
import iti.grad.nutriscan.data.remote.dto.DailyTrackingRequestDto
import iti.grad.nutriscan.data.remote.dto.DailyTrackingResponseDto
import io.mockk.coVerify
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.common.CairoDateProvider
import iti.grad.nutriscan.domain.streak.repository.IStreakRepository
import iti.grad.nutriscan.domain.user.model.User
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private class FakeDailyTrackingDao : DailyTrackingDao {
    private val rows = MutableStateFlow<List<DailyTrackingEntity>>(emptyList())

    override fun observeByUserAndDate(userId: String, date: String): Flow<DailyTrackingEntity?> =
        MutableStateFlow(rows.value.find { it.userId == userId && it.date == date })

    override suspend fun getByUserAndDate(userId: String, date: String): DailyTrackingEntity? =
        rows.value.find { it.userId == userId && it.date == date }

    override suspend fun upsert(entity: DailyTrackingEntity) {
        rows.value = rows.value.filterNot { it.userId == entity.userId && it.date == entity.date } + entity
    }

    override suspend fun getRange(userId: String, startDate: String, endDate: String): List<DailyTrackingEntity> =
        rows.value.filter { it.userId == userId && it.date >= startDate && it.date <= endDate }.sortedBy { it.date }

    override suspend fun getUnsyncedDays(userId: String): List<DailyTrackingEntity> =
        rows.value.filter { it.userId == userId && !it.syncedToBackend }.sortedBy { it.date }

    override suspend fun markSynced(userId: String, date: String) {
        rows.value = rows.value.map {
            if (it.userId == userId && it.date == date) it.copy(syncedToBackend = true) else it
        }
    }

    override suspend fun deleteByUserAndDate(userId: String, date: String) {
        rows.value = rows.value.filterNot { it.userId == userId && it.date == date }
    }
}

class DailyTrackingRepositoryImplTest {

    private lateinit var dao: FakeDailyTrackingDao
    private lateinit var api: DailyTrackingApiService
    private lateinit var authRepository: IAuthRepository
    private lateinit var userRepository: IUserRepository
    private lateinit var streakRepository: IStreakRepository
    private lateinit var repository: DailyTrackingRepositoryImpl

    // Shared across the class (not recreated per test) so its scheduler can be passed into
    // runTest(...) below — observeToday() now runs a delay()-based midnight ticker, and delay()
    // requires the TestDispatcher it runs on to share the same TestCoroutineScheduler as the
    // runTest {} that's driving virtual time, or it throws "different schedulers".
    private val testDispatcher = UnconfinedTestDispatcher()

    private fun user(weightKg: Double? = 70.0) = User(
        id = "user-1",
        firstName = "Test",
        lastName = null,
        email = "test@test.com",
        gender = null,
        dateOfBirth = null,
        heightCm = null,
        weightKg = weightKg,
        diseaseIds = emptyList(),
        allergyIds = emptyList(),
    )

    @BeforeEach
    fun setup() {
        stubAndroidLog()
        dao = FakeDailyTrackingDao()
        api = mockk()
        authRepository = mockk()
        userRepository = mockk()
        streakRepository = mockk(relaxed = true)
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { userRepository.getUserData() } returns MutableStateFlow(user())
        repository = DailyTrackingRepositoryImpl(dao, api, authRepository, userRepository, streakRepository, testDispatcher)
    }

    @Test
    fun `observeToday returns defaults when no row exists yet`() = runTest(testDispatcher.scheduler) {
        val today = repository.observeToday().first()

        assertEquals(0, today.waterCnt)
        assertEquals(8, today.targetWaterCnt)
        assertEquals(0, today.stepsCnt)
        assertFalse(today.syncedToBackend)
    }

    @Test
    fun `updateWaterCnt persists locally and marks unsynced`() = runTest(testDispatcher.scheduler) {
        repository.updateWaterCnt(3)

        val today = repository.observeToday().first()
        assertEquals(3, today.waterCnt)
        assertFalse(today.syncedToBackend)
    }

    @Test
    fun `updateWaterCnt pushes shortly after the write instead of waiting for the periodic worker`() =
        runTest(testDispatcher.scheduler) {
            val date = CairoDateProvider.today()
            coEvery { api.updateDay(date.toString(), any()) } returns DailyTrackingResponseDto(date = date.toString())

            repository.updateWaterCnt(4)
            testScheduler.advanceUntilIdle()

            coVerify(exactly = 1) { api.updateDay(date.toString(), any()) }
            val today = repository.observeToday().first()
            assertTrue(today.syncedToBackend)
        }

    @Test
    fun `a burst of water writes collapses into a single PATCH`() = runTest(testDispatcher.scheduler) {
        val date = CairoDateProvider.today()
        coEvery { api.updateDay(date.toString(), any()) } returns DailyTrackingResponseDto(date = date.toString())

        // Filling cups one after another, faster than the debounce window.
        repository.updateWaterCnt(1)
        repository.updateWaterCnt(2)
        repository.updateWaterCnt(3)
        repository.updateWaterCnt(4)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { api.updateDay(date.toString(), any()) }
        assertEquals(4, repository.observeToday().first().waterCnt)
    }

    @Test
    fun `updateWaterCnt keeps the row unsynced locally when the immediate push fails`() =
        runTest(testDispatcher.scheduler) {
            val date = CairoDateProvider.today()
            coEvery { api.updateDay(date.toString(), any()) } throws RuntimeException("offline")

            repository.updateWaterCnt(4)
            testScheduler.runCurrent()

            val today = repository.observeToday().first()
            assertEquals(4, today.waterCnt)
            assertFalse(today.syncedToBackend)
        }

    @Test
    fun `updateWaterCnt above zero recomputes the streak`() = runTest(testDispatcher.scheduler) {
        repository.updateWaterCnt(1)

        coVerify(exactly = 1) { streakRepository.recomputeStreak() }
    }

    @Test
    fun `updateWaterCnt back to zero does not recompute the streak`() = runTest(testDispatcher.scheduler) {
        repository.updateWaterCnt(0)

        coVerify(exactly = 0) { streakRepository.recomputeStreak() }
    }

    @Test
    fun `updateStepsCnt derives caloriesBurnedSteps from the user's weight`() = runTest(testDispatcher.scheduler) {
        repository.updateStepsCnt(1000)

        val today = repository.observeToday().first()
        assertEquals(1000, today.stepsCnt)
        // 1000 steps * 70kg * 0.0005 = 35
        assertEquals(35, today.caloriesBurnedSteps)
    }

    @Test
    fun `updateStepsCnt ignores a lower reading so a second device cannot zero out the day`() =
        runTest(testDispatcher.scheduler) {
            val date = CairoDateProvider.today()
            coEvery { api.updateDay(date.toString(), any()) } returns DailyTrackingResponseDto(date = date.toString())
            repository.updateStepsCnt(39)

            // A fresh device / reinstall / emulator starts its step counter from 0.
            repository.updateStepsCnt(0)

            assertEquals(39, repository.observeToday().first().stepsCnt)
        }

    @Test
    fun `updateStepsCnt still accepts a higher reading`() = runTest(testDispatcher.scheduler) {
        val date = CairoDateProvider.today()
        coEvery { api.updateDay(date.toString(), any()) } returns DailyTrackingResponseDto(date = date.toString())
        repository.updateStepsCnt(39)

        repository.updateStepsCnt(120)

        assertEquals(120, repository.observeToday().first().stepsCnt)
    }

    @Test
    fun `fetchAndSeedToday keeps a locally-higher step count and leaves the row owing a push`() =
        runTest(testDispatcher.scheduler) {
            val date = CairoDateProvider.today()
            coEvery { api.updateDay(date.toString(), any()) } returns DailyTrackingResponseDto(date = date.toString())
            repository.updateStepsCnt(120)
            repository.syncPendingDay(date)
            coEvery { api.getToday() } returns DailyTrackingResponseDto(date = date.toString(), stepsCnt = 39)

            repository.fetchAndSeedToday()

            val today = repository.observeToday().first()
            assertEquals(120, today.stepsCnt)
            assertFalse(today.syncedToBackend)
        }

    @Test
    fun `updateStepsCnt above zero recomputes the streak`() = runTest(testDispatcher.scheduler) {
        repository.updateStepsCnt(500)

        coVerify(exactly = 1) { streakRepository.recomputeStreak() }
    }

    @Test
    fun `addExerciseWorkout recomputes the streak`() = runTest(testDispatcher.scheduler) {
        repository.addExerciseWorkout(kcalBurned = 200, minutes = 20)

        coVerify(exactly = 1) { streakRepository.recomputeStreak() }
    }

    @Test
    fun `syncPendingDay calls PATCH and marks synced on success`() = runTest(testDispatcher.scheduler) {
        val date = CairoDateProvider.today()
        repository.updateWaterCnt(4)
        coEvery { api.updateDay(date.toString(), any()) } returns DailyTrackingResponseDto(date = date.toString())

        val result = repository.syncPendingDay(date)

        assertTrue(result.isSuccess)
        val today = repository.observeToday().first()
        assertTrue(today.syncedToBackend)
    }

    @Test
    fun `syncPendingDay leaves the row unsynced when the API call fails`() = runTest(testDispatcher.scheduler) {
        val date = CairoDateProvider.today()
        repository.updateWaterCnt(4)
        coEvery { api.updateDay(date.toString(), any()) } throws RuntimeException("network error")

        val result = repository.syncPendingDay(date)

        assertTrue(result.isFailure)
        val today = repository.observeToday().first()
        assertFalse(today.syncedToBackend)
    }

    @Test
    fun `syncAllPendingDays flushes an older day the today-only sync would have stranded`() =
        runTest(testDispatcher.scheduler) {
            val oldDay = "2026-07-20"
            dao.upsert(
                DailyTrackingEntity(
                    userId = "user-1",
                    date = oldDay,
                    targetWaterCnt = 8,
                    waterCnt = 5,
                    stepsCnt = 900,
                    caloriesBurnedSteps = 31,
                    exerciseKcal = 0,
                    exerciseMinutes = 0,
                    syncedToBackend = false,
                )
            )
            coEvery { api.updateDay(oldDay, any()) } returns DailyTrackingResponseDto(date = oldDay)

            val result = repository.syncAllPendingDays()

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { api.updateDay(oldDay, any()) }
            assertTrue(dao.getUnsyncedDays("user-1").none { it.date == oldDay })
        }

    @Test
    fun `syncAllPendingDays reports failure so the worker retries`() = runTest(testDispatcher.scheduler) {
        val oldDay = "2026-07-20"
        dao.upsert(
            DailyTrackingEntity(
                userId = "user-1",
                date = oldDay,
                targetWaterCnt = 8,
                waterCnt = 5,
                stepsCnt = 900,
                caloriesBurnedSteps = 31,
                exerciseKcal = 0,
                exerciseMinutes = 0,
                syncedToBackend = false,
            )
        )
        coEvery { api.updateDay(oldDay, any()) } throws RuntimeException("network down")

        assertTrue(repository.syncAllPendingDays().isFailure)
        assertTrue(dao.getUnsyncedDays("user-1").any { it.date == oldDay })
    }

    @Test
    fun `fetchAndSeedToday seeds Room only when no row exists yet`() = runTest(testDispatcher.scheduler) {
        coEvery { api.getToday() } returns DailyTrackingResponseDto(
            date = CairoDateProvider.today().toString(),
            targetWaterCnt = 6,
            waterCnt = 2,
            stepsCnt = 500,
            meals = listOf(DailyTrackingMealResponseDto(scanId = "scan-1", mealCnt = 3)),
        )

        val result = repository.fetchAndSeedToday()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().meals.size)
        assertEquals(3, result.getOrThrow().meals.first().mealCnt)
        val today = repository.observeToday().first()
        assertEquals(2, today.waterCnt)
        assertTrue(today.syncedToBackend)
    }

    @Test
    fun `fetchAndSeedToday does not overwrite an existing local row`() = runTest(testDispatcher.scheduler) {
        repository.updateWaterCnt(9)
        coEvery { api.getToday() } returns DailyTrackingResponseDto(date = CairoDateProvider.today().toString(), waterCnt = 2)

        repository.fetchAndSeedToday()

        val today = repository.observeToday().first()
        assertEquals(9, today.waterCnt)
    }

    @Test
    fun `addExerciseWorkout accumulates kcal and minutes onto today's total`() = runTest(testDispatcher.scheduler) {
        repository.addExerciseWorkout(kcalBurned = 100, minutes = 10)
        repository.addExerciseWorkout(kcalBurned = 50, minutes = 5)

        val today = repository.observeToday().first()
        assertEquals(150, today.exerciseKcal)
        assertEquals(15, today.exerciseMinutes)
    }

    @Test
    fun `addExerciseWorkout does not mark the row unsynced`() = runTest(testDispatcher.scheduler) {
        val date = CairoDateProvider.today()
        repository.updateWaterCnt(4)
        coEvery { api.updateDay(date.toString(), any()) } returns DailyTrackingResponseDto(date = date.toString())
        repository.syncPendingDay(date)

        repository.addExerciseWorkout(kcalBurned = 100, minutes = 10)

        val today = repository.observeToday().first()
        assertTrue(today.syncedToBackend)
    }

    @Test
    fun `fetchAndSeedToday falls back to today's date when the backend omits it`() =
        runTest(testDispatcher.scheduler) {
            coEvery { api.getToday() } returns DailyTrackingResponseDto(date = null, waterCnt = 2)

            val result = repository.fetchAndSeedToday()

            assertTrue(result.isSuccess)
            assertEquals(CairoDateProvider.today(), result.getOrThrow().date)
        }

    @Test
    fun `fetchAndSeedToday drops meals with a null scanId instead of failing to parse`() =
        runTest(testDispatcher.scheduler) {
            coEvery { api.getToday() } returns DailyTrackingResponseDto(
                date = CairoDateProvider.today().toString(),
                meals = listOf(
                    DailyTrackingMealResponseDto(scanId = null, mealCnt = 1),
                    DailyTrackingMealResponseDto(scanId = "scan-1", mealCnt = 2),
                ),
            )

            val result = repository.fetchAndSeedToday()

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrThrow().meals.size)
            assertEquals("scan-1", result.getOrThrow().meals.first().scanId)
        }

    @Test
    fun `fetchAndSeedToday falls back to mealCnt 1 when the backend sends it as explicit null`() =
        runTest(testDispatcher.scheduler) {
            coEvery { api.getToday() } returns DailyTrackingResponseDto(
                date = CairoDateProvider.today().toString(),
                meals = listOf(DailyTrackingMealResponseDto(scanId = "scan-1", mealCnt = null)),
            )

            val result = repository.fetchAndSeedToday()

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrThrow().meals.first().mealCnt)
        }
}
