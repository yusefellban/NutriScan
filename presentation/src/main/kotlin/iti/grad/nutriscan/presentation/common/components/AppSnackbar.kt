package iti.grad.nutriscan.presentation.common.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

/**
 * Custom snackbar composable matching the Figma design system.
 *
 * Renders a rounded pill-shaped snackbar with:
 * - **Leading icon** — driven by [SnackbarType] (warning / success / error)
 * - **Title + Message** — bold title line + smaller body text
 * - **Close button** — circular dismiss button on the trailing edge
 *
 * Uses a dark teal horizontal gradient background (same in both light and dark theme,
 * matching the Figma spec).
 *
 * @param snackbarData The Material3 [SnackbarData] provided by `SnackbarHost`.
 *                     If the visuals are [AppSnackbarVisuals], the type/title are read;
 *                     otherwise it falls back to [SnackbarType.SUCCESS].
 */
@Composable
fun AppSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier
) {
    val visuals = snackbarData.visuals
    val appVisuals = visuals as? AppSnackbarVisuals
    val type = appVisuals?.type ?: SnackbarType.SUCCESS
    val title = appVisuals?.title ?: type.defaultTitle()
    val message = visuals.message

    val iconRes = when (type) {
        SnackbarType.WARNING -> R.drawable.warning_ic
        SnackbarType.SUCCESS -> R.drawable.success_ic
        SnackbarType.ERROR -> R.drawable.error_ic
    }

    val titleColor = Color.White
    val messageColor = Color(0xFFA3E9EC) // Teal400
    val closeIconBg = Color(0xFF0B5F65) // Teal1400
    val closeIconTint = Color(0xFFA3E9EC) // Teal400

    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF0F474A), // Teal1600
            Color(0xFF0B5F65)  // Teal1400
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0xFF0F474A).copy(alpha = 0.3f),
                spotColor = Color(0xFF0F474A).copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(brush = gradientBrush)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading icon
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = type.name,
                modifier = Modifier.size(40.dp),
                tint = Color.Unspecified // Keep original icon colors
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title + message
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = titleColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = message,
                    color = messageColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Close button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(closeIconBg)
                    .clickable { snackbarData.dismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = stringResource(id = R.string.action_dismiss),
                    modifier = Modifier.size(14.dp),
                    tint = closeIconTint
                )
            }
        }
    }
}

/** Default title per snackbar type — used when no explicit title is set. */
@Composable
private fun SnackbarType.defaultTitle(): String = when (this) {
    SnackbarType.WARNING -> stringResource(id = R.string.alert_warning_title)
    SnackbarType.SUCCESS -> stringResource(id = R.string.alert_success_title)
    SnackbarType.ERROR -> stringResource(id = R.string.alert_error_title)
}

// ─── Previews ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFE8FAFA)
@Composable
private fun AppSnackbarWarningPreview() {
    AppTheme {
        AppSnackbarPreviewContent(SnackbarType.WARNING)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8FAFA)
@Composable
private fun AppSnackbarSuccessPreview() {
    AppTheme {
        AppSnackbarPreviewContent(SnackbarType.SUCCESS)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8FAFA)
@Composable
private fun AppSnackbarErrorPreview() {
    AppTheme {
        AppSnackbarPreviewContent(SnackbarType.ERROR)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F474A)
@Composable
private fun AppSnackbarWarningDarkPreview() {
    AppTheme(darkTheme = true) {
        AppSnackbarPreviewContent(SnackbarType.WARNING)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F474A)
@Composable
private fun AppSnackbarSuccessDarkPreview() {
    AppTheme(darkTheme = true) {
        AppSnackbarPreviewContent(SnackbarType.SUCCESS)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F474A)
@Composable
private fun AppSnackbarErrorDarkPreview() {
    AppTheme(darkTheme = true) {
        AppSnackbarPreviewContent(SnackbarType.ERROR)
    }
}

@Composable
private fun AppSnackbarPreviewContent(type: SnackbarType) {
    val visuals = AppSnackbarVisuals(
        message = "This is a test alert to verify that notifications are working correctly.",
        type = type
    )
    val data = PreviewSnackbarData(visuals)
    Box(modifier = Modifier.padding(16.dp)) {
        AppSnackbar(snackbarData = data)
    }
}

/**
 * Minimal [SnackbarData] implementation for Compose previews only.
 */
private class PreviewSnackbarData(
    override val visuals: AppSnackbarVisuals
) : SnackbarData {
    override fun dismiss() {}
    override fun performAction() {}
}
