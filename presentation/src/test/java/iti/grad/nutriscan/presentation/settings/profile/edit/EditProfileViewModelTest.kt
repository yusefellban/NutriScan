package iti.grad.nutriscan.presentation.settings.profile.edit

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import iti.grad.nutriscan.domain.allergy.model.Allergy
import iti.grad.nutriscan.domain.allergy.usecase.GetAllergiesUseCase
import iti.grad.nutriscan.domain.allergy.usecase.SyncAllergiesUseCase
import iti.grad.nutriscan.domain.disease.model.Disease
import iti.grad.nutriscan.domain.disease.usecase.GetDiseasesUseCase
import iti.grad.nutriscan.domain.disease.usecase.SyncDiseasesUseCase
import iti.grad.nutriscan.domain.user.model.User
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import iti.grad.nutriscan.domain.user.usecase.GetUserProfileUseCase
import iti.grad.nutriscan.domain.user.usecase.UpdateUserProfileUseCase
import iti.grad.nutriscan.domain.user.usecase.UploadAvatarUseCase
import iti.grad.nutriscan.presentation.settings.profile.edit.state.AvatarUploadState
import iti.grad.nutriscan.presentation.settings.profile.edit.state.EditProfileEffect
import iti.grad.nutriscan.presentation.settings.profile.edit.state.EditProfileEvent
import iti.grad.nutriscan.presentation.settings.profile.edit.viewmodel.EditProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

@OptIn(ExperimentalCoroutinesApi::class)
class EditProfileViewModelTest {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    private lateinit var viewModel: EditProfileViewModel

    private val userData = MutableStateFlow<User?>(null)
    private val getUserProfileUseCase: GetUserProfileUseCase = mockk {
        every { this@mockk() } returns userData
    }
    private val updateUserProfileUseCase: UpdateUserProfileUseCase = mockk()
    private val uploadAvatarUseCase: UploadAvatarUseCase = mockk()
    private val getDiseasesUseCase: GetDiseasesUseCase = mockk {
        every { this@mockk() } returns flowOf(emptyList())
    }
    private val getAllergiesUseCase: GetAllergiesUseCase = mockk {
        every { this@mockk() } returns flowOf(emptyList())
    }
    private val syncDiseasesUseCase: SyncDiseasesUseCase = mockk()
    private val syncAllergiesUseCase: SyncAllergiesUseCase = mockk()
    private val userRepository: IUserRepository = mockk()

    // A fake content:// picker Uri whose bytes the ViewModel copies into the app cache dir
    // before handing the resulting File to uploadAvatarUseCase.
    private val pickedUri: Uri = mockk()
    private val contentResolver: ContentResolver = mockk {
        every { openInputStream(pickedUri) } returns ByteArrayInputStream(byteArrayOf(1, 2, 3))
    }
    private val context: Context = mockk(relaxed = true) {
        every { this@mockk.contentResolver } returns contentResolver
        every { cacheDir } returns java.io.File(System.getProperty("java.io.tmpdir"), "edit_profile_test_cache").apply { mkdirs() }
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { syncDiseasesUseCase() } returns Result.success(Unit)
        coEvery { syncAllergiesUseCase() } returns Result.success(Unit)
        coEvery { userRepository.fetchAndSyncProfile() } returns Result.success(Unit)
        viewModel = EditProfileViewModel(
            getUserProfileUseCase,
            updateUserProfileUseCase,
            uploadAvatarUseCase,
            getDiseasesUseCase,
            getAllergiesUseCase,
            syncDiseasesUseCase,
            syncAllergiesUseCase,
            userRepository,
            context,
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty and not in edit mode`() = runTest(testDispatcher) {
        val state = viewModel.state.value
        assertEquals("", state.firstName)
        assertEquals("", state.lastName)
        assertEquals("", state.email)
        assertFalse(state.isEditMode)
        assertFalse(state.showSaveConfirmation)
        assertFalse(state.isSaving)
    }

    @Test
    fun `when repository emits a user, profile fields are pre-filled`() = runTest(testDispatcher) {
        userData.value = User(
            id = "1",
            firstName = "Ahmed",
            lastName = "Ali",
            email = "ahmed@example.com",
            gender = null,
            dateOfBirth = "2000-01-01",
            heightCm = 180.0,
            weightKg = 75.0,
            diseaseIds = listOf(1),
            allergyIds = listOf(2),
        )
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Ahmed", state.firstName)
        assertEquals("Ali", state.lastName)
        assertEquals("ahmed@example.com", state.email)
        assertEquals("2000-01-01", state.dateOfBirth)
        assertEquals(180.0, state.heightCm)
        assertEquals(75.0, state.weightKg)
        assertTrue(state.selectedDiseaseIds.contains(1))
        assertTrue(state.selectedAllergyIds.contains(2))
    }

    @Test
    fun `diseases and allergies load from the offline use cases on init`() = runTest(testDispatcher) {
        val diseases = MutableStateFlow(listOf(Disease(id = 1, name = "Diabetes")))
        val allergies = MutableStateFlow(listOf(Allergy(id = 2, name = "Peanuts")))
        every { getDiseasesUseCase() } returns diseases
        every { getAllergiesUseCase() } returns allergies

        viewModel = EditProfileViewModel(
            getUserProfileUseCase,
            updateUserProfileUseCase,
            uploadAvatarUseCase,
            getDiseasesUseCase,
            getAllergiesUseCase,
            syncDiseasesUseCase,
            syncAllergiesUseCase,
            userRepository,
            context,
        )
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(Disease(id = 1, name = "Diabetes")), viewModel.state.value.diseases)
        assertEquals(listOf(Allergy(id = 2, name = "Peanuts")), viewModel.state.value.allergies)
        assertFalse(viewModel.state.value.isDiseasesLoading)
        assertFalse(viewModel.state.value.isAllergiesLoading)
    }

    @Test
    fun `UpdateFirstName updates state correctly`() {
        viewModel.onEvent(EditProfileEvent.UpdateFirstName("Ahmed"))
        assertEquals("Ahmed", viewModel.state.value.firstName)
    }

    @Test
    fun `UpdateLastName updates state correctly`() {
        viewModel.onEvent(EditProfileEvent.UpdateLastName("Ali"))
        assertEquals("Ali", viewModel.state.value.lastName)
    }

    @Test
    fun `UpdateDateOfBirth updates state correctly`() {
        viewModel.onEvent(EditProfileEvent.UpdateDateOfBirth("1999-05-05"))
        assertEquals("1999-05-05", viewModel.state.value.dateOfBirth)
    }

    @Test
    fun `UpdateHeight updates state correctly`() {
        viewModel.onEvent(EditProfileEvent.UpdateHeight(175.0))
        assertEquals(175.0, viewModel.state.value.heightCm)
    }

    @Test
    fun `UpdateWeight updates state correctly`() {
        viewModel.onEvent(EditProfileEvent.UpdateWeight(70.0))
        assertEquals(70.0, viewModel.state.value.weightKg)
    }

    @Test
    fun `SelectAvatar uploads immediately and returns to Idle on success`() = runTest(testDispatcher) {
        coEvery { uploadAvatarUseCase(any()) } returns Result.success(Unit)

        viewModel.onEvent(EditProfileEvent.SelectAvatar(pickedUri))
        assertEquals(AvatarUploadState.Uploading, viewModel.state.value.avatarUploadState)

        testScheduler.advanceUntilIdle()

        assertEquals(AvatarUploadState.Idle, viewModel.state.value.avatarUploadState)
        coVerify(exactly = 1) { uploadAvatarUseCase(any()) }
    }

    @Test
    fun `SelectAvatar upload failure surfaces an error state`() = runTest(testDispatcher) {
        coEvery { uploadAvatarUseCase(any()) } returns Result.failure(Exception("Network error"))

        viewModel.onEvent(EditProfileEvent.SelectAvatar(pickedUri))
        testScheduler.advanceUntilIdle()

        assertEquals(AvatarUploadState.Error, viewModel.state.value.avatarUploadState)
    }

    @Test
    fun `RetryAvatarUpload re-runs the last picked photo`() = runTest(testDispatcher) {
        coEvery { uploadAvatarUseCase(any()) } returns Result.failure(Exception("Network error"))
        viewModel.onEvent(EditProfileEvent.SelectAvatar(pickedUri))
        testScheduler.advanceUntilIdle()

        coEvery { uploadAvatarUseCase(any()) } returns Result.success(Unit)
        viewModel.onEvent(EditProfileEvent.RetryAvatarUpload)
        testScheduler.advanceUntilIdle()

        assertEquals(AvatarUploadState.Idle, viewModel.state.value.avatarUploadState)
        coVerify(exactly = 2) { uploadAvatarUseCase(any()) }
    }

    @Test
    fun `DismissAvatarUploadError resets to Idle without retrying`() = runTest(testDispatcher) {
        coEvery { uploadAvatarUseCase(any()) } returns Result.failure(Exception("Network error"))
        viewModel.onEvent(EditProfileEvent.SelectAvatar(pickedUri))
        testScheduler.advanceUntilIdle()

        viewModel.onEvent(EditProfileEvent.DismissAvatarUploadError)

        assertEquals(AvatarUploadState.Idle, viewModel.state.value.avatarUploadState)
        coVerify(exactly = 1) { uploadAvatarUseCase(any()) }
    }

    @Test
    fun `ToggleDisease adds then removes disease ID`() {
        viewModel.onEvent(EditProfileEvent.ToggleDisease(1))
        assertTrue(viewModel.state.value.selectedDiseaseIds.contains(1))

        viewModel.onEvent(EditProfileEvent.ToggleDisease(1))
        assertFalse(viewModel.state.value.selectedDiseaseIds.contains(1))
    }

    @Test
    fun `ToggleAllergy adds then removes allergy ID`() {
        viewModel.onEvent(EditProfileEvent.ToggleAllergy(2))
        assertTrue(viewModel.state.value.selectedAllergyIds.contains(2))

        viewModel.onEvent(EditProfileEvent.ToggleAllergy(2))
        assertFalse(viewModel.state.value.selectedAllergyIds.contains(2))
    }

    @Test
    fun `EditClicked enters edit mode and re-syncs diseases and allergies`() = runTest(testDispatcher) {
        viewModel.onEvent(EditProfileEvent.EditClicked)
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isEditMode)
        coVerify(exactly = 1) { syncDiseasesUseCase() }
        coVerify(exactly = 1) { syncAllergiesUseCase() }
    }

    @Test
    fun `RetryLoadDiseases re-syncs diseases and allergies`() = runTest(testDispatcher) {
        viewModel.onEvent(EditProfileEvent.RetryLoadDiseases)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { syncDiseasesUseCase() }
        coVerify(exactly = 1) { syncAllergiesUseCase() }
    }

    @Test
    fun `SaveClicked shows the save confirmation dialog`() {
        viewModel.onEvent(EditProfileEvent.SaveClicked)
        assertTrue(viewModel.state.value.showSaveConfirmation)
    }

    @Test
    fun `DismissSaveConfirmation clears the dialog`() {
        viewModel.onEvent(EditProfileEvent.SaveClicked)
        viewModel.onEvent(EditProfileEvent.DismissSaveConfirmation)
        assertFalse(viewModel.state.value.showSaveConfirmation)
    }

    @Test
    fun `ConfirmSave success clears the dialog, exits edit mode and stops saving`() = runTest(testDispatcher) {
        coEvery {
            updateUserProfileUseCase(
                firstName = any(),
                lastName = any(),
                gender = any(),
                dateOfBirth = any(),
                heightCm = any(),
                weightKg = any(),
                diseaseIds = any(),
                allergyIds = any(),
            )
        } returns Result.success(Unit)

        viewModel.onEvent(EditProfileEvent.SaveClicked)
        viewModel.onEvent(EditProfileEvent.ConfirmSave)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.showSaveConfirmation)
        assertFalse(state.isSaving)
        assertFalse(state.isEditMode)
        coVerify(exactly = 1) { updateUserProfileUseCase(any(), any(), any(), any(), any(), any(), any(), any()) }
        // Regression guard: the avatar is never part of the text-field save request.
        coVerify(exactly = 0) { uploadAvatarUseCase(any()) }
    }

    @Test
    fun `ConfirmSave failure stops saving without exiting edit mode`() = runTest(testDispatcher) {
        coEvery {
            updateUserProfileUseCase(
                firstName = any(),
                lastName = any(),
                gender = any(),
                dateOfBirth = any(),
                heightCm = any(),
                weightKg = any(),
                diseaseIds = any(),
                allergyIds = any(),
            )
        } returns Result.failure(Exception("Network error"))

        viewModel.onEvent(EditProfileEvent.EditClicked)
        viewModel.onEvent(EditProfileEvent.SaveClicked)
        viewModel.onEvent(EditProfileEvent.ConfirmSave)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isSaving)
        assertTrue(state.isEditMode)
    }

    @Test
    fun `BackClicked emits NavigateBack effect`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(EditProfileEvent.BackClicked)
            assertEquals(EditProfileEffect.NavigateBack, awaitItem())
        }
    }
}
