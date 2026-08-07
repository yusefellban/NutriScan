package iti.grad.nutriscan.presentation.auth.forgot_password.viewmodel

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import iti.grad.nutriscan.domain.auth.usecase.ForgotPasswordUseCase
import iti.grad.nutriscan.presentation.auth.forgot_password.state.ForgotPasswordEffect
import iti.grad.nutriscan.presentation.auth.forgot_password.state.ForgotPasswordEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {

    private lateinit var useCase: ForgotPasswordUseCase
    private lateinit var viewModel: ForgotPasswordViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        useCase = mockk()
        viewModel = ForgotPasswordViewModel(useCase)
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `SendResetLink with invalid email updates state with error`() = runTest {
        viewModel.onEvent(ForgotPasswordEvent.EmailChanged("invalid"))
        viewModel.onEvent(ForgotPasswordEvent.SendResetLink)
        
        val state = viewModel.state.value
        assertTrue(state.emailErrorResId != null)
    }

    @Test
    fun `SendResetLink with valid email calls usecase and updates state on success`() = runTest {
        val validEmail = "test@example.com"
        coEvery { useCase(validEmail) } returns Result.success(Unit)
        
        viewModel.onEvent(ForgotPasswordEvent.EmailChanged(validEmail))
        
        viewModel.effect.test {
            viewModel.onEvent(ForgotPasswordEvent.SendResetLink)
            testDispatcher.scheduler.advanceUntilIdle()
            
            val state = viewModel.state.value
            assertTrue(state.showPasswordSentDialog)
            assertEquals(false, state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
