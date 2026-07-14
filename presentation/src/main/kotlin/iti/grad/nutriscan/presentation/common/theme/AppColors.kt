package iti.grad.nutriscan.presentation.common.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// --- Base Colors ---
val Teal100 = Color(0xFFE8FAFA)
val Teal200 = Color(0xFFD4F1F2)
val Teal300 = Color(0xFFCAF2F4)
val Teal400 = Color(0xFFA3E9EC)
val Teal500 = Color(0xFF75DEE3)
val Teal600 = Color(0xFF47D3D9)
val Teal700 = Color(0xFF2FC5CC)
val Teal800 = Color(0xFF17B8BE)
val Teal900 = Color(0xFF15AEB4)
val Teal1000 = Color(0xFF13A4AB)
val Teal1200 = Color(0xFF11939A)
val Teal1300 = Color(0xFF108188)
val Teal1400 = Color(0xFF0B5F65)
val Teal1500 = Color(0xFF0A545A)
val Teal1600 = Color(0xFF0F474A)

val Gray100 = Color(0xFFF8F8F9)
val Gray200 = Color(0xFFF1F1F1)
val Gray300 = Color(0xFFE5E5E4)
val Gray400 = Color(0xFFD6D6D5)
val Gray500 = Color(0xFFC0C0C0)
val Gray600 = Color(0xFFA6A5A5)
val Gray700 = Color(0xFF898989)
val Gray800 = Color(0xFF777777)
val Gray900 = Color(0xFF717171)
val Gray1000 = Color(0xFF6A6A6A)
val Gray1200 = Color(0xFF5F5F5F)
val Gray1300 = Color(0xFF545454)
val Gray1400 = Color(0xFF3E3E3E)
val Gray1500 = Color(0xFF535051)
val Gray1600 = Color(0xFF393C3C)
val White = Color(0xFFFFFFFF)

// --- Domain-Specific App Colors ---
@Immutable
data class AppColors(
    // These colors do not map directly to Material 3 standard structural colors,
    // so they are kept in a custom class for domain-specific usages.
    val VerdictGreen: Color = Color(0xFF388E3C),
    val VerdictYellow: Color = Color(0xFFF9A825),
    val VerdictRed: Color = Color(0xFFD32F2F),
    val Warning: Color = Color(0xFFFF9500),        // SystemOrange
    val ErrorBackground: Color = Color(0xFFFFF1F3) // Hygieia Red/10
)
