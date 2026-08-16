package iti.grad.nutriscan.presentation.scan.camera.view.components

import androidx.compose.animation.core.EaseInOutSine
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.scan.camera.state.ScanInputMode
import iti.grad.presentation.R

// The horizontal area the beam sweeps across (full screen width, limited vertical band)
private val ScanAreaHeight = 260.dp

@Composable
fun ScanFrameOverlay(
    selectedMode: ScanInputMode,
    modifier: Modifier = Modifier,
) {
    val frameDescription = stringResource(R.string.scan_frame_content_description)

    val durationMillis = when (selectedMode) {
        ScanInputMode.PHOTO   -> 1800
        ScanInputMode.GALLERY -> 2400
    }

    val primaryColor = AppTheme.colors.Primary  // #13A4AB
    val accentColor  = AppTheme.colors.Accent   // #47D3D9
    val teal300      = AppTheme.colors.Teal300  // #CAF2F4

    val density = LocalDensity.current

    val infiniteTransition = rememberInfiniteTransition(label = "scanBeam")

    // Beam moves top ↔ bottom with smooth ease
    val scanProgress by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = durationMillis, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scanProgress",
    )

    // Breathing glow — intensity pulses softly
    val glowPulse by infiniteTransition.animateFloat(
        initialValue  = 0.5f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )

    Box(
        modifier = modifier
            .semantics { contentDescription = frameDescription },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val areaHeightPx = with(density) { ScanAreaHeight.toPx() }

            val w = size.width
            val h = size.height

            // Scan band centered vertically, full screen width
            val top  = (h - areaHeightPx) / 2f

            // Current Y of the beam inside the band
            val lineY = top + areaHeightPx * scanProgress

            // ── 1. Wide vertical glow band around the beam ────────────────────
            val glowBandHeight = with(density) { 60.dp.toPx() }
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryColor.copy(alpha = 0.10f * glowPulse),
                        primaryColor.copy(alpha = 0.22f * glowPulse),
                        primaryColor.copy(alpha = 0.10f * glowPulse),
                        Color.Transparent,
                    ),
                    startY = lineY - glowBandHeight / 2f,
                    endY   = lineY + glowBandHeight / 2f,
                ),
                topLeft = Offset(0f, lineY - glowBandHeight / 2f),
                size    = Size(w, glowBandHeight),
            )

            // ── 2. Outer soft halo line (wide, feathered) ─────────────────────
            val outerStroke = with(density) { 8.dp.toPx() }
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        accentColor.copy(alpha = 0.18f * glowPulse),
                        primaryColor.copy(alpha = 0.35f * glowPulse),
                        accentColor.copy(alpha = 0.18f * glowPulse),
                        Color.Transparent,
                    ),
                    startX = 0f,
                    endX   = w,
                ),
                start       = Offset(0f, lineY),
                end         = Offset(w, lineY),
                strokeWidth = outerStroke,
                cap         = StrokeCap.Round,
            )

            // ── 3. Inner crisp laser beam ─────────────────────────────────────
            val beamStroke = with(density) { 2.dp.toPx() }
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        teal300.copy(alpha = 0.5f),
                        accentColor.copy(alpha = 0.85f * glowPulse),
                        primaryColor,
                        accentColor.copy(alpha = 0.85f * glowPulse),
                        teal300.copy(alpha = 0.5f),
                        Color.Transparent,
                    ),
                    startX = 0f,
                    endX   = w,
                ),
                start       = Offset(0f, lineY),
                end         = Offset(w, lineY),
                strokeWidth = beamStroke,
                cap         = StrokeCap.Round,
            )

        }
    }
}
