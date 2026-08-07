package iti.grad.nutriscan.presentation.exercises.viewmodel

import android.content.Context
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import iti.grad.nutriscan.domain.exercises.model.Exercise
import iti.grad.nutriscan.domain.exercises.model.ExercisePage
import iti.grad.nutriscan.domain.exercises.model.ExerciseQuery
import iti.grad.nutriscan.domain.exercises.repository.IExercisesRepository
import iti.grad.nutriscan.domain.exercises.usecase.GetExerciseCategoriesUseCase
import iti.grad.nutriscan.domain.exercises.usecase.GetExercisesUseCase
import iti.grad.nutriscan.presentation.exercises.state.ExercisesEffect
import iti.grad.nutriscan.presentation.exercises.state.ExercisesEvent
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
class ExercisesViewModelTest {

    private lateinit var context: Context
    private lateinit var repository: FakeExercisesRepository
    private lateinit var getExercisesUseCase: GetExercisesUseCase
    private lateinit var getCategoriesUseCase: GetExerciseCategoriesUseCase
    private lateinit var viewModel: ExercisesViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        every { context.getString(R.string.exercises_category_all) } returns "All"
        every { context.getString(R.string.exercises_load_error) } returns "Error"
        
        repository = FakeExercisesRepository()
        getExercisesUseCase = GetExercisesUseCase(repository)
        getCategoriesUseCase = GetExerciseCategoriesUseCase(repository)
    }

    private fun initViewModel() {
        viewModel = ExercisesViewModel(context, getExercisesUseCase, getCategoriesUseCase)
        testDispatcher.scheduler.runCurrent()
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Initial State")
    inner class InitialState {

        @Test
        fun `initial state loads all categories and exercises`() {
            initViewModel()
            val state = viewModel.state.value

            Assertions.assertFalse(state.isLoading)
            Assertions.assertEquals(4, state.categories.size) // "All", "Chest", "Back", "Legs"
            Assertions.assertEquals("All", state.categories[0].label)
            Assertions.assertEquals("Chest", state.categories[1].label)
            Assertions.assertEquals(1, state.exercises.size)
            Assertions.assertEquals("all", state.selectedCategoryId)
        }
    }

    @Nested
    @DisplayName("Category selection")
    inner class CategorySelection {

        @Test
        fun `selecting category updates selected category id`() = runTest {
            initViewModel()
            viewModel.onEvent(ExercisesEvent.OnCategorySelected("chest"))
            testDispatcher.scheduler.runCurrent()

            Assertions.assertEquals("chest", viewModel.state.value.selectedCategoryId)
        }
    }

    @Nested
    @DisplayName("Search & Filtering")
    inner class SearchAndFiltering {

        @Test
        fun `changing search query updates search query state and triggers debounced search`() = runTest {
            initViewModel()
            viewModel.onEvent(ExercisesEvent.OnSearchQueryChange("Push"))
            testDispatcher.scheduler.runCurrent()

            Assertions.assertEquals("Push", viewModel.state.value.searchQuery)
            
            // Advance time to pass the 300ms debounce delay
            testDispatcher.scheduler.advanceTimeBy(350)
            testDispatcher.scheduler.runCurrent()
            
            Assertions.assertEquals(1, viewModel.state.value.exercises.size)
        }
    }

    @Nested
    @DisplayName("Exercise Bottom Sheet")
    inner class ExerciseBottomSheet {

        @Test
        fun `clicking exercise displays bottom sheet instructions`() = runTest {
            initViewModel()
            viewModel.onEvent(ExercisesEvent.OnExerciseClick("1"))
            testDispatcher.scheduler.runCurrent()

            val state = viewModel.state.value
            Assertions.assertNotNull(state.selectedExercise)
            Assertions.assertEquals("1", state.selectedExercise?.id)
            Assertions.assertFalse(state.isInstructionsExpanded)
        }

        @Test
        fun `clicking read more expands bottom sheet instructions`() = runTest {
            initViewModel()
            viewModel.onEvent(ExercisesEvent.OnExerciseClick("1"))
            viewModel.onEvent(ExercisesEvent.OnReadMoreClick)
            testDispatcher.scheduler.runCurrent()

            val state = viewModel.state.value
            Assertions.assertTrue(state.isInstructionsExpanded)
        }

        @Test
        fun `dismissing instructions resets selected exercise to null`() = runTest {
            initViewModel()
            viewModel.onEvent(ExercisesEvent.OnExerciseClick("1"))
            viewModel.onEvent(ExercisesEvent.OnDismissInstructions)
            testDispatcher.scheduler.runCurrent()

            val state = viewModel.state.value
            Assertions.assertNull(state.selectedExercise)
        }
    }

    @Nested
    @DisplayName("Navigation")
    inner class Navigation {

        @Test
        fun `clicking back emits NavigateBack effect`() = runTest {
            initViewModel()
            viewModel.effect.test {
                viewModel.onEvent(ExercisesEvent.OnBackClick)
                testDispatcher.scheduler.runCurrent()

                val effect = awaitItem()
                Assertions.assertEquals(ExercisesEffect.NavigateBack, effect)
            }
        }

        @Test
        fun `clicking start workout inside bottom sheet emits NavigateToExerciseWorkout`() = runTest {
            initViewModel()
            viewModel.effect.test {
                viewModel.onEvent(ExercisesEvent.OnExerciseClick("1"))
                viewModel.onEvent(ExercisesEvent.OnStartWorkoutClick)
                testDispatcher.scheduler.runCurrent()

                val effect = awaitItem()
                Assertions.assertTrue(effect is ExercisesEffect.NavigateToExerciseWorkout)
                Assertions.assertEquals("1", (effect as ExercisesEffect.NavigateToExerciseWorkout).exerciseId)
            }
        }
    }

    @Nested
    @DisplayName("Error & Retry")
    inner class ErrorAndRetry {

        @Test
        fun `exercises load failure sets error state and retrying reloads`() = runTest {
            repository.exercisesResult = Result.failure(Exception("Network error"))
            initViewModel()

            Assertions.assertEquals(R.string.exercises_load_error, viewModel.state.value.errorMessageRes)

            repository.exercisesResult = Result.success(
                ExercisePage(
                    exercises = listOf(
                        Exercise(
                            id = "1",
                            name = "Push Up",
                            category = "strength",
                            bodyPart = "chest",
                            equipment = "body only",
                            target = "pectorals",
                            secondaryMuscles = emptyList(),
                            instructions = mapOf("en" to "Lie face down..."),
                            instructionSteps = emptyMap(),
                            imageUrl = null,
                            gifUrl = null,
                            repKcal = 0.20,
                            minKcal = null
                        )
                    ),
                    currentPage = 1,
                    totalPages = 1,
                    hasNext = false
                )
            )

            viewModel.onEvent(ExercisesEvent.OnRetryClick)
            testDispatcher.scheduler.runCurrent()

            Assertions.assertNull(viewModel.state.value.errorMessageRes)
            Assertions.assertEquals(1, viewModel.state.value.exercises.size)
        }
    }

    class FakeExercisesRepository : IExercisesRepository {
        var exercisesResult: Result<ExercisePage> = Result.success(
            ExercisePage(
                exercises = listOf(
                    Exercise(
                        id = "1",
                        name = "Push Up",
                        category = "strength",
                        bodyPart = "chest",
                        equipment = "body only",
                        target = "pectorals",
                        secondaryMuscles = emptyList(),
                        instructions = mapOf("en" to "Lie face down..."),
                        instructionSteps = emptyMap(),
                        imageUrl = null,
                        gifUrl = null,
                        repKcal = 0.20,
                        minKcal = null
                    )
                ),
                currentPage = 1,
                totalPages = 1,
                hasNext = false
            )
        )

        var categoriesResult: Result<List<String>> = Result.success(
            listOf("chest", "back", "legs")
        )

        override suspend fun getExercises(query: ExerciseQuery): Result<ExercisePage> {
            return exercisesResult
        }

        override suspend fun getExerciseById(id: String): Result<Exercise> {
            return exercisesResult.map { page -> page.exercises.first { it.id == id } }
        }

        override suspend fun getCategories(): Result<List<String>> {
            return categoriesResult
        }
    }
}
