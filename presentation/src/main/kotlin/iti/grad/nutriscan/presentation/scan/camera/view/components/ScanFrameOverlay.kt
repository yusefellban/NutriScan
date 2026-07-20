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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

private val FrameSize = 260.dp
private val CornerLength = 40.dp
private val CornerStroke = 4.dp

@Composable
fun ScanFrameOverlay(
    modifier: Modifier = Modifier,
) {
    val frameDescription = stringResource(R.string.scan_frame_content_description)
    val cornerColor = AppTheme.colors.OnPrimary
    val scanLineColor = AppTheme.colors.OnPrimary.copy(alpha = 0.45f)
    val density = LocalDensity.current

    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scanLineProgress",
    )

    Box(
        modifier = modifier
            .size(FrameSize)
            .semantics { contentDescription = frameDescription },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cornerLengthPx = with(density) { CornerLength.toPx() }
            val strokeWidthPx = with(density) { CornerStroke.toPx() }
            val w = size.width
            val h = size.height

            drawLine(cornerColor, Offset(0f, 0f), Offset(cornerLengthPx, 0f), strokeWidthPx, StrokeCap.Round)
            drawLine(cornerColor, Offset(0f, 0f), Offset(0f, cornerLengthPx), strokeWidthPx, StrokeCap.Round)
            drawLine(cornerColor, Offset(w, 0f), Offset(w - cornerLengthPx, 0f), strokeWidthPx, StrokeCap.Round)
            drawLine(cornerColor, Offset(w, 0f), Offset(w, cornerLengthPx), strokeWidthPx, StrokeCap.Round)
            drawLine(cornerColor, Offset(0f, h), Offset(cornerLengthPx, h), strokeWidthPx, StrokeCap.Round)
            drawLine(cornerColor, Offset(0f, h), Offset(0f, h - cornerLengthPx), strokeWidthPx, StrokeCap.Round)
            drawLine(cornerColor, Offset(w, h), Offset(w - cornerLengthPx, h), strokeWidthPx, StrokeCap.Round)
            drawLine(cornerColor, Offset(w, h), Offset(w, h - cornerLengthPx), strokeWidthPx, StrokeCap.Round)

            val lineY = h * scanLineProgress
            drawLine(
                color = scanLineColor,
                start = Offset(16f, lineY),
                end = Offset(w - 16f, lineY),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
        }
    }
}
