package iti.grad.nutriscan.presentation.profile_setup

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.domain.onboarding.usecase.CompleteOnboardingUseCase
import iti.grad.nutriscan.presentation.profile_setup.state.ProfileSetupPagerEffect
import iti.grad.nutriscan.presentation.profile_setup.state.ProfileSetupPagerEvent
import iti.grad.nutriscan.presentation.profile_setup.viewmodel.ProfileSetupPagerViewModel
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
class ProfileSetupPagerViewModelTest {

    private lateinit var viewModel: ProfileSetupPagerViewModel
    private val completeOnboardingUseCase: CompleteOnboardingUseCase = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ProfileSetupPagerViewModel(completeOnboardingUseCase)
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    // ── Pager Navigation ────────────────────────────────────────────────

    @Nested
    @DisplayName("Pager Navigation")
    inner class PagerNavigation {

        @Test
        fun `initial state starts at page 0 with 5 pages`() {
            val state = viewModel.state.value
            Assertions.assertEquals(0, state.currentPage)
            Assertions.assertEquals(5, state.pageCount)
        }

        @Test
        fun `NextClicked emits ScrollToPage with next page`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(ProfileSetupPagerEvent.NextClicked)
                testScheduler.advanceUntilIdle()

                val effect = awaitItem()
                Assertions.assertTrue(effect is ProfileSetupPagerEffect.ScrollToPage)
                Assertions.assertEquals(1, (effect as ProfileSetupPagerEffect.ScrollToPage).page)
            }
        }

        @Test
        fun `NextClicked on last page does not emit ScrollToPage`() = runTest {
            // Move to last page
            viewModel.onEvent(ProfileSetupPagerEvent.PageChanged(4))

            viewModel.effect.test {
                viewModel.onEvent(ProfileSetupPagerEvent.NextClicked)
                testScheduler.advanceUntilIdle()

                expectNoEvents()
            }
        }

        @Test
        fun `BackClicked on page 0 emits NavigateBack`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(ProfileSetupPagerEvent.BackClicked)
                testScheduler.advanceUntilIdle()

                val effect = awaitItem()
                Assertions.assertTrue(effect is ProfileSetupPagerEffect.NavigateBack)
            }
        }

        @Test
        fun `BackClicked on page greater than 0 emits ScrollToPage with previous page`() = runTest {
            viewModel.onEvent(ProfileSetupPagerEvent.PageChanged(2))

            viewModel.effect.test {
                viewModel.onEvent(ProfileSetupPagerEvent.BackClicked)
                testScheduler.advanceUntilIdle()

                val effect = awaitItem()
                Assertions.assertTrue(effect is ProfileSetupPagerEffect.ScrollToPage)
                Assertions.assertEquals(1, (effect as ProfileSetupPagerEffect.ScrollToPage).page)
            }
        }

        @Test
        fun `PageChanged updates currentPage in state`() {
            viewModel.onEvent(ProfileSetupPagerEvent.PageChanged(3))
            Assertions.assertEquals(3, viewModel.state.value.currentPage)
        }
    }

    // ── Health Profile: Conditions ───────────────────────────────────────

    @Nested
    @DisplayName("Health Profile — Conditions")
    inner class HealthProfileConditions {

        @Test
        fun `initial state has default list of conditions and nothing selected`() {
            val state = viewModel.state.value
            Assertions.assertTrue(state.selectedChronicConditions.isEmpty())
            Assertions.assertTrue(state.chronicConditions.contains("Diabetes"))
            Assertions.assertTrue(state.chronicConditions.contains("Hypertension"))
            Assertions.assertTrue(state.chronicConditions.contains("Celiac Disease"))
        }

        @Test
        fun `ToggleCondition adds then removes condition`() {
            viewModel.onEvent(ProfileSetupPagerEvent.ToggleCondition("Diabetes"))
            Assertions.assertTrue(viewModel.state.value.selectedChronicConditions.contains("Diabetes"))

            viewModel.onEvent(ProfileSetupPagerEvent.ToggleCondition("Diabetes"))
            Assertions.assertFalse(viewModel.state.value.selectedChronicConditions.contains("Diabetes"))
        }

        @Test
        fun `StartAddCustomCondition sets isAddingCustomCondition to true`() {
            viewModel.onEvent(ProfileSetupPagerEvent.StartAddCustomCondition)
            val state = viewModel.state.value
            Assertions.assertTrue(state.isAddingCustomCondition)
            Assertions.assertEquals("", state.customConditionInput)
        }

        @Test
        fun `UpdateCustomConditionInput updates state`() {
            viewModel.onEvent(ProfileSetupPagerEvent.UpdateCustomConditionInput("Asthma"))
            Assertions.assertEquals("Asthma", viewModel.state.value.customConditionInput)
        }

        @Test
        fun `SubmitCustomCondition with non-empty input adds and selects condition`() {
            viewModel.onEvent(ProfileSetupPagerEvent.StartAddCustomCondition)
            viewModel.onEvent(ProfileSetupPagerEvent.UpdateCustomConditionInput("Asthma"))
            viewModel.onEvent(ProfileSetupPagerEvent.SubmitCustomCondition)

            val state = viewModel.state.value
            Assertions.assertFalse(state.isAddingCustomCondition)
            Assertions.assertEquals("", state.customConditionInput)
            Assertions.assertTrue(state.chronicConditions.contains("Asthma"))
            Assertions.assertTrue(state.selectedChronicConditions.contains("Asthma"))
        }

        @Test
        fun `SubmitCustomCondition with blank input resets state`() {
            viewModel.onEvent(ProfileSetupPagerEvent.StartAddCustomCondition)
            viewModel.onEvent(ProfileSetupPagerEvent.UpdateCustomConditionInput("   "))
            viewModel.onEvent(ProfileSetupPagerEvent.SubmitCustomCondition)

            val state = viewModel.state.value
            Assertions.assertFalse(state.isAddingCustomCondition)
            Assertions.assertEquals("", state.customConditionInput)
            Assertions.assertFalse(state.chronicConditions.contains("   "))
        }

        @Test
        fun `CancelAddCustomCondition resets state without adding`() {
            viewModel.onEvent(ProfileSetupPagerEvent.StartAddCustomCondition)
            viewModel.onEvent(ProfileSetupPagerEvent.UpdateCustomConditionInput("Asthma"))
            viewModel.onEvent(ProfileSetupPagerEvent.CancelAddCustomCondition)

            val state = viewModel.state.value
            Assertions.assertFalse(state.isAddingCustomCondition)
            Assertions.assertEquals("", state.customConditionInput)
            Assertions.assertFalse(state.chronicConditions.contains("Asthma"))
        }
    }

    // ── Health Profile: Allergies ────────────────────────────────────────

    @Nested
    @DisplayName("Health Profile — Allergies")
    inner class HealthProfileAllergies {

        @Test
        fun `initial state has default list of allergies and nothing selected`() {
            val state = viewModel.state.value
            Assertions.assertTrue(state.selectedAllergies.isEmpty())
            Assertions.assertTrue(state.allergies.contains("Peanuts"))
            Assertions.assertTrue(state.allergies.contains("Gluten"))
            Assertions.assertTrue(state.allergies.contains("Dairy"))
        }

        @Test
        fun `ToggleAllergy adds then removes allergy`() {
            viewModel.onEvent(ProfileSetupPagerEvent.ToggleAllergy("Peanuts"))
            Assertions.assertTrue(viewModel.state.value.selectedAllergies.contains("Peanuts"))

            viewModel.onEvent(ProfileSetupPagerEvent.ToggleAllergy("Peanuts"))
            Assertions.assertFalse(viewModel.state.value.selectedAllergies.contains("Peanuts"))
        }

        @Test
        fun `StartAddCustomAllergy sets isAddingCustomAllergy to true`() {
            viewModel.onEvent(ProfileSetupPagerEvent.StartAddCustomAllergy)
            val state = viewModel.state.value
            Assertions.assertTrue(state.isAddingCustomAllergy)
            Assertions.assertEquals("", state.customAllergyInput)
        }

        @Test
        fun `UpdateCustomAllergyInput updates state`() {
            viewModel.onEvent(ProfileSetupPagerEvent.UpdateCustomAllergyInput("Soy"))
            Assertions.assertEquals("Soy", viewModel.state.value.customAllergyInput)
        }

        @Test
        fun `SubmitCustomAllergy with non-empty input adds and selects allergy`() {
            viewModel.onEvent(ProfileSetupPagerEvent.StartAddCustomAllergy)
            viewModel.onEvent(ProfileSetupPagerEvent.UpdateCustomAllergyInput("Soy"))
            viewModel.onEvent(ProfileSetupPagerEvent.SubmitCustomAllergy)

            val state = viewModel.state.value
            Assertions.assertFalse(state.isAddingCustomAllergy)
            Assertions.assertEquals("", state.customAllergyInput)
            Assertions.assertTrue(state.allergies.contains("Soy"))
            Assertions.assertTrue(state.selectedAllergies.contains("Soy"))
        }

        @Test
        fun `SubmitCustomAllergy with blank input resets state`() {
            viewModel.onEvent(ProfileSetupPagerEvent.StartAddCustomAllergy)
            viewModel.onEvent(ProfileSetupPagerEvent.UpdateCustomAllergyInput("   "))
            viewModel.onEvent(ProfileSetupPagerEvent.SubmitCustomAllergy)

            val state = viewModel.state.value
            Assertions.assertFalse(state.isAddingCustomAllergy)
            Assertions.assertEquals("", state.customAllergyInput)
            Assertions.assertFalse(state.allergies.contains("   "))
        }

        @Test
        fun `CancelAddCustomAllergy resets state without adding`() {
            viewModel.onEvent(ProfileSetupPagerEvent.StartAddCustomAllergy)
            viewModel.onEvent(ProfileSetupPagerEvent.UpdateCustomAllergyInput("Soy"))
            viewModel.onEvent(ProfileSetupPagerEvent.CancelAddCustomAllergy)

            val state = viewModel.state.value
            Assertions.assertFalse(state.isAddingCustomAllergy)
            Assertions.assertEquals("", state.customAllergyInput)
            Assertions.assertFalse(state.allergies.contains("Soy"))
        }
    }

    // ── Save Profile ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Save Profile")
    inner class SaveProfile {

        @Test
        fun `SaveProfile success calls use case and emits NavigateToHome`() = runTest {
            coEvery { completeOnboardingUseCase.invoke() } returns Unit

            viewModel.effect.test {
                viewModel.onEvent(ProfileSetupPagerEvent.SaveProfile)
                testScheduler.advanceUntilIdle()

                val effect = awaitItem()
                Assertions.assertTrue(effect is ProfileSetupPagerEffect.NavigateToHome)
                Assertions.assertFalse(viewModel.state.value.isLoading)
                coVerify(exactly = 1) { completeOnboardingUseCase.invoke() }
            }
        }

        @Test
        fun `SaveProfile failure calls use case and emits ShowSnackbar`() = runTest {
            val errorMessage = "Network Error"
            coEvery { completeOnboardingUseCase.invoke() } throws Exception(errorMessage)

            viewModel.effect.test {
                viewModel.onEvent(ProfileSetupPagerEvent.SaveProfile)
                testScheduler.advanceUntilIdle()

                val effect = awaitItem()
                Assertions.assertTrue(effect is ProfileSetupPagerEffect.ShowSnackbar)
                Assertions.assertEquals(
                    errorMessage,
                    (effect as ProfileSetupPagerEffect.ShowSnackbar).messageStr
                )
                Assertions.assertFalse(viewModel.state.value.isLoading)
                coVerify(exactly = 1) { completeOnboardingUseCase.invoke() }
            }
        }
    }
}
