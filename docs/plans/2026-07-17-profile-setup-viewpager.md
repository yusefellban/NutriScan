# Profile Setup ViewPager — Implementation Plan

## Goal

Wrap the post-registration profile setup flow into a single `HorizontalPager` (ViewPager) with **5 pages**:

| Page | Title | Status |
|------|-------|--------|
| 1/5 | Gender Selection | **Placeholder** |
| 2/5 | Date of Birth | **Placeholder** |
| 3/5 | Height | **Placeholder** |
| 4/5 | Weight | **Placeholder** |
| 5/5 | Health Profile (Conditions & Allergies) | **Migrated from old `auth/profile_setup/`** |

The user navigates via a circular "→" FAB at the bottom of each page. A `<` back button in the top-left navigates to the previous page (or exits on page 1). A page indicator (`1/5`, `2/5`, etc.) sits below the back button.

> [!WARNING]
> **No swipe navigation.** The user **cannot** swipe left/right between pages. Navigation is **exclusively** through the Next (→) and Back (<) buttons. This is enforced via `HorizontalPager(userScrollEnabled = false)`.

> [!IMPORTANT]
> The first 4 screens will be **placeholder composables only** — no real implementation. The 5th screen's UI and ViewModel logic will be **migrated** from the old `auth/profile_setup/` package, which will be **deleted entirely**.

---

## Proposed Changes

### 1. DELETE Old Package — `auth/profile_setup/`

The entire `presentation/auth/profile_setup/` directory will be **deleted**. Its contents will be absorbed into the new pager package:

#### [DELETE] Files to remove:
- `auth/profile_setup/state/HealthProfileSetupState.kt`
- `auth/profile_setup/state/HealthProfileSetupEvent.kt`
- `auth/profile_setup/state/HealthProfileSetupEffect.kt`
- `auth/profile_setup/viewmodel/HealthProfileSetupViewModel.kt`
- `auth/profile_setup/view/HealthProfileSetupScreen.kt`
- `auth/profile_setup/view/components/SelectableChip.kt`
- `auth/profile_setup/view/components/OtherInputChip.kt`
- `presentation/src/test/.../auth/profile_setup/HealthProfileSetupViewModelTest.kt`

> [!CAUTION]
> All logic from these files (ViewModel event handling, conditions/allergies state management, Save flow, UI composables) will be **migrated into** the new pager package — nothing is lost.

---

### 2. New Feature Package — `presentation/profile_setup/`

Per AGENTS.md MVI structure, gets its own `state/`, `view/`, `viewmodel/` folders.

#### Folder Structure

```
presentation/src/main/kotlin/iti/grad/nutriscan/presentation/profile_setup/
├── state/
│   ├── ProfileSetupPagerState.kt         ← pager state + health profile fields (merged)
│   ├── ProfileSetupPagerEvent.kt         ← pager events + health profile events (merged)
│   └── ProfileSetupPagerEffect.kt        ← pager effects + health profile effects (merged)
├── view/
│   ├── ProfileSetupPagerScreen.kt        ← HorizontalPager host
│   └── components/
│       ├── GenderSelectionPlaceholder.kt
│       ├── DateOfBirthPlaceholder.kt
│       ├── HeightSelectionPlaceholder.kt
│       ├── WeightSelectionPlaceholder.kt
│       ├── HealthProfileContent.kt       ← migrated from HealthProfileSetupScreen (page 5 UI)
│       ├── SelectableChip.kt             ← migrated from old components
│       ├── OtherInputChip.kt             ← migrated from old components
│       └── ProfileSetupPageIndicator.kt  ← "1/5" text indicator
└── viewmodel/
    └── ProfileSetupPagerViewModel.kt     ← pager nav + health profile save logic (merged)
```

---

### 3. MVI Contract (Merged)

#### [NEW] ProfileSetupPagerState.kt

Merges the old `HealthProfileSetupState` fields into the pager state:

```kotlin
@Immutable
data class ProfileSetupPagerState(
    // Pager navigation
    val currentPage: Int = 0,
    val pageCount: Int = 5,
    
    // Health Profile fields (migrated from HealthProfileSetupState)
    val chronicConditions: ImmutableList<String> = persistentListOf("Diabetes", "Hypertension", "Celiac Disease"),
    val selectedChronicConditions: ImmutableList<String> = persistentListOf(),
    val allergies: ImmutableList<String> = persistentListOf("Peanuts", "Gluten", "Dairy"),
    val selectedAllergies: ImmutableList<String> = persistentListOf(),
    val isAddingCustomCondition: Boolean = false,
    val isAddingCustomAllergy: Boolean = false,
    val customConditionInput: String = "",
    val customAllergyInput: String = "",
    val isLoading: Boolean = false
)
```

#### [NEW] ProfileSetupPagerEvent.kt

Merges pager events + old `HealthProfileSetupEvent`:

```kotlin
sealed interface ProfileSetupPagerEvent {
    // Pager navigation
    data object NextClicked : ProfileSetupPagerEvent
    data object BackClicked : ProfileSetupPagerEvent
    data class PageChanged(val page: Int) : ProfileSetupPagerEvent
    
    // Health Profile events (migrated)
    data class ToggleCondition(val condition: String) : ProfileSetupPagerEvent
    data class ToggleAllergy(val allergy: String) : ProfileSetupPagerEvent
    data object StartAddCustomCondition : ProfileSetupPagerEvent
    data class UpdateCustomConditionInput(val input: String) : ProfileSetupPagerEvent
    data object SubmitCustomCondition : ProfileSetupPagerEvent
    data object CancelAddCustomCondition : ProfileSetupPagerEvent
    data object StartAddCustomAllergy : ProfileSetupPagerEvent
    data class UpdateCustomAllergyInput(val input: String) : ProfileSetupPagerEvent
    data object SubmitCustomAllergy : ProfileSetupPagerEvent
    data object CancelAddCustomAllergy : ProfileSetupPagerEvent
    data object SaveProfile : ProfileSetupPagerEvent
}
```

#### [NEW] ProfileSetupPagerEffect.kt

```kotlin
sealed interface ProfileSetupPagerEffect {
    data class ScrollToPage(val page: Int) : ProfileSetupPagerEffect
    data object NavigateBack : ProfileSetupPagerEffect
    data object NavigateToHome : ProfileSetupPagerEffect
    data class ShowSnackbar(
        @StringRes val messageResId: Int? = null,
        val messageStr: String? = null
    ) : ProfileSetupPagerEffect
}
```

---

### 4. ViewModel (Merged)

#### [NEW] ProfileSetupPagerViewModel.kt

Single `@HiltViewModel` that handles **both** pager navigation and health profile logic:

- **Pager navigation:** `NextClicked` → `ScrollToPage(currentPage + 1)`, `BackClicked` → `ScrollToPage(currentPage - 1)` or `NavigateBack`, `PageChanged` → update state.
- **Health profile logic:** All toggle/custom condition/allergy/save handlers migrated from `HealthProfileSetupViewModel`.
- **Save flow:** Calls `completeOnboardingUseCase()` → emits `NavigateToHome`.
- Injects `CompleteOnboardingUseCase` (same dependency as old ViewModel).

---

### 5. Pager Host Screen

#### [NEW] ProfileSetupPagerScreen.kt

```
ProfileSetupPagerScreen(onNavigateBack, onNavigateToHome)
├── ProfileSetupPagerViewModel (handles everything)
├── Scaffold
│   └── Box(fillMaxSize)
│       ├── Column (top section)
│       │   ├── BackButton (< icon) → event BackClicked
│       │   └── PageIndicator ("1/5")
│       ├── HorizontalPager(pageCount = 5, userScrollEnabled = false)
│       │   ├── page 0 → GenderSelectionPlaceholder()
│       │   ├── page 1 → DateOfBirthPlaceholder()
│       │   ├── page 2 → HeightSelectionPlaceholder()
│       │   ├── page 3 → WeightSelectionPlaceholder()
│       │   └── page 4 → HealthProfileContent(state, onEvent)  ← migrated UI
│       └── NextButton (circular → FAB, bottom-center) — hidden on page 4 (Save button handles it)
```

**Key decisions:**
- `userScrollEnabled = false` — navigation only via buttons.
- On page 4 (Health Profile), the migrated `HealthProfileContent` composable is shown with its Save button → `SaveProfile` event → `NavigateToHome` effect.
- On pages 0–3, the circular "→" FAB triggers `NextClicked`.
- Back button on page 0 emits `NavigateBack`.

---

### 6. Placeholder Screens (Pages 0–3)

Each placeholder is a simple `@Composable` with title, subtitle, and styled placeholder body. No real logic.

#### [NEW] GenderSelectionPlaceholder.kt
- Title: "What is Your **Gender**?"
- Subtitle: "We'll use this to personalize your NutriScan experience."
- Body: Two placeholder cards (Female / Male) — styled but non-functional.

#### [NEW] DateOfBirthPlaceholder.kt
- Title: "Your **Date of birth**"
- Subtitle: "Your age helps us provide more accurate nutrition and health guidance."
- Body: Placeholder age display ("23 Years") + date field.

#### [NEW] HeightSelectionPlaceholder.kt
- Title: "How **tall** are you?"
- Subtitle: "We'll use your height to personalize your nutrition insights and calorie calculations."
- Body: Placeholder height ruler (182 | **183** | 184).

#### [NEW] WeightSelectionPlaceholder.kt
- Title: "Your current **weight**"
- Subtitle: "Your weight helps us estimate your daily calorie needs more accurately."
- Body: Placeholder weight display (59 | **60** | 61).

> All placeholders use `stringResource(R.string.xxx)` — string resources will be added for both EN and AR.

---

### 7. Migrated Components (Page 5)

#### [NEW] HealthProfileContent.kt
- Migrated from `HealthProfileSetupScreen.kt`'s `HealthProfileSetupScreenContent`.
- Receives state + `onEvent` lambda from the pager ViewModel.
- Contains the heart background, conditions/allergies FlowRows, and Save button.

#### [NEW] SelectableChip.kt + OtherInputChip.kt
- Direct copy from old `auth/profile_setup/view/components/` — only package name changes.

---

### 8. Navigation Updates

#### [MODIFY] [Route.kt](file:///c:/Users/DELL/Documents/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/navigation/Route.kt)

- Add `ProfileSetupPagerRoute`.
- **Remove** `HealthProfileSetupRoute` (no longer needed).

#### [MODIFY] [NavGraph.kt](file:///c:/Users/DELL/Documents/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt)

- **Remove** the `composable<HealthProfileSetupRoute>` entry.
- **Remove** the import for `HealthProfileSetupScreen`.
- **Add** `composable<ProfileSetupPagerRoute>` destination hosting `ProfileSetupPagerScreen`.
- **Change** Register's navigation: `HealthProfileSetupRoute` → `ProfileSetupPagerRoute`.

```diff
 composable<RegisterRoute> {
     RegisterScreen(
         onNavigateToHome = {
-            navController.navigate(HealthProfileSetupRoute) {
+            navController.navigate(ProfileSetupPagerRoute) {
                 popUpTo(RegisterRoute) { inclusive = true }
             }
         },
```

New composable:
```kotlin
composable<ProfileSetupPagerRoute> {
    ProfileSetupPagerScreen(
        onNavigateBack = { navController.navigateUp() },
        onNavigateToHome = {
            navController.navigate(HomeRoute) {
                popUpTo(ProfileSetupPagerRoute) { inclusive = true }
            }
        }
    )
}
```

---

### 9. String Resources

#### [MODIFY] `presentation/src/main/res/values/strings.xml`
#### [MODIFY] `presentation/src/main/res/values-ar/strings.xml`

Add localized strings for page titles, subtitles, and placeholder text for each of the 4 placeholder screens + page indicator format string. Existing health profile strings remain.

---

### 10. Drawable Resources

#### [NEW] `ic_arrow_forward.xml` — Circular forward arrow icon for the Next FAB button.

---

## Files Summary

| Action | File | Description |
|--------|------|-------------|
| **DELETE** | `auth/profile_setup/` (entire directory) | Old standalone health profile setup — absorbed into pager |
| **DELETE** | `auth/profile_setup/ test file` | Old ViewModel test — will be recreated for new VM |
| **NEW** | `profile_setup/state/ProfileSetupPagerState.kt` | Merged pager + health profile state |
| **NEW** | `profile_setup/state/ProfileSetupPagerEvent.kt` | Merged pager + health profile events |
| **NEW** | `profile_setup/state/ProfileSetupPagerEffect.kt` | Merged pager + health profile effects |
| **NEW** | `profile_setup/viewmodel/ProfileSetupPagerViewModel.kt` | Merged pager nav + health profile logic |
| **NEW** | `profile_setup/view/ProfileSetupPagerScreen.kt` | HorizontalPager host screen |
| **NEW** | `profile_setup/view/components/GenderSelectionPlaceholder.kt` | Page 1 placeholder |
| **NEW** | `profile_setup/view/components/DateOfBirthPlaceholder.kt` | Page 2 placeholder |
| **NEW** | `profile_setup/view/components/HeightSelectionPlaceholder.kt` | Page 3 placeholder |
| **NEW** | `profile_setup/view/components/WeightSelectionPlaceholder.kt` | Page 4 placeholder |
| **NEW** | `profile_setup/view/components/HealthProfileContent.kt` | Page 5 — migrated health profile UI |
| **NEW** | `profile_setup/view/components/SelectableChip.kt` | Migrated chip component |
| **NEW** | `profile_setup/view/components/OtherInputChip.kt` | Migrated chip component |
| **NEW** | `profile_setup/view/components/ProfileSetupPageIndicator.kt` | "1/5" page indicator |
| **MODIFY** | `navigation/Route.kt` | Add `ProfileSetupPagerRoute`, remove `HealthProfileSetupRoute` |
| **MODIFY** | `navigation/NavGraph.kt` | Wire pager route, remove old route, redirect Register → pager |
| **MODIFY** | `res/values/strings.xml` | Add EN strings |
| **MODIFY** | `res/values-ar/strings.xml` | Add AR strings |
| **NEW** | `res/drawable/ic_arrow_forward.xml` | Forward arrow icon |
| **NEW** | `profile_setup/ProfileSetupPagerViewModelTest.kt` (test) | Unit tests for merged pager + health profile ViewModel |

---

## Open Questions

> [!IMPORTANT]
> **Placeholder fidelity:** The 4 placeholder screens — should they be completely empty "TODO" screens with just the title, or should I make them look closer to the Figma layout (styled cards, mock ruler UI) even though they won't be functional?

---

## Verification Plan

### Automated Tests
```bash
.\gradlew.bat :presentation:compileDebugKotlin
.\gradlew.bat :app:compileDebugKotlin
```

### Manual Verification
- Navigate from Register → Profile Setup Pager → navigate through 5 pages → Save on page 5 → Home.
- Verify back navigation on each page.
- Verify page indicator updates correctly.
- Verify Health Profile page (page 5) retains full functionality inside the pager.
- Verify old `auth/profile_setup/` routes are fully removed and no dangling references.
