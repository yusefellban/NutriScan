package iti.grad.nutriscan.presentation.auth.forgot_password.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest
import iti.grad.nutriscan.presentation.auth.forgot_password.state.ForgotPasswordEffect
import iti.grad.nutriscan.presentation.auth.forgot_password.state.ForgotPasswordEvent
import iti.grad.nutriscan.presentation.auth.forgot_password.view.components.ResetMethodCard
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import iti.grad.nutriscan.presentation.auth.forgot_password.state.ResetMethod
import androidx.compose.material3.SnackbarHost
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.auth.forgot_password.view.components.PasswordSentDialog
import androidx.compose.foundation.layout.Spacer
import iti.grad.nutriscan.presentation.auth.forgot_password.viewmodel.ForgotPasswordViewModel
import iti.grad.nutriscan.presentation.auth.forgot_password.view.components.ForgotPasswordHeader
import iti.grad.nutriscan.presentation.common.components.AppSnackbar
import iti.grad.nutriscan.presentation.common.components.showAppSnackbar
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import iti.grad.nutriscan.presentation.common.components.ErrorAlert
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.SnackbarHostState
import iti.grad.nutriscan.presentation.common.components.AppButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import iti.grad.nutriscan.presentation.auth.forgot_password.view.components.EmailInputDialog
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import iti.grad.nutriscan.presentation.auth.forgot_password.state.ForgotPasswordState

@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ForgotPasswordEffect.NavigateBack -> onNavigateBack()
                is ForgotPasswordEffect.ShowErrorDialog -> {
                    errorDialogMessage = effect.messageStr
                }
                is ForgotPasswordEffect.ShowSnackbar -> {
                    val message = effect.messageStr
                        ?: effect.messageResId?.let { context.getString(it) }
                        ?: ""
                    snackbarHostState.showAppSnackbar(
                        message = message,
                        type = effect.type
                    )
                }
            }
        }
    }

    if (errorDialogMessage != null) {
        ErrorAlert(
            title = stringResource(id = R.string.alert_reset_failed_title),
            message = errorDialogMessage!!,
            onDismiss = { errorDialogMessage = null }
        )
    }

    ForgotPasswordScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState
    )
}

@Composable
private fun ForgotPasswordScreenContent(
    state: ForgotPasswordState,
    onEvent: (ForgotPasswordEvent) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                AppSnackbar(snackbarData = data)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Teal header with back button ─────────────────────────────
            ForgotPasswordHeader(
                onBackClick = { onEvent(ForgotPasswordEvent.BackClicked) }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Reset method cards ───────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ResetMethodCard(
                    iconRes = R.drawable.ic_email,
                    titleResId = R.string.reset_method_email,
                    subtitleResId = R.string.reset_method_email_desc,
                    isSelected = state.selectedMethod == ResetMethod.EMAIL,
                    onClick = { onEvent(ForgotPasswordEvent.MethodSelected(ResetMethod.EMAIL)) }
                )

                ResetMethodCard(
                    iconRes = R.drawable.ic_gender_male_1,
                    titleResId = R.string.reset_method_2fa,
                    subtitleResId = R.string.reset_method_2fa_desc,
                    isSelected = state.selectedMethod == ResetMethod.TWO_FA,
                    onClick = { onEvent(ForgotPasswordEvent.MethodSelected(ResetMethod.TWO_FA)) }
                )

                ResetMethodCard(
                    iconRes = R.drawable.ic_gauth,
                    titleResId = R.string.reset_method_gauth,
                    subtitleResId = R.string.reset_method_gauth_desc,
                    isSelected = state.selectedMethod == ResetMethod.GOOGLE_AUTH,
                    onClick = { onEvent(ForgotPasswordEvent.MethodSelected(ResetMethod.GOOGLE_AUTH)) }
                )

                ResetMethodCard(
                    iconRes = R.drawable.ic_phone,
                    titleResId = R.string.reset_method_sms,
                    subtitleResId = R.string.reset_method_sms_desc,
                    isSelected = state.selectedMethod == ResetMethod.SMS,
                    onClick = { onEvent(ForgotPasswordEvent.MethodSelected(ResetMethod.SMS)) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Reset Password button ────────────────────────────────────
            AppButton(
                textResId = R.string.action_reset_password,
                isLoading = state.isLoading,
                onClick = { onEvent(ForgotPasswordEvent.ResetPasswordClicked) }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // ── Password Sent dialog overlay ─────────────────────────────────────
    if (state.showPasswordSentDialog) {
        PasswordSentDialog(
            maskedEmail = state.maskedEmail,
            isLoading = state.isLoading,
            onResendCode = { onEvent(ForgotPasswordEvent.ResendCodeClicked) },
            onDismiss = { onEvent(ForgotPasswordEvent.DismissPasswordSentDialog) }
        )
    }

    // ── Email Input dialog overlay ───────────────────────────────────────
    if (state.showEmailInputDialog) {
        EmailInputDialog(
            email = state.email,
            onEmailChange = { onEvent(ForgotPasswordEvent.EmailChanged(it)) },
            errorResId = state.emailErrorResId,
            isLoading = state.isLoading,
            onSend = { onEvent(ForgotPasswordEvent.SendResetLink) },
            onDismiss = { onEvent(ForgotPasswordEvent.DismissEmailInputDialog) }
        )
    }
}
