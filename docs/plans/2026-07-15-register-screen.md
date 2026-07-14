# Register Screen Implementation Plan

## Goal Description
Implement the "Sign Up" (Register) screen for NutriScan AI based on the provided Figma specifications. The implementation must strictly adhere to the project's Clean Architecture, MVI, and testing rules.

## User Review Required
> [!IMPORTANT]
> Please review the MVI contract and the UI component split before execution begins.

## Proposed Changes

### Documentation
#### [NEW] docs/plans/2026-07-15-register-screen.md
- Create the mandatory planning document detailing the feature, MVI contract, UI components, and test plan.

### Resources
#### [MODIFY] app/src/main/res/values/strings.xml
- Add English string resources for the Sign Up screen.
#### [MODIFY] app/src/main/res/values-ar/strings.xml
- Add Arabic string resources for the Sign Up screen.

### Presentation - Register Feature
#### [NEW] presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/register/RegisterContract.kt
- Define `RegisterState`, `RegisterEvent`, and `RegisterEffect`.
#### [NEW] presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/register/RegisterViewModel.kt
- Create the ViewModel to handle validation, state updates, and effects.
#### [NEW] presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/register/RegisterScreen.kt
- Implement the Stateful `RegisterScreen` and Stateless `RegisterContent`.

### Tests
#### [NEW] presentation/src/test/kotlin/iti/grad/nutriscan/presentation/auth/register/RegisterViewModelTest.kt
- Write unit tests for the ViewModel using JUnit5 and Turbine.

## Verification Plan
### Automated Tests
- Run `RegisterViewModelTest` to ensure all validation and state transitions work as expected.
### Manual Verification
- Deploy the app and manually verify the Sign Up screen layout and functionality in both Light and Dark themes.
