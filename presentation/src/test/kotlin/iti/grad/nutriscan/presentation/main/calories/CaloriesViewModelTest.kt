package iti.grad.nutriscan.presentation.main.calories

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.dailytracking.model.DailyTracking
import iti.grad.nutriscan.domain.dailytracking.usecase.ObserveTodayDailyTrackingUseCase
import iti.grad.nutriscan.domain.dailytracking.usecase.UpdateStepsCntUseCase
import iti.grad.nutriscan.domain.dailytracking.usecase.UpdateTargetWaterCntUseCase
import iti.grad.nutriscan.domain.dailytracking.usecase.UpdateWaterCntUseCase
import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import iti.grad.nutriscan.domain.foodlog.usecase.ObserveTodayFoodLogUseCase
import iti.grad.nutriscan.domain.foodlog.usecase.RemoveFoodEntryUseCase
import iti.grad.nutriscan.domain.steps.usecase.CheckStepsPermissionUseCase
import iti.grad.nutriscan.domain.steps.usecase.ObserveTodayStepsUseCase
import iti.grad.nutriscan.presentation.common.model.BottomNavTab
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEffect
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEvent
import iti.grad.nutriscan.presentation.main.calories.viewmodel.CaloriesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
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
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CaloriesViewModelTest {

    private lateinit var checkStepsPermission: CheckStepsPermissionUseCase
    private lateinit var observeTodaySteps: ObserveTodayStepsUseCase
    private lateinit var observeTodayFoodLog: ObserveTodayFoodLogUseCase
    private lateinit var removeFoodEntry: RemoveFoodEntryUseCase
    private lateinit var observeTodayDailyTracking: ObserveTodayDailyTrackingUseCase
    private lateinit var updateWaterCnt: UpdateWaterCntUseCase
    private lateinit var updateTargetWaterCnt: UpdateTargetWaterCntUseCase
    private lateinit var updateStepsCnt: UpdateStepsCntUseCase
    private lateinit var dailyTrackingFlow: MutableStateFlow<DailyTracking>
    private lateinit var viewModel: CaloriesViewModel
    private val testDispatcher = StandardTestDispatcher()

    private fun foodEntry(id: String = "entry-1", calories: Int = 95) = FoodLogEntry(
        id = id,
        productId = "product-1",
        name = "Apple",
        calories = calories,
        imageUrl = null,
        verdict = ProductVerdict.SAFE,
        loggedDate = LocalDate.now(),
        addedAt = Instant.now(),
    )

    private fun defaultDailyTracking(waterCnt: Int = 4, targetWaterCnt: Int = 8) = DailyTracking(
        date = LocalDate.now(),
        targetWaterCnt = targetWaterCnt,
        waterCnt = waterCnt,
        stepsCnt = 0,
        caloriesBurnedSteps = 0,
        exerciseKcal = 0,
        exerciseMinutes = 0,
        syncedToBackend = false,
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        checkStepsPermission = mockk()
        observeTodaySteps = mockk()
        observeTodayFoodLog = mockk()
        removeFoodEntry = mockk()
        observeTodayDailyTracking = mockk()
        updateWaterCnt = mockk()
        updateTargetWaterCnt = mockk()
        updateStepsCnt = mockk()
        dailyTrackingFlow = MutableStateFlow(defaultDailyTracking())
        every { observeTodayFoodLog() } returns flowOf(emptyList())
        every { observeTodayDailyTracking() } returns dailyTrackingFlow
        coEvery { updateWaterCnt(any()) } returns Result.success(Unit)
        coEvery { updateTargetWaterCnt(any()) } returns Result.success(Unit)
        coEvery { updateStepsCnt(any()) } returns Result.success(Unit)
        viewModel = CaloriesViewModel(
            checkStepsPermission,
            observeTodaySteps,
            observeTodayFoodLog,
            removeFoodEntry,
            observeTodayDailyTracking,
            updateWaterCnt,
            updateTargetWaterCnt,
            updateStepsCnt,
        )
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Initial State")
    inner class InitialState {

        @Test
        fun `initial state matches the mocked dashboard defaults`() = runTest {
            testScheduler.runCurrent()
            val state = viewModel.state.value

            Assertions.assertEquals(2350, state.tdee)
            Assertions.assertEquals(0, state.caloriesGained)
            Assertions.assertTrue(state.addedFoods.isEmpty())
            Assertions.assertEquals(0, state.steps)
            Assertions.assertEquals(10000, state.stepsGoal)
            Assertions.assertFalse(state.stepsPermissionGranted)
            Assertions.assertEquals(0, state.exerciseKcal)
            Assertions.assertEquals(0, state.exerciseMinutes)
            Assertions.assertEquals(4, state.waterConsumed)
            Assertions.assertEquals(8, state.waterGoal)
            Assertions.assertFalse(state.isLoading)
            Assertions.assertNull(state.pendingRemoveFoodId)
        }
    }

    @Nested
    @DisplayName("Navigation")
    inner class Navigation {

        @Test
        fun `AddFoodClicked emits NavigateToSavedProducts and does not mutate state`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(CaloriesEvent.AddFoodClicked)
                testScheduler.runCurrent()

                Assertions.assertTrue(awaitItem() is CaloriesEffect.NavigateToSavedProducts)
            }
            Assertions.assertTrue(viewModel.state.value.addedFoods.isEmpty())
        }

        @Test
        fun `AddExerciseClicked emits NavigateToExercises`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(CaloriesEvent.AddExerciseClicked)
                testScheduler.runCurrent()

                Assertions.assertTrue(awaitItem() is CaloriesEffect.NavigateToExercises)
            }
        }
    }

    @Nested
    @DisplayName("Food Log")
    inner class FoodLog {

        private fun createViewModel(entries: Flow<List<FoodLogEntry>>): CaloriesViewModel {
            val foodLogUseCase = mockk<ObserveTodayFoodLogUseCase>()
            every { foodLogUseCase() } returns entries
            return CaloriesViewModel(
                checkStepsPermission,
                observeTodaySteps,
                foodLogUseCase,
                removeFoodEntry,
                observeTodayDailyTracking,
                updateWaterCnt,
                updateTargetWaterCnt,
                updateStepsCnt,
            )
        }

        @Test
        fun `observed food log populates addedFoods and caloriesGained`() = runTest {
            val vm = createViewModel(flowOf(listOf(foodEntry(calories = 95), foodEntry(id = "entry-2", calories = 105))))
            testScheduler.runCurrent()

            Assertions.assertEquals(2, vm.state.value.addedFoods.size)
            Assertions.assertEquals(200, vm.state.value.caloriesGained)
        }

        @Test
        fun `FoodItemSwipedToRemove sets pendingRemoveFoodId without removing`() = runTest {
            val vm = createViewModel(flowOf(listOf(foodEntry(id = "entry-1"))))
            testScheduler.runCurrent()

            vm.onEvent(CaloriesEvent.FoodItemSwipedToRemove("entry-1"))
            testScheduler.runCurrent()

            Assertions.assertEquals("entry-1", vm.state.value.pendingRemoveFoodId)
            Assertions.assertEquals(1, vm.state.value.addedFoods.size)
        }

        @Test
        fun `RemoveFoodConfirmed calls RemoveFoodEntryUseCase and clears pending id`() = runTest {
            val vm = createViewModel(flowOf(listOf(foodEntry(id = "entry-1"))))
            coEvery { removeFoodEntry("entry-1") } returns Result.success(Unit)
            testScheduler.runCurrent()

            vm.onEvent(CaloriesEvent.FoodItemSwipedToRemove("entry-1"))
            vm.onEvent(CaloriesEvent.RemoveFoodConfirmed)
            testScheduler.runCurrent()

            coVerify(exactly = 1) { removeFoodEntry("entry-1") }
            Assertions.assertNull(vm.state.value.pendingRemoveFoodId)
        }

        @Test
        fun `RemoveFoodDismissed clears pending id without calling the use case`() = runTest {
            val vm = createViewModel(flowOf(listOf(foodEntry(id = "entry-1"))))
            testScheduler.runCurrent()

            vm.onEvent(CaloriesEvent.FoodItemSwipedToRemove("entry-1"))
            vm.onEvent(CaloriesEvent.RemoveFoodDismissed)
            testScheduler.runCurrent()

            coVerify(exactly = 0) { removeFoodEntry(any()) }
            Assertions.assertNull(vm.state.value.pendingRemoveFoodId)
        }

        @Test
        fun `RemoveFoodConfirmed failure shows an error snackbar`() = runTest {
            val vm = createViewModel(flowOf(listOf(foodEntry(id = "entry-1"))))
            coEvery { removeFoodEntry("entry-1") } returns Result.failure(IllegalStateException("Not authenticated"))
            testScheduler.runCurrent()

            vm.onEvent(CaloriesEvent.FoodItemSwipedToRemove("entry-1"))
            vm.effect.test {
                vm.onEvent(CaloriesEvent.RemoveFoodConfirmed)
                testScheduler.runCurrent()

                Assertions.assertTrue(awaitItem() is CaloriesEffect.ShowSnackbar)
            }
        }
    }

    @Nested
    @DisplayName("Water Tracking")
    inner class WaterTracking {

        // Default dailyTrackingFlow: waterCnt=4, targetWaterCnt=8. Water state no longer mutates
        // in memory — the ViewModel calls updateWaterCnt/updateTargetWaterCnt and waits for the
        // round trip back through observeDailyTracking(). Tests verify the use-case call args;
        // where a test needs to observe cascading (e.g. filling multiple cups), the shared
        // dailyTrackingFlow is nudged manually to simulate the persisted value coming back,
        // mirroring what the real repository would emit after a successful write.

        @Test
        fun `AddWaterClicked adds an empty cup without filling it`() = runTest {
            viewModel.onEvent(CaloriesEvent.AddWaterClicked)
            testScheduler.runCurrent()

            coVerify { updateTargetWaterCnt(9) }
        }

        @Test
        fun `WaterCupClicked on the next empty cup fills it`() = runTest {
            // waterConsumed=4, waterGoal=8 by default — index 4 is the next empty cup
            viewModel.onEvent(CaloriesEvent.WaterCupClicked(4))
            testScheduler.runCurrent()

            coVerify { updateWaterCnt(5) }
        }

        @Test
        fun `WaterCupClicked on the last filled cup unfills it`() = runTest {
            // waterConsumed=4 by default — index 3 is the last filled cup
            viewModel.onEvent(CaloriesEvent.WaterCupClicked(3))
            testScheduler.runCurrent()

            coVerify { updateWaterCnt(3) }
        }

        @Test
        fun `WaterCupClicked out of order is a no-op`() = runTest {
            // index 6 is neither the next empty cup (4) nor the last filled one (3)
            viewModel.onEvent(CaloriesEvent.WaterCupClicked(6))
            testScheduler.runCurrent()

            coVerify(exactly = 0) { updateWaterCnt(any()) }
        }

        @Test
        fun `WaterCupClicked can fill all the way up to the water goal`() = runTest {
            var current = 4
            repeat(4) {
                viewModel.onEvent(CaloriesEvent.WaterCupClicked(current))
                testScheduler.runCurrent()
                current++
                dailyTrackingFlow.update { it.copy(waterCnt = current) }
                testScheduler.runCurrent()
            }

            coVerify { updateWaterCnt(8) }
        }

        @Test
        fun `WaterCupClicked can unfill all the way down to zero`() = runTest {
            var current = 4
            repeat(4) {
                viewModel.onEvent(CaloriesEvent.WaterCupClicked(current - 1))
                testScheduler.runCurrent()
                current--
                dailyTrackingFlow.update { it.copy(waterCnt = current) }
                testScheduler.runCurrent()
            }

            coVerify { updateWaterCnt(0) }
        }

        @Test
        fun `WaterCupLongPressed on the last empty cup deletes it`() = runTest {
            // waterGoal=8 by default — index 7 is the last cup (empty, since waterConsumed=4)
            viewModel.onEvent(CaloriesEvent.WaterCupLongPressed(7))
            testScheduler.runCurrent()

            coVerify { updateTargetWaterCnt(7) }
            coVerify { updateWaterCnt(4) }
        }

        @Test
        fun `WaterCupLongPressed on a filled last cup deletes it and drops water consumed`() = runTest {
            var current = 4
            repeat(4) {
                viewModel.onEvent(CaloriesEvent.WaterCupClicked(current)) // fill up to 8
                testScheduler.runCurrent()
                current++
                dailyTrackingFlow.update { it.copy(waterCnt = current) }
                testScheduler.runCurrent()
            }

            viewModel.onEvent(CaloriesEvent.WaterCupLongPressed(7))
            testScheduler.runCurrent()

            coVerify { updateTargetWaterCnt(7) }
            coVerify { updateWaterCnt(7) }
        }

        @Test
        fun `WaterCupLongPressed on a non-last cup is a no-op`() = runTest {
            viewModel.onEvent(CaloriesEvent.WaterCupLongPressed(3))
            testScheduler.runCurrent()

            coVerify(exactly = 0) { updateTargetWaterCnt(any()) }
            coVerify(exactly = 0) { updateWaterCnt(any()) }
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
            val vm = CaloriesViewModel(
                permissionUseCase,
                stepsUseCase,
                observeTodayFoodLog,
                removeFoodEntry,
                observeTodayDailyTracking,
                updateWaterCnt,
                updateTargetWaterCnt,
                updateStepsCnt,
            )
            return Triple(vm, permissionUseCase, stepsUseCase)
        }

        @Test
        fun `when permission already granted, steps load from the sensor`() = runTest {
            val (vm, _, _) = createViewModel(permissionGranted = true, steps = flowOf(4321))

            vm.onEvent(CaloriesEvent.StepsCardClicked)
            testScheduler.runCurrent()

            Assertions.assertEquals(4321, vm.state.value.steps)
            Assertions.assertTrue(vm.state.value.stepsPermissionGranted)
            coVerify { updateStepsCnt(4321) }
        }

        @Test
        fun `steps update live as new sensor readings arrive`() = runTest {
            val (vm, _, _) = createViewModel(permissionGranted = true, steps = flowOf(10, 25, 40))

            vm.onEvent(CaloriesEvent.StepsCardClicked)
            testScheduler.runCurrent()

            Assertions.assertEquals(40, vm.state.value.steps)
            coVerify { updateStepsCnt(40) }
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

    @Nested
    @DisplayName("Exercise Stats Observation")
    inner class ExerciseStatsObservation {

        @Test
        fun `CaloriesViewModel state updates when the daily tracking flow emits new exercise totals`() = runTest {
            testScheduler.runCurrent()

            dailyTrackingFlow.update { it.copy(exerciseKcal = 120, exerciseMinutes = 15) }
            testScheduler.runCurrent()

            val state = viewModel.state.value
            Assertions.assertEquals(120, state.exerciseKcal)
            Assertions.assertEquals(15, state.exerciseMinutes)
        }
    }
}
