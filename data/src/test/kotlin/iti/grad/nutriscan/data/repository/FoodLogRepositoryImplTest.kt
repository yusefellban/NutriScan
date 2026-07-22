package iti.grad.nutriscan.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.data.db.entity.FoodLogEntity
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
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
        MutableStateFlow(entries.value.filter { it.userId == userId && it.loggedDate == date })

    override suspend fun insert(entity: FoodLogEntity) {
        entries.value = entries.value.filterNot { it.id == entity.id } + entity
    }

    override suspend fun deleteByIdForUser(id: String, userId: String) {
        entries.value = entries.value.filterNot { it.id == id && it.userId == userId }
    }
}

class FoodLogRepositoryImplTest {

    private lateinit var dao: FakeFoodLogDao
    private lateinit var authRepository: IAuthRepository
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
        repository = FoodLogRepositoryImpl(dao, authRepository, UnconfinedTestDispatcher())
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
    fun `removeFoodEntry deletes a previously added entry`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        repository.addFoodEntry(entry())

        val removeResult = repository.removeFoodEntry("entry-1")
        assertTrue(removeResult.isSuccess)

        val entries = repository.observeTodayFoodLog().first()
        assertTrue(entries.isEmpty())
    }
}
