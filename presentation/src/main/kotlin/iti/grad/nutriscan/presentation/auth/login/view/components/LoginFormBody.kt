package iti.grad.nutriscan.presentation.auth.login.view.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.auth.login.state.LoginEvent
import iti.grad.nutriscan.presentation.auth.login.state.LoginState
import iti.grad.nutriscan.presentation.common.components.AppButton
import iti.grad.nutriscan.presentation.common.components.AuthBottomPrompt
import iti.grad.nutriscan.presentation.common.components.AuthDivider
import iti.grad.nutriscan.presentation.common.components.AuthInputField
import iti.grad.nutriscan.presentation.common.components.SocialLoginRow
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.PlusJakartaSans
import iti.grad.presentation.R

@Composable
fun LoginFormBody(
    state: LoginState,
    onEvent: (LoginEvent) -> Unit
) {
    val inputContainerBg = MaterialTheme.colorScheme.surface
    val inputLabelColor  = MaterialTheme.colorScheme.onSurfaceVariant
    val inputTextColor   = MaterialTheme.colorScheme.onSurface
    val dividerColor     = AppTheme.colors.AuthDividerColor
    val promptTextColor  = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Email ──────────────────────────────────────────────────────────
        AuthInputField(
            value = state.email,
            onValueChange = { onEvent(LoginEvent.EmailChanged(it)) },
            label = stringResource(R.string.email_label),
            hint = stringResource(R.string.email_hint),
            leadingIconRes = R.drawable.ic_email,
            hasError = state.emailErrorResId != null,
            errorResId = state.emailErrorResId,
            inputContainerBg = inputContainerBg,
            inputLabelColor = inputLabelColor,
            inputTextColor = inputTextColor,
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
        )

        // ── Password ───────────────────────────────────────────────────────
        AuthInputField(
            value = state.password,
            onValueChange = { onEvent(LoginEvent.PasswordChanged(it)) },
            label = stringResource(R.string.password_label),
            hint = stringResource(R.string.password_hint),
            leadingIconRes = R.drawable.ic_lock,
            isPassword = true,
            isPasswordVisible = state.passwordVisible,
            onVisibilityToggle = { onEvent(LoginEvent.TogglePasswordVisibility) },
            hasError = state.passwordErrorResId != null,
            errorResId = state.passwordErrorResId,
            inputContainerBg = inputContainerBg,
            inputLabelColor = inputLabelColor,
            inputTextColor = inputTextColor
        )

        // ── Forgot Password? ────────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = stringResource(R.string.forgot_password_title),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    onEvent(LoginEvent.ForgotPasswordClicked)
                }
            )
        }

        }

    Spacer(modifier = Modifier.height(28.dp))

        // ── Sign In Button ─────────────────────────────────────────────────
        AppButton(
            textResId = R.string.action_sign_in,
            isLoading = state.isLoading,
            onClick = { onEvent(LoginEvent.SignInClicked) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── OR divider ─────────────────────────────────────────────────────
        AuthDivider(
            textColor = promptTextColor,
            dividerColor = dividerColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Social icons ────────────────────────────────────────────────────
        SocialLoginRow(
            onSocialClick = { provider ->
                onEvent(LoginEvent.SocialLoginClicked(provider))
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Don't have an account? Sign Up. ─────────────────────────────────
        AuthBottomPrompt(
            questionText = "Don't have an account?",
            actionText = "Sign Up.",
            textColor = promptTextColor,
            onClick = { onEvent(LoginEvent.SignUpClicked) }
        )
}
