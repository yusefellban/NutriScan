package iti.grad.nutriscan.presentation.settings.profile.edit

import app.cash.turbine.test
import iti.grad.nutriscan.presentation.settings.profile.edit.state.EditProfileEffect
import iti.grad.nutriscan.presentation.settings.profile.edit.state.EditProfileEvent
import iti.grad.nutriscan.presentation.settings.profile.edit.viewmodel.EditProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditProfileViewModelTest {

    private lateinit var viewModel: EditProfileViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = EditProfileViewModel()
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state pre-fills correct profile information`() {
        val state = viewModel.state.value
        assertEquals("", state.name)
        assertEquals("", state.username)
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertTrue(state.selectedChronicConditions.contains("Celiac Disease"))
        assertTrue(state.selectedAllergies.isEmpty())
        assertFalse(state.showSaveConfirmation)
        assertFalse(state.isLoading)
    }

    @Test
    fun `UpdateName updates state correctly`() {
        viewModel.onEvent(EditProfileEvent.UpdateName("Ahmed Ali"))
        assertEquals("Ahmed Ali", viewModel.state.value.name)
    }

    @Test
    fun `UpdateUsername updates state correctly`() {
        viewModel.onEvent(EditProfileEvent.UpdateUsername("ahmed_ali"))
        assertEquals("ahmed_ali", viewModel.state.value.username)
    }

    @Test
    fun `UpdateEmail updates state correctly`() {
        viewModel.onEvent(EditProfileEvent.UpdateEmail("ahmed@gmail.com"))
        assertEquals("ahmed@gmail.com", viewModel.state.value.email)
    }

    @Test
    fun `UpdatePassword updates state correctly`() {
        viewModel.onEvent(EditProfileEvent.UpdatePassword("newpass"))
        assertEquals("newpass", viewModel.state.value.password)
    }

    @Test
    fun `ToggleCondition adds and removes conditions`() {
        // Toggle Celiac Disease (which is initially selected) -> should be removed
        viewModel.onEvent(EditProfileEvent.ToggleCondition("Celiac Disease"))
        assertFalse(viewModel.state.value.selectedChronicConditions.contains("Celiac Disease"))

        // Toggle Diabetes (initially unselected) -> should be added
        viewModel.onEvent(EditProfileEvent.ToggleCondition("Diabetes"))
        assertTrue(viewModel.state.value.selectedChronicConditions.contains("Diabetes"))
    }

    @Test
    fun `ToggleAllergy adds and removes allergies`() {
        // Toggle Peanuts (initially unselected) -> should be added
        viewModel.onEvent(EditProfileEvent.ToggleAllergy("Peanuts"))
        assertTrue(viewModel.state.value.selectedAllergies.contains("Peanuts"))

        // Toggle Peanuts again -> should be removed
        viewModel.onEvent(EditProfileEvent.ToggleAllergy("Peanuts"))
        assertFalse(viewModel.state.value.selectedAllergies.contains("Peanuts"))
    }

    @Test
    fun `Add custom condition works correctly`() {
        viewModel.onEvent(EditProfileEvent.StartAddCustomCondition)
        assertTrue(viewModel.state.value.isAddingCustomCondition)

        viewModel.onEvent(EditProfileEvent.UpdateCustomConditionInput("Lactose Intolerance"))
        assertEquals("Lactose Intolerance", viewModel.state.value.customConditionInput)

        viewModel.onEvent(EditProfileEvent.SubmitCustomCondition)
        assertFalse(viewModel.state.value.isAddingCustomCondition)
        assertTrue(viewModel.state.value.chronicConditions.contains("Lactose Intolerance"))
        assertTrue(viewModel.state.value.selectedChronicConditions.contains("Lactose Intolerance"))
    }

    @Test
    fun `Add custom allergy works correctly`() {
        viewModel.onEvent(EditProfileEvent.StartAddCustomAllergy)
        assertTrue(viewModel.state.value.isAddingCustomAllergy)

        viewModel.onEvent(EditProfileEvent.UpdateCustomAllergyInput("Soy"))
        assertEquals("Soy", viewModel.state.value.customAllergyInput)

        viewModel.onEvent(EditProfileEvent.SubmitCustomAllergy)
        assertFalse(viewModel.state.value.isAddingCustomAllergy)
        assertTrue(viewModel.state.value.allergies.contains("Soy"))
        assertTrue(viewModel.state.value.selectedAllergies.contains("Soy"))
    }

    @Test
    fun `BackClicked emits NavigateBack effect`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(EditProfileEvent.BackClicked)
            assertEquals(EditProfileEffect.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `SaveClicked triggers confirmation dialog and confirming emits NavigateBack`() = runTest(testDispatcher) {
        viewModel.onEvent(EditProfileEvent.SaveClicked)
        assertTrue(viewModel.state.value.showSaveConfirmation)

        viewModel.effect.test {
            viewModel.onEvent(EditProfileEvent.ConfirmSave)
            // Confirms save -> sets isLoading and then emits NavigateBack
            assertEquals(EditProfileEffect.NavigateBack, awaitItem())
            assertFalse(viewModel.state.value.showSaveConfirmation)
        }
    }

    @Test
    fun `Dismissing save confirmation dialog clears flag`() {
        viewModel.onEvent(EditProfileEvent.SaveClicked)
        assertTrue(viewModel.state.value.showSaveConfirmation)

        viewModel.onEvent(EditProfileEvent.DismissSaveConfirmation)
        assertFalse(viewModel.state.value.showSaveConfirmation)
    }
}
