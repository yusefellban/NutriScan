# Plan: Back-Press Sudden App Closing Fix

## 1. Problem Statement

The app closes abruptly when the user presses back instead of showing an exit confirmation dialog.

### Root Causes (in priority order)

#### Cause 1 — `MainScreen` is the last destination in the back-stack
`SplashScreen` navigates to `MainRoute` with `popUpTo(SplashRoute) { inclusive = true }`.
This leaves the back-stack with only one entry:

```
[MainRoute]  ← only entry in the stack
```

When the user presses back from `HomeTab` → no more entries in the back-stack → the `Activity` receives the back-press event and calls **finish()** → **App Closes** with no warning.

#### Cause 2 — `MainScreen` uses `when(selectedTab)` instead of a nested `NavHost`
Tab switches are not real navigation destinations, so pressing back from any tab closes the app immediately instead of switching tabs.

#### Cause 3 — `cameraExecutor` is never shut down
`Executors.newSingleThreadExecutor()` is created in `CameraScanScreen` but never released via `DisposableEffect`, causing a resource leak.

---

## 2. Proposed Solution

### Core Idea
Instead of letting the app crash close silently:
1. If the user is on any tab **other than HOME** → navigate back to `HomeTab`.
2. If the user is on `HomeTab` → show an **Exit Confirmation Dialog** asking "Are you sure you want to exit?".
   - **"Exit"** → `moveTaskToBack(true)` (sends app to background, does not kill it)
   - **"Cancel"** → dismiss the dialog, stay in the app

---

## 3. State Design

No ViewModel needed for this. The exit dialog flag is purely local UI state managed via `rememberSaveable` inside `MainScreen`:

```kotlin
var showExitDialog by rememberSaveable { mutableStateOf(false) }
```

---

## 4. Files to Modify

### `presentation` Module

---

#### [MODIFY] MainScreen.kt
`presentation/src/main/kotlin/iti/grad/nutriscan/presentation/main/container/view/MainScreen.kt`

**New imports:**
```kotlin
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import iti.grad.nutriscan.presentation.common.components.ConfirmationDialog
```

**Inside `MainScreen()` — right after the existing `rememberSaveable(initialTab)` block:**
```kotlin
val context = LocalContext.current
var showExitDialog by rememberSaveable { mutableStateOf(false) }

// Exit confirmation dialog
if (showExitDialog) {
    ConfirmationDialog(
        title = stringResource(R.string.exit_dialog_title),
        message = stringResource(R.string.exit_dialog_message),
        confirmText = stringResource(R.string.exit_dialog_confirm),
        cancelText = stringResource(R.string.action_cancel),
        onConfirm = {
            showExitDialog = false
            (context as? Activity)?.moveTaskToBack(true)
        },
        onDismiss = { showExitDialog = false }
    )
}

// Back press handler
BackHandler {
    if (selectedTab != BottomNavTab.HOME) {
        selectedTab = BottomNavTab.HOME
    } else {
        showExitDialog = true
    }
}
```

---

#### [MODIFY] CameraScanScreen.kt
`presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/view/CameraScanScreen.kt`

Add a `DisposableEffect` right after the `cameraExecutor` and `mediaActionSound` declarations:
```kotlin
DisposableEffect(Unit) {
    onDispose {
        cameraExecutor.shutdown()
        mediaActionSound.release()
    }
}
```

---

#### [MODIFY] strings.xml (EN)
`presentation/src/main/res/values/strings.xml`

```xml
<string name="exit_dialog_title">Exit App</string>
<string name="exit_dialog_message">Are you sure you want to exit NutriScan?</string>
<string name="exit_dialog_confirm">Exit</string>
```

#### [MODIFY] strings.xml (AR)
`presentation/src/main/res/values-ar/strings.xml`

```xml
<string name="exit_dialog_title">الخروج من التطبيق</string>
<string name="exit_dialog_message">هل أنت متأكد أنك تريد الخروج من NutriScan؟</string>
<string name="exit_dialog_confirm">خروج</string>
```

---

## 5. Files NOT Modified

| File | Reason |
|------|--------|
| `NavGraph.kt` | Back-stack setup is correct; issue is isolated to `MainScreen` |
| `MainActivity.kt` | No Activity-level changes needed |
| `Route.kt` | Routes are correct |
| `CameraScanViewModel.kt` | ViewModel logic is correct |
| `ConfirmationDialog.kt` | Component already exists in `common/components` — reused as-is |

---

## 6. Layer Breakdown

### Domain
- No changes.

### Data
- No changes.

### Presentation
- **`MainScreen.kt`** ← primary fix (BackHandler + Exit Dialog)
- **`CameraScanScreen.kt`** ← secondary fix (resource cleanup)
- **`strings.xml` (EN + AR)** ← 3 new strings each

### App / Navigation
- No changes to `NavGraph.kt` or `Route.kt`.

---

## 7. Navigation Changes
- No route or graph changes.
- The fix is entirely a Compose-level `BackHandler` inside `MainScreen`.

---

## 8. Strings — MANDATORY (Zero Hardcoded Text)

| Key | EN | AR |
|-----|----|----|
| `exit_dialog_title` | Exit App | الخروج من التطبيق |
| `exit_dialog_message` | Are you sure you want to exit NutriScan? | هل أنت متأكد أنك تريد الخروج من NutriScan؟ |
| `exit_dialog_confirm` | Exit | خروج |

---

## 9. Edge Cases

| Scenario | Expected Behavior |
|----------|------------------|
| User on `HomeTab` presses back | Exit Confirmation Dialog appears |
| User taps "Exit" in dialog | App moves to background (`moveTaskToBack`) |
| User taps "Cancel" in dialog | Dialog dismissed, app stays open |
| User on `CaloriesTab` presses back | Navigates back to `HomeTab` (no dialog) |
| User on `ScanTab` presses back | Navigates back to `HomeTab` (no dialog) |
| User on `SavedTab` presses back | Navigates back to `HomeTab` (no dialog) |
| User on `ProfileTab` presses back | Navigates back to `HomeTab` (no dialog) |
| User inside a sub-screen (e.g. `ScanHistoryRoute`) | Normal `navigateUp()` — no changes needed |

---

## 10. Testing Plan

### Build Verification
```bash
./gradlew :app:assembleDebug
```

### Manual Test Cases

| # | Scenario | Expected |
|---|----------|----------|
| 1 | Press back from `HomeTab` | Exit dialog appears |
| 2 | Dialog visible → tap "Exit" | App moves to background |
| 3 | Dialog visible → tap "Cancel" | Dialog dismissed, app stays |
| 4 | Navigate to `CaloriesTab` → press back | Returns to `HomeTab`, no dialog |
| 5 | Navigate to `ScanTab` → press back | Returns to `HomeTab`, no dialog |
| 6 | Navigate to `SavedTab` → press back | Returns to `HomeTab`, no dialog |
| 7 | Navigate to `ProfileTab` → press back | Returns to `HomeTab`, no dialog |
| 8 | From `HomeTab` → open any sub-screen → press back | Returns to `MainScreen`, no dialog |

---

## 11. Definition of Done

- [ ] `BackHandler` added in `MainScreen.kt` with correct logic
- [ ] Exit Confirmation Dialog shown when pressing back from `HomeTab`
- [ ] "Exit" in dialog calls `moveTaskToBack(true)`
- [ ] "Cancel" dismisses dialog with no side effects
- [ ] 3 new strings added in both EN and AR `strings.xml`
- [ ] `cameraExecutor.shutdown()` and `mediaActionSound.release()` in `DisposableEffect`
- [ ] Project builds without errors: `./gradlew :app:assembleDebug`
- [ ] All manual test cases pass
