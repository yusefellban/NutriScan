package iti.grad.nutriscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import iti.grad.nutriscan.presentation.common.theme.AppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = AppTheme.colors.Background
                ) { innerPadding ->
                    ThemeShowcase(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun ThemeShowcase(modifier: Modifier = Modifier) {
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
        "Divider" to colors.Divider
    )

    val alertColors = listOf(
        "Verdict Green" to colors.VerdictGreen,
        "Verdict Yellow" to colors.VerdictYellow,
        "Verdict Red" to colors.VerdictRed,
        "Warning" to colors.Warning,
        "Error" to colors.Error,
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
                color = colors.TextPrimary
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
                color = colors.TextPrimary
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
                color = colors.TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(alertColors.size) { index ->
            ColorItem(alertColors[index].first, alertColors[index].second)
        }
    }
}

@Composable
fun TypographyItem(name: String, style: TextStyle) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(text = name, style = AppTheme.typography.labelMedium, color = AppTheme.colors.TextSecondary)
        Text(text = "The quick brown fox jumps over the lazy dog", style = style, color = AppTheme.colors.TextPrimary)
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
            color = AppTheme.colors.TextPrimary
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