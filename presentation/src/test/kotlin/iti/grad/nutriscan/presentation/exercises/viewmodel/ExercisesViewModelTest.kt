package iti.grad.nutriscan.presentation.exercises.viewmodel

import android.content.Context
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import iti.grad.nutriscan.presentation.exercises.state.ExercisesEffect
import iti.grad.nutriscan.presentation.exercises.state.ExercisesEvent
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
    private lateinit var viewModel: ExercisesViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        every { context.getString(any()) } returns "mock_string"
        viewModel = ExercisesViewModel(context)
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
            val state = viewModel.state.value

            Assertions.assertFalse(state.isLoading)
            Assertions.assertEquals(7, state.categories.size)
            Assertions.assertEquals(4, state.exercises.size)
            Assertions.assertEquals("all", state.selectedCategoryId)
        }
    }

    @Nested
    @DisplayName("Category selection")
    inner class CategorySelection {

        @Test
        fun `selecting category updates selected category id`() = runTest {
            viewModel.onEvent(ExercisesEvent.OnCategorySelected("warm_up"))
            testScheduler.runCurrent()

            Assertions.assertEquals("warm_up", viewModel.state.value.selectedCategoryId)
        }
    }

    @Nested
    @DisplayName("Search & Filtering")
    inner class SearchAndFiltering {

        @Test
        fun `changing search query updates search query state`() = runTest {
            viewModel.onEvent(ExercisesEvent.OnSearchQueryChange("Warm Up"))
            testScheduler.runCurrent()

            Assertions.assertEquals("Warm Up", viewModel.state.value.searchQuery)
        }
    }

    @Nested
    @DisplayName("Exercise Bottom Sheet")
    inner class ExerciseBottomSheet {

        @Test
        fun `clicking exercise displays bottom sheet instructions`() = runTest {
            viewModel.onEvent(ExercisesEvent.OnExerciseClick("1"))
            testScheduler.runCurrent()

            val state = viewModel.state.value
            Assertions.assertNotNull(state.selectedExercise)
            Assertions.assertEquals("1", state.selectedExercise?.id)
            Assertions.assertFalse(state.isInstructionsExpanded)
        }

        @Test
        fun `clicking read more expands bottom sheet instructions`() = runTest {
            viewModel.onEvent(ExercisesEvent.OnExerciseClick("1"))
            viewModel.onEvent(ExercisesEvent.OnReadMoreClick)
            testScheduler.runCurrent()

            val state = viewModel.state.value
            Assertions.assertTrue(state.isInstructionsExpanded)
        }

        @Test
        fun `dismissing instructions resets selected exercise to null`() = runTest {
            viewModel.onEvent(ExercisesEvent.OnExerciseClick("1"))
            viewModel.onEvent(ExercisesEvent.OnDismissInstructions)
            testScheduler.runCurrent()

            val state = viewModel.state.value
            Assertions.assertNull(state.selectedExercise)
        }
    }

    @Nested
    @DisplayName("Navigation")
    inner class Navigation {

        @Test
        fun `clicking back emits NavigateBack effect`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(ExercisesEvent.OnBackClick)
                testScheduler.runCurrent()

                val effect = awaitItem()
                Assertions.assertEquals(ExercisesEffect.NavigateBack, effect)
            }
        }

        @Test
        fun `clicking start workout inside bottom sheet emits NavigateToExerciseWorkout`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(ExercisesEvent.OnExerciseClick("1"))
                viewModel.onEvent(ExercisesEvent.OnStartWorkoutClick)
                testScheduler.runCurrent()

                val effect = awaitItem()
                Assertions.assertTrue(effect is ExercisesEffect.NavigateToExerciseWorkout)
                Assertions.assertEquals("1", (effect as ExercisesEffect.NavigateToExerciseWorkout).exerciseId)
            }
        }
    }
}
