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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import iti.grad.nutriscan.presentation.common.components.VerdictBadge
import iti.grad.nutriscan.presentation.common.components.customShadow
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.scan.camera.state.ActiveScanUiModel
import iti.grad.nutriscan.domain.scan.model.FamilyAlert
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.presentation.R

@Composable
fun ActiveScanCard(
    scan: ActiveScanUiModel,
    onBookmarkClick: () -> Unit,
    onCardClick: () -> Unit = {},
    onRetryClick: () -> Unit = {},
    bottomSafeInset: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    // Sheet look: flush to the screen edges, rounded only at the top — like a bottom
    // sheet resting under the camera preview, instead of a floating margined card.
    val sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f)
            .customShadow(
                shape = sheetShape,
                color = AppTheme.colors.Primary.copy(alpha = 0.35f),
                blurRadius = 50f,
                offsetY = 8f,
            )
            .clip(sheetShape)
            .background(AppTheme.colors.PrimaryVariant)
            .verticalScroll(rememberScrollState())
            .clickable(onClick = onCardClick),
    ) {
        // Drag handle, drawn inside the sheet itself (standard bottom-sheet affordance)
        // instead of as a separate bar floating below the card.
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 10.dp, bottom = 2.dp)
                .width(40.dp)
                .height(4.dp)
                .background(
                    color = AppTheme.colors.OnPrimary.copy(alpha = 0.32f),
                    shape = RoundedCornerShape(50),
                ),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
            val familyAlerts = scan.fullResult?.foodSafetyResponse?.familyAlerts ?: emptyList()
            if (scan.isProcessing) {
                ProcessingBadge(statusResId = R.string.scan_status_processing)
            } else if (scan.isFailed) {
                HealthBadge(text = stringResource(R.string.scan_status_failed))
            } else if (scan.statusResId != null) {
                ProcessingBadge(statusResId = scan.statusResId)
            } else if (verdict != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VerdictBadge(verdict = verdict)
                    if (familyAlerts.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            familyAlerts.forEach { alert ->
                                FamilyAlertBadge(alert = alert)
                            }
                        }
                    }
                }
            } else if (scan.healthTagResId != null) {
                HealthBadge(text = stringResource(scan.healthTagResId))
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Action buttons: bookmark only makes sense once we have a result to save, but
        // retry is offered any time the card isn't actively processing — including the
        // failed state, which previously had no way to try again.
        if (!scan.isProcessing) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!scan.isFailed && scan.fullResult != null) {
                    CardActionButton(
                        icon = painterResource(if (scan.isSaved) R.drawable.ic_bookmark_solid else R.drawable.ic_bookmark),
                        contentDescription = stringResource(R.string.scan_saved_content_description),
                        onClick = onBookmarkClick,
                    )
                }

                CardActionButton(
                    icon = rememberVectorPainter(Icons.Default.Refresh),
                    contentDescription = stringResource(R.string.scan_retry_content_description),
                    onClick = onRetryClick,
                )
            }
        }
        }

        // Extra empty space at the bottom of the sheet, sized to the real bottom safe-area
        // inset (nav bar height) instead of a guessed fixed number — since the sheet now
        // reaches the screen's bottom edge, this keeps it sitting "behind" the nav bar /
        // center capture button (drawn on top, further down the z-order) exactly like Figma,
        // instead of overlapping the product row above.
        Spacer(modifier = Modifier.height(bottomSafeInset))
    }
}

@Composable
private fun CardActionButton(
    icon: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppTheme.colors.Teal800)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription,
            tint = AppTheme.colors.PrimaryVariant,
            modifier = Modifier.size(22.dp),
        )
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

@Composable
private fun FamilyAlertBadge(alert: FamilyAlert, modifier: Modifier = Modifier) {
    val backgroundColor = when (alert.severity) {
        ProductVerdict.SAFE -> AppTheme.colors.Teal1000
        ProductVerdict.CAUTION -> AppTheme.colors.VerdictCautionBackground
        ProductVerdict.UNSAFE -> AppTheme.colors.VerdictUnsafeBackground
    }
    
    val verdictTextRes = when (alert.severity) {
        ProductVerdict.SAFE -> R.string.verdict_safe
        ProductVerdict.CAUTION -> R.string.verdict_caution
        ProductVerdict.UNSAFE -> R.string.verdict_unsafe
    }
    
    val verdictText = stringResource(verdictTextRes)
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = stringResource(R.string.scan_family_alert_badge, verdictText, alert.targetProfile),
            style = AppTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color.White,
        )
    }
}