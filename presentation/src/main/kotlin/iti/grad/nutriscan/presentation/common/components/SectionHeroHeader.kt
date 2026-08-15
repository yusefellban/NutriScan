package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

/**
 * Teal curved-bottom hero band shared by top-level tab screens (Home, Profile,
 * Calories, Saved) — same shape/edge decoration as
 * [iti.grad.nutriscan.presentation.settings.profile.view.components.ProfileHeaderSection],
 * but content-agnostic so each tab can show whatever's relevant to it instead
 * of the profile identity block.
 */
@Composable
fun SectionHeroHeader(
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clipToBounds()
            .background(AppTheme.colors.ProfileHeaderBackground),
    ) {
        // Purely decorative curve — pin it to the physical top-right corner regardless of
        // locale so it looks identical in Arabic instead of jumping to the opposite corner
        // (Alignment.TopEnd flips with RTL, but the artwork itself isn't mirrored to match).
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Image(
                painter = painterResource(R.drawable.profile_edge),
                contentDescription = null,
                colorFilter = ColorFilter.tint(AppTheme.colors.ProfileHeaderEdge),
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
        content()
    }
}
