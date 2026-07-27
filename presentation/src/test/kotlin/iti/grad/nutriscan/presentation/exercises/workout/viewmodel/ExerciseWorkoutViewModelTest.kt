package iti.grad.nutriscan.presentation.exercises.workout.viewmodel

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.domain.dailytracking.usecase.AddExerciseWorkoutUseCase
import iti.grad.nutriscan.domain.exercises.model.Exercise
import iti.grad.nutriscan.domain.exercises.model.ExercisePage
import iti.grad.nutriscan.domain.exercises.model.ExerciseQuery
import iti.grad.nutriscan.domain.exercises.repository.IExercisesRepository
import iti.grad.nutriscan.domain.exercises.usecase.GetExerciseByIdUseCase
import iti.grad.nutriscan.presentation.common.model.ExerciseType
import iti.grad.nutriscan.presentation.exercises.workout.state.ExerciseWorkoutEffect
import iti.grad.nutriscan.presentation.exercises.workout.state.ExerciseWorkoutEvent
import iti.grad.presentation.R
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

    private lateinit var repository: FakeExercisesRepository
    private lateinit var getExerciseByIdUseCase: GetExerciseByIdUseCase
    private lateinit var addExerciseWorkout: AddExerciseWorkoutUseCase
    private lateinit var viewModel: ExerciseWorkoutViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeExercisesRepository()
        getExerciseByIdUseCase = GetExerciseByIdUseCase(repository)
        addExerciseWorkout = mockk()
        coEvery { addExerciseWorkout(any(), any()) } returns Result.success(Unit)
        viewModel = ExerciseWorkoutViewModel(getExerciseByIdUseCase, addExerciseWorkout)
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
            testDispatcher.scheduler.runCurrent()

            val state = viewModel.state.value
            Assertions.assertNotNull(state.exercise)
            Assertions.assertEquals("1", state.exercise?.id)
            Assertions.assertEquals(ExerciseType.CARDIO, state.exercise?.type)
            Assertions.assertEquals(0, state.secondsElapsed)
        }

        @Test
        fun `InitExercise load failure sets error state and retrying reloads`() = runTest {
            repository.shouldFail = true
            viewModel.onEvent(ExerciseWorkoutEvent.InitExercise("1"))
            testDispatcher.scheduler.runCurrent()

            Assertions.assertNull(viewModel.state.value.exercise)
            Assertions.assertEquals(R.string.exercises_load_error, viewModel.state.value.errorMessageRes)

            repository.shouldFail = false
            viewModel.onEvent(ExerciseWorkoutEvent.OnRetryInitClick)
            testDispatcher.scheduler.runCurrent()

            Assertions.assertNull(viewModel.state.value.errorMessageRes)
            Assertions.assertNotNull(viewModel.state.value.exercise)
            Assertions.assertEquals("1", viewModel.state.value.exercise?.id)
        }
    }

    @Nested
    @DisplayName("Timer Operation")
    inner class TimerOperation {

        @Test
        fun `OnStartResumeClick starts the timer and updates isTimerRunning`() = runTest {
            viewModel.onEvent(ExerciseWorkoutEvent.InitExercise("1"))
            testDispatcher.scheduler.runCurrent()
            viewModel.onEvent(ExerciseWorkoutEvent.OnStartResumeClick)
            testDispatcher.scheduler.runCurrent()

            Assertions.assertTrue(viewModel.state.value.isTimerRunning)
        }

        @Test
        fun `OnPauseClick stops the timer`() = runTest {
            viewModel.onEvent(ExerciseWorkoutEvent.InitExercise("1"))
            testDispatcher.scheduler.runCurrent()
            viewModel.onEvent(ExerciseWorkoutEvent.OnStartResumeClick)
            viewModel.onEvent(ExerciseWorkoutEvent.OnPauseClick)
            testDispatcher.scheduler.runCurrent()

            Assertions.assertFalse(viewModel.state.value.isTimerRunning)
        }

        @Test
        fun `OnTimerTick increments secondsElapsed`() = runTest {
            viewModel.onEvent(ExerciseWorkoutEvent.InitExercise("1"))
            testDispatcher.scheduler.runCurrent()
            viewModel.onEvent(ExerciseWorkoutEvent.OnTimerTick)
            testDispatcher.scheduler.runCurrent()

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
            testDispatcher.scheduler.runCurrent()
        }

        @Test
        fun `set increment increases sets by one`() = runTest {
            viewModel.onEvent(ExerciseWorkoutEvent.OnSetIncrement)
            testDispatcher.scheduler.runCurrent()

            Assertions.assertEquals(2, viewModel.state.value.sets)
        }

        @Test
        fun `set decrement decreases sets but not below one`() = runTest {
            viewModel.onEvent(ExerciseWorkoutEvent.OnSetDecrement)
            testDispatcher.scheduler.runCurrent()

            Assertions.assertEquals(1, viewModel.state.value.sets)
        }

        @Test
        fun `rep increment increases reps by one`() = runTest {
            viewModel.onEvent(ExerciseWorkoutEvent.OnRepIncrement)
            testDispatcher.scheduler.runCurrent()

            Assertions.assertEquals(2, viewModel.state.value.reps)
        }

        @Test
        fun `rep decrement decreases reps but not below one`() = runTest {
            viewModel.onEvent(ExerciseWorkoutEvent.OnRepDecrement)
            testDispatcher.scheduler.runCurrent()

            Assertions.assertEquals(1, viewModel.state.value.reps)
        }

        @Test
        fun `OnSetChange updates sets to custom value`() = runTest {
            viewModel.onEvent(ExerciseWorkoutEvent.OnSetChange(5))
            testDispatcher.scheduler.runCurrent()

            Assertions.assertEquals(5, viewModel.state.value.sets)
        }

        @Test
        fun `OnRepChange updates reps to custom value`() = runTest {
            viewModel.onEvent(ExerciseWorkoutEvent.OnRepChange(12))
            testDispatcher.scheduler.runCurrent()

            Assertions.assertEquals(12, viewModel.state.value.reps)
        }
    }

    @Nested
    @DisplayName("Finish & Calorie Calculations")
    inner class FinishWorkout {

        @Test
        fun `finish cardio exercise calculates calories using time`() = runTest {
            viewModel.onEvent(ExerciseWorkoutEvent.InitExercise("1")) // Cardio (warm up, 0.23 kcal/min)
            testDispatcher.scheduler.runCurrent()
            // Simulating 5 minutes (300 seconds)
            repeat(300) {
                viewModel.onEvent(ExerciseWorkoutEvent.OnTimerTick)
            }
            viewModel.onEvent(ExerciseWorkoutEvent.OnFinishClick)
            testDispatcher.scheduler.runCurrent()

            // 5 * 0.23 = 1.15 -> round to 1
            Assertions.assertEquals(1, viewModel.state.value.caloriesBurned)
            Assertions.assertTrue(viewModel.state.value.showCongratsDialog)
            coVerify { addExerciseWorkout(1, 5) }
        }

        @Test
        fun `finish normal workout calculates calories using sets and reps`() = runTest {
            viewModel.onEvent(ExerciseWorkoutEvent.InitExercise("2")) // Normal (sit up, 0.3 kcal/rep)
            testDispatcher.scheduler.runCurrent()
            viewModel.onEvent(ExerciseWorkoutEvent.OnSetChange(3))
            viewModel.onEvent(ExerciseWorkoutEvent.OnRepChange(10))
            viewModel.onEvent(ExerciseWorkoutEvent.OnFinishClick)
            testDispatcher.scheduler.runCurrent()

            // 3 * 10 * 0.3 = 9
            Assertions.assertEquals(9, viewModel.state.value.caloriesBurned)
            Assertions.assertTrue(viewModel.state.value.showCongratsDialog)
            coVerify { addExerciseWorkout(9, 1) }
        }
    }

    @Nested
    @DisplayName("Navigation Effects")
    inner class NavEffects {

        @Test
        fun `OnCancelClick emits NavigateToCaloriesDashboard`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(ExerciseWorkoutEvent.OnCancelClick)
                testDispatcher.scheduler.runCurrent()

                val effect = awaitItem()
                Assertions.assertEquals(ExerciseWorkoutEffect.NavigateToCaloriesDashboard, effect)
            }
        }

        @Test
        fun `OnCongratsDialogConfirm emits NavigateToCaloriesDashboard`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(ExerciseWorkoutEvent.InitExercise("1"))
                testDispatcher.scheduler.runCurrent()
                viewModel.onEvent(ExerciseWorkoutEvent.OnFinishClick)
                viewModel.onEvent(ExerciseWorkoutEvent.OnCongratsDialogConfirm)
                testDispatcher.scheduler.runCurrent()

                val effect = awaitItem()
                Assertions.assertEquals(ExerciseWorkoutEffect.NavigateToCaloriesDashboard, effect)
            }
        }

        @Test
        fun `OnBackClick emits NavigateBack`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(ExerciseWorkoutEvent.OnBackClick)
                testDispatcher.scheduler.runCurrent()

                val effect = awaitItem()
                Assertions.assertEquals(ExerciseWorkoutEffect.NavigateBack, effect)
            }
        }
    }

    class FakeExercisesRepository : IExercisesRepository {
        var shouldFail = false
        private val mockExercises = listOf(
            Exercise(
                id = "1",
                name = "Push Up",
                category = "cardio",
                bodyPart = "chest",
                equipment = "body only",
                target = "pectorals",
                secondaryMuscles = emptyList(),
                instructions = "Lie face down...",
                instructionSteps = emptyList(),
                imageUrl = null,
                gifUrl = null,
                repKcal = null,
                minKcal = 0.23
            ),
            Exercise(
                id = "2",
                name = "Sit Up",
                category = "strength",
                bodyPart = "waist",
                equipment = "body only",
                target = "abs",
                secondaryMuscles = emptyList(),
                instructions = "Lie on back...",
                instructionSteps = emptyList(),
                imageUrl = null,
                gifUrl = null,
                repKcal = 0.30,
                minKcal = null
            )
        )

        override suspend fun getExercises(query: ExerciseQuery): Result<ExercisePage> {
            return if (shouldFail) Result.failure(Exception()) else Result.success(
                ExercisePage(mockExercises, 1, 1, false)
            )
        }

        override suspend fun getExerciseById(id: String): Result<Exercise> {
            return if (shouldFail) Result.failure(Exception()) else Result.success(
                mockExercises.first { it.id == id }
            )
        }

        override suspend fun getCategories(): Result<List<String>> {
            return if (shouldFail) Result.failure(Exception()) else Result.success(
                listOf("cardio", "strength")
            )
        }
    }
}
