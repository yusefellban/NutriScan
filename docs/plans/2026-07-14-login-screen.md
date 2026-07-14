# Login Screen Implementation Plan

## Goal Description
Implement the "Sign In" (Login) screen for NutriScan AI based on the provided Figma designs. The screen supports **Email + Password** login and includes **social login buttons** (Facebook, Google, Instagram — UI-only stubs for now). The login and register screens are cross-linked via their bottom navigation prompts.

The implementation strictly adheres to Clean Architecture, MVI, and all rules defined in `AGENTS.md`.

---

## User Review Required

> [!IMPORTANT]
> 1. The social login buttons (Facebook, Google, Instagram) will be **UI-only stubs** in this phase — they will fire a `ShowSnackbar("Coming soon")` effect. The actual `LoginWithGoogleUseCase` / OAuth flows will be wired in a future plan.
> 2. The `FigmaInputField` and `AuthHeader` components will be **extracted from the register screen into shared `common/components/`** so both screens reuse the same composables. This means a small refactor of the register screen's imports.
> 3. The `SocialLoginRow` will also be a shared component — the register screen's Figma design shows the same social row, so it's reusable.
> 4. The "Sign In" button and "OR" divider will also be extracted as reusable `AuthActionButton` and `AuthDivider`.

> [!IMPORTANT]
> **New Package Structure** — Starting with the login feature, all presentation features will follow this sub-package layout:
> ```
> feature/
> ├── state/          ← FeatureState.kt, FeatureEvent.kt, FeatureEffect.kt
> ├── view/           ← FeatureScreen.kt
> │   └── components/ ← Screen-specific composables
> └── viewmodel/      ← FeatureViewModel.kt
> ```
> The register screen will also be refactored to match this structure. The `AGENTS.md` folder tree will be updated to reflect this as the standard going forward.

---

## Differences from Register Screen

| Concern                | Register Screen                                  | Login Screen                                      |
|------------------------|--------------------------------------------------|---------------------------------------------------|
| **Fields**             | Email, Password, Confirm Password                | Email, Password (only)                            |
| **Header title**       | "Sign Up For Free!"                              | "Sign In"                                         |
| **Primary button**     | "Sign Up"                                        | "Sign in"                                         |
| **Bottom prompt**      | "Already have an account? Sign In."              | "Don't have an account? Sign Up."                 |
| **Social login row**   | Not present (current impl)                       | Facebook, Google, Instagram icons + "OR" divider  |
| **Confirm Password**   | Present                                          | Not present                                       |
| **Navigation targets** | → Home (on success), → SignIn (bottom link)      | → Home (on success), → Register (bottom link)     |

---

## Proposed Changes

### Phase 0 — New Package Structure & AGENTS.md Update

#### [MODIFY] AGENTS.md
- Update the presentation folder tree (§2.2) to use the new sub-package convention for **all** features:
```
presentation/src/main/kotlin/ iti.grad.nutriscan.presentation/
├── auth/
│   ├── login/
│   │   ├── state/
│   │   │   ├── LoginState.kt
│   │   │   ├── LoginEvent.kt
│   │   │   └── LoginEffect.kt
│   │   ├── view/
│   │   │   ├── LoginScreen.kt
│   │   │   └── components/
│   │   │       └── LoginFormBody.kt
│   │   └── viewmodel/
│   │       └── LoginViewModel.kt
│   └── register/
│       ├── state/
│       │   ├── RegisterState.kt
│       │   ├── RegisterEvent.kt
│       │   └── RegisterEffect.kt
│       ├── view/
│       │   ├── RegisterScreen.kt
│       │   └── components/
│       │       └── RegisterFormBody.kt
│       └── viewmodel/
│           └── RegisterViewModel.kt
```
- This pattern applies to **every** feature (home, scan, nutrigpt, etc.).

#### [REFACTOR] Register screen — migrate to new package structure
- Move `RegisterState.kt`, `RegisterEvent.kt`, `RegisterEffect.kt` → `register/state/`
- Move `RegisterScreen.kt` → `register/view/`
- Move `register/components/RegisterFormBody.kt` → `register/view/components/`
- Move `RegisterViewModel.kt` → `register/viewmodel/`
- Update all package declarations and imports.

---

### Phase 1 — Shared Enum & Extract Reusable Components

#### [NEW] presentation/src/main/kotlin/.../common/model/SocialMediaProvider.kt
```kotlin
enum class SocialMediaProvider {
    FACEBOOK,
    GOOGLE,
    INSTAGRAM
}
```
- Used by `SocialLoginRow` and consumed in `LoginEvent.SocialLoginClicked(provider)`.

#### [NEW] presentation/src/main/kotlin/.../common/components/AuthHeader.kt
- Extracted from `RegisterHeader.kt`. Takes `titleResId: Int` as a parameter (so it can show "Sign In" or "Sign Up For Free!").

#### [NEW] presentation/src/main/kotlin/.../common/components/AuthActionButton.kt
- Extracted from the "Sign Up" button in `RegisterFormBody.kt`. Takes `textResId`, `isLoading`, `onClick` params. Includes the glow ellipse effect.

#### [NEW] presentation/src/main/kotlin/.../common/components/AuthBottomPrompt.kt
- Extracted from the "Already have an account? Sign In." text in `RegisterFormBody.kt`. Takes `promptText`, `actionText`, `onClick` params.

#### [NEW] presentation/src/main/kotlin/.../common/components/AuthDivider.kt
- The "── OR ──" divider visible in the login Figma design. Used between the primary button and social login row.

#### [NEW] presentation/src/main/kotlin/.../common/components/SocialLoginRow.kt
- Row of 3 circular bordered icons (Facebook, Google, Instagram). Each icon fires `onSocialClick(provider: SocialMediaProvider)`.

#### [MOVE] presentation/.../auth/register/components/FigmaInputField.kt → presentation/.../common/components/FigmaInputField.kt
- Move the input field to shared components. Update package and imports in `RegisterFormBody.kt`.

#### [MODIFY] presentation/.../auth/register/view/components/RegisterFormBody.kt
- Update imports to use shared components from `common/components/`.
- Replace inline button, prompt, and input field code with shared component calls.

#### [DELETE] presentation/.../auth/register/components/RegisterHeader.kt
- Replaced by shared `AuthHeader`.

---

### Phase 2 — SVG → Vector Drawable Conversion

##### [NEW] presentation/src/main/res/drawable/ic_facebook.xml
- Convert `ic_facebook.svg` → Android vector drawable.

##### [NEW] presentation/src/main/res/drawable/ic_google.xml
- Convert `ic_google.svg` → Android vector drawable.

##### [NEW] presentation/src/main/res/drawable/ic_instagram.xml
- Convert `ic_instagram.svg` → Android vector drawable.

---

### Phase 3 — Login Feature (New Files)

#### Resources

##### [MODIFY] presentation/src/main/res/values/strings.xml
- Add English strings:
  - `signin_title` → "Sign In"
  - `action_sign_in` → "Sign in"
  - `prompt_no_account` → "Don't have an account?"
  - `prompt_sign_up_link` → "Sign Up."
  - `social_login_coming_soon` → "Social login coming soon"
  - `or_divider` → "OR"

##### [MODIFY] presentation/src/main/res/values-ar/strings.xml
- Add Arabic strings for all the above.

---

#### Presentation — Login MVI Contract (`auth/login/state/`)

##### [NEW] presentation/src/main/kotlin/.../auth/login/state/LoginState.kt
```kotlin
data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val passwordVisible: Boolean = false,
    @StringRes val emailErrorResId: Int? = null,
    @StringRes val passwordErrorResId: Int? = null,
)
```

##### [NEW] presentation/src/main/kotlin/.../auth/login/state/LoginEvent.kt
```kotlin
sealed interface LoginEvent {
    data class EmailChanged(val value: String) : LoginEvent
    data class PasswordChanged(val value: String) : LoginEvent
    data object TogglePasswordVisibility : LoginEvent
    data object SignInClicked : LoginEvent
    data object SignUpClicked : LoginEvent
    data class SocialLoginClicked(val provider: SocialMediaProvider) : LoginEvent
}
```

##### [NEW] presentation/src/main/kotlin/.../auth/login/state/LoginEffect.kt
```kotlin
sealed interface LoginEffect {
    data object NavigateToHome : LoginEffect
    data object NavigateToRegister : LoginEffect
    data class ShowSnackbar(@StringRes val messageResId: Int) : LoginEffect
}
```

---

#### Presentation — Login ViewModel (`auth/login/viewmodel/`)

##### [NEW] presentation/src/main/kotlin/.../auth/login/viewmodel/LoginViewModel.kt
- Follows `RegisterViewModel` pattern exactly.
- Email regex validation + empty-field checks.
- `handleSignIn()` — validates → sets loading → simulates delay → emits `NavigateToHome`.
- `handleSocialLogin(provider)`:
  ```kotlin
  when (provider) {
      SocialMediaProvider.FACEBOOK  -> { /* stub */ }
      SocialMediaProvider.GOOGLE    -> { /* stub */ }
      SocialMediaProvider.INSTAGRAM -> { /* stub */ }
  }
  // All emit ShowSnackbar(R.string.social_login_coming_soon) for now
  ```
- `navigateToRegister()` → emits `NavigateToRegister`.

---

#### Presentation — Login View (`auth/login/view/`)

##### [NEW] presentation/src/main/kotlin/.../auth/login/view/LoginScreen.kt
- Stateful composable: collects `state` via `collectAsStateWithLifecycle()`, handles effects.
- Layout:
  1. `AuthHeader(titleResId = R.string.signin_title)`
  2. `LoginFormBody(state, onEvent, isDark)`

##### [NEW] presentation/src/main/kotlin/.../auth/login/view/components/LoginFormBody.kt
- Stateless composable containing:
  1. Email `FigmaInputField`
  2. Password `FigmaInputField`
  3. `AuthActionButton(textResId = R.string.action_sign_in, ...)`
  4. `AuthDivider`
  5. `SocialLoginRow(onSocialClick = { onEvent(LoginEvent.SocialLoginClicked(it)) })`
  6. `AuthBottomPrompt(prompt = "Don't have an account?", action = "Sign Up.", ...)`

---

### Phase 4 — Wiring (Optional, for manual testing)

##### [MODIFY] app/.../MainActivity.kt
- Temporarily wire `LoginScreen` for manual testing (same pattern used for register).

---

## Verification Plan

### Automated Tests
#### [NEW] presentation/src/test/kotlin/.../auth/login/LoginViewModelTest.kt
- Email validation: empty → error, invalid format → error, valid → no error.
- Password validation: empty → error.
- Sign in success flow: valid input → loading → `NavigateToHome` effect.
- Social stubs: `SocialLoginClicked(FACEBOOK|GOOGLE|INSTAGRAM)` → `ShowSnackbar` effect.
- Sign up click → `NavigateToRegister` effect.

### Manual Verification
- Deploy the app with `LoginScreen` wired in `MainActivity`.
- Verify layout matches Figma in both Light and Dark themes.
- Verify "Sign Up." link fires the correct navigation event.
- Verify social buttons show "Coming soon" snackbar.

---

## Definition of Done
- [ ] `AGENTS.md` updated with new `state/view/viewmodel` package convention.
- [ ] Register screen refactored to new package structure.
- [ ] All shared components extracted and register screen still works.
- [ ] `SocialMediaProvider` enum created in `common/model/`.
- [ ] SVG social icons converted to Android vector drawables.
- [ ] Login MVI contract files created (State, Event, Effect) in `login/state/`.
- [ ] LoginViewModel created in `login/viewmodel/` with validation logic.
- [ ] LoginScreen + LoginFormBody created in `login/view/`.
- [ ] String resources added for EN and AR.
- [ ] ViewModel unit tests pass.
- [ ] Project builds without errors (`gradlew :presentation:assembleDebug`).
- [ ] No hardcoded strings or colors in any composable.
