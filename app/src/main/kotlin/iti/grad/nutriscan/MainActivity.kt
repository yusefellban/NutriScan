package iti.grad.nutriscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import iti.grad.nutriscan.navigation.NutriScanNavGraph
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.navigation.AppNavGraph

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Edge-to-edge must be enabled BEFORE installSplashScreen
        enableEdgeToEdge()

        // 2. Install the SplashScreen API — reads Theme.NutriScan.Splash from Manifest
        installSplashScreen()

        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppNavGraph()
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeShowcase(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    val colorScheme = MaterialTheme.colorScheme

    val semanticColors = listOf(
        "Primary" to colorScheme.primary,
        "Background" to colorScheme.background,
        "Surface" to colorScheme.surface,
        "Surface Variant" to colorScheme.surfaceVariant,
        "On Primary" to colorScheme.onPrimary,
        "Text Primary" to colorScheme.onBackground,
        "Text Secondary" to colorScheme.onSurfaceVariant,
        "Divider" to colorScheme.outline
    )

    val alertColors = listOf(
        "Verdict Green" to colors.VerdictGreen,
        "Verdict Yellow" to colors.VerdictYellow,
        "Verdict Red" to colors.VerdictRed,
        "Warning" to colors.Warning,
        "Error" to colorScheme.error,
        "Error Background" to colors.ErrorBackground
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Typography Showcase",
                style = typography.displaySmall,
                color = colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            TypographyItem("Display Large", typography.displayLarge)
            TypographyItem("Display Medium", typography.displayMedium)
            TypographyItem("Display Small", typography.displaySmall)
            TypographyItem("Headline Large", typography.headlineLarge)
            TypographyItem("Headline Medium", typography.headlineMedium)
            TypographyItem("Headline Small", typography.headlineSmall)
            TypographyItem("Title Large", typography.titleLarge)
            TypographyItem("Title Medium", typography.titleMedium)
            TypographyItem("Title Small", typography.titleSmall)
            TypographyItem("Body Large", typography.bodyLarge)
            TypographyItem("Body Medium", typography.bodyMedium)
            TypographyItem("Body Small", typography.bodySmall)
            TypographyItem("Label Large", typography.labelLarge)
            TypographyItem("Label Medium", typography.labelMedium)
            TypographyItem("Label Small", typography.labelSmall)
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Semantic Colors Showcase",
                style = typography.displaySmall,
                color = colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(semanticColors.size) { index ->
            ColorItem(semanticColors[index].first, semanticColors[index].second)
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Health Verdicts & Alerts",
                style = typography.displaySmall,
                color = colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 3. Set Compose content — Starting Window is dismissed instantly when Compose draws
        setContent {
            AppTheme {
                NutriScanNavGraph()
            }
        }
    }
}

@Composable
fun TypographyItem(name: String, style: TextStyle) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(text = name, style = AppTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = "The quick brown fox jumps over the lazy dog", style = style, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun ColorItem(name: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
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
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ThemeShowcasePreview() {
    AppTheme {
        ThemeShowcase()
    }
}