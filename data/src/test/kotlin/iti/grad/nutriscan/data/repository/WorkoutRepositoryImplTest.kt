package iti.grad.nutriscan.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.data.db.dao.WorkoutLogDao
import iti.grad.nutriscan.data.db.entity.WorkoutLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutRepositoryImplTest {

    private val dao: WorkoutLogDao = mockk()
    private val repository = WorkoutRepositoryImpl(dao, Dispatchers.Unconfined)

    @Test
    fun `observeTodayDone returns false when no row exists`() = runTest {
        coEvery { dao.observeByDate(any()) } returns flowOf(null)

        assertFalse(repository.observeTodayDone().first())
    }

    @Test
    fun `observeTodayDone maps existing row`() = runTest {
        coEvery { dao.observeByDate(any()) } returns flowOf(
            WorkoutLogEntity(date = "2026-07-24", done = true)
        )

        assertTrue(repository.observeTodayDone().first())
    }

    @Test
    fun `markDone upserts a done row for today`() = runTest {
        coEvery { dao.upsert(any()) } returns Unit

        val result = repository.markDone()

        assertTrue(result.isSuccess)
        coVerify { dao.upsert(match { it.done }) }
    }
}
