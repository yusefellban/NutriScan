package iti.grad.nutriscan.presentation.common.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalAppColors = staticCompositionLocalOf { AppColors() }
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
    val colors = AppColors()
    val typography = AppTypography
    val shapes = AppShapes()
    
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Set system status bar and navigation bar colors to Background
                @Suppress("DEPRECATION")
                window.statusBarColor = colors.Background.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = colors.Background.toArgb()
                
                val insetsController = WindowCompat.getInsetsController(window, view)
                // Since our background is very light, we want dark icons (isAppearanceLightStatusBars = true)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    // Material fallback to prevent default purple styling for ripples, selections, etc.
    val materialColorScheme = lightColorScheme(
        primary = colors.Primary,
        secondary = colors.PrimaryVariant,
        tertiary = colors.Accent,
        background = colors.Background,
        surface = colors.Surface,
        error = colors.Error,
        onPrimary = colors.OnPrimary,
        onBackground = colors.TextPrimary,
        onSurface = colors.TextPrimary,
        onError = colors.OnPrimary
    )

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
