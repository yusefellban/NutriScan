package iti.grad.nutriscan.presentation.settings.app

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.domain.settings.model.AppLanguage
import iti.grad.nutriscan.domain.settings.model.ThemeMode
import iti.grad.nutriscan.domain.settings.usecase.GetLanguageUseCase
import iti.grad.nutriscan.domain.settings.usecase.GetThemeModeUseCase
import iti.grad.nutriscan.domain.settings.usecase.SetLanguageUseCase
import iti.grad.nutriscan.domain.settings.usecase.SetThemeModeUseCase
import iti.grad.nutriscan.domain.auth.usecase.LogoutUseCase
import iti.grad.nutriscan.presentation.settings.app.state.AppSettingsEffect
import iti.grad.nutriscan.presentation.settings.app.state.AppSettingsEvent
import iti.grad.nutriscan.presentation.settings.app.viewmodel.AppSettingsViewModel
import iti.grad.nutriscan.domain.user.repository.IUserRepository
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import iti.grad.nutriscan.domain.user.usecase.DeleteAccountUseCase
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppSettingsViewModelTest {

    private lateinit var viewModel: AppSettingsViewModel
    private val getThemeModeUseCase: GetThemeModeUseCase = mockk()
    private val setThemeModeUseCase: SetThemeModeUseCase = mockk()
    private val getLanguageUseCase: GetLanguageUseCase = mockk()
    private val setLanguageUseCase: SetLanguageUseCase = mockk()
    private val logoutUseCase: LogoutUseCase = mockk()
    private val deleteAccountUseCase: DeleteAccountUseCase = mockk()
    private val userRepository: IUserRepository = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getThemeModeUseCase() } returns ThemeMode.SYSTEM
        coEvery { getLanguageUseCase() } returns AppLanguage.EN
        coEvery { setThemeModeUseCase(any()) } returns Unit
        coEvery { setLanguageUseCase(any()) } returns Unit
        coEvery { logoutUseCase() } returns Result.success(Unit)
        coEvery { userRepository.getUserData() } returns flowOf(null)
        coEvery { deleteAccountUseCase() } returns Result.success(mockk(relaxed = true))
        viewModel = AppSettingsViewModel(
            getThemeModeUseCase,
            setThemeModeUseCase,
            getLanguageUseCase,
            setLanguageUseCase,
            logoutUseCase,
            deleteAccountUseCase,
            userRepository,
        )
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Initial State")
    inner class InitialState {

        @Test
        fun `initial state defaults to SYSTEM theme and EN language before loading`() {
            val state = viewModel.state.value
            Assertions.assertEquals(ThemeMode.SYSTEM, state.selectedThemeMode)
            Assertions.assertEquals(AppLanguage.EN, state.selectedLanguage)
            Assertions.assertFalse(state.showLogoutConfirmDialog)
        }

        @Test
        fun `state loads persisted theme and language on init`() = runTest {
            coEvery { getThemeModeUseCase() } returns ThemeMode.DARK
            coEvery { getLanguageUseCase() } returns AppLanguage.AR
            val loadedViewModel = AppSettingsViewModel(
                getThemeModeUseCase,
                setThemeModeUseCase,
                getLanguageUseCase,
                setLanguageUseCase,
                logoutUseCase,
                deleteAccountUseCase,
                userRepository,
            )
            testScheduler.advanceUntilIdle()

            Assertions.assertEquals(ThemeMode.DARK, loadedViewModel.state.value.selectedThemeMode)
            Assertions.assertEquals(AppLanguage.AR, loadedViewModel.state.value.selectedLanguage)
        }
    }

    @Nested
    @DisplayName("Theme Selection")
    inner class ThemeSelection {

        @Test
        fun `ThemeModeSelected updates state and persists via use case`() = runTest {
            viewModel.onEvent(AppSettingsEvent.ThemeModeSelected(ThemeMode.DARK))
            testScheduler.advanceUntilIdle()

            Assertions.assertEquals(ThemeMode.DARK, viewModel.state.value.selectedThemeMode)
            coVerify { setThemeModeUseCase(ThemeMode.DARK) }
        }
    }

    @Nested
    @DisplayName("Language Selection")
    inner class LanguageSelection {

        @Test
        fun `LanguageSelected updates state and persists via use case`() = runTest {
            viewModel.onEvent(AppSettingsEvent.LanguageSelected(AppLanguage.AR))
            testScheduler.advanceUntilIdle()

            Assertions.assertEquals(AppLanguage.AR, viewModel.state.value.selectedLanguage)
            coVerify { setLanguageUseCase(AppLanguage.AR) }
        }
    }

    @Nested
    @DisplayName("Navigation")
    inner class Navigation {

        @Test
        fun `BackClicked emits NavigateBack`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(AppSettingsEvent.BackClicked)
                testScheduler.advanceUntilIdle()

                Assertions.assertTrue(awaitItem() is AppSettingsEffect.NavigateBack)
            }
        }

        @Test
        fun `ProfileSettingsClicked emits NavigateToEditProfile`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(AppSettingsEvent.ProfileSettingsClicked)
                testScheduler.advanceUntilIdle()

                Assertions.assertTrue(awaitItem() is AppSettingsEffect.NavigateToEditProfile)
            }
        }

        @Test
        fun `TermsAndConditionsClicked emits NavigateToTermsAndConditions`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(AppSettingsEvent.TermsAndConditionsClicked)
                testScheduler.advanceUntilIdle()

                Assertions.assertTrue(awaitItem() is AppSettingsEffect.NavigateToTermsAndConditions)
            }
        }

        @Test
        fun `HelpClicked emits NavigateToHelp`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(AppSettingsEvent.HelpClicked)
                testScheduler.advanceUntilIdle()

                Assertions.assertTrue(awaitItem() is AppSettingsEffect.NavigateToHelp)
            }
        }
    }

    @Nested
    @DisplayName("Logout Flow")
    inner class LogoutFlow {

        @Test
        fun `LogoutClicked shows confirm dialog with no effect`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(AppSettingsEvent.LogoutClicked)
                testScheduler.advanceUntilIdle()

                Assertions.assertTrue(viewModel.state.value.showLogoutConfirmDialog)
                expectNoEvents()
            }
        }

        @Test
        fun `LogoutDismissed clears the dialog with no effect`() = runTest {
            viewModel.onEvent(AppSettingsEvent.LogoutClicked)

            viewModel.effect.test {
                viewModel.onEvent(AppSettingsEvent.LogoutDismissed)
                testScheduler.advanceUntilIdle()

                Assertions.assertFalse(viewModel.state.value.showLogoutConfirmDialog)
                expectNoEvents()
            }
        }

        @Test
        fun `LogoutConfirmed clears the dialog and emits NavigateToLogin`() = runTest {
            viewModel.onEvent(AppSettingsEvent.LogoutClicked)

            viewModel.effect.test {
                viewModel.onEvent(AppSettingsEvent.LogoutConfirmed)
                testScheduler.advanceUntilIdle()

                Assertions.assertFalse(viewModel.state.value.showLogoutConfirmDialog)
                Assertions.assertTrue(awaitItem() is AppSettingsEffect.NavigateToLogin)
                coVerify(exactly = 1) { logoutUseCase() }
            }
        }
    @Test
    fun `DeleteAccountClicked event should show confirmation dialog`() = runTest {
        viewModel.onEvent(AppSettingsEvent.DeleteAccountClicked)
        
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.showDeleteAccountConfirmDialog)
        }
    }

    @Test
    fun `DeleteAccountDismissed event should hide confirmation dialog`() = runTest {
        viewModel.onEvent(AppSettingsEvent.DeleteAccountClicked)
        viewModel.onEvent(AppSettingsEvent.DeleteAccountDismissed)
        
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showDeleteAccountConfirmDialog)
            assertFalse(state.isDeletingAccount)
        }
    }

    @Test
    fun `DeleteAccountConfirmed on success should logout and emit AccountDeleted effect`() = runTest(testDispatcher) {
        val info = iti.grad.nutriscan.domain.user.model.AccountDeletionInfo("2026-08-22", 15)
        coEvery { deleteAccountUseCase() } returns Result.success(info)
        coEvery { logoutUseCase() } returns Result.success(Unit)

        viewModel.effect.test {
            viewModel.onEvent(AppSettingsEvent.DeleteAccountConfirmed)
            
            testScheduler.advanceUntilIdle()

            assertEquals(AppSettingsEffect.AccountDeleted, awaitItem())
            
            coVerify(exactly = 1) { deleteAccountUseCase() }
            coVerify(exactly = 1) { logoutUseCase() }
        }
    }

    @Test
    fun `DeleteAccountConfirmed on failure should hide loading and show error`() = runTest(testDispatcher) {
        val errorMessage = "Network Error"
        coEvery { deleteAccountUseCase() } returns Result.failure(Exception(errorMessage))

        viewModel.onEvent(AppSettingsEvent.DeleteAccountConfirmed)
        testScheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showDeleteAccountConfirmDialog)
            assertFalse(state.isDeletingAccount)
            assertEquals(errorMessage, state.deleteAccountError)
        }
    }
}
}
