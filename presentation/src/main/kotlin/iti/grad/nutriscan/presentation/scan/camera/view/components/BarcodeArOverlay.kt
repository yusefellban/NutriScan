package iti.grad.nutriscan.presentation.scan.camera.view.components

import android.graphics.RectF
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import kotlinx.coroutines.launch


/**
 * Full-screen AR overlay drawn directly on top of the camera preview in PHOTO mode.
 *
 * ## How coordinate mapping works
 * [normalizedBounds] carries the barcode's bounding box as normalised [0,1] values
 * (produced by [BarcodeScanAnalyzer]). Inside [BoxWithConstraints] we multiply by the
 * composable's pixel size to obtain display-pixel coordinates for the Canvas — keeping
 * the ViewModel entirely free of screen-dimension knowledge.
 *
 * ## Smooth tracking
 * Four [Animatable]<Float> instances (left/top/right/bottom) are independently animated
 * with a low-stiffness spring. This makes the overlay bracket elastically follow the
 * barcode as the camera or the product moves, instead of snapping abruptly.
 *
 * ## Appearance / disappearance
 * A separate [Animatable]<Float> alpha fades in (200 ms) when a barcode is first detected
 * and fades out when the barcode leaves the frame.
 *
 * ## Lock colour transition
 * When [isLocked] becomes true (submission in-flight), the corner brackets and glow border
 * smoothly animate from white → [AppColors.VerdictGreen] to give the user clear AR feedback
 * that the product was captured.
 *
 * ## Tappable barcode chip
 * The barcode digits pill is rendered on the Canvas, so a transparent [Spacer] is positioned
 * on top of it at runtime to capture tap gestures, which are forwarded via [onBarcodeChipClicked].
 *
 * @param normalizedBounds    Normalised [0,1] bounding box from the last ML Kit frame, or null.
 * @param barcodeValue        The raw barcode digits to display in the floating pill badge.
 * @param isLocked            True while the barcode submission is in-flight.
 * @param onBarcodeChipClicked Invoked when the user taps the barcode chip. Use to trigger POST /v1/scans/barcode.
 */
@Composable
fun BarcodeArOverlay(
    normalizedBounds: RectF?,
    barcodeValue: String?,
    isLocked: Boolean,
    onBarcodeChipClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // ── Spring-animated bounding box coordinates ──────────────────────────────────
    val animLeft   = remember { Animatable(0f) }
    val animTop    = remember { Animatable(0f) }
    val animRight  = remember { Animatable(0.5f) }
    val animBottom = remember { Animatable(0.5f) }

    // ── Alpha for the whole overlay ────────────────────────────────────────────────
    val alpha by animateFloatAsState(
        targetValue = if (normalizedBounds != null) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "barcodeOverlayAlpha",
    )

    // ── Colour transition: white tracking → green locked ──────────────────────────
    val trackingColor = AppTheme.colors.OnPrimary
    val lockedColor   = AppTheme.colors.VerdictGreen
    val overlayColor by animateColorAsState(
        targetValue = if (isLocked) lockedColor else trackingColor,
        animationSpec = tween(durationMillis = 350),
        label = "barcodeOverlayColor",
    )

    // Animate the four edges independently using a medium-low spring so the bracket
    // elastically tracks the barcode even when the camera or product moves quickly.
    LaunchedEffect(normalizedBounds) {
        if (normalizedBounds != null) {
            val springSpec = spring<Float>(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMediumLow,
            )
            launch { animLeft  .animateTo(normalizedBounds.left,   springSpec) }
            launch { animTop   .animateTo(normalizedBounds.top,    springSpec) }
            launch { animRight .animateTo(normalizedBounds.right,  springSpec) }
            launch { animBottom.animateTo(normalizedBounds.bottom, springSpec) }
        }
    }

    // Early exit — don't draw anything while fully transparent
    if (alpha == 0f) return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val canvasWidthPx  = with(density) { maxWidth.toPx() }
        val canvasHeightPx = with(density) { maxHeight.toPx() }

        Canvas(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            // Map normalised [0,1] coords → display pixels
            val left   = animLeft  .value * canvasWidthPx
            val top    = animTop   .value * canvasHeightPx
            val right  = animRight .value * canvasWidthPx
            val bottom = animBottom.value * canvasHeightPx

            val boxWidth  = right - left
            val boxHeight = bottom - top

            // ── 1. Dim everything outside the bounding box ────────────────────────
            drawDimOverlay(left, top, right, bottom, alpha)

            // ── 2. Glow border around the barcode ─────────────────────────────────
            drawGlowBorder(
                left, top, right, bottom,
                color = overlayColor,
                alpha = alpha,
            )

            // ── 3. Corner brackets (same visual language as ScanFrameOverlay) ─────
            drawCornerBrackets(
                left, top, right, bottom,
                color = overlayColor,
                alpha = alpha,
            )

            // ── 4. Barcode value pill badge below the bounding box ─────────────────
            if (!barcodeValue.isNullOrBlank()) {
                drawBarcodeBadge(
                    barcodeValue    = barcodeValue,
                    centerX         = left + boxWidth / 2f,
                    boxBottom       = bottom,
                    color           = overlayColor,
                    alpha           = alpha,
                    textMeasurer    = textMeasurer,
                )
            }
        }

        // ── Tappable area over the barcode chip ───────────────────────────────────
        // Since the badge is drawn on a Canvas we position a transparent clickable Spacer
        // exactly where the badge will render, measured in Dp so Compose can lay it out.
        if (!barcodeValue.isNullOrBlank() && alpha > 0f) {
            val left   = animLeft  .value * canvasWidthPx
            val bottom = animBottom.value * canvasHeightPx
            val right  = animRight .value * canvasWidthPx
            val boxWidth = right - left

            // Badge geometry in px (must match drawBarcodeBadge exactly)
            val badgePaddingH  = with(density) { 16.dp.toPx() }
            val badgePaddingV  = with(density) { 7.dp.toPx() }
            val badgeMarginTop = with(density) { 10.dp.toPx() }
            val textStyle = TextStyle(
                fontSize      = 13.sp,
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
            )
            val measured  = textMeasurer.measure(barcodeValue, textStyle)
            val textW     = measured.size.width.toFloat()
            val textH     = measured.size.height.toFloat()
            val badgeW    = textW + badgePaddingH * 2
            val badgeH    = textH + badgePaddingV * 2
            val badgeCenterX = left + boxWidth / 2f
            val badgeL    = badgeCenterX - badgeW / 2f
            val badgeT    = bottom + badgeMarginTop

            // Convert to Dp for Compose layout
            val badgeLDp = with(density) { badgeL.toDp() }
            val badgeTDp = with(density) { badgeT.toDp() }
            val badgeWDp = with(density) { badgeW.toDp() }
            val badgeHDp = with(density) { badgeH.toDp() }

            Spacer(
                modifier = Modifier
                    .absoluteOffset(x = badgeLDp, y = badgeTDp)
                    .width(badgeWDp)
                    .height(badgeHDp)
                    .clickable(
                        enabled = !isLocked,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBarcodeChipClicked,
                    ),
            )
        }
    }
}

// ── Private drawing helpers ────────────────────────────────────────────────────

/** Draws a subtle dark scrim outside the detected barcode area to focus attention. */
private fun DrawScope.drawDimOverlay(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    alpha: Float,
) {
    // Top strip
    drawRect(
        color  = Color.Black.copy(alpha = 0.35f * alpha),
        topLeft = Offset(0f, 0f),
        size   = androidx.compose.ui.geometry.Size(size.width, top),
    )
    // Bottom strip
    drawRect(
        color  = Color.Black.copy(alpha = 0.35f * alpha),
        topLeft = Offset(0f, bottom),
        size   = androidx.compose.ui.geometry.Size(size.width, size.height - bottom),
    )
    // Left strip (between top and bottom)
    drawRect(
        color  = Color.Black.copy(alpha = 0.35f * alpha),
        topLeft = Offset(0f, top),
        size   = androidx.compose.ui.geometry.Size(left, bottom - top),
    )
    // Right strip (between top and bottom)
    drawRect(
        color  = Color.Black.copy(alpha = 0.35f * alpha),
        topLeft = Offset(right, top),
        size   = androidx.compose.ui.geometry.Size(size.width - right, bottom - top),
    )
}

/** Draws a rounded-rect glow stroke around the barcode bounding box. */
private fun DrawScope.drawGlowBorder(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    color: Color,
    alpha: Float,
) {
    val cornerRadius = 12.dp.toPx()
    val glowStroke = 2.dp.toPx()
    // Outer glow — wide, very translucent
    drawRoundRect(
        color        = color.copy(alpha = 0.18f * alpha),
        topLeft      = Offset(left - glowStroke * 2, top - glowStroke * 2),
        size         = androidx.compose.ui.geometry.Size(
            right - left + glowStroke * 4,
            bottom - top + glowStroke * 4,
        ),
        cornerRadius = CornerRadius(cornerRadius + glowStroke * 2),
        style        = Stroke(width = glowStroke * 3),
    )
    // Inner crisp border
    drawRoundRect(
        color        = color.copy(alpha = 0.75f * alpha),
        topLeft      = Offset(left, top),
        size         = androidx.compose.ui.geometry.Size(right - left, bottom - top),
        cornerRadius = CornerRadius(cornerRadius),
        style        = Stroke(width = glowStroke),
    )
}

/** Draws corner bracket accents — matching the design language of [ScanFrameOverlay]. */
private fun DrawScope.drawCornerBrackets(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    color: Color,
    alpha: Float,
) {
    val bracketLength = minOf((right - left), (bottom - top)) * 0.25f
    val strokeWidth   = 3.5.dp.toPx()
    val cornerRadius  = 10.dp.toPx()

    val paint = Stroke(
        width    = strokeWidth,
        cap      = StrokeCap.Round,
        join     = StrokeJoin.Round,
    )
    val c = color.copy(alpha = alpha)

    val path = Path().apply {
        // ── Top-Left ──
        moveTo(left, top + bracketLength)
        arcTo(
            rect = Rect(left, top, left + 2 * cornerRadius, top + 2 * cornerRadius),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false,
        )
        lineTo(left + bracketLength, top)

        // ── Top-Right ──
        moveTo(right - bracketLength, top)
        arcTo(
            rect = Rect(right - 2 * cornerRadius, top, right, top + 2 * cornerRadius),
            startAngleDegrees = 270f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false,
        )
        lineTo(right, top + bracketLength)

        // ── Bottom-Right ──
        moveTo(right, bottom - bracketLength)
        arcTo(
            rect = Rect(right - 2 * cornerRadius, bottom - 2 * cornerRadius, right, bottom),
            startAngleDegrees = 0f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false,
        )
        lineTo(right - bracketLength, bottom)

        // ── Bottom-Left ──
        moveTo(left + bracketLength, bottom)
        arcTo(
            rect = Rect(left, bottom - 2 * cornerRadius, left + 2 * cornerRadius, bottom),
            startAngleDegrees = 90f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false,
        )
        lineTo(left, bottom - bracketLength)
    }
    drawPath(path, color = c, style = paint)
}

/** Draws a glass-morphism pill badge showing the raw barcode digits below the bounding box. */
private fun DrawScope.drawBarcodeBadge(
    barcodeValue: String,
    centerX: Float,
    boxBottom: Float,
    color: Color,
    alpha: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    val badgePaddingH = 16.dp.toPx()
    val badgePaddingV = 7.dp.toPx()
    val badgeMarginTop = 10.dp.toPx()
    val cornerRadius   = 20.dp.toPx()

    val textStyle = TextStyle(
        color      = color,
        fontSize   = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
    )

    val measured = textMeasurer.measure(barcodeValue, textStyle)
    val textW    = measured.size.width.toFloat()
    val textH    = measured.size.height.toFloat()

    val badgeW   = textW + badgePaddingH * 2
    val badgeH   = textH + badgePaddingV * 2
    val badgeL   = centerX - badgeW / 2f
    val badgeT   = boxBottom + badgeMarginTop

    // Badge background — dark glass
    drawRoundRect(
        color        = Color.Black.copy(alpha = 0.55f * alpha),
        topLeft      = Offset(badgeL, badgeT),
        size         = androidx.compose.ui.geometry.Size(badgeW, badgeH),
        cornerRadius = CornerRadius(cornerRadius),
    )
    // Badge border — matches overlay color
    drawRoundRect(
        color        = color.copy(alpha = 0.45f * alpha),
        topLeft      = Offset(badgeL, badgeT),
        size         = androidx.compose.ui.geometry.Size(badgeW, badgeH),
        cornerRadius = CornerRadius(cornerRadius),
        style        = Stroke(width = 1.dp.toPx()),
    )
    // Barcode text
    drawText(
        textMeasurer = textMeasurer,
        text         = barcodeValue,
        style        = textStyle.copy(color = color.copy(alpha = alpha)),
        topLeft      = Offset(badgeL + badgePaddingH, badgeT + badgePaddingV),
    )
}
