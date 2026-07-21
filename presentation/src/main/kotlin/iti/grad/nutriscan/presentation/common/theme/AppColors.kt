package iti.grad.nutriscan.presentation.common.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    // --- Teal Palette ---
    val Teal100: Color,
    val Teal200: Color,
    val Teal300: Color,
    val Teal400: Color,
    val Teal500: Color,
    val Teal600: Color,
    val Teal700: Color,
    val Teal800: Color,
    val Teal900: Color,
    val Teal1000: Color,
    val Teal1200: Color,
    val Teal1300: Color,
    val Teal1400: Color,
    val Teal1500: Color,
    val Teal1600: Color,

    // --- Gray Palette ---
    val Gray100: Color,
    val Gray200: Color,
    val Gray300: Color,
    val Gray400: Color,
    val Gray500: Color,
    val Gray600: Color,
    val Gray700: Color,
    val Gray800: Color,
    val Gray900: Color,
    val Gray1000: Color,
    val Gray1200: Color,
    val Gray1300: Color,
    val Gray1400: Color,
    val Gray1500: Color,
    val Gray1600: Color,

    // --- Semantic App Colors ---
    val Primary: Color,
    val PrimaryVariant: Color,
    val Accent: Color,
    val Background: Color,
    val Surface: Color,
    val SurfaceVariant: Color,
    val OnPrimary: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val Divider: Color,

    // --- Health Verdicts & Alerts ---
    val VerdictGreen: Color,
    val VerdictYellow: Color,
    val VerdictRed: Color,
    val Warning: Color,
    val Error: Color,
    val ErrorBackground: Color,

    // --- Social Media Buttons ---
    val SocialButtonBorder: Color,
    val SocialButtonIconTint: Color,

    // --- Auth Divider ---
    val AuthDividerColor: Color,

    // --- Splash specific ---
    /** The starting background color for the Compose splash. Matches the Starting Window exactly. */
    val SplashBackground: Color,
    val SplashBackgroundEnd: Color,

    // --- Specific Overrides ---
    val BottomNavBarBackground: Color,
    /** Floating Scan-button background. Matches the iOS `FloatingTabButton` fill exactly. */
    val ScanButtonBackground: Color,
    /** Floating Scan-button icon tint. Matches the iOS `FloatingTabButton` icon color exactly. */
    val ScanButtonIconTint: Color,
    val VerdictRedBackground: Color,
    val VerdictRedText: Color,
    val HealthSubtitleColor: Color,
    val GreetingTitleColor: Color,
    val WaterDropIconTint: Color,
    val HistoryItemTitleColor: Color,
    val VerdictGreenBadgeColor: Color,
    val VerdictCyanBadgeColor: Color,
    val VerdictRedWarningIconTint: Color,
    val HistoryItemDateColor: Color,
    // --- Forgot Password / Auth ---
    val MethodCardBackground: Color,
    val MethodCardIconBgUnselected: Color,
    val MethodCardIconTintUnselected: Color,
    val AuthDialogBackground: Color,
    val AuthDialogSubtitle: Color,
)

/** Light theme color palette. All teal values kept identical to original. */
fun lightColors() = AppColors(
    // Teal palette
    Teal100 = Color(0xFFE8FAFA),
    Teal200 = Color(0xFFD4F1F2),
    Teal300 = Color(0xFFCAF2F4),
    Teal400 = Color(0xFFA3E9EC),
    Teal500 = Color(0xFF75DEE3),
    Teal600 = Color(0xFF47D3D9),
    Teal700 = Color(0xFF2FC5CC),
    Teal800 = Color(0xFF17B8BE),
    Teal900 = Color(0xFF15AEB4),
    Teal1000 = Color(0xFF13A4AB),
    Teal1200 = Color(0xFF11939A),
    Teal1300 = Color(0xFF108188),
    Teal1400 = Color(0xFF0B5F65),
    Teal1500 = Color(0xFF0A545A),
    Teal1600 = Color(0xFF0F474A),
    // Gray palette
    Gray100 = Color(0xFFF8F8F9),
    Gray200 = Color(0xFFF1F1F1),
    Gray300 = Color(0xFFE5E5E4),
    Gray400 = Color(0xFFD6D6D5),
    Gray500 = Color(0xFFC0C0C0),
    Gray600 = Color(0xFFA6A5A5),
    Gray700 = Color(0xFF898989),
    Gray800 = Color(0xFF777777),
    Gray900 = Color(0xFF717171),
    Gray1000 = Color(0xFF6A6A6A),
    Gray1200 = Color(0xFF5F5F5F),
    Gray1300 = Color(0xFF545454),
    Gray1400 = Color(0xFF3E3E3E),
    Gray1500 = Color(0xFF535051),
    Gray1600 = Color(0xFF393C3C),
    // Semantic
    Primary = Color(0xFF13A4AB),
    PrimaryVariant = Color(0xFF0B5F65),
    Accent = Color(0xFF47D3D9),
    Background = Color(0xFFFFFFFF),
    Surface = Color(0xFFFFFFFF),
    SurfaceVariant = Color(0xFFF8F8F9),
    OnPrimary = Color(0xFFFFFFFF),
    TextPrimary = Color(0xFF393C3C),
    TextSecondary = Color(0xFF777777),
    Divider = Color(0xFFE5E5E4),
    // Verdicts
    VerdictGreen = Color(0xFF388E3C),
    VerdictYellow = Color(0xFFF9A825),
    VerdictRed = Color(0xFFD32F2F),
    Warning = Color(0xFFFF9500),
    Error = Color(0xFFFA4D5E),
    ErrorBackground = Color(0xFFFFF1F3),
    // Social Buttons
    SocialButtonBorder = Color(0xFF777777),   // Gray800
    SocialButtonIconTint = Color(0xFF0F474A), // Teal1600
    // Auth Divider
    AuthDividerColor = Color(0xFF777777),     // Gray800
    // Splash
    SplashBackground = Color(0xFF13A4AB),     // Teal1000 — must match windowSplashScreenBackground
    SplashBackgroundEnd = Color(0xFFFFFFFF),  // White — final background after animation
    BottomNavBarBackground = Color(0xFF17B8BE), // Teal800
    ScanButtonBackground = Color(0xFF0B5F65), // Teal1400
    ScanButtonIconTint = Color(0xFFE8FAFA), // Teal100
    VerdictRedBackground = Color(0xFFD32F2F).copy(alpha = 0.12f),
    VerdictRedText = Color(0xFFD32F2F),
    HealthSubtitleColor = Color(0xFF0B5F65), // PrimaryVariant
    GreetingTitleColor = Color(0xFF17B8BE), // Teal800
    WaterDropIconTint = Color(0xFF0B5F65), // PrimaryVariant
    HistoryItemTitleColor = Color(0xFF0B5F65), // PrimaryVariant
    VerdictGreenBadgeColor = Color(0xFF13A4AB), // Primary
    VerdictCyanBadgeColor = Color(0xFF13A4AB), // Primary
    VerdictRedWarningIconTint = Color(0xFFF9A825), // VerdictYellow
    HistoryItemDateColor = Color(0xFF898989), // Gray700
    // Forgot Password / Auth
    MethodCardBackground = Color(0xFFFFFFFF), // White
    MethodCardIconBgUnselected = Color(0xFFE8FAFA), // Teal100
    MethodCardIconTintUnselected = Color(0xFF6A6A6A), // Gray1000
    AuthDialogBackground = Color(0xFFFFFFFF), // Surface
    AuthDialogSubtitle = Color(0xFF777777), // Gray800
)

/** Dark theme color palette. Provides an appropriately dark set of semantic colors. */
fun darkColors() = AppColors(
    // Teal palette — customized to invert hardcoded component usage in dark mode
    Teal100 = Color(0xFFE8FAFA),
    Teal200 = Color(0xFF2FC5CC), // Mapped to Teal700 to provide glowing shadows/badges
    Teal300 = Color(0xFFCAF2F4),
    Teal400 = Color(0xFFA3E9EC),
    Teal500 = Color(0xFF75DEE3),
    Teal600 = Color(0xFF47D3D9),
    Teal700 = Color(0xFF2FC5CC),
    Teal800 = Color(0xFFE8FAFA), // Mapped to Teal100 for primary headers
    Teal900 = Color(0xFF15AEB4),
    Teal1000 = Color(0xFF13A4AB),
    Teal1200 = Color(0xFFA3E9EC), // Mapped to Teal400 for secondary text
    Teal1300 = Color(0xFF108188),
    Teal1400 = Color(0xFF0B5F65),
    Teal1500 = Color(0xFF0A545A),
    Teal1600 = Color(0xFF0F474A),
    // Gray palette — same raw values; semantic usage inverted below
    Gray100 = Color(0xFFF8F8F9),
    Gray200 = Color(0xFFF1F1F1),
    Gray300 = Color(0xFFE5E5E4),
    Gray400 = Color(0xFFD6D6D5),
    Gray500 = Color(0xFFC0C0C0),
    Gray600 = Color(0xFFA6A5A5),
    Gray700 = Color(0xFF898989),
    Gray800 = Color(0xFF777777),
    Gray900 = Color(0xFF717171),
    Gray1000 = Color(0xFF6A6A6A),
    Gray1200 = Color(0xFF5F5F5F),
    Gray1300 = Color(0xFF545454),
    Gray1400 = Color(0xFF3E3E3E),
    Gray1500 = Color(0xFF535051),
    Gray1600 = Color(0xFF393C3C),
    // Semantic — inverted for dark
    Primary = Color(0xFF0B5F65),            // Teal1400 — for Nav bar and cards
    PrimaryVariant = Color(0xFF2FC5CC),     // Teal700 — for Scan FAB and accented text
    Accent = Color(0xFF75DEE3),             // Teal500
    Background = Color(0xFF0F474A),         // Teal1600 — background
    Surface = Color(0xFF0B5F65),            // Teal1400 — surface
    SurfaceVariant = Color(0xFF0A545A),     // Teal1500 — surface variant
    OnPrimary = Color(0xFFA3E9EC),          // Teal400 — light icons
    TextPrimary = Color(0xFFE8FAFA),        // Teal100 — very light text
    TextSecondary = Color(0xFFA6A5A5),      // Gray600
    Divider = Color(0xFF0F474A),
    // Verdicts — slightly lighter for dark backgrounds
    VerdictGreen = Color(0xFF81C784),
    VerdictYellow = Color(0xFFFFCA28),
    VerdictRed = Color(0xFFFF80AB),
    Warning = Color(0xFFFFAD33),
    Error = Color(0xFFFF6B7A),
    ErrorBackground = Color(0xFF2A1215),
    // Social Buttons
    SocialButtonBorder = Color(0xFF75DEE3),   // Teal500
    SocialButtonIconTint = Color(0xFFA3E9EC), // Teal400
    // Auth Divider
    AuthDividerColor = Color(0xFF75DEE3),     // Teal500
    // Splash — starts on the dark teal starting window color
    SplashBackground = Color(0xFF108188),    // Teal1300 — matches values-night/colors.xml
    SplashBackgroundEnd = Color(0xFF0F1A1A), // Dark background — final state in dark mode
    BottomNavBarBackground = Color(0xFF108188), // Teal1300 — matches iOS dark-mode bar fill
    ScanButtonBackground = Color(0xFFE8FAFA), // Teal100
    ScanButtonIconTint = Color(0xFF13A4AB), // Teal1000
    VerdictRedBackground = Color(0xFFFF80AB), // Solid pink in dark mode
    VerdictRedText = Color(0xFFFFFFFF), // White for contrast
    HealthSubtitleColor = Color(0xFFA3E9EC), // OnPrimary in dark mode
    GreetingTitleColor = Color(0xFF2FC5CC), // PrimaryVariant in dark mode
    WaterDropIconTint = Color(0xFF81C784), // VerdictGreen in dark mode
    HistoryItemTitleColor = Color(0xFFE8FAFA), // TextPrimary in dark mode
    VerdictGreenBadgeColor = Color(0xFF81C784), // VerdictGreen in dark mode
    VerdictCyanBadgeColor = Color(0xFF2FC5CC), // PrimaryVariant in dark mode
    VerdictRedWarningIconTint = Color(0xFFFFCA28), // VerdictYellow in dark mode
    HistoryItemDateColor = Color(0xFFA3E9EC), // Teal400
    // Forgot Password / Auth
    MethodCardBackground = Color(0xFF0B5F65), // Teal1400
    MethodCardIconBgUnselected = Color(0xFF108188), // Teal1300
    MethodCardIconTintUnselected = Color(0xFFC0C0C0), // Gray500
    AuthDialogBackground = Color(0xFF0A545A), // Teal1500
    AuthDialogSubtitle = Color(0xFFE5E5E4), // Gray300
)
