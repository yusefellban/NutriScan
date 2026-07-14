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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalAppColors = staticCompositionLocalOf { AppColors() }
val LocalAppTypography = staticCompositionLocalOf { AppTypography }
val LocalAppShapes = staticCompositionLocalOf { AppShapes() }

private val DarkColors = darkColorScheme(
    primary = Teal1000,
    onPrimary = White,
    background = Teal1600,
    onBackground = Teal400,
    surface = Teal1400,
    onSurface = Teal400,
    onSurfaceVariant = Teal600,
    secondary = Teal500,
    outline = Teal700,
    error = Color(0xFFFA4D5E), // Hygieia Red/50
    onError = White
)

private val LightColors = lightColorScheme(
    primary = Teal1000,
    onPrimary = White,
    background = Teal100,
    onBackground = Gray800,
    surface = White,
    onSurface = Teal1000,
    onSurfaceVariant = Teal1000,
    secondary = Gray1000,
    outline = Teal400,
    error = Color(0xFFFA4D5E), // Hygieia Red/50
    onError = White
)

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
    val colors = AppColors()
    val typography = AppTypography
    val shapes = AppShapes()
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
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
