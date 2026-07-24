package iti.grad.nutriscan.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.data.db.dao.WaterLogDao
import iti.grad.nutriscan.data.db.entity.WaterLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WaterRepositoryImplTest {

    private val dao: WaterLogDao = mockk()
    private val repository = WaterRepositoryImpl(dao, Dispatchers.Unconfined)

    @Test
    fun `observeToday returns default goal of 8 glasses when no row exists`() = runTest {
        coEvery { dao.observeByDate(any()) } returns flowOf(null)

        val result = repository.observeToday().first()

        assertEquals(0, result.glassCount)
        assertEquals(8, result.goalGlasses)
    }

    @Test
    fun `observeToday maps existing row`() = runTest {
        coEvery { dao.observeByDate(any()) } returns flowOf(
            WaterLogEntity(date = "2026-07-24", glassCount = 3, goalGlasses = 8)
        )

        val result = repository.observeToday().first()

        assertEquals(3, result.glassCount)
        assertEquals(8, result.goalGlasses)
    }

    @Test
    fun `logGlass increments today's count via upsert`() = runTest {
        coEvery { dao.observeByDate(any()) } returns flowOf(
            WaterLogEntity(date = "2026-07-24", glassCount = 3, goalGlasses = 8)
        )
        coEvery { dao.upsert(any()) } returns Unit

        val result = repository.logGlass()

        assertTrue(result.isSuccess)
        coVerify { dao.upsert(match { it.glassCount == 4 }) }
    }

    @Test
    fun `unlogGlass decrements today's count via upsert`() = runTest {
        coEvery { dao.observeByDate(any()) } returns flowOf(
            WaterLogEntity(date = "2026-07-24", glassCount = 3, goalGlasses = 8)
        )
        coEvery { dao.upsert(any()) } returns Unit

        val result = repository.unlogGlass()

        assertTrue(result.isSuccess)
        coVerify { dao.upsert(match { it.glassCount == 2 }) }
    }

    @Test
    fun `unlogGlass floors at zero instead of going negative`() = runTest {
        coEvery { dao.observeByDate(any()) } returns flowOf(
            WaterLogEntity(date = "2026-07-24", glassCount = 0, goalGlasses = 8)
        )
        coEvery { dao.upsert(any()) } returns Unit

        val result = repository.unlogGlass()

        assertTrue(result.isSuccess)
        coVerify { dao.upsert(match { it.glassCount == 0 }) }
    }
}
