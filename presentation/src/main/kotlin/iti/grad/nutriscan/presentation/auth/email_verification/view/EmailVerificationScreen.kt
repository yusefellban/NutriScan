package iti.grad.nutriscan.presentation.auth.email_verification.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iti.grad.nutriscan.presentation.auth.email_verification.state.EmailVerificationEffect
import iti.grad.nutriscan.presentation.auth.email_verification.state.EmailVerificationEvent
import iti.grad.nutriscan.presentation.auth.email_verification.viewmodel.EmailVerificationViewModel
import iti.grad.nutriscan.presentation.common.components.AppButton
import iti.grad.nutriscan.presentation.common.components.ErrorAlert
import iti.grad.nutriscan.presentation.common.components.InternetAlert
import iti.grad.nutriscan.presentation.common.components.SuccessAlert
import iti.grad.nutriscan.presentation.common.components.WarningAlert
import iti.grad.nutriscan.presentation.common.state.AuthAlertState
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.LexendDeca
import iti.grad.nutriscan.presentation.common.theme.PlusJakartaSans
import iti.grad.presentation.R

@Composable
fun EmailVerificationScreen(
    viewModel: EmailVerificationViewModel = hiltViewModel(),
    onNavigateToSignIn: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is EmailVerificationEffect.NavigateToSignIn -> onNavigateToSignIn()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
            ) {
            // ── Teal Header ──────────────────────────────────────────────────
            EmailVerificationHeader(onNavigateBack = onNavigateBack)

            Spacer(modifier = Modifier.height(48.dp))

            // ── Email Icon ───────────────────────────────────────────────────
            EmailVerificationIcon()

            Spacer(modifier = Modifier.height(32.dp))

            // ── "A verification link was sent to:" ──────────────────────────
            Text(
                text = stringResource(R.string.email_verification_sent_to),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = AppTheme.colors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Email address (bold) ─────────────────────────────────────────
            Text(
                text = state.email,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = AppTheme.colors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Instructions ─────────────────────────────────────────────────
            Text(
                text = stringResource(R.string.email_verification_instructions),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = AppTheme.colors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Go to Sign In Button ─────────────────────────────────────────
            AppButton(
                textResId = R.string.email_verification_go_to_sign_in,
                isLoading = false,
                onClick = { viewModel.onEvent(EmailVerificationEvent.GoToSignInClicked) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Resend Verification Email Link ───────────────────────────────
            ResendVerificationLink(
                isResending = state.isResending,
                onClick = { viewModel.onEvent(EmailVerificationEvent.ResendEmailClicked) }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        when (val alert = state.alertState) {
            is AuthAlertState.InternetError -> {
                InternetAlert(
                    onRetry = { viewModel.onEvent(EmailVerificationEvent.ResendEmailClicked) },
                    onDismiss = { viewModel.onEvent(EmailVerificationEvent.DismissAlert) }
                )
            }
            is AuthAlertState.Error -> {
                ErrorAlert(
                    title = stringResource(id = R.string.alert_verification_failed_title),
                    message = alert.message.asString(),
                    onDismiss = { viewModel.onEvent(EmailVerificationEvent.DismissAlert) }
                )
            }
            is AuthAlertState.Warning -> {
                WarningAlert(
                    title = stringResource(id = R.string.alert_verification_failed_title),
                    message = alert.message.asString(),
                    onDismiss = { viewModel.onEvent(EmailVerificationEvent.DismissAlert) }
                )
            }
            is AuthAlertState.Success -> {
                SuccessAlert(
                    title = stringResource(id = R.string.alert_success_title),
                    message = alert.message.asString(),
                    onDismiss = { viewModel.onEvent(EmailVerificationEvent.DismissAlert) }
                )
            }
            is AuthAlertState.None -> Unit
        }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header: teal panel with back button, title, and subtitle
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EmailVerificationHeader(onNavigateBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        // Glow ellipse behind the header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(29.dp)
                .align(Alignment.BottomCenter)
                .blur(15.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(MaterialTheme.colorScheme.primary)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .padding(top = 48.dp, bottom = 32.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Back button
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = stringResource(R.string.email_verification_title),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 35.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Subtitle
                Text(
                    text = stringResource(R.string.email_verification_subtitle),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = AppTheme.colors.Teal200,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Centered email icon with circular teal background rings
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EmailVerificationIcon() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.Teal400.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            // Middle ring
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.Teal500.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                // Inner circle with icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MarkEmailRead,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// "Didn't receive it? Resend Verification Email." clickable text
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ResendVerificationLink(
    isResending: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isResending) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
        } else {
            val annotatedString = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = AppTheme.colors.TextSecondary,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp
                    )
                ) {
                    append(stringResource(R.string.email_verification_didnt_receive))
                    append(" ")
                }
                withStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(stringResource(R.string.email_verification_resend))
                }
            }

            Text(
                text = annotatedString,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
            )
        }
    }
}
