# Registration Refactor — Add First Name, Last Name & Auto-Generated Username

## Background

The registration flow currently collects only `email`, `password`, and `confirmPassword`. The backend `RegisterRequestDto` already contains `firstName`, `lastName`, and `username` fields, but the repository hard-codes placeholder values (`emailPrefix`, `""`, `email`). This refactor surfaces `firstName` and `lastName` as real user-facing fields and auto-generates `username` from the email in the repository layer — exactly where it belongs under Clean Architecture.

## Proposed Changes

---

### Domain Layer

#### [MODIFY] [IAuthRepository.kt](file:///home/yousef/Desktop/NutriScan/domain/src/main/kotlin/iti/grad/nutriscan/domain/auth/repository/IAuthRepository.kt)
- Update `register` signature: `suspend fun register(firstName: String, lastName: String, email: String, password: String): Result<Unit>`

#### [MODIFY] [RegisterUseCase.kt](file:///home/yousef/Desktop/NutriScan/domain/src/main/kotlin/iti/grad/nutriscan/domain/auth/usecase/RegisterUseCase.kt)
- Forward `firstName` and `lastName` to the repository call.
- Signature: `suspend operator fun invoke(firstName: String, lastName: String, email: String, password: String): Result<Unit>`

---

### Data Layer

#### [MODIFY] [AuthRepositoryImpl.kt](file:///home/yousef/Desktop/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/repository/AuthRepositoryImpl.kt)
- Update `register` to accept `firstName`, `lastName`, `email`, `password`.
- Auto-generate `username = email.substringBefore("@")` here (inside the repository — this is the correct Clean Architecture layer for mapping concerns).
- Build the `RegisterRequestDto` with real `firstName`, `lastName`, and the generated `username`.

> **Note:** `RegisterRequestDto` already has all required fields (`firstName`, `lastName`, `username`, etc.) — no DTO changes needed.

---

### Presentation Layer — State

#### [MODIFY] [RegisterState.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/register/state/RegisterState.kt)
Add:
```kotlin
val firstName: String = ""
val lastName: String = ""
val firstNameErrorResId: Int? = null
val lastNameErrorResId: Int? = null
```

#### [MODIFY] [RegisterEvent.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/register/state/RegisterEvent.kt)
Add two new events:
```kotlin
data class FirstNameChanged(val value: String) : RegisterEvent
data class LastNameChanged(val value: String) : RegisterEvent
```

---

### Presentation Layer — ViewModel

#### [MODIFY] [RegisterViewModel.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/register/viewmodel/RegisterViewModel.kt)
- Handle `FirstNameChanged` and `LastNameChanged` events.
- Add validation for `firstName` and `lastName` (blank check only — consistent with existing approach).
- Pass `firstName`, `lastName`, `email`, `password` to `registerUseCase`.

**Validation approach** (mirrors existing pattern):
```kotlin
val firstNameError = if (currentState.firstName.isBlank()) R.string.error_empty_field else null
val lastNameError  = if (currentState.lastName.isBlank())  R.string.error_empty_field else null
```

---

### Presentation Layer — UI

#### [MODIFY] [RegisterFormBody.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/register/view/components/RegisterFormBody.kt)
Add two `FigmaInputField` entries — **First Name** and **Last Name** — at the top of the form, before the Email field. Uses the same spacing (`verticalArrangement = Arrangement.spacedBy(16.dp)`), same icon, same error pattern as the existing fields.

---

### String Resources

#### [MODIFY] [strings.xml](file:///home/yousef/Desktop/NutriScan/presentation/src/main/res/values/strings.xml)
Add:
```xml
<string name="first_name_label">First Name</string>
<string name="first_name_hint">Enter your first name…</string>
<string name="last_name_label">Last Name</string>
<string name="last_name_hint">Enter your last name…</string>
```

---

### Tests

#### [MODIFY] [RegisterViewModelTest.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/test/kotlin/iti/grad/nutriscan/presentation/auth/register/viewmodel/RegisterViewModelTest.kt)
- Update `registerUseCase` mock to match the new 4-parameter signature.
- Add test: `SignUpClicked with blank first name sets error state`.
- Add test: `SignUpClicked with blank last name sets error state`.

---

## Data Flow (unchanged structure)

```
UI (RegisterFormBody)
  → onEvent(FirstNameChanged / LastNameChanged / EmailChanged / ...)
    → RegisterViewModel.onEvent()
      → handleSignUp() validates all fields
        → registerUseCase(firstName, lastName, email, password)
          → IAuthRepository.register(firstName, lastName, email, password)
            → AuthRepositoryImpl: generates username, builds RegisterRequestDto, calls API
```

## Verification Plan

### Build check
```
./gradlew :domain:compileDebugKotlin :data:compileDebugKotlin :presentation:compileDebugKotlin :app:compileDebugKotlin
```

### Unit tests
```
./gradlew :presentation:test
```

### Manual
- Run the app, open the Register screen → two new fields (First Name, Last Name) appear at the top.
- Submit without filling them → both show `error_empty_field` error banners.
- Fill all fields and submit → registration succeeds end-to-end with correct payload.
