package iti.grad.nutriscan.presentation.onboarding.splash

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iti.grad.nutriscan.presentation.common.components.AppErrorWidget
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest

/**
 * Splash Screen — the very first Composable the user sees.
 *
 * ## Animation Sequence
 * ```
 * t = 0ms      Compose first frame drawn → CompositionReady event sent → Starting Window exits
 * t = 500ms    animPhase flips false→true → all animations fire simultaneously:
 *                • Background: SplashBackground → SplashBackgroundEnd  (1200ms, FastOutSlowIn)
 *                • Blobs:      drift top-left → bottom-right            (spring, stiffness very low)
 *                • White logo: alpha 1→0, scale 1.0→0.9                (1200ms)
 *                • Teal logo:  alpha 0→1, scale 0.9→1.0                (1200ms)
 * t = ~1700ms  background animation settles → AnimationCompleted event sent
 * t = ~2000ms  ViewModel checks onboarding state → NavigateToOnboarding or NavigateToLogin
 * ```
 *
 * ## Theme Awareness All colors come from [AppTheme.colors]. No hardcoded Color values except
 * [Color.White] used as the initial logo tint before the theme kicks in — which matches the
 * Starting Window (always a teal background in both light and dark).
 *
 * @param onNavigateToOnboarding Lambda invoked when [SplashEffect.NavigateToOnboarding] is
 * received.
 * @param onNavigateToLogin Lambda invoked when [SplashEffect.NavigateToLogin] is received.
 * @param onNavigateToHome Lambda invoked when [SplashEffect.NavigateToHome] is received.
 */
@Composable
fun SplashScreen(
        onNavigateToOnboarding: () -> Unit,
        onNavigateToLogin: () -> Unit,
        onNavigateToHome: () -> Unit,
        onNavigateToProfileSetup: () -> Unit,
        onNavigateToAccountPendingDeletion: (String) -> Unit = {},
        viewModel: SplashViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.statusBars())
        }
    }

    val colors = AppTheme.colors

    // Animation phase toggles
    var logoVisible by remember { mutableStateOf(false) }
    var animPhase by remember { mutableStateOf(false) }

    // Tracks whether the background color animation has settled (slowest animation)
    var animationDone by remember { mutableStateOf(false) }

    // -----------------------------------------------------------------------
    // Animated values — driven by animPhase and logoVisible
    // -----------------------------------------------------------------------

    // Background: SplashBackground → SplashBackgroundEnd
    val backgroundColor by
            animateColorAsState(
                    targetValue =
                            if (animPhase) colors.SplashBackgroundEnd else colors.SplashBackground,
                    animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                    label = "splash_bg_color",
                    finishedListener = { if (animPhase) animationDone = true }
            )

    // Blob progress: 0f (top-left) → 1f (bottom-right)
    val blobProgress by
            animateFloatAsState(
                    targetValue = if (animPhase) 1f else 0f,
                    animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                    label = "blob_progress",
            )

    // White logo: Fades in initially (entrance), then fades out (exit) + shrinks
    val entranceAlpha by
            animateFloatAsState(
                    targetValue = if (logoVisible) 1f else 0f,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "entrance_alpha"
            )
    val exitAlpha by
            animateFloatAsState(
                    targetValue = if (animPhase) 0f else 1f,
                    animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                    label = "exit_alpha"
            )
    val whiteLogoAlpha = entranceAlpha * exitAlpha

    val whiteLogoScale by
            animateFloatAsState(
                    targetValue = if (animPhase) 0.9f else 1.0f,
                    animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                    label = "white_logo_scale",
            )

    // Primary-tinted logo: fades in + grows
    val tealLogoAlpha by
            animateFloatAsState(
                    targetValue = if (animPhase) 1f else 0f,
                    animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                    label = "teal_logo_alpha",
            )
    val tealLogoScale by
            animateFloatAsState(
                    targetValue = if (animPhase) 1.0f else 0.9f,
                    animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                    label = "teal_logo_scale",
            )

    // Blob gradient colors shift with the animation phase
    val blob1Color1 by
            animateColorAsState(
                    targetValue = if (animPhase) colors.Teal1000 else colors.Teal400,
                    animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                    label = "blob1_c1",
            )
    val blob1Color2 by
            animateColorAsState(
                    targetValue = if (animPhase) colors.Teal700 else colors.Teal300,
                    animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                    label = "blob1_c2",
            )
    val blob2Color1 by
            animateColorAsState(
                    targetValue = if (animPhase) colors.Teal400 else colors.Teal300,
                    animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                    label = "blob2_c1",
            )
    val blob2Color2 by
            animateColorAsState(
                    targetValue = if (animPhase) colors.Teal200 else colors.Teal100,
                    animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                    label = "blob2_c2",
            )

    // -----------------------------------------------------------------------
    // Side effects
    // -----------------------------------------------------------------------

    // 1. Start animations instantly
    LaunchedEffect(Unit) {
        logoVisible = true
        animPhase = true
    }

    // 3. Notify ViewModel when animation settles
    LaunchedEffect(animationDone) {
        if (animationDone) {
            viewModel.onEvent(SplashEvent.AnimationCompleted)
        }
    }

    // 4. Collect and consume one-shot navigation effects
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                SplashEffect.NavigateToOnboarding -> onNavigateToOnboarding()
                SplashEffect.NavigateToLogin -> onNavigateToLogin()
                SplashEffect.NavigateToHome -> onNavigateToHome()
                SplashEffect.NavigateToProfileSetup -> onNavigateToProfileSetup()
                is SplashEffect.NavigateToAccountPendingDeletion -> {
                    onNavigateToAccountPendingDeletion(effect.scheduledDeletionAt)
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // UI
    // -----------------------------------------------------------------------

    Box(
            modifier = Modifier.fillMaxSize().background(backgroundColor),
            contentAlignment = Alignment.Center,
    ) {

        // Background blob layer — covers the full screen
        SplashBlobLayer(
                progress = blobProgress,
                blob1Color1 = blob1Color1,
                blob1Color2 = blob1Color2,
                blob2Color1 = blob2Color1,
                blob2Color2 = blob2Color2,
                modifier = Modifier.fillMaxSize(),
        )

        // Logo layer — White and Primary-tinted logos cross-fade
        Box(contentAlignment = Alignment.Center) {

            // White logo — visible initially, fades out
            Box(
                    modifier =
                            Modifier.graphicsLayer {
                                alpha = whiteLogoAlpha
                                scaleX = whiteLogoScale
                                scaleY = whiteLogoScale
                            }
            ) { LogoImage(tint = Color.White) }

            // Primary-tinted logo — invisible initially, fades in
            Box(
                    modifier =
                            Modifier.graphicsLayer {
                                alpha = tealLogoAlpha
                                scaleX = tealLogoScale
                                scaleY = tealLogoScale
                            }
            ) { LogoImage(tint = colors.Primary) }
        }

        // Profile sync failed (offline or server error) — block navigation and let the user retry
        // the same fetchAndSyncProfile step instead of silently continuing with no data.
        state.errorType?.let { errorType ->
            AppErrorWidget(
                    errorType = errorType,
                    onRetry = { viewModel.onEvent(SplashEvent.RetryClicked) },
                    modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Renders [R.drawable.app_name] with the specified [tint] ColorFilter. A single drawable is used —
 * the appearance changes only via [ColorFilter.tint].
 */
@Composable
private fun LogoImage(tint: Color) {
    Image(
            painter = painterResource(id = R.drawable.app_name),
            contentDescription = null, // purely decorative on the splash
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(width = 240.dp, height = 40.dp),
    )
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(
        name = "Splash — Light (initial state)",
        showBackground = true,
        backgroundColor = 0xFF13A4AB
)
@Composable
private fun SplashLightPreview() {
    AppTheme(darkTheme = false) {
        SplashScreen(
                onNavigateToOnboarding = {},
                onNavigateToLogin = {},
                onNavigateToHome = {},
                onNavigateToProfileSetup = {}
        )
    }
}

@Preview(
        name = "Splash — Dark (initial state)",
        showBackground = true,
        backgroundColor = 0xFF108188
)
@Composable
private fun SplashDarkPreview() {
    AppTheme(darkTheme = true) {
        SplashScreen(
                onNavigateToOnboarding = {},
                onNavigateToLogin = {},
                onNavigateToHome = {},
                onNavigateToProfileSetup = {}
        )
    }
}
