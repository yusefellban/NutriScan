# Onboarding Screens Implementation Plan

This plan details the implementation of the 3-page onboarding carousel for NutriScan AI. It supports bilingual (English/Arabic) content, Dark Theme matching the system/app branding, and elegant animations.

## 1. Requirements

- **Theme & Colors:**
  - Light and Dark theme support.
  - Background, typography, and accent colors will use the `AppTheme.colors` and `AppTheme.typography` dynamically.
  - Action buttons will use the `AuthActionButton` layout or similar custom design with a bottom glow.
- **Animations:**
  - Image and the small description text below it will appear with animations (e.g. fade-in and slide-up/down or scaling).
- **Localization:**
  - English and Arabic strings for titles and descriptions.
- **State Management & Navigation:**
  - Full Clean Architecture + MVI.
  - Onboarding carousel states managed by `OnboardingCarouselViewModel`.
  - Onboarding completed flag stored in DataStore Preferences.
  - On splash completed, check onboarding state. Navigate to `OnboardingRoute` if not completed, otherwise skip straight to `LoginRoute`.

## 2. Dependencies & Project Setup
- Add `androidx-datastore-preferences` to version catalog `libs.versions.toml` and `:data` module.

## 3. Implementation Steps

### Step 3.1: Data Layer Setup
- Create `IOnboardingPreferencesDataSource` interface and `OnboardingPreferencesDataSourceImpl` using DataStore.
- Bind them via Hilt in `:data` and `:app`.

### Step 3.2: Domain Layer Setup
- Create `IOnboardingRepository` in `:domain`.
- Implement `OnboardingRepositoryImpl` in `:data`.
- Create `IsOnboardingCompletedUseCase` and `CompleteOnboardingUseCase` in `:domain`.

### Step 3.3: MVI Presentation Contracts & ViewModel
- Define state (`OnboardingCarouselState`), events (`OnboardingCarouselEvent`), and side effects (`OnboardingCarouselEffect`).
- Create `OnboardingCarouselViewModel` to handle navigation and completing onboarding.

### Step 3.4: Compose Screens & Components
- **`OnboardingCarouselScreen.kt`**: Uses `HorizontalPager` to render pages, shows Page Indicator, Action Button, and BACK/SKIP header.
- **Animations**:
  - Image and description text will slide up/fade in when pages are selected.
- **Dark Theme**:
  - Automatically adjusts background colors using `MaterialTheme.colorScheme.background` and text colors using `MaterialTheme.colorScheme.onBackground` or `AppTheme.colors.TextSecondary`.

### Step 3.5: NavGraph Integration
- Register `OnboardingRoute` in `Route.kt`.
- Add `composable<OnboardingRoute>` to `AppNavGraph`.
- Update `SplashViewModel` and `SplashScreen` to read onboarding completion and navigate appropriately.
