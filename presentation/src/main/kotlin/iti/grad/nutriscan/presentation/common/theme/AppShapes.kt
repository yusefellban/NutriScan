package iti.grad.nutriscan.presentation.common.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

@Immutable
data class AppShapes(
    val Small: CornerBasedShape = RoundedCornerShape(12.dp),
    val Medium: CornerBasedShape = RoundedCornerShape(14.dp),
    val Large: CornerBasedShape = RoundedCornerShape(24.dp),
    val ExtraLarge: CornerBasedShape = RoundedCornerShape(32.dp)
)
