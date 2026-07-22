package iti.grad.nutriscan.presentation.main.calories

import app.cash.turbine.test
import iti.grad.nutriscan.presentation.common.model.BottomNavTab
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEffect
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEvent
import iti.grad.nutriscan.presentation.main.calories.viewmodel.CaloriesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaloriesViewModelTest {

    private lateinit var viewModel: CaloriesViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CaloriesViewModel()
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Initial State")
    inner class InitialState {

        @Test
        fun `initial state matches the mocked dashboard defaults`() {
            val state = viewModel.state.value

            Assertions.assertEquals(2350, state.tdee)
            Assertions.assertEquals(2100, state.caloriesGained)
            Assertions.assertEquals(10000, state.steps)
            Assertions.assertEquals(10000, state.stepsGoal)
            Assertions.assertEquals(250, state.exerciseKcal)
            Assertions.assertEquals(45, state.exerciseMinutes)
            Assertions.assertEquals(4, state.waterConsumed)
            Assertions.assertEquals(8, state.waterGoal)
            Assertions.assertFalse(state.isLoading)
        }
    }

    @Nested
    @DisplayName("Navigation")
    inner class Navigation {

        @Test
        fun `AddFoodClicked emits NavigateToSavedProducts`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(CaloriesEvent.AddFoodClicked)
                testScheduler.advanceUntilIdle()

                Assertions.assertTrue(awaitItem() is CaloriesEffect.NavigateToSavedProducts)
            }
        }

        @Test
        fun `BottomNavTabClicked with HOME emits NavigateToHome`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(CaloriesEvent.BottomNavTabClicked(BottomNavTab.HOME))
                testScheduler.advanceUntilIdle()

                Assertions.assertTrue(awaitItem() is CaloriesEffect.NavigateToHome)
            }
        }

        @Test
        fun `BottomNavTabClicked with CALORIES emits no effect`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(CaloriesEvent.BottomNavTabClicked(BottomNavTab.CALORIES))
                testScheduler.advanceUntilIdle()

                expectNoEvents()
            }
        }
    }

    @Nested
    @DisplayName("Water Tracking")
    inner class WaterTracking {

        @Test
        fun `AddWaterClicked adds an empty cup without filling it`() = runTest {
            viewModel.onEvent(CaloriesEvent.AddWaterClicked)
            testScheduler.advanceUntilIdle()

            Assertions.assertEquals(9, viewModel.state.value.waterGoal)
            Assertions.assertEquals(4, viewModel.state.value.waterConsumed)
        }

        @Test
        fun `WaterCupClicked on the next empty cup fills it`() = runTest {
            // waterConsumed=4, waterGoal=8 by default — index 4 is the next empty cup
            viewModel.onEvent(CaloriesEvent.WaterCupClicked(4))
            testScheduler.advanceUntilIdle()

            Assertions.assertEquals(5, viewModel.state.value.waterConsumed)
        }

        @Test
        fun `WaterCupClicked on the last filled cup unfills it`() = runTest {
            // waterConsumed=4 by default — index 3 is the last filled cup
            viewModel.onEvent(CaloriesEvent.WaterCupClicked(3))
            testScheduler.advanceUntilIdle()

            Assertions.assertEquals(3, viewModel.state.value.waterConsumed)
        }

        @Test
        fun `WaterCupClicked out of order is a no-op`() = runTest {
            // index 6 is neither the next empty cup (4) nor the last filled one (3)
            viewModel.onEvent(CaloriesEvent.WaterCupClicked(6))
            testScheduler.advanceUntilIdle()

            Assertions.assertEquals(4, viewModel.state.value.waterConsumed)
        }

        @Test
        fun `WaterCupClicked can fill all the way up to the water goal`() = runTest {
            repeat(10) { index -> viewModel.onEvent(CaloriesEvent.WaterCupClicked(4 + index)) }
            testScheduler.advanceUntilIdle()

            Assertions.assertEquals(8, viewModel.state.value.waterConsumed)
        }

        @Test
        fun `WaterCupClicked can unfill all the way down to zero`() = runTest {
            repeat(10) { index -> viewModel.onEvent(CaloriesEvent.WaterCupClicked(3 - index)) }
            testScheduler.advanceUntilIdle()

            Assertions.assertEquals(0, viewModel.state.value.waterConsumed)
        }

        @Test
        fun `WaterCupLongPressed on the last empty cup deletes it`() = runTest {
            // waterGoal=8 by default — index 7 is the last cup (empty, since waterConsumed=4)
            viewModel.onEvent(CaloriesEvent.WaterCupLongPressed(7))
            testScheduler.advanceUntilIdle()

            Assertions.assertEquals(7, viewModel.state.value.waterGoal)
            Assertions.assertEquals(4, viewModel.state.value.waterConsumed)
        }

        @Test
        fun `WaterCupLongPressed on a filled last cup deletes it and drops water consumed`() = runTest {
            repeat(4) { index -> viewModel.onEvent(CaloriesEvent.WaterCupClicked(4 + index)) } // fill up to 8
            viewModel.onEvent(CaloriesEvent.WaterCupLongPressed(7))
            testScheduler.advanceUntilIdle()

            Assertions.assertEquals(7, viewModel.state.value.waterGoal)
            Assertions.assertEquals(7, viewModel.state.value.waterConsumed)
        }

        @Test
        fun `WaterCupLongPressed on a non-last cup is a no-op`() = runTest {
            viewModel.onEvent(CaloriesEvent.WaterCupLongPressed(3))
            testScheduler.advanceUntilIdle()

            Assertions.assertEquals(8, viewModel.state.value.waterGoal)
            Assertions.assertEquals(4, viewModel.state.value.waterConsumed)
        }
    }
}
