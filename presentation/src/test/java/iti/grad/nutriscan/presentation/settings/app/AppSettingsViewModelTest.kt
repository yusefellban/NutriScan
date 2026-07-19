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
import iti.grad.nutriscan.presentation.settings.app.state.AppSettingsEffect
import iti.grad.nutriscan.presentation.settings.app.state.AppSettingsEvent
import iti.grad.nutriscan.presentation.settings.app.viewmodel.AppSettingsViewModel
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
class AppSettingsViewModelTest {

    private lateinit var viewModel: AppSettingsViewModel
    private val getThemeModeUseCase: GetThemeModeUseCase = mockk()
    private val setThemeModeUseCase: SetThemeModeUseCase = mockk()
    private val getLanguageUseCase: GetLanguageUseCase = mockk()
    private val setLanguageUseCase: SetLanguageUseCase = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getThemeModeUseCase() } returns ThemeMode.SYSTEM
        coEvery { getLanguageUseCase() } returns AppLanguage.EN
        coEvery { setThemeModeUseCase(any()) } returns Unit
        coEvery { setLanguageUseCase(any()) } returns Unit
        viewModel = AppSettingsViewModel(
            getThemeModeUseCase,
            setThemeModeUseCase,
            getLanguageUseCase,
            setLanguageUseCase,
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
        fun `LanguageSelected updates state, persists, and emits ApplyLocale`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(AppSettingsEvent.LanguageSelected(AppLanguage.AR))
                testScheduler.advanceUntilIdle()

                Assertions.assertEquals(AppLanguage.AR, viewModel.state.value.selectedLanguage)
                coVerify { setLanguageUseCase(AppLanguage.AR) }

                val effect = awaitItem()
                Assertions.assertTrue(effect is AppSettingsEffect.ApplyLocale)
                Assertions.assertEquals(AppLanguage.AR, (effect as AppSettingsEffect.ApplyLocale).language)
            }
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
        fun `ProfileSettingsClicked emits NavigateToUserProfile`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(AppSettingsEvent.ProfileSettingsClicked)
                testScheduler.advanceUntilIdle()

                Assertions.assertTrue(awaitItem() is AppSettingsEffect.NavigateToUserProfile)
            }
        }

        @Test
        fun `TermsAndConditionsClicked emits ShowSnackbarRes`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(AppSettingsEvent.TermsAndConditionsClicked)
                testScheduler.advanceUntilIdle()

                val effect = awaitItem()
                Assertions.assertTrue(effect is AppSettingsEffect.ShowSnackbarRes)
                Assertions.assertEquals(
                    R.string.profile_setup_placeholder_coming_soon,
                    (effect as AppSettingsEffect.ShowSnackbarRes).messageResId,
                )
            }
        }

        @Test
        fun `HelpClicked emits ShowSnackbarRes`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(AppSettingsEvent.HelpClicked)
                testScheduler.advanceUntilIdle()

                Assertions.assertTrue(awaitItem() is AppSettingsEffect.ShowSnackbarRes)
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
            }
        }
    }
}
