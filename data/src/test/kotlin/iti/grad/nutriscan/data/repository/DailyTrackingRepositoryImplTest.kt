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
    fun `fetchAndSeedToday seeds Room only when no row exists yet`() = runTest(testDispatcher.scheduler) {
        coEvery { api.getToday() } returns DailyTrackingResponseDto(
            date = CairoDateProvider.today().toString(),
            targetWaterCnt = 6,
            waterCnt = 2,
            stepsCnt = 500,
            meals = listOf(DailyTrackingMealResponseDto(scanId = "scan-1", mealCnt = 1)),
        )

        val result = repository.fetchAndSeedToday()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().meals.size)
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
}
