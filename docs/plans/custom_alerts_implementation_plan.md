# Custom Alert Dialogs Implementation Plan

This plan details the steps to build reusable, standalone, and strictly localized custom Alert/Dialog components (Stateless Composables) that adapt to both Light and Dark modes, based on the provided mockups.

## Open Questions
- None. The requirements and mockups provide a clear direction for the stateless components and their behavior.

## Proposed Changes

### 1. Localization (String Resources)
Extract all hardcoded texts from the mockups and generate Arabic translations.

#### [MODIFY] `presentation/src/main/res/values/strings.xml`
Add default English strings for titles (Delete Warning, Success Alert, Error Alert, Warning Alert, Internet Alert), messages, and button labels (Cancel, Delete, ok, Retry).

#### [MODIFY] `presentation/src/main/res/values-ar/strings.xml`
Add Arabic translations for all the newly added strings to support RTL layouts and Arabic locales natively.

### 2. Base Dialog Component
Create a generic container that handles the specific UI layout shown in the mockups.

#### [NEW] `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/components/CustomAlertDialog.kt`
- Build a generic `CustomAlertDialog` composable.
- Implement the top-center overlapping icon layout using `Box` and `offset`.
- Apply background glow/shadow effects dynamically based on the current theme (Light/Dark mode) and alert type.
- Expose slots for buttons to allow flexibility in one or two-button layouts.

### 3. Specific Alert Variants
Implement the explicit variants requested by the mockups.

#### [NEW] `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/components/Alerts.kt`
Create specific `@Composable` functions for each alert type:
- `DeleteWarningAlert`
- `SuccessAlert` (with variants for 1 or 2 buttons)
- `ErrorAlert`
- `WarningAlert`
- `InternetAlert`

Each composable will:
- Be 100% stateless, exposing callbacks like `onConfirm: () -> Unit` and `onDismiss: () -> Unit`.
- Default to the localized strings created in step 1, but accept optional string parameters for maximum reusability.
- Provide `@Preview` annotations for both Light and Dark themes to verify styling.

## Verification Plan

### Automated Tests
- N/A for UI-only components, but Compose Preview will be used extensively.

### Manual Verification
- Render all `@Preview` functions in Android Studio.
- Verify that Light and Dark modes match the mockups (dark teal vs white backgrounds).
- Verify that the Arabic layout mirrors correctly (RTL).
