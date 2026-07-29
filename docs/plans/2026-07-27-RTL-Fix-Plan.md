# NutriScan – RTL/LTR Bug Audit & Fix Plan

**Scope:** Android app (Jetpack Compose), module `presentation` + `app`.
**Goal:** Fix all places where the UI does not correctly flip direction when the user
switches language to Arabic (AR), **without changing any behavior, spacing, colors,
navigation, or business logic for the English (LTR) app.**

**Ground truth confirmed first:** the core mechanism is already correct.
`MainActivity.kt` computes `layoutDirection` from `AppLanguage` and injects it via
`CompositionLocalProvider(LocalLayoutDirection provides layoutDirection, ...)`
around the whole `AppNavGraph()`. So Compose's own direction-aware primitives
(`Alignment.Start/End`, `Arrangement.Start/End`, `Modifier.padding(start=, end=)`,
`TextAlign.Start/End`, `Icons.AutoMirrored.*`) already flip correctly everywhere they're used.

**The bug is not architectural — it's a set of individual components that bypass this
mechanism** by hardcoding a physical/absolute direction (`left`/`right`, `Left`/`Right`,
`ArrowBack`/`KeyboardArrowLeft` non-mirrored icons, `absolutePadding`) instead of the
logical one (`start`/`end`). Below is every instance found, file-by-file, with the
exact fix. **Do not touch anything not listed here.**

---

## 0. Ground rules for the developer (read first)

1. **Never search-and-replace blindly.** Every fix below is scoped to a specific
   file/line. Apply them one at a time and verify visually in both `EN` and `AR` mode
   after each change (toggle in-app language switch, not device locale — the app
   drives direction from its own `AppLanguage` state, not the OS locale).
2. **Only two kinds of changes are allowed in this pass:**
   - Swap a physical API (`left`, `right`, `Left`, `Right`, non-mirrored back/forward
     icon) for its logical/direction-aware equivalent (`start`, `end`,
     `Icons.AutoMirrored.*`, or an explicit `isRtl` check using
     `LocalLayoutDirection.current`).
   - Add a drawable resource duplication (mirrored asset) ONLY where a vector asset
     itself is inherently directional and Compose's built-in mirroring doesn't apply
     (custom XML drawables aren't auto-mirrored the way `Icons.AutoMirrored` icons are).
3. **Do not change:** sizes, paddings values, colors, animation timings, navigation
   logic, ViewModel/state code, or any EN-only visual output. After each fix, the
   EN layout must be pixel-identical to before.
4. Build and run `./gradlew assembleDebug` after all changes; also run existing tests
   (`ExercisesViewModelTest`, etc.) to confirm no regression — these fixes are UI/View
   layer only and touch zero ViewModel/state files, so they should be unaffected.

---

## 1. Global back-button icon renders in the wrong direction (highest impact)

**File:** `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/components/AppBackButton.kt`

This is a shared component confirmed used in **9 call sites** across the app:
`ExercisesScreen.kt`, `ExerciseWorkoutScreen.kt`, `ForgotPasswordHeader.kt`,
`AppTopHeader.kt` (itself a shared header, so its own callers inherit this too),
`AppSettingsHeader.kt`, `EditProfileScreen.kt`, `NewsScreen.kt`,
`ProfileSetupPagerScreen.kt`, and `ChatTopBar.kt`. Fixing this one component fixes the
back-button direction on effectively every screen with a header/back affordance.

**Problem:** It renders `painterResource(R.drawable.ic_back)` directly. The drawable
`presentation/src/main/res/drawable/ic_back.xml` is a plain vector chevron pointing
left, and it has **no `android:autoMirrored="true"` attribute**. Unlike
`Icons.AutoMirrored.Filled.ArrowBack` (Compose's built-in mirrored icon, used
correctly in `EmailVerificationScreen.kt`), a raw XML vector drawable referenced via
`painterResource` is never auto-flipped by Compose based on `LocalLayoutDirection`.
Result: in Arabic mode, the back button still points left, but a "back" affordance in
an RTL layout should point right (toward the trailing edge, i.e. the direction content
"returns" to).

**Fix (two-line, no visual change in EN):**

1. Open `presentation/src/main/res/drawable/ic_back.xml` and add
   `android:autoMirrored="true"` as an attribute on the `<vector>` root tag (same
   pattern Android uses for all directional system icons). This alone is **not**
   sufficient for `painterResource` — that flag only auto-mirrors drawables loaded
   through the classic View system (`ImageView`, `android:src`), not
   `androidx.compose.ui.res.painterResource`.
2. In `AppBackButton.kt`, mirror it explicitly for Compose:

```kotlin
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.draw.scale
// ...
val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
Icon(
    painter = painterResource(R.drawable.ic_back),
    contentDescription = stringResource(R.string.onboarding_back),
    tint = iconTint,
    modifier = Modifier
        .size(48.dp)
        .then(if (isRtl) Modifier.scale(scaleX = -1f, scaleY = 1f) else Modifier)
)
```

`scale(scaleX = -1f, scaleY = 1f)` horizontally flips only the icon glyph, not its
layout box/size/padding, so nothing else about the button changes. This is the same
technique Android recommends for mirroring a `painterResource` in Compose (there is no
built-in "auto-mirror for arbitrary painter" API as of the Compose version this app
uses — confirm current Compose BOM version in `libs.versions.toml`/`build.gradle.kts`
before implementing, in case a newer stable API exists; if not, the manual scale
approach above is correct and safe).

**Verification:** Toggle language on any screen using `AppBackButton` (e.g. Login →
Forgot Password, Settings → Edit Profile). Confirm the chevron points right in AR and
still points left in EN (unchanged).

---

## 2. Hardcoded "chevron / row-forward" icon (`ic_arrow_right`) never flips

**Files (all use `painterResource(R.drawable.ic_arrow_right)` directly, no RTL check):**

- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/forgot_password/view/components/ResetMethodCard.kt` (line ~124)
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/app/view/components/SettingsActionRow.kt` (line ~93)
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/profile/view/components/ProfileMenuRow.kt` (line ~75)
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/components/ExerciseListItemCard.kt` (line ~84)
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/home/view/components/ExploreItemRow.kt` (line ~77)
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/profile_setup/view/components/ProgressNextButton.kt` (line ~96)

**Problem:** These render a static "row disclosure" / "next" chevron
(`ic_arrow_right.xml`, a plain vector, no `autoMirrored`) used to indicate
"tap to go forward / expand / navigate." In RTL this affordance should point left, but
it stays pointing right in every screen listed above.

**Note there is already a project-internal reference implementation that does this
correctly** — `ProductCard.kt` (lines ~271, ~344-345) already computes:

```kotlin
val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
...
val arrowRes = when {
    isRtl -> R.drawable.ic_arrow_left
    else -> R.drawable.ic_arrow_right
}
```

This is the **exact pattern to replicate** in the six files above — the project
already ships a mirrored companion asset, `ic_arrow_left.xml`, so no new drawable
needs to be created.

**Fix, applied identically in each of the 6 files:**

```kotlin
val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
Icon(
    painter = painterResource(if (isRtl) R.drawable.ic_arrow_left else R.drawable.ic_arrow_right),
    // ...rest unchanged (contentDescription, tint, modifier, size, etc.)
)
```

Add the two imports if missing in that file:
```kotlin
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
```

**Do not** change anything else in these composables (size, tint, padding, click
targets) — only the icon-resource selection line.

**Verification:** For each of the 6 screens, toggle language and confirm the little
disclosure chevron now points left in AR, right in EN (unchanged).

---

## 3. `NutriGptVoiceScreen.kt` — two separate hardcoded-direction bugs

**File:** `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/nutrigpt/voice/view/NutriGptVoiceScreen.kt`

### 3a. Back button uses `Icons.Rounded.KeyboardArrowLeft` (line ~117)

```kotlin
Icon(
    imageVector = Icons.Rounded.KeyboardArrowLeft,
    contentDescription = "Back",
    tint = AppTheme.colors.Primary
)
```

`KeyboardArrowLeft` is a plain (non-mirrored) Material icon — Compose does **not**
auto-flip it. Compare with the correct pattern already used in
`EmailVerificationScreen.kt`, which imports
`androidx.compose.material.icons.automirrored.filled.ArrowBack` and uses
`Icons.AutoMirrored.Filled.ArrowBack`.

**Fix:** Replace with the auto-mirrored equivalent, which Compose flips automatically
based on `LocalLayoutDirection` with zero extra logic needed:

```kotlin
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
// ...
Icon(
    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
    contentDescription = "Back",
    tint = AppTheme.colors.Primary
)
```

(`Icons.AutoMirrored.Rounded.KeyboardArrowLeft` exists in the same
`androidx.compose.material:material-icons-extended` / core icons artifact this app
already depends on — verify the icon set import path compiles; if the "Rounded"
auto-mirrored variant isn't available in the icon library version used, fall back to
`Icons.AutoMirrored.Filled.KeyboardArrowLeft` and keep everything else the same, or use the
manual `LocalLayoutDirection` + conditional icon-swap pattern from Section 1/2 instead —
whichever compiles; either is an acceptable, equally-safe fix.)

*(Minor, separate observation — do not act on this unless asked: this screen also has
its own local "chatLanguage" toggle (`ChatLanguage.EN`/`AR`) independent from the
global `AppLanguage`. The back button's direction should still follow the app's real
`LocalLayoutDirection` (global), not `chatLanguage` — the fix above is correct as-is
since it relies on `LocalLayoutDirection`, not `chatLanguage`.)*

### 3b. Hardcoded `TextAlign.Right` / `TextAlign.Left` instead of `Start`/`End` (line ~177)

```kotlin
val textAlign = if (isHintText) {
    TextAlign.Center
} else {
    if (state.chatLanguage == ChatLanguage.AR) TextAlign.Right else TextAlign.Left
}
```

**Problem:** `TextAlign.Left`/`Right` are **physical** (absolute) alignments — they
always mean the physical left/right edge of the screen regardless of layout
direction. `TextAlign.Start`/`End` are **logical** and automatically resolve to the
correct physical side based on `LocalLayoutDirection`. Here the code manually
re-implements what `Start`/`End` already do, but keys it off the screen-local
`chatLanguage` enum instead of the real layout direction — functionally it happens to
produce the right *visual* result only because `chatLanguage` and the app's global
`AppLanguage` are normally in sync, but it's fragile, redundant, and inconsistent with
every other screen in the app (which correctly uses `Start`/`End`).

**Fix:**

```kotlin
val textAlign = if (isHintText) {
    TextAlign.Center
} else {
    TextAlign.Start
}
```

`TextAlign.Start` already resolves to left in EN and right in AR automatically via
`LocalLayoutDirection` — this is a strict simplification with identical visual output,
not a behavior change. (If a future requirement wants the text aligned to
`chatLanguage` even when it differs from the app's UI language, flag that to the team
before changing — but as of this codebase the two are always the same, so this fix is safe.)

**Verification:** Toggle language on the NutriGPT voice screen; confirm back icon
mirrors and the status text still hugs the correct edge in both modes.

---

## 4. Decorative image uses `absolutePadding` combined with a logical `Alignment`

**File:** `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/profile_setup/view/components/HealthProfileContent.kt` (line ~50)

```kotlin
Image(
    painter = painterResource(heartRes),
    contentDescription = null,
    modifier = Modifier
        .align(Alignment.TopStart)
        .absolutePadding(left = 22.dp, top = 21.dp)
        .size(width = 150.dp, height = 228.dp)
)
```

**Problem:** `Alignment.TopStart` is logical (flips with layout direction — becomes
top-right in AR, which is intended, since the very next `Image` in the same file uses
`Alignment.TopEnd` for a matching decoration on the opposite side). But
`absolutePadding(left = 22.dp, ...)` is **physical** — it always pads from the true
left edge of the screen, never the trailing/leading edge. In RTL, the box aligns to
the top-right (correct), but then still gets pushed 22dp away from the physical left
edge (wrong side), causing the decoration to sit in the wrong place / overlap
incorrectly relative to its mirrored counterpart.

**Fix:**

```kotlin
Image(
    painter = painterResource(heartRes),
    contentDescription = null,
    modifier = Modifier
        .align(Alignment.TopStart)
        .padding(start = 22.dp, top = 21.dp)
        .size(width = 150.dp, height = 228.dp)
)
```

Just swap `absolutePadding(left = ..., top = ...)` for `padding(start = ..., top = ...)`
and remove the now-unused `androidx.compose.foundation.layout.absolutePadding` import
if it's not used elsewhere in the file (check first — grep the file for other
`absolutePadding` calls before removing the import; there is only this one call in
this file, so the import can be removed).

**Verification:** On the Health Profile setup page, toggle language and confirm the
heart decoration mirrors to the top-right with the same 22dp/21dp offsets from that
corner, matching the edge decoration on the other side. In EN it must look pixel-identical
to before the change (22dp from the left, same as `left = 22.dp` produced).

---

## 5. Sweep checklist (already verified clean — do NOT change these)

To avoid wasted effort or accidental regressions, these were checked and are already
correct / not applicable, confirmed by direct code inspection:

- `Arrangement.Start` / `Arrangement.End` usages elsewhere in the codebase — these are
  logical and already flip correctly; no action needed.
- `Modifier.padding(start = ..., end = ...)` usages throughout the codebase — already
  logical; only the single `absolutePadding` case in Section 4 was physical.
- `EmailVerificationScreen.kt`'s use of `Icons.AutoMirrored.Filled.ArrowBack` — already
  correct, use as the reference pattern.
- `ProductCard.kt`'s `isRtl` swipe-delta and icon-swap logic (lines ~271, ~323, ~344) —
  already correct, use as the reference pattern for Sections 1–2.
- `BottomNavCurveShape.kt` / `ShadowExt.kt` — these already receive and use
  `layoutDirection` as a parameter from the Compose `Shape`/`drawWithCache` APIs
  correctly; no hardcoding found.
- `ic_selected_arrow.xml` (small downward-pointing caret used in
  `WeightSelectionPage.kt`, `HeightSelectionPage.kt`, `GenderSelectionPage.kt`) — this
  is a **vertical** pointer (points down, under a title), not a horizontal
  left/right cue, so it is not direction-sensitive and needs no mirroring.
  `ic_arrow_forward.xml` is unused by any `.kt` file that also needs a mirror check
  beyond what's already listed — no further action.
- `AndroidManifest.xml` already has `android:supportsRtl="true"`.

---

## 6. One architectural risk to flag to the team (investigate, don't fix blindly)

`MainActivity.kt` switches language in-place using:
```kotlin
val configContext = baseContext.createConfigurationContext(configuration)
object : ContextWrapper(baseContext) {
    override fun getResources() = configContext.resources
}
```
This overrides `getResources()` only. Calls like `Toast.makeText(context, ...)` (found
in `NotificationSettingsScreen.kt`, `NutriGptVoiceScreen.kt`, `NutriGptScreen.kt`) and
any classic-View inflation (`getSystemService(LAYOUT_INFLATER_SERVICE)`) go through
`ContextWrapper`'s default delegation to `mBase` (the real, un-wrapped Activity
context) for anything other than `getResources()`. On some OEM/Android versions this
can mean a `Toast`'s internal view is inflated/laid out using the Activity's original
(un-toggled) configuration rather than the localized one, so a Toast message shown
right after switching to Arabic could still lay out LTR until the Activity is
naturally recreated (e.g. rotation, or process restart).

This is **out of scope for a quick fix** (it would require either recreating the
Activity on language change, or also overriding `getSystemService` in the
`ContextWrapper` to return a `LayoutInflater` built against `configContext`), and
touching it carries real regression risk to the whole app (Hilt's `hiltViewModel()`
context-walking, which the existing code comment explicitly warns about). **Flag this
to the team/lead before attempting a fix**, and only address it as a separate,
dedicated task with its own testing pass — do not bundle it into this RTL icon/padding
fix pass.

---

## 7. Suggested order of work & PR structure

To keep the change reviewable and low-risk, do this as small, isolated commits/PRs,
each independently testable:

1. `ic_back.xml` `autoMirrored` attribute + `AppBackButton.kt` scale fix (Section 1).
2. The 6-file `ic_arrow_right` → conditional `ic_arrow_left` fix (Section 2) — one
   commit, since it's the same 2-line pattern repeated identically.
3. `NutriGptVoiceScreen.kt` back-icon + `TextAlign` fix (Section 3).
4. `HealthProfileContent.kt` `absolutePadding` → `padding` fix (Section 4).

After each commit: manually toggle EN↔AR on the affected screen(s) and on 2–3
unrelated screens (e.g. Home, Settings) to confirm no unintended layout shift, then
run the full existing test suite.
