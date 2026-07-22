package iti.grad.nutriscan.presentation.main.calories

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import iti.grad.nutriscan.domain.steps.usecase.CheckStepsPermissionUseCase
import iti.grad.nutriscan.domain.steps.usecase.ObserveTodayStepsUseCase
import iti.grad.nutriscan.presentation.common.model.BottomNavTab
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEffect
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEvent
import iti.grad.nutriscan.presentation.main.calories.viewmodel.CaloriesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

    private lateinit var checkStepsPermission: CheckStepsPermissionUseCase
    private lateinit var observeTodaySteps: ObserveTodayStepsUseCase
    private lateinit var viewModel: CaloriesViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        checkStepsPermission = mockk()
        observeTodaySteps = mockk()
        viewModel = CaloriesViewModel(checkStepsPermission, observeTodaySteps)
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
            Assertions.assertEquals(0, state.steps)
            Assertions.assertEquals(10000, state.stepsGoal)
            Assertions.assertFalse(state.stepsPermissionGranted)
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
                testScheduler.runCurrent()

                Assertions.assertTrue(awaitItem() is CaloriesEffect.NavigateToSavedProducts)
            }
        }

        @Test
        fun `BottomNavTabClicked with HOME emits NavigateToHome`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(CaloriesEvent.BottomNavTabClicked(BottomNavTab.HOME))
                testScheduler.runCurrent()

                Assertions.assertTrue(awaitItem() is CaloriesEffect.NavigateToHome)
            }
        }

        @Test
        fun `BottomNavTabClicked with CALORIES emits no effect`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(CaloriesEvent.BottomNavTabClicked(BottomNavTab.CALORIES))
                testScheduler.runCurrent()

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
            testScheduler.runCurrent()

            Assertions.assertEquals(9, viewModel.state.value.waterGoal)
            Assertions.assertEquals(4, viewModel.state.value.waterConsumed)
        }

        @Test
        fun `WaterCupClicked on the next empty cup fills it`() = runTest {
            // waterConsumed=4, waterGoal=8 by default — index 4 is the next empty cup
            viewModel.onEvent(CaloriesEvent.WaterCupClicked(4))
            testScheduler.runCurrent()

            Assertions.assertEquals(5, viewModel.state.value.waterConsumed)
        }

        @Test
        fun `WaterCupClicked on the last filled cup unfills it`() = runTest {
            // waterConsumed=4 by default — index 3 is the last filled cup
            viewModel.onEvent(CaloriesEvent.WaterCupClicked(3))
            testScheduler.runCurrent()

            Assertions.assertEquals(3, viewModel.state.value.waterConsumed)
        }

        @Test
        fun `WaterCupClicked out of order is a no-op`() = runTest {
            // index 6 is neither the next empty cup (4) nor the last filled one (3)
            viewModel.onEvent(CaloriesEvent.WaterCupClicked(6))
            testScheduler.runCurrent()

            Assertions.assertEquals(4, viewModel.state.value.waterConsumed)
        }

        @Test
        fun `WaterCupClicked can fill all the way up to the water goal`() = runTest {
            repeat(10) { index -> viewModel.onEvent(CaloriesEvent.WaterCupClicked(4 + index)) }
            testScheduler.runCurrent()

            Assertions.assertEquals(8, viewModel.state.value.waterConsumed)
        }

        @Test
        fun `WaterCupClicked can unfill all the way down to zero`() = runTest {
            repeat(10) { index -> viewModel.onEvent(CaloriesEvent.WaterCupClicked(3 - index)) }
            testScheduler.runCurrent()

            Assertions.assertEquals(0, viewModel.state.value.waterConsumed)
        }

        @Test
        fun `WaterCupLongPressed on the last empty cup deletes it`() = runTest {
            // waterGoal=8 by default — index 7 is the last cup (empty, since waterConsumed=4)
            viewModel.onEvent(CaloriesEvent.WaterCupLongPressed(7))
            testScheduler.runCurrent()

            Assertions.assertEquals(7, viewModel.state.value.waterGoal)
            Assertions.assertEquals(4, viewModel.state.value.waterConsumed)
        }

        @Test
        fun `WaterCupLongPressed on a filled last cup deletes it and drops water consumed`() = runTest {
            repeat(4) { index -> viewModel.onEvent(CaloriesEvent.WaterCupClicked(4 + index)) } // fill up to 8
            viewModel.onEvent(CaloriesEvent.WaterCupLongPressed(7))
            testScheduler.runCurrent()

            Assertions.assertEquals(7, viewModel.state.value.waterGoal)
            Assertions.assertEquals(7, viewModel.state.value.waterConsumed)
        }

        @Test
        fun `WaterCupLongPressed on a non-last cup is a no-op`() = runTest {
            viewModel.onEvent(CaloriesEvent.WaterCupLongPressed(3))
            testScheduler.runCurrent()

            Assertions.assertEquals(8, viewModel.state.value.waterGoal)
            Assertions.assertEquals(4, viewModel.state.value.waterConsumed)
        }

        @Test
        fun `WaterCupLongPressed on the last cup shows a snackbar`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(CaloriesEvent.WaterCupLongPressed(7))
                testScheduler.runCurrent()

                Assertions.assertTrue(awaitItem() is CaloriesEffect.ShowSnackbar)
            }
        }

        @Test
        fun `WaterCupLongPressed on a non-last cup emits no snackbar`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(CaloriesEvent.WaterCupLongPressed(3))
                testScheduler.runCurrent()

                expectNoEvents()
            }
        }
    }

    @Nested
    @DisplayName("Steps Tracking")
    inner class StepsTracking {

        /**
         * Own mocks per test — isolated from the outer shared [viewModel]. Nothing here runs
         * until [CaloriesEvent.StepsCardClicked] is dispatched (the screen fires it once on
         * start); the outer shared viewModel never sees it.
         */
        private fun createViewModel(
            permissionGranted: Boolean = true,
            steps: Flow<Int> = flowOf(0),
        ): Triple<CaloriesViewModel, CheckStepsPermissionUseCase, ObserveTodayStepsUseCase> {
            val permissionUseCase = mockk<CheckStepsPermissionUseCase>()
            val stepsUseCase = mockk<ObserveTodayStepsUseCase>()
            coEvery { permissionUseCase() } returns permissionGranted
            every { stepsUseCase() } returns steps
            val vm = CaloriesViewModel(permissionUseCase, stepsUseCase)
            return Triple(vm, permissionUseCase, stepsUseCase)
        }

        @Test
        fun `when permission already granted, steps load from the sensor`() = runTest {
            val (vm, _, _) = createViewModel(permissionGranted = true, steps = flowOf(4321))

            vm.onEvent(CaloriesEvent.StepsCardClicked)
            testScheduler.runCurrent()

            Assertions.assertEquals(4321, vm.state.value.steps)
            Assertions.assertTrue(vm.state.value.stepsPermissionGranted)
        }

        @Test
        fun `steps update live as new sensor readings arrive`() = runTest {
            val (vm, _, _) = createViewModel(permissionGranted = true, steps = flowOf(10, 25, 40))

            vm.onEvent(CaloriesEvent.StepsCardClicked)
            testScheduler.runCurrent()

            Assertions.assertEquals(40, vm.state.value.steps)
        }

        @Test
        fun `when permission not granted, requests it via effect`() = runTest {
            val (vm, _, _) = createViewModel(permissionGranted = false)

            vm.effect.test {
                vm.onEvent(CaloriesEvent.StepsCardClicked)
                testScheduler.runCurrent()
                Assertions.assertTrue(awaitItem() is CaloriesEffect.RequestStepsPermission)
            }
        }

        @Test
        fun `StepsPermissionResult granted loads steps and starts tracking`() = runTest {
            val (vm, _, _) = createViewModel(permissionGranted = false, steps = flowOf(1500))
            vm.onEvent(CaloriesEvent.StepsCardClicked)
            testScheduler.runCurrent()

            vm.onEvent(CaloriesEvent.StepsPermissionResult(granted = true))
            testScheduler.runCurrent()

            Assertions.assertTrue(vm.state.value.stepsPermissionGranted)
            Assertions.assertEquals(1500, vm.state.value.steps)
        }

        @Test
        fun `StepsPermissionResult denied leaves steps unloaded`() = runTest {
            val (vm, _, _) = createViewModel(permissionGranted = false)
            vm.onEvent(CaloriesEvent.StepsCardClicked)
            testScheduler.runCurrent()

            vm.onEvent(CaloriesEvent.StepsPermissionResult(granted = false))
            testScheduler.runCurrent()

            Assertions.assertFalse(vm.state.value.stepsPermissionGranted)
            Assertions.assertEquals(0, vm.state.value.steps)
        }

        @Test
        fun `StepsCardClicked re-checks permission`() = runTest {
            val (vm, permissionUseCase, _) = createViewModel(permissionGranted = false)
            vm.onEvent(CaloriesEvent.StepsCardClicked)
            testScheduler.runCurrent()

            vm.onEvent(CaloriesEvent.StepsCardClicked)
            testScheduler.runCurrent()

            coVerify(exactly = 2) { permissionUseCase() }
        }
    }
}
