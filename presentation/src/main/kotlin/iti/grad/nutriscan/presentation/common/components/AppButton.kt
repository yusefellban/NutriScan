package iti.grad.nutriscan.presentation.common.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.theme.LexendDeca

class PuffedShape(
    private val puffHeight: Dp = 5.dp,
    private val cornerRadius: Dp = 14.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val puffPx = with(density) { puffHeight.toPx() }
        val radiusPx = with(density) { cornerRadius.toPx() }

        val path = Path().apply {
            val insetY = puffPx
            val minX = 0f
            val maxX = size.width
            val minY = 0f
            val maxY = size.height
            val midX = size.width / 2f

            // Start at top left, after the corner
            moveTo(minX + radiusPx, minY + insetY)

            // Top bulging curve
            quadraticBezierTo(
                x1 = midX, y1 = minY,
                x2 = maxX - radiusPx, y2 = minY + insetY
            )

            // Top right corner arc
            arcTo(
                rect = Rect(
                    left = maxX - radiusPx * 2,
                    top = minY + insetY,
                    right = maxX,
                    bottom = minY + insetY + radiusPx * 2
                ),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Right edge
            lineTo(maxX, maxY - insetY - radiusPx)

            // Bottom right corner arc
            arcTo(
                rect = Rect(
                    left = maxX - radiusPx * 2,
                    top = maxY - insetY - radiusPx * 2,
                    right = maxX,
                    bottom = maxY - insetY
                ),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Bottom bulging curve
            quadraticBezierTo(
                x1 = midX, y1 = maxY,
                x2 = minX + radiusPx, y2 = maxY - insetY
            )

            // Bottom left corner arc
            arcTo(
                rect = Rect(
                    left = minX,
                    top = maxY - insetY - radiusPx * 2,
                    right = minX + radiusPx * 2,
                    bottom = maxY - insetY
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Left edge
            lineTo(minX, minY + insetY + radiusPx)

            // Top left corner arc
            arcTo(
                rect = Rect(
                    left = minX,
                    top = minY + insetY,
                    right = minX + radiusPx * 2,
                    bottom = minY + insetY + radiusPx * 2
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            close()
        }

        return Outline.Generic(path)
    }
}

/**
 * Overload that accepts a pre-resolved [text] string instead of a string resource ID.
 * Useful when the label is already resolved (e.g. passed in from a composable that already
 * called `stringResource`).
 */
@Composable
fun AppButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    outlined: Boolean = false,
) {
    val puffedShape = remember { PuffedShape(puffHeight = 5.dp, cornerRadius = 14.dp) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
    ) {
        if (!outlined) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 17.dp)
                    .height(7.dp)
                    .align(Alignment.BottomCenter)
                    .blur(15.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50)
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(puffedShape)
                .then(
                    if (outlined) {
                        Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary), puffedShape)
                    } else {
                        Modifier.background(MaterialTheme.colorScheme.primary)
                    }
                )
                .height(62.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            val contentColor = if (outlined) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
            if (isLoading) {
                CircularProgressIndicator(
                    color = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = text,
                    fontFamily = LexendDeca,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun AppButton(
    @StringRes textResId: Int,
    isLoading: Boolean,
    onClick: () -> Unit,
    outlined: Boolean = false,
) {
    val puffedShape = remember { PuffedShape(puffHeight = 5.dp, cornerRadius = 14.dp) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
    ) {
        // Glow ellipse beneath the button
        if (!outlined) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 17.dp)
                    .height(7.dp)
                    .align(Alignment.BottomCenter)
                    .blur(15.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50)
                    )
            )
        }

        // Puffed button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(puffedShape)
                .then(
                    if (outlined) {
                        Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary), puffedShape)
                    } else {
                        Modifier.background(MaterialTheme.colorScheme.primary)
                    }
                )
                .height(62.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            val contentColor = if (outlined) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
            if (isLoading) {
                CircularProgressIndicator(
                    color = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = stringResource(textResId),
                    fontFamily = LexendDeca,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = contentColor
                )
            }
        }
    }
}
