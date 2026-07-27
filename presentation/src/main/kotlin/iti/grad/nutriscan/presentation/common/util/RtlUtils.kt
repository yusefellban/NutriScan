package iti.grad.nutriscan.presentation.common.util

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Single source of truth for "are we currently laying out right-to-left?".
 *
 * Reads the [LocalLayoutDirection] that `MainActivity` already injects from the
 * app's own [iti.grad.nutriscan.domain.settings.model.AppLanguage] state (not the
 * OS locale). Every composable that previously did:
 *
 * ```
 * val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
 * ```
 *
 * should call this instead, so the check lives in exactly one place.
 */
@Composable
@ReadOnlyComposable
fun isAppRtl(): Boolean = LocalLayoutDirection.current == LayoutDirection.Rtl

/**
 * Picks the correct drawable resource for the current layout direction.
 *
 * Use for components that ship two mirrored drawable variants (e.g. a
 * "row disclosure" chevron) instead of relying on `autoMirrored`/manual scaling.
 *
 * Example:
 * ```
 * painter = painterResource(directionalDrawable(R.drawable.ic_arrow_right, R.drawable.ic_arrow_left))
 * ```
 */
@Composable
@ReadOnlyComposable
@DrawableRes
fun directionalDrawable(@DrawableRes ltr: Int, @DrawableRes rtl: Int): Int =
    if (isAppRtl()) rtl else ltr

// NOTE: There is intentionally no manual "mirror this icon in RTL" modifier here.
// This project's Compose UI version honors `android:autoMirrored="true"` on vector
// drawables loaded via `painterResource` (confirmed by the ic_back.xml / app_name.xml
// investigation), so a directional single-asset icon only needs that XML attribute —
// adding a manual scale(-1f, 1f) on top double-flips it back to looking unflipped.
// Only reach for [directionalDrawable] (two real assets) or a plain `autoMirrored="true"`
// vector; do not reintroduce a manual mirror modifier for single-asset icons.
