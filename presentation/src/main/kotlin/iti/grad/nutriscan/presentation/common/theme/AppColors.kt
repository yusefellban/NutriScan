package iti.grad.nutriscan.presentation.common.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Colors for the most recently added screens (User Profile, App Settings).
 *
 * Kept as a separate nested object rather than flat fields on [AppColors]:
 * a single Kotlin data class constructor call with ~150+ [Color] parameters
 * triggers a real D8/ART bug (`VerifyError: Rejecting invocation, expected N
 * argument registers, method signature has M or more`) that crashes the app
 * at startup. Splitting the newest additions into their own small
 * constructor call keeps every existing `AppTheme.colors.X` call site
 * unchanged via the forwarding properties on [AppColors] below.
 */
@Immutable
internal data class AppColorsExtension(
    // --- User Profile Screen ---
    val ProfileHeaderBackground: Color,
    val ProfileHeaderAccent: Color,
    val ProfileSheetBackground: Color,
    val ProfileFamilyBoxBackground: Color,
    val ProfileMenuRowBackground: Color,
    val ProfileMenuIconBackground: Color,
    val ProfileMemberCardBackground: Color,
    val ProfileMemberCardBorder: Color,
    val ProfileAddCardBackground: Color,
    val ProfileAddIconBackground: Color,
    val ProfileAddIconTint: Color,
    val ProfileAddText: Color,
    val ProfileStreakBadgeBackground: Color,
    val ProfileHeaderEdge: Color,
    val ProfileShowDetailsBackground: Color,
    val ProfileMenuLabel: Color,
    val ProfileMenuChevron: Color,
    val ProfileAddMemberAvatarBackground: Color,

    // --- Edit Profile Input Fields ---
    val EditProfileInputBackground: Color,
    val EditProfileInputBorder: Color,

    // --- Gender Selection ---
    val GenderFemaleCardBackground: Color,
    val GenderFemaleCardText: Color,
    val GenderMaleCardBackground: Color,
    val GenderMaleCardText: Color,

    // --- Page Indicator ---
    val PageIndicatorCurrent: Color,
    val PageIndicatorTotal: Color,

    // --- Date of Birth ---
    val DobCardBackground: Color,
    val DobAgeBadgeBackground: Color,
    val DobAgeBadgeText: Color,
    val DobCardText: Color,
    val DobInputBorder: Color,
    val DobInputText: Color,
    val DobCalendarIcon: Color,

    // --- Height Selection ---
    val HeightSelectedCardBackground: Color,
    val HeightSelectedCardText: Color,
    val HeightSelectedCardUnitText: Color,
    val HeightUnselectedCardBackground: Color,
    val HeightUnselectedCardText: Color,
    val HeightUnselectedCardUnitText: Color,
    val HeightRulerMajorTick: Color,
    val HeightRulerMinorTick: Color,
    val HeightRulerText: Color,

    // --- Progress Button ---
    val ProgressTrackColor: Color,
    val ProgressFillColor: Color,

    // --- Shadow Colors ---
    val ShadowSelected: Color,
    val ShadowUnselected: Color,
    val ShadowFemaleSelected: Color,
    val ShadowMaleSelected: Color,
    val ShadowDobActive: Color,
    
    // --- Product Card ---
    val ProductCardBackground: Color,
    val SavedSearchBarBorder: Color,
    val SavedSearchIconBackground: Color,
    val SavedSearchIconTint: Color,
    val ProductCardNameText: Color,
    val ProductCardVerdictBackground: Color,
    val ProductCardVerdictText: Color,
    val ProductCardCautionText: Color,
    val ProductCardCaloriesBackground: Color,
    val ProductCardCaloriesText: Color,
    val ProductCardSwipeIconBackground: Color,
    val ProductCardSwipeContainerBackground: Color,
    val ProductCardSwipeText: Color,
    val ProductCardShadow: Color,
    
    // --- App Settings ---
    val AppSettingsCardBackground: Color,
    val AppSettingsIconContainerBackground: Color,
    val AppSettingsRowLabel: Color,
    val AppSettingsToggleContainerBackground: Color,
    val AppSettingsToggleChipSelectedBackground: Color,
    val AppSettingsToggleTextUnselected: Color,
    val AppSettingsToggleTextSelected: Color,
    val AppSettingsLogoutAccent: Color,
    val AppSettingsLogoutIconBackground: Color,

    // --- Calories Dashboard ---
    // Teal200/Teal800/Teal1200 above are deliberately remapped to different
    // raw hex values in darkColors() for other screens' glow effects, so
    // they can't be reused where the Figma spec pins an exact literal swatch
    // in both themes. These three are fixed (same value in light and dark).
    val CaloriesAccentTeal1200: Color,
    val CaloriesMutedTeal: Color,
    val CaloriesIconOnAccent: Color,
    val ExerciseSecondaryText: Color,

    // --- Product Details ---
    val ProductDetailImageCardBg: Color,
    val ProductDetailScanDate: Color,
    val ProductDetailScanDateBadgeBg: Color,
    val ProductDetailScanDateBadgeText: Color,
    val ProductDetailSafetyText: Color,
    val ProductDetailTitleText: Color,
    val ProductDetailSafetyReasonText: Color,
    val ProductDetailWhyNotSafeText: Color,
    val ProductDetailIngredientCardBg: Color,
    val ProductDetailIngredientCardBorder: Color,
    val ProductDetailIngredientName: Color,
    val ProductDetailIngredientReasonText: Color,
    val ProductDetailFlaggedContainerBg: Color,
    val ProductDetailMatchTag: Color,
    val ProductDetailMatchTagText: Color,
    val ProductDetailNutritionCardBg: Color,
    val ProductDetailNutritionLabelText: Color,
    val ProductDetailNutritionPill: Color,
    val ProductDetailNutritionText: Color,
    val ProductDetailBookmarkTint: Color,

    // --- Overlays ---
    /** Modal bottom sheet / dialog scrim. Replaces ad-hoc `Color(0x66...)` literals. */
    val ScrimOverlay: Color,
)

@Immutable
@ConsistentCopyVisibility
data class AppColors internal constructor(
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

    // --- Profile Setup ---
    val ProfileSetupTitle: Color,
    val ProfileSetupSubtitle: Color,
    val ProfileSetupSectionTitle: Color,

    // --- Chips ---
    val ChipBackgroundSelected: Color,
    val ChipBackgroundUnselected: Color,
    val ChipBorderSelected: Color,
    val ChipBorderUnselected: Color,
    val ChipTextSelected: Color,
    val ChipTextUnselected: Color,
    val OtherChipActive: Color,
    val OtherChipBorder: Color,
    val OtherChipText: Color,
    val OtherChipBackground: Color,
    val OtherChipTextUnselected: Color,

    // --- Gender Selection ---
    val GenderFemaleCardBackground: Color,
    val GenderFemaleCardText: Color,
    val GenderMaleCardBackground: Color,
    val GenderMaleCardText: Color,

    // --- Page Indicator ---
    val PageIndicatorCurrent: Color,
    val PageIndicatorTotal: Color,

    // --- Date of Birth ---
    val DobCardBackground: Color,
    val DobAgeBadgeBackground: Color,
    val DobAgeBadgeText: Color,
    val DobCardText: Color,
    val DobInputBorder: Color,
    val DobInputText: Color,
    val DobCalendarIcon: Color,

    // --- Height Selection ---
    val HeightSelectedCardBackground: Color,
    val HeightSelectedCardText: Color,
    val HeightSelectedCardUnitText: Color,
    val HeightUnselectedCardBackground: Color,
    val HeightUnselectedCardText: Color,
    val HeightUnselectedCardUnitText: Color,
    val HeightRulerMajorTick: Color,
    val HeightRulerMinorTick: Color,
    val HeightRulerText: Color,

    // --- Progress Button ---
    val ProgressTrackColor: Color,
    val ProgressFillColor: Color,

    // --- Shadow Colors ---
    val ShadowSelected: Color,
    val ShadowUnselected: Color,
    val ShadowFemaleSelected: Color,
    val ShadowMaleSelected: Color,
    val ShadowDobActive: Color,

    // --- User Profile Screen / App Settings ---
    // See AppColorsExtension's kdoc: kept out of this constructor to avoid
    // a D8 VerifyError crash from having too many constructor parameters.
    private val extension: AppColorsExtension,
) {
    val ProfileHeaderBackground: Color get() = extension.ProfileHeaderBackground
    val ProfileHeaderAccent: Color get() = extension.ProfileHeaderAccent
    val ProfileSheetBackground: Color get() = extension.ProfileSheetBackground
    val ProfileFamilyBoxBackground: Color get() = extension.ProfileFamilyBoxBackground
    val ProfileMenuRowBackground: Color get() = extension.ProfileMenuRowBackground
    val ProfileMenuIconBackground: Color get() = extension.ProfileMenuIconBackground
    val ProfileMemberCardBackground: Color get() = extension.ProfileMemberCardBackground
    val ProfileMemberCardBorder: Color get() = extension.ProfileMemberCardBorder
    val ProfileAddCardBackground: Color get() = extension.ProfileAddCardBackground
    val ProfileAddIconBackground: Color get() = extension.ProfileAddIconBackground
    val ProfileAddIconTint: Color get() = extension.ProfileAddIconTint
    val ProfileAddText: Color get() = extension.ProfileAddText
    val ProfileStreakBadgeBackground: Color get() = extension.ProfileStreakBadgeBackground
    val ProfileHeaderEdge: Color get() = extension.ProfileHeaderEdge
    val ProfileShowDetailsBackground: Color get() = extension.ProfileShowDetailsBackground
    val ProfileMenuLabel: Color get() = extension.ProfileMenuLabel
    val ProfileMenuChevron: Color get() = extension.ProfileMenuChevron
    val ProfileAddMemberAvatarBackground: Color get() = extension.ProfileAddMemberAvatarBackground
    val EditProfileInputBackground: Color get() = extension.EditProfileInputBackground
    val EditProfileInputBorder: Color get() = extension.EditProfileInputBorder

    val ProductCardBackground: Color get() = extension.ProductCardBackground
    val SavedSearchBarBorder: Color get() = extension.SavedSearchBarBorder
    val SavedSearchIconBackground: Color get() = extension.SavedSearchIconBackground
    val SavedSearchIconTint: Color get() = extension.SavedSearchIconTint
    val ProductCardNameText: Color get() = extension.ProductCardNameText
    val ProductCardVerdictBackground: Color get() = extension.ProductCardVerdictBackground
    val ProductCardVerdictText: Color get() = extension.ProductCardVerdictText
    val ProductCardCautionText: Color get() = extension.ProductCardCautionText
    val ProductCardCaloriesBackground: Color get() = extension.ProductCardCaloriesBackground
    val ProductCardCaloriesText: Color get() = extension.ProductCardCaloriesText
    val ProductCardSwipeIconBackground: Color get() = extension.ProductCardSwipeIconBackground
    val ProductCardSwipeContainerBackground: Color get() = extension.ProductCardSwipeContainerBackground
    val ProductCardSwipeText: Color get() = extension.ProductCardSwipeText
    val ProductCardShadow: Color get() = extension.ProductCardShadow

    val AppSettingsCardBackground: Color get() = extension.AppSettingsCardBackground
    val AppSettingsIconContainerBackground: Color get() = extension.AppSettingsIconContainerBackground
    val AppSettingsRowLabel: Color get() = extension.AppSettingsRowLabel
    val AppSettingsToggleContainerBackground: Color get() = extension.AppSettingsToggleContainerBackground
    val AppSettingsToggleChipSelectedBackground: Color get() = extension.AppSettingsToggleChipSelectedBackground
    val AppSettingsToggleTextUnselected: Color get() = extension.AppSettingsToggleTextUnselected
    val AppSettingsToggleTextSelected: Color get() = extension.AppSettingsToggleTextSelected
    val AppSettingsLogoutAccent: Color get() = extension.AppSettingsLogoutAccent
    val AppSettingsLogoutIconBackground: Color get() = extension.AppSettingsLogoutIconBackground

    val CaloriesAccentTeal1200: Color get() = extension.CaloriesAccentTeal1200
    val CaloriesMutedTeal: Color get() = extension.CaloriesMutedTeal
    val CaloriesIconOnAccent: Color get() = extension.CaloriesIconOnAccent
    val ExerciseSecondaryText: Color get() = extension.ExerciseSecondaryText

    val ProductDetailImageCardBg: Color get() = extension.ProductDetailImageCardBg
    val ProductDetailScanDate: Color get() = extension.ProductDetailScanDate
    val ProductDetailScanDateBadgeBg: Color get() = extension.ProductDetailScanDateBadgeBg
    val ProductDetailScanDateBadgeText: Color get() = extension.ProductDetailScanDateBadgeText
    val ProductDetailSafetyText: Color get() = extension.ProductDetailSafetyText
    val ProductDetailTitleText: Color get() = extension.ProductDetailTitleText
    val ProductDetailSafetyReasonText: Color get() = extension.ProductDetailSafetyReasonText
    val ProductDetailWhyNotSafeText: Color get() = extension.ProductDetailWhyNotSafeText
    val ProductDetailIngredientCardBg: Color get() = extension.ProductDetailIngredientCardBg
    val ProductDetailIngredientCardBorder: Color get() = extension.ProductDetailIngredientCardBorder
    val ProductDetailIngredientName: Color get() = extension.ProductDetailIngredientName
    val ProductDetailIngredientReasonText: Color get() = extension.ProductDetailIngredientReasonText
    val ProductDetailFlaggedContainerBg: Color get() = extension.ProductDetailFlaggedContainerBg
    val ProductDetailMatchTag: Color get() = extension.ProductDetailMatchTag
    val ProductDetailMatchTagText: Color get() = extension.ProductDetailMatchTagText
    val ProductDetailNutritionCardBg: Color get() = extension.ProductDetailNutritionCardBg
    val ProductDetailNutritionLabelText: Color get() = extension.ProductDetailNutritionLabelText
    val ProductDetailNutritionPill: Color get() = extension.ProductDetailNutritionPill
    val ProductDetailNutritionText: Color get() = extension.ProductDetailNutritionText
    val ProductDetailBookmarkTint: Color get() = extension.ProductDetailBookmarkTint
    val ScrimOverlay: Color get() = extension.ScrimOverlay
}

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

    // Profile Setup
    ProfileSetupTitle = Color(0xFF13A4AB), // Teal1000
    ProfileSetupSubtitle = Color(0xFF898989), // Gray700
    ProfileSetupSectionTitle = Color(0xFF545454), // Gray1300

    // Chips
    ChipBackgroundSelected = Color(0xFFC0C0C0), // Gray500
    ChipBackgroundUnselected = Color(0xFFFFFFFF), // White
    ChipBorderSelected = Color(0xFF898989), // Gray700
    ChipBorderUnselected = Color(0xFFC0C0C0), // Gray500
    ChipTextSelected = Color(0xFF3E3E3E), // Gray1400
    ChipTextUnselected = Color(0xFF3E3E3E), // Gray1400
    OtherChipActive = Color(0xFF13A4AB), // Teal1000
    OtherChipBorder = Color(0xFFD6D6D5), // Gray400
    OtherChipText = Color(0xFF3E3E3E), // Gray1400
    OtherChipBackground = Color(0xFFFFFFFF), // White
    OtherChipTextUnselected = Color(0xFF898989), // Gray700 (grey in light mode)

    // Gender Selection
    GenderFemaleCardBackground = Color(0xFFFFF1F3), // Red100 equivalent
    GenderFemaleCardText = Color(0xFFFA4D5E), // #FA4D5E
    GenderMaleCardBackground = Color(0xFF75DEE3), // Teal500
    GenderMaleCardText = Color(0xFF0F474A), // Teal1600

    // Page Indicator
    PageIndicatorCurrent = Color(0xFF13A4AB), // Teal1000
    PageIndicatorTotal = Color(0xFFA6A5A5), // Gray600

    // Date of Birth
    DobCardBackground = Color(0xFF75DEE3), // Teal500
    DobAgeBadgeBackground = Color(0xFF2FC5CC), // Teal700
    DobAgeBadgeText = Color(0xFF0F474A), // Teal1600
    DobCardText = Color(0xFF0F474A), // Teal1600
    DobInputBorder = Color(0xFFE5E5E4), // Gray300
    DobInputText = Color(0xFFA6A5A5), // Gray600
    DobCalendarIcon = Color(0xFFE5E5E4), // Gray300

    // Height Selection
    HeightSelectedCardBackground = Color(0xFF75DEE3), // Teal500
    HeightSelectedCardText = Color(0xFF0F474A), // Teal1600
    HeightSelectedCardUnitText = Color(0xFF108188), // Teal1300
    HeightUnselectedCardBackground = Color(0xFFE5E5E4), // Gray300 (Light Gray)
    HeightUnselectedCardText = Color(0xFF5F5F5F), // Gray1200 (Darker Gray)
    HeightUnselectedCardUnitText = Color(0xFF898989), // Gray700
    HeightRulerMajorTick = Color(0xFF13A4AB), // Teal1000
    HeightRulerMinorTick = Color(0xFFCAF2F4), // Teal300
    HeightRulerText = Color(0xFF13A4AB), // Teal1000

    // Progress Button
    ProgressTrackColor = Color(0xFFD4F1F2), // Teal200
    ProgressFillColor = Color(0xFF13A4AB), // Teal1000

    // Shadows
    ShadowSelected = Color(0x6613A4AB), // Teal1000/Primary 40% alpha
    ShadowUnselected = Color(0x3313A4AB), // Teal1000/Primary 20% alpha
    ShadowFemaleSelected = Color(0x6613A4AB),
    ShadowMaleSelected = Color(0x6613A4AB),
    ShadowDobActive = Color(0x6613A4AB),

    extension = AppColorsExtension(
        // User Profile Screen — light values match the Figma light spec
        ProfileHeaderBackground = Color(0xFF13A4AB), // Teal1000 — screen bg behind header
        ProfileHeaderAccent = Color(0xFF11939A), // Teal1200 — avatar ring
        ProfileSheetBackground = Color(0xFFFFFFFF), // White — main sheet
        ProfileFamilyBoxBackground = Color(0xFFFFFFFF), // White — dashed container fill
        ProfileMenuRowBackground = Color(0xFFF8F8F9), // Gray100
        ProfileMenuIconBackground = Color(0xFFD4F1F2), // Teal200 — icon circle (0.55 alpha in code)
        ProfileMemberCardBackground = Color(0xFFE8FAFA), // Teal100
        ProfileMemberCardBorder = Color(0xFF11939A), // Teal1200 — solid teal border
        ProfileAddCardBackground = Color(0xFFF8F8F9), // Gray100 — light grey/off-white
        ProfileAddIconBackground = Color(0xFFFFFFFF), // White
        ProfileAddIconTint = Color(0xFF13A4AB), // Teal1000 — teal "+" icon
        ProfileAddText = Color(0xFF0F474A), // Teal1600
        ProfileStreakBadgeBackground = Color(0xFF11939A), // Teal1200 — fixed both themes
        ProfileHeaderEdge = Color(0xFF11939A), // Teal1200 — decorative edge shape
        ProfileShowDetailsBackground = Color(0xFF17B8BE), // Teal800 — fixed both themes
        ProfileMenuLabel = Color(0xFFC0C0C0), // Gray500
        ProfileMenuChevron = Color(0xFFC0C0C0), // Gray500
        ProfileAddMemberAvatarBackground = Color(0xFF11939A), // Teal1200 - matches settings avatar in light theme

        // Edit Profile Input Fields
        EditProfileInputBackground = Color(0xFFFFFFFF), // White / Background
        EditProfileInputBorder = Color(0xFFE5E5E4), // Gray300 / Divider

        // Gender Selection
        GenderFemaleCardBackground = Color(0xFFFFF1F3), // Red100 equivalent
        GenderFemaleCardText = Color(0xFFFA4D5E), // #FA4D5E
        GenderMaleCardBackground = Color(0xFF75DEE3), // Teal500
        GenderMaleCardText = Color(0xFF0F474A), // Teal1600

        // Page Indicator
        PageIndicatorCurrent = Color(0xFF13A4AB), // Teal1000
        PageIndicatorTotal = Color(0xFFA6A5A5), // Gray600

        // Date of Birth
        DobCardBackground = Color(0xFF75DEE3), // Teal500
        DobAgeBadgeBackground = Color(0xFF2FC5CC), // Teal700
        DobAgeBadgeText = Color(0xFF0F474A), // Teal1600
        DobCardText = Color(0xFF0F474A), // Teal1600
        DobInputBorder = Color(0xFFE5E5E4), // Gray300
        DobInputText = Color(0xFFA6A5A5), // Gray600
        DobCalendarIcon = Color(0xFFE5E5E4), // Gray300

        // Height Selection
        HeightSelectedCardBackground = Color(0xFF75DEE3), // Teal500
        HeightSelectedCardText = Color(0xFF0F474A), // Teal1600
        HeightSelectedCardUnitText = Color(0xFF108188), // Teal1300
        HeightUnselectedCardBackground = Color(0xFFE5E5E4), // Gray300 (Light Gray)
        HeightUnselectedCardText = Color(0xFF5F5F5F), // Gray1200 (Darker Gray)
        HeightUnselectedCardUnitText = Color(0xFF898989), // Gray700
        HeightRulerMajorTick = Color(0xFF13A4AB), // Teal1000
        HeightRulerMinorTick = Color(0xFFCAF2F4), // Teal300
        HeightRulerText = Color(0xFF13A4AB), // Teal1000

        // Progress Button
        ProgressTrackColor = Color(0xFFD4F1F2), // Teal200
        ProgressFillColor = Color(0xFF13A4AB), // Teal1000

        // Shadows
        ShadowSelected = Color(0x6613A4AB), // Teal1000/Primary 40% alpha
        ShadowUnselected = Color(0x3313A4AB), // Teal1000/Primary 20% alpha
        ShadowFemaleSelected = Color(0x6613A4AB),
        ShadowMaleSelected = Color(0x6613A4AB),
        ShadowDobActive = Color(0x6613A4AB),
        
        // Product Card
        ProductCardBackground = Color(0xFFFFFFFF), // White
        SavedSearchBarBorder = Color(0xFFC0C0C0), // Gray500
        SavedSearchIconBackground = Color(0xFF13A4AB), // Teal1000
        SavedSearchIconTint = Color(0xFFE8FAFA), // Teal100
        ProductCardNameText = Color(0xFF13A4AB), // Teal1000
        ProductCardVerdictBackground = Color(0xFF13A4AB), // Teal1000
        ProductCardVerdictText = Color(0xFFE8FAFA), // Teal100
        ProductCardCautionText = Color(0xFF0B5F65), // Teal1400 (matches dark mode surface)
        ProductCardCaloriesBackground = Color(0xFFCAF2F4), // Teal300
        ProductCardCaloriesText = Color(0xFF2FC5CC), // Teal700
        ProductCardSwipeIconBackground = Color(0xFF13A4AB), // Teal1000
        ProductCardSwipeContainerBackground = Color(0xFFF1F1F1), // Gray200
        ProductCardSwipeText = Color(0xFFC0C0C0), // Gray500
        ProductCardShadow = Color(0x3313A4AB), // Teal1000 20% alpha

        // App Settings
        AppSettingsCardBackground = Color(0xFFF8F8F9), // Gray100
        AppSettingsIconContainerBackground = Color(0xFFD4F1F2), // Teal200 (light)
        AppSettingsRowLabel = Color(0xFFC0C0C0), // Gray500
        AppSettingsToggleContainerBackground = Color(0xFFE5E5E4), // Gray300
        AppSettingsToggleChipSelectedBackground = Color(0xFFFFFFFF), // White
        AppSettingsToggleTextUnselected = Color(0xFF777777), // Gray800
        AppSettingsToggleTextSelected = Color(0xFF3E3E3E), // Gray1400 (matches ChipTextSelected)
        AppSettingsLogoutAccent = Color(0xFFFA4D5E),
        AppSettingsLogoutIconBackground = Color(0xFFFFF1F3), // ErrorBackground (light)

        // Calories Dashboard — fixed literals, identical in both themes (see AppColorsExtension kdoc)
        CaloriesAccentTeal1200 = Color(0xFF11939A), // Teal/1200 swatch literal
        CaloriesMutedTeal = Color(0xFFD4F1F2), // Teal/200 swatch literal
        CaloriesIconOnAccent = Color(0xFFFFFFFF),
        ExerciseSecondaryText = Color(0xFF3E4949), // literal from Figma, light mode only

        // Product Details
        ProductDetailImageCardBg = Color(0xFFF8F8F9), // Gray100
        ProductDetailScanDate = Color(0xFF898989), // Gray700
        ProductDetailScanDateBadgeBg = Color(0xFFE8FAFA), // Teal100
        ProductDetailScanDateBadgeText = Color(0xFF13A4AB), // Primary (Teal1000)
        ProductDetailSafetyText = Color(0xFF393C3C), // Gray1600

        ProductDetailTitleText = Color(0xFF13A4AB), // Teal1000
        ProductDetailSafetyReasonText = Color(0xFF777777), // Gray800
        ProductDetailWhyNotSafeText = Color(0xFF393C3C), // Gray1600

        ProductDetailIngredientCardBg = Color(0xFFF8F8F9), // Gray100
        ProductDetailIngredientCardBorder = Color(0xFFE5E5E5), // Lighter border? or transparent
        ProductDetailIngredientName = Color(0xFF13A4AB), // Teal1000
        ProductDetailIngredientReasonText = Color(0xFF757575), // Gray600
        ProductDetailFlaggedContainerBg = Color(0xFFF8F8F9), // Gray100

        ProductDetailMatchTag = Color(0xFFC0C0C0), // Gray500
        ProductDetailMatchTagText = Color(0xFFF8F8F9), // Gray100

        ProductDetailNutritionCardBg = Color(0xFFF8F8F9), // Gray100
        ProductDetailNutritionLabelText = Color(0xFF777777), // Gray800
        ProductDetailNutritionPill = Color(0xFF13A4AB), // Teal1000
        ProductDetailNutritionText = Color(0xFFF8F8F9), // Gray100

        ProductDetailBookmarkTint = Color(0xFF13A4AB), // Teal1000

        ScrimOverlay = Color(0x660F474A), // Teal1600 40% alpha — light mode scrim
    ),
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

    // Profile Setup
    ProfileSetupTitle = Color(0xFFE8FAFA), // Teal100
    ProfileSetupSubtitle = Color(0xFF108188), // Teal1300
    ProfileSetupSectionTitle = Color(0xFF75DEE3), // Teal500

    // Chips
    ChipBackgroundSelected = Color(0xFF13A4AB), // Teal1000
    ChipBackgroundUnselected = Color(0xFF0B5F65), // Teal1400
    ChipBorderSelected = Color(0xFF75DEE3), // Teal500
    ChipBorderUnselected = Color(0xFF108188), // Teal1300
    ChipTextSelected = Color(0xFF0F474A), // Teal1600
    ChipTextUnselected = Color(0xFF2FC5CC), // Teal700
    OtherChipActive = Color(0xFF2FC5CC), // Teal700
    OtherChipBorder = Color(0xFF108188), // Teal1300
    OtherChipText = Color(0xFF2FC5CC), // Teal700
    OtherChipBackground = Color(0xFF0B5F65), // Teal1400
    OtherChipTextUnselected = Color(0xFF2FC5CC), // Teal700 (stays teal in dark mode)

    // Gender Selection
    GenderFemaleCardBackground = Color(0xFF0B5F65), // Teal1400
    GenderFemaleCardText = Color(0xFFFDE8E8), // Red100
    GenderMaleCardBackground = Color(0xFF75DEE3), // Teal500
    GenderMaleCardText = Color(0xFF0F474A), // Teal1600

    // Page Indicator
    PageIndicatorCurrent = Color(0xFF13A4AB), // Teal1000
    PageIndicatorTotal = Color(0xFF108188), // Teal1300

    // Date of Birth
    DobCardBackground = Color(0xFF75DEE3), // Teal500
    DobAgeBadgeBackground = Color(0xFF2FC5CC), // Teal700
    DobAgeBadgeText = Color(0xFF0F474A), // Teal1600
    DobCardText = Color(0xFF0F474A), // Teal1600
    DobInputBorder = Color(0xFF169098), // Teal1200
    DobInputText = Color(0xFF169098), // Teal1200
    DobCalendarIcon = Color(0xFF169098), // Teal1200

    // Height Selection
    HeightSelectedCardBackground = Color(0xFF75DEE3), // Teal500
    HeightSelectedCardText = Color(0xFF0F474A), // Teal1600
    HeightSelectedCardUnitText = Color(0xFF0F474A), // Teal1600
    HeightUnselectedCardBackground = Color(0xFF0B5F65), // Teal1400
    HeightUnselectedCardText = Color(0xFF2FC5CC), // Teal700
    HeightUnselectedCardUnitText = Color(0xFFCAF2F4), // Teal300
    HeightRulerMajorTick = Color(0xFF75DEE3), // Teal500
    HeightRulerMinorTick = Color(0xFF0B5F65), // Teal1400
    HeightRulerText = Color(0xFF75DEE3), // Teal500

    // Progress Button
    ProgressTrackColor = Color(0xFF0B5F65), // Teal1400
    ProgressFillColor = Color(0xFF13A4AB), // Teal1000

    // Shadows
    ShadowSelected = Color(0x9975DEE3), // Teal500/Accent 60% alpha (bright cyan)
    ShadowUnselected = Color(0x33FFFFFF), // Soft white glow 20% alpha
    ShadowFemaleSelected = Color(0x99FF80AB), // Pink glow
    ShadowMaleSelected = Color(0x9975DEE3),
    ShadowDobActive = Color(0x9975DEE3),

    extension = AppColorsExtension(
        // User Profile Screen — dark values match the Figma dark spec
        ProfileHeaderBackground = Color(0xFF0A545A), // Teal1500 — screen bg behind header
        ProfileHeaderAccent = Color(0xFF0B5F65), // Teal1400 — avatar ring
        ProfileSheetBackground = Color(0xFF0F474A), // Teal1600 — main sheet
        ProfileFamilyBoxBackground = Color(0xFF0A545A), // Teal1500 — dashed container fill
        ProfileMenuRowBackground = Color(0xFF0A545A), // Teal1500
        ProfileMenuIconBackground = Color(0xFF0F474A), // Teal1600 — icon circle (0.55 alpha in code)
        ProfileMemberCardBackground = Color(0xFF0F474A), // Teal1600 — elevated deep teal
        ProfileMemberCardBorder = Color(0xFF11939A), // Teal1200 — solid teal border
        ProfileAddCardBackground = Color(0xFF0F474A), // Teal1600
        ProfileAddIconBackground = Color(0xFFE8FAFA), // Teal100
        ProfileAddIconTint = Color(0xFF13A4AB), // Teal1000
        ProfileAddText = Color(0xFFE8FAFA), // Teal100
        ProfileStreakBadgeBackground = Color(0xFF11939A), // Teal1200 — fixed both themes
        ProfileHeaderEdge = Color(0xFF0B5F65), // Teal1400 — decorative edge shape
        ProfileShowDetailsBackground = Color(0xFF17B8BE), // Teal800 — fixed both themes
        ProfileMenuLabel = Color(0xFF11939A), // Teal1200
        ProfileMenuChevron = Color(0xFF13A4AB), // Teal1000
        ProfileAddMemberAvatarBackground = Color(0xFF0F474A), // Teal1600 - darker background in dark theme

        // Edit Profile Input Fields
        EditProfileInputBackground = Color(0xFF0A545A), // Teal1500 / SurfaceVariant
        EditProfileInputBorder = Color(0xFF108188), // Teal1300 / OtherChipBorder

        // Gender Selection
        GenderFemaleCardBackground = Color(0xFF0B5F65), // Teal1400
        GenderFemaleCardText = Color(0xFFFDE8E8), // Red100
        GenderMaleCardBackground = Color(0xFF75DEE3), // Teal500
        GenderMaleCardText = Color(0xFF0F474A), // Teal1600

        // Page Indicator
        PageIndicatorCurrent = Color(0xFF13A4AB), // Teal1000
        PageIndicatorTotal = Color(0xFF108188), // Teal1300

        // Date of Birth
        DobCardBackground = Color(0xFF75DEE3), // Teal500
        DobAgeBadgeBackground = Color(0xFF2FC5CC), // Teal700
        DobAgeBadgeText = Color(0xFF0F474A), // Teal1600
        DobCardText = Color(0xFF0F474A), // Teal1600
        DobInputBorder = Color(0xFF169098), // Teal1200
        DobInputText = Color(0xFF169098), // Teal1200
        DobCalendarIcon = Color(0xFF169098), // Teal1200

        // Height Selection
        HeightSelectedCardBackground = Color(0xFF75DEE3), // Teal500
        HeightSelectedCardText = Color(0xFF0F474A), // Teal1600
        HeightSelectedCardUnitText = Color(0xFF0F474A), // Teal1600
        HeightUnselectedCardBackground = Color(0xFF0B5F65), // Teal1400
        HeightUnselectedCardText = Color(0xFF2FC5CC), // Teal700
        HeightUnselectedCardUnitText = Color(0xFFCAF2F4), // Teal300
        HeightRulerMajorTick = Color(0xFF75DEE3), // Teal500
        HeightRulerMinorTick = Color(0xFF0B5F65), // Teal1400
        HeightRulerText = Color(0xFF75DEE3), // Teal500

        // Progress Button
        ProgressTrackColor = Color(0xFF0B5F65), // Teal1400
        ProgressFillColor = Color(0xFF13A4AB), // Teal1000

        // Shadows
        ShadowSelected = Color(0x9975DEE3), // Teal500/Accent 60% alpha (bright cyan)
        ShadowUnselected = Color(0x33FFFFFF), // Soft white glow 20% alpha
        ShadowFemaleSelected = Color(0x99FF80AB), // Pink glow
        ShadowMaleSelected = Color(0x9975DEE3),
        ShadowDobActive = Color(0x9975DEE3),
        
        // Product Card
        ProductCardBackground = Color(0xFF0A545A), // Teal1500
        SavedSearchBarBorder = Color(0xFF0F8389), // Teal1200
        SavedSearchIconBackground = Color(0xFF13A4AB), // Teal1000
        SavedSearchIconTint = Color(0xFFE8FAFA), // Teal100
        ProductCardNameText = Color(0xFF75DEE3), // Teal500
        ProductCardVerdictBackground = Color(0xFF13A4AB), // Teal1000
        ProductCardVerdictText = Color(0xFFE8FAFA), // Teal100
        ProductCardCautionText = Color(0xFF0B5F65), // Teal1400
        ProductCardCaloriesBackground = Color(0xFFCAF2F4), // Teal300
        ProductCardCaloriesText = Color(0xFF0F474A), // Teal1600
        ProductCardSwipeIconBackground = Color(0xFF13A4AB), // Teal1000
        ProductCardSwipeContainerBackground = Color(0xFF0F474A), // Teal1600
        ProductCardSwipeText = Color(0xFF108188), // Teal1300
        ProductCardShadow = Color(0x8013A4AB), // Teal1000 50% alpha

        // App Settings
        AppSettingsCardBackground = Color(0xFF0A545A), // Teal1500
        // Teal200 is remapped to 0xFF2FC5CC in this palette for glowing shadows/badges — cannot reuse for this
        AppSettingsIconContainerBackground = Color(0xFF0F474A), // Teal1600
        // Teal1200 is remapped to 0xFFA3E9EC in this palette for secondary text — cannot reuse for this
        AppSettingsRowLabel = Color(0xFF11939A), // Teal1200 (light) literal
        AppSettingsToggleContainerBackground = Color(0xFF0F474A), // Teal1600
        AppSettingsToggleChipSelectedBackground = Color(0xFF13A4AB), // Teal1000
        AppSettingsToggleTextUnselected = Color(0xFFA3E9EC), // Teal400
        AppSettingsToggleTextSelected = Color(0xFFA3E9EC), // Teal400 — same as unselected, differentiated by chip bg
        // Error is remapped to 0xFFFF6B7A in this palette — spec requires the same red in both themes here
        AppSettingsLogoutAccent = Color(0xFFFA4D5E),
        AppSettingsLogoutIconBackground = Color(0xFF0A545A), // Teal1500 — ErrorBackground dark value doesn't fit here

        // Calories Dashboard — fixed literals, identical in both themes (see AppColorsExtension kdoc)
        CaloriesAccentTeal1200 = Color(0xFF11939A), // Teal/1200 swatch literal
        CaloriesMutedTeal = Color(0xFFD4F1F2), // Teal/200 swatch literal
        CaloriesIconOnAccent = Color(0xFFFFFFFF),
        ExerciseSecondaryText = Color(0xFF11939A), // Teal/1200 literal, matches dark mode's own Figma value

        // Product Details
        ProductDetailImageCardBg = Color(0xFF0F474A), // Teal1600
        ProductDetailScanDate = Color(0xFFA3E9EC), // Teal400
        ProductDetailScanDateBadgeBg = Color(0xFFD4F1F2), // Teal200
        ProductDetailScanDateBadgeText = Color(0xFF17B8BE), // Teal800
        ProductDetailSafetyText = Color(0xFF13A4AB), // Teal1000

        ProductDetailTitleText = Color(0xFFA3E9EC), // Teal400
        ProductDetailSafetyReasonText = Color(0xFF11939A), // Teal1200
        ProductDetailWhyNotSafeText = Color(0xFF13A4AB), // Teal1000

        ProductDetailIngredientCardBg = Color(0xFF0A545A), // Teal1500
        ProductDetailIngredientCardBorder = Color(0xFF11939A), // Teal1200
        ProductDetailIngredientName = Color(0xFF13A4AB), // Teal1000
        ProductDetailIngredientReasonText = Color(0xFF108188), // Teal1300
        ProductDetailFlaggedContainerBg = Color(0xFF0A545A), // Teal1500

        ProductDetailMatchTag = Color(0xFF0F474A), // Teal1600
        ProductDetailMatchTagText = Color(0xFF11939A), // Teal1200

        ProductDetailNutritionCardBg = Color(0xFF0B5F65), // Teal1400
        ProductDetailNutritionLabelText = Color(0xFF11939A), // Teal1200
        ProductDetailNutritionPill = Color(0xFF13A4AB), // Teal1000
        ProductDetailNutritionText = Color(0xFFF8F8F9), // Gray100

        ProductDetailBookmarkTint = Color(0xFF13A4AB), // Teal1000

        ScrimOverlay = Color(0x99000000), // Black 60% alpha — dark mode scrim
    ),
)
