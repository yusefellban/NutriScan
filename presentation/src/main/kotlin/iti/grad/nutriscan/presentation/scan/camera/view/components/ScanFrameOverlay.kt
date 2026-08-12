package iti.grad.nutriscan.presentation.scan.camera.view.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.scan.camera.state.ScanInputMode
import iti.grad.presentation.R
import androidx.compose.ui.graphics.drawscope.Stroke

private val FrameSize = 260.dp
private val CornerLength = 40.dp
private val CornerStroke = 4.dp

@Composable
fun ScanFrameOverlay(
    selectedMode: ScanInputMode,
    modifier: Modifier = Modifier,
) {
    val frameDescription = stringResource(R.string.scan_frame_content_description)
    val cornerAlpha = when (selectedMode) {
        ScanInputMode.BARCODE -> 1f
        ScanInputMode.PHOTO   -> 0.9f
        ScanInputMode.GALLERY -> 0.6f
    }
    val lineAlpha = when (selectedMode) {
        ScanInputMode.BARCODE -> 0.66f
        ScanInputMode.PHOTO   -> 0.5f
        ScanInputMode.GALLERY -> 0.24f
    }
    val durationMillis = when (selectedMode) {
        ScanInputMode.BARCODE -> 1200
        ScanInputMode.PHOTO   -> 2100
        ScanInputMode.GALLERY -> 2800
    }
    val cornerColor = AppTheme.colors.OnPrimary.copy(alpha = cornerAlpha)
    val scanLineColor = AppTheme.colors.OnPrimary.copy(alpha = lineAlpha)
    val density = LocalDensity.current

    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scanLineProgress",
    )

    Box(
        modifier = modifier
            .semantics { contentDescription = frameDescription },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val frameSizePx = with(density) { FrameSize.toPx() }
            val cornerLengthPx = with(density) { CornerLength.toPx() }
            val strokeWidthPx = with(density) { CornerStroke.toPx() }
            
            val w = size.width
            val h = size.height
            
            val left = (w - frameSizePx) / 2f
            val top = (h - frameSizePx) / 2f
            val right = left + frameSizePx
            val bottom = top + frameSizePx
            val cornerRadius = 35.dp.toPx()
            
            val overlayPath = Path().apply {
                addRect(Rect(0f, 0f, w, h))
                addRoundRect(
                    RoundRect(
                        left = left,
                        top = top,
                        right = right,
                        bottom = bottom,
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )
                )
                fillType = PathFillType.EvenOdd
            }
            
            drawPath(
                path = overlayPath,
                color = Color.Black.copy(alpha = 0.6f)
            )

            val cornersPath = Path().apply {
                // Top-Left
                moveTo(left, top + cornerLengthPx)
                arcTo(
                    rect = Rect(left, top, left + 2 * cornerRadius, top + 2 * cornerRadius),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(left + cornerLengthPx, top)

                // Top-Right
                moveTo(right - cornerLengthPx, top)
                arcTo(
                    rect = Rect(right - 2 * cornerRadius, top, right, top + 2 * cornerRadius),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(right, top + cornerLengthPx)

                // Bottom-Right
                moveTo(right, bottom - cornerLengthPx)
                arcTo(
                    rect = Rect(right - 2 * cornerRadius, bottom - 2 * cornerRadius, right, bottom),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(right - cornerLengthPx, bottom)

                // Bottom-Left
                moveTo(left + cornerLengthPx, bottom)
                arcTo(
                    rect = Rect(left, bottom - 2 * cornerRadius, left + 2 * cornerRadius, bottom),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(left, bottom - cornerLengthPx)
            }

            drawPath(
                path = cornersPath,
                color = cornerColor,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            val lineY = top + (frameSizePx * scanLineProgress)
            drawLine(
                color = scanLineColor,
                start = Offset(left + 16f, lineY),
                end = Offset(right - 16f, lineY),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
        }
    }
}
