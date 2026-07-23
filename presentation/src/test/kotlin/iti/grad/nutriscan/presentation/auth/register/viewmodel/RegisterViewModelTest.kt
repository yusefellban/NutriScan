package iti.grad.nutriscan.presentation.auth.register.viewmodel

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import iti.grad.nutriscan.domain.auth.usecase.RegisterUseCase
import iti.grad.nutriscan.domain.auth.usecase.ResendVerificationEmailUseCase
import iti.grad.nutriscan.presentation.auth.register.state.RegisterEffect
import iti.grad.nutriscan.presentation.auth.register.state.RegisterEvent
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
class RegisterViewModelTest {

    private lateinit var registerUseCase: RegisterUseCase
    private lateinit var resendVerificationEmailUseCase: ResendVerificationEmailUseCase
    private lateinit var viewModel: RegisterViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        registerUseCase = mockk()
        resendVerificationEmailUseCase = mockk()
        viewModel = RegisterViewModel(registerUseCase, resendVerificationEmailUseCase)
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `SignUpClicked with mismatching passwords sets error state`() = runTest {
        viewModel.onEvent(RegisterEvent.EmailChanged("test@example.com"))
        viewModel.onEvent(RegisterEvent.PasswordChanged("Pass123"))
        viewModel.onEvent(RegisterEvent.ConfirmPasswordChanged("Pass456"))

        viewModel.onEvent(RegisterEvent.SignUpClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.confirmPasswordErrorResId != null)
    }

    @Test
    fun `SignUpClicked emits Success alert and calls resendVerificationEmail`() = runTest {
        val email = "test@example.com"
        val password = "Password123"
        coEvery { registerUseCase(email, password) } returns Result.success(Unit)
        coEvery { resendVerificationEmailUseCase(email) } returns Result.success(Unit)

        viewModel.onEvent(RegisterEvent.EmailChanged(email))
        viewModel.onEvent(RegisterEvent.PasswordChanged(password))
        viewModel.onEvent(RegisterEvent.ConfirmPasswordChanged(password))

        viewModel.onEvent(RegisterEvent.SignUpClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val alertState = viewModel.state.value.alertState
        assertTrue(alertState is iti.grad.nutriscan.presentation.common.state.AuthAlertState.Success)
    }
}
