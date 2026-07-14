package iti.grad.nutriscan.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import android.app.Activity
import iti.grad.nutriscan.presentation.common.theme.AppTheme

/**
 * Home Screen — placeholder wrapping the theme/typography showcase.
 *
 * This will be replaced by the actual NutriScan Home screen in a future sprint.
 * It exists solely so the Splash → Home navigation is testable end-to-end without
 * a production Home screen being required at this stage.
 */
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    val colors = AppTheme.colors
    val typography = AppTheme.typography

    val semanticColors = listOf(
        "Primary" to colors.Primary,
        "Primary Variant" to colors.PrimaryVariant,
        "Accent" to colors.Accent,
        "Background" to colors.Background,
        "Surface" to colors.Surface,
        "Surface Variant" to colors.SurfaceVariant,
        "On Primary" to colors.OnPrimary,
        "Text Primary" to colors.TextPrimary,
        "Text Secondary" to colors.TextSecondary,
        "Divider" to colors.Divider,
    )

    val alertColors = listOf(
        "Verdict Green" to colors.VerdictGreen,
        "Verdict Yellow" to colors.VerdictYellow,
        "Verdict Red" to colors.VerdictRed,
        "Warning" to colors.Warning,
        "Error" to colors.Error,
        "Error Background" to colors.ErrorBackground,
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.Background)
            .padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp),
    ) {
        item {
            Text(
                text = "Typography Showcase",
                style = typography.displaySmall,
                color = colors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            TypographyRow("Display Large", typography.displayLarge)
            TypographyRow("Display Medium", typography.displayMedium)
            TypographyRow("Display Small", typography.displaySmall)
            TypographyRow("Headline Large", typography.headlineLarge)
            TypographyRow("Headline Medium", typography.headlineMedium)
            TypographyRow("Headline Small", typography.headlineSmall)
            TypographyRow("Title Large", typography.titleLarge)
            TypographyRow("Title Medium", typography.titleMedium)
            TypographyRow("Title Small", typography.titleSmall)
            TypographyRow("Body Large", typography.bodyLarge)
            TypographyRow("Body Medium", typography.bodyMedium)
            TypographyRow("Body Small", typography.bodySmall)
            TypographyRow("Label Large", typography.labelLarge)
            TypographyRow("Label Medium", typography.labelMedium)
            TypographyRow("Label Small", typography.labelSmall)
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Semantic Colors",
                style = typography.displaySmall,
                color = colors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(semanticColors) { (name, color) ->
            ColorRow(name, color)
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Health Verdicts & Alerts",
                style = typography.displaySmall,
                color = colors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(alertColors) { (name, color) ->
            ColorRow(name, color)
        }
    }
}

@Composable
private fun TypographyRow(name: String, style: TextStyle) {
    val colors = AppTheme.colors
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(text = name, style = AppTheme.typography.labelMedium, color = colors.TextSecondary)
        Text(text = "The quick brown fox jumps over the lazy dog", style = style, color = colors.TextPrimary)
    }
}

@Composable
private fun ColorRow(name: String, color: Color) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(AppTheme.shapes.Medium)
                .background(color)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = name,
            style = AppTheme.typography.bodyLarge,
            color = colors.TextPrimary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    AppTheme {
        HomeScreen()
    }
}
