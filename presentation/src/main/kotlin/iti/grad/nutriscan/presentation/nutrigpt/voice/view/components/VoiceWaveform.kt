package iti.grad.nutriscan.presentation.nutrigpt.voice.view.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import kotlin.math.sin

@Composable
fun VoiceWaveform(
    modifier: Modifier = Modifier,
    isAnimating: Boolean = false,
    isRtl: Boolean = false,
    color: Color = AppTheme.colors.Primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_transition")
    
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )
    
    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase3"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        
        val amplitude = if (isAnimating) height / 3f else height / 10f
        
        fun drawWave(phase: Float, freqMultiplier: Float, alpha: Float, strokeWidth: Float) {
            val path = Path()
            path.moveTo(0f, centerY)
            
            for (i in 0 until width.toInt() step 5) {
                val x = i.toFloat()
                // Dampen the ends so it looks like a bounded wave
                val progress = x / width
                val dampening = sin(progress * Math.PI).toFloat()
                
                val currentPhase = if (isRtl) phase else -phase
                val y = centerY + sin(x * 0.02f * freqMultiplier + currentPhase) * amplitude * dampening
                path.lineTo(x, y)
            }
            
            drawPath(
                path = path,
                color = color.copy(alpha = alpha),
                style = Stroke(width = strokeWidth)
            )
        }
        
        drawWave(phase1, 1f, 1f, 4f)
        drawWave(phase2 + 1f, 1.2f, 0.6f, 3f)
        drawWave(phase3 + 2f, 0.8f, 0.3f, 2f)
    }
}
