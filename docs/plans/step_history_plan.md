# Step History Screen — Full Implementation Plan

## Overview

Build a **Step History** screen that displays a circular gauge with daily step insight, a bar chart showing monthly step history, and bottom stats cards (Calories, Distance, Active Minutes). The screen follows the existing Clean Architecture + MVI patterns exactly. Data comes from `DailyTrackingRepository` (Room-persisted per-day step/calorie/exercise data) and is presented via mocked data initially, with time-period filtering (Week, Month, 3 Months, 6 Months). Data is scoped to the current authenticated user; on sign-out all local data is already cleared by the existing auth flow.

> [!IMPORTANT]
> The design references the attached screenshot. All colors will use `AppTheme.colors.*` tokens — no hardcoded hex values in Composables.

---

## Approved Plan Adjustments

1. **Navigation source**: Triggered from the Steps card/section inside the Calories Dashboard.
2. **Real vs. mock data**: Using purely in-memory mock data for the initial build inside `StepHistoryRepositoryImpl` to achieve 100% UI fidelity and test period-filtering logic. The repository interface is designed to cleanly swap with Room implementation later.
3. **User data scoping**: `AuthRepositoryImpl` logout flow will be verified, and if necessary, updated with an explicit `dao.deleteAll()` to ensure daily tracking/step history data is cleared upon sign-out.
4. **Color Tokens & RTL Support**: `AppColorsExtension3` will be used for new tokens to avoid D8 limits. Arabic RTL support will be implemented using Start/End margins.

---

## Proposed Changes

### Component 1 — Domain Layer (Pure Kotlin)

New feature package: `domain/steps/history/`

#### [NEW] [StepHistoryPeriod.kt](file:///home/yousef/Desktop/NutriScan/domain/src/main/kotlin/iti/grad/nutriscan/domain/steps/history/model/StepHistoryPeriod.kt)
- Enum: `WEEK`, `MONTH`, `THREE_MONTHS`, `SIX_MONTHS`

#### [NEW] [MonthlyStepData.kt](file:///home/yousef/Desktop/NutriScan/domain/src/main/kotlin/iti/grad/nutriscan/domain/steps/history/model/MonthlyStepData.kt)
- Data class: `monthLabel: String`, `totalSteps: Int`

#### [NEW] [StepHistorySummary.kt](file:///home/yousef/Desktop/NutriScan/domain/src/main/kotlin/iti/grad/nutriscan/domain/steps/history/model/StepHistorySummary.kt)
- Data class containing:
  - `periodAverage: Int` — average daily steps for the period
  - `stepGoal: Int` — always 10,000
  - `startDate: LocalDate`, `endDate: LocalDate`
  - `monthlyData: List<MonthlyStepData>` — per-month totals
  - `totalCaloriesBurned: Int`
  - `totalDistanceKm: Double`
  - `totalActiveMinutes: Int`

#### [NEW] [IStepHistoryRepository.kt](file:///home/yousef/Desktop/NutriScan/domain/src/main/kotlin/iti/grad/nutriscan/domain/steps/history/repository/IStepHistoryRepository.kt)
- Interface with:
  ```kotlin
  suspend fun getStepHistory(period: StepHistoryPeriod): Result<StepHistorySummary>
  ```

#### [NEW] [GetStepHistoryUseCase.kt](file:///home/yousef/Desktop/NutriScan/domain/src/main/kotlin/iti/grad/nutriscan/domain/steps/history/usecase/GetStepHistoryUseCase.kt)
- Single `invoke(period: StepHistoryPeriod): Result<StepHistorySummary>` operator function
- `@Inject constructor(private val repository: IStepHistoryRepository)`

---

### Component 2 — Data Layer (Mocked Repository)

#### [NEW] [StepHistoryRepositoryImpl.kt](file:///home/yousef/Desktop/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/repository/StepHistoryRepositoryImpl.kt)
- Implements `IStepHistoryRepository`
- Returns **mocked data** per period (different values for each of Week/Month/3M/6M)
- 6-month mock matches the screenshot exactly:
  - Jan: 34,726 · Feb: 168,902 · Mar: 210,551 · Apr: 192,243 · May: 196,882 · Jun: 176,862 · Jul: 177,633
  - Average: 6,361 · Calories: 47,277 · Distance: 816.8 km · Active Minutes: 11,578
- `@Inject constructor()`, `@IoDispatcher` dispatcher injected

---

### Component 3 — DI Wiring (app module)

#### [MODIFY] [RepositoryModule.kt](file:///home/yousef/Desktop/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/di/RepositoryModule.kt)
- Add `@Binds` binding: `IStepHistoryRepository ← StepHistoryRepositoryImpl`

---

### Component 4 — Navigation (app module)

#### [MODIFY] [Route.kt](file:///home/yousef/Desktop/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/navigation/Route.kt)
- Add `@Serializable object StepHistoryRoute`

#### [MODIFY] [NavGraph.kt](file:///home/yousef/Desktop/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt)
- Add `composable<StepHistoryRoute>` entry wiring `StepHistoryScreen(onNavigateBack = ...)`

#### [MODIFY] `CaloriesScreen.kt` (or similar)
- Trigger navigation to `StepHistoryRoute` from the Steps section click

---

### Component 5 — Theme Extension (presentation module)

#### [MODIFY] [AppColors.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/theme/AppColors.kt)
- Add **new `AppColorsExtension3`** data class with Step History–specific tokens:

| Token Name | Light Value | Dark Value | Usage |
|---|---|---|---|
| `StepHistoryCardBg` | White | Teal1500 | Card backgrounds |
| `StepHistoryScreenBg` | Gray100 / `#F8F8F9` | Teal1600 | Screen bg behind cards |
| `StepHistoryBarFill` | `#47D3D9` (Teal600) | `#47D3D9` | Bar chart fill |
| `StepHistoryBarLabel` | Gray1400 | Teal400 | Value labels above bars |
| `StepHistoryGoalLine` | `#FA4D5E` (Error) | `#FF6B7A` | 10,000 goal dotted line |
| `StepHistoryGoalText` | `#FA4D5E` | `#FF6B7A` | "10,000" label |
| `StepHistoryXAxisLabel` | Gray700 | Teal1200 | Month abbreviations |
| `StepHistoryAccent` | Teal1000 | Teal1000 | Circular gauge arc |
| `StepHistoryAccentBg` | Teal200 | Teal1400 | Circular gauge track |
| `StepHistoryTitleText` | Gray1600 | Teal100 | Section titles |
| `StepHistorySubText` | Gray700 | Gray600 | Secondary text |
| `StepHistoryChipSelectedBg` | Teal1000 | Teal1000 | Active period pill bg |
| `StepHistoryChipSelectedText` | White | White | Active period pill text |
| `StepHistoryChipUnselectedBg` | White | Teal1400 | Inactive period pill bg |
| `StepHistoryChipUnselectedBorder` | Gray400 | Teal1300 | Inactive period pill border |
| `StepHistoryChipUnselectedText` | Gray800 | Teal500 | Inactive period pill text |
| `StepHistoryStatIconBg` | Light per-icon | Dark per-icon | Bottom stat icon circles |
| `StepHistoryDateCardBg` | Teal100 | Teal1400 | Start/End date cards bg |
| `StepHistoryDateLabel` | Teal1000 | Teal500 | "Start"/"End" label |
| `StepHistoryDateValue` | Gray1600 | Teal100 | "Jan 27" / "Jul 27" |

- Add forwarding `val` properties on `AppColors`
- Wire into `lightColors()` and `darkColors()`

---

### Component 6 — String Resources (presentation module)

#### [MODIFY] [strings.xml](file:///home/yousef/Desktop/NutriScan/presentation/src/main/res/values/strings.xml) (English)
```xml
<!-- Step History Screen -->
<string name="step_history_title">Step History</string>
<string name="step_history_week">Week</string>
<string name="step_history_month">Month</string>
<string name="step_history_3_months">3 Months</string>
<string name="step_history_6_months">6 Months</string>
<string name="step_history_start">Start</string>
<string name="step_history_end">End</string>
<string name="step_history_daily_insight">Daily Insight</string>
<string name="step_history_period_average">Period Average</string>
<string name="step_history_steps">Steps</string>
<string name="step_history_of_goal">of %1$s Goal</string>
<string name="step_history_section_title">Step History</string>
<string name="step_history_calories_burned">Calories\nBurned</string>
<string name="step_history_distance_covered">Distance\nCovered</string>
<string name="step_history_active_minutes">Active\nMinutes</string>
<string name="step_history_kcal_unit">kcal</string>
<string name="step_history_km_unit">km</string>
<string name="step_history_min_unit">min</string>
```

#### [MODIFY] [strings.xml](file:///home/yousef/Desktop/NutriScan/presentation/src/main/res/values-ar/strings.xml) (Arabic)
- Full Arabic translations for all strings above (RTL support)

---

### Component 7 — Drawable Icons (presentation module)

#### [NEW] `ic_walking_person.xml` — walking figure for circular gauge
#### [NEW] `ic_flame.xml` — flame icon for calories (or reuse existing `ic_fire_outline.xml`)
#### [NEW] `ic_map_pin.xml` — location pin for distance
#### [NEW] `ic_stopwatch.xml` — stopwatch for active minutes
#### [NEW] `ic_calendar_small.xml` — calendar icon for date cards

> Existing drawables `ic_fire_outline.xml`, `ic_fire_solid.xml`, `steps.xml` may be reusable. I'll verify their visual match to the design and create new ones only where needed.

---

### Component 8 — Presentation Layer — MVI Contract

New feature package: `presentation/step_history/`

#### [NEW] [StepHistoryState.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/step_history/state/StepHistoryState.kt)
```kotlin
@Immutable
data class StepHistoryState(
    val isLoading: Boolean = true,
    val selectedPeriod: StepHistoryPeriodUi = StepHistoryPeriodUi.SIX_MONTHS,
    val periodAverage: Int = 0,
    val stepGoal: Int = 10_000,
    val startDate: String = "",      // formatted "Jan 27"
    val endDate: String = "",        // formatted "Jul 27"
    val monthlyData: ImmutableList<MonthlyStepDataUi> = persistentListOf(),
    val totalCaloriesBurned: Int = 0,
    val totalDistanceKm: Double = 0.0,
    val totalActiveMinutes: Int = 0,
    val errorMessageRes: Int? = null,
)
```

#### [NEW] [StepHistoryEvent.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/step_history/state/StepHistoryEvent.kt)
```kotlin
sealed interface StepHistoryEvent {
    data class OnPeriodSelected(val period: StepHistoryPeriodUi) : StepHistoryEvent
    data object OnBackClick : StepHistoryEvent
    data object OnStepHistoryChartClick : StepHistoryEvent
    data object OnRetryClick : StepHistoryEvent
}
```

#### [NEW] [StepHistoryEffect.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/step_history/state/StepHistoryEffect.kt)
```kotlin
sealed interface StepHistoryEffect {
    data object NavigateBack : StepHistoryEffect
    data object NavigateToDetailedHistory : StepHistoryEffect
}
```

---

### Component 9 — Presentation Layer — ViewModel

#### [NEW] [StepHistoryViewModel.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/step_history/viewmodel/StepHistoryViewModel.kt)
- `@HiltViewModel`, injects `GetStepHistoryUseCase`
- `_state: MutableStateFlow<StepHistoryState>`, `_effect: Channel<StepHistoryEffect>(BUFFERED)`
- `init { loadData(SIX_MONTHS) }`
- `onEvent(event)` handles all 4 events
- `loadData(period)` calls use case, maps domain → UI, updates state
- Maps `StepHistoryPeriodUi ↔ StepHistoryPeriod` (domain enum)

---

### Component 10 — Presentation Layer — UI Models

#### [NEW] [StepHistoryPeriodUi.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/step_history/model/StepHistoryPeriodUi.kt)
- Enum: `WEEK`, `MONTH`, `THREE_MONTHS`, `SIX_MONTHS`
- Holds `@StringRes labelRes: Int` for each period

#### [NEW] [MonthlyStepDataUi.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/step_history/model/MonthlyStepDataUi.kt)
- Data class: `label: String`, `totalSteps: Int`

---

### Component 11 — Presentation Layer — Compose UI

#### [NEW] [StepHistoryScreen.kt](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/step_history/view/StepHistoryScreen.kt)
Top-level screen composable:
- **Scaffold** with `AppTheme.colors.StepHistoryScreenBg` background
- `LaunchedEffect` collecting effects → navigation lambdas
- Vertically scrollable `Column` containing all sections below

#### [NEW] Sub-composable components (all in `view/components/`):

| File | Purpose |
|------|---------|
| `StepHistoryHeader.kt` | Back button + "Step History" title row |
| `PeriodSelector.kt` | Row of 4 pill-shaped chips (Week/Month/3M/6M) |
| `DateRangeRow.kt` | Start/End date cards with calendar icons |
| `DailyInsightSection.kt` | "Daily Insight" title + circular gauge + period average card |
| `StepCircularGauge.kt` | Custom `Canvas`-drawn arc gauge with walking person icon center |
| `StepBarChart.kt` | Custom `Canvas`-drawn bar chart with value labels, goal line, x-axis |
| `BottomStatsRow.kt` | Three equal-width stat cards (Calories, Distance, Active Minutes) |

**Key UI details matching the screenshot:**
- **Header**: `AppBackButton` (existing component) + title text
- **Period Selector**: Horizontal row of `FilterChip`-style capsules. Active pill has teal bg. Inactive has border + transparent bg.
- **Date Range**: Two cards side-by-side, each with a small calendar icon, label ("Start"/"End"), and formatted date below
- **Circular Gauge**: `Canvas` arc from 0° to `(steps/goal) * 360°`. Walking person icon drawn in the center. Step count text below with "of 10,000 Goal"
- **Period Average Card**: Bordered card showing "Period Average" label + formatted step count
- **Bar Chart**: Custom `Canvas` with rounded-top bars. Value label above each bar. Dashed red goal line at scaled 10,000 position. Month labels on x-axis
- **Bottom Stats**: Three cards each with colored circular icon background + icon + label + value

**RTL support**: All `Row` composables and `LazyRow` respect `LayoutDirection`. String resources handle Arabic. `CompositionLocalProvider(LocalLayoutDirection)` already provided by the system.

---

## File Summary (33 files)

| Layer | New Files | Modified Files |
|-------|-----------|----------------|
| **Domain** | 5 (models, repo interface, use case) | 0 |
| **Data** | 1 (mock repository impl) | 0 |
| **App (DI + Nav)** | 0 | 3 (RepositoryModule, Route, NavGraph) |
| **Presentation** | 14 (state/event/effect, VM, models, screen, 7 components) | 3 (AppColors, strings EN, strings AR) |
| **Resources** | ~3-5 new vector drawables | 0 |
| **Total** | ~25 new | ~6 modified |
