# Plan: AppColors Deduplication — Remove Redundant Feature-Specific Color Aliases

## 1. Problem

`AppColors.kt` is **1,403 lines** and growing. Many feature-specific colors are simple aliases of base palette tokens with identical values in both light AND dark themes. Others don't map to any existing base token, but the same (light, dark) pair is repeated across multiple features.

## 2. Approach

1. **Remove** colors where both light and dark values are identical to an existing base/semantic token → replace at usage sites with the base token directly.
2. **Create new semantic tokens** for (light, dark) pairs that don't match any existing token but are reused across 2+ features → consolidate into one shared semantic name.
3. **Leave** colors that have unique (light, dark) pairs used only once — those are genuinely feature-specific.

---

## 3. Category 1 — Exact Match to Existing Base Token → REMOVE

These colors are pure aliases. Screens should use the base token directly.

> **Important:** `Primary` dark = `0xFF0B5F65` (NOT `0xFF13A4AB`). Colors pinned at `0xFF13A4AB` in both themes map to **`Teal1000`**, not `Primary`.
> `OnPrimary` dark = `0xFFA3E9EC` (NOT White). Colors pinned at White in both themes do NOT map to `OnPrimary`.

### Maps to `Teal1000` (`0xFF13A4AB` in both themes)

| # | Property | Location |
|---|---|---|
| 1 | `ExerciseWorkoutPrimaryButtonBg` | Extension2 |
| 2 | `ExerciseWorkoutSecondaryButton` | Extension2 |
| 3 | `ExerciseSetsRepsIconTint` | Extension2 |
| 4 | `ProfileAddIconTint` | Extension |
| 5 | `SaveAlertButtonBackground` | Extension |
| 6 | `ProductCardVerdictBackground` | Extension |
| 7 | `SavedSearchIconBackground` | Extension |
| 8 | `ProductCardSwipeIconBackground` | Extension |
| 9 | `ProductDetailBookmarkTint` | Extension2 |
| 10 | `ProductDetailNutritionPill` | Extension2 |
| 11 | `ProductDetailIngredientName` | Extension2 |
| 12 | `ProgressFillColor` | Main + Extension |
| 13 | `PageIndicatorCurrent` | Main + Extension |
| 14 | `CaloriesHistoryStatIconTint` | Extension3 |

### Maps to `Teal100` (`0xFFE8FAFA` in both themes)

| # | Property | Location |
|---|---|---|
| 15 | `ProductCardVerdictText` | Extension |
| 16 | `SavedSearchIconTint` | Extension |

### Maps to `Teal1200` (light=`0xFF11939A`, dark=`0xFFA3E9EC`)

| # | Property | Location |
|---|---|---|
| 17 | `StepHistoryTopBarIconTint` | Extension3 |
| 18 | `StepHistoryGaugeIconTint` | Extension3 |
| 19 | `StepHistorySummaryTitleText` | Extension3 |
| 20 | `StepHistorySummaryIconTint` | Extension3 |
| 21 | `CaloriesHistoryTopBarIconTint` | Extension3 |

### Maps to `TextPrimary` (light=`0xFF393C3C`, dark=`0xFFE8FAFA`)

| # | Property | Location |
|---|---|---|
| 22 | `StepHistoryGaugeValueText` | Extension3 |
| 23 | `StepHistoryChipTextSelected` | Extension3 |
| 24 | `StepHistorySummaryValueText` | Extension3 |
| 25 | `ChatInputText` | Extension2 |

### Maps to `TextSecondary` (light=`0xFF777777`, dark=`0xFFA6A5A5`)

| # | Property | Location |
|---|---|---|
| 26 | `StepHistoryChartLabel` | Extension3 |

### Maps to `Background` (light=`0xFFFFFFFF`, dark=`0xFF0F474A`)

| # | Property | Location |
|---|---|---|
| 27 | `AuthDialogBackground` | Main |
| 28 | `NewsSearchBarBg` | Extension3 |
| 29 | `CaloriesHistoryCardBg` | Extension3 |

### Maps to `Surface` (light=`0xFFFFFFFF`, dark=`0xFF0B5F65`)

| # | Property | Location |
|---|---|---|
| 30 | `MethodCardBackground` | Main |
| 31 | `StepHistoryGaugeCardBg` | Extension3 |
| 32 | `StepHistorySummaryCardBg` | Extension3 |

### Maps to `SurfaceVariant` (light=`0xFFF8F8F9`, dark=`0xFF0A545A`)

| # | Property | Location |
|---|---|---|
| 33 | `ProfileMenuRowBackground` | Extension |
| 34 | `AppSettingsCardBackground` | Extension |
| 35 | `ProductDetailIngredientCardBg` | Extension2 |
| 36 | `ProductDetailFlaggedContainerBg` | Extension2 |

### Maps to `Teal500` (`0xFF75DEE3` in both themes)

| # | Property | Location |
|---|---|---|
| 37 | `DobCardBackground` | Main + Extension |
| 38 | `HeightSelectedCardBackground` | Main + Extension |
| 39 | `GenderMaleCardBackground` | Main + Extension |

**Total Category 1: ~39 properties to remove**

---

## 4. Category 2 — Same (Light, Dark) Pair Used 2+ Times, No Existing Token → NEW Semantic

These pairs are reused but don't match any existing base/semantic token. Create a new one instead of keeping multiple feature-specific names.

### Pair: `(0xFFF8F8F9, 0xFF0F474A)` — Gray100 / Background dark

Used for "slightly elevated screen backgrounds" — lighter than white in light, same as Background in dark.

| Property | Feature |
|---|---|
| `StepHistoryScreenBg` | Step History |
| `CaloriesHistoryScreenBg` | Calories History |
| `ProfileAddCardBackground` | Profile |
| `ProductDetailImageCardBg` | Product Detail |

> **Proposed name:** `ScreenSurfaceBackground` — represents a neutral screen surface that differs from the main `Background`/`SurfaceVariant`.

### Pair: `(0xFFC0C0C0, 0xFF11939A)` — Gray500 / Teal1200 literal

Used for muted section labels in settings-like screens.

| Property | Feature |
|---|---|
| `ProfileMenuLabel` | Profile Menu |
| `AppSettingsRowLabel` | App Settings |

> **Proposed name:** `MenuSectionLabel`

### Pair: `(0xFFD4F1F2, 0xFF0F474A)` — Teal200 literal / Background dark

Used for icon container backgrounds in menu rows.

| Property | Feature |
|---|---|
| `ProfileMenuIconBackground` | Profile Menu |
| `AppSettingsIconContainerBackground` | App Settings |

> **Proposed name:** `MenuIconContainerBackground`

**Total Category 2: 3 new semantic tokens replacing ~8 properties**

---

## 5. Summary

| Category | Properties Removed | Lines Saved (est.) |
|---|---|---|
| Maps to existing base token | ~39 | ~230 |
| Consolidated to new semantic | ~8 (→ 3 new) | ~30 |
| **Total** | **~47 properties removed, 3 added** | **~260 lines** |

Final file size: ~1,403 → ~1,140 lines (**~19% reduction**)

---

## 6. Implementation Steps

### Phase 1: Remove base-token matches
For each of the 39 properties:
1. `grep` all usages across the codebase
2. Replace `AppTheme.colors.XxxColor` with `AppTheme.colors.<BaseToken>`
3. Remove the property from Extension class, forwarding property, and both `lightColors()`/`darkColors()` assignments

### Phase 2: Create new semantic tokens
1. Add 3 new semantic properties to AppColors (directly or via extension)
2. Set values in `lightColors()` and `darkColors()`
3. Replace all usages of the old feature-specific names with the new semantic names
4. Remove old properties

### Phase 3: Build & Verify
1. `./gradlew assembleDebug` — no compile errors
2. Compare light/dark screenshots for affected screens

---

## 7. Files to Modify

### Core (always)
- `presentation/.../common/theme/AppColors.kt` — remove properties, add 3 new ones

### Usage-site screens (per removed property)
- Product Card, Product Detail, Saved Search, Exercise Workout, Profile, App Settings, Step History, Calories History, Chat, Onboarding (DoB, Height, Gender), News, Auth dialogs
- Exact files will be identified by `grep` during implementation
