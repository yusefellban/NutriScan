package iti.grad.nutriscan.presentation.profile_setup

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import iti.grad.nutriscan.domain.allergy.usecase.GetAllergiesUseCase
import iti.grad.nutriscan.domain.allergy.usecase.SyncAllergiesUseCase
import iti.grad.nutriscan.domain.disease.usecase.GetDiseasesUseCase
import iti.grad.nutriscan.domain.disease.usecase.SyncDiseasesUseCase
import iti.grad.nutriscan.domain.onboarding.usecase.CompleteOnboardingUseCase
import iti.grad.nutriscan.domain.user.usecase.UpdateUserProfileUseCase
import iti.grad.nutriscan.presentation.profile_setup.state.Gender
import iti.grad.nutriscan.presentation.profile_setup.state.ProfileSetupPagerEffect
import iti.grad.nutriscan.presentation.profile_setup.state.ProfileSetupPagerEvent
import iti.grad.nutriscan.presentation.profile_setup.viewmodel.ProfileSetupPagerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
    private val completeOnboardingUseCase: CompleteOnboardingUseCase = mockk(relaxed = true)
    private val getDiseasesUseCase: GetDiseasesUseCase = mockk()
    private val getAllergiesUseCase: GetAllergiesUseCase = mockk()
    private val syncDiseasesUseCase: SyncDiseasesUseCase = mockk()
    private val syncAllergiesUseCase: SyncAllergiesUseCase = mockk()
    private val updateUserProfileUseCase: UpdateUserProfileUseCase = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock initial data loading
        every { getDiseasesUseCase() } returns flowOf(emptyList())
        every { getAllergiesUseCase() } returns flowOf(emptyList())
        coEvery { syncDiseasesUseCase() } returns Result.success(Unit)
        coEvery { syncAllergiesUseCase() } returns Result.success(Unit)

        viewModel = ProfileSetupPagerViewModel(
            completeOnboardingUseCase = completeOnboardingUseCase,
            getDiseasesUseCase = getDiseasesUseCase,
            getAllergiesUseCase = getAllergiesUseCase,
            syncDiseasesUseCase = syncDiseasesUseCase,
            syncAllergiesUseCase = syncAllergiesUseCase,
            updateUserProfileUseCase = updateUserProfileUseCase
        )
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
            // Move to last page (index 4)
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

        @Test
        fun `SelectGender FEMALE updates selectedGender in state`() {
            viewModel.onEvent(ProfileSetupPagerEvent.SelectGender(Gender.FEMALE))
            Assertions.assertEquals(Gender.FEMALE, viewModel.state.value.selectedGender)
        }

        @Test
        fun `SelectGender MALE replaces previously selected FEMALE`() {
            viewModel.onEvent(ProfileSetupPagerEvent.SelectGender(Gender.FEMALE))
            viewModel.onEvent(ProfileSetupPagerEvent.SelectGender(Gender.MALE))
            Assertions.assertEquals(Gender.MALE, viewModel.state.value.selectedGender)
        }
    }

    // ── Height & DOB Selection ──────────────────────────────────────────

    @Nested
    @DisplayName("Height and DOB Selection")
    inner class HeightAndDobSelection {

        @Test
        fun `initial state has default selectedHeightCm as 170`() {
            val state = viewModel.state.value
            Assertions.assertEquals(170, state.selectedHeightCm)
            Assertions.assertNull(state.selectedDateOfBirthMillis)
        }

        @Test
        fun `SelectHeight updates selectedHeightCm in state`() {
            viewModel.onEvent(ProfileSetupPagerEvent.SelectHeight(183))
            Assertions.assertEquals(183, viewModel.state.value.selectedHeightCm)
        }

        @Test
        fun `SelectDateOfBirth updates selectedDateOfBirthMillis in state`() {
            val millis = 1069542000000L // 23/11/2003
            viewModel.onEvent(ProfileSetupPagerEvent.SelectDateOfBirth(millis))
            Assertions.assertEquals(millis, viewModel.state.value.selectedDateOfBirthMillis)
        }
    }

    // ── Weight Selection ────────────────────────────────────────────────

    @Nested
    @DisplayName("Weight Selection")
    inner class WeightSelection {

        @Test
        fun `initial state has default selectedWeightKg as 60`() {
            Assertions.assertEquals(60, viewModel.state.value.selectedWeightKg)
        }

        @Test
        fun `SelectWeight updates selectedWeightKg in state`() {
            viewModel.onEvent(ProfileSetupPagerEvent.SelectWeight(75))
            Assertions.assertEquals(75, viewModel.state.value.selectedWeightKg)
        }
    }

    // ── Health Profile: Diseases ────────────────────────────────────────

    @Nested
    @DisplayName("Health Profile — Diseases")
    inner class HealthProfileDiseases {

        @Test
        fun `initial state has empty selectedDiseaseIds`() {
            val state = viewModel.state.value
            Assertions.assertTrue(state.selectedDiseaseIds.isEmpty())
        }

        @Test
        fun `ToggleDisease adds then removes disease ID`() {
            viewModel.onEvent(ProfileSetupPagerEvent.ToggleDisease(1))
            Assertions.assertTrue(viewModel.state.value.selectedDiseaseIds.contains(1))

            viewModel.onEvent(ProfileSetupPagerEvent.ToggleDisease(1))
            Assertions.assertFalse(viewModel.state.value.selectedDiseaseIds.contains(1))
        }
    }

    // ── Health Profile: Allergies ────────────────────────────────────────

    @Nested
    @DisplayName("Health Profile — Allergies")
    inner class HealthProfileAllergies {

        @Test
        fun `initial state has empty selectedAllergyIds`() {
            val state = viewModel.state.value
            Assertions.assertTrue(state.selectedAllergyIds.isEmpty())
        }

        @Test
        fun `ToggleAllergy adds then removes allergy ID`() {
            viewModel.onEvent(ProfileSetupPagerEvent.ToggleAllergy(10))
            Assertions.assertTrue(viewModel.state.value.selectedAllergyIds.contains(10))

            viewModel.onEvent(ProfileSetupPagerEvent.ToggleAllergy(10))
            Assertions.assertFalse(viewModel.state.value.selectedAllergyIds.contains(10))
        }
    }

    // ── Save Profile ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Save Profile")
    inner class SaveProfile {

        @Test
        fun `SaveProfile success calls use cases and emits NavigateToHome`() = runTest {
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
                    avatarUrl = any(),
                )
            } returns Result.success(Unit)

            viewModel.effect.test {
                viewModel.onEvent(ProfileSetupPagerEvent.SaveProfile)
                testScheduler.advanceUntilIdle()

                val effect = awaitItem()
                Assertions.assertTrue(effect is ProfileSetupPagerEffect.NavigateToHome)
                Assertions.assertFalse(viewModel.state.value.isLoading)
                coVerify(exactly = 1) {
                    updateUserProfileUseCase(
                        firstName = any(),
                        lastName = any(),
                        gender = any(),
                        dateOfBirth = any(),
                        heightCm = any(),
                        weightKg = any(),
                        diseaseIds = any(),
                        allergyIds = any(),
                        avatarUrl = any(),
                    )
                }
                coVerify(exactly = 1) { completeOnboardingUseCase() }
            }
        }

        @Test
        fun `SaveProfile failure emits ShowSnackbar`() = runTest {
            val errorMessage = "Network Error"
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
                    avatarUrl = any(),
                )
            } returns Result.failure(Exception(errorMessage))

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
            }
        }
    }
}
