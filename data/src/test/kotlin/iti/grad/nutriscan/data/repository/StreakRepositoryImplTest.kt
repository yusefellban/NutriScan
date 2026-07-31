package iti.grad.nutriscan.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.data.db.dao.DailyTrackingDao
import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.data.db.entity.DailyTrackingEntity
import iti.grad.nutriscan.data.db.dao.StreakDao
import iti.grad.nutriscan.data.db.entity.StreakEntity
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class StreakRepositoryImplTest {

    private val streakDao: StreakDao = mockk()
    private val foodLogDao: FoodLogDao = mockk()
    private val dailyTrackingDao: DailyTrackingDao = mockk {
        coEvery { getByUserAndDate(any(), any()) } returns null
    }
    private val authRepository: IAuthRepository = mockk()
    private val repository =
        StreakRepositoryImpl(streakDao, foodLogDao, dailyTrackingDao, authRepository, Dispatchers.Unconfined)

    @Test
    fun `observeStreak returns zeroed streak when no row exists`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns USER_ID
        coEvery { streakDao.observe(USER_ID) } returns flowOf(null)

        val result = repository.observeStreak().first()

        assertEquals(0, result.currentStreak)
        assertEquals(0, result.longestStreak)
    }

    @Test
    fun `recomputeStreak extends streak when last active was yesterday`() = runTest {
        val yesterday = LocalDate.now().minusDays(1).toString()
        coEvery { streakDao.get(USER_ID) } returns
            StreakEntity(userId = USER_ID, currentStreak = 4, longestStreak = 4, lastActiveDate = yesterday)
        coEvery { authRepository.getCurrentUserId() } returns USER_ID
        coEvery { foodLogDao.observeByUserAndDate(any(), any()) } returns flowOf(
            listOf(mockk(relaxed = true))
        )
        coEvery { streakDao.upsert(any()) } returns Unit

        val result = repository.recomputeStreak()

        assertTrue(result.isSuccess)
        coVerify { streakDao.upsert(match { it.currentStreak == 5 }) }
    }

    @Test
    fun `recomputeStreak resets to 1 when a day was missed`() = runTest {
        val threeDaysAgo = LocalDate.now().minusDays(3).toString()
        coEvery { streakDao.get(USER_ID) } returns
            StreakEntity(userId = USER_ID, currentStreak = 4, longestStreak = 4, lastActiveDate = threeDaysAgo)
        coEvery { authRepository.getCurrentUserId() } returns USER_ID
        coEvery { foodLogDao.observeByUserAndDate(any(), any()) } returns flowOf(
            listOf(mockk(relaxed = true))
        )
        coEvery { streakDao.upsert(any()) } returns Unit

        val result = repository.recomputeStreak()

        assertTrue(result.isSuccess)
        coVerify { streakDao.upsert(match { it.currentStreak == 1 }) }
    }

    @Test
    fun `recomputeStreak extends streak from water logging alone, without any food logged`() = runTest {
        val yesterday = LocalDate.now().minusDays(1).toString()
        coEvery { streakDao.get(USER_ID) } returns
            StreakEntity(userId = USER_ID, currentStreak = 2, longestStreak = 4, lastActiveDate = yesterday)
        coEvery { authRepository.getCurrentUserId() } returns USER_ID
        coEvery { foodLogDao.observeByUserAndDate(any(), any()) } returns flowOf(emptyList())
        coEvery { dailyTrackingDao.getByUserAndDate(any(), any()) } returns
            DailyTrackingEntity(
                userId = USER_ID,
                date = LocalDate.now().toString(),
                targetWaterCnt = 8,
                waterCnt = 2,
                stepsCnt = 0,
                caloriesBurnedSteps = 0,
                exerciseKcal = 0,
                exerciseMinutes = 0,
                syncedToBackend = false,
            )
        coEvery { streakDao.upsert(any()) } returns Unit

        val result = repository.recomputeStreak()

        assertTrue(result.isSuccess)
        coVerify { streakDao.upsert(match { it.currentStreak == 3 }) }
    }

    @Test
    fun `recomputeStreak is a no-op when neither food nor daily-tracking activity happened today`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns USER_ID
        coEvery { foodLogDao.observeByUserAndDate(any(), any()) } returns flowOf(emptyList())
        coEvery { dailyTrackingDao.getByUserAndDate(any(), any()) } returns null

        val result = repository.recomputeStreak()

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { streakDao.upsert(any()) }
    }

    @Test
    fun `recomputeStreak failure leaves prior streak value untouched`() = runTest {
        coEvery { streakDao.get(USER_ID) } throws RuntimeException("db error")

        val result = repository.recomputeStreak()

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { streakDao.upsert(any()) }
    }

    @Test
    fun `observeStreak reads only the signed-in user's row`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns USER_ID
        coEvery { streakDao.observe(USER_ID) } returns flowOf(
            StreakEntity(userId = USER_ID, currentStreak = 7, longestStreak = 9, lastActiveDate = null)
        )
        // A different account's row must never be consulted — before the per-user re-key, both
        // accounts shared a single row and this is the leak that produced.
        coEvery { streakDao.observe("other-user") } returns flowOf(
            StreakEntity(userId = "other-user", currentStreak = 99, longestStreak = 99, lastActiveDate = null)
        )

        val result = repository.observeStreak().first()

        assertEquals(7, result.currentStreak)
        assertEquals(9, result.longestStreak)
    }

    @Test
    fun `recomputeStreak fails instead of writing to a shared bucket when signed out`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns null

        val result = repository.recomputeStreak()

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { streakDao.upsert(any()) }
    }

    private companion object {
        const val USER_ID = "user-1"
    }
}
