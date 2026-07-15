# Forgot Password Screen — Implementation Plan

## Goal Description

Implement the **Forgot Password** screen for NutriScan AI based on the provided Figma screenshots. The screen allows users to choose a password reset method (Email, 2FA, Google Auth, SMS), with one option pre-selected, and tap "Reset Password" to proceed. It features the same teal gradient header used across auth screens, a back-navigation button, and four selectable method cards. After tapping "Reset Password", a **"Password Sent!" confirmation dialog** appears showing the masked user email, with a "Resend code" button and a close (X) dismiss button.

The implementation strictly follows Clean Architecture + MVI, Hilt DI, type-safe navigation, `StateFlow` + `Channel(BUFFERED)`, and all AGENTS.md rules.

---

## UI Breakdown (from Figma screenshots)

### Layout Structure
1. **Top Section** — Teal gradient header (same `AuthHeader` pattern) with:
   - Back button (rounded-rect with `ic_back` chevron) → top-left corner
   - Title: **"Forgot Password?"** (white, bold, ~28sp)
   - Subtitle: **"Then let's submit password reset."** (lighter teal, ~14sp)
2. **Method Cards Section** — 4 selectable cards stacked vertically:
   - **Send via Email** — icon: `ic_email` (envelope), subtitle: "Reset password via email."
   - **Send via 2FA** — icon: `ic_2fa` (lock/shield), subtitle: "Reset password via 2FA."
   - **Send via Google Auth** — icon: `ic_gauth`, subtitle: "Reset password via G-Auth."
   - **Send via SMS** — icon: `ic_phone`, subtitle: "Reset password via SMS."
   - Each card: rounded rect, teal-tinted icon left, text center, right chevron (`ic_arrow_right`)
   - **Selected card** has teal border highlight + slightly elevated/different background
3. **Bottom CTA** — The shared `AppButton` composable with text **"Reset Password"**

### "Password Sent!" Confirmation Dialog
A modal dialog that overlays the Forgot Password screen after tapping "Reset Password":
1. **Illustration** — Phone device graphic with a purple circle containing a teal lock icon (using the provided `ic_password_sent` drawable)
2. **Title** — **"Password Sent!"** in teal, bold (~24sp)
3. **Subtitle** — "We've sent the password to elem*******221b@gmail.com" — dynamically shows the user's masked email address
4. **"Resend code" button** — teal `AppButton` style, allows re-sending the reset code
5. **Close (X) button** — circular teal-bordered button at the bottom center to dismiss the dialog
6. The background behind the dialog is dimmed (scrim overlay)
7. The dialog card has large rounded corners (~24dp) with white/surface background

### Light vs Dark Theme
- **Light**: White/light-gray card backgrounds, dark text, teal accents, white dialog surface
- **Dark**: Deep teal-tinted dark card backgrounds, light text, teal border on selected card, dark surface dialog

---

## User Review Required

> [!IMPORTANT]
> The SVG icons (`ic_back.svg`, `ic_arrow_right.svg`, `ic_gauth.svg`, `ic_phone.svg`) in the drawable folder are **raw SVGs**. Android cannot use SVGs directly — they must be converted to **Android Vector Drawables** (`.xml`). I will convert these SVGs to proper vector drawable XML files during implementation. Please confirm you also have `ic_email` and `ic_2fa` icons available, or should I create them as vector drawables?

> [!IMPORTANT]
> This screen is currently **UI-only** (no backend integration). The "Reset Password" button will emit a navigation effect but won't call any API yet. The use-case / repository layer for password reset will be added when the backend endpoint is available.

---

## Open Questions

1. **Missing icons** — I can see `ic_back.svg`, `ic_arrow_right.svg`, `ic_gauth.svg`, `ic_phone.svg`, `ic_close.svg`, and `ic_password_sent.svg` in drawable. Do you have `ic_email` (envelope) and `ic_2fa` (lock/shield) icons as well, or should I create them as vector drawables from Material icons?
2. **Back button** — Should the back button navigate back to the **Login Screen** via `navigateUp()`?

---

## MVI Contract

### `ForgotPasswordState`
```kotlin
data class ForgotPasswordState(
    val selectedMethod: ResetMethod = ResetMethod.EMAIL,
    val isLoading: Boolean = false,
    val showPasswordSentDialog: Boolean = false,
    val maskedEmail: String = ""   // e.g. "elem*******221b@gmail.com"
)
```

### `ResetMethod` enum
```kotlin
enum class ResetMethod {
    EMAIL, TWO_FA, GOOGLE_AUTH, SMS
}
```

### `ForgotPasswordEvent`
```kotlin
sealed interface ForgotPasswordEvent {
    data class MethodSelected(val method: ResetMethod) : ForgotPasswordEvent
    data object ResetPasswordClicked : ForgotPasswordEvent
    data object BackClicked : ForgotPasswordEvent
    data object ResendCodeClicked : ForgotPasswordEvent
    data object DismissPasswordSentDialog : ForgotPasswordEvent
}
```

### `ForgotPasswordEffect`
```kotlin
sealed interface ForgotPasswordEffect {
    data object NavigateBack : ForgotPasswordEffect
    data class ShowSnackbar(
        @StringRes val messageResId: Int? = null,
        val messageStr: String? = null
    ) : ForgotPasswordEffect
}
```

---

## Proposed Changes

### Resources

#### [MODIFY] [strings.xml](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/res/values/strings.xml)
Add English strings:
```xml
<string name="forgot_password_title">Forgot Password?</string>
<string name="forgot_password_subtitle">Then let\'s submit password reset.</string>
<string name="reset_method_email">Send via Email</string>
<string name="reset_method_email_desc">Reset password via email.</string>
<string name="reset_method_2fa">Send via 2FA</string>
<string name="reset_method_2fa_desc">Reset password via 2FA.</string>
<string name="reset_method_gauth">Send via Google Auth</string>
<string name="reset_method_gauth_desc">Reset password via G-Auth.</string>
<string name="reset_method_sms">Send via SMS</string>
<string name="reset_method_sms_desc">Reset password via SMS.</string>
<string name="action_reset_password">Reset Password</string>
<string name="password_sent_title">Password Sent!</string>
<string name="password_sent_subtitle">We\'ve sent the password to %1$s</string>
<string name="action_resend_code">Resend code</string>
```

#### [MODIFY] [strings.xml (AR)](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/res/values-ar/strings.xml)
Add Arabic translations for all the strings above.

---

### Drawable Resources — SVG → Vector Drawable Conversions

#### [NEW] `ic_back.xml`
Convert `ic_back.svg` to Android vector drawable (48×48, white stroke rounded-rect + chevron).

#### [NEW] `ic_arrow_right.xml`
Convert `ic_arrow_right.svg` to Android vector drawable (24×24, teal chevron right).

#### [NEW] `ic_gauth.xml` → Already has `.svg`, convert to `.xml` vector drawable

#### [NEW] `ic_phone.xml` → Already has `.svg`, convert to `.xml` vector drawable

#### [NEW] `ic_email.xml`
Create envelope icon vector drawable for the "Send via Email" option.

#### [NEW] `ic_2fa.xml`
Create lock/shield icon vector drawable for the "Send via 2FA" option.

#### [NEW] `ic_close.xml`
Convert `ic_close.svg` to Android vector drawable (24×24, teal X icon for dialog dismiss).

#### [NEW] `ic_password_sent.xml`
Convert `ic_password_sent.svg` to Android vector drawable (375×417, phone+lock illustration for the Password Sent dialog).

---

### Presentation — Forgot Password Feature

#### [NEW] [ForgotPasswordState.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/forgot_password/state/ForgotPasswordState.kt)
- `ForgotPasswordState` data class with `selectedMethod: ResetMethod`, `isLoading: Boolean`
- `ResetMethod` enum: `EMAIL`, `TWO_FA`, `GOOGLE_AUTH`, `SMS`

#### [NEW] [ForgotPasswordEvent.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/forgot_password/state/ForgotPasswordEvent.kt)
- `sealed interface ForgotPasswordEvent`: `MethodSelected`, `ResetPasswordClicked`, `BackClicked`

#### [NEW] [ForgotPasswordEffect.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/forgot_password/state/ForgotPasswordEffect.kt)
- `sealed interface ForgotPasswordEffect`: `NavigateBack`, `ShowSnackbar`

#### [NEW] [ForgotPasswordViewModel.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/forgot_password/viewmodel/ForgotPasswordViewModel.kt)
- `@HiltViewModel`, `MutableStateFlow<ForgotPasswordState>`, `Channel<ForgotPasswordEffect>(BUFFERED)`
- `onEvent()` handles: method selection, reset click (shows dialog + masks email), resend code, dismiss dialog, back
- `ResetPasswordClicked` → sets `isLoading = true`, simulates delay, then sets `showPasswordSentDialog = true` with `maskedEmail`
- Follows the exact same pattern as `LoginViewModel`

#### [NEW] [ForgotPasswordScreen.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/forgot_password/view/ForgotPasswordScreen.kt)
- Stateful `ForgotPasswordScreen(viewModel, onNavigateBack)` — collects state + effects
- Stateless `ForgotPasswordScreenContent(state, onEvent, snackbarHostState)` — renders UI
- When `state.showPasswordSentDialog` is `true`, shows the `PasswordSentDialog` composable as an overlay

#### [NEW] [ResetMethodCard.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/forgot_password/view/components/ResetMethodCard.kt)
- Reusable selectable card composable matching Figma design
- Parameters: `iconRes`, `titleResId`, `subtitleResId`, `isSelected`, `onClick`
- Selected state: teal border + slight background tint
- Chevron right icon on trailing side

#### [NEW] [ForgotPasswordHeader.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/forgot_password/view/components/ForgotPasswordHeader.kt)
- Variant of `AuthHeader` with back button + title + subtitle (no logo)
- Back button: rounded rect (48×48) with `ic_back` icon, top-left positioned
- Title: "Forgot Password?" + Subtitle: "Then let's submit password reset."

#### [NEW] [PasswordSentDialog.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/forgot_password/view/components/PasswordSentDialog.kt)
- Full-screen dialog overlay with scrim background
- White/surface rounded card (~24dp corners) containing:
  - Phone + lock illustration using `ic_password_sent` vector drawable
  - "Password Sent!" title in teal bold
  - "We've sent the password to {maskedEmail}" subtitle
  - "Resend code" `AppButton`
- Close (X) button: circular teal-bordered icon button at the bottom using `ic_close`, dismisses dialog
- Parameters: `maskedEmail: String`, `onResendCode: () -> Unit`, `onDismiss: () -> Unit`

---

### Navigation

#### [MODIFY] [Route.kt](file:///c:/Users/DELL/Documents/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/navigation/Route.kt)
Add `@Serializable object ForgotPasswordRoute`

#### [MODIFY] [NavGraph.kt](file:///c:/Users/DELL/Documents/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt)
- Add `composable<ForgotPasswordRoute>` with `ForgotPasswordScreen`
- Wire `onNavigateBack` → `navController.navigateUp()`

#### [MODIFY] [LoginScreen.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/login/view/LoginScreen.kt)
- Add `onNavigateToForgotPassword` lambda parameter
- Pass it through the composable chain

#### [MODIFY] [LoginEvent.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/login/state/LoginEvent.kt)
- Add `data object ForgotPasswordClicked : LoginEvent`

#### [MODIFY] [LoginEffect.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/login/state/LoginEffect.kt)
- Add `data object NavigateToForgotPassword : LoginEffect`

#### [MODIFY] [LoginViewModel.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/login/viewmodel/LoginViewModel.kt)
- Handle `ForgotPasswordClicked` → send `NavigateToForgotPassword` effect

---

## File Tree Summary

```
presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/forgot_password/
├── state/
│   ├── ForgotPasswordState.kt       [NEW]
│   ├── ForgotPasswordEvent.kt       [NEW]
│   └── ForgotPasswordEffect.kt      [NEW]
├── view/
│   ├── ForgotPasswordScreen.kt      [NEW]
│   └── components/
│       ├── ForgotPasswordHeader.kt   [NEW]
│       ├── PasswordSentDialog.kt     [NEW]
│       └── ResetMethodCard.kt        [NEW]
└── viewmodel/
    └── ForgotPasswordViewModel.kt    [NEW]
```

---

## Verification Plan

### Automated Tests
- Build the project with `./gradlew assembleDebug` to verify compilation.

### Manual Verification
- Deploy to device/emulator and navigate Login → Forgot Password.
- Verify light & dark theme rendering matches the Figma screenshots.
- Confirm back button navigates to Login.
- Confirm method card selection is reflected visually.
- Confirm "Reset Password" button shows the "Password Sent!" dialog with masked email.
- Confirm "Resend code" button in dialog triggers the resend event.
- Confirm close (X) button dismisses the dialog.
- Verify Arabic locale renders correctly.
