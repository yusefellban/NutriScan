package iti.grad.nutriscan.presentation.notification_history.viewmodel

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import iti.grad.nutriscan.domain.notification.model.NotificationHistoryItem
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.usecase.ClearNotificationHistoryUseCase
import iti.grad.nutriscan.domain.notification.usecase.DeleteNotificationUseCase
import iti.grad.nutriscan.domain.notification.usecase.GetNotificationHistoryUseCase
import iti.grad.nutriscan.domain.notification.usecase.MarkNotificationReadUseCase
import iti.grad.nutriscan.presentation.notification_history.state.NotificationHistoryEffect
import iti.grad.nutriscan.presentation.notification_history.state.NotificationHistoryEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationHistoryViewModelTest {

    private lateinit var getNotificationHistory: GetNotificationHistoryUseCase
    private lateinit var deleteNotification: DeleteNotificationUseCase
    private lateinit var clearNotificationHistory: ClearNotificationHistoryUseCase
    private lateinit var markNotificationRead: MarkNotificationReadUseCase
    
    private lateinit var viewModel: NotificationHistoryViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val dummyNotifications = listOf(
        NotificationHistoryItem(
            id = 1L,
            title = "Scan Result",
            body = "Scan is safe",
            type = NotificationType.SCAN,
            timestamp = System.currentTimeMillis() - 60000, // 1 min ago
            isRead = false
        ),
        NotificationHistoryItem(
            id = 2L,
            title = "Water Reminder",
            body = "Drink water",
            type = NotificationType.WATER,
            timestamp = System.currentTimeMillis() - 3600000, // 1 hour ago
            isRead = true
        )
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getNotificationHistory = mockk()
        deleteNotification = mockk()
        clearNotificationHistory = mockk()
        markNotificationRead = mockk()
        
        every { getNotificationHistory() } returns flowOf(dummyNotifications)
        coEvery { deleteNotification(any()) } returns Unit
        coEvery { clearNotificationHistory() } returns Unit
        coEvery { markNotificationRead(any()) } returns Unit

        viewModel = NotificationHistoryViewModel(
            getNotificationHistory,
            deleteNotification,
            clearNotificationHistory,
            markNotificationRead
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
        fun `initial state loads notifications from usecase`() = runTest {
            testScheduler.runCurrent()
            val state = viewModel.state.value
            
            assertFalse(state.isLoading)
            assertFalse(state.isEmpty)
            assertEquals(2, state.notifications.size)
            assertEquals(1L, state.notifications[0].id)
            assertEquals(2L, state.notifications[1].id)
        }
    }

    @Nested
    @DisplayName("Events")
    inner class Events {

        @Test
        fun `DeleteNotification calls usecase and emits undo snackbar`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(NotificationHistoryEvent.DeleteNotification(1L))
                testScheduler.runCurrent()

                coVerify(exactly = 1) { deleteNotification(1L) }
                val effect = awaitItem()
                assertTrue(effect is NotificationHistoryEffect.ShowUndoSnackbar)
            }
        }

        @Test
        fun `ClearAll calls usecase and emits undo snackbar`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(NotificationHistoryEvent.ClearAll)
                testScheduler.runCurrent()

                coVerify(exactly = 1) { clearNotificationHistory() }
                val effect = awaitItem()
                assertTrue(effect is NotificationHistoryEffect.ShowUndoSnackbar)
            }
        }

        @Test
        fun `NotificationClicked marks notification as read`() = runTest {
            viewModel.onEvent(NotificationHistoryEvent.NotificationClicked(1L))
            testScheduler.runCurrent()

            coVerify(exactly = 1) { markNotificationRead(1L) }
        }

        @Test
        fun `BackClicked emits NavigateBack effect`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(NotificationHistoryEvent.BackClicked)
                testScheduler.runCurrent()

                val effect = awaitItem()
                assertTrue(effect is NotificationHistoryEffect.NavigateBack)
            }
        }

        @Test
        fun `NavigateToSettingsClicked emits NavigateToSettings effect`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(NotificationHistoryEvent.NavigateToSettingsClicked)
                testScheduler.runCurrent()

                val effect = awaitItem()
                assertTrue(effect is NotificationHistoryEffect.NavigateToSettings)
            }
        }
    }
}
