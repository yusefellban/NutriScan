package iti.grad.nutriscan.presentation.scan.camera.view.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import iti.grad.nutriscan.presentation.common.components.VerdictBadge
import iti.grad.nutriscan.presentation.common.components.customShadow
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.scan.camera.state.ActiveScanUiModel
import iti.grad.presentation.R

@Composable
fun ActiveScanCard(
    scan: ActiveScanUiModel,
    onBookmarkClick: () -> Unit,
    onCardClick: () -> Unit = {},
    onRetryClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(22.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .customShadow(
                shape = cardShape,
                color = AppTheme.colors.Primary.copy(alpha = 0.35f),
                blurRadius = 50f,
                offsetY = 8f,
            )
            .clip(cardShape)
            .background(AppTheme.colors.PrimaryVariant)
            .clickable(onClick = onCardClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScanThumbnail(thumbnailUrl = scan.thumbnailUrl, isProcessing = scan.isProcessing)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = scan.fullResult?.productName?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.scan_product_unknown),
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.OnPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            val verdict = scan.fullResult?.foodSafetyResponse?.verdict
            if (scan.isProcessing) {
                ProcessingBadge(statusResId = R.string.scan_status_processing)
            } else if (scan.isFailed) {
                HealthBadge(text = stringResource(R.string.scan_status_failed))
            } else if (scan.statusResId != null) {
                ProcessingBadge(statusResId = scan.statusResId)
            } else if (verdict != null) {
                VerdictBadge(verdict = verdict)
            } else if (scan.healthTagResId != null) {
                HealthBadge(text = stringResource(scan.healthTagResId))
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (scan.isFailed) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.colors.Error.copy(alpha = 0.3f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onRetryClick,
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Scan Again", // In a real app this would be in strings.xml
                    style = AppTheme.typography.labelMedium,
                    color = AppTheme.colors.Error,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (!scan.isProcessing && scan.fullResult != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppTheme.colors.VerdictGreen.copy(alpha = 0.3f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onRetryClick,
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Scan Again", // In a real app this would be in strings.xml
                        style = AppTheme.typography.labelMedium,
                        color = AppTheme.colors.VerdictGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppTheme.colors.Teal800)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onBookmarkClick,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(if (scan.isSaved) R.drawable.ic_bookmark_solid else R.drawable.ic_bookmark),
                        contentDescription = stringResource(R.string.scan_saved_content_description),
                        tint = AppTheme.colors.PrimaryVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanThumbnail(
    thumbnailUrl: String?,
    isProcessing: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    if (thumbnailUrl != null) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(56.dp)
                .clip(shape)
                .background(AppTheme.colors.Divider),
        )
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
        
        Box(
            modifier = modifier
                .alpha(if (isProcessing) alpha else 1f)
                .size(56.dp)
                .clip(shape)
                .background(AppTheme.colors.Divider),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_qr_scan),
                contentDescription = null,
                tint = AppTheme.colors.OnPrimary.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun ProcessingBadge(
    statusResId: Int,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "badgePulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgePulseAlpha"
    )

    Box(
        modifier = modifier
            .alpha(alpha)
            .clip(RoundedCornerShape(6.dp))
            .background(AppTheme.colors.VerdictYellow)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(statusResId),
            style = AppTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.TextPrimary,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun HealthBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AppTheme.colors.Teal800.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AppTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.Teal800,
            fontSize = 11.sp,
        )
    }
}
