package iti.grad.nutriscan.presentation.exercises.workout.viewmodel

import app.cash.turbine.test
import iti.grad.nutriscan.presentation.common.model.ExerciseType
import iti.grad.nutriscan.presentation.exercises.tracker.ExercisesSharedTracker
import iti.grad.nutriscan.presentation.exercises.workout.state.ExerciseWorkoutEffect
import iti.grad.nutriscan.presentation.exercises.workout.state.ExerciseWorkoutEvent
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
class ExerciseWorkoutViewModelTest {

    private lateinit var viewModel: ExerciseWorkoutViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ExerciseWorkoutViewModel()
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Initial State & Exercise Initialization")
    inner class InitState {

        @Test
        fun `initial workout state has default values`() {
            val state = viewModel.state.value
            Assertions.assertNull(state.exercise)
            Assertions.assertEquals(0, state.secondsElapsed)
            Assertions.assertFalse(state.isTimerRunning)
            Assertions.assertEquals(1, state.sets)
            Assertions.assertEquals(1, state.reps)
        }

        @Test
        fun `InitExercise sets exercise object and resets counters`() = runTest {
            viewModel.onEvent(ExerciseWorkoutEvent.InitExercise("1"))
            testScheduler.runCurrent()

            val state = viewModel.state.value
            Assertions.assertNotNull(state.exercise)
            Assertions.assertEquals("1", state.exercise?.id)
            Assertions.assertEquals(ExerciseType.CARDIO, state.exercise?.type)
            Assertions.assertEquals(0, state.secondsElapsed)
        }
    }

    @Nested
    @DisplayName("Timer Operation")
    inner class TimerOperation {

        @Test
        fun `OnStartResumeClick starts the timer and updates isTimerRunning`() = runTest {
            viewModel.onEvent(ExerciseWorkoutEvent.InitExercise("1"))
            viewModel.onEvent(ExerciseWorkoutEvent.OnStartResumeClick)
            testScheduler.runCurrent()

            Assertions.assertTrue(viewModel.state.value.isTimerRunning)
        }

        @Test
        fun `OnPauseClick stops the timer`() = runTest {
            viewModel.onEvent(ExerciseWorkoutEvent.InitExercise("1"))
            viewModel.onEvent(ExerciseWorkoutEvent.OnStartResumeClick)
            viewModel.onEvent(ExerciseWorkoutEvent.OnPauseClick)
            testScheduler.runCurrent()

            Assertions.assertFalse(viewModel.state.value.isTimerRunning)
        }

        @Test
        fun `OnTimerTick increments secondsElapsed`() = runTest {
            viewModel.onEvent(ExerciseWorkoutEvent.InitExercise("1"))
            viewModel.onEvent(ExerciseWorkoutEvent.OnTimerTick)
            testScheduler.runCurrent()

            Assertions.assertEquals(1, viewModel.state.value.secondsElapsed)
        }
    }

    @Nested
    @DisplayName("Sets & Reps Controls")
    inner class SetsRepsControls {

        @BeforeEach
        fun prepare() {
            // "2" is Normal Workout
            viewModel.onEvent(ExerciseWorkoutEvent.InitExercise("2"))
        }

        @Test
        fun `set increment increases sets by one`() = runTest {
            viewModel.onEvent(ExerciseWorkoutEvent.OnSetIncrement)
            testScheduler.runCurrent()

            Assertions.assertEquals(2, viewModel.state.value.sets)
        }

        @Test
        fun `set decrement decreases sets but not below one`() = runTest {
            viewModel.onEvent(ExerciseWorkoutEvent.OnSetDecrement)
            testScheduler.runCurrent()

            Assertions.assertEquals(1, viewModel.state.value.sets)
        }

        @Test
        fun `rep increment increases reps by one`() = runTest {
            viewModel.onEvent(ExerciseWorkoutEvent.OnRepIncrement)
            testScheduler.runCurrent()

            Assertions.assertEquals(2, viewModel.state.value.reps)
        }

        @Test
        fun `rep decrement decreases reps but not below one`() = runTest {
            viewModel.onEvent(ExerciseWorkoutEvent.OnRepDecrement)
            testScheduler.runCurrent()

            Assertions.assertEquals(1, viewModel.state.value.reps)
        }

        @Test
        fun `OnSetChange updates sets to custom value`() = runTest {
            viewModel.onEvent(ExerciseWorkoutEvent.OnSetChange(5))
            testScheduler.runCurrent()

            Assertions.assertEquals(5, viewModel.state.value.sets)
        }

        @Test
        fun `OnRepChange updates reps to custom value`() = runTest {
            viewModel.onEvent(ExerciseWorkoutEvent.OnRepChange(12))
            testScheduler.runCurrent()

            Assertions.assertEquals(12, viewModel.state.value.reps)
        }
    }

    @Nested
    @DisplayName("Finish & Calorie Calculations")
    inner class FinishWorkout {

        @Test
        fun `finish cardio exercise calculates calories using time`() = runTest {
            viewModel.onEvent(ExerciseWorkoutEvent.InitExercise("1")) // Cardio (warm up, 0.23 kcal/min)
            // Simulating 5 minutes (300 seconds)
            repeat(300) {
                viewModel.onEvent(ExerciseWorkoutEvent.OnTimerTick)
            }
            viewModel.onEvent(ExerciseWorkoutEvent.OnFinishClick)
            testScheduler.runCurrent()

            // 5 * 0.23 = 1.15 -> round to 1
            Assertions.assertEquals(1, viewModel.state.value.caloriesBurned)
            Assertions.assertTrue(viewModel.state.value.showCongratsDialog)
        }

        @Test
        fun `finish normal workout calculates calories using sets and reps`() = runTest {
            viewModel.onEvent(ExerciseWorkoutEvent.InitExercise("2")) // Normal (sit up, 0.3 kcal/rep)
            viewModel.onEvent(ExerciseWorkoutEvent.OnSetChange(3))
            viewModel.onEvent(ExerciseWorkoutEvent.OnRepChange(10))
            viewModel.onEvent(ExerciseWorkoutEvent.OnFinishClick)
            testScheduler.runCurrent()

            // 3 * 10 * 0.3 = 9
            Assertions.assertEquals(9, viewModel.state.value.caloriesBurned)
            Assertions.assertTrue(viewModel.state.value.showCongratsDialog)
        }
    }

    @Nested
    @DisplayName("Navigation Effects")
    inner class NavEffects {

        @Test
        fun `OnCancelClick emits NavigateToCaloriesDashboard`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(ExerciseWorkoutEvent.OnCancelClick)
                testScheduler.runCurrent()

                val effect = awaitItem()
                Assertions.assertEquals(ExerciseWorkoutEffect.NavigateToCaloriesDashboard, effect)
            }
        }

        @Test
        fun `OnCongratsDialogConfirm emits NavigateToCaloriesDashboard`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(ExerciseWorkoutEvent.InitExercise("1"))
                viewModel.onEvent(ExerciseWorkoutEvent.OnFinishClick)
                viewModel.onEvent(ExerciseWorkoutEvent.OnCongratsDialogConfirm)
                testScheduler.runCurrent()

                val effect = awaitItem()
                Assertions.assertEquals(ExerciseWorkoutEffect.NavigateToCaloriesDashboard, effect)
            }
        }

        @Test
        fun `OnBackClick emits NavigateBack`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(ExerciseWorkoutEvent.OnBackClick)
                testScheduler.runCurrent()

                val effect = awaitItem()
                Assertions.assertEquals(ExerciseWorkoutEffect.NavigateBack, effect)
            }
        }
    }
}
