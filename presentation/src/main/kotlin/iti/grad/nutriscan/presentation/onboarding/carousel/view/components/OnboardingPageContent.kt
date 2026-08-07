package iti.grad.nutriscan.presentation.onboarding.carousel.view.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.LexendDeca
import iti.grad.nutriscan.presentation.common.theme.PlusJakartaSans

/**
 * Renders a single onboarding page with stable layout.
 *
 * **Layout stability:** The image container [Box] is always present in the layout tree
 * with a fixed [imageHeight], so the title and description texts never shift vertically
 * when the image animates in. Only [alpha] and [translationY] are animated via
 * [graphicsLayer], which is a drawing-only operation that never triggers re-layout.
 *
 * **Responsiveness:** On screens shorter than 680dp, [imageHeight] shrinks and spacing
 * is reduced. The [Column] is wrapped in [verticalScroll] to guarantee that all text
 * is accessible even on the smallest phones.
 */
@Composable
fun OnboardingPageContent(
    imageRes: Int,
    titleRes: Int,
    descriptionRes: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    var showImage by remember { mutableStateOf(false) }
    var showText by remember { mutableStateOf(false) }

    LaunchedEffect(isSelected) {
        showImage = isSelected
        showText = isSelected
    }

    // ── Responsive sizing ────────────────────────────────────────────────────
    val screenHeight: Dp = LocalConfiguration.current.screenHeightDp.dp
    val imageHeight  = if (screenHeight < 680.dp) 200.dp else 300.dp
    val spacerPrimary   = if (screenHeight < 680.dp) 12.dp  else 24.dp
    val spacerSecondary = if (screenHeight < 680.dp) 8.dp   else 16.dp

    // ── Drawing-only animations (no layout shift) ────────────────────────────
    val density = LocalDensity.current

    val imageAlpha by animateFloatAsState(
        targetValue    = if (showImage) 1f else 0f,
        animationSpec  = tween(durationMillis = 800),
        label          = "image_alpha"
    )
    // Raw pixel offset — positive = shift down, 0 = final position
    val imageShiftPx by animateFloatAsState(
        targetValue   = if (showImage) 0f else with(density) { 40.dp.toPx() },
        animationSpec = tween(durationMillis = 800),
        label         = "image_shift"
    )

    val textAlpha by animateFloatAsState(
        targetValue   = if (showText) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 200),
        label         = "text_alpha"
    )
    val textShiftPx by animateFloatAsState(
        targetValue   = if (showText) 0f else with(density) { 20.dp.toPx() },
        animationSpec = tween(durationMillis = 800, delayMillis = 200),
        label         = "text_shift"
    )

    // ── UI ───────────────────────────────────────────────────────────────────
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Fixed-height container → titles/descriptions never shift vertically
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier
                    .padding(16.dp)
                    .graphicsLayer {
                        alpha        = imageAlpha
                        translationY = imageShiftPx
                    }
            )
        }

        Spacer(modifier = Modifier.height(spacerPrimary))

        val colors = AppTheme.colors

        Text(
            text        = stringResource(id = titleRes),
            fontFamily  = PlusJakartaSans,
            fontWeight  = FontWeight.Bold,
            fontSize    = 28.sp,
            lineHeight  = 36.sp,
            textAlign   = TextAlign.Center,
            color       = colors.TextPrimary
        )

        Spacer(modifier = Modifier.height(spacerSecondary))

        // Description — always in the layout tree, only fades in
        Text(
            text       = stringResource(id = descriptionRes),
            fontFamily = LexendDeca,
            fontWeight = FontWeight.Normal,
            fontSize   = 14.sp,
            lineHeight = 22.sp,
            textAlign  = TextAlign.Center,
            color      = colors.TextSecondary,
            modifier   = Modifier.graphicsLayer {
                alpha        = textAlpha
                translationY = textShiftPx
            }
        )

        // Bottom padding so content clears the action button / indicator
        Spacer(modifier = Modifier.height(24.dp))
    }
}
