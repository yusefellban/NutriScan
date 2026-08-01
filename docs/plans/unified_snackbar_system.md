# Unified Custom Snackbar System

## Problem
The app currently has an inconsistent, bare-bones `AppSnackbar` component that just wraps Material3 `Snackbar` with minimal styling. Snackbar usage is scattered across 6+ screens with different patterns. The Figma design shows a beautiful custom snackbar with three types (**Warning**, **Success**, **Error**), each supporting **dark** and **light** variants, with a leading icon, title, message, and a close (×) button.

## Design Analysis (from Figma screenshots)

From the provided images, the snackbar design has:
- **3 types**: Warning (yellow triangle icon `warning_ic`), Success (teal checkmark icon `success_ic`), Error (red exclamation icon `error_ic`) — all already exist in `drawable/`
- **2 themes**: Dark mode (dark teal gradient background `#0F474A→#13A4AB`) and Light mode (white/soft background)
- **Layout**: `[Icon] [Title + Message] [Close ×]` in a rounded pill shape
- **Close button**: Circle with `×` icon, dismisses the snackbar
- **Auto-dismiss**: Standard snackbar duration behavior

---

## Proposed Changes

### 1. Common — Snackbar Model & Shared Type

#### [NEW] [SnackbarType.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/components/SnackbarType.kt)

Create an enum to unify all snackbar types across the app:

```kotlin
enum class SnackbarType {
    WARNING,   // Yellow triangle icon — for non-critical alerts
    SUCCESS,   // Teal checkmark icon — for confirmations
    ERROR      // Red exclamation icon — for failures
}
```

#### [NEW] [AppSnackbarVisuals.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/components/AppSnackbarVisuals.kt)

Custom `SnackbarVisuals` implementation that carries the `SnackbarType` along with the message:

```kotlin
data class AppSnackbarVisuals(
    override val message: String,
    val type: SnackbarType = SnackbarType.SUCCESS,
    val title: String? = null,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = true,
    override val duration: SnackbarDuration = SnackbarDuration.Short
) : SnackbarVisuals
```

#### [NEW] [showAppSnackbar.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/components/ShowAppSnackbar.kt)

Extension function on `SnackbarHostState` for convenience:

```kotlin
suspend fun SnackbarHostState.showAppSnackbar(
    message: String,
    type: SnackbarType = SnackbarType.SUCCESS,
    title: String? = null,
    duration: SnackbarDuration = SnackbarDuration.Short
): SnackbarResult
```

---

### 2. Common — New AppSnackbar Component

#### [MODIFY] [AppSnackbar.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/components/AppSnackbar.kt)

Complete rewrite of the existing `AppSnackbar.kt` to match the Figma design:

**Visual Design:**
- **Shape**: `RoundedCornerShape(16.dp)` pill shape
- **Background**: Dark mode → horizontal gradient from `Teal1600` to `Teal1400`; Light mode → `Surface` (white)
- **Leading icon**: `warning_ic`, `success_ic`, or `error_ic` based on `SnackbarType` — sized 40dp
- **Title text**: Bold, using the snackbar type name (or custom title) — `AppTheme.typography.titleSmall`
- **Message text**: The snackbar message — `AppTheme.typography.bodySmall`
- **Close button**: Circle background with `×` icon, calls `SnackbarData.dismiss()`
- **Elevation/Shadow**: Subtle shadow via `Modifier.shadow()` for depth

**Component Signature:**
```kotlin
@Composable
fun AppSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier
)
```

The function reads the `SnackbarType` from `snackbarData.visuals` (cast to `AppSnackbarVisuals`), falls back to `SUCCESS` for plain `SnackbarVisuals`.

---

### 3. Theme Colors — Snackbar-Specific Tokens

#### [MODIFY] [AppColors.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/theme/AppColors.kt)

Add snackbar-specific color tokens to `AppColorsExtension3` (which still has room):

```
SnackbarDarkBgStart:    Teal1600 (#0F474A)   | Teal1600 (#0F474A)
SnackbarDarkBgEnd:      Teal1400 (#0B5F65)   | Teal1400 (#0B5F65)
SnackbarLightBg:        Surface  (#FFFFFF)    | Teal1600 (#0F474A) ← dark mode uses dark bg
SnackbarTitleDark:      OnPrimary(#FFFFFF)    | OnPrimary(#FFFFFF)
SnackbarTitleLight:     Teal1600 (#0F474A)    | OnPrimary(#FFFFFF)
SnackbarMessageDark:    Teal400  (#A3E9EC)    | Teal400  (#A3E9EC)
SnackbarMessageLight:   Gray700  (#898989)    | Teal400  (#A3E9EC)
SnackbarCloseIconBg:    Teal1400 (#0B5F65)    | Teal1400 (#0B5F65)
SnackbarCloseIconTint:  Teal400  (#A3E9EC)    | Teal400  (#A3E9EC)
```

> [!NOTE]
> The snackbar uses the same dark-teal style in **both** light/dark themes based on the Figma screenshots (the dark variant is the default). The light variant is available for specific contexts. The `SnackbarType` determines the **icon**, not the background color. The background is always the dark gradient by default (matching the Figma).

---

### 4. Screen-by-Screen Migration

Every screen that currently uses `SnackbarHostState.showSnackbar(message)` will be migrated to use `snackbarHostState.showAppSnackbar(message, type)`.

#### [MODIFY] [MainScreen.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/main/container/view/MainScreen.kt)

- Change the `SnackbarHost` lambda to use the new `AppSnackbar(snackbarData = data)` that accepts `SnackbarData` instead of just `message`

#### [MODIFY] [CaloriesScreen.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/main/calories/view/CaloriesScreen.kt)

- Replace `snackbarHostState.showSnackbar(message = ...)` with `snackbarHostState.showAppSnackbar(message = ..., type = SnackbarType.ERROR)` for errors, `SnackbarType.SUCCESS` for water cup removed

#### [MODIFY] [SavedScreen.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/saved/view/SavedScreen.kt)

- Replace `snackbarHostState.showSnackbar(...)` with `showAppSnackbar(type = SnackbarType.SUCCESS)` for added-to-food-log, `SnackbarType.ERROR` for add-error

#### [MODIFY] [CameraScanScreen.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/view/CameraScanScreen.kt)

- Replace `snackbarHostState.showSnackbar(...)` with `showAppSnackbar(type = SnackbarType.ERROR)` for failures, `SnackbarType.SUCCESS` for "Scan saved to Bookmarks"

#### [MODIFY] [StepHistoryScreen.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/main/calories/stephistory/view/StepHistoryScreen.kt)

- Replace bare `SnackbarHost(snackbarHostState)` with custom rendering via `AppSnackbar`
- Replace `snackbarHostState.showSnackbar(effect.message)` with `showAppSnackbar(message = ..., type = SnackbarType.ERROR)`

#### [MODIFY] [ProfileSetupPagerScreen.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/profile_setup/view/ProfileSetupPagerScreen.kt)

- Replace `snackbarHostState.showSnackbar(message = message)` with `showAppSnackbar(message = ..., type = SnackbarType.ERROR)` (profile save failures)

#### [MODIFY] [ForgotPasswordScreen.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/forgot_password/view/ForgotPasswordScreen.kt)

- Replace `snackbarHostState.showSnackbar(message = message)` with `showAppSnackbar(message = ..., type = SnackbarType.SUCCESS)` for "Code resent" snackbar

#### [MODIFY] [NutriGptScreen.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/nutrigpt/chat/view/NutriGptScreen.kt)

- The `ShowError` effect currently has a `// Show error somehow, maybe toast or snackbar` comment — implement it using the new snackbar system with `SnackbarType.ERROR`

---

### 5. Hardcoded Strings Fix (AGENTS.md compliance)

#### [MODIFY] [CameraScanViewModel.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/viewmodel/CameraScanViewModel.kt)

Replace hardcoded strings (`"Scan saved to Bookmarks"`, `"Failed to save scan"`, `"Image capture failed: ..."`) with `@StringRes` resource IDs, using the existing `ShowSnackBarRes` effect.

#### [MODIFY] [strings.xml](file:///home/yousef/Desktop/NutriScan/presentation/src/main/res/values/strings.xml)

Add new string resources:
```xml
<string name="scan_saved_to_bookmarks">Scan saved to Bookmarks</string>
<string name="scan_save_failed">Failed to save scan</string>
<string name="scan_capture_failed">Image capture failed: %1$s</string>
```

---

## Summary of All Changes

| File | Action | Description |
|------|--------|-------------|
| `SnackbarType.kt` | **NEW** | Enum: WARNING, SUCCESS, ERROR |
| `AppSnackbarVisuals.kt` | **NEW** | Custom SnackbarVisuals carrying type info |
| `ShowAppSnackbar.kt` | **NEW** | Extension function on SnackbarHostState |
| `AppSnackbar.kt` | **REWRITE** | Full Figma-matching custom snackbar composable |
| `AppColors.kt` | **MODIFY** | Add snackbar color tokens to extension3 |
| `MainScreen.kt` | **MODIFY** | Update SnackbarHost rendering |
| `CaloriesScreen.kt` | **MODIFY** | Typed snackbar calls |
| `SavedScreen.kt` | **MODIFY** | Typed snackbar calls |
| `CameraScanScreen.kt` | **MODIFY** | Typed snackbar calls |
| `StepHistoryScreen.kt` | **MODIFY** | Custom SnackbarHost + typed calls |
| `ProfileSetupPagerScreen.kt` | **MODIFY** | Typed snackbar calls |
| `ForgotPasswordScreen.kt` | **MODIFY** | Typed snackbar calls |
| `NutriGptScreen.kt` | **MODIFY** | Implement the pending ShowError via snackbar |
| `CameraScanViewModel.kt` | **MODIFY** | Replace hardcoded strings with @StringRes |
| `strings.xml` | **MODIFY** | Add new string resources |

---

## Open Questions

> [!IMPORTANT]
> **Default snackbar variant**: Based on the Figma, the **dark teal** background is the primary style. Should this always be used regardless of app theme (light/dark mode)? Or should the snackbar follow the current theme — dark bg in dark mode, light bg in light mode?

> [!NOTE]
> The `NutriGptScreen` currently has an unused import of `AppSnackbar` and a TODO comment for error display. I'll implement this properly with the new snackbar system.

---

## Verification Plan

### Automated Tests
- Existing unit tests that reference `ShowSnackbar` effects (`CaloriesViewModelTest`, `SavedViewModelTest`, `ProfileSetupPagerViewModelTest`) should still pass unchanged since we're only modifying presentation layer (Screen composables), not the Effect sealed interfaces or ViewModels.

### Manual Verification
- Build the project with `./gradlew assembleDebug` to verify compilation
- Visually verify each snackbar type matches the Figma design (preview composables will be included)
