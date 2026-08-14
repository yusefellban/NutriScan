package iti.grad.nutriscan.presentation.main.calories

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.dailytracking.model.DailyTracking
import iti.grad.nutriscan.domain.dailytracking.usecase.ObserveTodayDailyTrackingUseCase
import iti.grad.nutriscan.domain.dailytracking.usecase.ReconcileTodayUseCase
import iti.grad.nutriscan.domain.dailytracking.usecase.UpdateStepsCntUseCase
import iti.grad.nutriscan.domain.dailytracking.usecase.UpdateTargetWaterCntUseCase
import iti.grad.nutriscan.domain.dailytracking.usecase.UpdateWaterCntUseCase
import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import iti.grad.nutriscan.domain.foodlog.usecase.ObserveTodayFoodLogUseCase
import iti.grad.nutriscan.domain.foodlog.usecase.RemoveFoodEntryUseCase
import iti.grad.nutriscan.domain.steps.usecase.CheckStepsPermissionUseCase
import iti.grad.nutriscan.domain.steps.usecase.ObserveTodayStepsUseCase
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import iti.grad.nutriscan.presentation.common.model.BottomNavTab
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEffect
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEvent
import iti.grad.nutriscan.presentation.main.calories.viewmodel.CaloriesViewModel
import kotlinx.coroutines.CompletableDeferred
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
    private lateinit var reconcileToday: ReconcileTodayUseCase
    private lateinit var userRepository: IUserRepository
    private lateinit var dailyTrackingFlow: MutableStateFlow<DailyTracking>
    private lateinit var viewModel: CaloriesViewModel
    private val testDispatcher = StandardTestDispatcher()

    private fun foodEntry(
        id: String = "entry-1",
        calories: Int = 95,
        productId: String = "product-1",
        mealCnt: Int = 1,
    ) = FoodLogEntry(
        id = id,
        productId = productId,
        name = "Apple",
        calories = calories,
        imageUrl = null,
        verdict = ProductVerdict.SAFE,
        loggedDate = LocalDate.now(),
        addedAt = Instant.now(),
        mealCnt = mealCnt,
    )

    private fun defaultDailyTracking(
        waterCnt: Int = 4,
        targetWaterCnt: Int = 8,
        caloriesBurnedSteps: Int = 0,
        exerciseKcal: Int = 0,
    ) = DailyTracking(
        date = LocalDate.now(),
        targetWaterCnt = targetWaterCnt,
        waterCnt = waterCnt,
        stepsCnt = 0,
        caloriesBurnedSteps = caloriesBurnedSteps,
        exerciseKcal = exerciseKcal,
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
        reconcileToday = mockk()
        coEvery { reconcileToday() } returns Result.success(Unit)
        userRepository = mockk()
        dailyTrackingFlow = MutableStateFlow(defaultDailyTracking())
        every { observeTodayFoodLog() } returns flowOf(emptyList())
        every { observeTodayDailyTracking() } returns dailyTrackingFlow
        every { userRepository.getUserData() } returns flowOf(null)
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
            reconcileToday,
            userRepository,
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

            Assertions.assertEquals(0, state.tdee)
            Assertions.assertEquals(0, state.caloriesGained)
            Assertions.assertEquals(0, state.caloriesBurned)
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
                reconcileToday,
                userRepository,
            )
        }

        @Test
        fun `observed food log populates addedFoods and caloriesGained`() = runTest {
            val vm = createViewModel(
                flowOf(
                    listOf(
                        foodEntry(calories = 95, productId = "product-1"),
                        foodEntry(id = "entry-2", calories = 105, productId = "product-2"),
                    )
                )
            )
            testScheduler.runCurrent()

            Assertions.assertEquals(2, vm.state.value.addedFoods.size)
            Assertions.assertEquals(200, vm.state.value.caloriesGained)
        }

        @Test
        fun `a single entry with mealCnt above one shows the quantity badge and multiplies calories gained`() = runTest {
            val entry = foodEntry(id = "entry-1", calories = 95, mealCnt = 2)
            val vm = createViewModel(flowOf(listOf(entry)))
            testScheduler.runCurrent()

            Assertions.assertEquals(1, vm.state.value.addedFoods.size)
            val card = vm.state.value.addedFoods.first()
            Assertions.assertEquals(2, card.quantity)
            Assertions.assertEquals("entry-1", card.logEntryId)
            Assertions.assertEquals("95", card.calories)
            Assertions.assertEquals(190, vm.state.value.caloriesGained)
        }

        @Test
        fun `swiping a card with quantity above one calls removeFoodEntry and reflects the next emission`() = runTest {
            val entry = foodEntry(id = "entry-1", calories = 95, mealCnt = 2)
            val entriesFlow = MutableStateFlow(listOf(entry))
            val foodLogUseCase = mockk<ObserveTodayFoodLogUseCase>()
            every { foodLogUseCase() } returns entriesFlow
            coEvery { removeFoodEntry("entry-1") } returns Result.success(Unit)
            val vm = CaloriesViewModel(
                checkStepsPermission,
                observeTodaySteps,
                foodLogUseCase,
                removeFoodEntry,
                observeTodayDailyTracking,
                updateWaterCnt,
                updateTargetWaterCnt,
                updateStepsCnt,
                reconcileToday,
                userRepository,
            )
            testScheduler.runCurrent()

            val card = vm.state.value.addedFoods.first()
            vm.onEvent(CaloriesEvent.FoodItemDeleteClicked(card.logEntryId!!))
            vm.onEvent(CaloriesEvent.RemoveFoodConfirmed)
            testScheduler.runCurrent()

            coVerify(exactly = 1) { removeFoodEntry("entry-1") }

            // Simulate the repository's next emission once the PUT decrement is confirmed —
            // the ViewModel does no count math itself, it just re-renders what it's given.
            entriesFlow.value = listOf(entry.copy(mealCnt = 1))
            testScheduler.runCurrent()

            val remaining = vm.state.value.addedFoods.first()
            Assertions.assertEquals(1, vm.state.value.addedFoods.size)
            Assertions.assertEquals(1, remaining.quantity)
            Assertions.assertEquals("entry-1", remaining.logEntryId)
        }

        @Test
        fun `FoodItemDeleteClicked sets pendingRemoveFoodId without removing`() = runTest {
            val vm = createViewModel(flowOf(listOf(foodEntry(id = "entry-1"))))
            testScheduler.runCurrent()

            vm.onEvent(CaloriesEvent.FoodItemDeleteClicked("entry-1"))
            testScheduler.runCurrent()

            Assertions.assertEquals("entry-1", vm.state.value.pendingRemoveFoodId)
            Assertions.assertEquals(1, vm.state.value.addedFoods.size)
        }

        @Test
        fun `RemoveFoodConfirmed calls RemoveFoodEntryUseCase and clears pending id`() = runTest {
            val vm = createViewModel(flowOf(listOf(foodEntry(id = "entry-1"))))
            coEvery { removeFoodEntry("entry-1") } returns Result.success(Unit)
            testScheduler.runCurrent()

            vm.onEvent(CaloriesEvent.FoodItemDeleteClicked("entry-1"))
            vm.onEvent(CaloriesEvent.RemoveFoodConfirmed)
            testScheduler.runCurrent()

            coVerify(exactly = 1) { removeFoodEntry("entry-1") }
            Assertions.assertNull(vm.state.value.pendingRemoveFoodId)
        }

        @Test
        fun `RemoveFoodDismissed clears pending id without calling the use case`() = runTest {
            val vm = createViewModel(flowOf(listOf(foodEntry(id = "entry-1"))))
            testScheduler.runCurrent()

            vm.onEvent(CaloriesEvent.FoodItemDeleteClicked("entry-1"))
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

            vm.onEvent(CaloriesEvent.FoodItemDeleteClicked("entry-1"))
            vm.effect.test {
                vm.onEvent(CaloriesEvent.RemoveFoodConfirmed)
                testScheduler.runCurrent()

                Assertions.assertTrue(awaitItem() is CaloriesEffect.ShowSnackbar)
            }
        }
    }

    @Nested
    @DisplayName("Pull To Refresh")
    inner class PullToRefresh {

        @Test
        fun `Refreshed pulls today from the backend and clears the refreshing flag`() = runTest {
            viewModel.onEvent(CaloriesEvent.Refreshed)
            testScheduler.runCurrent()

            coVerify(exactly = 1) { reconcileToday() }
            Assertions.assertFalse(viewModel.state.value.isRefreshing)
        }

        @Test
        fun `Refreshed while already refreshing does not fire a second pull`() = runTest {
            // Suspends until released, so the first refresh is still in flight for the second event.
            val gate = CompletableDeferred<Result<Unit>>()
            coEvery { reconcileToday() } coAnswers { gate.await() }

            viewModel.onEvent(CaloriesEvent.Refreshed)
            testScheduler.runCurrent()
            Assertions.assertTrue(viewModel.state.value.isRefreshing)

            viewModel.onEvent(CaloriesEvent.Refreshed)
            testScheduler.runCurrent()

            coVerify(exactly = 1) { reconcileToday() }

            gate.complete(Result.success(Unit))
            testScheduler.runCurrent()
            Assertions.assertFalse(viewModel.state.value.isRefreshing)
        }

        @Test
        fun `Refreshed failure shows an error snackbar and still clears the refreshing flag`() = runTest {
            coEvery { reconcileToday() } returns Result.failure(IllegalStateException("offline"))

            viewModel.effect.test {
                viewModel.onEvent(CaloriesEvent.Refreshed)
                testScheduler.runCurrent()

                Assertions.assertTrue(awaitItem() is CaloriesEffect.ShowSnackbar)
            }
            Assertions.assertFalse(viewModel.state.value.isRefreshing)
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
        fun `AddWaterClicked adds an empty cup and persists the new goal`() = runTest {
            viewModel.onEvent(CaloriesEvent.AddWaterClicked)
            testScheduler.runCurrent()

            coVerify { updateTargetWaterCnt(9) }
        }

        @Test
        fun `WaterCupClicked on the next empty cup fills it and logs a glass`() = runTest {
            // waterConsumed=4, waterGoal=8 by default — index 4 is the next empty cup
            viewModel.onEvent(CaloriesEvent.WaterCupClicked(4))
            testScheduler.runCurrent()

            coVerify { updateWaterCnt(5) }
        }

        @Test
        fun `WaterCupClicked on the last filled cup unfills it and unlogs a glass`() = runTest {
            // waterConsumed=4 by default — index 3 is the last filled cup
            viewModel.onEvent(CaloriesEvent.WaterCupClicked(3))
            testScheduler.runCurrent()

            coVerify { updateWaterCnt(3) }
        }

        @Test
        fun `WaterCupClicked on an empty cup ahead of the next one jumps straight to it`() = runTest {
            // waterConsumed=4 by default — index 6 is empty but not the immediate next cup
            viewModel.onEvent(CaloriesEvent.WaterCupClicked(6))
            testScheduler.runCurrent()

            coVerify { updateWaterCnt(7) }
        }

        @Test
        fun `WaterCupClicked on a filled cup other than the last one is a no-op`() = runTest {
            // waterConsumed=4 by default — index 1 is filled but not the last filled cup
            viewModel.onEvent(CaloriesEvent.WaterCupClicked(1))
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
        fun `WaterCupLongPressed on the last empty cup deletes it and persists the new goal`() = runTest {
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
        private fun createStepsViewModel(
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
                reconcileToday,
                userRepository,
            )
            return Triple(vm, permissionUseCase, stepsUseCase)
        }

        @Test
        fun `when permission already granted, steps load from the sensor`() = runTest {
            val (vm, _, _) = createStepsViewModel(permissionGranted = true, steps = flowOf(4321))

            vm.onEvent(CaloriesEvent.StepsCardClicked)
            testScheduler.runCurrent()

            Assertions.assertEquals(4321, vm.state.value.steps)
            Assertions.assertTrue(vm.state.value.stepsPermissionGranted)
            coVerify { updateStepsCnt(4321) }
        }

        @Test
        fun `steps update live as new sensor readings arrive`() = runTest {
            val (vm, _, _) = createStepsViewModel(permissionGranted = true, steps = flowOf(10, 25, 40))

            vm.onEvent(CaloriesEvent.StepsCardClicked)
            testScheduler.runCurrent()

            Assertions.assertEquals(40, vm.state.value.steps)
            coVerify { updateStepsCnt(40) }
        }

        @Test
        fun `when permission not granted, requests it via effect`() = runTest {
            val (vm, _, _) = createStepsViewModel(permissionGranted = false)

            vm.effect.test {
                vm.onEvent(CaloriesEvent.StepsCardClicked)
                testScheduler.runCurrent()
                Assertions.assertTrue(awaitItem() is CaloriesEffect.RequestStepsPermission)
            }
        }

        @Test
        fun `StepsPermissionResult granted loads steps and starts tracking`() = runTest {
            val (vm, _, _) = createStepsViewModel(permissionGranted = false, steps = flowOf(1500))
            vm.onEvent(CaloriesEvent.StepsCardClicked)
            testScheduler.runCurrent()

            vm.onEvent(CaloriesEvent.StepsPermissionResult(granted = true))
            testScheduler.runCurrent()

            Assertions.assertTrue(vm.state.value.stepsPermissionGranted)
            Assertions.assertEquals(1500, vm.state.value.steps)
        }

        @Test
        fun `StepsPermissionResult denied leaves steps unloaded`() = runTest {
            val (vm, _, _) = createStepsViewModel(permissionGranted = false)
            vm.onEvent(CaloriesEvent.StepsCardClicked)
            testScheduler.runCurrent()

            vm.onEvent(CaloriesEvent.StepsPermissionResult(granted = false))
            testScheduler.runCurrent()

            Assertions.assertFalse(vm.state.value.stepsPermissionGranted)
            Assertions.assertEquals(0, vm.state.value.steps)
        }

        @Test
        fun `StepsCardClicked re-checks permission`() = runTest {
            val (vm, permissionUseCase, _) = createStepsViewModel(permissionGranted = false)
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

        @Test
        fun `caloriesBurned sums steps and exercise kcal from the daily tracking flow`() = runTest {
            dailyTrackingFlow.update { it.copy(caloriesBurnedSteps = 35, exerciseKcal = 120) }
            testScheduler.runCurrent()

            Assertions.assertEquals(155, viewModel.state.value.caloriesBurned)
        }
    }

    @Nested
    @DisplayName("TDEE Observation")
    inner class UserMetricsObservation {

        @Test
        fun `CaloriesViewModel state populates tdee from the user profile`() = runTest {
            val user = iti.grad.nutriscan.domain.user.model.User(
                id = "user-1",
                firstName = "Test",
                lastName = null,
                email = "test@test.com",
                gender = null,
                dateOfBirth = null,
                heightCm = 170.0,
                weightKg = 70.0,
                diseaseIds = emptyList(),
                allergyIds = emptyList(),
                bmi = 24.2,
                tdee = 2350.0,
            )
            every { userRepository.getUserData() } returns flowOf(user)
            val vm = CaloriesViewModel(
                checkStepsPermission,
                observeTodaySteps,
                observeTodayFoodLog,
                removeFoodEntry,
                observeTodayDailyTracking,
                updateWaterCnt,
                updateTargetWaterCnt,
                updateStepsCnt,
                reconcileToday,
                userRepository,
            )
            testScheduler.runCurrent()

            Assertions.assertEquals(2350, vm.state.value.tdee)
        }

        @Test
        fun `tdee stays at default when the user has no computed metrics yet`() = runTest {
            testScheduler.runCurrent()

            Assertions.assertEquals(0, viewModel.state.value.tdee)
        }
    }
}
