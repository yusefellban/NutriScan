package iti.grad.nutriscan.presentation.common.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme

/**
 * A unified empty/error/not-found state composable used across the entire app.
 *
 * Provides consistent sizing, typography, and spacing for all empty states:
 * - Offline / No Connection
 * - No Results / Search Not Found
 * - Feature-specific empty lists (Saved, Scan History, Calories History, etc.)
 * - 404 / Not Found
 *
 * @param lightImageRes  Drawable resource to use in light theme.
 * @param darkImageRes   Drawable resource to use in dark theme.
 * @param title          Primary heading text (bold, prominent).
 * @param subtitle       Secondary descriptive text (muted).
 * @param modifier       Optional modifier applied to the root [Column].
 * @param buttonText     Label for the action button. Pass `null` to hide the button.
 * @param onButtonClick  Click handler for the action button. Must be provided when [buttonText] is not null.
 */
@Composable
fun AppEmptyStateWidget(
    @DrawableRes lightImageRes: Int,
    @DrawableRes darkImageRes: Int,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null,
) {
    val imageRes = if (AppTheme.isDark) darkImageRes else lightImageRes

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            modifier = Modifier.size(220.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = AppTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = AppTheme.colors.TextPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.TextSecondary,
            textAlign = TextAlign.Center,
        )

        if (buttonText != null && onButtonClick != null) {
            Spacer(modifier = Modifier.height(32.dp))

            AppButton(
                text = buttonText,
                isLoading = false,
                onClick = onButtonClick,
            )
        }
    }
}
