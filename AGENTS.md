# NUTRISCAN AI — Android · AI Agent Rules & Architecture Contract

> **MANDATORY:** Read this file **completely** before writing a single line of code.
> Every rule below is non-negotiable and applies to every AI agent working on this project.
> Violations will be rejected in code review without exception.
> This document is the single authoritative source of truth for all engineering decisions on this project.

---

## 0. Project Identity

| Field                | Value                                                                                           |
|----------------------|-------------------------------------------------------------------------------------------------|
| **App Name**         | NutriScan AI                                                                                    |
| **Package**          | ` iti.grad.nutriscan`                                                                              |
| **Platform**         | Android (Kotlin + Jetpack Compose)                                                              |
| **Domain**           | HealthTech — AI-powered food label scanning & personalized dietary safety for Egyptian market   |
| **Users**            | Authenticated User (individual or household manager)                                            |
| **Backend**          | Custom REST API + Gemini 2.5 Flash (LLM) + RAG pipeline (EOS / MOH documents)                  |
| **Target Market**    | Egypt (MENA region) — bilingual Arabic/English support required throughout                      |
| **Spec Reference**   | `NutriScan_AI_Project_Documentation.md` + `NutriScan_AI_Screens_Documentation.md`               |

---

## ⚠️ CRITICAL DOMAIN NOTICE — Health & Safety Data

NutriScan AI operates in the **health and safety domain**. Every scan verdict — Red, Yellow, or
Green — is evaluated against a user's medical conditions and allergies. An incorrect verdict
can have **real-world health consequences** for users with conditions like Celiac disease,
severe nut allergies, or Diabetes.

### Non-Negotiable Safety Rules for the Entire Codebase

- **Health Profile data is sacred.** Never silently default, truncate, or discard a user's
  conditions or allergies in any mapping, DTO conversion, or caching operation.
- **Verdicts must always be grounded.** The LLM response pipeline must include the user's
  complete, current profile. A stale or empty profile silently producing a "Green" verdict
  is the worst possible failure mode in this app.
- **NutriGPT answers must never contradict the Result Screen verdict.** The chat layer is
  grounded in the same RAG pipeline — it is not a general-purpose model free to improvise.
  Any response touching medical consequences must surface the disclaimer that it is not a
  substitute for professional medical advice.
- **Health Profile edits must be confirmed by the user** (confirmation dialog before saving),
  because a mis-edit directly changes every future scan verdict.
- **Receipt OCR match failures must be surfaced honestly** — an unrecognized product must
  be shown as "Unrecognized", never silently assigned a verdict or dropped from the list.

> Any agent that writes code that silently swallows, ignores, or defaults health or
> allergy data must be rejected immediately — no exceptions.

---

## 1. Confirmed Technology Stack

> ⚠️ Do NOT suggest or use any technology not listed below without explicit approval.

### 1.1 Core Language & UI

| Concern        | Technology                          | Notes                             |
|----------------|-------------------------------------|-----------------------------------|
| **Language**   | Kotlin (latest stable)              | 100% Kotlin — zero Java files     |
| **UI Toolkit** | Jetpack Compose (BOM latest stable) | Zero XML layouts anywhere         |
| **Min SDK**    | 26 (Android 8.0)                    |                                   |
| **Target SDK** | Latest stable                       |                                   |

### 1.2 Architecture & DI

| Concern                  | Technology                                       | Notes                                          |
|--------------------------|--------------------------------------------------|------------------------------------------------|
| **Architecture**         | Clean Architecture + MVI                         | Strict layer separation — see §3               |
| **Dependency Injection** | Hilt                                             | Only DI framework allowed — no Koin            |
| **ViewModel**            | `androidx.lifecycle:lifecycle-viewmodel-compose` | One ViewModel per screen                       |
| **State Management**     | `StateFlow<FeatureState>` + `Channel<FeatureEffect>` | No LiveData — see §3 for full MVI contract |

### 1.3 Async & Reactive

| Concern               | Technology                                           | Notes                                          |
|-----------------------|------------------------------------------------------|------------------------------------------------|
| **Async**             | Kotlin Coroutines                                    | `viewModelScope`, `suspend fun`                |
| **Reactive Streams**  | Kotlin Flow (cold) · StateFlow · Channel (hot)       | No RxJava                                      |
| **Dispatchers**       | Injected into **data layer only** via Hilt           | ViewModels are 100% dispatcher-agnostic — see §6 |
| **Exception Handling**| `runCatchingCancellable` + `Result<T>`               | See §6.4                                       |

### 1.4 Network

| Concern              | Technology                                  | Notes                                    |
|----------------------|---------------------------------------------|------------------------------------------|
| **HTTP Client**      | Retrofit 2 (latest) + OkHttp 4             | All API calls — suspend only             |
| **Serialization**    | `kotlinx.serialization`                     | No Gson, no Moshi                        |
| **Retrofit Converter** | `retrofit2-kotlinx-serialization-converter` |                                        |
| **Interceptors**     | OkHttp `Interceptor` (Auth, Logging, Error) | See §7                                   |

### 1.5 Local Storage

| Concern            | Technology            | Notes                                                        |
|--------------------|-----------------------|--------------------------------------------------------------|
| **Database**       | Room (latest)         | All DAOs: `suspend fun` or `Flow<>`                          |
| **Preferences**    | DataStore Preferences | Simple key-value: language, notification toggles             |
| **Typed Prefs**    | Proto DataStore       | Structured data: `UserPreferences.proto` (auth token, etc.)  |

### 1.6 Navigation

| Concern        | Technology                         | Notes                                                     |
|----------------|------------------------------------|-----------------------------------------------------------|
| **Navigation** | Jetpack Navigation Compose 2.8.0+  | **Type-safe routing only** — string routes are BANNED     |
| **Deep Links** | Declared in `NavGraph` + `AndroidManifest` |                                                   |

### 1.7 Collections in UI State

| Concern                    | Technology                      | Notes                                             |
|----------------------------|---------------------------------|---------------------------------------------------|
| **Collections in State**   | `kotlinx.collections.immutable` | `ImmutableList`, `ImmutableMap` — see §3.2        |

### 1.8 Images

| Concern           | Technology       | Notes                         |
|-------------------|------------------|-------------------------------|
| **Image Loading** | Coil 3 (Compose) | `AsyncImage` everywhere       |

### 1.9 Camera & Vision

| Concern             | Technology             | Notes                                                              |
|---------------------|------------------------|--------------------------------------------------------------------|
| **Camera**          | CameraX (latest)       | Label scan + receipt capture; lifecycle-aware                      |
| **OCR**             | ML Kit Text Recognition | On-device preprocessing pass before server-side OCR              |

### 1.10 Build Tooling

| Concern                    | Technology | Notes                                   |
|----------------------------|------------|-----------------------------------------|
| **Annotation Processing**  | KSP        | **KAPT is BANNED** — KSP is 2× faster  |

### 1.11 Testing

| Concern          | Technology                           | Notes                               |
|------------------|--------------------------------------|-------------------------------------|
| **Unit Testing** | JUnit 5 + Kotlin Coroutines Test     | `runTest`, `TestCoroutineScheduler` |
| **Mocking**      | MockK                                | No Mockito                          |
| **Flow Testing** | Turbine                              | StateFlow + Channel assertions      |
| **UI Testing**   | Compose Testing (`composeTestRule`)  |                                     |
| **Test Doubles** | Fakes preferred over Mocks for repos | See §11                             |

### ❌ Explicitly Forbidden — Zero Exceptions

| Technology                                     | Reason                                              | Use Instead                        |
|------------------------------------------------|-----------------------------------------------------|------------------------------------|
| `kapt`                                         | Slow — deprecated for Kotlin projects               | KSP                                |
| String-based nav routes                        | Type-unsafe, runtime crashes                        | Type-safe nav (§5)                 |
| `List<T>` in State                             | Compose treats it as unstable → over-recomposition  | `ImmutableList<T>`                 |
| `MutableSharedFlow` for effects                | Drops events when no subscriber                     | `Channel(BUFFERED)`                |
| `LiveData`                                     | Replaced by StateFlow                               | `StateFlow`                        |
| `RxJava` / `RxKotlin`                         | Replaced by Kotlin Flow                             | Kotlin Flow                        |
| `Koin`                                         | Only Hilt allowed                                   | Hilt                               |
| XML layouts                                    | Compose-only project                                | Jetpack Compose                    |
| `Gson` / `Moshi`                              | Replaced by kotlinx.serialization                   | `kotlinx.serialization`            |
| `GlobalScope`                                  | No lifecycle awareness — leaks                      | `viewModelScope`                   |
| `CoroutineDispatcher` injected into ViewModel  | VM must be threading-agnostic                       | Inject into repos only             |
| `mutableStateOf` for VM-level state            | Not observable outside Compose                      | `StateFlow` in VM                  |
| `UiIntent` naming                              | Replaced by `Event` in the new MVI contract         | `sealed interface FeatureEvent`    |
| Hardcoded `Color(0xFF...)` in any Composable   | Breaks theming, fails accessibility review          | `AppColors.*` only                 |
| Hardcoded strings in `.kt` files               | Blocks localization (AR/EN)                         | `stringResource(R.string.xxx)`     |
| `android.util.Log` / `println`                 | No log hygiene                                      | `Timber.d()` / `Timber.e()`       |
| Any global `object` with mutable state         | Thread-unsafe singleton                             | Inject via Hilt                    |
| Silently defaulting health profile to empty    | Safety-critical failure — see Domain Notice         | Always validate before use         |
| Fully qualified inline class names (e.g. `androidx...`) | Clutters code and reduces readability      | Use normal imports at the top      |

---

## 2. Module Structure — STRICTLY ENFORCED

Four Gradle modules. Each has its own `build.gradle.kts`.

```
root/
├── app/           ← Android: @HiltAndroidApp, MainActivity, NavHost, Hilt wiring modules
├── data/          ← Android: RepoImpl, Room entities/DAOs, Retrofit services, DTOs
├── domain/        ← Pure Kotlin: UseCases, domain Models, Repository interfaces
└── presentation/  ← Android: Compose screens, ViewModels, State/Event/Effect, Components
```

### 2.1 Module Dependency Graph

```
       ┌─────────────────────────────────────┐
       │               app                   │  ← only module seeing both data & presentation
       └───────────┬─────────────┬───────────┘
                   ▼             ▼
           presentation        data
                   │             │
                   └──────┬──────┘
                          ▼
                        domain           ← knows nothing; pure Kotlin
```

**Hard Rules:**
- `domain` → imports nothing outside stdlib
- `data` → imports `domain` only
- `presentation` → imports `domain` only
- `app` → imports `presentation` + `data` (for Hilt `@Module` binding only)

### 2.2 Complete Folder Structure

```
app/src/main/kotlin/ iti.grad.nutriscan/
├── di/
│   ├── CameraModule.kt
│   ├── DatabaseModule.kt
│   ├── DataSourceModule.kt
│   ├── DispatcherModule.kt
│   ├── NetworkModule.kt
│   ├── RepositoryModule.kt
│   └── UseCaseModule.kt
├── navigation/
│   ├── NavGraph.kt                      ← single NavHost for entire app
│   └── Route.kt                         ← @Serializable graph roots + route objects
├── App.kt
├── MainActivity.kt                      ← @AndroidEntryPoint, single Activity
└── NutriScanApplication.kt             ← @HiltAndroidApp

data/src/main/kotlin/ iti.grad.nutriscan.data/
├── db/
│   ├── NutriScanDatabase.kt
│   ├── entity/
│   │   ├── ScanHistoryEntity.kt
│   │   ├── HealthProfileEntity.kt
│   │   ├── FamilyMemberEntity.kt
│   │   ├── ShoppingListItemEntity.kt
│   │   └── WeeklyReportEntity.kt
│   └── dao/
│       ├── ScanHistoryDao.kt
│       ├── HealthProfileDao.kt
│       ├── FamilyMemberDao.kt
│       ├── ShoppingListDao.kt
│       └── WeeklyReportDao.kt
├── di/
│   └── DispatcherQualifiers.kt
├── local/
│   └── datasource/
│       ├── IOnboardingPreferencesDataSource.kt
│       ├── OnboardingPreferencesDataSourceImpl.kt
│       ├── IScanHistoryLocalDataSource.kt
│       └── ScanHistoryLocalDataSourceImpl.kt
├── remote/
│   ├── api/
│   │   ├── ScanApiService.kt
│   │   ├── ProfileApiService.kt
│   │   ├── ReportApiService.kt
│   │   └── ShoppingListApiService.kt
│   ├── datasource/
│   │   ├── IScanRemoteDataSource.kt
│   │   ├── ScanRemoteDataSourceImpl.kt
│   │   ├── IProfileRemoteDataSource.kt
│   │   ├── ProfileRemoteDataSourceImpl.kt
│   │   ├── IReportRemoteDataSource.kt
│   │   └── ReportRemoteDataSourceImpl.kt
│   ├── dto/
│   │   ├── ScanResultDto.kt
│   │   ├── IngredientDto.kt
│   │   ├── SafeAlternativeDto.kt
│   │   ├── HealthProfileDto.kt
│   │   ├── FamilyMemberDto.kt
│   │   ├── ReceiptAnalysisDto.kt
│   │   ├── NutriGptMessageDto.kt
│   │   └── WeeklyReportDto.kt
│   └── interceptor/
│       ├── AuthInterceptor.kt
│       ├── ErrorInterceptor.kt
│       └── LoggingInterceptor.kt        ← DEBUG builds only
└── repository/
    ├── ScanRepositoryImpl.kt
    ├── HealthProfileRepositoryImpl.kt
    ├── NutriGptRepositoryImpl.kt
    ├── ReceiptRepositoryImpl.kt
    ├── ShoppingListRepositoryImpl.kt
    └── ReportRepositoryImpl.kt

domain/src/main/kotlin/ iti.grad.nutriscan.domain/
├── auth/
│   ├── model/
│   │   └── AuthUser.kt
│   ├── repository/
│   │   └── IAuthRepository.kt
│   └── usecase/
│       ├── LoginWithEmailUseCase.kt
│       ├── LoginWithGoogleUseCase.kt
│       ├── RegisterUseCase.kt
│       └── GetCurrentUserUseCase.kt
├── common/
│   ├── DataResult.kt                    ← sealed class DataResult<T>
│   ├── DomainError.kt                   ← sealed class DomainError
│   ├── RunCatchingCancellable.kt
│   └── ValidationError.kt
├── profile/
│   ├── model/
│   │   ├── HealthProfile.kt
│   │   ├── FamilyMember.kt
│   │   ├── MedicalCondition.kt          ← enum: DIABETES, HYPERTENSION, CELIAC, etc.
│   │   └── AllergyItem.kt
│   ├── repository/
│   │   └── IHealthProfileRepository.kt
│   └── usecase/
│       ├── GetActiveProfilesUseCase.kt
│       ├── SaveHealthProfileUseCase.kt
│       ├── AddFamilyMemberUseCase.kt
│       ├── EditFamilyMemberUseCase.kt
│       └── DeleteFamilyMemberUseCase.kt
├── scan/
│   ├── model/
│   │   ├── ScanResult.kt
│   │   ├── Ingredient.kt
│   │   ├── SafeAlternative.kt
│   │   ├── ScanVerdict.kt               ← enum: RED, YELLOW, GREEN
│   │   ├── ProfileVerdict.kt            ← per-family-member verdict
│   │   └── ScanHistoryEntry.kt
│   ├── repository/
│   │   └── IScanRepository.kt
│   └── usecase/
│       ├── AnalyzeLabelImageUseCase.kt
│       ├── GetScanHistoryUseCase.kt
│       └── SaveScanResultUseCase.kt
├── nutrigpt/
│   ├── model/
│   │   ├── NutriGptMessage.kt
│   │   └── NutriGptConversation.kt
│   ├── repository/
│   │   └── INutriGptRepository.kt
│   └── usecase/
│       └── SendNutriGptMessageUseCase.kt
├── receipt/
│   ├── model/
│   │   ├── ReceiptScanResult.kt
│   │   └── ReceiptLineItem.kt
│   ├── repository/
│   │   └── IReceiptRepository.kt
│   └── usecase/
│       └── AnalyzeReceiptImageUseCase.kt
├── shopping/
│   ├── model/
│   │   ├── ShoppingListItem.kt
│   │   └── SmartAlternative.kt
│   ├── repository/
│   │   └── IShoppingListRepository.kt
│   └── usecase/
│       ├── GetShoppingListUseCase.kt
│       ├── AddShoppingItemUseCase.kt
│       ├── RemoveShoppingItemUseCase.kt
│       └── CheckShoppingItemSafetyUseCase.kt
├── report/
│   ├── model/
│   │   └── WeeklyReport.kt
│   ├── repository/
│   │   └── IReportRepository.kt
│   └── usecase/
│       ├── GetLatestReportUseCase.kt
│       └── GetReportHistoryUseCase.kt
└── home/
    ├── model/
    │   └── HomeFeedCategory.kt
    ├── repository/
    │   └── IHomeFeedRepository.kt
    └── usecase/
        └── GetHomeFeedUseCase.kt

presentation/src/main/kotlin/ iti.grad.nutriscan.presentation/
├── auth/
│   ├── login/
│   │   ├── state/
│   │   │   ├── LoginState.kt
│   │   │   ├── LoginEvent.kt
│   │   │   └── LoginEffect.kt
│   │   ├── view/
│   │   │   ├── LoginScreen.kt
│   │   │   └── components/
│   │   │       └── LoginFormBody.kt
│   │   └── viewmodel/
│   │       └── LoginViewModel.kt
│   └── register/
│       ├── state/
│       │   ├── RegisterState.kt
│       │   ├── RegisterEvent.kt
│       │   └── RegisterEffect.kt
│       ├── view/
│       │   ├── RegisterScreen.kt
│       │   └── components/
│       │       └── RegisterFormBody.kt
│       └── viewmodel/
│           └── RegisterViewModel.kt
├── common/
│   ├── theme/
│   │   ├── AppTheme.kt
│   │   ├── AppColors.kt
│   │   ├── AppTypography.kt
│   │   └── AppShapes.kt
│   ├── model/
│   │   └── SocialMediaProvider.kt
│   ├── components/
│   │   ├── AppButton.kt
│   │   ├── AppTextField.kt
│   │   ├── AppLoadingOverlay.kt
│   │   ├── AppErrorWidget.kt
│   │   ├── AppSnackbar.kt
│   │   ├── ConfirmationDialog.kt
│   │   ├── EmptyStateWidget.kt
│   │   ├── VerdictBadge.kt              ← Red/Yellow/Green verdict indicator
│   │   ├── LoadingShimmer.kt
│   │   ├── FigmaInputField.kt           ← Shared auth input field
│   │   ├── AuthHeader.kt                ← Shared auth header (logo + title)
│   │   ├── AppButton.kt                 ← Shared primary button with puffed 3D glow
│   │   ├── AuthBottomPrompt.kt          ← Shared "Already have account?" / "Don't have account?" prompt
│   │   ├── AuthDivider.kt               ← "── OR ──" divider
│   │   └── SocialLoginRow.kt            ← Facebook / Google / Instagram row
│   └── Validation.kt
├── onboarding/
│   ├── splash/
│   │   ├── state/
│   │   │   ├── SplashState.kt
│   │   │   ├── SplashEvent.kt
│   │   │   └── SplashEffect.kt
│   │   ├── view/
│   │   │   └── SplashScreen.kt
│   │   └── viewmodel/
│   │       └── SplashViewModel.kt
│   ├── carousel/
│   │   ├── state/
│   │   │   ├── OnboardingCarouselState.kt
│   │   │   ├── OnboardingCarouselEvent.kt
│   │   │   └── OnboardingCarouselEffect.kt
│   │   ├── view/
│   │   │   └── OnboardingCarouselScreen.kt
│   │   └── viewmodel/
│   │       └── OnboardingCarouselViewModel.kt
│   └── profile_setup/
│       ├── state/
│       │   ├── HealthProfileSetupState.kt
│       │   ├── HealthProfileSetupEvent.kt
│       │   ├── HealthProfileSetupEffect.kt
│       │   ├── FamilyProfileSetupState.kt
│       │   ├── FamilyProfileSetupEvent.kt
│       │   └── FamilyProfileSetupEffect.kt
│       ├── view/
│       │   ├── HealthProfileSetupScreen.kt
│       │   └── FamilyProfileSetupScreen.kt
│       └── viewmodel/
│           ├── HealthProfileSetupViewModel.kt
│           └── FamilyProfileSetupViewModel.kt
├── home/
│   ├── state/
│   │   ├── HomeState.kt
│   │   ├── HomeEvent.kt
│   │   └── HomeEffect.kt
│   ├── view/
│   │   ├── HomeScreen.kt
│   │   └── components/
│   │       ├── HomeFeedCategoryRow.kt
│   │       ├── FeedProductCard.kt
│   │       └── ProfileSwitcher.kt
│   └── viewmodel/
│       └── HomeViewModel.kt
├── scan/
│   ├── camera/
│   │   ├── state/
│   │   │   ├── CameraScanState.kt
│   │   │   ├── CameraScanEvent.kt
│   │   │   └── CameraScanEffect.kt
│   │   ├── view/
│   │   │   ├── CameraScanScreen.kt
│   │   │   └── components/
│   │   │       ├── CameraPreview.kt
│   │   │       └── ScanFrameOverlay.kt
│   │   └── viewmodel/
│   │       └── CameraScanViewModel.kt
│   ├── processing/
│   │   ├── state/
│   │   │   ├── ScanProcessingState.kt
│   │   │   ├── ScanProcessingEvent.kt
│   │   │   └── ScanProcessingEffect.kt
│   │   ├── view/
│   │   │   └── ScanProcessingScreen.kt
│   │   └── viewmodel/
│   │       └── ScanProcessingViewModel.kt
│   └── result/
│       ├── state/
│       │   ├── ScanResultState.kt
│       │   ├── ScanResultEvent.kt
│       │   └── ScanResultEffect.kt
│       ├── view/
│       │   ├── ScanResultScreen.kt
│       │   └── components/
│       │       ├── VerdictHeader.kt
│       │       ├── ProfileVerdictCard.kt
│       │       ├── IngredientChipRow.kt
│       │       └── SafeAlternativeCard.kt
│       └── viewmodel/
│           └── ScanResultViewModel.kt
├── nutrigpt/
│   ├── state/
│   │   ├── NutriGptState.kt
│   │   ├── NutriGptEvent.kt
│   │   └── NutriGptEffect.kt
│   ├── view/
│   │   ├── NutriGptScreen.kt
│   │   └── components/
│   │       ├── ChatBubble.kt
│   │       └── QuickQuestionChips.kt
│   └── viewmodel/
│       └── NutriGptViewModel.kt
├── ingredient_detail/
│   ├── state/
│   │   ├── IngredientDetailState.kt
│   │   ├── IngredientDetailEvent.kt
│   │   └── IngredientDetailEffect.kt
│   ├── view/
│   │   └── IngredientDetailScreen.kt
│   └── viewmodel/
│       └── IngredientDetailViewModel.kt
├── receipt/
│   ├── capture/
│   │   ├── state/
│   │   │   ├── ReceiptCaptureState.kt
│   │   │   ├── ReceiptCaptureEvent.kt
│   │   │   └── ReceiptCaptureEffect.kt
│   │   ├── view/
│   │   │   └── ReceiptCaptureScreen.kt
│   │   └── viewmodel/
│   │       └── ReceiptCaptureViewModel.kt
│   └── result/
│       ├── state/
│       │   ├── ReceiptResultState.kt
│       │   ├── ReceiptResultEvent.kt
│       │   └── ReceiptResultEffect.kt
│       ├── view/
│       │   └── ReceiptResultScreen.kt
│       └── viewmodel/
│           └── ReceiptResultViewModel.kt
├── history/
│   ├── state/
│   │   ├── ScanHistoryState.kt
│   │   ├── ScanHistoryEvent.kt
│   │   └── ScanHistoryEffect.kt
│   ├── view/
│   │   ├── ScanHistoryScreen.kt
│   │   └── components/
│   │       └── ScanHistoryCard.kt
│   └── viewmodel/
│       └── ScanHistoryViewModel.kt
├── report/
│   ├── list/
│   │   ├── state/
│   │   │   ├── ReportListState.kt
│   │   │   ├── ReportListEvent.kt
│   │   │   └── ReportListEffect.kt
│   │   ├── view/
│   │   │   └── ReportListScreen.kt
│   │   └── viewmodel/
│   │       └── ReportListViewModel.kt
│   └── detail/
│       ├── state/
│       │   ├── ReportDetailState.kt
│       │   ├── ReportDetailEvent.kt
│       │   └── ReportDetailEffect.kt
│       ├── view/
│       │   └── ReportDetailScreen.kt
│       └── viewmodel/
│           └── ReportDetailViewModel.kt
├── shopping/
│   ├── list/
│   │   ├── state/
│   │   │   ├── ShoppingListState.kt
│   │   │   ├── ShoppingListEvent.kt
│   │   │   └── ShoppingListEffect.kt
│   │   ├── view/
│   │   │   ├── ShoppingListScreen.kt
│   │   │   └── components/
│   │   │       └── ShoppingItemRow.kt
│   │   └── viewmodel/
│   │       └── ShoppingListViewModel.kt
│   └── alternative/
│       ├── state/
│       │   ├── SmartAlternativeState.kt
│       │   ├── SmartAlternativeEvent.kt
│       │   └── SmartAlternativeEffect.kt
│       ├── view/
│       │   └── SmartAlternativeSheet.kt
│       └── viewmodel/
│           └── SmartAlternativeViewModel.kt
└── settings/
    ├── profile/
    │   ├── state/
    │   │   ├── UserProfileState.kt
    │   │   ├── UserProfileEvent.kt
    │   │   └── UserProfileEffect.kt
    │   ├── view/
    │   │   └── UserProfileScreen.kt
    │   └── viewmodel/
    │       └── UserProfileViewModel.kt
    ├── family/
    │   ├── state/
    │   │   ├── ManageFamilyState.kt
    │   │   ├── ManageFamilyEvent.kt
    │   │   └── ManageFamilyEffect.kt
    │   ├── view/
    │   │   └── ManageFamilyScreen.kt
    │   └── viewmodel/
    │       └── ManageFamilyViewModel.kt
    ├── conditions/
    │   ├── state/
    │   │   ├── EditConditionsState.kt
    │   │   ├── EditConditionsEvent.kt
    │   │   └── EditConditionsEffect.kt
    │   ├── view/
    │   │   └── EditConditionsScreen.kt
    │   └── viewmodel/
    │       └── EditConditionsViewModel.kt
    ├── notifications/
    │   ├── state/
    │   │   ├── NotificationSettingsState.kt
    │   │   ├── NotificationSettingsEvent.kt
    │   │   └── NotificationSettingsEffect.kt
    │   ├── view/
    │   │   └── NotificationSettingsScreen.kt
    │   └── viewmodel/
    │       └── NotificationSettingsViewModel.kt
    └── app/
        ├── state/
        │   ├── AppSettingsState.kt
        │   ├── AppSettingsEvent.kt
        │   └── AppSettingsEffect.kt
        ├── view/
        │   └── AppSettingsScreen.kt
        └── viewmodel/
            └── AppSettingsViewModel.kt
```

---

## 3. MVI Architecture — STRICTLY ENFORCED

### 3.1 The MVI Contract (Event → State → Effect)

```
User Action (Event)
       │
       ▼
ViewModel.onEvent(event: FeatureEvent)
       │
       ├──► private fun calls UseCase (suspend)
       │           │
       │           ▼
       │       Repository (handles IO dispatch internally)
       │           │
       │     Result<T> returned
       │           │
       ├──► _state.update { newState }           (StateFlow — for UI rendering)
       └──► _effect.send(Effect)                 (Channel  — for one-shot events)
                   │
                   ▼
       Composable collects in LaunchedEffect → navigation / snackbar / camera
```

### 3.2 Three Contract Files Per Screen — MANDATORY

Every screen folder **must** contain exactly these three contract files: `FeatureState.kt`,
`FeatureEvent.kt`, and `FeatureEffect.kt`.

The naming convention for these files follows the **new contract** (no legacy `UiIntent`):

| Old (WearZone)       | New (NutriScan AI)     |
|----------------------|------------------------|
| `FeatureUiIntent.kt` | `FeatureEvent.kt`      |
| `FeatureUiEffect.kt` | `FeatureEffect.kt`     |
| `FeatureUiState.kt`  | `FeatureState.kt`      |

#### `FeatureState.kt` — What the UI renders

```kotlin
// presentation/scan/result/ScanResultState.kt
data class ScanResultState(
    val isLoading: Boolean = true,
    val verdict: ScanVerdict? = null,
    val explanation: String? = null,
    // ✅ ImmutableList REQUIRED — standard List<T> is BANNED in State
    val ingredients: ImmutableList<IngredientUiModel> = persistentListOf(),
    val profileVerdicts: ImmutableList<ProfileVerdictUiModel> = persistentListOf(),
    val safeAlternative: SafeAlternativeUiModel? = null,
    val error: UiError? = null,
    val isSaving: Boolean = false,
)

// UiModel — what the presentation layer maps domain models to
data class IngredientUiModel(
    val name: String,
    val localName: String?,          // Arabic/local synonym if available
    val isFlagged: Boolean,
    val riskReason: String?,
)

data class ProfileVerdictUiModel(
    val memberName: String,
    val verdict: ScanVerdict,
    val reason: String,
)
```

#### `FeatureEvent.kt` — What the user does

```kotlin
// presentation/scan/result/ScanResultEvent.kt
sealed interface ScanResultEvent {
    data object SaveResultClicked : ScanResultEvent
    data object ScanAgainClicked : ScanResultEvent
    data object OpenNutriGptClicked : ScanResultEvent
    data class IngredientClicked(val ingredientName: String) : ScanResultEvent
    data object RetryClicked : ScanResultEvent
    data object BackClicked : ScanResultEvent
    data object ShareClicked : ScanResultEvent
}
```

#### `FeatureEffect.kt` — One-shot side effects (via Channel)

```kotlin
// presentation/scan/result/ScanResultEffect.kt
sealed interface ScanResultEffect {
    data object NavigateBack : ScanResultEffect
    data object NavigateToCameraScreen : ScanResultEffect
    data object NavigateToNutriGpt : ScanResultEffect
    data class NavigateToIngredientDetail(val ingredientName: String) : ScanResultEffect
    data class ShowSnackBar(val message: String) : ScanResultEffect
    data class ShowSnackBarRes(val messageResId: Int) : ScanResultEffect
}
```

### 3.3 ViewModel Structure — STRICTLY ENFORCED

```kotlin
// presentation/scan/result/ScanResultViewModel.kt
@HiltViewModel
class ScanResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getScanResultUseCase: GetScanResultUseCase,   // ← from nav arg
    private val saveScanResultUseCase: SaveScanResultUseCase,
    // ✅ NO dispatcher injected here — ViewModel is threading-agnostic
) : ViewModel() {

    // ── State ──────────────────────────────────────────────────────────────
    private val _state = MutableStateFlow(ScanResultState())
    val state: StateFlow<ScanResultState> = _state.asStateFlow()

    // ── Effects ────────────────────────────────────────────────────────────
    // Channel guarantees delivery — no event is ever dropped
    private val _effect = Channel<ScanResultEffect>(Channel.BUFFERED)
    val effect: Flow<ScanResultEffect> = _effect.receiveAsFlow()

    // ✅ Type-safe argument extraction — Navigation 2.8.0+ toRoute() extension
    private val route: ScanResultRoute = savedStateHandle.toRoute<ScanResultRoute>()
    private val imageUri: String = route.imageUri

    init { analyzeLabel() }

    // ── Event Handler — lean dispatcher; all logic in private functions ────
    fun onEvent(event: ScanResultEvent) {
        when (event) {
            is ScanResultEvent.SaveResultClicked     -> saveResult()
            is ScanResultEvent.ScanAgainClicked      -> navigateToCameraScreen()
            is ScanResultEvent.OpenNutriGptClicked   -> navigateToNutriGpt()
            is ScanResultEvent.IngredientClicked     -> navigateToIngredientDetail(event.ingredientName)
            is ScanResultEvent.RetryClicked          -> analyzeLabel()
            is ScanResultEvent.BackClicked           -> navigateBack()
            is ScanResultEvent.ShareClicked          -> shareResult()
        }
    }

    // ── Private functions — each does ONE thing ────────────────────────────
    private fun analyzeLabel() {
        viewModelScope.launch {          // ✅ No dispatcher — Main by default; repo handles IO
            _state.update { it.copy(isLoading = true, error = null) }
            getScanResultUseCase(imageUri)
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            isLoading      = false,
                            verdict        = result.verdict,
                            explanation    = result.explanation,
                            ingredients    = result.ingredients.map { i -> i.toUiModel() }.toImmutableList(),
                            profileVerdicts = result.profileVerdicts.map { p -> p.toUiModel() }.toImmutableList(),
                            safeAlternative = result.safeAlternative?.toUiModel(),
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = UiError(error.localizedMessage)) }
                }
        }
    }

    private fun saveResult() {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            saveScanResultUseCase(_state.value.toScanResult())
                .onSuccess  { _effect.send(ScanResultEffect.ShowSnackBarRes(R.string.result_saved)) }
                .onFailure  { _effect.send(ScanResultEffect.ShowSnackBarRes(R.string.error_save_failed)) }
            _state.update { it.copy(isSaving = false) }
        }
    }

    private fun navigateToCameraScreen() {
        viewModelScope.launch { _effect.send(ScanResultEffect.NavigateToCameraScreen) }
    }

    private fun navigateToNutriGpt() {
        viewModelScope.launch { _effect.send(ScanResultEffect.NavigateToNutriGpt) }
    }

    private fun navigateToIngredientDetail(ingredientName: String) {
        viewModelScope.launch { _effect.send(ScanResultEffect.NavigateToIngredientDetail(ingredientName)) }
    }

    private fun navigateBack() {
        viewModelScope.launch { _effect.send(ScanResultEffect.NavigateBack) }
    }

    private fun shareResult() {
        // build shareable text, send effect
    }
}
```

### 3.4 Composable Screen Structure — STRICTLY ENFORCED

```kotlin
// presentation/scan/result/ScanResultScreen.kt

// ✅ Stateful root — only this level touches the ViewModel
@Composable
fun ScanResultScreen(
    viewModel: ScanResultViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToCameraScreen: () -> Unit,
    onNavigateToNutriGpt: () -> Unit,
    onNavigateToIngredientDetail: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // ✅ Effects collected in LaunchedEffect(Unit) — runs once, tied to composition
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ScanResultEffect.NavigateBack                  -> onNavigateBack()
                is ScanResultEffect.NavigateToCameraScreen        -> onNavigateToCameraScreen()
                is ScanResultEffect.NavigateToNutriGpt            -> onNavigateToNutriGpt()
                is ScanResultEffect.NavigateToIngredientDetail    -> onNavigateToIngredientDetail(effect.ingredientName)
                is ScanResultEffect.ShowSnackBar                  -> snackbarHostState.showSnackbar(effect.message)
                is ScanResultEffect.ShowSnackBarRes               -> snackbarHostState.showSnackbar(context.getString(effect.messageResId))
            }
        }
    }

    // ✅ State hoisting — pass state down, events up; content is stateless
    ScanResultContent(
        state   = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
    )
}

// ✅ Stateless content composable — pure rendering, no ViewModel reference
@Composable
private fun ScanResultContent(
    state: ScanResultState,
    onEvent: (ScanResultEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        when {
            state.isLoading -> AppLoadingOverlay()
            state.error != null -> AppErrorWidget(
                message = state.error.message,
                onRetry = { onEvent(ScanResultEvent.RetryClicked) },
            )
            else -> ScanResultBody(state = state, onEvent = onEvent, modifier = Modifier.padding(padding))
        }
    }
}
```

### 3.5 Layer Violation Rules — Zero Tolerance

| Rule                                                  | Example of Violation                             |
|-------------------------------------------------------|--------------------------------------------------|
| Domain has ZERO Android imports                       | `import android.*` in domain module              |
| Domain has ZERO Room/Retrofit imports                 | `@Entity` on a domain Model                      |
| Presentation has ZERO data layer imports              | `ScanResultDto` referenced in ViewModel          |
| ViewModel never skips UseCase                         | Direct repo call from ViewModel                  |
| UseCase has exactly ONE public `invoke()` operator    | UseCase with multiple public methods             |
| DTOs never cross the repository boundary              | `ScanResultDto` passed to ViewModel              |
| Domain Models never enter DTOs                        | `ScanResult` imported inside `ScanResultDto`     |
| `List<T>` never appears in a State data class         | `val ingredients: List<IngredientUiModel>`       |
| `MutableSharedFlow` never used for UI effects         | `_effect = MutableSharedFlow<>()`                |
| `viewModelScope.launch(Dispatchers.IO)`               | Dispatcher argument in any ViewModel launch      |
| `UiIntent` naming anywhere in codebase                | `sealed interface ScanResultUiIntent`            |
| Health profile silently defaulting to empty/null      | `profile ?: HealthProfile()` with empty fields   |
| ViewModels have ZERO Android framework references     | `import android.content.Context` in ViewModel — use `@ApplicationContext` in data layer or `LocalContext.current` in Composable |

### 3.6 SOLID Checklist — Before Writing Any Class

- **S** — Does this class have exactly one reason to change?
- **O** — Is the repository an interface (open for extension, closed for modification)?
- **L** — Can `FakeScanRepository` substitute `ScanRepositoryImpl` with zero caller changes?
- **I** — Is `IScanRepository` focused only on scanning (no profile or report methods)?
- **D** — Does `ScanResultViewModel` depend on `AnalyzeLabelImageUseCase`, not `ScanRepositoryImpl`?

---

## 4. Hilt Dependency Injection — STRICTLY ENFORCED

### 4.1 Application & Activity

```kotlin
// app/NutriScanApplication.kt
@HiltAndroidApp
class NutriScanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }
}

// app/MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NutriScanTheme {
                AppNavHost()
            }
        }
    }
}
```

### 4.2 Hilt Module Patterns

```kotlin
// ✅ @Binds — for binding interface to implementation (abstract module)
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindScanRepository(impl: ScanRepositoryImpl): IScanRepository

    @Binds @Singleton
    abstract fun bindHealthProfileRepository(impl: HealthProfileRepositoryImpl): IHealthProfileRepository

    @Binds @Singleton
    abstract fun bindNutriGptRepository(impl: NutriGptRepositoryImpl): INutriGptRepository

    @Binds @Singleton
    abstract fun bindReceiptRepository(impl: ReceiptRepositoryImpl): IReceiptRepository

    @Binds @Singleton
    abstract fun bindShoppingListRepository(impl: ShoppingListRepositoryImpl): IShoppingListRepository

    @Binds @Singleton
    abstract fun bindReportRepository(impl: ReportRepositoryImpl): IReportRepository
}

// ✅ @Provides — for third-party classes or types you don't own (object module)
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        errorInterceptor: ErrorInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(errorInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        })
        .connectTimeout(30, TimeUnit.SECONDS)  // longer timeout: OCR+LLM pipeline can take 2-4s
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.NUTRISCAN_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
```

### 4.3 Dispatcher Injection — Data Layer ONLY

```kotlin
// data/di/DispatcherQualifiers.kt

// Step 1 — Declare qualifiers
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class IoDispatcher
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class DefaultDispatcher

// Step 2 — Provide them (in app/di/DispatcherModule.kt)
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

// Step 3 — Inject ONLY into Repository and DataSource implementations
class ScanRepositoryImpl @Inject constructor(
    private val localDataSource: IScanHistoryLocalDataSource,
    private val remoteDataSource: IScanRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,   // ✅ here only
) : IScanRepository {

    override suspend fun analyzeLabelImage(imageUri: String, profileIds: List<String>): Result<ScanResult> =
        withContext(ioDispatcher) {
            runCatchingCancellable {
                val dto = remoteDataSource.submitLabelImage(imageUri, profileIds)
                dto.toDomain()
            }
        }
}

// ✅ ViewModel — zero dispatcher knowledge
@HiltViewModel
class ScanResultViewModel @Inject constructor(
    private val analyzeLabelImageUseCase: AnalyzeLabelImageUseCase,
) : ViewModel() {
    private fun analyzeLabel() {
        viewModelScope.launch {   // ✅ main — repo handles IO internally
            /* ... */
        }
    }
}
```

### 4.4 Hilt Scopes

| Scope               | Use For                                                 |
|---------------------|---------------------------------------------------------|
| `@Singleton`        | Retrofit, OkHttpClient, RoomDatabase, DataStore, CameraX Process |
| `@ViewModelScoped`  | Objects whose lifetime matches a single ViewModel       |
| `@ActivityScoped`   | Objects shared across the single activity's lifetime    |

### 4.5 ViewModels

```kotlin
// ✅ Always @HiltViewModel + @Inject constructor
@HiltViewModel
class NutriGptViewModel @Inject constructor(
    private val sendNutriGptMessageUseCase: SendNutriGptMessageUseCase,
) : ViewModel()

// ✅ In Composable — always hiltViewModel()
@Composable
fun NutriGptScreen(viewModel: NutriGptViewModel = hiltViewModel())
```

### 4.6 KSP (not KAPT) — MANDATORY

```kotlin
// build.gradle.kts (module level) — ✅ KSP for all annotation processors
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    ksp("androidx.room:room-compiler:$roomVersion")
    ksp("com.google.dagger:hilt-android-compiler:$hiltVersion")
}

// ❌ BANNED — no kapt block anywhere in the project
// kapt("androidx.room:room-compiler:$roomVersion")
```

---

## 5. Navigation — Type-Safe Only

### 5.1 Route Definitions

```kotlin
// app/navigation/Route.kt
// ── Screen routes ─────────────────────────────────────────────────────────
@Serializable data object SplashRoute
@Serializable data object OnboardingCarouselRoute
@Serializable data object LoginRoute
@Serializable data object RegisterRoute
@Serializable data object HealthProfileSetupRoute
@Serializable data object FamilyProfileSetupRoute
@Serializable data object HomeRoute
@Serializable data object CameraScanRoute
@Serializable data class ScanProcessingRoute(val imageUri: String)
@Serializable data class ScanResultRoute(val imageUri: String)
@Serializable data class NutriGptRoute(val scanResultId: String)
@Serializable data class IngredientDetailRoute(val ingredientName: String)
@Serializable data object ReceiptCaptureRoute
@Serializable data class ReceiptResultRoute(val receiptImageUri: String)
@Serializable data object ScanHistoryRoute
@Serializable data object ReportListRoute
@Serializable data class ReportDetailRoute(val reportId: String)
@Serializable data object ShoppingListRoute
@Serializable data object UserProfileRoute
@Serializable data object ManageFamilyRoute
@Serializable data class EditConditionsRoute(val memberProfileId: String)
@Serializable data object NotificationSettingsRoute
@Serializable data object AppSettingsRoute
```

### 5.2 NavGraph Structure

```kotlin
// app/navigation/NavGraph.kt
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: Any = SplashRoute
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable<SplashRoute> {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(OnboardingCarouselRoute) {
                        popUpTo<SplashRoute> { inclusive = true }
                    }
                }
            )
        }

        composable<OnboardingCarouselRoute> {
            OnboardingCarouselScreen(
                onGetStarted = {
                    navController.navigate(LoginRoute) {
                        popUpTo<OnboardingCarouselRoute> { inclusive = true }
                    }
                }
            )
        }

        composable<LoginRoute> {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(HomeRoute) {
                        popUpTo<LoginRoute> { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(RegisterRoute) },
            )
        }

        composable<RegisterRoute> {
            RegisterScreen(
                onNavigateToHome = {
                    navController.navigate(HomeRoute) {
                        popUpTo<LoginRoute> { inclusive = true }
                    }
                },
                onNavigateToSignIn = { navController.navigateUp() }
            )
        }

        composable<HealthProfileSetupRoute> {
            HealthProfileSetupScreen(
                onProfileSaved = { navController.navigate(FamilyProfileSetupRoute) }
            )
        }

        composable<FamilyProfileSetupRoute> {
            FamilyProfileSetupScreen(
                onComplete = {
                    navController.navigate(HomeRoute) {
                        popUpTo<LoginRoute> { inclusive = true }
                    }
                }
            )
        }

        composable<HomeRoute> {
            HomeScreen(
                onNavigateToScan = { navController.navigate(CameraScanRoute) },
                onNavigateToReceipt = { navController.navigate(ReceiptCaptureRoute) },
                onNavigateToHistory = { navController.navigate(ScanHistoryRoute) },
                onNavigateToProfile = { navController.navigate(UserProfileRoute) }
            )
        }

        composable<CameraScanRoute> {
            CameraScanScreen(
                onNavigateBack = { navController.popBackStack() },
                onImageCaptured = { uri -> navController.navigate(ScanProcessingRoute(uri)) }
            )
        }

        composable<ScanProcessingRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ScanProcessingRoute>()
            ScanProcessingScreen(
                imageUri = route.imageUri,
                onAnalysisComplete = { uri ->
                    navController.navigate(ScanResultRoute(uri)) {
                        popUpTo<ScanProcessingRoute> { inclusive = true }
                    }
                },
                onAnalysisFailed = { navController.popBackStack() }
            )
        }

        composable<ScanResultRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ScanResultRoute>()
            ScanResultScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCameraScreen = {
                    navController.navigate(CameraScanRoute) {
                        popUpTo<ScanResultRoute> { inclusive = true }
                    }
                },
                onNavigateToNutriGpt = { scanResultId ->
                    navController.navigate(NutriGptRoute(scanResultId))
                },
                onNavigateToIngredientDetail = { name ->
                    navController.navigate(IngredientDetailRoute(name))
                }
            )
        }

        composable<NutriGptRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<NutriGptRoute>()
            NutriGptScreen(
                scanResultId = route.scanResultId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<IngredientDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<IngredientDetailRoute>()
            IngredientDetailScreen(
                ingredientName = route.ingredientName,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<ReceiptCaptureRoute> {
            ReceiptCaptureScreen(
                onNavigateBack = { navController.popBackStack() },
                onReceiptCaptured = { uri -> navController.navigate(ReceiptResultRoute(uri)) }
            )
        }

        composable<ReceiptResultRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ReceiptResultRoute>()
            ReceiptResultScreen(
                receiptImageUri = route.receiptImageUri,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToIngredientDetail = { name ->
                    navController.navigate(IngredientDetailRoute(name))
                }
            )
        }

        // Remaining routes (history, shopping, settings, etc.) follow the same flat pattern.
    }
}
```

### 5.3 Navigation Rules

| Rule                                              | Detail                                                                              |
|---------------------------------------------------|-------------------------------------------------------------------------------------|
| String routes                                     | BANNED — use `@Serializable` objects / data classes only                            |
| Navigation files                                  | Exactly two: `Route.kt` and `NavGraph.kt` — no others                              |
| Argument passing                                  | IDs and primitive strings only via typed route — never pass domain objects          |
| NavController in ViewModel                        | NEVER — emit `Effect` → collect in screen → call navController                     |
| Flat Single Graph                                 | Define all routes in a single flat AppNavGraph. Nested graphs are forbidden.        |
| Back-stack clearing on auth success               | `popUpTo<LoginRoute> { inclusive = true }` when entering `HomeRoute`                |
| Scan processing back-stack                        | `popUpTo<ScanProcessingRoute> { inclusive = true }` when navigating to Result       |
| Image URIs in routes                              | Pass as `String` in the route data class — never pass `Uri` directly               |

---

## 6. Kotlin Coroutines & Flows — Rules

### 6.1 Dispatcher Responsibility Model

```
┌─────────────────┐    viewModelScope.launch { }   ┌─────────────────┐
│   ViewModel     │ ──────────── (Main) ──────────► │   UseCase       │
│  (no dispatcher)│                                 │  (no dispatcher)│
└─────────────────┘                                 └────────┬────────┘
                                                             │  suspend fun call
                                                    ┌────────▼────────┐
                                                    │   Repository    │
                                                    │  withContext(   │  ← @IoDispatcher
                                                    │  ioDispatcher)  │     injected here
                                                    └────────┬────────┘
                                                             │
                                              ┌──────────────▼──────────────┐
                                              │  Room DAO / Retrofit Service │
                                              │  (suspend — handles own IO)  │
                                              └──────────────────────────────┘
```

**Rule:** Dispatchers are injected exclusively at the `RepositoryImpl` and `DataSourceImpl` level.
ViewModels and UseCases are completely dispatcher-agnostic. Tests substitute
`UnconfinedTestDispatcher` via a `TestCoroutineRule` at the test boundary — no
production code changes needed.

### 6.2 Hot vs Cold Flows

| Type              | Use Case                                                | Behaviour                             |
|-------------------|---------------------------------------------------------|---------------------------------------|
| `Flow<T>` (cold)  | Repository exposing DB data or a single API call        | Starts on collection                  |
| `StateFlow<T>`    | State in ViewModel                                      | Always has value; replays last        |
| `Channel<T>`      | One-shot Effects (navigation, snackbar, camera trigger) | Guaranteed delivery via `receiveAsFlow()` |

```kotlin
// ✅ CORRECT — Room returns cold Flow; ViewModel converts to hot StateFlow
@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun observeHistory(): Flow<List<ScanHistoryEntity>>    // cold
}

// In ViewModel — stateIn converts cold Flow to hot StateFlow
val historyState: StateFlow<ScanHistoryState> = getScanHistoryUseCase()
    .map { result ->
        result.fold(
            onSuccess = { entries -> ScanHistoryState.Success(entries.map { it.toUiModel() }.toImmutableList()) },
            onFailure = { ScanHistoryState.Error(it.localizedMessage ?: "") },
        )
    }
    .stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScanHistoryState.Loading,
    )

// ✅ CORRECT — Channel for effects (never SharedFlow for one-shot events)
private val _effect = Channel<ScanHistoryEffect>(Channel.BUFFERED)
val effect: Flow<ScanHistoryEffect> = _effect.receiveAsFlow()

// ❌ BANNED — SharedFlow for one-shot effects (events can be dropped)
private val _effect = MutableSharedFlow<ScanHistoryEffect>(replay = 0)
```

### 6.3 Preferred Flow Operators

```kotlin
.map { }                   // transform
.filter { }                // filter
.catch { e -> }            // error handling in flow chain
.onStart { }               // emit loading state before first item
.combine(other) { a, b ->} // merge two streams
.flatMapLatest { }         // cancel previous — use for NutriGPT streaming or search
.debounce(300)             // throttle search / shopping list input — always 300ms
.distinctUntilChanged()    // skip duplicate emissions
.stateIn(...)              // cold Flow → hot StateFlow for ViewModel exposure
```

### 6.4 Exception Handling

> ⚠️ **Never use bare `runCatching { }` in suspend functions.**
> `runCatching` catches `Throwable`, including `CancellationException`, which breaks
> structured concurrency. A cancelled coroutine becomes a silent `Result.failure`.

**Always use `runCatchingCancellable` in coroutine contexts:**

```kotlin
// domain/common/RunCatchingCancellable.kt  ← add this utility once, use everywhere
suspend fun <T> runCatchingCancellable(block: suspend () -> T): Result<T> =
    runCatching { block() }.also { result ->
        result.exceptionOrNull()?.let { e ->
            if (e is CancellationException) throw e   // re-propagate cancellation
        }
    }
```

```kotlin
// ✅ In suspend repository functions
override suspend fun analyzeLabelImage(imageUri: String, profileIds: List<String>): Result<ScanResult> =
    withContext(ioDispatcher) {
        runCatchingCancellable {
            remoteDataSource.submitLabelImage(imageUri, profileIds).toDomain()
        }
    }

// ✅ In Flow chains — use .catch (never .runCatching inside flow)
fun observeScanHistory(): Flow<List<ScanHistoryEntry>> = scanHistoryDao
    .observeHistory()
    .map { it.map { entity -> entity.toDomain() } }
    .catch { e ->
        Timber.e(e, "Scan history observation error")
        emit(emptyList())
    }

// ✅ In ViewModel — handle Result from UseCase
viewModelScope.launch {
    analyzeLabelImageUseCase(imageUri, profileIds)
        .onSuccess { result -> _state.update { it.copy(isLoading = false, verdict = result.verdict) } }
        .onFailure { error  -> _state.update { it.copy(isLoading = false, error = UiError(error.localizedMessage)) } }
}

// ❌ BANNED — bare runCatching in suspend functions
runCatching { remoteDataSource.submitLabelImage(imageUri, profileIds) }

// ❌ BANNED — silent catch
try { ... } catch (e: Exception) { /* empty */ }
```

---

## 7. Network Layer

### 7.1 API Authentication

```kotlin
// data/remote/interceptor/AuthInterceptor.kt
class AuthInterceptor @Inject constructor(
    private val authTokenProvider: IAuthTokenProvider,   // reads from Proto DataStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { authTokenProvider.getToken() }
        val request = chain.request().newBuilder().apply {
            if (token != null) header("Authorization", "Bearer $token")
        }.build()
        return chain.proceed(request)
    }
}
```

Store secrets in `local.properties` — **never commit this file to git**:

```properties
NUTRISCAN_BASE_URL=https://api.nutriscan.ai/v1/
```

Expose via `BuildConfig` in `build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        buildConfigField("String", "NUTRISCAN_BASE_URL",
            "\"${localProperties["NUTRISCAN_BASE_URL"]}\"")
    }
}
```

### 7.2 API Service Rules

```kotlin
// ✅ Suspend only — no Call<T>, no blocking
interface ScanApiService {
    @Multipart
    @POST("scan/label")
    suspend fun submitLabelImage(
        @Part image: MultipartBody.Part,
        @Part("profile_ids") profileIds: RequestBody,
    ): ScanResultDto

    @Multipart
    @POST("scan/receipt")
    suspend fun submitReceiptImage(
        @Part image: MultipartBody.Part,
        @Part("profile_ids") profileIds: RequestBody,
    ): ReceiptAnalysisDto
}

interface ProfileApiService {
    @GET("profiles")
    suspend fun getHealthProfiles(): List<HealthProfileDto>

    @POST("profiles")
    suspend fun saveHealthProfile(@Body dto: HealthProfileDto): HealthProfileDto
}
```

### 7.3 Interceptor Stack (execution order)

```
1. AuthInterceptor    → adds Bearer token to every request
2. ErrorInterceptor   → maps HTTP codes to typed domain exceptions
3. LoggingInterceptor → DEBUG builds only (Level.BODY)
```

```kotlin
// data/remote/interceptor/ErrorInterceptor.kt
sealed class NutriScanHttpException(message: String) : IOException(message)
class UnauthorizedException : NutriScanHttpException("401 — token invalid or expired")
class ForbiddenException    : NutriScanHttpException("403 — insufficient permissions")
class NotFoundException     : NutriScanHttpException("404 — resource not found")
class OcrLowConfidenceException : NutriScanHttpException("422 — OCR confidence too low; retake required")
class ServerException(code: Int) : NutriScanHttpException("5xx Server Error: $code")

class ErrorInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        when (response.code) {
            401         -> throw UnauthorizedException()
            403         -> throw ForbiddenException()
            404         -> throw NotFoundException()
            422         -> throw OcrLowConfidenceException()
            in 500..599 -> throw ServerException(response.code)
        }
        return response
    }
}
```

> ⚠️ **OCR low-confidence (422) is a special domain error.** The Result Screen must surface
> a distinct "We couldn't read this label clearly — please retake the photo" state, not a
> generic error. Map `OcrLowConfidenceException` to `DomainError.OcrLowConfidence` in the
> repository, and handle it distinctly in the ViewModel's state.

---

## 8. Local Storage

### 8.1 Room Database

```kotlin
// data/db/NutriScanDatabase.kt
@Database(
    entities = [
        ScanHistoryEntity::class,
        HealthProfileEntity::class,
        FamilyMemberEntity::class,
        ShoppingListItemEntity::class,
        WeeklyReportEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class NutriScanDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun healthProfileDao(): HealthProfileDao
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun weeklyReportDao(): WeeklyReportDao
}
```

All DAOs expose **either `suspend fun` or `Flow<>`** — never blocking calls.

> **First real worked example**: the entity list above is the long-term target
> shape; `FoodLogEntity`/`FoodLogDao` (`data/db/entity/FoodLogEntity.kt`,
> `data/db/dao/FoodLogDao.kt`) is the first entity actually implemented in the
> codebase — use it as the concrete reference for a new entity/DAO pair
> (scoped by `userId` + a `loggedDate` string column for "today" queries,
> `@Insert(onConflict = REPLACE)`, a `Flow`-returning `observe*` query).
> `NutriScanDatabase` currently only declares `FoodLogEntity`; add the others
> to the `entities` list as they're implemented, not upfront.

### 8.2 DataStore Preferences

> ✅ Use for: selected language, notification toggle preferences.
> ❌ **NEVER store `auth_token`, health profile data, or session state here.**

```kotlin
class AppPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val selectedLanguage: Flow<String> = dataStore.data.map { it[LANGUAGE_KEY] ?: "ar" }

    suspend fun setLanguage(code: String) {
        dataStore.edit { it[LANGUAGE_KEY] = code }
    }

    companion object {
        val LANGUAGE_KEY = stringPreferencesKey("language_code")
    }
}
```

### 8.3 Proto DataStore — Auth Token & Structured User State

```proto
// data/src/main/proto/user_preferences.proto
syntax = "proto3";
option java_package = " iti.grad.nutriscan.data.local.proto";

message UserPreferences {
    string auth_token      = 1;
    string user_id         = 2;
    bool   has_profile     = 3;    // used by SplashViewModel to decide start destination
    string locale          = 4;
}
```

Use Proto DataStore for: **auth token, user identity state, onboarding completion flag** —
data that must survive process death with type safety.

---

## 9. Offline-First / SSOT Architecture

Features that read persistent data (Scan History, Shopping List, Reports) follow SSOT
via the Repository:

```kotlin
// ✅ CORRECT — channelFlow: local emits immediately; remote refresh is concurrent
class ScanHistoryRepositoryImpl @Inject constructor(
    private val localDs: IScanHistoryLocalDataSource,
    private val remoteDs: IScanRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : IScanHistoryRepository {

    override fun observeScanHistory(): Flow<List<ScanHistoryEntry>> = channelFlow {
        launch {   // inherits ioDispatcher context from flowOn below
            runCatchingCancellable { remoteDs.fetchScanHistory() }
                .onSuccess { dtos -> localDs.cacheScanHistory(dtos.map { it.toEntity() }) }
        }
        // Reactively collect from Room and forward each emission down the channel
        localDs.observeScanHistory()
            .map { list -> list.map { it.toDomain() } }
            .collect { send(it) }
    }.flowOn(ioDispatcher)
}

// ❌ WRONG — onStart blocks local emission until network call finishes
override fun observeScanHistory(): Flow<List<ScanHistoryEntry>> =
    localDs.observeScanHistory()
        .map { it.map { entity -> entity.toDomain() } }
        .onStart { refreshFromRemote() }   // ← blocks; emit is delayed
```

**Rules:**
- Room is the single source of truth — the UI always collects from Room's `Flow`
- Remote fetches update Room; Room's `Flow` re-emits automatically
- Network failure while offline: cached data is served silently
- **Scan results always written to Room immediately** — the scan is never lost due to a crash

---

## 10. Compose State Management & Recomposition

### 10.1 Immutable Collections in State — MANDATORY

```kotlin
// ✅ CORRECT — ImmutableList prevents unnecessary recomposition
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

data class ScanResultState(
    val ingredients: ImmutableList<IngredientUiModel> = persistentListOf(),   // ✅
)

// ❌ BANNED in State
data class ScanResultState(
    val ingredients: List<IngredientUiModel> = emptyList()   // ← Compose sees as unstable
)
```

### 10.2 State Hoisting

```kotlin
// ✅ CORRECT — stateless leaf; state owned by parent
@Composable
fun VerdictBadge(
    verdict: ScanVerdict,
    modifier: Modifier = Modifier,
) {
    // pure rendering only — no local mutableStateOf
}

// ❌ WRONG — local uncontrolled state inside a leaf that should be controlled
@Composable
fun VerdictBadge(...) {
    var animate by remember { mutableStateOf(false) }  // not for this component to own
}
```

### 10.3 Intelligent Recomposition

```kotlin
// ✅ Keys in LazyColumn prevent full recomposition on list change
LazyColumn {
    items(scanHistory, key = { it.id }) { entry ->
        ScanHistoryCard(entry = entry, onEvent = onEvent)
    }
}

// ✅ derivedStateOf for computed values
val hasRiskyItems by remember(shoppingItems) {
    derivedStateOf { shoppingItems.any { it.verdict == ScanVerdict.RED } }
}
```

### 10.4 Side Effects Rules

```kotlin
// LaunchedEffect(Unit)   — collect Effects from Channel (runs once)
LaunchedEffect(Unit) {
    viewModel.effect.collect { effect -> /* navigate, show snackbar */ }
}

// LaunchedEffect(key)    — re-run when key changes
LaunchedEffect(scanResultId) { viewModel.loadResult(scanResultId) }

// SideEffect             — post-recomposition non-Compose side effects
SideEffect { analyticsTracker.setCurrentScreen("ScanResult") }

// DisposableEffect       — CameraX lifecycle binding
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event -> cameraController.bindLifecycle(lifecycleOwner) }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
}
```

---

## 11. Testing Architecture

### 11.1 Test Strategy Per Layer

| Layer        | What to Test                                                  | Tools                        | Target      |
|--------------|---------------------------------------------------------------|------------------------------|-------------|
| Domain       | UseCase logic, model transformations, health profile mapping  | JUnit5 + MockK               | 100%        |
| Data         | Repository SSOT logic, DTO→domain mappers, DAO operations     | JUnit5 + MockK + Turbine     | 90%         |
| Presentation | **ViewModel state emissions, event handling, effect output**  | JUnit5 + MockK + Turbine     | 95%         |
| UI           | Composable rendering with given State                         | Compose Test                 | Key screens |

### 11.2 MANDATORY ViewModel Test Requirement

> ❌ **A feature is NOT considered complete without its ViewModel unit test file.**
> Every ViewModel MUST have a corresponding `*ViewModelTest.kt` file that covers:
> - Initial state emissions
> - Each `Event` handler producing the correct `State` transition
> - Each `Event` handler producing the correct `Effect` via Channel
> - Error/failure paths producing the correct error state

This requirement is enforced at PR review. A PR that adds or modifies a ViewModel without
an accompanying test update will be **rejected without review**.

### 11.3 Test Dispatcher Rule

```kotlin
// Shared test utility — add once to test source sets
@OptIn(ExperimentalCoroutinesApi::class)
class TestCoroutineRule : TestWatcher() {
    val testDispatcher = UnconfinedTestDispatcher()

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

### 11.4 Test Doubles Strategy

```
Fakes  ← preferred for IScanRepository, IHealthProfileRepository (deterministic, no magic)
Mocks  ← acceptable for UseCases in ViewModel tests (MockK)
Stubs  ← simple scenarios with fixed return values
```

```kotlin
// ✅ CORRECT — Fake repository (preferred over mock)
class FakeScanRepository : IScanRepository {
    var resultToReturn: ScanResult? = null
    var errorToThrow: Throwable? = null

    override suspend fun analyzeLabelImage(
        imageUri: String,
        profileIds: List<String>,
    ): Result<ScanResult> =
        errorToThrow?.let { Result.failure(it) }
            ?: Result.success(checkNotNull(resultToReturn) { "Set resultToReturn before calling" })
}
```

### 11.5 ViewModel Test Template (Turbine + MockK)

```kotlin
// presentation/src/test/scan/result/ScanResultViewModelTest.kt
@OptIn(ExperimentalCoroutinesApi::class)
class ScanResultViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val fakeRepo = FakeScanRepository()
    private val analyzeLabelImageUseCase = AnalyzeLabelImageUseCase(fakeRepo)
    private val saveScanResultUseCase    = mockk<SaveScanResultUseCase>(relaxed = true)

    private fun buildViewModel(): ScanResultViewModel {
        // Replicate how Navigation Compose serializes type-safe routes into a SavedStateHandle.
        // A raw mapOf("imageUri" to ...) will cause toRoute<ScanResultRoute>() to throw at
        // test runtime — the type-safe route must be set under the navigation bundle key.
        val route = ScanResultRoute(imageUri = "content://test/image.jpg")
        val savedStateHandle = SavedStateHandle().apply {
            set("androidx.navigation.NavStartDestinationArguments", route)
        }
        return ScanResultViewModel(
            savedStateHandle         = savedStateHandle,
            analyzeLabelImageUseCase = analyzeLabelImageUseCase,
            saveScanResultUseCase    = saveScanResultUseCase,
        )
    }

    @Test
    fun `when analysis succeeds, state transitions to loaded with correct verdict`() = runTest {
        fakeRepo.resultToReturn = TestFixtures.greenScanResult
        val viewModel = buildViewModel()

        viewModel.state.test {
            // Loading emitted first
            val loading = awaitItem()
            assertTrue(loading.isLoading)

            // Loaded state emitted on success
            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals(ScanVerdict.GREEN, loaded.verdict)
            assertNull(loaded.error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when analysis fails with OcrLowConfidence, state reflects OCR error`() = runTest {
        fakeRepo.errorToThrow = OcrLowConfidenceException()
        val viewModel = buildViewModel()

        viewModel.state.test {
            awaitItem() // loading
            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertNotNull(errorState.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `IngredientClicked event emits NavigateToIngredientDetail effect`() = runTest {
        fakeRepo.resultToReturn = TestFixtures.redScanResult
        val viewModel = buildViewModel()

        // Drain initial state
        viewModel.state.test {
            awaitItem(); awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.effect.test {
            viewModel.onEvent(ScanResultEvent.IngredientClicked("Maltodextrin"))
            val effect = awaitItem()
            assertIs<ScanResultEffect.NavigateToIngredientDetail>(effect)
            assertEquals("Maltodextrin", effect.ingredientName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `BackClicked event emits NavigateBack effect`() = runTest {
        fakeRepo.resultToReturn = TestFixtures.greenScanResult
        val viewModel = buildViewModel()

        viewModel.effect.test {
            viewModel.onEvent(ScanResultEvent.BackClicked)
            assertIs<ScanResultEffect.NavigateBack>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### 11.6 Test File Locations

```
data/src/test/
├── repository/
│   ├── ScanRepositoryImplTest.kt
│   └── HealthProfileRepositoryImplTest.kt
├── datasource/
└── mapper/
    ├── ScanResultMapperTest.kt
    └── HealthProfileMapperTest.kt

domain/src/test/
├── scan/usecase/
│   ├── AnalyzeLabelImageUseCaseTest.kt
│   └── GetScanHistoryUseCaseTest.kt
├── profile/usecase/
│   └── GetActiveProfilesUseCaseTest.kt
└── nutrigpt/usecase/
    └── SendNutriGptMessageUseCaseTest.kt

presentation/src/test/
├── scan/
│   ├── result/
│   │   └── ScanResultViewModelTest.kt
│   └── history/
│       └── ScanHistoryViewModelTest.kt
├── home/
│   └── HomeViewModelTest.kt
├── nutrigpt/
│   └── NutriGptViewModelTest.kt
├── receipt/
│   └── ReceiptResultViewModelTest.kt
└── auth/
    └── LoginViewModelTest.kt
```

### 11.7 Test Fixtures Object

```kotlin
// presentation/src/test/util/TestFixtures.kt
object TestFixtures {
    val greenScanResult = ScanResult(
        id             = "result_001",
        verdict        = ScanVerdict.GREEN,
        explanation    = "This product is safe for all your active profiles.",
        ingredients    = listOf(Ingredient("Sugar", null, false, null)),
        profileVerdicts = emptyList(),
        safeAlternative = null,
    )

    val redScanResult = ScanResult(
        id             = "result_002",
        verdict        = ScanVerdict.RED,
        explanation    = "Contains Maltodextrin — high glycemic, avoid for Diabetes.",
        ingredients    = listOf(Ingredient("Maltodextrin", "مالتوديكسترين", true, "High glycemic index")),
        profileVerdicts = emptyList(),
        safeAlternative = SafeAlternative("BrandX Biscuits", "Carrefour — Nasr City", "img_url"),
    )
}
```

---

## 12. MANDATORY Context-Aware Planning

### 12.1 The Planning Rule — Non-Negotiable

> **Before writing, modifying, or executing any code, the AI agent MUST produce a
> step-by-step implementation plan and save it to `docs/plans/` in the repository.**
> Writing code without a saved plan first is a protocol violation that will be rejected.

### 12.2 Plan File Location & Naming

```
docs/plans/YYYY-MM-DD-feature-name.md

Examples:
  docs/plans/2025-09-01-scan-result-screen.md
  docs/plans/2025-09-03-nutrigpt-chat.md
  docs/plans/2025-09-07-receipt-ocr-integration.md
```

### 12.3 Required Plan Sections

Every plan file **must** contain all of the following sections:

```markdown
# Plan: [Feature Name]

## 1. Feature Summary
Brief description of what the feature does and which documentation section it maps to.

## 2. Files to Create
List every new file path that will be created, with a one-line purpose for each.

## 3. Files to Modify
List every existing file to be changed, with the specific change described.

## 4. Layer Breakdown
### Domain
- Models to add/modify
- UseCases to create (one per `invoke()`)
- Repository interface changes

### Data
- DTOs to add
- Mapper functions (DTO → Domain, Entity → Domain)
- DAO changes
- Remote DataSource changes

### Presentation
- State properties (list all fields and types)
- Events (list every event the user can trigger)
- Effects (list every one-shot side effect)
- ViewModel logic outline

## 5. Navigation Changes
New routes to add to Route.kt and NavGraph.kt connections.

## 6. Strings — MANDATORY (Zero Hardcoded Text)
List every user-facing string this feature introduces, with its key and value in BOTH
Arabic and English. No text may be hardcoded in Kotlin files — every string must be
planned here before it appears in code.

| Key (R.string.xxx)               | English Value                              | Arabic Value                         |
|----------------------------------|--------------------------------------------|--------------------------------------|
| `scan_result_verdict_green`      | "This product is safe for you"             | "هذا المنتج آمن بالنسبة لك"          |
| `scan_result_verdict_red`        | "This product is unsafe — avoid it"        | "هذا المنتج غير آمن — تجنبه"         |
| `scan_result_alternative_label`  | "Safe alternative"                         | "بديل آمن"                           |
| `scan_result_save_success`       | "Result saved to history"                  | "تم حفظ النتيجة في السجل"            |
| `error_ocr_low_confidence`       | "Couldn't read the label — please retake"  | "تعذّر قراءة الملصق — حاول مجدداً"   |

## 7. Testing Plan
List the test cases that will be written in the ViewModel test file,
covering: initial state, each Event → State transition, each Event → Effect emission,
and error paths.

## 8. Edge Cases
List the edge cases that must be explicitly handled (e.g., OCR low confidence,
empty profile list, network offline, null safe alternative).

## 9. Definition of Done
Checklist of what must be true before this plan is considered complete:
- [ ] All listed files created
- [ ] All ViewModel test cases passing
- [ ] All strings defined in strings.xml (Arabic + English)
- [ ] No hardcoded colors, strings, or dimensions in any Composable
- [ ] README.md updated to mark feature as ✅
```

---

## 13. Feature-Specific Business Rules

### 13.1 Authentication

- Firebase Auth is the identity provider (Email/Password + Google Sign-In)
- Auth token stored in Proto DataStore — never `SharedPreferences`
- `SplashViewModel` reads `hasProfile` from Proto DataStore to decide start destination:
  - No token → Onboarding/Auth flow
  - Token + no profile → `HealthProfileSetupRoute`
  - Token + profile exists → `HomeRoute`
- On login success: navigate to Home, pop entire auth graph from back stack

> **Reality check**: the actual implementation is Keycloak/OIDC (email+password
> and Google, via AppAuth), and tokens are stored in `TokenManager`
> (`EncryptedSharedPreferences`, not Proto DataStore) — reconcile this section
> when Proto DataStore is actually adopted, don't assume it's there yet.
>
> `IAuthRepository.getCurrentUserId(): String?` decodes the `sub` claim out of
> the stored OIDC ID token (`TokenManager.getIdToken()` → `JwtDecoder`,
> `data/local/util/JwtDecoder.kt`) — this is how any locally-persisted,
> per-user data (e.g. `FoodLogEntity.userId`) should be scoped. Returns `null`
> if logged out or the token is undecodable; callers must handle that, not
> assume a user id always exists.

### 13.2 Health Profile Setup — Safety-Critical Rules

- The onboarding health profile form is the **most safety-critical screen** in the app.
- Every condition and allergy selection must be persisted atomically — no partial saves.
- Health profile data must never be truncated in DTO mapping. A mapper that drops allergy
  entries (e.g., because of a nullable field) must be treated as a severity-1 bug.
- **Every edit to an existing profile requires a `ShowConfirmDialog` effect first**, since
  an accidental edit directly changes every future scan verdict.
- The `EditConditionsRoute` receives a `memberProfileId` string, never a domain model.

### 13.3 Core Label Scan

- The scan pipeline is `CameraCapture → ScanProcessingRoute → ScanResultRoute`.
  The `ScanProcessingRoute` is responsible for submitting the image and polling/waiting
  for the backend response. It must not be skipped.
- OCR low-confidence (422) produces a `DomainError.OcrLowConfidence` — the Result Screen
  must distinguish this from a generic network error and prompt the user to retake the photo.
- The `ScanResultState` always contains `safeAlternative` as nullable. If the verdict is
  `GREEN`, `safeAlternative` is `null` and the UI must not render the alternative card.
- If Family Profiles are active, `profileVerdicts` is a non-empty `ImmutableList`. The
  UI must render per-member verdict cards rather than a single blanket verdict.
- Scan results are saved to Room **immediately on success** — before the user taps "Save".
  The "Save" action is an explicit bookmark/pin action, distinct from the auto-save.

### 13.4 NutriGPT Chat

- `NutriGptViewModel` holds the full conversation history in `State` as an `ImmutableList<NutriGptMessageUiModel>`.
- Each `SendMessage` event appends a user bubble optimistically, then emits a loading
  bubble, then replaces the loading bubble with the model response on success.
- Answers are grounded in the same RAG pipeline as the scan — the ViewModel must pass
  the `scanResultId` in every message to the repository so the API can maintain context.
- A disclaimer string (`R.string.nutrigpt_medical_disclaimer`) must be shown as a pinned
  banner at the top of the chat — it cannot be removed or hidden by the user.

### 13.5 Smart Receipt Scanning

- Receipt OCR is different from label OCR: text is flat and well-lit, but product names
  are abbreviated. The result DTO includes a `matchConfidence` per line item (0.0–1.0).
- Any `ReceiptLineItem` with `matchConfidence < 0.7` must render as "Unrecognized" in the
  UI — it must not receive a fabricated verdict.
- `ReceiptResultState` shows an `overallHealthScore` (0–100) alongside the per-item list.

### 13.6 Home Feed

- The Home Feed is always-on and category-organized. `HomeState` holds
  `feedCategories: ImmutableList<HomeFeedCategoryUiModel>`.
- Each category loads independently with its own loading shimmer — the entire feed must
  not show a full-screen blocker while categories load in parallel.
- For cold-start users (no scan history), the feed is populated purely from the rule-based
  layer and the health profile. An empty scan history must never cause an empty feed.
- The `ProfileSwitcher` component must always be visible when family profiles are active,
  showing whose feed is currently displayed.

### 13.7 Shopping List

- Every item added to the shopping list triggers a background safety check
  (`CheckShoppingItemSafetyUseCase`) immediately — not on next app open.
- Items flagged `RED` or `YELLOW` display a visual risk indicator in the list row.
- The `SmartAlternativeSheet` is a bottom sheet (not a separate route) triggered by tapping
  a flagged item. It emits `SmartAlternativeEffect.AddToDeliveryCart` if a delivery service
  is connected.
- Removing an item from the shopping list requires a `ShowConfirmDialog` effect first.

### 13.8 Weekly Health Reports

- Reports are generated by a scheduled background job — the Report screens are read-only
  views of stored `WeeklyReportEntity` data.
- `ReportDetailScreen` includes a "Share" action that emits `ReportDetailEffect.ExportAsPdf`
  — the export logic lives in the ViewModel and produces a file URI for sharing.
- Sending a report to a physician emits `ReportDetailEffect.ShareWithDoctor(recipientEmail)`.

### 13.9 Destructive Action Contract — HARD REQUIREMENT

Every destructive action **must** show a confirmation dialog first.
Execution happens only after the user explicitly confirms.

Destructive actions list:
- Delete a shopping list item
- Clear the entire shopping list
- Delete a family member profile
- Remove a food-log entry (Calories dashboard, swipe-to-remove)
- Edit health conditions / allergies (safety-critical — see §13.2)
- Log out
- Delete account

> **Established real-code convention** (not the Effect-based sketch this
> section used to show): a nullable/boolean field on `State` gates the
> shared `ConfirmationDialog` composable (`presentation/common/components/ConfirmationDialog.kt`)
> directly in the screen — no dedicated `ShowConfirmDialog` effect class.
> See `AppSettingsScreen`/`AppSettingsState.showLogoutConfirmDialog` (logout),
> `UserProfileScreen` (remove family member), and
> `CaloriesState.pendingRemoveFoodId` (remove food-log entry) for three real
> examples of the same shape. Use this pattern for new destructive actions.

```kotlin
// ✅ CORRECT — swipe/tap sets a pending-id field; a separate Confirmed event executes
data class CaloriesState(
    // ...
    val pendingRemoveFoodId: String? = null,
)

is CaloriesEvent.FoodItemSwipedToRemove -> _state.update { it.copy(pendingRemoveFoodId = event.entryId) }
CaloriesEvent.RemoveFoodConfirmed -> confirmRemoveFood() // calls the use case, then clears pendingRemoveFoodId
CaloriesEvent.RemoveFoodDismissed -> _state.update { it.copy(pendingRemoveFoodId = null) }
```

```kotlin
// Screen: gate ConfirmationDialog on the pending field, same as AppSettingsScreen's logout dialog
if (state.pendingRemoveFoodId != null) {
    ConfirmationDialog(
        title = stringResource(R.string.food_log_remove_confirm_title),
        message = stringResource(R.string.food_log_remove_confirm_message),
        confirmLabel = stringResource(R.string.action_remove),
        cancelLabel = stringResource(R.string.action_cancel),
        onConfirm = { onEvent(CaloriesEvent.RemoveFoodConfirmed) },
        onDismiss = { onEvent(CaloriesEvent.RemoveFoodDismissed) },
    )
}
```

---

## 14. UI & Design System Rules

### 14.1 Tokens — AppColors (STRICTLY ENFORCED)

```kotlin
// presentation/common/theme/AppColors.kt
object AppColors {
    val Primary          = Color(0xFF1B5E20)   // deep health green
    val PrimaryVariant   = Color(0xFF2E7D32)
    val Accent           = Color(0xFF00C853)   // scan success / green verdict
    val Surface          = Color(0xFFFFFFFF)
    val SurfaceVariant   = Color(0xFFF1F8E9)
    val Background       = Color(0xFFFAFAFA)
    val OnPrimary        = Color(0xFFFFFFFF)
    val TextPrimary      = Color(0xFF1C1C1E)
    val TextSecondary    = Color(0xFF757575)
    val VerdictGreen     = Color(0xFF388E3C)
    val VerdictYellow    = Color(0xFFF9A825)
    val VerdictRed       = Color(0xFFD32F2F)
    val Warning          = Color(0xFFFF6F00)
    val Error            = Color(0xFFB71C1C)
    val Divider          = Color(0xFFE0E0E0)
}
// ❌ NEVER use Color(0xFF...) inline inside any Composable
// ❌ NEVER use MaterialTheme.colorScheme colors not mapped to AppColors tokens
```

### 14.2 Tokens — AppTypography

```kotlin
val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = NotoSansFamily, fontWeight = FontWeight.Bold,     fontSize = 28.sp),
    titleMedium   = TextStyle(fontFamily = NotoSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyMedium    = TextStyle(fontFamily = NotoSansFamily, fontWeight = FontWeight.Normal,   fontSize = 14.sp),
    bodySmall     = TextStyle(fontFamily = NotoSansFamily, fontWeight = FontWeight.Normal,   fontSize = 12.sp),
    labelSmall    = TextStyle(fontFamily = NotoSansFamily, fontWeight = FontWeight.Medium,   fontSize = 11.sp),
)
// ✅ NotoSans chosen for Arabic/Latin bilingual rendering quality
// ❌ NEVER use a hardcoded fontSize in a Composable — use MaterialTheme.typography.*
```

### 14.3 Shared Component Catalogue — Use These, Never Reinvent

```
AppButton(text, onClick, modifier, isLoading, enabled, variant: Primary|Secondary|Destructive)
AppTextField(value, onValueChange, label, isError, errorMessage, modifier)
AppLoadingOverlay()
AppErrorWidget(message, onRetry)
AppSnackbar — via SnackbarHostState in Scaffold
ConfirmationDialog(titleResId, messageResId, confirmLabel, onConfirm, onDismiss)
EmptyStateWidget(titleResId, subtitleResId, illustrationRes)
VerdictBadge(verdict: ScanVerdict, modifier)    ← Red/Yellow/Green chip
IngredientChip(name, isFlagged, onClick)
SafeAlternativeCard(alternative: SafeAlternativeUiModel, onClick)
ProfileVerdictCard(profileVerdict: ProfileVerdictUiModel)
LoadingShimmer(modifier, shape)
ScanHistoryCard(entry, onEvent)
```

> ❌ **FORBIDDEN:** Creating a new component that duplicates the function of any item above.
> ❌ **FORBIDDEN:** Using default unthemed `Button(...)` or `TextField(...)` directly —
>     always use the App-prefixed shared components.

### 14.4 Localization — MANDATORY

Every string displayed to the user **must** be defined in `res/values/strings.xml`
(English) **and** `res/values-ar/strings.xml` (Arabic), and referenced via
`stringResource(R.string.xxx)`.

```kotlin
// ✅ CORRECT
Text(text = stringResource(R.string.scan_result_verdict_green))
AppButton(text = stringResource(R.string.scan_again))
ConfirmationDialog(
    titleResId   = R.string.dialog_delete_member_title,
    messageResId = R.string.dialog_delete_member_message,
)

// ❌ BANNED — hardcoded strings anywhere in Composables
Text(text = "This product is safe for you")
AppButton(text = "Scan again")
Text(text = "Contains Maltodextrin")
```

**Rules:**
- Zero hardcoded user-facing strings in any `.kt` file — no exceptions
- The planning phase (§12.3 §6) must list all new strings before code is written
- Error messages, labels, button text, dialog messages, empty state text, disclaimers,
  snackbar messages, and ingredient descriptions — all must use `stringResource`
- String keys follow `snake_case`: `R.string.scan_result_verdict_red`
- Plurals use `pluralStringResource` — never manual `if/else` string building
- Arabic strings must be reviewed for right-to-left correctness and local phrasing —
  not machine-translated directly from English

### 14.5 Verdict Colors — Special Accessibility Rule

The Red/Yellow/Green verdict must **never be conveyed by color alone** — always pair
the color with an icon and/or text label, since color-blind users must receive the same
information.

```kotlin
// ✅ CORRECT — color + icon + text
@Composable
fun VerdictBadge(verdict: ScanVerdict, modifier: Modifier = Modifier) {
    val (color, icon, textRes) = when (verdict) {
        ScanVerdict.GREEN  -> Triple(AppColors.VerdictGreen,  Icons.Default.CheckCircle, R.string.verdict_green_label)
        ScanVerdict.YELLOW -> Triple(AppColors.VerdictYellow, Icons.Default.Warning,     R.string.verdict_yellow_label)
        ScanVerdict.RED    -> Triple(AppColors.VerdictRed,    Icons.Default.Cancel,      R.string.verdict_red_label)
    }
    Row(modifier = modifier) {
        Icon(imageVector = icon, contentDescription = null, tint = color)
        Text(text = stringResource(textRes), color = color, style = MaterialTheme.typography.labelSmall)
    }
}

// ❌ WRONG — color only; inaccessible to color-blind users
Box(modifier = Modifier.background(AppColors.VerdictRed))
```

### 14.6 Accessibility Checklist

- Every `Image` and `AsyncImage` must have a non-empty `contentDescription`
- Every tappable element: minimum touch target `48.dp × 48.dp`
- Apply `semantics { role = Role.Button }` to custom interactive elements
- Color contrast ratio ≥ 4.5:1 for all text on background
- RTL layout support must be verified for Arabic locale — use `start`/`end` padding, not `left`/`right`

---

## 15. Code Quality Rules

### 15.1 Prohibited Patterns

| Pattern                               | Use Instead                                      |
|---------------------------------------|--------------------------------------------------|
| `TODO()` in production code           | Resolve before commit                            |
| `println()` / `android.util.Log.*`   | `Timber.d()` / `Timber.e()`                     |
| Empty `catch` blocks                  | At minimum log with Timber                       |
| `!!` without a comment                | `?: return`, `let`, or `requireNotNull(message)` |
| Functions > 40 lines                  | Extract private functions                        |
| Files > 300 lines                     | Extract components or helpers                    |
| Hardcoded strings in Composables      | `stringResource(R.string.xxx)`                   |
| Hardcoded `Color(0xFF...)` inline     | `AppColors.*` only                               |
| Hardcoded dimensions                  | `dimensionResource` or theme tokens              |
| `UiIntent` naming                     | `sealed interface FeatureEvent`                  |
| `List<T>` in State data class         | `ImmutableList<T>` from kotlinx.collections      |
| `MutableSharedFlow` for effects       | `Channel(Channel.BUFFERED)`                      |
| Dispatcher in ViewModel               | Inject at Repository level only                  |
| Silently defaulting health profile    | Validate and surface error explicitly            |
| Default unthemed Compose components   | Use shared `App*` component catalogue (§14.3)    |

---

## 16. Scope & Safety Rules

- ❌ Do NOT modify files outside the scope of the requested task
- ❌ Do NOT rename existing files unless explicitly asked
- ❌ Do NOT refactor working code while implementing a new feature
- ❌ Do NOT add new Gradle dependencies without explicit approval
- ❌ Do NOT change module boundaries without a discussion
- ❌ Do NOT write code before saving an implementation plan to `docs/plans/`
- ❌ Do NOT add a new screen without listing its strings in the plan (§12.3 §6)
- ✅ Work on ONE feature or layer at a time
- ✅ Show the list of files to create/change BEFORE writing code
- ✅ Map every feature implementation to its section in the NutriScan docs
- ✅ Every user-facing string must be planned in `docs/plans/` before it appears in code
- ✅ Every user-facing string must use `stringResource(R.string.xxx)` — no exceptions
- ✅ Every ViewModel must have a test file before the feature is marked done
- ✅ Every UseCase must reach 100% test coverage before a PR is raised

---

## 17. Documentation Rules

### 17.1 After Completing Any Task

1. Update `README.md` — mark feature status as ✅
2. Write a summary to `docs/ai/YYYY-MM-DD-task-name.md`
3. New architecture decision made → `docs/adr/ADR-XXX.md`
4. Confirm with the team before closing the task

### 17.2 Quick Reference

| I need to know...                        | Location                                            |
|------------------------------------------|-----------------------------------------------------|
| Full feature requirements                | `NutriScan_AI_Project_Documentation.md`             |
| Screen-by-screen flows                   | `NutriScan_AI_Screens_Documentation.md`             |
| Module dependency rules                  | §2.1                                                |
| Complete folder structure                | §2.2                                                |
| MVI contract (State / Event / Effect)    | §3                                                  |
| ViewModel structure & Channel effect     | §3.3                                                |
| Hilt modules & scopes                    | §4                                                  |
| KSP configuration                        | §4.6                                                |
| Type-safe navigation setup               | §5                                                  |
| Dispatcher model (why VM has none)       | §6.1                                                |
| Flow hot/cold decision                   | §6.2                                                |
| Auth interceptor & secrets               | §7.1                                                |
| OCR low-confidence error contract        | §7.3                                                |
| Offline-first SSOT pattern               | §9                                                  |
| ImmutableList rule                       | §10.1                                               |
| State hoisting rules                     | §10.2                                               |
| Side effects cheat sheet                 | §10.4                                               |
| ViewModel test requirement               | §11.2                                               |
| Testing strategy + Turbine template      | §11                                                 |
| Mandatory planning rule                  | §12                                                 |
| Health profile safety rules              | §13.2                                               |
| NutriGPT disclaimer contract            | §13.4                                               |
| Destructive action contract              | §13.9                                               |
| Verdict accessibility rule               | §14.5                                               |
| Shared component catalogue              | §14.3                                               |
| Localization rules                       | §14.4                                               |
| Git commit conventions                   | §17.3                                               |

### 17.3 Git Commit Conventions

- **No AI co-author trailer.** Commits made with AI agent assistance must
  **not** include a `Co-Authored-By:` trailer for the agent (e.g. no
  `Co-Authored-By: Claude <noreply@anthropic.com>`). Author the commit as
  the human developer only.

---

*Last updated: July 2026 · NutriScan AI — Android Track · v1.0*
