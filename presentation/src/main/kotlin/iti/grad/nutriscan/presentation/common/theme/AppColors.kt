package iti.grad.nutriscan.presentation.common.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val Primary: Color = Color(0xFF1B5E20),
    val PrimaryVariant: Color = Color(0xFF2E7D32),
    val Accent: Color = Color(0xFF00C853),
    val Surface: Color = Color(0xFFFFFFFF),
    val SurfaceVariant: Color = Color(0xFFF1F8E9),
    val Background: Color = Color(0xFFFAFAFA),
    val OnPrimary: Color = Color(0xFFFFFFFF),
    val TextPrimary: Color = Color(0xFF1C1C1E),
    val TextSecondary: Color = Color(0xFF757575),
    val VerdictGreen: Color = Color(0xFF388E3C),
    val VerdictYellow: Color = Color(0xFFF9A825),
    val VerdictRed: Color = Color(0xFFD32F2F),
    val Warning: Color = Color(0xFFFF6F00),
    val Error: Color = Color(0xFFB71C1C),
    val Divider: Color = Color(0xFFE0E0E0)
)
