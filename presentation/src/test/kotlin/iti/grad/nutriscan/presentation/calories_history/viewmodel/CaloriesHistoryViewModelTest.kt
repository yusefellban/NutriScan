package iti.grad.nutriscan.presentation.calories_history.viewmodel

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import iti.grad.nutriscan.domain.dailytracking.model.DailyTrackingHistoryPage
import iti.grad.nutriscan.domain.dailytracking.model.DailyTrackingSummary
import iti.grad.nutriscan.domain.dailytracking.usecase.GetCaloriesHistoryUseCase
import iti.grad.nutriscan.domain.dailytracking.usecase.GetDayTrackingByDateUseCase
import iti.grad.nutriscan.presentation.calories_history.state.CaloriesHistoryEffect
import iti.grad.nutriscan.presentation.calories_history.state.CaloriesHistoryEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CaloriesHistoryViewModelTest {

    private lateinit var getCaloriesHistory: GetCaloriesHistoryUseCase
    private lateinit var getDayByDate: GetDayTrackingByDateUseCase
    private lateinit var viewModel: CaloriesHistoryViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getCaloriesHistory = mockk()
        getDayByDate = mockk()
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun initViewModel() {
        viewModel = CaloriesHistoryViewModel(
            getCaloriesHistory,
            getDayByDate
        )
    }

    @Test
    fun `init loads first page successfully`() = runTest {
        val date = LocalDate.of(2026, 8, 3)
        val dummySummary = DailyTrackingSummary(
            date = date,
            targetWaterCnt = 8,
            waterCnt = 4,
            stepsCnt = 5000,
            stepsKcal = 150,
            exerciseKcal = 200,
            exerciseMinutes = 30,
            totalMealKcal = 1200,
            mealCount = 2
        )
        val dummyPage = DailyTrackingHistoryPage(
            entries = listOf(dummySummary),
            isLastPage = false,
            currentPage = 0
        )
        coEvery { getCaloriesHistory(page = 0, size = 10) } returns Result.success(dummyPage)

        initViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(1, state.entries.size)
        assertEquals(0, state.currentPage)
        assertFalse(state.isLastPage)
    }

    @Test
    fun `init sets error message on failure`() = runTest {
        val errorMsg = "Network error"
        coEvery { getCaloriesHistory(page = 0, size = 10) } returns Result.failure(Exception(errorMsg))

        initViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(errorMsg, state.errorMessage)
        assertTrue(state.entries.isEmpty())
    }

    @Test
    fun `LoadMore triggers pagination successfully`() = runTest {
        val dummyPage1 = DailyTrackingHistoryPage(
            entries = listOf(mockk(relaxed = true)),
            isLastPage = false,
            currentPage = 0
        )
        val dummyPage2 = DailyTrackingHistoryPage(
            entries = listOf(mockk(relaxed = true), mockk(relaxed = true)),
            isLastPage = true,
            currentPage = 1
        )
        
        coEvery { getCaloriesHistory(page = 0, size = 10) } returns Result.success(dummyPage1)
        coEvery { getCaloriesHistory(page = 1, size = 10) } returns Result.success(dummyPage2)

        initViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onEvent(CaloriesHistoryEvent.LoadMore)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(3, state.entries.size)
        assertEquals(1, state.currentPage)
        assertTrue(state.isLastPage)
    }

    @Test
    fun `DateSelected loads single day and updates state`() = runTest {
        coEvery { getCaloriesHistory(page = 0, size = 10) } returns Result.success(
            DailyTrackingHistoryPage(emptyList(), false, 0)
        )
        
        val date = LocalDate.of(2026, 8, 1)
        val dummySummary = DailyTrackingSummary(
            date = date, targetWaterCnt = 8, waterCnt = 0, stepsCnt = 0,
            stepsKcal = 0, exerciseKcal = 0, exerciseMinutes = 0, totalMealKcal = 0, mealCount = 0
        )
        coEvery { getDayByDate(date) } returns Result.success(dummySummary)

        initViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onEvent(CaloriesHistoryEvent.DateSelected(date))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.showDatePicker)
        assertEquals(date, state.selectedDate)
        assertEquals(1, state.entries.size)
        assertTrue(state.isLastPage)
    }

    @Test
    fun `ClearDateFilter clears filter and reloads first page`() = runTest {
        coEvery { getCaloriesHistory(page = 0, size = 10) } returns Result.success(
            DailyTrackingHistoryPage(emptyList(), false, 0)
        )
        
        initViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Mock state to have a selected date
        viewModel.onEvent(CaloriesHistoryEvent.ClearDateFilter)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.selectedDate)
        assertFalse(state.showDatePicker)
    }

    @Test
    fun `NavigateBack event emits NavigateBack effect`() = runTest {
        coEvery { getCaloriesHistory(page = 0, size = 10) } returns Result.success(
            DailyTrackingHistoryPage(emptyList(), false, 0)
        )
        
        initViewModel()
        
        viewModel.effect.test {
            viewModel.onEvent(CaloriesHistoryEvent.NavigateBack)
            testDispatcher.scheduler.advanceUntilIdle()
            
            val effect = awaitItem()
            assertTrue(effect is CaloriesHistoryEffect.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `NavigateToAddMeals event emits NavigateToAddMeals effect`() = runTest {
        coEvery { getCaloriesHistory(page = 0, size = 10) } returns Result.success(
            DailyTrackingHistoryPage(emptyList(), false, 0)
        )
        
        initViewModel()
        
        viewModel.effect.test {
            viewModel.onEvent(CaloriesHistoryEvent.NavigateToAddMeals)
            testDispatcher.scheduler.advanceUntilIdle()
            
            val effect = awaitItem()
            assertTrue(effect is CaloriesHistoryEffect.NavigateToAddMeals)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
