package iti.grad.nutriscan.presentation.common.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.PlusJakartaSans

/** Canonical title style for every teal header band (Settings, Help, Terms,
 * Notifications, Home, Profile, Calories, Saved, Product Details) so they all
 * read as one family instead of drifting apart per screen. */
val HeroHeaderTitleStyle = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,
    lineHeight = 24.sp,
)

@Composable
fun HeroHeaderTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = HeroHeaderTitleStyle,
        color = AppTheme.colors.Teal300,
        modifier = modifier,
    )
}

@Composable
fun HeroHeaderSubtitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppTheme.typography.titleSmall,
        color = AppTheme.colors.Gray100,
        modifier = modifier,
    )
}
