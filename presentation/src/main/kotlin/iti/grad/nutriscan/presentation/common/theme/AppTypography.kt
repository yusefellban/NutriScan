package iti.grad.nutriscan.presentation.common.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import iti.grad.presentation.R

val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold)
)

val LexendDeca = FontFamily(
    Font(R.font.lexend_deca_light, FontWeight.Light),
    Font(R.font.lexend_deca_regular, FontWeight.Normal),
    Font(R.font.lexend_deca_medium, FontWeight.Medium),
    Font(R.font.lexend_deca_semibold, FontWeight.SemiBold)
)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    displayMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    displaySmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = LexendDeca,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = LexendDeca,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = LexendDeca,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = LexendDeca,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = LexendDeca,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = LexendDeca,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)

/**
 * One-off text styles for Home screen composables that don't fit Material3's fixed
 * 15-slot [Typography] scale (which can't gain new named roles) but are reused across
 * 2+ Home screen composables, so they live here rather than being duplicated inline
 * per call site.
 */
object HomeTypography {
    /** "Daily Health Tip" card title — Plus Jakarta Sans SemiBold 14sp/20sp. */
    val dailyTipTitle = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    /** Recent History item product name — Lexend Deca Medium 18sp/22sp. */
    val historyItemTitle = TextStyle(
        fontFamily = LexendDeca,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 22.sp
    )

    /** Recent History item scan date/time — Lexend Deca Light 12sp/15sp. */
    val historyItemDate = TextStyle(
        fontFamily = LexendDeca,
        fontWeight = FontWeight.Light,
        fontSize = 12.sp,
        lineHeight = 15.sp
    )

    /** Verdict badge pill text ("HEALTHY", "PROBIOTIC"...) — Lexend Deca Light 10sp/13sp. */
    val verdictBadge = TextStyle(
        fontFamily = LexendDeca,
        fontWeight = FontWeight.Light,
        fontSize = 10.sp,
        lineHeight = 13.sp
    )
}

/**
 * One-off text styles that don't fit Material3's fixed 15-slot [Typography] scale
 * (which can't gain new named roles) but are reused across 2+ Calories Dashboard
 * composables, so they live here rather than being duplicated inline per call site.
 */
object CaloriesTypography {
    /** "Daily Products" header title — Plus Jakarta Sans SemiBold 22sp/28sp. */
    val headerTitle = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    )

    /** "Calorie Goals" / "Water" section titles — Plus Jakarta Sans Medium 20sp/25sp. */
    val sectionTitle = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 25.sp
    )

    /** Small badge/pill text and the gauge's "Steps" label — Lexend Deca Light 12sp/15sp. */
    val badgeText = TextStyle(
        fontFamily = LexendDeca,
        fontWeight = FontWeight.Light,
        fontSize = 12.sp,
        lineHeight = 15.sp
    )
}

// --- Feature-Specific Typography Tokens ---

/**
 * Typography styles specific to the Product Details screen.
 */
object ProductDetailsTypography {
    val productTitle = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
    )
}
