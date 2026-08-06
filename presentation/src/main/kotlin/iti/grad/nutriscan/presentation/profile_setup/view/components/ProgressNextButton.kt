package iti.grad.nutriscan.presentation.profile_setup.view.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.util.directionalDrawable
import iti.grad.presentation.R

/**
 * Circular FAB with a progress arc ring around it.
 * The arc fills proportionally based on [currentPage] / [totalPages].
 * Page 0 → 25%, Page 1 → 50%, Page 2 → 75%, Page 3 → 100%.
 */
@Composable
fun ProgressNextButton(
    currentPage: Int,
    totalPages: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Progress fraction: (currentPage + 1) / totalPages
    val targetProgress = (currentPage + 1).toFloat() / totalPages.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 400),
        label = "progress_animation"
    )

    val trackColor = AppTheme.colors.ProgressTrackColor
    val progressColor = AppTheme.colors.Teal1000

    Box(
        modifier = modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background track ring
        Canvas(modifier = Modifier.size(80.dp)) {
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Progress arc
        Canvas(modifier = Modifier.size(80.dp)) {
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Inner FAB button
        FloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = AppTheme.colors.Teal1000,
            contentColor = AppTheme.colors.OnPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            ),
            modifier = Modifier
                .padding(8.dp)
                .size(56.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    spotColor = AppTheme.colors.Teal1000,
                    ambientColor = AppTheme.colors.Teal1000
                )
        ) {
            Icon(
                painter = painterResource(directionalDrawable(R.drawable.ic_arrow_right, R.drawable.ic_arrow_left)),
                contentDescription = stringResource(R.string.onboarding_next),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

