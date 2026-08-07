# Delete Account Feature — Implementation Plan

**Feature:** Delete Account with Grace Period & Account Restoration  
**Status:** Ready for Implementation  
**Architecture:** Clean Architecture + MVI (as per AGENTS.md)

---

## Overview

This feature allows a user to permanently delete their account from the App Settings screen.
The backend enforces a **grace period** (15 days by default). During this window the account
is not yet removed, so if the user logs in again they are shown a dedicated
**"Account Deletion Pending"** screen where they can choose to **Restore** the account or
**Log Out**.

### Full User Flow

```
Settings → Delete Account button
    → ConfirmationDialog
        → [Confirmed] → DELETE /v1/users/profile
            → Logout → Auth screens
                → [User logs in again within grace period]
                    → Home triggers fetchAndSyncProfile()
                        → 409 ACCOUNT_PENDING_DELETION
                            → AccountPendingDeletion screen
                                → [Restore] → POST /v1/users/profile/restore → Home
                                → [Log Out] → Auth screens
```

---

## API Contract

### Delete Account
```
DELETE https://nutriscan.dev/api/v1/users/profile
Authorization: Bearer <token>

200 OK
{
  "scheduledDeletionAt": "2026-08-06",
  "gracePeriodDays": 15
}
```

### Restore Account
```
POST https://nutriscan.dev/api/v1/users/profile/restore
Authorization: Bearer <token>

200 OK
{
  "message": "Your account has been restored.",
  "restoredAt": "2026-08-06T22:43:50.214895048Z"
}
```

### 409 Error — Account Pending Deletion (returned by GET /v1/users/profile)
```json
{
  "timestamp": "2026-08-06T22:39:46.192540174Z",
  "status": 409,
  "error": "ACCOUNT_PENDING_DELETION",
  "message": "Account is already scheduled for deletion on 2026-08-22",
  "details": null,
  "path": "http://nutriscan.dev/api/v1/users/profile"
}
```

---

## Task Breakdown

Tasks must be implemented **in order** — each task depends on the previous one.

---

### TASK 1 — Domain: Model + Repository Interface

**Files to create/modify:**

#### [NEW] domain/src/main/kotlin/iti/grad/nutriscan/domain/user/model/AccountDeletionInfo.kt
```kotlin
package iti.grad.nutriscan.domain.user.model

data class AccountDeletionInfo(
    val scheduledDeletionAt: String,   // e.g. "2026-08-22"
    val gracePeriodDays: Int
)
```

#### [MODIFY] domain/src/main/kotlin/iti/grad/nutriscan/domain/user/repository/IUserRepository.kt
Add two new suspend functions:
```kotlin
/** Schedules the account for deletion. Returns deletion date and grace period info. */
suspend fun deleteAccount(): Result<AccountDeletionInfo>

/** Cancels a pending deletion and restores the account. */
suspend fun restoreAccount(): Result<Unit>
```

---

### TASK 2 — Domain: Use Cases

**Files to create:**

#### [NEW] domain/src/main/kotlin/iti/grad/nutriscan/domain/user/usecase/DeleteAccountUseCase.kt
```kotlin
package iti.grad.nutriscan.domain.user.usecase

import iti.grad.nutriscan.domain.user.model.AccountDeletionInfo
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(): Result<AccountDeletionInfo> =
        userRepository.deleteAccount()
}
```

#### [NEW] domain/src/main/kotlin/iti/grad/nutriscan/domain/user/usecase/RestoreAccountUseCase.kt
```kotlin
package iti.grad.nutriscan.domain.user.usecase

import iti.grad.nutriscan.domain.user.repository.IUserRepository
import javax.inject.Inject

class RestoreAccountUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(): Result<Unit> =
        userRepository.restoreAccount()
}
```

---

### TASK 3 — Data: DTO + Exception

**Files to create:**

#### [NEW] data/src/main/kotlin/iti/grad/nutriscan/data/remote/dto/AccountDeletionResponseDto.kt
```kotlin
package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountDeletionResponseDto(
    @SerialName("scheduledDeletionAt") val scheduledDeletionAt: String,
    @SerialName("gracePeriodDays") val gracePeriodDays: Int
)

@Serializable
data class RestoreAccountResponseDto(
    @SerialName("message") val message: String = "",
    @SerialName("restoredAt") val restoredAt: String = ""
)
```

#### [NEW] data/src/main/kotlin/iti/grad/nutriscan/data/util/AccountPendingDeletionException.kt
```kotlin
package iti.grad.nutriscan.data.util

/**
 * Thrown when the backend returns HTTP 409 ACCOUNT_PENDING_DELETION
 * on GET /v1/users/profile.
 *
 * This is NOT a generic network error — it is a domain-specific signal
 * that must be propagated to the UI layer to redirect to the
 * AccountPendingDeletion screen. Never swallow this exception.
 */
class AccountPendingDeletionException(
    val scheduledDeletionAt: String
) : Exception("ACCOUNT_PENDING_DELETION: scheduled for $scheduledDeletionAt")
```

---

### TASK 4 — Data: API Service + DataSource + Repository

**Files to modify:**

#### [MODIFY] data/src/main/kotlin/iti/grad/nutriscan/data/remote/api/UserApiService.kt
Add:
```kotlin
@DELETE("v1/users/profile")
suspend fun deleteAccount(): AccountDeletionResponseDto

@POST("v1/users/profile/restore")
suspend fun restoreAccount(): Response<Unit>
```

#### [MODIFY] data/src/main/kotlin/iti/grad/nutriscan/data/remote/datasource/IUserRemoteDataSource.kt
Add:
```kotlin
suspend fun deleteAccount(): AccountDeletionResponseDto
suspend fun restoreAccount(): Response<Unit>
```

#### [MODIFY] data/src/main/kotlin/iti/grad/nutriscan/data/remote/datasource/UserRemoteDataSourceImpl.kt
Implement the two new methods by delegating to userApiService.

#### [MODIFY] data/src/main/kotlin/iti/grad/nutriscan/data/repository/UserRepositoryImpl.kt

Key changes:
1. Implement deleteAccount() — call remote, map DTO to domain model.
2. Implement restoreAccount() — call remote, check response.
3. CRITICAL: In fetchAndSyncProfile(), when a 409 ACCOUNT_PENDING_DELETION HTTP error
   is received, parse the scheduled date from the error body and emit it via a SharedFlow
   (`accountPendingDeletionEvent`) instead of throwing an exception:

```kotlin
// Inside the catch block of fetchAndSyncProfile():
} catch (e: HttpException) {
    if (e.code() == 409) {
        val body = e.response()?.errorBody()?.string()
        val parsed = json.decodeFromString<ApiErrorDto>(body ?: "{}")
        if (parsed.error == "ACCOUNT_PENDING_DELETION") {
            // Extract date from message: "Account is already scheduled for deletion on 2026-08-22"
            val scheduledDate = parsed.message.substringAfterLast("on ").trim()
            _accountPendingDeletionEvent.emit(scheduledDate)
            return Result.failure(AccountPendingDeletionException(scheduledDate))
        }
    }
    Result.failure(e)
}
```

Since this uses a `SharedFlow`, the app does not crash, and the UI layer (HomeViewModel)
can safely observe this event and navigate to the pending deletion screen.

---

### TASK 5 — Presentation: Settings — Delete Account

**Files to modify:**

#### [MODIFY] presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/app/state/AppSettingsEvent.kt
Add:
```kotlin
data object DeleteAccountClicked : AppSettingsEvent
data object DeleteAccountConfirmed : AppSettingsEvent
data object DeleteAccountDismissed : AppSettingsEvent
```

#### [MODIFY] presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/app/state/AppSettingsState.kt
Add:
```kotlin
val showDeleteAccountConfirmDialog: Boolean = false,
val isDeletingAccount: Boolean = false,
val deleteAccountError: String? = null,
```

#### [MODIFY] presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/app/state/AppSettingsEffect.kt
Add:
```kotlin
data object AccountDeleted : AppSettingsEffect   // triggers logout then navigate to Login
```

#### [MODIFY] presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/app/viewmodel/AppSettingsViewModel.kt
- Inject DeleteAccountUseCase.
- LogoutUseCase already injected.
- Handle new events:
  - DeleteAccountClicked → showDeleteAccountConfirmDialog = true
  - DeleteAccountDismissed → showDeleteAccountConfirmDialog = false
  - DeleteAccountConfirmed → call deleteAccountUseCase() → on success call logoutUseCase() → emit AccountDeleted

#### [MODIFY] presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/app/view/AppSettingsScreen.kt
- Add a red-tinted "Delete Account" SettingsActionRow below LogoutButton.
- Add ConfirmationDialog for delete account (reuse existing component).
- Handle AppSettingsEffect.AccountDeleted → onNavigateToLogin().

---

### TASK 6 — Presentation: AccountPendingDeletion Screen

This is the main new screen, matching the provided design mockup exactly.

Package: presentation/src/main/kotlin/iti/grad/nutriscan/presentation/account_deletion/

**Files to create:**

#### state/AccountPendingDeletionState.kt
```kotlin
@Immutable
data class AccountPendingDeletionState(
    val scheduledDeletionAt: String = "",
    val daysRemaining: Int = 0,
    val formattedDeletionDate: String = "",
    val isRestoring: Boolean = false,
    val error: String? = null
)
```

#### state/AccountPendingDeletionEvent.kt
```kotlin
sealed interface AccountPendingDeletionEvent {
    data object RestoreAccountClicked : AccountPendingDeletionEvent
    data object LogoutClicked : AccountPendingDeletionEvent
    data object ErrorDismissed : AccountPendingDeletionEvent
}
```

#### state/AccountPendingDeletionEffect.kt
```kotlin
sealed interface AccountPendingDeletionEffect {
    data object NavigateToHome : AccountPendingDeletionEffect
    data object NavigateToLogin : AccountPendingDeletionEffect
}
```

#### viewmodel/AccountPendingDeletionViewModel.kt
- Receives scheduledDeletionAt: String via SavedStateHandle
- On init: calculates daysRemaining from today to scheduledDeletionAt
- RestoreAccountClicked → call restoreAccountUseCase() → on success emit NavigateToHome
- LogoutClicked → call logoutUseCase() → emit NavigateToLogin

#### view/AccountPendingDeletionScreen.kt

Design spec (from mockup):
- Top section (Teal gradient): Title "Restore Account" + subtitle "Your account is scheduled for deletion."
- Center icon: Large circular restore/sync icon with 2 concentric teal rings (opacity layered)
- Heading: "Account Deletion Pending" (bold, dark)
- Sub-description: "You can restore your account within the grace period to retain all your data and settings."
- Info card (rounded, light gray background):
  - Row: "Days remaining to restore:" → "X days" (red/error color, bold)
  - Row: "Scheduled Deletion Date:" → "19 Aug 2026" (TextPrimary, bold)
- Primary CTA button: "Restore My Account" (full-width teal button)
- Footer text: "Want to exit? " + "Log Out" (teal clickable text)

Supports:
- Dark mode and Light mode
- English and Arabic (RTL-aware)

---

### TASK 7 — String Resources (EN + AR)

#### [MODIFY] presentation/src/main/res/values/strings.xml
```xml
<!-- Settings — Delete Account -->
<string name="delete_account_button_label">Delete Account</string>
<string name="delete_account_confirm_title">Delete Account?</string>
<string name="delete_account_confirm_message">This will schedule your account for permanent deletion. You have a 15-day grace period to restore it before all your data is lost.</string>
<string name="delete_account_confirm_action">Delete</string>
<string name="deleting_account_loading">Deleting account…</string>

<!-- Account Pending Deletion Screen -->
<string name="account_pending_deletion_header_title">Restore Account</string>
<string name="account_pending_deletion_header_subtitle">Your account is scheduled for deletion.</string>
<string name="account_pending_deletion_heading">Account Deletion Pending</string>
<string name="account_pending_deletion_description">You can restore your account within the grace period to retain all your data and settings.</string>
<string name="account_pending_deletion_days_remaining_label">Days remaining to restore:</string>
<string name="account_pending_deletion_days_remaining_value">%d days</string>
<string name="account_pending_deletion_scheduled_date_label">Scheduled Deletion Date:</string>
<string name="account_pending_deletion_restore_btn">Restore My Account</string>
<string name="account_pending_deletion_restoring">Restoring…</string>
<string name="account_pending_deletion_logout_prompt">Want to exit?</string>
<string name="account_pending_deletion_logout_link">Log Out</string>
<string name="account_pending_deletion_restore_error">Failed to restore account. Please try again.</string>
```

#### [MODIFY] presentation/src/main/res/values-ar/strings.xml
```xml
<!-- Settings — Delete Account -->
<string name="delete_account_button_label">حذف الحساب</string>
<string name="delete_account_confirm_title">حذف الحساب؟</string>
<string name="delete_account_confirm_message">سيتم جدولة حسابك للحذف النهائي. لديك مهلة 15 يومًا لاسترجاعه قبل فقدان جميع بياناتك.</string>
<string name="delete_account_confirm_action">حذف</string>
<string name="deleting_account_loading">جارٍ حذف الحساب…</string>

<!-- Account Pending Deletion Screen -->
<string name="account_pending_deletion_header_title">استعادة الحساب</string>
<string name="account_pending_deletion_header_subtitle">تم جدولة حسابك للحذف.</string>
<string name="account_pending_deletion_heading">الحذف قيد الانتظار</string>
<string name="account_pending_deletion_description">يمكنك استعادة حسابك خلال فترة السماح للاحتفاظ بجميع بياناتك وإعداداتك.</string>
<string name="account_pending_deletion_days_remaining_label">الأيام المتبقية للاستعادة:</string>
<string name="account_pending_deletion_days_remaining_value">%d أيام</string>
<string name="account_pending_deletion_scheduled_date_label">تاريخ الحذف المجدول:</string>
<string name="account_pending_deletion_restore_btn">استعادة حسابي</string>
<string name="account_pending_deletion_restoring">جارٍ الاستعادة…</string>
<string name="account_pending_deletion_logout_prompt">تريد الخروج؟</string>
<string name="account_pending_deletion_logout_link">تسجيل الخروج</string>
<string name="account_pending_deletion_restore_error">فشل استعادة الحساب. يرجى المحاولة مرة أخرى.</string>
```

---

### TASK 8 — AppColors: New Extension for Account Deletion Screen

#### [MODIFY] presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/theme/AppColors.kt

Add a new AppColorsExtension4 (to avoid the D8 VerifyError constructor limit):
```kotlin
@Immutable
internal data class AppColorsExtension4(
    val AccountDeletionHeaderBg: Color,         // Teal1000 light / Teal1400 dark
    val AccountDeletionIconRingOuter: Color,     // Teal200 with alpha
    val AccountDeletionIconRingMiddle: Color,    // Teal400 with alpha
    val AccountDeletionIconBg: Color,            // Teal1000 / Teal800
    val AccountDeletionInfoCardBg: Color,        // Gray100 / surface variant
    val AccountDeletionDaysRemainingText: Color, // Error color (same both themes)
    val AccountDeletionDateText: Color,          // TextPrimary
    val AccountDeletionLogoutLinkText: Color,    // Teal1000 / Teal600
)
```

Add forwarding properties to AppColors and values to lightColors() and darkColors().

---

### TASK 9 — Navigation: Route + NavGraph + HomeEffect

#### [MODIFY] app/src/main/kotlin/iti/grad/nutriscan/navigation/Route.kt
Add:
```kotlin
@Serializable
data class AccountPendingDeletionRoute(val scheduledDeletionAt: String)
```

#### [MODIFY] presentation/src/main/kotlin/iti/grad/nutriscan/presentation/home/state/HomeEffect.kt
Add:
```kotlin
data class NavigateToAccountPendingDeletion(
    val scheduledDeletionAt: String
) : HomeEffect
```

#### [MODIFY] presentation/src/main/kotlin/iti/grad/nutriscan/presentation/home/viewmodel/HomeViewModel.kt
Observe the `accountPendingDeletionEvent` from UserRepository:
```kotlin
viewModelScope.launch {
    userRepository.accountPendingDeletionEvent.collect { scheduledDeletionAt ->
        emitEffect(HomeEffect.NavigateToAccountPendingDeletion(scheduledDeletionAt))
    }
}
```

#### [MODIFY] presentation/src/main/kotlin/iti/grad/nutriscan/presentation/home/view/HomeScreen.kt
Add onNavigateToAccountPendingDeletion: (String) -> Unit parameter and wire the effect.

#### [MODIFY] presentation/src/main/kotlin/iti/grad/nutriscan/presentation/main/container/view/MainScreen.kt
Thread the new lambda parameter down from MainScreen to HomeScreen.

#### [MODIFY] app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt

1. In the MainRoute composable, add:
```kotlin
onNavigateToAccountPendingDeletion = { scheduledDeletionAt ->
    navController.navigate(AccountPendingDeletionRoute(scheduledDeletionAt)) {
        popUpTo(0) { inclusive = true }
    }
},
```

2. Add a new destination:
```kotlin
composable<AccountPendingDeletionRoute> { backStackEntry ->
    val route = backStackEntry.toRoute<AccountPendingDeletionRoute>()
    AccountPendingDeletionScreen(
        scheduledDeletionAt = route.scheduledDeletionAt,
        onNavigateToHome = {
            navController.navigate(MainRoute()) {
                popUpTo(0) { inclusive = true }
            }
        },
        onNavigateToLogin = {
            navController.navigate(LoginRoute()) {
                popUpTo(0) { inclusive = true }
            }
        }
    )
}
```

---

## Summary Table

| # | Task | Layer | Risk |
|---|------|-------|------|
| 1 | Domain Model + IUserRepository | Domain | Low |
| 2 | Delete + Restore Use Cases | Domain | Low |
| 3 | DTO + AccountPendingDeletionException | Data | Low |
| 4 | API Service + DataSource + Repository | Data | Medium |
| 5 | Settings Delete Account (MVI) | Presentation | Medium |
| 6 | AccountPendingDeletion Screen | Presentation | Medium |
| 7 | String Resources EN + AR | Resources | Low |
| 8 | AppColors Extension4 | Presentation/Theme | Low |
| 9 | Navigation wiring | App / Nav | High (many touch points) |
| 10 | Comprehensive Unit Tests | Tests | Medium |

Start with Task 1. Do not skip tasks.

---

### TASK 10 — Unit Tests

**Files to create/modify:**
- `DeleteAccountUseCaseTest.kt` & `RestoreAccountUseCaseTest.kt`: Verify API calls.
- `AppSettingsViewModelTest.kt`: Test `DeleteAccountClicked`, `DeleteAccountConfirmed`, `DeleteAccountDismissed`.
- `AccountPendingDeletionViewModelTest.kt`: Test `RestoreAccountClicked` and `LogoutClicked`.
- Fix legacy broken tests due to constructor injection changes.

---

## Important Technical Notes

**SAFETY RULE:** The AccountPendingDeletionException must NEVER be silently swallowed.
If the backend returns 409, the user MUST be redirected to the pending deletion screen.
Failing silently here would leave the user in an inconsistent state.

**Polling behaviour:** UserRepositoryImpl triggers fetchAndSyncProfile periodically. When AccountPendingDeletionException is encountered, we emit it to a SharedFlow which navigates the user out, stopping them from seeing their data.

**Local data cleanup:** On successful deleteAccount(), the local Room database does NOT
need to be wiped immediately — the subsequent logoutUseCase() call handles that.
