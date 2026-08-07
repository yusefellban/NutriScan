package iti.grad.nutriscan.presentation.account_deletion.view

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iti.grad.nutriscan.presentation.account_deletion.state.AccountPendingDeletionEffect
import iti.grad.nutriscan.presentation.account_deletion.state.AccountPendingDeletionEvent
import iti.grad.nutriscan.presentation.account_deletion.state.AccountPendingDeletionState
import iti.grad.nutriscan.presentation.account_deletion.viewmodel.AccountPendingDeletionViewModel
import iti.grad.nutriscan.presentation.common.components.AppButton
import iti.grad.nutriscan.presentation.common.components.ErrorAlert
import iti.grad.nutriscan.presentation.common.components.HeroHeaderSubtitle
import iti.grad.nutriscan.presentation.common.components.HeroHeaderTitle
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AccountPendingDeletionScreen(
    scheduledDeletionAt: String,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AccountPendingDeletionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is AccountPendingDeletionEffect.NavigateToHome -> onNavigateToHome()
                is AccountPendingDeletionEffect.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    AccountPendingDeletionContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun AccountPendingDeletionContent(
    state: AccountPendingDeletionState,
    onEvent: (AccountPendingDeletionEvent) -> Unit,
) {
    val colors = AppTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.Background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        // ── Header gradient section ──────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AppTheme.colors.Teal1000,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                )
                .statusBarsPadding()
                .height(242.dp)
                .padding(start = 24.dp, end = 24.dp, top = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HeroHeaderTitle(text = stringResource(R.string.account_pending_deletion_header_title))
                HeroHeaderSubtitle(text = stringResource(R.string.account_pending_deletion_header_subtitle))
            }
        }

        Spacer(Modifier.height(32.dp))

        // ── Animated icon with concentric rings ─────────────────────────
        PulsatingRestoreIcon()

        Spacer(Modifier.height(32.dp))

        // ── Heading ────────────────────────────────────────────────────
        Text(
            text = stringResource(R.string.account_pending_deletion_heading),
            style = AppTheme.typography.headlineSmall,
            color = colors.TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(Modifier.height(12.dp))

        // ── Description ───────────────────────────────────────────────
        Text(
            text = stringResource(R.string.account_pending_deletion_description),
            style = AppTheme.typography.bodyMedium,
            color = colors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )

        Spacer(Modifier.height(28.dp))

        // ── Info card ─────────────────────────────────────────────────
        DeletionInfoCard(state = state)

        Spacer(Modifier.height(36.dp))

        // ── Restore CTA button ────────────────────────────────────────
        AppButton(
            text = if (state.isRestoring) {
                stringResource(R.string.account_pending_deletion_restoring)
            } else {
                stringResource(R.string.account_pending_deletion_restore_btn)
            },
            isLoading = state.isRestoring,
            onClick = { if (!state.isRestoring) onEvent(AccountPendingDeletionEvent.RestoreAccountClicked) },
        )

        Spacer(Modifier.height(24.dp))

        // ── Footer logout link ────────────────────────────────────────
        LogoutFooter(onLogout = { onEvent(AccountPendingDeletionEvent.LogoutClicked) })

        Spacer(Modifier.height(40.dp))
    }

    // ── Error dialog ─────────────────────────────────────────────────────
    state.error?.let { error ->
        ErrorAlert(
            message = error,
            onDismiss = { onEvent(AccountPendingDeletionEvent.ErrorDismissed) },
        )
    }
}

@Composable
private fun PulsatingRestoreIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val outerScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "outerRing",
    )
    val middleScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "middleRing",
    )

    Box(contentAlignment = Alignment.Center) {
        // Outer ring
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(outerScale)
                .clip(CircleShape)
                .background(AppTheme.colors.Teal1000.copy(alpha = 0.10f)),
        )
        // Middle ring
        Box(
            modifier = Modifier
                .size(110.dp)
                .scale(middleScale)
                .clip(CircleShape)
                .background(AppTheme.colors.Teal1000.copy(alpha = 0.18f)),
        )
        // Inner filled circle
        Box(
            modifier = Modifier
                .size(82.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.Teal1000),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Sync,
                contentDescription = null,
                tint = AppTheme.colors.OnPrimary,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

@Composable
private fun DeletionInfoCard(state: AccountPendingDeletionState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.SurfaceVariant)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        InfoRow(
            label = stringResource(R.string.account_pending_deletion_days_remaining_label),
            value = stringResource(R.string.account_pending_deletion_days_remaining_value, state.daysRemaining),
            valueColor = AppTheme.colors.Error,
        )
        InfoRow(
            label = stringResource(R.string.account_pending_deletion_scheduled_date_label),
            value = state.formattedDeletionDate,
            valueColor = AppTheme.colors.TextPrimary,
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = AppTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
    }
}

@Composable
private fun LogoutFooter(onLogout: () -> Unit) {
    val colors = AppTheme.colors
    val promptText = stringResource(R.string.account_pending_deletion_logout_prompt)
    val linkText = stringResource(R.string.account_pending_deletion_logout_link)
    val annotated = buildAnnotatedString {
        append(promptText)
        append(" ")
        withStyle(
            SpanStyle(
                color = colors.Teal1000,
                fontWeight = FontWeight.SemiBold,
            )
        ) {
            append(linkText)
        }
    }
    val promptLen = promptText.length + 1   // +1 for the space

    androidx.compose.foundation.text.ClickableText(
        text = annotated,
        style = AppTheme.typography.bodyMedium.copy(
            color = colors.TextSecondary,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
        ),
        onClick = { offset ->
            if (offset >= promptLen) onLogout()
        },
    )
}
