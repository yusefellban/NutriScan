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
    val ProfileMemberCardBackground: Color,
    val ProfileMemberCardBorder: Color,
    val ProfileAddIconBackground: Color,
    val ProfileAddText: Color,
    val ProfileStreakBadgeBackground: Color,
    val ProfileHeaderEdge: Color,
    val ProfileShowDetailsBackground: Color,
    val ProfileMenuChevron: Color,
    val ProfileAddMemberAvatarBackground: Color,

    // --- Edit Profile Input Fields ---
    val EditProfileInputBackground: Color,
    val EditProfileInputBorder: Color,

    // --- Gender Selection ---
    val GenderFemaleCardBackground: Color,
    val GenderFemaleCardText: Color,
    val GenderMaleCardText: Color,

    // --- Page Indicator ---
    val PageIndicatorTotal: Color,

    // --- Date of Birth ---
    val DobAgeBadgeBackground: Color,
    val DobAgeBadgeText: Color,
    val DobCardText: Color,
    val DobInputBorder: Color,
    val DobInputText: Color,
    val DobCalendarIcon: Color,

    // --- Height Selection ---
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

    // --- Shadow Colors ---
    val ShadowSelected: Color,
    val ShadowUnselected: Color,
    val ShadowFemaleSelected: Color,
    val ShadowMaleSelected: Color,
    val ShadowDobActive: Color,
    
    // --- Product Card ---
    val ProductCardBackground: Color,
    val SavedSearchBarBorder: Color,
    val ProductCardNameText: Color,
    val ProductCardCautionText: Color,
    val ProductCardCaloriesBackground: Color,
    val ProductCardCaloriesText: Color,
    val ProductCardSwipeContainerBackground: Color,
    val ProductCardSwipeText: Color,
    val ProductCardShadow: Color,
    
    // --- App Settings ---
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
    val SectionSubtitle: Color,
    val CaloriesMutedTeal: Color,
    val CaloriesIconOnAccent: Color,
    val ExerciseSecondaryText: Color,

    
)

@Immutable
internal data class AppColorsExtension2(
    // --- Exercise Screens (dark mode support) ---
    val ExerciseCardBackground: Color,
    val ExerciseCardTitle: Color,
    val ExerciseCardSubtitle: Color,
    val ExerciseCardChevron: Color,
    val ExerciseChipSelectedBg: Color,
    val ExerciseChipSelectedBorder: Color,
    val ExerciseChipSelectedText: Color,
    val ExerciseChipUnselectedBg: Color,
    val ExerciseChipUnselectedBorder: Color,
    val ExerciseChipUnselectedText: Color,
    val ExerciseInstructionsTitle: Color,
    val ExerciseInstructionsBody: Color,
    val ExerciseInstructionsBullet: Color,
    val ExerciseCancelWorkoutText: Color,

    val ExerciseScreenTitle: Color,
    val ExerciseBackButtonTint: Color,
    val ExerciseSearchPlaceholder: Color,
    val ExerciseWorkoutHeaderTitle: Color,
    val ExerciseWorkoutImageBackground: Color,
    val ExerciseWorkoutTimerText: Color,
    val ExerciseWorkoutPrimaryButtonText: Color,
    val ExerciseWorkoutTotalTimeLabel: Color,
    val ExerciseSetsRepsCardBg: Color,
    val ExerciseSetsRepsCardBorder: Color,
    val ExerciseSetsRepsBtnBg: Color,
    val ExerciseSetsRepsLabelColor: Color,
    val ExerciseSetsRepsValueColor: Color,
    val ExerciseReadMoreColor: Color,

    // --- Product Details ---
    val ProductDetailScanDate: Color,
    val ProductDetailScanDateBadgeBg: Color,
    val ProductDetailScanDateBadgeText: Color,
    val ProductDetailSafetyText: Color,
    val ProductDetailTitleText: Color,
    val ProductDetailSafetyReasonText: Color,
    val ProductDetailWhyNotSafeText: Color,
    val ProductDetailIngredientCardBorder: Color,
    val ProductDetailIngredientReasonText: Color,
    val ProductDetailMatchTag: Color,
    val ProductDetailMatchTagText: Color,
    val ProductDetailNutritionCardBg: Color,
    val ProductDetailNutritionLabelText: Color,
    val ProductDetailNutritionText: Color,
    /** Fixed brand hex, same in both modes — CAUTION verdict badge background. */
    val VerdictCautionBackground: Color,
    /** Fixed brand hex, same in both modes — CAUTION verdict badge text. */
    val VerdictCautionText: Color,
    /** Fixed brand hex, same in both modes — UNSAFE verdict badge background. */
    val VerdictUnsafeBackground: Color,
    /** Fixed brand hex, same in both modes — UNSAFE verdict badge text. */
    val VerdictUnsafeText: Color,

    // --- Overlays ---
    /** Modal bottom sheet / dialog scrim. Replaces ad-hoc `Color(0x66...)` literals. */
    val ScrimOverlay: Color,
    
    // --- NutriGPT Chat ---
    val ChatScreenBackground: Color,
    val ChatScreenBackgroundEnd: Color,
    val ChatUserBubble: Color,
    val ChatUserBubbleText: Color,
    val ChatBotBubble: Color,
    val ChatBotBubbleText: Color,
    val ChatSourceCardBackground: Color,
    val ChatSourceScoreBadge: Color,
    val ChatInputBackground: Color,
    val ChatInputPlaceholder: Color,
    val ChatSendButtonBackground: Color,
    val ChatSendButtonIcon: Color,
    val ChatDisclaimerBackground: Color,
    val ChatDisclaimerText: Color,
    val ChatDisclaimerIcon: Color,
)

@Immutable
internal data class AppColorsExtension3(
    // Top Bar & Header
    val StepHistoryTopBarIconBg: Color,
    val StepHistoryTitle: Color,

    // Gauge Card
    val StepHistoryGaugeTrack: Color,
    val StepHistoryGaugeFill: Color,
    val StepHistoryGaugeLabelText: Color,

    // Period Chips
    val StepHistoryChipContainerBg: Color,
    val StepHistoryChipBgSelected: Color,
    val StepHistoryChipBgUnselected: Color,
    val StepHistoryChipTextUnselected: Color,

    // Bar Chart
    val StepHistoryChartBarBg: Color,
    val StepHistoryChartBarFill: Color,

    // Summary Cards
    val StepHistorySummaryIconBg: Color,

    // News Screen Redesign
    val NewsChipSelectedBg: Color,
    val NewsChipSelectedText: Color,
    val NewsChipUnselectedBg: Color,
    val NewsChipUnselectedBorder: Color,
    val NewsChipUnselectedText: Color,
    val NewsCategoryLabel: Color,
    val NewsCardTitle: Color,
    val NewsSourceText: Color,
    val NewsSourceAvatarBg: Color,
    val NewsSearchBarBorder: Color,
    val NewsSearchIconTint: Color,
    val NewsDivider: Color,
    val NewsScreenTitle: Color,
    val NewsCardBg: Color,
    val NewsCardBorder: Color,

    // --- Calories History ---
    val CaloriesHistoryTopBarIconBg: Color,
    val CaloriesHistoryTitle: Color,
    val CaloriesHistoryDateText: Color,
    val CaloriesHistoryCardBorder: Color,
    val CaloriesHistoryStatLabel: Color,
    val CaloriesHistoryStatValue: Color,
    val CaloriesHistoryStatSecondary: Color,
    val CaloriesHistoryCalendarIconBg: Color,
    val CaloriesHistoryCalendarIconTint: Color,
    val CaloriesHistoryOuterCardBg: Color,
    val CaloriesHistoryDateChipBg: Color,
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
    val ScreenSurfaceBackground: Color,
    val MenuSectionLabel: Color,
    val MenuIconContainerBackground: Color,

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
    val MethodCardIconBgUnselected: Color,
    val MethodCardIconTintUnselected: Color,
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
    val GenderMaleCardText: Color,

    // --- Page Indicator ---
    val PageIndicatorTotal: Color,

    // --- Date of Birth ---
    val DobAgeBadgeBackground: Color,
    val DobAgeBadgeText: Color,
    val DobCardText: Color,
    val DobInputBorder: Color,
    val DobInputText: Color,
    val DobCalendarIcon: Color,

    // --- Height Selection ---
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

    // --- Shadow Colors ---
    val ShadowSelected: Color,
    val ShadowUnselected: Color,
    val ShadowFemaleSelected: Color,
    val ShadowMaleSelected: Color,
    val ShadowDobActive: Color,

    // --- Water Tracker ---
    val WaterEmptyGlassTint: Color,
    val WaterAddButtonBackground: Color,
    val WaterAddButtonIconTint: Color,

    // --- User Profile Screen / App Settings ---
    // See AppColorsExtension's kdoc: kept out of this constructor to avoid
    // a D8 VerifyError crash from having too many constructor parameters.
    private val extension: AppColorsExtension,
    private val extension2: AppColorsExtension2,
    private val extension3: AppColorsExtension3,
) {
    val ProfileHeaderBackground: Color get() = extension.ProfileHeaderBackground
    val ProfileHeaderAccent: Color get() = extension.ProfileHeaderAccent
    val ProfileSheetBackground: Color get() = extension.ProfileSheetBackground
    val ProfileFamilyBoxBackground: Color get() = extension.ProfileFamilyBoxBackground
    val ProfileMemberCardBackground: Color get() = extension.ProfileMemberCardBackground
    val ProfileMemberCardBorder: Color get() = extension.ProfileMemberCardBorder
    val ProfileAddIconBackground: Color get() = extension.ProfileAddIconBackground
    val ProfileAddText: Color get() = extension.ProfileAddText
    val ProfileStreakBadgeBackground: Color get() = extension.ProfileStreakBadgeBackground
    val ProfileHeaderEdge: Color get() = extension.ProfileHeaderEdge
    val ProfileShowDetailsBackground: Color get() = extension.ProfileShowDetailsBackground
    val ProfileMenuChevron: Color get() = extension.ProfileMenuChevron
    val ProfileAddMemberAvatarBackground: Color get() = extension.ProfileAddMemberAvatarBackground
    val EditProfileInputBackground: Color get() = extension.EditProfileInputBackground
    val EditProfileInputBorder: Color get() = extension.EditProfileInputBorder

    val ProductCardBackground: Color get() = extension.ProductCardBackground
    val SavedSearchBarBorder: Color get() = extension.SavedSearchBarBorder
    val ProductCardNameText: Color get() = extension.ProductCardNameText
    val ProductCardCautionText: Color get() = extension.ProductCardCautionText
    val ProductCardCaloriesBackground: Color get() = extension.ProductCardCaloriesBackground
    val ProductCardCaloriesText: Color get() = extension.ProductCardCaloriesText
    val ProductCardSwipeContainerBackground: Color get() = extension.ProductCardSwipeContainerBackground
    val ProductCardSwipeText: Color get() = extension.ProductCardSwipeText
    val ProductCardShadow: Color get() = extension.ProductCardShadow

    val AppSettingsToggleContainerBackground: Color get() = extension.AppSettingsToggleContainerBackground
    val AppSettingsToggleChipSelectedBackground: Color get() = extension.AppSettingsToggleChipSelectedBackground
    val AppSettingsToggleTextUnselected: Color get() = extension.AppSettingsToggleTextUnselected
    val AppSettingsToggleTextSelected: Color get() = extension.AppSettingsToggleTextSelected
    val AppSettingsLogoutAccent: Color get() = extension.AppSettingsLogoutAccent
    val AppSettingsLogoutIconBackground: Color get() = extension.AppSettingsLogoutIconBackground

    val CaloriesAccentTeal1200: Color get() = extension.CaloriesAccentTeal1200
    val SectionSubtitle: Color get() = extension.SectionSubtitle
    val CaloriesMutedTeal: Color get() = extension.CaloriesMutedTeal
    val CaloriesIconOnAccent: Color get() = extension.CaloriesIconOnAccent
    val ExerciseSecondaryText: Color get() = extension.ExerciseSecondaryText

    val ExerciseCardBackground: Color get() = extension2.ExerciseCardBackground
    val ExerciseCardTitle: Color get() = extension2.ExerciseCardTitle
    val ExerciseCardSubtitle: Color get() = extension2.ExerciseCardSubtitle
    val ExerciseCardChevron: Color get() = extension2.ExerciseCardChevron
    val ExerciseChipSelectedBg: Color get() = extension2.ExerciseChipSelectedBg
    val ExerciseChipSelectedBorder: Color get() = extension2.ExerciseChipSelectedBorder
    val ExerciseChipSelectedText: Color get() = extension2.ExerciseChipSelectedText
    val ExerciseChipUnselectedBg: Color get() = extension2.ExerciseChipUnselectedBg
    val ExerciseChipUnselectedBorder: Color get() = extension2.ExerciseChipUnselectedBorder
    val ExerciseChipUnselectedText: Color get() = extension2.ExerciseChipUnselectedText
    val ExerciseInstructionsTitle: Color get() = extension2.ExerciseInstructionsTitle
    val ExerciseInstructionsBody: Color get() = extension2.ExerciseInstructionsBody
    val ExerciseInstructionsBullet: Color get() = extension2.ExerciseInstructionsBullet
    val ExerciseCancelWorkoutText: Color get() = extension2.ExerciseCancelWorkoutText

    val ExerciseScreenTitle: Color get() = extension2.ExerciseScreenTitle
    val ExerciseBackButtonTint: Color get() = extension2.ExerciseBackButtonTint
    val ExerciseSearchPlaceholder: Color get() = extension2.ExerciseSearchPlaceholder
    val ExerciseWorkoutHeaderTitle: Color get() = extension2.ExerciseWorkoutHeaderTitle
    val ExerciseWorkoutImageBackground: Color get() = extension2.ExerciseWorkoutImageBackground
    val ExerciseWorkoutTimerText: Color get() = extension2.ExerciseWorkoutTimerText
    val ExerciseWorkoutPrimaryButtonText: Color get() = extension2.ExerciseWorkoutPrimaryButtonText
    val ExerciseWorkoutTotalTimeLabel: Color get() = extension2.ExerciseWorkoutTotalTimeLabel
    val ExerciseSetsRepsCardBg: Color get() = extension2.ExerciseSetsRepsCardBg
    val ExerciseSetsRepsCardBorder: Color get() = extension2.ExerciseSetsRepsCardBorder
    val ExerciseSetsRepsBtnBg: Color get() = extension2.ExerciseSetsRepsBtnBg
    val ExerciseSetsRepsLabelColor: Color get() = extension2.ExerciseSetsRepsLabelColor
    val ExerciseSetsRepsValueColor: Color get() = extension2.ExerciseSetsRepsValueColor
    val ExerciseReadMoreColor: Color get() = extension2.ExerciseReadMoreColor



    val ProductDetailScanDate: Color get() = extension2.ProductDetailScanDate
    val ProductDetailScanDateBadgeBg: Color get() = extension2.ProductDetailScanDateBadgeBg
    val ProductDetailScanDateBadgeText: Color get() = extension2.ProductDetailScanDateBadgeText
    val ProductDetailSafetyText: Color get() = extension2.ProductDetailSafetyText
    val ProductDetailTitleText: Color get() = extension2.ProductDetailTitleText
    val ProductDetailSafetyReasonText: Color get() = extension2.ProductDetailSafetyReasonText
    val ProductDetailWhyNotSafeText: Color get() = extension2.ProductDetailWhyNotSafeText
    val ProductDetailIngredientCardBorder: Color get() = extension2.ProductDetailIngredientCardBorder
    val ProductDetailIngredientReasonText: Color get() = extension2.ProductDetailIngredientReasonText
    val ProductDetailMatchTag: Color get() = extension2.ProductDetailMatchTag
    val ProductDetailMatchTagText: Color get() = extension2.ProductDetailMatchTagText
    val ProductDetailNutritionCardBg: Color get() = extension2.ProductDetailNutritionCardBg
    val ProductDetailNutritionLabelText: Color get() = extension2.ProductDetailNutritionLabelText
    val ProductDetailNutritionText: Color get() = extension2.ProductDetailNutritionText
    val VerdictCautionBackground: Color get() = extension2.VerdictCautionBackground
    val VerdictCautionText: Color get() = extension2.VerdictCautionText
    val VerdictUnsafeBackground: Color get() = extension2.VerdictUnsafeBackground
    val VerdictUnsafeText: Color get() = extension2.VerdictUnsafeText
    val ScrimOverlay: Color get() = extension2.ScrimOverlay

    val ChatScreenBackground: Color get() = extension2.ChatScreenBackground
    val ChatScreenBackgroundEnd: Color get() = extension2.ChatScreenBackgroundEnd
    val ChatUserBubble: Color get() = extension2.ChatUserBubble
    val ChatUserBubbleText: Color get() = extension2.ChatUserBubbleText
    val ChatBotBubble: Color get() = extension2.ChatBotBubble
    val ChatBotBubbleText: Color get() = extension2.ChatBotBubbleText
    val ChatSourceCardBackground: Color get() = extension2.ChatSourceCardBackground
    val ChatSourceScoreBadge: Color get() = extension2.ChatSourceScoreBadge
    val ChatInputBackground: Color get() = extension2.ChatInputBackground
    val ChatInputPlaceholder: Color get() = extension2.ChatInputPlaceholder
    val ChatSendButtonBackground: Color get() = extension2.ChatSendButtonBackground
    val ChatSendButtonIcon: Color get() = extension2.ChatSendButtonIcon
    val ChatDisclaimerBackground: Color get() = extension2.ChatDisclaimerBackground
    val ChatDisclaimerText: Color get() = extension2.ChatDisclaimerText
    val ChatDisclaimerIcon: Color get() = extension2.ChatDisclaimerIcon

    val StepHistoryTopBarIconBg: Color get() = extension3.StepHistoryTopBarIconBg
    val StepHistoryTitle: Color get() = extension3.StepHistoryTitle
    val StepHistoryGaugeTrack: Color get() = extension3.StepHistoryGaugeTrack
    val StepHistoryGaugeFill: Color get() = extension3.StepHistoryGaugeFill
    val StepHistoryGaugeLabelText: Color get() = extension3.StepHistoryGaugeLabelText
    val StepHistoryChipContainerBg: Color get() = extension3.StepHistoryChipContainerBg
    val StepHistoryChipBgSelected: Color get() = extension3.StepHistoryChipBgSelected
    val StepHistoryChipBgUnselected: Color get() = extension3.StepHistoryChipBgUnselected
    val StepHistoryChipTextUnselected: Color get() = extension3.StepHistoryChipTextUnselected
    val StepHistoryChartBarBg: Color get() = extension3.StepHistoryChartBarBg
    val StepHistoryChartBarFill: Color get() = extension3.StepHistoryChartBarFill
    val StepHistorySummaryIconBg: Color get() = extension3.StepHistorySummaryIconBg

    // News Screen Redesign
    val NewsChipSelectedBg: Color get() = extension3.NewsChipSelectedBg
    val NewsChipSelectedText: Color get() = extension3.NewsChipSelectedText
    val NewsChipUnselectedBg: Color get() = extension3.NewsChipUnselectedBg
    val NewsChipUnselectedBorder: Color get() = extension3.NewsChipUnselectedBorder
    val NewsChipUnselectedText: Color get() = extension3.NewsChipUnselectedText
    val NewsCategoryLabel: Color get() = extension3.NewsCategoryLabel
    val NewsCardTitle: Color get() = extension3.NewsCardTitle
    val NewsSourceText: Color get() = extension3.NewsSourceText
    val NewsSourceAvatarBg: Color get() = extension3.NewsSourceAvatarBg
    val NewsSearchBarBorder: Color get() = extension3.NewsSearchBarBorder
    val NewsSearchIconTint: Color get() = extension3.NewsSearchIconTint
    val NewsDivider: Color get() = extension3.NewsDivider
    val NewsScreenTitle: Color get() = extension3.NewsScreenTitle
    val NewsCardBg: Color get() = extension3.NewsCardBg
    val NewsCardBorder: Color get() = extension3.NewsCardBorder

    // Calories History
    val CaloriesHistoryTopBarIconBg: Color get() = extension3.CaloriesHistoryTopBarIconBg
    val CaloriesHistoryTitle: Color get() = extension3.CaloriesHistoryTitle
    val CaloriesHistoryDateText: Color get() = extension3.CaloriesHistoryDateText
    val CaloriesHistoryCardBorder: Color get() = extension3.CaloriesHistoryCardBorder
    val CaloriesHistoryStatLabel: Color get() = extension3.CaloriesHistoryStatLabel
    val CaloriesHistoryStatValue: Color get() = extension3.CaloriesHistoryStatValue
    val CaloriesHistoryStatSecondary: Color get() = extension3.CaloriesHistoryStatSecondary
    val CaloriesHistoryCalendarIconBg: Color get() = extension3.CaloriesHistoryCalendarIconBg
    val CaloriesHistoryCalendarIconTint: Color get() = extension3.CaloriesHistoryCalendarIconTint
    val CaloriesHistoryOuterCardBg: Color get() = extension3.CaloriesHistoryOuterCardBg
    val CaloriesHistoryDateChipBg: Color get() = extension3.CaloriesHistoryDateChipBg
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
    ScreenSurfaceBackground = Color(0xFFF8F8F9), // Gray100
    MenuSectionLabel = Color(0xFFC0C0C0), // Gray500
    MenuIconContainerBackground = Color(0xFFD4F1F2), // Teal200
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
    MethodCardIconBgUnselected = Color(0xFFE8FAFA), // Teal100
    MethodCardIconTintUnselected = Color(0xFF6A6A6A), // Gray1000
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
    GenderMaleCardText = Color(0xFF0F474A), // Teal1600

    // Page Indicator
    PageIndicatorTotal = Color(0xFFA6A5A5), // Gray600

    // Date of Birth
    DobAgeBadgeBackground = Color(0xFF2FC5CC), // Teal700
    DobAgeBadgeText = Color(0xFF0F474A), // Teal1600
    DobCardText = Color(0xFF0F474A), // Teal1600
    DobInputBorder = Color(0xFFE5E5E4), // Gray300
    DobInputText = Color(0xFFA6A5A5), // Gray600
    DobCalendarIcon = Color(0xFFE5E5E4), // Gray300

    // Height Selection
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

    // Shadows
    ShadowSelected = Color(0x6613A4AB), // Teal1000/Primary 40% alpha
    ShadowUnselected = Color(0x3313A4AB), // Teal1000/Primary 20% alpha
    ShadowFemaleSelected = Color(0x6613A4AB),
    ShadowMaleSelected = Color(0x6613A4AB),
    ShadowDobActive = Color(0x6613A4AB),

    WaterEmptyGlassTint = Color(0xFFD6D6D5), // Gray400
    WaterAddButtonBackground = Color(0xFFF1F1F1), // Gray200
    WaterAddButtonIconTint = Color(0xFF6A6A6A), // Gray1000

    extension = AppColorsExtension(
        // User Profile Screen — light values match the Figma light spec
        ProfileHeaderBackground = Color(0xFF13A4AB), // Teal1000 — screen bg behind header
        ProfileHeaderAccent = Color(0xFF11939A), // Teal1200 — avatar ring
        ProfileSheetBackground = Color(0xFFFFFFFF), // White — main sheet
        ProfileFamilyBoxBackground = Color(0xFFFFFFFF), // White — dashed container fill
        ProfileMemberCardBackground = Color(0xFFE8FAFA), // Teal100
        ProfileMemberCardBorder = Color(0xFF11939A), // Teal1200 — solid teal border
        ProfileAddIconBackground = Color(0xFFFFFFFF), // White
        ProfileAddText = Color(0xFF0F474A), // Teal1600
        ProfileStreakBadgeBackground = Color(0xFF11939A), // Teal1200 — fixed both themes
        ProfileHeaderEdge = Color(0xFF11939A), // Teal1200 — decorative edge shape
        ProfileShowDetailsBackground = Color(0xFF17B8BE), // Teal800 — fixed both themes
        ProfileMenuChevron = Color(0xFFC0C0C0), // Gray500
        ProfileAddMemberAvatarBackground = Color(0xFF11939A), // Teal1200 - matches settings avatar in light theme

        // Edit Profile Input Fields
        EditProfileInputBackground = Color(0xFFFFFFFF), // White / Background
        EditProfileInputBorder = Color(0xFFE5E5E4), // Gray300 / Divider

        // Gender Selection
        GenderFemaleCardBackground = Color(0xFFFFF1F3), // Red100 equivalent
        GenderFemaleCardText = Color(0xFFFA4D5E), // #FA4D5E
        GenderMaleCardText = Color(0xFF0F474A), // Teal1600

        // Page Indicator
        PageIndicatorTotal = Color(0xFFA6A5A5), // Gray600

        // Date of Birth
        DobAgeBadgeBackground = Color(0xFF2FC5CC), // Teal700
        DobAgeBadgeText = Color(0xFF0F474A), // Teal1600
        DobCardText = Color(0xFF0F474A), // Teal1600
        DobInputBorder = Color(0xFFE5E5E4), // Gray300
        DobInputText = Color(0xFFA6A5A5), // Gray600
        DobCalendarIcon = Color(0xFFE5E5E4), // Gray300

        // Height Selection
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

        // Shadows
        ShadowSelected = Color(0x6613A4AB), // Teal1000/Primary 40% alpha
        ShadowUnselected = Color(0x3313A4AB), // Teal1000/Primary 20% alpha
        ShadowFemaleSelected = Color(0x6613A4AB),
        ShadowMaleSelected = Color(0x6613A4AB),
        ShadowDobActive = Color(0x6613A4AB),
        
        // Product Card
        ProductCardBackground = Color(0xFFFFFFFF), // White
        SavedSearchBarBorder = Color(0xFFC0C0C0), // Gray500
        ProductCardNameText = Color(0xFF13A4AB), // Teal1000
        ProductCardCautionText = Color(0xFF0B5F65), // Teal1400 (matches dark mode surface)
        ProductCardCaloriesBackground = Color(0xFFCAF2F4), // Teal300
        ProductCardCaloriesText = Color(0xFF2FC5CC), // Teal700
        ProductCardSwipeContainerBackground = Color(0xFFF1F1F1), // Gray200
        ProductCardSwipeText = Color(0xFFC0C0C0), // Gray500
        ProductCardShadow = Color(0x3313A4AB), // Teal1000 20% alpha

        // App Settings
        AppSettingsToggleContainerBackground = Color(0xFFE5E5E4), // Gray300
        AppSettingsToggleChipSelectedBackground = Color(0xFFFFFFFF), // White
        AppSettingsToggleTextUnselected = Color(0xFF777777), // Gray800
        AppSettingsToggleTextSelected = Color(0xFF3E3E3E), // Gray1400 (matches ChipTextSelected)
        AppSettingsLogoutAccent = Color(0xFFFA4D5E),
        AppSettingsLogoutIconBackground = Color(0xFFFFF1F3), // ErrorBackground (light)

        // Calories Dashboard — fixed literals, identical in both themes (see AppColorsExtension kdoc)
        CaloriesAccentTeal1200 = Color(0xFF11939A), // Teal/1200 swatch literal
        SectionSubtitle = Color(0xFF11939A), // Teal/1200 — unified title/sub-title color; fixed, identical in both themes (see docs/plans/2026-08-08-unify-section-subtitle-color-v2.md)
        CaloriesMutedTeal = Color(0xFFD4F1F2), // Teal/200 swatch literal
        CaloriesIconOnAccent = Color(0xFFFFFFFF),
        ExerciseSecondaryText = Color(0xFF3E4949), // literal from Figma, light mode only
    ),
    extension2 = AppColorsExtension2(

        // Exercise Screens — light values match current hardcoded colors exactly
        ExerciseCardBackground = Color(0xFFE8FAFA), // Teal100 — exercise list card bg
        ExerciseCardTitle = Color(0xFF0F474A), // Teal1600 — card/bottom sheet exercise name
        ExerciseCardSubtitle = Color(0xFF898989), // Gray700 — card/bottom sheet subtitle
        ExerciseCardChevron = Color(0xFF13A4AB), // Primary — card arrow icon
        ExerciseChipSelectedBg = Color(0xFFFFFFFF), // White
        ExerciseChipSelectedBorder = Color(0xFF13A4AB), // Primary
        ExerciseChipSelectedText = Color(0xFF13A4AB), // Primary
        ExerciseChipUnselectedBg = Color(0xFFFFFFFF), // White
        ExerciseChipUnselectedBorder = Color(0xFFE5E5E4), // Gray300
        ExerciseChipUnselectedText = Color(0xFF898989), // Gray700
        ExerciseInstructionsTitle = Color(0xFF0F474A), // Teal1600
        ExerciseInstructionsBody = Color(0xFF9E9E9E), // Lighter grey
        ExerciseInstructionsBullet = Color(0xFF0F474A), // Teal1600
        ExerciseCancelWorkoutText = Color(0xFF9E9E9E), // Lighter grey

        ExerciseScreenTitle = Color(0xFF393C3C), // TextPrimary in light mode
        ExerciseBackButtonTint = Color(0xFF13A4AB), // Teal1000 in light mode
        ExerciseSearchPlaceholder = Color(0x80393C3C), // TextPrimary with 0.5 alpha
        ExerciseWorkoutHeaderTitle = Color(0xFF393C3C), // TextPrimary in light mode
        ExerciseWorkoutImageBackground = Color(0x66D4F1F2), // Teal200 with 0.4 alpha
        ExerciseWorkoutTimerText = Color(0xFF393C3C), // TextPrimary
        ExerciseWorkoutPrimaryButtonText = Color(0xFFFFFFFF), // OnPrimary
        ExerciseWorkoutTotalTimeLabel = Color(0xFF777777), // TextSecondary
        ExerciseSetsRepsCardBg = Color(0x99D4F1F2), // Teal200 with 0.6 alpha
        ExerciseSetsRepsCardBorder = Color(0xFFD4F1F2), // Teal200
        ExerciseSetsRepsBtnBg = Color(0xFFD4F1F2), // Teal200
        ExerciseSetsRepsLabelColor = Color(0xFF393C3C), // TextPrimary
        ExerciseSetsRepsValueColor = Color(0xFF393C3C), // TextPrimary
        ExerciseReadMoreColor = Color(0xFF13A4AB), // Primary



        // Product Details
        ProductDetailScanDate = Color(0xFF898989), // Gray700
        ProductDetailScanDateBadgeBg = Color(0xFFE8FAFA), // Teal100
        ProductDetailScanDateBadgeText = Color(0xFF13A4AB), // Primary (Teal1000)
        ProductDetailSafetyText = Color(0xFF393C3C), // Gray1600

        ProductDetailTitleText = Color(0xFF13A4AB), // Teal1000
        ProductDetailSafetyReasonText = Color(0xFF777777), // Gray800
        ProductDetailWhyNotSafeText = Color(0xFF393C3C), // Gray1600

        ProductDetailIngredientCardBorder = Color(0xFFE5E5E5), // Lighter border? or transparent
        ProductDetailIngredientReasonText = Color(0xFF757575), // Gray600

        ProductDetailMatchTag = Color(0xFFC0C0C0), // Gray500
        ProductDetailMatchTagText = Color(0xFFF8F8F9), // Gray100

        ProductDetailNutritionCardBg = Color(0xFFF8F8F9), // Gray100
        ProductDetailNutritionLabelText = Color(0xFF777777), // Gray800
        ProductDetailNutritionText = Color(0xFFF8F8F9), // Gray100

        VerdictCautionBackground = Color(0xFFFFCC00),
        VerdictCautionText = Color(0xFFFFFFFF),
        VerdictUnsafeBackground = Color(0xFFFA4D5E),
        VerdictUnsafeText = Color(0xFFFFFFFF),

        ScrimOverlay = Color(0x660F474A), // Teal1600 40% alpha — light mode scrim
        
        // NutriGPT Chat (Light Mode)
        ChatScreenBackground = Color(0xFFFFFFFF), // White
        ChatScreenBackgroundEnd = Color(0xFFFFFFFF), // White
        ChatUserBubble = Color(0xFF2FC5CC), // Teal700
        ChatUserBubbleText = Color(0xFFFFFFFF), // White
        ChatBotBubble = Color(0xFFE8FAFA), // Teal100
        ChatBotBubbleText = Color(0xFF0F474A), // Teal1600
        ChatSourceCardBackground = Color(0xFFE8FAFA), // Teal100
        ChatSourceScoreBadge = Color.Transparent, // No background in light mode
        ChatInputBackground = Color(0xFFFFFFFF), // White
        ChatInputPlaceholder = Color(0xFFA6A5A5), // Gray600
        ChatSendButtonBackground = Color(0xFFD6D6D5), // Gray400
        ChatSendButtonIcon = Color(0xFFFFFFFF), // White
        ChatDisclaimerBackground = Color(0xFFF8F8F9), // Gray100
        ChatDisclaimerText = Color(0xFF777777), // Gray800
        ChatDisclaimerIcon = Color(0xFF13A4AB), // Teal1000
    ),
    extension3 = AppColorsExtension3(
        StepHistoryTopBarIconBg = Color(0xFFD4F1F2),
        StepHistoryTitle = Color(0xFF0A545A),

        StepHistoryGaugeTrack = Color(0xFFD4F1F2),
        StepHistoryGaugeFill = Color(0xFF13A4AB),
        StepHistoryGaugeLabelText = Color(0xFF393C3C),

        StepHistoryChipContainerBg = Color(0xFFE5E5E4),
        StepHistoryChipBgSelected = Color(0xFFFFFFFF),
        StepHistoryChipBgUnselected = Color.Transparent,
        StepHistoryChipTextUnselected = Color(0xFF777777),

        StepHistoryChartBarBg = Color(0xFFE8FAFA),
        StepHistoryChartBarFill = Color(0xFF13A4AB),

        StepHistorySummaryIconBg = Color(0xFFE8FAFA),

        // News Screen Redesign (Light Mode)
        NewsChipSelectedBg = Color(0xFF17B8BE), // Teal800
        NewsChipSelectedText = Color(0xFFFFFFFF), // White
        NewsChipUnselectedBg = Color(0xFFFFFFFF), // White
        NewsChipUnselectedBorder = Color(0xFFD6D6D5), // Gray400
        NewsChipUnselectedText = Color(0xFF898989), // Gray700
        NewsCategoryLabel = Color(0xFF17B8BE), // Teal800
        NewsCardTitle = Color(0xFF393C3C), // Gray1600
        NewsSourceText = Color(0xFF898989), // Gray700
        NewsSourceAvatarBg = Color(0xFFD4F1F2), // Teal200 (light bg for icon)
        NewsSearchBarBorder = Color(0xFFD6D6D5), // Gray400
        NewsSearchIconTint = Color(0xFF898989), // Gray700
        NewsDivider = Color(0xFFE5E5E4), // Gray300
        NewsScreenTitle = Color(0xFF393C3C), // Gray1600
        NewsCardBg = Color(0xFFFFFFFF), // White
        NewsCardBorder = Color(0xFFE5E5E4), // Gray300

        // Calories History (Light Mode)
        CaloriesHistoryTopBarIconBg = Color(0xFFD4F1F2),    // Teal200
        CaloriesHistoryTitle = Color(0xFF000000),           // Black
        CaloriesHistoryDateText = Color(0xFF11939A),        // Teal1200
        CaloriesHistoryCardBorder = Color(0xFFE5E5E4),      // Gray300
        CaloriesHistoryStatLabel = Color(0xFF11939A),       // Teal1200
        CaloriesHistoryStatValue = Color(0xFF000000),       // Black
        CaloriesHistoryStatSecondary = Color(0xFF898989),   // Gray700
        CaloriesHistoryCalendarIconBg = Color(0xFFCAF2F4),  // Teal300
        CaloriesHistoryCalendarIconTint = Color(0xFF75DEE3),// Teal500
        CaloriesHistoryOuterCardBg = Color(0xFFF1F1F1),     // Gray200
        CaloriesHistoryDateChipBg = Color(0xFFFFFFFF),      // White date chip
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
    ScreenSurfaceBackground = Color(0xFF0F474A), // Background / Teal1600
    MenuSectionLabel = Color(0xFF11939A), // Teal1200
    MenuIconContainerBackground = Color(0xFF0F474A), // Background / Teal1600
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
    MethodCardIconBgUnselected = Color(0xFF108188), // Teal1300
    MethodCardIconTintUnselected = Color(0xFFC0C0C0), // Gray500
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
    GenderMaleCardText = Color(0xFF0F474A), // Teal1600

    // Page Indicator
    PageIndicatorTotal = Color(0xFF108188), // Teal1300

    // Date of Birth
    DobAgeBadgeBackground = Color(0xFF2FC5CC), // Teal700
    DobAgeBadgeText = Color(0xFF0F474A), // Teal1600
    DobCardText = Color(0xFF0F474A), // Teal1600
    DobInputBorder = Color(0xFF169098), // Teal1200
    DobInputText = Color(0xFF169098), // Teal1200
    DobCalendarIcon = Color(0xFF169098), // Teal1200

    // Height Selection
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

    // Shadows
    ShadowSelected = Color(0x9975DEE3), // Teal500/Accent 60% alpha (bright cyan)
    ShadowUnselected = Color(0x33FFFFFF), // Soft white glow 20% alpha
    ShadowFemaleSelected = Color(0x99FF80AB), // Pink glow
    ShadowMaleSelected = Color(0x9975DEE3),
    ShadowDobActive = Color(0x9975DEE3),

    WaterEmptyGlassTint = Color(0xFF108188), // Teal1300
    WaterAddButtonBackground = Color(0xFF0F474A), // Teal1600
    WaterAddButtonIconTint = Color(0xFF13A4AB), // Teal1000

    extension = AppColorsExtension(
        // User Profile Screen — dark values match the Figma dark spec
        ProfileHeaderBackground = Color(0xFF0A545A), // Teal1500 — screen bg behind header
        ProfileHeaderAccent = Color(0xFF0B5F65), // Teal1400 — avatar ring
        ProfileSheetBackground = Color(0xFF0F474A), // Teal1600 — main sheet
        ProfileFamilyBoxBackground = Color(0xFF0A545A), // Teal1500 — dashed container fill
        ProfileMemberCardBackground = Color(0xFF0F474A), // Teal1600 — elevated deep teal
        ProfileMemberCardBorder = Color(0xFF11939A), // Teal1200 — solid teal border
        ProfileAddIconBackground = Color(0xFFE8FAFA), // Teal100
        ProfileAddText = Color(0xFFE8FAFA), // Teal100
        ProfileStreakBadgeBackground = Color(0xFF11939A), // Teal1200 — fixed both themes
        ProfileHeaderEdge = Color(0xFF0B5F65), // Teal1400 — decorative edge shape
        ProfileShowDetailsBackground = Color(0xFF17B8BE), // Teal800 — fixed both themes
        ProfileMenuChevron = Color(0xFF13A4AB), // Teal1000
        ProfileAddMemberAvatarBackground = Color(0xFF0F474A), // Teal1600 - darker background in dark theme

        // Edit Profile Input Fields
        EditProfileInputBackground = Color(0xFF0A545A), // Teal1500 / SurfaceVariant
        EditProfileInputBorder = Color(0xFF108188), // Teal1300 / OtherChipBorder

        // Gender Selection
        GenderFemaleCardBackground = Color(0xFF0B5F65), // Teal1400
        GenderFemaleCardText = Color(0xFFFDE8E8), // Red100
        GenderMaleCardText = Color(0xFF0F474A), // Teal1600

        // Page Indicator
        PageIndicatorTotal = Color(0xFF108188), // Teal1300

        // Date of Birth
        DobAgeBadgeBackground = Color(0xFF2FC5CC), // Teal700
        DobAgeBadgeText = Color(0xFF0F474A), // Teal1600
        DobCardText = Color(0xFF0F474A), // Teal1600
        DobInputBorder = Color(0xFF169098), // Teal1200
        DobInputText = Color(0xFF169098), // Teal1200
        DobCalendarIcon = Color(0xFF169098), // Teal1200

        // Height Selection
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

        // Shadows
        ShadowSelected = Color(0x9975DEE3), // Teal500/Accent 60% alpha (bright cyan)
        ShadowUnselected = Color(0x33FFFFFF), // Soft white glow 20% alpha
        ShadowFemaleSelected = Color(0x99FF80AB), // Pink glow
        ShadowMaleSelected = Color(0x9975DEE3),
        ShadowDobActive = Color(0x9975DEE3),
        
        // Product Card
        ProductCardBackground = Color(0xFF0A545A), // Teal1500
        SavedSearchBarBorder = Color(0xFF0F8389), // Teal1200
        ProductCardNameText = Color(0xFF75DEE3), // Teal500
        ProductCardCautionText = Color(0xFF0B5F65), // Teal1400
        ProductCardCaloriesBackground = Color(0xFFCAF2F4), // Teal300
        ProductCardCaloriesText = Color(0xFF0F474A), // Teal1600
        ProductCardSwipeContainerBackground = Color(0xFF0A545A), // Teal1500
        ProductCardSwipeText = Color(0xFF108188), // Teal1300
        ProductCardShadow = Color(0x8013A4AB), // Teal1000 50% alpha

        // App Settings
        // Teal200 is remapped to 0xFF2FC5CC in this palette for glowing shadows/badges — cannot reuse for this
        // Teal1200 is remapped to 0xFFA3E9EC in this palette for secondary text — cannot reuse for this
        AppSettingsToggleContainerBackground = Color(0xFF0F474A), // Teal1600
        AppSettingsToggleChipSelectedBackground = Color(0xFF13A4AB), // Teal1000
        AppSettingsToggleTextUnselected = Color(0xFFA3E9EC), // Teal400
        AppSettingsToggleTextSelected = Color(0xFFA3E9EC), // Teal400 — same as unselected, differentiated by chip bg
        // Error is remapped to 0xFFFF6B7A in this palette — spec requires the same red in both themes here
        AppSettingsLogoutAccent = Color(0xFFFA4D5E),
        AppSettingsLogoutIconBackground = Color(0xFF0A545A), // Teal1500 — ErrorBackground dark value doesn't fit here

        // Calories Dashboard — fixed literals, identical in both themes (see AppColorsExtension kdoc)
        CaloriesAccentTeal1200 = Color(0xFF11939A), // Teal/1200 swatch literal
        SectionSubtitle = Color(0xFF11939A), // Teal/1200 — intentionally identical to light mode
        CaloriesMutedTeal = Color(0xFFD4F1F2), // Teal/200 swatch literal
        CaloriesIconOnAccent = Color(0xFFFFFFFF),
        ExerciseSecondaryText = Color(0xFF11939A), // Teal/1200 literal, matches dark mode's own Figma value
    ),
    extension2 = AppColorsExtension2(

        // Exercise Screens — dark values from Figma dark mode screenshots
        ExerciseCardBackground = Color(0xFF0B5F65), // Medium-dark teal card bg (#0B5F65)
        ExerciseCardTitle = Color(0xFFA3E9EC), // Light text on dark (#A3E9EC)
        ExerciseCardSubtitle = Color(0xFF11939A), // Muted teal subtitle (#11939A)
        ExerciseCardChevron = Color(0xFF47D3D9), // Teal600 — bright teal arrow
        ExerciseChipSelectedBg = Color.Transparent, // Transparent background in dark
        ExerciseChipSelectedBorder = Color(0xFF11939A), // Teal active border (#11939A)
        ExerciseChipSelectedText = Color(0xFF11939A), // Teal active text (#11939A)
        ExerciseChipUnselectedBg = Color.Transparent, // Transparent unselected background
        ExerciseChipUnselectedBorder = Color(0xFF0B5F65), // Unselected border (#0B5F65)
        ExerciseChipUnselectedText = Color(0xFF0B5F65), // Unselected text (#0B5F65)
        ExerciseInstructionsTitle = Color(0xFF11939A), // Instructions title (#11939A)
        ExerciseInstructionsBody = Color(0xFF11939A), // Instructions body text (#11939A)
        ExerciseInstructionsBullet = Color(0xFF11939A), // Bullet dot color (#11939A)
        ExerciseCancelWorkoutText = Color(0xFF108188), // Cancel workout text (#108188)

        ExerciseScreenTitle = Color(0xFF11939A), // Exercises title (#11939A)
        ExerciseBackButtonTint = Color(0xFF11939A), // Back button tint (#11939A)
        ExerciseSearchPlaceholder = Color(0xFF11939A), // Search here text (#11939A)
        ExerciseWorkoutHeaderTitle = Color(0xFF11939A), // Workout header & name above image (#11939A)
        ExerciseWorkoutImageBackground = Color(0xFF0B5F65), // Image circular bg (#0B5F65)
        ExerciseWorkoutTimerText = Color(0xFFA3E9EC), // Timer text (#A3E9EC)
        ExerciseWorkoutPrimaryButtonText = Color(0xFFFFFFFF), // Pause text color (White)
        ExerciseWorkoutTotalTimeLabel = Color(0xFF108188), // Total Time label (#108188)
        ExerciseSetsRepsCardBg = Color(0xFF0B5F65), // Card bg (#0B5F65)
        ExerciseSetsRepsCardBorder = Color(0xFF0B5F65), // Card border (#0B5F65)
        ExerciseSetsRepsBtnBg = Color(0xFF0F474A), // Plus/Minus button bg (#0F474A)
        ExerciseSetsRepsLabelColor = Color(0xFF13A4AB), // Label text (#13A4AB)
        ExerciseSetsRepsValueColor = Color(0xFF13A4AB), // Value text (#13A4AB)
        ExerciseReadMoreColor = Color(0xFF11939A), // Read more text color (#11939A)



        // Product Details
        ProductDetailScanDate = Color(0xFFA3E9EC), // Teal400
        ProductDetailScanDateBadgeBg = Color(0xFFD4F1F2), // Teal200
        ProductDetailScanDateBadgeText = Color(0xFF17B8BE), // Teal800
        ProductDetailSafetyText = Color(0xFF13A4AB), // Teal1000

        ProductDetailTitleText = Color(0xFFA3E9EC), // Teal400
        ProductDetailSafetyReasonText = Color(0xFF11939A), // Teal1200
        ProductDetailWhyNotSafeText = Color(0xFF13A4AB), // Teal1000

        ProductDetailIngredientCardBorder = Color(0xFF11939A), // Teal1200
        ProductDetailIngredientReasonText = Color(0xFF108188), // Teal1300

        ProductDetailMatchTag = Color(0xFF0F474A), // Teal1600
        ProductDetailMatchTagText = Color(0xFF11939A), // Teal1200

        ProductDetailNutritionCardBg = Color(0xFF0B5F65), // Teal1400
        ProductDetailNutritionLabelText = Color(0xFF11939A), // Teal1200
        ProductDetailNutritionText = Color(0xFFF8F8F9), // Gray100

        VerdictCautionBackground = Color(0xFFFFCC00),
        VerdictCautionText = Color(0xFFFFFFFF),
        VerdictUnsafeBackground = Color(0xFFFA4D5E),
        VerdictUnsafeText = Color(0xFFFFFFFF),

        ScrimOverlay = Color(0x99000000), // Black 60% alpha — dark mode scrim
        
        // NutriGPT Chat (Dark Mode)
        ChatScreenBackground = Color(0xFF0A545A), // Teal1500
        ChatScreenBackgroundEnd = Color(0xFF0F474A), // Teal1600
        ChatUserBubble = Color(0xFF0B5F65), // Teal1400
        ChatUserBubbleText = Color(0xFFE8FAFA), // Teal100
        ChatBotBubble = Color(0x33108188), // Teal1300 at 20%
        ChatBotBubbleText = Color(0xFFE8FAFA), // Teal100
        ChatSourceCardBackground = Color(0x4D0F474A), // Teal1600 at 30%
        ChatSourceScoreBadge = Color(0xFF75DEE3), // Teal500
        ChatInputBackground = Color(0x330F474A), // Teal1600 at 20%
        ChatInputPlaceholder = Color(0xFF108188), // Teal1300
        ChatSendButtonBackground = Color(0xFF11939A), // Teal1200
        ChatSendButtonIcon = Color(0xFFE8FAFA), // Teal100
        ChatDisclaimerBackground = Color(0x4D0F474A), // Teal1600 at 30%
        ChatDisclaimerText = Color(0xFFE8FAFA), // Teal100
        ChatDisclaimerIcon = Color(0xFF75DEE3), // Teal500
    ),
    extension3 = AppColorsExtension3(
        StepHistoryTopBarIconBg = Color(0xFF0B5F65),
        StepHistoryTitle = Color(0xFFE8FAFA),

        StepHistoryGaugeTrack = Color(0xFF0F474A),
        StepHistoryGaugeFill = Color(0xFF75DEE3),
        StepHistoryGaugeLabelText = Color(0xFFA6A5A5),

        StepHistoryChipContainerBg = Color(0xFF0A545A),
        StepHistoryChipBgSelected = Color(0xFF13A4AB),
        StepHistoryChipBgUnselected = Color.Transparent,
        StepHistoryChipTextUnselected = Color(0xFFA3E9EC),

        StepHistoryChartBarBg = Color(0xFF0F474A),
        StepHistoryChartBarFill = Color(0xFF75DEE3),

        StepHistorySummaryIconBg = Color(0xFF0F474A),

        // News Screen Redesign (Dark Mode)
        NewsChipSelectedBg = Color(0xFF17B8BE), // Teal800
        NewsChipSelectedText = Color(0xFFFFFFFF), // White
        NewsChipUnselectedBg = Color(0xFF0F474A), // Teal1600
        NewsChipUnselectedBorder = Color(0xFF0B5F65), // Teal1400
        NewsChipUnselectedText = Color(0xFFA3E9EC), // Teal400
        NewsCategoryLabel = Color(0xFF17B8BE), // Teal800
        NewsCardTitle = Color(0xFFCAF2F4), // Teal300
        NewsSourceText = Color(0xFF2FC5CC), // Teal700
        NewsSourceAvatarBg = Color(0xFF0B5F65), // Teal1400
        NewsSearchBarBorder = Color(0xFF0B5F65), // Teal1400
        NewsSearchIconTint = Color(0xFFA3E9EC), // Teal400
        NewsDivider = Color(0xFF0B5F65), // Teal1400
        NewsScreenTitle = Color(0xFFCAF2F4), // Teal300
        NewsCardBg = Color(0xFF0A545A), // Teal1500
        NewsCardBorder = Color(0xFF0B5F65), // Teal1400

        // Calories History (Dark Mode)
        CaloriesHistoryTopBarIconBg = Color(0xFF0B5F65),     // Teal1400
        CaloriesHistoryTitle = Color(0xFF13A4AB),            // Teal1000
        CaloriesHistoryDateText = Color(0xFF13A4AB),         // Teal1000
        CaloriesHistoryCardBorder = Color(0xFF11939A),       // Teal1200
        CaloriesHistoryStatLabel = Color(0xFF13A4AB),        // Teal1000
        CaloriesHistoryStatValue = Color(0xFF2FC5CC),        // Teal700
        CaloriesHistoryStatSecondary = Color(0xFF108188),    // Teal1300
        CaloriesHistoryCalendarIconBg = Color(0xFF0A545A),   // Teal1500
        CaloriesHistoryCalendarIconTint = Color(0xFF11939A), // Teal1200
        CaloriesHistoryOuterCardBg = Color(0xFF0A545A),      // Teal1500
        CaloriesHistoryDateChipBg = Color(0xFF0A545A),       // Teal1500
    ),
)
