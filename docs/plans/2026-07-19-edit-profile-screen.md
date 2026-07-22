# Edit Profile Screen Implementation

This plan outlines the design and implementation of the **Edit Profile** screen. It supports light/dark modes, full EN/AR localization, custom input fields with right-aligned icons, chronic conditions and allergy chip selections, user confirmation on save (Domain safety requirement), and unit tests.

## User Review Required

> [!IMPORTANT]
> **Temporary Route Change**: The application `startDestination` in `NavGraph.kt` will be temporarily set to `EditProfileRoute` so that it opens directly into the new screen for design verification.
> **Health Safety Confirmation**: Saves must trigger a `ConfirmationDialog` confirming changes to chronic conditions or allergies before writing them, preventing accidental mis-edits.
> **Visual Polish**: Input fields will feature custom border styling, right-aligned icons (pencil for name/username, email envelope for email, lock shield for password), and theme-appropriate colors for Light and Dark modes.

## Open Questions

- None. Visual layout requirements are fully detailed in the provided screenshots and architecture constraints.

## Proposed Changes

### app

#### [MODIFY] [Route.kt](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/navigation/Route.kt)
- Add `@Serializable object EditProfileRoute`.

#### [MODIFY] [NavGraph.kt](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt)
- Import `EditProfileScreen` and `EditProfileRoute`.
- Add `composable<EditProfileRoute>` block mapping to `EditProfileScreen`.
- Set `startDestination = EditProfileRoute` temporarily.

---

### presentation

#### [NEW] [ic_pencil.xml](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/presentation/src/main/res/drawable/ic_pencil.xml)
- Vector drawable for text fields editing name and username.

#### [NEW] [ic_lock.xml](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/presentation/src/main/res/drawable/ic_lock.xml)
- Vector drawable for the password input field.

#### [MODIFY] [strings.xml](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/presentation/src/main/res/values/strings.xml)
- Add Edit Profile labels:
  - `edit_profile_name_hint`: "Name"
  - `edit_profile_username_hint`: "Username"
  - `edit_profile_email_hint`: "Email"
  - `edit_profile_password_hint`: "Password"
  - `edit_profile_confirm_title`: "Save Profile Changes?"
  - `edit_profile_confirm_message`: "Are you sure you want to modify your health profile? This will update future food safety scan verdicts."

#### [MODIFY] [strings.xml (Arabic)](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/presentation/src/main/res/values-ar/strings.xml)
- Add corresponding Arabic translations for Edit Profile strings.

#### [NEW] [EditProfileState.kt](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/profile/edit/state/EditProfileState.kt)
- State model containing name, username, email, password, conditions list, selected conditions, allergies list, selected allergies, custom inputs, show save confirmation dialog flag, loading, etc.

#### [NEW] [EditProfileEvent.kt](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/profile/edit/state/EditProfileEvent.kt)
- Input mutations, chip toggling, dialog confirmations, and save requests.

#### [NEW] [EditProfileEffect.kt](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/profile/edit/state/EditProfileEffect.kt)
- Navigation callbacks: `NavigateBack`.

#### [NEW] [EditProfileViewModel.kt](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/profile/edit/viewmodel/EditProfileViewModel.kt)
- Collect and update Edit Profile states, validate inputs, toggle conditions/allergies, handle "+ Other" custom chips, show the confirmation dialog, and trigger saving actions.

#### [NEW] [EditProfileInputField.kt](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/profile/edit/view/components/EditProfileInputField.kt)
- A highly polished input field featuring an optional right-aligned icon, customizable placeholder text, standard rounded corners, and a border style matching Light/Dark themes.

#### [NEW] [EditProfileScreen.kt](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/profile/edit/view/EditProfileScreen.kt)
- Main user interface layout with:
  - Top `AppBackButton` container.
  - Profile Avatar preview with edit overlay badge.
  - Four input fields: Name, Username, Email, Password.
  - Reused `SelectableChip` and `OtherInputChip` components for Conditions and Allergies.
  - Save button aligned to bottom container.
  - `ConfirmationDialog` integration.

#### [NEW] [EditProfileViewModelTest.kt](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/presentation/src/test/java/iti/grad/nutriscan/presentation/settings/profile/edit/EditProfileViewModelTest.kt)
- Unit tests validating:
  - Intial states.
  - Input field updates.
  - Toggling conditions/allergies (including custom tags via "+ Other").
  - Dialog flow (showing dialog -> confirming -> effect emission).

---

## Verification Plan

### Automated Tests
- Run `./gradlew :presentation:testDebugUnitTest --tests "iti.grad.nutriscan.presentation.settings.profile.edit.*"` to verify the ViewModel tests pass.
- Run `./gradlew compileDebugKotlin` to verify the codebase compiles successfully.

### Manual Verification
- Visual inspection of Light & Dark modes to ensure styling matches mockup images.
- Verify that clicking "Save" (light theme) or "Sign in" (dark theme) triggers the safety Confirmation Dialog.
- Verify proper back-navigation behavior when clicking the back button or confirming the dialog.
