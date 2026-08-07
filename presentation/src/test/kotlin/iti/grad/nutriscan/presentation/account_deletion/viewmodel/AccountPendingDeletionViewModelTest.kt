package iti.grad.nutriscan.presentation.account_deletion.viewmodel

import app.cash.turbine.test
import androidx.lifecycle.SavedStateHandle
import iti.grad.nutriscan.domain.auth.usecase.LogoutUseCase
import iti.grad.nutriscan.domain.user.usecase.RestoreAccountUseCase
import iti.grad.nutriscan.presentation.account_deletion.state.AccountPendingDeletionEffect
import iti.grad.nutriscan.presentation.account_deletion.state.AccountPendingDeletionEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
class AccountPendingDeletionViewModelTest {

    private lateinit var restoreAccountUseCase: RestoreAccountUseCase
    private lateinit var logoutUseCase: LogoutUseCase
    private lateinit var viewModel: AccountPendingDeletionViewModel

    private lateinit var savedStateHandle: SavedStateHandle

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        savedStateHandle = SavedStateHandle(mapOf("scheduledDeletionAt" to "2026-08-22"))
        restoreAccountUseCase = mockk(relaxed = true)
        logoutUseCase = mockk(relaxed = true)
        
        viewModel = AccountPendingDeletionViewModel(savedStateHandle, restoreAccountUseCase, logoutUseCase)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `RestoreClicked on success should emit NavigateToHome`() = runTest(testDispatcher) {
        coEvery { restoreAccountUseCase() } returns Result.success(Unit)

        viewModel.effect.test {
            viewModel.onEvent(AccountPendingDeletionEvent.RestoreAccountClicked)
            
            testScheduler.advanceUntilIdle()

            assertEquals(AccountPendingDeletionEffect.NavigateToHome, awaitItem())
            
            coVerify(exactly = 1) { restoreAccountUseCase() }
        }
    }

    @Test
    fun `RestoreClicked on failure should show error state`() = runTest(testDispatcher) {
        val errorMessage = "Network Error"
        coEvery { restoreAccountUseCase() } returns Result.failure(Exception(errorMessage))

        viewModel.onEvent(AccountPendingDeletionEvent.RestoreAccountClicked)
        testScheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isRestoring)
            assertEquals(errorMessage, state.error)
        }
    }

    @Test
    fun `LogoutClicked should logout and emit NavigateToLogin`() = runTest(testDispatcher) {
        coEvery { logoutUseCase() } returns Result.success(Unit)

        viewModel.effect.test {
            viewModel.onEvent(AccountPendingDeletionEvent.LogoutClicked)
            
            testScheduler.advanceUntilIdle()

            assertEquals(AccountPendingDeletionEffect.NavigateToLogin, awaitItem())
            
            coVerify(exactly = 1) { logoutUseCase() }
        }
    }
}
