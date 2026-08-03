# Calories History Screen — UI Implementation Plan

## Overview

Build a **Calories History** screen accessible from the **User Profile** screen (as a new `ProfileMenuRow`). This screen displays a scrollable list of daily calorie summary cards, each showing the date and four stat boxes: **Total Meals**, **Water**, **Steps**, and **Exercise**. The screen follows the existing Clean Architecture + MVI pattern exactly.

This phase is **UI-only** — all data will be hardcoded/mocked directly in the ViewModel. Backend integration will be done in a separate phase.

> [!IMPORTANT]
> All colors will use `AppColors.*` tokens — no hardcoded hex values in Composables.
> All strings will use `stringResource(R.string.xxx)` — no hardcoded strings in `.kt` files.

---

## Design Reference

From the attached screenshot, each day entry card contains:
- **Date header**: Centered text (e.g. `23-7-2026`)
- **4 stat boxes** in a horizontal row:
  - 🔥 **Total Meals**: value (Kcal) 
  - 💧 **Water**: value (Cups) / target
  - 👟 **Steps**: value (step) / Kcal
  - 🏃 **Exercise**: value (min) / Kcal

**Top bar**: Back button (left) + "Calories History" title (center) + Calendar icon (right)

**Theme support**: Light mode (white/gray cards) + Dark mode (dark teal glassmorphism cards with glow borders)

---

## Proposed Changes

### Component 1 — Presentation Layer: MVI State/Event/Effect

New feature package: `presentation/.../calories_history/`

#### [NEW] CaloriesHistoryState.kt
Path: `presentation/.../calories_history/state/CaloriesHistoryState.kt`

```kotlin
@Immutable
data class CaloriesHistoryState(
    val isLoading: Boolean = false,
    val entries: ImmutableList<CaloriesHistoryDayUiModel> = persistentListOf(),
)
```

#### [NEW] CaloriesHistoryDayUiModel.kt
Path: `presentation/.../calories_history/state/CaloriesHistoryDayUiModel.kt`

UI model per day entry:
```kotlin
@Immutable
data class CaloriesHistoryDayUiModel(
    val dateLabel: String,        // e.g. "23-7-2026"
    val totalMealsKcal: Int,      // e.g. 2400
    val waterCups: Int,           // e.g. 7
    val waterTarget: Int,         // e.g. 8
    val steps: Int,               // e.g. 10000
    val stepsKcal: Int,           // e.g. 415
    val exerciseMinutes: Int,     // e.g. 46
    val exerciseKcal: Int,        // e.g. 2009
)
```

#### [NEW] CaloriesHistoryEvent.kt
Path: `presentation/.../calories_history/state/CaloriesHistoryEvent.kt`

```kotlin
sealed interface CaloriesHistoryEvent {
    data object NavigateBack : CaloriesHistoryEvent
    data object CalendarClicked : CaloriesHistoryEvent  // placeholder for future date picker
}
```

#### [NEW] CaloriesHistoryEffect.kt
Path: `presentation/.../calories_history/state/CaloriesHistoryEffect.kt`

```kotlin
sealed interface CaloriesHistoryEffect {
    data object NavigateBack : CaloriesHistoryEffect
}
```

---

### Component 2 — Presentation Layer: ViewModel

#### [NEW] CaloriesHistoryViewModel.kt
Path: `presentation/.../calories_history/viewmodel/CaloriesHistoryViewModel.kt`

- `@HiltViewModel` with `@Inject constructor()`
- Exposes `state: StateFlow<CaloriesHistoryState>` and `effect: Channel<CaloriesHistoryEffect>`
- `onEvent(event: CaloriesHistoryEvent)` handler
- `init` block populates state with **4 mocked day entries** matching the screenshot data (23-7-2026, all with identical values: 2400 Kcal, 7/8 cups, 10000 steps/415 Kcal, 46 min/2009 Kcal)

---

### Component 3 — Presentation Layer: UI Components

#### [NEW] CaloriesHistoryTopBar.kt
Path: `presentation/.../calories_history/view/components/CaloriesHistoryTopBar.kt`

- Row: Back button (reuses `AppBackButton`) + "Calories History" title (centered) + Calendar icon button (right)
- Calendar icon uses existing `R.drawable.ic_calendar_small`
- Colors from `AppColors` tokens (new tokens added below)

#### [NEW] CaloriesHistoryDayCard.kt
Path: `presentation/.../calories_history/view/components/CaloriesHistoryDayCard.kt`

- Date label centered above the card
- Card with rounded corners containing a Row of 4 stat items
- Each stat item: icon + label + primary value + secondary value
- Light mode: white/light gray card, dark mode: dark teal with subtle border glow
- Uses `AppColors` theme tokens throughout

#### [NEW] CaloriesHistoryStatItem.kt
Path: `presentation/.../calories_history/view/components/CaloriesHistoryStatItem.kt`

- Reusable composable for a single stat box (icon, label, value, secondary text)
- Extracted as shared component since it's repeated 4 times per card

---

### Component 4 — Presentation Layer: Screen

#### [NEW] CaloriesHistoryScreen.kt
Path: `presentation/.../calories_history/view/CaloriesHistoryScreen.kt`

- Follows exact same pattern as `StepHistoryScreen`:
  - `hiltViewModel()` injection
  - `collectAsStateWithLifecycle()` for state
  - `LaunchedEffect` for effects
  - `Scaffold` with `LazyColumn` body
- Content: TopBar → LazyColumn of `CaloriesHistoryDayCard` items
- Background color from theme token

---

### Component 5 — Theme: New Color Tokens

#### [MODIFY] AppColors.kt
Path: `presentation/.../common/theme/AppColors.kt`

Add new tokens to `AppColorsExtension3` (to avoid the D8 constructor parameter limit):

```kotlin
// --- Calories History ---
val CaloriesHistoryScreenBg: Color,
val CaloriesHistoryTopBarIconBg: Color,
val CaloriesHistoryTopBarIconTint: Color,
val CaloriesHistoryTitle: Color,
val CaloriesHistoryDateText: Color,
val CaloriesHistoryCardBg: Color,
val CaloriesHistoryCardBorder: Color,
val CaloriesHistoryStatLabel: Color,
val CaloriesHistoryStatValue: Color,
val CaloriesHistoryStatSecondary: Color,
val CaloriesHistoryStatIconTint: Color,
val CaloriesHistoryCalendarIconBg: Color,
val CaloriesHistoryCalendarIconTint: Color,
```

Light values: white/light gray cards, teal accents
Dark values: dark teal (#0A1A2A-ish) background, glassmorphism cards with teal borders, cyan text

Add forwarding properties on `AppColors` and populate in both `lightColors()` and `darkColors()`.

---

### Component 6 — String Resources

#### [MODIFY] strings.xml (EN)
Path: `presentation/src/main/res/values/strings.xml`

```xml
<!-- Calories History -->
<string name="calories_history_title">Calories History</string>
<string name="calories_history_total_meals">Total\nMeals</string>
<string name="calories_history_water">Water</string>
<string name="calories_history_steps">Steps</string>
<string name="calories_history_exercise">Exercise</string>
<string name="calories_history_kcal">Kcal</string>
<string name="calories_history_cups">Cups</string>
<string name="calories_history_target">target</string>
<string name="calories_history_step_unit">step</string>
<string name="calories_history_min">min</string>
```

#### [MODIFY] strings.xml (AR)
Path: `presentation/src/main/res/values-ar/strings.xml`

Arabic translations for all the above strings.

---

### Component 7 — Navigation Wiring (via User Profile)

The navigation chain: **Profile tab → ProfileMenuRow → CaloriesHistoryRoute**

#### [MODIFY] Route.kt
Path: `app/.../navigation/Route.kt`

```kotlin
@Serializable
object CaloriesHistoryRoute
```

#### [MODIFY] UserProfileEvent.kt
Add `data object CaloriesHistoryClicked : UserProfileEvent`

#### [MODIFY] UserProfileEffect.kt
Add `data object NavigateToCaloriesHistory : UserProfileEffect`

#### [MODIFY] UserProfileViewModel.kt
Handle `CaloriesHistoryClicked` event → send `NavigateToCaloriesHistory` effect.

#### [MODIFY] UserProfileScreen.kt
- Add `onNavigateToCaloriesHistory: () -> Unit` parameter
- Add new `ProfileMenuRow` for "Calories History" (with a calorie icon, placed before the Scan History row)
- Handle `NavigateToCaloriesHistory` effect → call `onNavigateToCaloriesHistory()`

#### [MODIFY] MainScreen.kt
- Add `onNavigateToCaloriesHistory: () -> Unit` parameter
- Pass it to `UserProfileScreen`

#### [MODIFY] NavGraph.kt
- Add `composable<CaloriesHistoryRoute>` destination with `CaloriesHistoryScreen`
- Pass `onNavigateToCaloriesHistory = { navController.navigate(CaloriesHistoryRoute) }` to `MainScreen`

---

## Verification Plan

### Build Check
- Run `./gradlew assembleDebug` to verify the build compiles without errors.

### Manual Verification
- Navigate from Profile tab → Calories History menu row → Calories History screen
- Verify top bar renders correctly (back button, title, calendar icon)
- Verify 4 mocked day cards render with correct data
- Verify light/dark mode theming
- Verify RTL (Arabic) layout
- Verify back navigation works

