package iti.grad.nutriscan.presentation.auth.profile_setup

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.domain.onboarding.usecase.CompleteOnboardingUseCase
import iti.grad.nutriscan.presentation.auth.profile_setup.state.HealthProfileSetupEffect
import iti.grad.nutriscan.presentation.auth.profile_setup.state.HealthProfileSetupEvent
import iti.grad.nutriscan.presentation.auth.profile_setup.viewmodel.HealthProfileSetupViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HealthProfileSetupViewModelTest {

    private lateinit var viewModel: HealthProfileSetupViewModel
    private val completeOnboardingUseCase: CompleteOnboardingUseCase = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = HealthProfileSetupViewModel(completeOnboardingUseCase)
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has default list of conditions and allergies and nothing selected`() {
        val state = viewModel.state.value
        Assertions.assertFalse(state.isLoading)
        Assertions.assertTrue(state.selectedChronicConditions.isEmpty())
        Assertions.assertTrue(state.selectedAllergies.isEmpty())
        Assertions.assertTrue(state.chronicConditions.contains("Diabetes"))
        Assertions.assertTrue(state.chronicConditions.contains("Hypertension"))
        Assertions.assertTrue(state.chronicConditions.contains("Celiac Disease"))
        Assertions.assertTrue(state.allergies.contains("Peanuts"))
        Assertions.assertTrue(state.allergies.contains("Gluten"))
        Assertions.assertTrue(state.allergies.contains("Dairy"))
    }

    @Test
    fun `when ToggleCondition is triggered, condition is added or removed`() {
        // Toggle once to add
        viewModel.onEvent(HealthProfileSetupEvent.ToggleCondition("Diabetes"))
        Assertions.assertTrue(viewModel.state.value.selectedChronicConditions.contains("Diabetes"))

        // Toggle again to remove
        viewModel.onEvent(HealthProfileSetupEvent.ToggleCondition("Diabetes"))
        Assertions.assertFalse(viewModel.state.value.selectedChronicConditions.contains("Diabetes"))
    }

    @Test
    fun `when ToggleAllergy is triggered, allergy is added or removed`() {
        // Toggle once to add
        viewModel.onEvent(HealthProfileSetupEvent.ToggleAllergy("Peanuts"))
        Assertions.assertTrue(viewModel.state.value.selectedAllergies.contains("Peanuts"))

        // Toggle again to remove
        viewModel.onEvent(HealthProfileSetupEvent.ToggleAllergy("Peanuts"))
        Assertions.assertFalse(viewModel.state.value.selectedAllergies.contains("Peanuts"))
    }

    @Test
    fun `when StartAddCustomCondition is triggered, state updates accordingly`() {
        viewModel.onEvent(HealthProfileSetupEvent.StartAddCustomCondition)
        val state = viewModel.state.value
        Assertions.assertTrue(state.isAddingCustomCondition)
        Assertions.assertEquals("", state.customConditionInput)
    }

    @Test
    fun `when UpdateCustomConditionInput is triggered, state customConditionInput is updated`() {
        viewModel.onEvent(HealthProfileSetupEvent.UpdateCustomConditionInput("Asthma"))
        Assertions.assertEquals("Asthma", viewModel.state.value.customConditionInput)
    }

    @Test
    fun `when SubmitCustomCondition is triggered with non-empty input, custom condition is added and selected`() {
        viewModel.onEvent(HealthProfileSetupEvent.StartAddCustomCondition)
        viewModel.onEvent(HealthProfileSetupEvent.UpdateCustomConditionInput("Asthma"))
        viewModel.onEvent(HealthProfileSetupEvent.SubmitCustomCondition)

        val state = viewModel.state.value
        Assertions.assertFalse(state.isAddingCustomCondition)
        Assertions.assertEquals("", state.customConditionInput)
        Assertions.assertTrue(state.chronicConditions.contains("Asthma"))
        Assertions.assertTrue(state.selectedChronicConditions.contains("Asthma"))
    }

    @Test
    fun `when SubmitCustomCondition is triggered with empty input, state is reset`() {
        viewModel.onEvent(HealthProfileSetupEvent.StartAddCustomCondition)
        viewModel.onEvent(HealthProfileSetupEvent.UpdateCustomConditionInput("   "))
        viewModel.onEvent(HealthProfileSetupEvent.SubmitCustomCondition)

        val state = viewModel.state.value
        Assertions.assertFalse(state.isAddingCustomCondition)
        Assertions.assertEquals("", state.customConditionInput)
        Assertions.assertFalse(state.chronicConditions.contains("   "))
    }

    @Test
    fun `when CancelAddCustomCondition is triggered, state is reset`() {
        viewModel.onEvent(HealthProfileSetupEvent.StartAddCustomCondition)
        viewModel.onEvent(HealthProfileSetupEvent.UpdateCustomConditionInput("Asthma"))
        viewModel.onEvent(HealthProfileSetupEvent.CancelAddCustomCondition)

        val state = viewModel.state.value
        Assertions.assertFalse(state.isAddingCustomCondition)
        Assertions.assertEquals("", state.customConditionInput)
        Assertions.assertFalse(state.chronicConditions.contains("Asthma"))
    }

    @Test
    fun `when StartAddCustomAllergy is triggered, state updates accordingly`() {
        viewModel.onEvent(HealthProfileSetupEvent.StartAddCustomAllergy)
        val state = viewModel.state.value
        Assertions.assertTrue(state.isAddingCustomAllergy)
        Assertions.assertEquals("", state.customAllergyInput)
    }

    @Test
    fun `when UpdateCustomAllergyInput is triggered, state customAllergyInput is updated`() {
        viewModel.onEvent(HealthProfileSetupEvent.UpdateCustomAllergyInput("Soy"))
        Assertions.assertEquals("Soy", viewModel.state.value.customAllergyInput)
    }

    @Test
    fun `when SubmitCustomAllergy is triggered with non-empty input, custom allergy is added and selected`() {
        viewModel.onEvent(HealthProfileSetupEvent.StartAddCustomAllergy)
        viewModel.onEvent(HealthProfileSetupEvent.UpdateCustomAllergyInput("Soy"))
        viewModel.onEvent(HealthProfileSetupEvent.SubmitCustomAllergy)

        val state = viewModel.state.value
        Assertions.assertFalse(state.isAddingCustomAllergy)
        Assertions.assertEquals("", state.customAllergyInput)
        Assertions.assertTrue(state.allergies.contains("Soy"))
        Assertions.assertTrue(state.selectedAllergies.contains("Soy"))
    }

    @Test
    fun `when SubmitCustomAllergy is triggered with empty input, state is reset`() {
        viewModel.onEvent(HealthProfileSetupEvent.StartAddCustomAllergy)
        viewModel.onEvent(HealthProfileSetupEvent.UpdateCustomAllergyInput("   "))
        viewModel.onEvent(HealthProfileSetupEvent.SubmitCustomAllergy)

        val state = viewModel.state.value
        Assertions.assertFalse(state.isAddingCustomAllergy)
        Assertions.assertEquals("", state.customAllergyInput)
        Assertions.assertFalse(state.allergies.contains("   "))
    }

    @Test
    fun `when CancelAddCustomAllergy is triggered, state is reset`() {
        viewModel.onEvent(HealthProfileSetupEvent.StartAddCustomAllergy)
        viewModel.onEvent(HealthProfileSetupEvent.UpdateCustomAllergyInput("Soy"))
        viewModel.onEvent(HealthProfileSetupEvent.CancelAddCustomAllergy)

        val state = viewModel.state.value
        Assertions.assertFalse(state.isAddingCustomAllergy)
        Assertions.assertEquals("", state.customAllergyInput)
        Assertions.assertFalse(state.allergies.contains("Soy"))
    }

    @Test
    fun `when SaveProfile succeeds, completeOnboardingUseCase is called and effect is NavigateToHome`() = runTest {
        coEvery { completeOnboardingUseCase.invoke() } returns Unit

        viewModel.effect.test {
            viewModel.onEvent(HealthProfileSetupEvent.SaveProfile)
            
            // Advance coroutines execution to trigger loading and usecase
            testScheduler.advanceUntilIdle()

            val effect = awaitItem()
            Assertions.assertTrue(effect is HealthProfileSetupEffect.NavigateToHome)
            Assertions.assertFalse(viewModel.state.value.isLoading)
            coVerify(exactly = 1) { completeOnboardingUseCase.invoke() }
        }
    }

    @Test
    fun `when SaveProfile fails, completeOnboardingUseCase is called and effect is ShowSnackbar`() = runTest {
        val errorMessage = "Network Error"
        coEvery { completeOnboardingUseCase.invoke() } throws Exception(errorMessage)

        viewModel.effect.test {
            viewModel.onEvent(HealthProfileSetupEvent.SaveProfile)

            // Advance coroutines execution to trigger loading and usecase
            testScheduler.advanceUntilIdle()

            val effect = awaitItem()
            Assertions.assertTrue(effect is HealthProfileSetupEffect.ShowSnackbar)
            Assertions.assertEquals(errorMessage, (effect as HealthProfileSetupEffect.ShowSnackbar).messageStr)
            Assertions.assertFalse(viewModel.state.value.isLoading)
            coVerify(exactly = 1) { completeOnboardingUseCase.invoke() }
        }
    }
}
