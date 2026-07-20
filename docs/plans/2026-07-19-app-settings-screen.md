# Plan: App Settings Screen

## 1. Feature Summary

Implements the "App Settings" screen: a 242dp teal header with back button/title/subtitle above a scrollable list of setting rows (Profile Settings, Appearance theme toggle, Language toggle, Terms and Conditions, Help) and a Logout button. Wires the already-declared `AppSettingsRoute` (currently a `PlaceholderScreen`) to the real screen, and adds the minimum domain/data plumbing to persist theme mode and language selection via DataStore and apply them app-wide.

## 2. Files to Create

| File | Purpose |
|---|---|
| `domain/.../settings/model/ThemeMode.kt` | `enum class ThemeMode { SYSTEM, DARK, LIGHT }` |
| `domain/.../settings/model/AppLanguage.kt` | `enum class AppLanguage { EN, AR }` |
| `domain/.../settings/repository/IThemeRepository.kt` | get/set theme mode |
| `domain/.../settings/repository/ILanguageRepository.kt` | get/set language |
| `domain/.../settings/usecase/GetThemeModeUseCase.kt` | reads persisted theme mode |
| `domain/.../settings/usecase/SetThemeModeUseCase.kt` | persists theme mode |
| `domain/.../settings/usecase/GetLanguageUseCase.kt` | reads persisted language |
| `domain/.../settings/usecase/SetLanguageUseCase.kt` | persists language |
| `data/.../local/datasource/IThemePreferencesDataSource.kt` + `ThemePreferencesDataSourceImpl.kt` | DataStore-backed theme storage |
| `data/.../local/datasource/ILanguagePreferencesDataSource.kt` + `LanguagePreferencesDataSourceImpl.kt` | DataStore-backed language storage |
| `data/.../repository/ThemeRepositoryImpl.kt` | implements `IThemeRepository` |
| `data/.../repository/LanguageRepositoryImpl.kt` | implements `ILanguageRepository` |
| `presentation/.../settings/app/state/AppSettingsState.kt` | screen state |
| `presentation/.../settings/app/state/AppSettingsEvent.kt` | user actions |
| `presentation/.../settings/app/state/AppSettingsEffect.kt` | one-shot effects |
| `presentation/.../settings/app/viewmodel/AppSettingsViewModel.kt` | screen ViewModel |
| `presentation/.../settings/app/view/AppSettingsScreen.kt` | stateful root + content composable |
| `presentation/.../settings/app/view/components/AppSettingsHeader.kt` | teal header |
| `presentation/.../settings/app/view/components/SettingsActionRow.kt` | base row shell |
| `presentation/.../settings/app/view/components/SettingsToggleRow.kt` | row with segmented toggle trailing |
| `presentation/.../settings/app/view/components/SettingsSegmentedToggle.kt` | 2/3-way pill toggle |
| `presentation/.../settings/app/view/components/LogoutButton.kt` | logout row |
| `presentation/.../common/components/ConfirmationDialog.kt` | shared confirm dialog (logout) |
| `app/.../MainActivityViewModel.kt` | exposes persisted theme mode to `MainActivity` |
| `presentation/src/test/.../settings/app/AppSettingsViewModelTest.kt` | ViewModel tests |
| `domain/src/test/.../settings/usecase/*UseCaseTest.kt` (x4) | use case tests |

No new drawable assets — row icons use the existing `material-icons-extended` dependency; chevron reuses `ic_arrow_right.xml`; back button reuses `AppBackButton`.

## 3. Files to Modify

| File | Change |
|---|---|
| `app/.../navigation/NavGraph.kt` | Replace `AppSettingsRoute` placeholder block with real `AppSettingsScreen` |
| `app/.../MainActivity.kt` | Collect persisted theme mode, pass into `AppTheme(darkTheme = ...)` |
| `app/.../di/DataSourceModule.kt` | Bind the 2 new preference data sources |
| `app/.../di/RepositoryModule.kt` | Bind `IThemeRepository`/`ILanguageRepository` |
| `presentation/.../common/theme/AppColors.kt` | Add `// --- App Settings ---` section (9 fields) |
| `presentation/src/main/res/values/strings.xml` | Add App Settings strings (EN) |
| `presentation/src/main/res/values-ar/strings.xml` | Add App Settings strings (AR) |

## 4. Layer Breakdown

### Domain
- Models: `ThemeMode`, `AppLanguage` (plain enums)
- Repositories: `IThemeRepository { getThemeMode/setThemeMode }`, `ILanguageRepository { getLanguage/setLanguage }` — two separate interfaces so the ViewModel test can mock them independently
- UseCases: `GetThemeModeUseCase`, `SetThemeModeUseCase`, `GetLanguageUseCase`, `SetLanguageUseCase` — one `invoke()` each

### Data
- No DTOs. `ThemePreferencesDataSourceImpl`/`LanguagePreferencesDataSourceImpl` each use one `stringPreferencesKey` on the existing shared `DataStore<Preferences>`, defaulting to `SYSTEM`/`EN`.
- `ThemeRepositoryImpl`/`LanguageRepositoryImpl` map stored string ⇄ enum via `enumValueOf`, falling back to the default on a bad/missing value.

### Presentation
- **State:** `selectedThemeMode: ThemeMode = SYSTEM`, `selectedLanguage: AppLanguage = EN`, `showLogoutConfirmDialog: Boolean = false`
- **Events:** `BackClicked`, `ProfileSettingsClicked`, `ThemeModeSelected(mode)`, `LanguageSelected(language)`, `TermsAndConditionsClicked`, `HelpClicked`, `LogoutClicked`, `LogoutConfirmed`, `LogoutDismissed`
- **Effects:** `NavigateBack`, `NavigateToUserProfile`, `ApplyLocale(language)`, `NavigateToLogin`, `ShowSnackbarRes(messageResId)`
- **ViewModel logic:** loads persisted theme/language on `init`; theme/language selection optimistically updates state then persists via UseCase (language additionally emits `ApplyLocale`, applied in the Screen via `AppCompatDelegate.setApplicationLocales`, since that's an Android framework call); Logout flow toggles a state flag then emits `NavigateToLogin` on confirm — mirrors the existing `ForgotPasswordState.showPasswordSentDialog` boolean-in-state precedent rather than an effect-driven dialog.

## 5. Navigation Changes

- `AppSettingsRoute` (already declared in `Route.kt`) is wired to the real `AppSettingsScreen` in `NavGraph.kt`.
- "Profile Settings" row navigates to the already-declared `UserProfileRoute`.
- "Terms and Conditions" / "Help" have no destination screens in the design spec — they show a `ShowSnackbarRes(R.string.app_settings_coming_soon)` stub instead of new placeholder routes.
- Logout confirm navigates to `LoginRoute` with `popUpTo(0) { inclusive = true }` (no session/auth layer exists in this codebase yet to clear).
- **Known gap:** nothing in the current graph navigates *to* `AppSettingsRoute` yet (the real caller, `UserProfileScreen`'s Settings row, lives only on the unmerged `feature/profile` branch). Verified instead via Compose Previews and a temporary manual `startDestination` swap.

## 6. Strings — MANDATORY (Zero Hardcoded Text)

| Key (R.string.xxx) | English Value | Arabic Value |
|---|---|---|
| `app_settings_title` | "App Settings" | "إعدادات التطبيق" |
| `app_settings_subtitle` | "Change application settings here" | "غيّر إعدادات التطبيق هنا" |
| `app_settings_profile_settings` | "Profile Settings" | "إعدادات الملف الشخصي" |
| `app_settings_appearance` | "Appearance" | "المظهر" |
| `app_settings_language` | "Language" | "اللغة" |
| `app_settings_terms_and_conditions` | "Terms and Conditions" | "الشروط والأحكام" |
| `app_settings_help` | "Help" | "مساعدة" |
| `app_settings_logout` | "Logout" | "تسجيل الخروج" |
| `app_settings_theme_system` | "System" | "النظام" |
| `app_settings_theme_dark` | "Dark" | "داكن" |
| `app_settings_theme_light` | "Light" | "فاتح" |
| `app_settings_lang_en` | "En" | "En" |
| `app_settings_lang_ar` | "Ar" | "Ar" |
| `app_settings_logout_confirm_title` | "Log out?" | "تسجيل الخروج؟" |
| `app_settings_logout_confirm_message` | "Are you sure you want to log out of NutriScan?" | "هل أنت متأكد أنك تريد تسجيل الخروج من نوتري سكان؟" |
| `app_settings_coming_soon` | "Coming soon" | "قريباً" |
| `action_cancel` | "Cancel" | "إلغاء" |

Note: `app_settings_lang_en`/`app_settings_lang_ar` are kept as fixed Latin abbreviations in both locales — translating them would make the control that lets a user *reach* Arabic illegible in Arabic before they've switched.

## 7. Testing Plan

`AppSettingsViewModelTest.kt` (JUnit5 + MockK + Turbine):
- Initial state loads persisted `ThemeMode`/`AppLanguage` from mocked `GetThemeModeUseCase`/`GetLanguageUseCase`
- `ThemeModeSelected(DARK)` updates state and calls `SetThemeModeUseCase` (`coVerify`)
- `LanguageSelected(AR)` updates state, calls `SetLanguageUseCase`, and emits `ApplyLocale(AR)`
- `ProfileSettingsClicked` emits `NavigateToUserProfile`
- `TermsAndConditionsClicked` / `HelpClicked` each emit `ShowSnackbarRes(R.string.app_settings_coming_soon)`
- `BackClicked` emits `NavigateBack`
- `LogoutClicked` sets `showLogoutConfirmDialog = true`, no effect
- `LogoutDismissed` resets the flag, no effect
- `LogoutConfirmed` resets the flag and emits `NavigateToLogin`

Domain UseCase tests: each mocks its repository interface and asserts delegation (get returns repo value, set calls repo with the given value).

## 8. Edge Cases

- First launch: no DataStore value yet → defaults to `SYSTEM`/`EN`; toggle must render "System"/"En" as selected, not blank.
- `init`'s async load means state briefly shows defaults before the persisted value arrives — acceptable given local DataStore IO is fast; asserted in the VM test via `advanceUntilIdle()`.
- Re-selecting the already-active language/theme is a harmless no-op.
- Only `ThemeMode.SYSTEM` tracks `isSystemInDarkTheme()` live — explicit `DARK`/`LIGHT` must override it.
- Logout dialog dismiss/cancel must not navigate anywhere.
- RTL: header back button, toggle pill order, and row icon/chevron sides must mirror correctly in Arabic (`start`/`end`, never `left`/`right`).
- The 45dp icon container's `.copy(alpha = 0.55f)` applies to the container color only, not the row background.

## 9. Definition of Done

- [ ] All listed files created
- [ ] All ViewModel test cases passing
- [ ] All strings defined in strings.xml (Arabic + English)
- [ ] No hardcoded colors, strings, or dimensions in any Composable
- [ ] `AppSettingsRoute` renders the real screen in both light and dark mode, matching spec
- [ ] Theme/language selection persist via DataStore and take effect app-wide
