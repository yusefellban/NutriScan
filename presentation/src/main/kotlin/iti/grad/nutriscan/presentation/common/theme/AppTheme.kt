package iti.grad.nutriscan.presentation.common.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppColors = staticCompositionLocalOf { lightColors() }
val LocalAppTypography = staticCompositionLocalOf { AppTypography }
val LocalAppShapes = staticCompositionLocalOf { AppShapes() }

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTypography.current

    val shapes: AppShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalAppShapes.current
}

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) darkColors() else lightColors()
    val typography = AppTypography
    val shapes = AppShapes()

    // Material color scheme — used as a safety net for Material3 components
    // (prevents default purple ripples/selection handles bleeding through).
    // Status bar / navigation bar colors are handled by enableEdgeToEdge() in
    // MainActivity — no deprecated window.statusBarColor calls here.
    val materialColorScheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.Primary,
            secondary = colors.PrimaryVariant,
            tertiary = colors.Accent,
            background = colors.Background,
            surface = colors.Surface,
            error = colors.Error,
            onPrimary = colors.OnPrimary,
            onBackground = colors.TextPrimary,
            onSurface = colors.TextPrimary,
            onError = colors.Background,
        )
    } else {
        lightColorScheme(
            primary = colors.Primary,
            secondary = colors.PrimaryVariant,
            tertiary = colors.Accent,
            background = colors.Background,
            surface = colors.Surface,
            error = colors.Error,
            onPrimary = colors.OnPrimary,
            onBackground = colors.TextPrimary,
            onSurface = colors.TextPrimary,
            onError = colors.OnPrimary,
        )
    }

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        LocalAppShapes provides shapes
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = typography,
            content = content
        )
    }
}
