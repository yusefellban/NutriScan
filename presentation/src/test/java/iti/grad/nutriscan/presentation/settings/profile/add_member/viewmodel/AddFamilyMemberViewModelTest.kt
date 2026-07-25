package iti.grad.nutriscan.presentation.settings.profile.add_member.viewmodel

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.domain.allergy.model.Allergy
import iti.grad.nutriscan.domain.allergy.usecase.GetAllergiesUseCase
import iti.grad.nutriscan.domain.allergy.usecase.SyncAllergiesUseCase
import iti.grad.nutriscan.domain.disease.model.Disease
import iti.grad.nutriscan.domain.disease.usecase.GetDiseasesUseCase
import iti.grad.nutriscan.domain.disease.usecase.SyncDiseasesUseCase
import iti.grad.nutriscan.domain.family.usecase.AddFamilyMemberUseCase
import iti.grad.nutriscan.presentation.settings.profile.add_member.state.AddFamilyMemberEffect
import iti.grad.nutriscan.presentation.settings.profile.add_member.state.AddFamilyMemberEvent
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddFamilyMemberViewModelTest {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    private val getDiseasesUseCase: GetDiseasesUseCase = mockk()
    private val getAllergiesUseCase: GetAllergiesUseCase = mockk()
    private val syncDiseasesUseCase: SyncDiseasesUseCase = mockk()
    private val syncAllergiesUseCase: SyncAllergiesUseCase = mockk()
    private val addFamilyMemberUseCase: AddFamilyMemberUseCase = mockk()

    private val diseasesFlow = MutableStateFlow<List<Disease>>(emptyList())
    private val allergiesFlow = MutableStateFlow<List<Allergy>>(emptyList())

    private lateinit var viewModel: AddFamilyMemberViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        coEvery { syncDiseasesUseCase() } returns Result.success(Unit)
        coEvery { getDiseasesUseCase() } returns diseasesFlow
        coEvery { syncAllergiesUseCase() } returns Result.success(Unit)
        coEvery { getAllergiesUseCase() } returns allergiesFlow

        viewModel = AddFamilyMemberViewModel(
            getDiseasesUseCase = getDiseasesUseCase,
            getAllergiesUseCase = getAllergiesUseCase,
            syncDiseasesUseCase = syncDiseasesUseCase,
            syncAllergiesUseCase = syncAllergiesUseCase,
            addFamilyMemberUseCase = addFamilyMemberUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads diseases and allergies from use cases`() = runTest(testDispatcher) {
        val mockDiseases = listOf(Disease(1, "Diabetes", "Diabetes desc"))
        val mockAllergies = listOf(Allergy(2, "Peanut", "Peanut desc"))

        diseasesFlow.value = mockDiseases
        allergiesFlow.value = mockAllergies
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(mockDiseases.toImmutableList(), state.diseases)
        assertEquals(mockAllergies.toImmutableList(), state.allergies)
        assertFalse(state.isDiseasesLoading)
        assertFalse(state.isAllergiesLoading)
        assertNull(state.diseasesErrorMessage)
        assertNull(state.allergiesErrorMessage)
    }

    @Test
    fun `when NameChanged event, state name is updated and nameError is cleared`() = runTest(testDispatcher) {
        // Force a name error first
        viewModel.onEvent(AddFamilyMemberEvent.SaveClicked)
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.nameError != null)

        viewModel.onEvent(AddFamilyMemberEvent.NameChanged("Ahmed"))
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Ahmed", state.name)
        assertNull(state.nameError)
    }

    @Test
    fun `when SaveClicked with blank name, nameError is set and use case is not invoked`() = runTest(testDispatcher) {
        viewModel.onEvent(AddFamilyMemberEvent.NameChanged("   "))
        testScheduler.advanceUntilIdle()

        viewModel.onEvent(AddFamilyMemberEvent.SaveClicked)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.nameError != null)
        coVerify(exactly = 0) { addFamilyMemberUseCase(any(), any(), any()) }
    }

    @Test
    fun `when SaveClicked succeeds, Dismiss effect is emitted`() = runTest(testDispatcher) {
        viewModel.onEvent(AddFamilyMemberEvent.NameChanged("Ahmed"))
        coEvery { addFamilyMemberUseCase("Ahmed", any(), any()) } returns Result.success(Unit)

        viewModel.effect.test {
            viewModel.onEvent(AddFamilyMemberEvent.SaveClicked)
            testScheduler.advanceUntilIdle()
            assertEquals(AddFamilyMemberEffect.Dismiss, awaitItem())
        }
    }

    @Test
    fun `when SaveClicked fails, ShowError effect is emitted with the failure message`() = runTest(testDispatcher) {
        viewModel.onEvent(AddFamilyMemberEvent.NameChanged("Ahmed"))
        coEvery { addFamilyMemberUseCase("Ahmed", any(), any()) } returns Result.failure(Exception("Sync failed"))

        viewModel.effect.test {
            viewModel.onEvent(AddFamilyMemberEvent.SaveClicked)
            testScheduler.advanceUntilIdle()
            assertEquals(AddFamilyMemberEffect.ShowError("Sync failed"), awaitItem())
        }
    }

    @Test
    fun `when ToggleDisease called twice, selectedDiseaseIds returns to original`() = runTest(testDispatcher) {
        viewModel.onEvent(AddFamilyMemberEvent.ToggleDisease(10))
        testScheduler.advanceUntilIdle()
        assertEquals(listOf(10), viewModel.state.value.selectedDiseaseIds)

        viewModel.onEvent(AddFamilyMemberEvent.ToggleDisease(10))
        testScheduler.advanceUntilIdle()
        assertEquals(emptyList<Int>(), viewModel.state.value.selectedDiseaseIds)
    }

    @Test
    fun `when ToggleAllergy called, selectedAllergyIds is updated`() = runTest(testDispatcher) {
        viewModel.onEvent(AddFamilyMemberEvent.ToggleAllergy(5))
        testScheduler.advanceUntilIdle()
        assertEquals(listOf(5), viewModel.state.value.selectedAllergyIds)
    }

    @Test
    fun `when diseases sync fails, diseasesErrorMessage is set`() = runTest(testDispatcher) {
        coEvery { syncDiseasesUseCase() } returns Result.failure(Exception("Network error"))
        viewModel.onEvent(AddFamilyMemberEvent.RetryLoadDiseases)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Network error", state.diseasesErrorMessage)
        assertFalse(state.isDiseasesLoading)
    }

    @Test
    fun `when allergies sync fails, allergiesErrorMessage is set`() = runTest(testDispatcher) {
        coEvery { syncAllergiesUseCase() } returns Result.failure(Exception("Timeout error"))
        viewModel.onEvent(AddFamilyMemberEvent.RetryLoadAllergies)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Timeout error", state.allergiesErrorMessage)
        assertFalse(state.isAllergiesLoading)
    }
}
