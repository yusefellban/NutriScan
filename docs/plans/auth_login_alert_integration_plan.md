# Goal: Integrate Custom Alerts into Auth Flows

Integrate the newly built Custom Alert components (`SuccessAlert`, `ErrorAlert`, `WarningAlert`, `InternetAlert`) into the `Login` and `Registration` flows, strictly adhering to the MVI architecture (State, Event, Effect) and handling specific network edge cases.

## User Review Required

> [!IMPORTANT]
> - Since we are mapping exceptions, I'll rely on `retrofit2.HttpException` for HTTP status codes (401, 409, 500) and `java.io.IOException` for network timeouts.
> - I will introduce a shared `AuthAlertState` sealed class in `presentation/common/state/AuthAlertState.kt` to avoid duplicating the alert state logic in both Login and Register.

## Proposed Changes

---

### Shared Presentation Logic

#### [NEW] `AuthAlertState.kt` (presentation/common/state/AuthAlertState.kt)
Create a sealed class representing the possible dialog states:
- `None`
- `InternetError` (For IOExceptions)
- `Error(val messageResId: Int)` (For 401, 500)
- `Warning(val messageResId: Int)` (For Validation, 409)
- `Success(val messageResId: Int)` (For 201 Created)

---

### Login Flow

#### [MODIFY] `LoginState.kt`
- Add `val alertState: AuthAlertState = AuthAlertState.None`
- Remove `genericErrorMessage` as it is superseded by `alertState`.

#### [MODIFY] `LoginEvent.kt`
- Add `DismissAlert`
- Add `RetryAction` (which will retry the last failed login attempt).

#### [MODIFY] `LoginEffect.kt`
- Remove `ShowErrorDialog(val messageStr: String)` (We now use State to drive alerts).

#### [MODIFY] `LoginViewModel.kt`
- Update `handleSignIn` to map `onFailure { throwable }` to the proper `AuthAlertState`:
  - `IOException` -> `InternetError`
  - `HttpException` (401) -> `Error` ("Invalid email or password")
  - `HttpException` (500) -> `Error` ("Server down")
- Map validation errors directly to `Warning` instead of inline text field errors if they prefer a dialog, OR keep inline and also show a WarningAlert as requested. (The prompt specifies: "Action: Display the single-button WarningAlert indicating exactly which fields need correction.").
- Handle `DismissAlert` event by setting `alertState = AuthAlertState.None`.
- Handle `RetryAction` by re-triggering `handleSignIn()`.

#### [MODIFY] `LoginScreen.kt`
- Remove the old `AppErrorDialog` logic.
- Observe `state.alertState` and selectively render `InternetAlert`, `ErrorAlert`, `WarningAlert`, or `SuccessAlert`.
- Map the alerts' `onDismiss` and `onConfirm`/`onRetry` callbacks to `viewModel.onEvent(LoginEvent.DismissAlert)` and `viewModel.onEvent(LoginEvent.RetryAction)`.

---

### Registration Flow

#### [MODIFY] `RegisterState.kt`
- Add `val alertState: AuthAlertState = AuthAlertState.None`

#### [MODIFY] `RegisterEvent.kt`
- Add `DismissAlert`
- Add `RetryAction`

#### [MODIFY] `RegisterEffect.kt`
- Remove `ShowErrorDialog` effect.
- Add `NavigateToLogin` (to be triggered when user dismisses the Success alert).

#### [MODIFY] `RegisterViewModel.kt`
- Update `handleSignUp` to map exceptions:
  - `IOException` -> `InternetError`
  - `HttpException` (409) -> `Warning` ("Email already exists")
  - `HttpException` (500) -> `Error` ("Server down")
  - `onSuccess` -> `Success` ("Account created successfully")
- Handle `DismissAlert` (If the dismissed alert was `Success`, emit `NavigateToLogin` Effect!).
- Handle `RetryAction` (re-trigger `handleSignUp()`).

#### [MODIFY] `RegisterScreen.kt`
- Remove old dialog logic.
- Observe `alertState` and render the corresponding custom composables.
- Wire up the MVI events (`DismissAlert`, `RetryAction`).

## Verification Plan

### Manual Verification
- Attempt login with no Wi-Fi to test `InternetAlert` and `Retry` functionality.
- Attempt login with incorrect credentials to test `ErrorAlert` (401).
- Attempt register with an existing email to test `WarningAlert` (409).
- Register with a new email to test `SuccessAlert` and verify it navigates to Login via SharedFlow effect after clicking OK.
