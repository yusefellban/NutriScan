# Plan: Integrate Profile Setup API with Gender, DOB, Height, and Weight

This plan outlines the steps to integrate the user profile update API in the setup profile screens, ensuring that gender, date of birth, height, and weight are persisted to the backend along with diseases and allergies.

## 1. Feature Summary
- Update the `PATCH /v1/users/profile` integration.
- Persist `gender`, `dateOfBirth`, `heightCm`, and `weightKg` during the onboarding profile setup.
- Follow Clean Architecture and MVI patterns.

## 2. Files to Create
None. Existing files will be modified.

## 3. Files to Modify
- [IUserRepository.kt](file:///home/yousef/Desktop/NutriScan/domain/src/main/kotlin/iti/grad/nutriscan/domain/user/repository/IUserRepository.kt): Update interface to include new fields.
- [UserRepositoryImpl.kt](file:///home/yousef/Desktop/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/repository/UserRepositoryImpl.kt): Implement new fields and handle date formatting.
- [UpdateHealthProfileUseCase.kt](file:///home/yousef/Desktop/NutriScan/domain/src/main/kotlin/iti/grad/nutriscan/domain/user/usecase/UpdateHealthProfileUseCase.kt): Update to accept new fields.
- [ProfileSetupPagerViewModel.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/profile_setup/viewmodel/ProfileSetupPagerViewModel.kt): Pass state values to the UseCase.

## 4. Layer Breakdown

### Domain
- **IUserRepository**: Add `gender`, `dateOfBirth`, `heightCm`, and `weightKg` (as `Double?` or `Int?`) to `updateHealthProfile`.
- **UpdateHealthProfileUseCase**: Update `invoke` to accept the new parameters.

### Data
- **UserRepositoryImpl**: 
    - Convert `dateOfBirth` (Long millis) to `yyyy-MM-dd` format.
    - Map `gender` enum to String.
    - Include all fields in `UpdateUserProfileRequestDto`.

### Presentation
- **ProfileSetupPagerViewModel**: In `saveProfile()`, extract `selectedGender`, `selectedDateOfBirthMillis`, `selectedHeightCm`, and `selectedWeightKg` from the state and pass them to the UseCase.

## 5. Navigation Changes
None. The flow is already established in `ProfileSetupPagerScreen`.

## 6. Strings — MANDATORY (Zero Hardcoded Text)
Existing strings are used in the UI. No new strings are introduced for this integration.

## 7. Testing Plan
- Update `ProfileSetupPagerViewModelTest.kt` to verify that `updateHealthProfileUseCase` is called with the correct arguments from the state.

## 8. Edge Cases
- `dateOfBirth` being null (should handle gracefully if optional, but usually required in setup).
- `gender` being null.
- Network errors during API call (already handled in ViewModel with snackbar).

## 9. Definition of Done
- [ ] `IUserRepository` updated.
- [ ] `UserRepositoryImpl` updated with date formatting.
- [ ] `UpdateHealthProfileUseCase` updated.
- [ ] `ProfileSetupPagerViewModel` passing all 6 fields to the UseCase.
- [ ] App builds and runs.
