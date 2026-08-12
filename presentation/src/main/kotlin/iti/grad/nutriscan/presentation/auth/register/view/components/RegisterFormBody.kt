package iti.grad.nutriscan.presentation.auth.register.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.auth.register.state.RegisterEvent
import iti.grad.nutriscan.presentation.auth.register.state.RegisterState
import iti.grad.nutriscan.presentation.common.components.AppButton
import iti.grad.nutriscan.presentation.common.components.AuthBottomPrompt
import iti.grad.nutriscan.presentation.common.components.FigmaInputField
import androidx.compose.material3.MaterialTheme
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.LexendDeca
import iti.grad.nutriscan.presentation.common.theme.PlusJakartaSans
import iti.grad.presentation.R

// ─────────────────────────────────────────────────────────────────────────────
// Form body: fields + button + sign-in redirect
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RegisterFormBody(
    state: RegisterState,
    onEvent: (RegisterEvent) -> Unit
) {
    val inputContainerBg = MaterialTheme.colorScheme.surface
    val inputLabelColor  = MaterialTheme.colorScheme.onSurfaceVariant
    val inputTextColor   = MaterialTheme.colorScheme.onSurface
    val signInTextColor  = MaterialTheme.colorScheme.secondary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── First Name ─────────────────────────────────────────────────────
        FigmaInputField(
            value = state.firstName,
            onValueChange = { onEvent(RegisterEvent.FirstNameChanged(it)) },
            label = stringResource(R.string.first_name_label),
            hint = stringResource(R.string.first_name_hint),
            leadingIconRes = R.drawable.ic_gender_male,
            hasError = state.firstNameErrorResId != null,
            errorResId = state.firstNameErrorResId,
            inputContainerBg = inputContainerBg,
            inputLabelColor = inputLabelColor,
            inputTextColor = inputTextColor
        )

        // ── Last Name ──────────────────────────────────────────────────────
        FigmaInputField(
            value = state.lastName,
            onValueChange = { onEvent(RegisterEvent.LastNameChanged(it)) },
            label = stringResource(R.string.last_name_label),
            hint = stringResource(R.string.last_name_hint),
            leadingIconRes = R.drawable.ic_gender_male,
            hasError = state.lastNameErrorResId != null,
            errorResId = state.lastNameErrorResId,
            inputContainerBg = inputContainerBg,
            inputLabelColor = inputLabelColor,
            inputTextColor = inputTextColor
        )

        // ── Email ──────────────────────────────────────────────────────────
        FigmaInputField(
            value = state.email,
            onValueChange = { onEvent(RegisterEvent.EmailChanged(it)) },
            label = stringResource(R.string.email_label),
            hint = stringResource(R.string.email_hint),
            leadingIconRes = R.drawable.ic_gender_male,
            hasError = state.emailErrorResId != null,
            errorResId = state.emailErrorResId,
            inputContainerBg = inputContainerBg,
            inputLabelColor = inputLabelColor,
            inputTextColor = inputTextColor
        )

        // ── Password ───────────────────────────────────────────────────────
        FigmaInputField(
            value = state.password,
            onValueChange = { onEvent(RegisterEvent.PasswordChanged(it)) },
            label = stringResource(R.string.password_label),
            hint = stringResource(R.string.password_hint),
            leadingIconRes = R.drawable.ic_gender_male_1,
            isPassword = true,
            isPasswordVisible = state.passwordVisible,
            onVisibilityToggle = { onEvent(RegisterEvent.TogglePasswordVisibility) },
            hasError = state.passwordErrorResId != null,
            errorResId = state.passwordErrorResId,
            inputContainerBg = inputContainerBg,
            inputLabelColor = inputLabelColor,
            inputTextColor = inputTextColor
        )

        // ── Confirm Password ───────────────────────────────────────────────
        FigmaInputField(
            value = state.confirmPassword,
            onValueChange = { onEvent(RegisterEvent.ConfirmPasswordChanged(it)) },
            label = stringResource(R.string.confirm_password_label),
            hint = stringResource(R.string.password_hint),
            leadingIconRes = R.drawable.ic_gender_male_1,
            isPassword = true,
            isPasswordVisible = state.confirmPasswordVisible,
            onVisibilityToggle = { onEvent(RegisterEvent.ToggleConfirmPasswordVisibility) },
            hasError = state.confirmPasswordErrorResId != null,
            errorResId = state.confirmPasswordErrorResId,
            inputContainerBg = inputContainerBg,
            inputLabelColor = inputLabelColor,
            inputTextColor = inputTextColor
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // ── Sign Up Button ─────────────────────────────────────────────────────
    AppButton(
        textResId = R.string.action_sign_up,
        isLoading = state.isLoading,
        onClick = { onEvent(RegisterEvent.SignUpClicked) }
    )

    Spacer(modifier = Modifier.height(24.dp))



    // ── Already have an account? Sign In. ─────────────────────────────────
    AuthBottomPrompt(
        questionText = "Already have an account?",
        actionText = "Sign In.",
        textColor = signInTextColor,
        onClick = { onEvent(RegisterEvent.SignInClicked) }
    )
}
