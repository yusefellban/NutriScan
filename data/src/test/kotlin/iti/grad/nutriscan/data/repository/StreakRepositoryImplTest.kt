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
        coEvery { streakDao.observe() } returns flowOf(null)

        val result = repository.observeStreak().first()

        assertEquals(0, result.currentStreak)
        assertEquals(0, result.longestStreak)
    }

    @Test
    fun `recomputeStreak extends streak when last active was yesterday`() = runTest {
        val yesterday = LocalDate.now().minusDays(1).toString()
        coEvery { streakDao.observe() } returns flowOf(
            StreakEntity(currentStreak = 4, longestStreak = 4, lastActiveDate = yesterday)
        )
        coEvery { authRepository.getCurrentUserId() } returns null
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
        coEvery { streakDao.observe() } returns flowOf(
            StreakEntity(currentStreak = 4, longestStreak = 4, lastActiveDate = threeDaysAgo)
        )
        coEvery { authRepository.getCurrentUserId() } returns null
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
        coEvery { streakDao.observe() } returns flowOf(
            StreakEntity(currentStreak = 2, longestStreak = 4, lastActiveDate = yesterday)
        )
        coEvery { authRepository.getCurrentUserId() } returns null
        coEvery { foodLogDao.observeByUserAndDate(any(), any()) } returns flowOf(emptyList())
        coEvery { dailyTrackingDao.getByUserAndDate(any(), any()) } returns
            DailyTrackingEntity(
                userId = "local_device_user",
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
        coEvery { authRepository.getCurrentUserId() } returns null
        coEvery { foodLogDao.observeByUserAndDate(any(), any()) } returns flowOf(emptyList())
        coEvery { dailyTrackingDao.getByUserAndDate(any(), any()) } returns null

        val result = repository.recomputeStreak()

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { streakDao.upsert(any()) }
    }

    @Test
    fun `recomputeStreak failure leaves prior streak value untouched`() = runTest {
        coEvery { streakDao.observe() } throws RuntimeException("db error")

        val result = repository.recomputeStreak()

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { streakDao.upsert(any()) }
    }
}
