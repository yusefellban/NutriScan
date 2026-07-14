package iti.grad.nutriscan.presentation.auth.register.components

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
import iti.grad.nutriscan.presentation.auth.register.RegisterEvent
import iti.grad.nutriscan.presentation.auth.register.RegisterState
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
    onEvent: (RegisterEvent) -> Unit,
    isDark: Boolean
) {
    val inputContainerBg = if (isDark) AppTheme.colors.Teal1400 else AppTheme.colors.Surface
    val inputLabelColor  = if (isDark) AppTheme.colors.Teal600  else AppTheme.colors.Teal1000
    val inputTextColor   = if (isDark) AppTheme.colors.Teal400  else AppTheme.colors.Teal1000
    val signInTextColor  = if (isDark) AppTheme.colors.Teal500  else AppTheme.colors.Gray1000

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
            inputTextColor = inputTextColor,
            isDark = isDark
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
            inputTextColor = inputTextColor,
            isDark = isDark
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
            inputTextColor = inputTextColor,
            isDark = isDark
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // ── Sign Up Button ─────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Glow ellipse beneath the button (Figma: blur 15px teal ellipse)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 17.dp)
                .height(7.dp)
                .align(Alignment.BottomCenter)
                .blur(15.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(
                    color = AppTheme.colors.Teal1000,
                    shape = RoundedCornerShape(50)
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(AppTheme.colors.Teal1000)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onEvent(RegisterEvent.SignUpClicked) },
            contentAlignment = Alignment.Center
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    color = AppTheme.colors.OnPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = stringResource(R.string.action_sign_up),
                    fontFamily = LexendDeca,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 19.sp,
                    lineHeight = 24.sp,
                    color = AppTheme.colors.OnPrimary
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // ── Already have an account? Sign In. ─────────────────────────────────
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        letterSpacing = (-0.14).sp,
                        color = signInTextColor
                    )
                ) { append("Already have an account? ") }
                withStyle(
                    SpanStyle(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        letterSpacing = (-0.14).sp,
                        color = signInTextColor,
                        textDecoration = TextDecoration.Underline
                    )
                ) { append("Sign In.") }
            },
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onEvent(RegisterEvent.SignInClicked) }
        )
    }
}
