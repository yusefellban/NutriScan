package iti.grad.nutriscan.presentation.settings.notifications

import android.content.Context
import android.os.PowerManager
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.usecase.ObserveNotificationPrefsUseCase
import iti.grad.nutriscan.domain.notification.usecase.SendTestNotificationUseCase
import iti.grad.nutriscan.domain.notification.usecase.SetNotificationPrefUseCase
import iti.grad.nutriscan.domain.notification.usecase.SetQuietHoursEnabledUseCase
import iti.grad.nutriscan.presentation.settings.notifications.state.NotificationSettingsEffect
import iti.grad.nutriscan.presentation.settings.notifications.state.NotificationSettingsEvent
import iti.grad.nutriscan.presentation.settings.notifications.viewmodel.NotificationSettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
class NotificationSettingsViewModelTest {

    private lateinit var viewModel: NotificationSettingsViewModel
    private val context: Context = mockk()
    private val powerManager: PowerManager = mockk()
    private val observePrefs: ObserveNotificationPrefsUseCase = mockk()
    private val setPref: SetNotificationPrefUseCase = mockk()
    private val setQuietHoursEnabled: SetQuietHoursEnabledUseCase = mockk()
    private val sendTestNotification: SendTestNotificationUseCase = mockk()
    private val testDispatcher = StandardTestDispatcher()

    private fun createViewModel() = NotificationSettingsViewModel(
        context, observePrefs, setPref, setQuietHoursEnabled, sendTestNotification,
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { context.getSystemService(Context.POWER_SERVICE) } returns powerManager
        every { context.packageName } returns "iti.grad.nutriscan"
        every { powerManager.isIgnoringBatteryOptimizations("iti.grad.nutriscan") } returns true
        coEvery { observePrefs() } returns flowOf(NotificationPrefs.default())
        coEvery { setPref(any(), any()) } returns Result.success(Unit)
        coEvery { setQuietHoursEnabled(any()) } returns Result.success(Unit)
        every { sendTestNotification() } returns Unit
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Initial State")
    inner class InitialState {

        @Test
        fun `state loads prefs on init`() = runTest {
            testScheduler.advanceUntilIdle()

            val state = viewModel.state.value
            Assertions.assertTrue(state.enabled[NotificationType.WATER] == true)
            Assertions.assertTrue(state.quietHoursEnabled)
            Assertions.assertEquals(NotificationPrefs.DEFAULT_QUIET_START, state.quietHoursStart)
            Assertions.assertEquals(NotificationPrefs.DEFAULT_QUIET_END, state.quietHoursEnd)
        }
    }

    @Nested
    @DisplayName("Toggle Type")
    inner class ToggleType {

        @Test
        fun `ToggleType updates state and persists via use case`() = runTest {
            viewModel.onEvent(NotificationSettingsEvent.ToggleType(NotificationType.WATER, false))
            testScheduler.advanceUntilIdle()

            Assertions.assertFalse(viewModel.state.value.enabled[NotificationType.WATER]!!)
            coVerify { setPref(NotificationType.WATER, false) }
        }

        @Test
        fun `external prefs update after a local edit is still applied to state`() = runTest {
            val prefsFlow = MutableSharedFlow<NotificationPrefs>(replay = 1)
            prefsFlow.tryEmit(NotificationPrefs.default())
            coEvery { observePrefs() } returns prefsFlow
            viewModel = createViewModel()
            testScheduler.advanceUntilIdle()

            viewModel.onEvent(NotificationSettingsEvent.ToggleType(NotificationType.WATER, false))
            testScheduler.advanceUntilIdle()
            Assertions.assertFalse(viewModel.state.value.enabled[NotificationType.WATER]!!)

            // Simulates an external reset/sync: the underlying prefs flow emits a completely
            // different value after the local edit.
            val externalReset = NotificationPrefs.default().copy(
                enabled = NotificationType.entries.associateWith { false },
                quietHoursEnabled = false,
            )
            prefsFlow.tryEmit(externalReset)
            testScheduler.advanceUntilIdle()

            Assertions.assertEquals(externalReset.enabled, viewModel.state.value.enabled)
            Assertions.assertFalse(viewModel.state.value.quietHoursEnabled)
        }
    }

    @Nested
    @DisplayName("Quiet Hours")
    inner class QuietHours {

        @Test
        fun `QuietHoursEnabledChanged updates state and persists via use case`() = runTest {
            viewModel.onEvent(NotificationSettingsEvent.QuietHoursEnabledChanged(false))
            testScheduler.advanceUntilIdle()

            Assertions.assertFalse(viewModel.state.value.quietHoursEnabled)
            coVerify { setQuietHoursEnabled(false) }
        }
    }

    @Nested
    @DisplayName("Test Notification")
    inner class TestNotification {

        @Test
        fun `SendTestNotificationClicked calls use case and emits TestNotificationSent`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(NotificationSettingsEvent.SendTestNotificationClicked)
                testScheduler.advanceUntilIdle()

                Assertions.assertTrue(awaitItem() is NotificationSettingsEffect.TestNotificationSent)
            }
            verify(exactly = 1) { sendTestNotification() }
        }
    }

    @Nested
    @DisplayName("Navigation")
    inner class Navigation {

        @Test
        fun `BackClicked emits NavigateBack`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(NotificationSettingsEvent.BackClicked)
                testScheduler.advanceUntilIdle()

                Assertions.assertTrue(awaitItem() is NotificationSettingsEffect.NavigateBack)
            }
        }
    }

    @Nested
    @DisplayName("Battery Optimization")
    inner class BatteryOptimization {

        @Test
        fun `state reflects isIgnoringBatteryOptimizations on init`() = runTest {
            every { powerManager.isIgnoringBatteryOptimizations("iti.grad.nutriscan") } returns false
            viewModel = createViewModel()
            testScheduler.advanceUntilIdle()

            Assertions.assertFalse(viewModel.state.value.isIgnoringBatteryOptimizations)
        }

        @Test
        fun `refreshBatteryOptimizationState re-reads the current exemption state`() = runTest {
            every { powerManager.isIgnoringBatteryOptimizations("iti.grad.nutriscan") } returns false
            viewModel = createViewModel()
            testScheduler.advanceUntilIdle()
            Assertions.assertFalse(viewModel.state.value.isIgnoringBatteryOptimizations)

            every { powerManager.isIgnoringBatteryOptimizations("iti.grad.nutriscan") } returns true
            viewModel.refreshBatteryOptimizationState()

            Assertions.assertTrue(viewModel.state.value.isIgnoringBatteryOptimizations)
        }

        @Test
        fun `AllowBackgroundNotificationsClicked emits RequestIgnoreBatteryOptimizations`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(NotificationSettingsEvent.AllowBackgroundNotificationsClicked)
                testScheduler.advanceUntilIdle()

                Assertions.assertTrue(awaitItem() is NotificationSettingsEffect.RequestIgnoreBatteryOptimizations)
            }
        }
    }
}
