# KOSans Arabic Font Integration

## Goal Description

NutriScan supports both English and Arabic locales, but the app's typography
system (`AppTypography.kt`) was originally built around two Latin font
families — **Plus Jakarta Sans** (display/headline/title-adjacent + label
roles) and **Lexend Deca** (title/body roles). Neither font ships Arabic
glyphs, so when the app locale is switched to Arabic the system falls back to
the platform's default Arabic font, which breaks the app's visual identity —
weights, x-height, and letter spacing no longer match the Latin type system,
and screens look visually "unbranded" for Arabic-speaking users.

The goal of this feature is to introduce **KOSans**, an Arabic-supporting
type family, as the font used whenever the active app locale is Arabic
(`ar`), while continuing to use Plus Jakarta Sans / Lexend Deca for all other
locales — with **zero changes required at any individual call site**. The
switch must be transparent: every `TextStyle` that already references
`getAppFontFamily(...)` (or is migrated to do so) should automatically
render in KOSans the moment the user's locale is Arabic, and revert
automatically when it isn't.

This document also captures the **current state of the implementation**
(much of which has already been hand-rolled into the codebase), the gaps
still open, and the verification plan required to sign this off as complete
per the team's Feature Planning Standard.

---

## User Review Required

The following decisions need explicit confirmation from the reviewer before
this plan is considered ready to execute (or, since core plumbing already
exists, before the remaining gaps below are closed out):

1. **Locale-detection strategy** — `isArabic` currently reads
   `androidx.compose.ui.text.intl.Locale.current.language`, which reflects
   the **app/system Compose locale**, not necessarily
   `AppCompatDelegate.getApplicationLocales()` if the app also exposes an
   in-app language switcher independent of system settings. **Confirm**
   whether NutriScan has (or will have) an in-app "Language" setting
   (`AppSettingsScreen` suggests it might, via `SettingsSegmentedToggle`).
   If so, `isArabic` must be re-derived from the app's persisted language
   preference rather than `Locale.current`, or the two must be guaranteed to
   stay in sync (e.g. via `AppCompatDelegate.setApplicationLocales`, which
   Compose's `Locale.current` does track after recomposition).
2. **Font weight coverage gap** — KOSans ships 7 weights (Thin, ExtraLight,
   Light, Regular, Medium, SemiBold, Bold), but the design system's
   `displayLarge`/`displayMedium`/`displaySmall`/`headlineLarge` styles
   request `FontWeight.ExtraBold`, which **KOSans does not provide**.
   Compose's `FontFamily` matcher will fall back to the nearest declared
   weight (Bold) automatically, but **confirm this is visually acceptable**
   rather than sourcing an ExtraBold KOSans cut.
3. **Non-Typography-scale text styles** — `HomeTypography`,
   `CaloriesTypography`, `ProductDetailsTypography`, and
   `ExerciseWorkoutTypography` (the four "one-off" object token holders at
   the bottom of `AppTypography.kt`) currently hardcode
   `fontFamily = PlusJakartaSans` / `LexendDeca` directly and **do not** call
   `getAppFontFamily(...)`. **Confirm** whether these should also switch to
   KOSans under Arabic (recommended, for visual consistency) — if yes, this
   plan includes fixing them; if no, explicitly out of scope.
4. **RTL mirroring vs. font swap are separate concerns** — `RtlUtils.kt`
   already exists in `common/util`. Confirm this plan's scope is the **font
   family swap only**, and RTL layout mirroring is handled/verified
   separately (not re-litigated here).
5. **`strings.xml` / KOSans overlap** — Confirm this plan does not need to
   touch `values-ar/strings.xml` translations themselves; it is purely a
   typography/rendering concern layered on top of already-localized strings.

---

## Proposed Changes (by Module)

### 1. `presentation/src/main/res/font/` (resources — already present)
Status: **Done.** The following KOSans `.otf` files are already committed:
`kosans_thin.otf`, `kosans_extralight.otf`, `kosans_light.otf`,
`kosans_regular.otf`, `kosans_medium.otf`, `kosans_semibold.otf`,
`kosans_bold.otf`.

Action items:
- [ ] Verify each file is a valid, licensed-for-distribution static OTF
  (no variable-font axes), since `androidx.compose.ui.text.font.Font(resId,
  weight)` expects one static file per weight, not a variable font.
- [ ] Confirm file naming follows the existing project convention
  (`snake_case`, weight suffix) so future contributors can find/extend it
  without ambiguity — already consistent with `plus_jakarta_sans_*` /
  `lexend_deca_*`.
- [ ] Add a `presentation/src/main/res/font/font_cert.xml` /
  `preloaded_fonts.xml` **only if** downloadable-fonts optimization is
  desired later — out of scope for this pass since fonts are bundled, not
  fetched.

### 2. `common/theme/AppTypography.kt` (core plumbing — mostly done)
Status: **Mostly done**, with the gaps from "User Review Required" #2/#3
still open.

Already implemented:
- `val KOSans = FontFamily(...)` declared with all 7 weights mapped to their
  `FontWeight` equivalents.
- `private val isArabic: Boolean` computed from
  `Locale.current.language == "ar"`.
- `fun getAppFontFamily(defaultFont: FontFamily): FontFamily` — the single
  chokepoint that every `TextStyle.fontFamily` in the main `AppTypography`
  Material3 `Typography()` block now calls (`displayLarge` through
  `labelSmall` — all 15 slots already migrated).

Remaining changes:
- [ ] **Migrate the 4 "one-off" typography objects** (`HomeTypography`,
  `CaloriesTypography`, `ProductDetailsTypography`,
  `ExerciseWorkoutTypography`) to call `getAppFontFamily(PlusJakartaSans)` /
  `getAppFontFamily(LexendDeca)` instead of the raw family references, e.g.:
  ```kotlin
  val dailyTipTitle = TextStyle(
      fontFamily = getAppFontFamily(PlusJakartaSans),
      fontWeight = FontWeight.SemiBold,
      fontSize = 14.sp,
      lineHeight = 20.sp
  )
  ```
  (pending sign-off on review item #3).
- [ ] Re-evaluate `isArabic` as a `@Composable` / `remember`-friendly
  function rather than a top-level `val` getter if it needs to react to
  **in-app** language switching without an Activity recreation — currently
  it's a plain property re-evaluated on every read, which works for
  recomposition-triggered reads but should be explicitly tested (see
  Verification Plan).
- [ ] Add KDoc above `getAppFontFamily` documenting the fallback contract
  (what happens for locales that are neither `en` nor `ar`, e.g. any future
  third locale — currently defaults to `defaultFont`, i.e. Latin family).

### 3. `common/theme/AppTheme.kt` (composition — verify only)
Status: **Verify, no expected code change.**
- [ ] Confirm `AppTheme` wraps content in `MaterialTheme(typography =
  AppTypography, ...)` so the swapped `fontFamily` values propagate through
  `MaterialTheme.typography.*` everywhere screens consume it, rather than
  screens importing `PlusJakartaSans`/`LexendDeca` directly (audit below).

### 4. Call-site audit (`presentation/.../view/**`, `.../components/**`)
Status: **Audit required — likely partial gaps.**
- [ ] Grep the codebase for direct `fontFamily = PlusJakartaSans` /
  `fontFamily = LexendDeca` usages **outside** `AppTypography.kt` (e.g. any
  Composable that builds an ad-hoc `TextStyle` inline instead of pulling
  from `MaterialTheme.typography` or the token objects above). Each hit is a
  screen that will **not** pick up KOSans under Arabic and needs to be
  routed through `getAppFontFamily(...)` or the shared token objects
  instead.
  - Known candidates to check first (highest text density / most visible):
    `LoginScreen`, `RegisterScreen`, `HomeTypography`-adjacent Home screen
    composables, `ProductCard`, `HistoryItemCard`, `VerdictBadge`,
    `AppTopHeader`, `CalorieGoalsCard`, `WaterTrackerCard`.
- [ ] For any `Text(...)` composables passing `style = TextStyle(...)`
  inline with a hardcoded family, refactor to reuse the shared typography
  tokens.

### 5. Localization cross-check (`values/strings.xml` / `values-ar/strings.xml`)
Status: **No plan changes** — this feature does not add or change string
keys. Included here only to confirm no overlap with Rule #2 of the team's
Localization & String Safety standard (no hardcoded user-facing strings) —
font family swapping is orthogonal to string externalization and this plan
does not introduce any new hardcoded strings.

### 6. Theming cross-check (Rule #3 — no hardcoded color hexes)
Status: **No plan changes** — font resources are not colors; nothing in this
change introduces a hardcoded `Color(0xFF...)`. Confirmed non-issue.

### 7. `AppSettingsScreen` / language toggle (conditional on Review Item #1)
Status: **Conditional — only if an in-app language switch exists/ships.**
- [ ] If `AppSettingsViewModel` / `SettingsSegmentedToggle` drives an
  in-app locale change (via `AppCompatDelegate.setApplicationLocales`),
  add an instrumented check that after toggling language without killing
  the process, `AppTypography` re-resolves `isArabic` correctly on the next
  recomposition (Compose's `Locale.current` is expected to update, but this
  needs explicit verification since `isArabic` is not itself a
  `@Composable` function reading `LocalConfiguration`/`LocalContext` — it
  reads a static Compose `Locale.current` accessor).

---

## Verification Plan

### Automated

1. **Unit test — font resolution logic**
   Since `isArabic`/`getAppFontFamily` are plain Kotlin (not tied to
   Android framework classes beyond `androidx.compose.ui.text.intl.Locale`),
   add a focused test class:
   `presentation/src/test/kotlin/iti/grad/nutriscan/presentation/common/theme/AppTypographyTest.kt`
   using the project's existing JUnit 5 pattern from the Standard Test
   Template (adapted — no ViewModel/Turbine needed here, just plain
   assertions):
   ```kotlin
   class AppTypographyTest {
       @Test
       fun `getAppFontFamily returns KOSans when locale is Arabic`() {
           // set/mock Locale.current to "ar" via test double or
           // androidx.compose.ui.test locale override
           assertEquals(KOSans, getAppFontFamily(PlusJakartaSans))
       }

       @Test
       fun `getAppFontFamily returns default family for non-Arabic locale`() {
           // Locale.current = "en"
           assertEquals(PlusJakartaSans, getAppFontFamily(PlusJakartaSans))
           assertEquals(LexendDeca, getAppFontFamily(LexendDeca))
       }
   }
   ```
   Note: `Locale.current` is a Compose-runtime global, so this may require
   running under `androidx.compose.ui.test` (Robolectric or instrumented)
   rather than pure JUnit — if pure-JVM unit testing proves impractical,
   move this to the `androidTest` source set instead and document why.

2. **Static analysis / lint rule (optional but recommended)**
   Add a lightweight custom lint check (or a CI grep step) that fails the
   build if any `Text(` / `TextStyle(` in `presentation/src/main` references
   `PlusJakartaSans` or `LexendDeca` directly rather than through
   `getAppFontFamily(...)` or `MaterialTheme.typography`, to prevent future
   regressions from the Call-Site Audit above being reintroduced.

3. **Screenshot / Paparazzi tests (if the project has a screenshot-testing
   setup)** — render 2–3 representative screens (e.g. `LoginScreen`,
   `HomeScreen`, `ProductCard`) once with locale `en` and once with `ar`,
   and assert the Arabic snapshot uses KOSans glyph shapes (visual diff
   baseline). If no screenshot-testing library is present in `build.gradle.kts`
   yet, flag this as a follow-up rather than blocking this plan.

### Manual

1. **Device/locale switch smoke test**
   - Set system language to Arabic (or use the in-app switch, if it exists)
     and cold-launch the app. Confirm every screen in the bottom nav
     (Home, Scan, Exercises, Calories, Settings) renders text in KOSans, not
     the system fallback Arabic font (visually: KOSans has distinct stroke
     contrast vs. Android's default `Noto Sans Arabic`/`Droid Arabic`
     fallback — compare against a reference screenshot from the design
     team).
   - Switch back to English without killing the process (if in-app toggle
     exists) and confirm the UI reverts to Plus Jakarta Sans / Lexend Deca
     without requiring an app restart.
2. **Weight fidelity spot-check**
   - Compare `displayLarge`/`headlineLarge` (which request `ExtraBold`) in
     Arabic against the design mock — confirm the Bold-weight fallback is
     visually acceptable per Review Item #2's resolution.
3. **RTL + font combination check**
   - Confirm KOSans renders correctly in RTL flow (numerals, mixed Latin/Arabic
     strings such as product names or `%1$s`-formatted strings) on the
     Product Details and Scan History screens, since these commonly mix
     Arabic body copy with Latin brand/product names.
4. **Regression pass on English locale**
   - Full click-through of primary flows (Login → Home → Scan → Product
     Details → Calories → Settings) in English to confirm no visual
     regression was introduced to the existing Plus Jakarta Sans / Lexend
     Deca rendering by the `getAppFontFamily` indirection.
5. **Low-end device check**
   - Verify no added jank/startup-time regression from loading 7 additional
     OTF files (KOsans) alongside the existing 11 (Plus Jakarta Sans +
     Lexend Deca) — spot check cold-start time on a low/mid-tier test
     device.

---

## Summary of Outstanding Work

| Item | Status |
|---|---|
| KOSans font files added to `res/font/` | ✅ Done |
| `KOSans` `FontFamily` declared with 7 weights | ✅ Done |
| `isArabic` locale check | ✅ Done (pending Review Item #1 confirmation) |
| `getAppFontFamily()` helper | ✅ Done |
| Main `AppTypography` Material3 `Typography()` (15 slots) migrated | ✅ Done |
| `HomeTypography` / `CaloriesTypography` / `ProductDetailsTypography` / `ExerciseWorkoutTypography` migrated | ❌ Not started |
| Call-site audit for inline hardcoded `fontFamily` usages | ❌ Not started |
| Unit test coverage for `getAppFontFamily` | ❌ Not started |
| Manual QA pass (device, RTL, weight fidelity) | ❌ Not started |
