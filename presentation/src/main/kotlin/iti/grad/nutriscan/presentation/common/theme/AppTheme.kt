package iti.grad.nutriscan.presentation.common.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
    dynamicColor: Boolean = false,
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

    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> materialColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Set system status bar and navigation bar colors to Background
                @Suppress("DEPRECATION")
                window.statusBarColor = colorScheme.background.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = colorScheme.background.toArgb()
                
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        LocalAppShapes provides shapes
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
