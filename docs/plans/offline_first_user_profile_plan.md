# Implement Offline-First User Profile Strategy

This document outlines the architectural changes required to implement an Offline-First (Single Source of Truth) strategy for User Profile data across the Home, Profile (Editing), and Settings screens, strictly following the Clean Architecture and MVI guidelines specified for NutriScan AI.

## Architecture Decisions

1. **Event Naming Convention:** The use of `UiIntent` (e.g., `ProfileIntent`) is explicitly **BANNED**. We use `Event` (e.g., `ProfileEvent`).
2. **Immutable Collections in State:** We use `kotlinx.collections.immutable` (e.g., `ImmutableList`) in State classes instead of standard `List`. 
3. **Unified Single Source of Truth:** We are completely unifying the basic identity data (name, email) and the health data (height, weight, diseases, allergies) into a single `UserEntity` and a single `IUserRepository`. This unified entity acts as the true Single Source of Truth for the authenticated user.

## Proposed Implementation

### 1. Data Layer: Room Database

#### [NEW] `data/src/main/kotlin/iti/grad/nutriscan/data/db/entity/UserEntity.kt`
- Data class representing the full user profile (basic info + health info).
- Fields: `id`, `firstName`, `lastName`, `email`, `heightCm`, `weightKg`, `diseaseIds` (JSON or TypeConverter), `allergyIds` (JSON or TypeConverter).

#### [NEW] `data/src/main/kotlin/iti/grad/nutriscan/data/db/dao/UserDao.kt`
- `@Query("SELECT * FROM users LIMIT 1") fun getUserFlow(): Flow<UserEntity?>`
- `@Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertOrUpdateUser(user: UserEntity)`
- `@Query("DELETE FROM users") suspend fun deleteUser()`

#### [MODIFY] `data/src/main/kotlin/iti/grad/nutriscan/data/db/NutriScanDatabase.kt`
- Add `UserEntity::class` to the `@Database` entities array.
- Expose `abstract fun userDao(): UserDao`.

---

### 2. Domain & Data Repository Layer

#### [NEW] `data/src/main/kotlin/iti/grad/nutriscan/data/remote/dto/ProfileUpdateRequest.kt`
- DTO for Retrofit API calls containing updatable user fields.

#### [MODIFY] `domain/src/main/kotlin/iti/grad/nutriscan/domain/user/repository/IUserRepository.kt`
- Define `User` domain model (or use `User` in `domain/user/model/User.kt`).
- Add `fun getUserData(): Flow<User?>`.
- Add `suspend fun fetchAndSyncProfile(): Result<Unit>`.
- Add `suspend fun updateProfile(request: ProfileUpdateRequest): Result<Unit>`.
- Remove legacy `updateHealthProfile` as it's replaced by `updateProfile`.

#### [NEW] `data/src/main/kotlin/iti/grad/nutriscan/data/repository/UserRepositoryImpl.kt`
- Implements `IUserRepository`.
- Injects `UserDao` and `UserApiService`.
- `fetchAndSyncProfile`: Fetches from API -> Maps to `UserEntity` -> `userDao.insertOrUpdateUser()`.
- `updateProfile`: Calls API -> If successful, creates an updated `UserEntity` and saves it to Room immediately.

---

### 3. Presentation Layer: MVI Contracts & ViewModels

#### [MODIFY] Home Screen (`presentation/home/`)
- **HomeState**: Add `val firstName: String = ""`, `val isLoading: Boolean = false`.
- **HomeViewModel**: 
  - Inject `IUserRepository`.
  - In `init`, `viewModelScope.launch { userRepository.getUserData().collectLatest { user -> _state.update { it.copy(firstName = user?.firstName ?: "") } } }`.

#### [NEW] Settings Screen (`presentation/settings/main/`)
- **SettingsState**: `val fullName: String = ""`, `val email: String = ""`.
- **SettingsEvent**: User actions for the settings screen.
- **SettingsEffect**: Navigation effects.
- **SettingsViewModel**:
  - Collects `userRepository.getUserData()` and maps to `SettingsState`.

#### [MODIFY] Profile Screen (`presentation/settings/profile/`) 
*(Assuming this is the Edit Profile screen based on requirements)*
- **ProfileState**: Holds `firstName`, `lastName`, `heightCm`, `weightKg`, `isLoading`, `isSaving`.
- **ProfileEvent**: `LoadProfile`, `UpdateField(key, value)`, `SaveProfile`.
- **ProfileEffect**: `ShowToast(messageResId)`, `NavigateBack`.
- **ProfileViewModel**:
  - `init { fetchProfileBackground() }` (calls `fetchAndSyncProfile()` silently).
  - Collects `getUserData()` to populate state.
  - On `SaveProfile` event: Sets `isSaving = true`, calls `updateProfile()`, emits `ShowToast` effect, sets `isSaving = false`.

### 4. Architectural Enhancements (Completed)
- **Unified UseCase (`UpdateUserProfileUseCase`)**: Merged the old `UpdateHealthProfileUseCase` into a comprehensive `UpdateUserProfileUseCase` that manages all user identity and health fields, eliminating direct `UserRepositoryImpl` injections in ViewModels.
- **Optimistic UI with Failure Rollback**: Modified `UserRepositoryImpl` to perform an immediate optimistic local DB update to ensure UI responsiveness. Added a strict **rollback mechanism** where, if the remote API sync fails, the local changes are reverted to the previous state, preventing false positives and ensuring true data consistency for critical health profiles.
- **Removed Over-Fetching**: Stopped calling `fetchAndSyncProfile()` after a successful API update, reducing network overhead, as our optimistic update has already set the correct Local DB state.
