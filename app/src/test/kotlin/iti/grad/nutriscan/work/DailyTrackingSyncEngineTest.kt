package iti.grad.nutriscan.work

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.data.db.entity.FoodLogEntity
import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import iti.grad.nutriscan.domain.dailytracking.usecase.SyncPendingDailyTrackingUseCase
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Tests [DailyTrackingSyncEngine] directly rather than [DailyTrackingSyncWorker] — constructing a
 * real CoroutineWorker needs an Android Context (Robolectric), which would force this module into
 * mixing JUnit4+Vintage alongside its JUnit5 test stack for one file. The engine holds all the
 * behavior that matters (yesterday sync + pending meal retry + clear/hard-delete on success);
 * DailyTrackingSyncWorker itself is a thin, untested Android-framework wrapper around it.
 */
class DailyTrackingSyncEngineTest {

    private fun pendingEntry(id: String, deleted: Boolean) = FoodLogEntity(
        id = id,
        userId = "user-1",
        productId = id,
        name = "Test food",
        calories = 100,
        imageUrl = null,
        verdict = "SAFE",
        loggedDate = "2026-07-26",
        addedAtEpochMillis = 0L,
        pendingSync = true,
        deleted = deleted,
    )

    @Test
    fun `syncs yesterday and retries pending meal pushes and deletes`() = runTest {
        val syncPendingDailyTracking = mockk<SyncPendingDailyTrackingUseCase>()
        val dailyTrackingRepository = mockk<IDailyTrackingRepository>()
        val foodLogDao = mockk<FoodLogDao>()
        val yesterday = LocalDate.of(2026, 7, 26)

        coEvery { syncPendingDailyTracking(yesterday) } returns Result.success(Unit)
        coEvery { foodLogDao.getPendingSyncEntries() } returns listOf(
            pendingEntry("push-me", deleted = false),
            pendingEntry("delete-me", deleted = true),
        )
        coEvery { dailyTrackingRepository.pushMeal(any(), "push-me", any()) } returns Result.success(Unit)
        coEvery { dailyTrackingRepository.deleteMeal(any(), "delete-me") } returns Result.success(Unit)
        coEvery { foodLogDao.clearPendingSync("push-me") } returns Unit
        coEvery { foodLogDao.hardDelete("delete-me") } returns Unit

        val engine = DailyTrackingSyncEngine(syncPendingDailyTracking, dailyTrackingRepository, foodLogDao)
        val succeeded = engine.sync(yesterday)

        assertTrue(succeeded)
        coVerify { syncPendingDailyTracking(yesterday) }
        coVerify { dailyTrackingRepository.pushMeal(LocalDate.of(2026, 7, 26), "push-me", 1) }
        coVerify { dailyTrackingRepository.deleteMeal(LocalDate.of(2026, 7, 26), "delete-me") }
        coVerify { foodLogDao.clearPendingSync("push-me") }
        coVerify { foodLogDao.hardDelete("delete-me") }
    }

    @Test
    fun `failed meal retry is not cleared and overall result is unsuccessful`() = runTest {
        val syncPendingDailyTracking = mockk<SyncPendingDailyTrackingUseCase>()
        val dailyTrackingRepository = mockk<IDailyTrackingRepository>()
        val foodLogDao = mockk<FoodLogDao>()
        val yesterday = LocalDate.of(2026, 7, 26)

        coEvery { syncPendingDailyTracking(yesterday) } returns Result.success(Unit)
        coEvery { foodLogDao.getPendingSyncEntries() } returns listOf(pendingEntry("push-me", deleted = false))
        coEvery {
            dailyTrackingRepository.pushMeal(any(), "push-me", any())
        } returns Result.failure(RuntimeException("network down"))

        val engine = DailyTrackingSyncEngine(syncPendingDailyTracking, dailyTrackingRepository, foodLogDao)
        val succeeded = engine.sync(yesterday)

        assertFalse(succeeded)
        coVerify(exactly = 0) { foodLogDao.clearPendingSync(any()) }
        coVerify(exactly = 0) { foodLogDao.hardDelete(any()) }
    }

    @Test
    fun `failed day sync makes overall result unsuccessful even if meal retries pass`() = runTest {
        val syncPendingDailyTracking = mockk<SyncPendingDailyTrackingUseCase>()
        val dailyTrackingRepository = mockk<IDailyTrackingRepository>()
        val foodLogDao = mockk<FoodLogDao>()
        val yesterday = LocalDate.of(2026, 7, 26)

        coEvery { syncPendingDailyTracking(yesterday) } returns Result.failure(RuntimeException("network down"))
        coEvery { foodLogDao.getPendingSyncEntries() } returns emptyList()

        val engine = DailyTrackingSyncEngine(syncPendingDailyTracking, dailyTrackingRepository, foodLogDao)
        val succeeded = engine.sync(yesterday)

        assertFalse(succeeded)
    }
}
