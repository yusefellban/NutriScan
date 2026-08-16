# Unify AppTopHeader / AppBackButton, fix header sizing, wire up scan-history header

## Context

Three related complaints, one root cause each:

1. **Headers too tall / empty space at bottom.** `AppTopHeader` hardcodes `top=71dp, bottom=32dp` padding — no relation to actual status-bar height or content, just dead space. `AppSettingsHeader` hardcodes `.height(242.dp)` — a fixed box regardless of how much title/subtitle text is inside, leaving a visible gap under the subtitle. Figma reference (node `992:6878`, "Settings" screen) shows the teal panel hugging its content: back button → 35dp gap → title → 8dp gap → subtitle → tight bottom padding to the rounded corner. No fixed height.

2. **Back button not unified.** `AppBackButton` exists and is reused in 12 places, but every caller passes its own `iconTint`/`borderColor` — 8 different color tokens across the codebase for what should be exactly 2 visual states. One screen (`StepHistoryTopBar`) doesn't use the shared component at all and hand-rolls its own filled circle. User's rule, confirmed by the Figma back button (white icon+border on the teal panel): back button is icon+border, no fill, and takes exactly one of two forms —
   - On a teal/cyan panel: **white** in light mode, **`#13A4AB`** in dark mode.
   - On a white/plain surface: **`#13A4AB`** in both modes.

   `AppTheme.colors.Teal1000` already equals `#13A4AB` in *both* light and dark palettes (`AppColors.kt`), so the white-surface case needs no new token — just consistent usage.

3. **Scan history header not wired to the shared pattern.** The "scan history" screen the user means is `CaloriesHistoryScreen` (confirmed with user) — its `CaloriesHistoryTopBar` renders its own back button with one-off colors (`Teal1200` icon / `CaloriesHistoryTopBarIconBg` border) instead of the unified button.

## Design: `AppBackButton` variant

Replace the free-form `iconTint`/`borderColor` params with a 2-state enum computed from `AppTheme.colors` internally — callers pick *where the button sits*, not raw colors:

```kotlin
enum class BackButtonSurface { OnAccent, OnLight }

@Composable
fun AppBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    surface: BackButtonSurface = BackButtonSurface.OnLight,
) {
    val color = when (surface) {
        BackButtonSurface.OnAccent -> if (AppTheme.isDark) AppTheme.colors.Teal1000 else Color.White
        BackButtonSurface.OnLight -> AppTheme.colors.Teal1000
    }
    // Box(border=color, icon tint=color) — same shape as today, just single color source
}
```

No back-compat shim — every call site is updated in the same change (ladder rung: fix the shared function once, not every caller).

### Call-site classification (verified by reading each screen's `Scaffold`/`Box` background)

**`OnAccent`** (sits directly on a teal/cyan panel):
- `AppTopHeader.kt` (internal default — callers wrap it in a `Primary`-colored `Box`: `ScanHistoryScreen`, `ProductDetailsScreen`)
- `ForgotPasswordHeader.kt` (teal `Primary` panel)
- `AppSettingsHeader.kt` (teal `Teal1000` panel — matches Figma exactly)

**`OnLight`** (white/`Background` scaffold):
- `CaloriesHistoryTopBar.kt` (the "scan history" screen — currently `Teal1200`/`CaloriesHistoryTopBarIconBg`)
- `ExercisesScreen.kt` (currently `ExerciseBackButtonTint` = dark gray — wrong, fix to unified color)
- `ExerciseWorkoutScreen.kt` (same wrong token)
- `ProfileSetupPagerScreen.kt` (currently `Primary` — already visually `#13A4AB`, just switch to `surface` param)
- `NewsHomeScreen.kt` (2 call sites, currently `NewsCategoryLabel` = Teal800 — wrong, fix)
- `NewsScreen.kt` (2 call sites, same wrong token)
- `EditProfileScreen.kt` (currently `Teal1000` — already correct, switch to `surface` param)
- `ChatTopBar.kt` (currently `Primary` on white `ChatScreenBackground` — switch to `surface` param)

**`StepHistoryTopBar.kt`**: not using `AppBackButton` at all — hand-rolled 40dp filled circle. Replace with `AppBackButton(surface = OnLight)` to unify shape (icon+border box, no fill — matches every other screen).

## Header sizing fixes

**`AppTopHeader.kt`**: drop the hardcoded `top=71dp, bottom=32dp`. Use `.statusBarsPadding()` (already the pattern in `AppSettingsHeader.kt`/`CaloriesHistoryScreen.kt`) followed by small fixed padding (`top=12dp, bottom=20dp`) — removes the dead-space gap while still clearing the status bar on any device.

**`AppSettingsHeader.kt`**: remove `.height(242.dp)`. Keep `.statusBarsPadding()` + `top=24dp` padding on the panel, and add a `bottom` padding after the title/subtitle column (e.g. `24dp`) instead of forcing a fixed box height — the teal panel now hugs its content exactly like the Figma reference, no matter how long the subtitle is.

Both fixes are the root cause, applied once in the shared component — every screen using `AppTopHeader` (`ScanHistoryScreen`, `ProductDetailsScreen`) and `AppSettingsHeader` (`AppSettingsScreen`, `NotificationSettingsScreen`, `HelpScreen`, `NotificationHistoryScreen`, `StepHistoryScreen`, `TermsAndConditionsScreen`) gets the fix automatically — no per-screen edits needed for sizing.

## Scan history (`CaloriesHistoryScreen`)

`CaloriesHistoryTopBar.kt` keeps its own layout (it has extra trailing icons — calendar, active-filter-clear — that don't fit `AppTopHeader`'s single-action slot, so it stays a bespoke composable) but its `AppBackButton` call switches to `surface = OnLight`, matching the unified rule.

## Files touched

- `presentation/common/components/AppBackButton.kt` — replace color params with `BackButtonSurface` enum
- `presentation/common/components/AppTopHeader.kt` — fix padding, update `AppBackButton` call
- `presentation/settings/app/view/components/AppSettingsHeader.kt` — remove fixed height, update `AppBackButton` call
- `presentation/main/calories/stephistory/view/components/StepHistoryTopBar.kt` — swap hand-rolled circle for `AppBackButton`
- `presentation/calories_history/view/components/CaloriesHistoryTopBar.kt` — update `AppBackButton` call
- `presentation/auth/forgot_password/view/components/ForgotPasswordHeader.kt` — update `AppBackButton` call
- `presentation/exercises/view/ExercisesScreen.kt`, `presentation/exercises/workout/view/ExerciseWorkoutScreen.kt` — update `AppBackButton` calls
- `presentation/profile_setup/view/ProfileSetupPagerScreen.kt` — update `AppBackButton` call
- `presentation/news/home/view/NewsHomeScreen.kt`, `presentation/news/view/NewsScreen.kt` — update `AppBackButton` calls (2 each)
- `presentation/settings/profile/edit/view/EditProfileScreen.kt` — update `AppBackButton` call
- `presentation/nutrigpt/chat/view/components/ChatTopBar.kt` — update `AppBackButton` call

Now-unused color tokens (`ExerciseBackButtonTint`, `NewsCategoryLabel` if only used for this, `CaloriesHistoryTopBarIconBg` if only used for this) are left in place — not deleting design tokens as part of this change, out of scope.

## Verification

- `./gradlew :presentation:compileDebugKotlin` — confirms every call site compiles against the new `AppBackButton` signature (compiler catches any missed caller).
- Manual pass in a running build (light + dark mode) on: App Settings (teal panel — check no bottom gap, white back button in light / `#13A4AB` in dark), Scan/Calories History (white back button now `#13A4AB`), Step History (back button now square icon+border, not filled circle), Exercises, News, News Home, Chat, Edit Profile, Product Details, Profile Setup.
