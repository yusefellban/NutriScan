package iti.grad.nutriscan.presentation.common.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val Teal100: Color = Color(0xFFE8FAFA),
    val Teal200: Color = Color(0xFFD4F1F2),
    val Teal300: Color = Color(0xFFCAF2F4),
    val Teal400: Color = Color(0xFFA3E9EC),
    val Teal500: Color = Color(0xFF75DEE3),
    val Teal600: Color = Color(0xFF47D3D9),
    val Teal700: Color = Color(0xFF2FC5CC),
    val Teal800: Color = Color(0xFF17B8BE),
    val Teal900: Color = Color(0xFF15AEB4),
    val Teal1000: Color = Color(0xFF13A4AB),
    val Teal1200: Color = Color(0xFF11939A),
    val Teal1300: Color = Color(0xFF108188),
    val Teal1400: Color = Color(0xFF0B5F65),
    val Teal1500: Color = Color(0xFF0A545A),
    val Teal1600: Color = Color(0xFF0F474A),

    val Gray100: Color = Color(0xFFF8F8F9),
    val Gray200: Color = Color(0xFFF1F1F1),
    val Gray300: Color = Color(0xFFE5E5E4),
    val Gray400: Color = Color(0xFFD6D6D5),
    val Gray500: Color = Color(0xFFC0C0C0),
    val Gray600: Color = Color(0xFFA6A5A5),
    val Gray700: Color = Color(0xFF898989),
    val Gray800: Color = Color(0xFF777777),
    val Gray900: Color = Color(0xFF717171),
    val Gray1000: Color = Color(0xFF6A6A6A),
    val Gray1200: Color = Color(0xFF5F5F5F),
    val Gray1300: Color = Color(0xFF545454),
    val Gray1400: Color = Color(0xFF3E3E3E),
    val Gray1500: Color = Color(0xFF535051),
    val Gray1600: Color = Color(0xFF393C3C),

    // --- Semantic App Colors ---
    val Primary: Color = Color(0xFF13A4AB),        // Teal1000
    val PrimaryVariant: Color = Color(0xFF0B5F65), // Teal1400
    val Accent: Color = Color(0xFF47D3D9),         // Teal600
    val Background: Color = Color(0xFFFFFFFF),     // White
    val Surface: Color = Color(0xFFFFFFFF),        // White
    val SurfaceVariant: Color = Color(0xFFF8F8F9), // Gray100
    val OnPrimary: Color = Color(0xFFFFFFFF),      // White
    val TextPrimary: Color = Color(0xFF393C3C),    // Gray1600
    val TextSecondary: Color = Color(0xFF777777),  // Gray800
    val Divider: Color = Color(0xFFE5E5E4),        // Gray300

    // --- Health Verdicts & Alerts ---
    val VerdictGreen: Color = Color(0xFF388E3C),
    val VerdictYellow: Color = Color(0xFFF9A825),
    val VerdictRed: Color = Color(0xFFD32F2F),
    val Warning: Color = Color(0xFFFF9500),        // SystemOrange
    val Error: Color = Color(0xFFFA4D5E),          // Hygieia Red/50
    val ErrorBackground: Color = Color(0xFFFFF1F3) // Hygieia Red/10
)
