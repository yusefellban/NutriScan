# Registration API Integration & Email Verification Screen

Implement the registration API call and a new Email Verification screen, following the existing Clean Architecture + MVI patterns in the codebase.

## User Review Required

> [!IMPORTANT]
> **ErrorInterceptor change:** The current `ErrorInterceptor` throws exceptions for **all** 400-level and 500-level responses, meaning the Retrofit call for registration will never receive the JSON error body (e.g., `409 CONFLICT — email taken`). I will modify the interceptor to **only** throw for 401, 403, 404, and 5xx, and let **400** and **409** responses pass through to Retrofit so the repository can parse the error body and produce proper domain errors.

> [!IMPORTANT]  
> **`kotlinx.serialization` plugin in `data` module:** The data module's `build.gradle.kts` does **not** apply the `kotlin.serialization` plugin, but it needs `@Serializable` for the DTOs. I will add `alias(libs.plugins.kotlin.serialization)` to the data module's plugins block.

## Proposed Changes

### Domain Layer — New `auth` package

Creates the auth domain contract. The domain module is pure Kotlin (no Android deps).

#### [NEW] [IAuthRepository.kt](file:///home/yousef/Desktop/NutriScan/domain/src/main/kotlin/iti/grad/nutriscan/domain/auth/repository/IAuthRepository.kt)
- Interface with two methods:
  - `suspend fun register(email: String, password: String): Result<Unit>` — wraps success/failure
  - `suspend fun resendVerificationEmail(email: String): Result<Unit>`
- Uses Kotlin's `Result<T>` — lightweight, no custom sealed class needed for now

#### [NEW] [RegisterUseCase.kt](file:///home/yousef/Desktop/NutriScan/domain/src/main/kotlin/iti/grad/nutriscan/domain/auth/usecase/RegisterUseCase.kt)
- Follows the existing `operator fun invoke()` pattern (see `CompleteOnboardingUseCase`)
- Delegates to `IAuthRepository.register(email, password)`
- `@Inject constructor(private val authRepository: IAuthRepository)`

#### [NEW] [ResendVerificationEmailUseCase.kt](file:///home/yousef/Desktop/NutriScan/domain/src/main/kotlin/iti/grad/nutriscan/domain/auth/usecase/ResendVerificationEmailUseCase.kt)
- Delegates to `IAuthRepository.resendVerificationEmail(email)`
- Same pattern as above

---

### Data Layer — API service, DTOs, data source, repository impl

#### [MODIFY] [build.gradle.kts](file:///home/yousef/Desktop/NutriScan/data/build.gradle.kts)
- Add `alias(libs.plugins.kotlin.serialization)` to the plugins block so `@Serializable` works on DTOs

#### [NEW] [RegisterRequestDto.kt](file:///home/yousef/Desktop/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/remote/dto/RegisterRequestDto.kt)
- `@Serializable data class` matching the API contract:
  ```
  firstName, lastName, email, username, password,
  dateOfBirth, gender, heightCm, weightKg, allergies, diseases
  ```
- Missing UI fields (firstName, lastName, username, dateOfBirth, gender, heightCm, weightKg) will be defaulted to `""`, `0.0`, or `emptyList()` in the mapper

#### [NEW] [RegisterResponseDto.kt](file:///home/yousef/Desktop/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/remote/dto/RegisterResponseDto.kt)
- `@Serializable data class` with `message: String`, `requiresEmailVerification: Boolean`

#### [NEW] [ResendVerificationRequestDto.kt](file:///home/yousef/Desktop/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/remote/dto/ResendVerificationRequestDto.kt)
- `@Serializable data class` with `email: String`

#### [NEW] [ResendVerificationResponseDto.kt](file:///home/yousef/Desktop/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/remote/dto/ResendVerificationResponseDto.kt)
- `@Serializable data class` with `message: String`

#### [NEW] [ApiErrorDto.kt](file:///home/yousef/Desktop/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/remote/dto/ApiErrorDto.kt)
- `@Serializable data class` matching the backend Standard Error Format:
  ```
  timestamp, status, error, message, details (nullable list), path
  ```
- Used to parse error bodies from 400/409 responses

#### [NEW] [AuthApiService.kt](file:///home/yousef/Desktop/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/remote/api/AuthApiService.kt)
- Retrofit interface with:
  - `@POST("v1/auth/register") suspend fun register(@Body request: RegisterRequestDto): Response<RegisterResponseDto>`
  - `@POST("v1/auth/resend-verification") suspend fun resendVerification(@Body request: ResendVerificationRequestDto): Response<ResendVerificationResponseDto>`
- Returns `Response<T>` (not raw `T`) so we can inspect HTTP status codes for 400/409

#### [NEW] [IAuthRemoteDataSource.kt](file:///home/yousef/Desktop/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/remote/datasource/IAuthRemoteDataSource.kt)
- Interface mirroring the API service methods

#### [NEW] [AuthRemoteDataSourceImpl.kt](file:///home/yousef/Desktop/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/remote/datasource/AuthRemoteDataSourceImpl.kt)
- `@Inject constructor(private val authApiService: AuthApiService)`
- Delegates directly to the API service

#### [NEW] [AuthRepositoryImpl.kt](file:///home/yousef/Desktop/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/repository/AuthRepositoryImpl.kt)
- Implements `IAuthRepository`
- `register()`: Maps email → `RegisterRequestDto` (defaulting missing fields), calls data source, parses error body on failure using `ApiErrorDto`, returns `Result.failure(Exception(parsedMessage))` or `Result.success(Unit)`
- `resendVerificationEmail()`: Same pattern

#### [MODIFY] [ErrorInterceptor.kt](file:///home/yousef/Desktop/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/remote/interceptor/ErrorInterceptor.kt)
- Remove the interceptor's handling of 400 and 409 (currently not handled but let's ensure they pass through)
- Keep 401, 403, 404, 422, 5xx handling as-is

---

### App Layer — DI wiring & navigation

#### [MODIFY] [NetworkModule.kt](file:///home/yousef/Desktop/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/di/NetworkModule.kt)
- Add `provideAuthApiService(retrofit: Retrofit): AuthApiService` using `retrofit.create()`

#### [MODIFY] [DataSourceModule.kt](file:///home/yousef/Desktop/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/di/DataSourceModule.kt)
- Add binding: `IAuthRemoteDataSource` → `AuthRemoteDataSourceImpl`

#### [MODIFY] [RepositoryModule.kt](file:///home/yousef/Desktop/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/di/RepositoryModule.kt)
- Add binding: `IAuthRepository` → `AuthRepositoryImpl`

#### [MODIFY] [Route.kt](file:///home/yousef/Desktop/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/navigation/Route.kt)
- Add: `@Serializable data class EmailVerificationRoute(val email: String)`

#### [MODIFY] [NavGraph.kt](file:///home/yousef/Desktop/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt)
- Update `RegisterScreen` composable: change `onNavigateToHome` → `onNavigateToEmailVerification: (String) -> Unit` — navigates to `EmailVerificationRoute(email)`, popping RegisterRoute
- Add `composable<EmailVerificationRoute>` destination wiring `EmailVerificationScreen`
  - `onNavigateToSignIn` → navigate to `LoginRoute`, popping EmailVerificationRoute

---

### Presentation Layer — ViewModel changes + new Email Verification feature

#### [MODIFY] [RegisterEffect.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/register/state/RegisterEffect.kt)
- Change `NavigateToHome` → `NavigateToEmailVerification(val email: String)` — carries the registered email

#### [MODIFY] [RegisterViewModel.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/register/viewmodel/RegisterViewModel.kt)
- Inject `RegisterUseCase` into the constructor
- Replace the `delay(1500)` simulation with the actual use case call
- On success: emit `RegisterEffect.NavigateToEmailVerification(email)`
- On failure: emit `RegisterEffect.ShowSnackbar(messageStr = errorMessage)`

#### [MODIFY] [RegisterScreen.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/register/view/RegisterScreen.kt)
- Change `onNavigateToHome` lambda to `onNavigateToEmailVerification: (String) -> Unit`
- Update effect collector to handle new `NavigateToEmailVerification` effect
- Improve `ShowSnackbar` handler to display the actual message string

#### [NEW] Email Verification MVI — 4 files:

##### [NEW] [EmailVerificationState.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/email_verification/state/EmailVerificationState.kt)
```kotlin
data class EmailVerificationState(
    val email: String = "",
    val isResending: Boolean = false
)
```

##### [NEW] [EmailVerificationEvent.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/email_verification/state/EmailVerificationEvent.kt)
```kotlin
sealed interface EmailVerificationEvent {
    data object GoToSignInClicked : EmailVerificationEvent
    data object ResendEmailClicked : EmailVerificationEvent
}
```

##### [NEW] [EmailVerificationEffect.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/email_verification/state/EmailVerificationEffect.kt)
```kotlin
sealed interface EmailVerificationEffect {
    data object NavigateToSignIn : EmailVerificationEffect
    data class ShowSnackbar(val message: String) : EmailVerificationEffect
}
```

##### [NEW] [EmailVerificationViewModel.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/email_verification/viewmodel/EmailVerificationViewModel.kt)
- `@HiltViewModel`, injects `ResendVerificationEmailUseCase` and `SavedStateHandle`
- Extracts `email` from `SavedStateHandle` (from nav args)
- `onEvent()` dispatcher:
  - `GoToSignInClicked` → emit `NavigateToSignIn` effect
  - `ResendEmailClicked` → set `isResending = true`, call use case, on success emit success snackbar, on failure emit error snackbar, set `isResending = false`

##### [NEW] [EmailVerificationScreen.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/email_verification/view/EmailVerificationScreen.kt)
- Matches the provided design image:
  - **Top section**: Teal gradient header with back arrow, "Verify Your Email" title, "Confirm it's you to start scanning." subtitle (reuses `AuthHeader` styling pattern)
  - **Center**: Email envelope icon (Material Icons `MarkEmailRead` or `Email`), "A verification link was sent to:" text, **bold email address** (dynamic from state)
  - **Instruction text**: "Please check your email and click the verification link to activate your account. After verifying, you can sign in below."
  - **"Go to Sign In" button**: Uses existing `AppButton` component
  - **"Didn't receive it? Resend Verification Email."**: Clickable text link
- All colors from `AppTheme.colors` / `MaterialTheme.colorScheme` — no hardcoded colors
- All text from `stringResource(R.string.xxx)` — no hardcoded strings

---

### String Resources

#### [MODIFY] [strings.xml (EN)](file:///home/yousef/Desktop/NutriScan/presentation/src/main/res/values/strings.xml)
Add:
```xml
<!-- Email Verification -->
<string name="email_verification_title">Verify Your Email</string>
<string name="email_verification_subtitle">Confirm it\'s you to start scanning.</string>
<string name="email_verification_sent_to">A verification link was sent to:</string>
<string name="email_verification_instructions">Please check your email and click the verification link to activate your account. After verifying, you can sign in below.</string>
<string name="email_verification_go_to_sign_in">Go to Sign In</string>
<string name="email_verification_didnt_receive">Didn\'t receive it?</string>
<string name="email_verification_resend">Resend Verification Email.</string>
<string name="email_verification_resend_success">Verification email sent successfully!</string>
<string name="email_verification_resend_error">Failed to resend verification email. Please try again.</string>
<string name="register_error_email_taken">This email is already registered. Please sign in or use a different email.</string>
<string name="register_error_generic">Registration failed. Please try again.</string>
```

#### [MODIFY] [strings.xml (AR)](file:///home/yousef/Desktop/NutriScan/presentation/src/main/res/values-ar/strings.xml)
Add matching Arabic translations for all new strings above.

## Verification Plan

### Automated Tests
- Build the project with `./gradlew assembleDebug` to confirm no compilation errors
- Verify all new Hilt bindings resolve correctly (build-time KSP check)

### Manual Verification
- Run the app and navigate to the Register screen
- Click "Sign Up" with valid fields → confirm loading state appears → confirm navigation to Email Verification screen
- Verify the email is displayed correctly on the verification screen
- Tap "Go to Sign In" → confirm navigation to Login screen
- Tap "Resend Verification Email" → confirm loading state and snackbar message
- Toggle dark mode → confirm the verification screen renders correctly in both themes
- Switch language to Arabic → confirm all strings display in Arabic
