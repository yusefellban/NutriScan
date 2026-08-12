# Calories History — Backend Integration Plan

## Overview

Wire the **Calories History** screen (currently UI-only with mocked data) to the real backend endpoint `GET /api/v1/daily-tracking`. The API returns a **paginated** response with daily tracking entries including all stat fields the UI already displays (total meals kcal, water, steps, exercise).

Most of the plumbing already exists — the API service, repository interface, and repository implementation all have a `getHistoryPage` method. The main work is:
1. **Update the DTO** to include the new fields from the API response.
2. **Update or create a domain model** that carries all the fields the UI needs.
3. **Create a UseCase** for fetching the history.
4. **Update the ViewModel** to call the UseCase instead of mocked data, with pagination support.

---

## API Response Reference

```
GET /api/v1/daily-tracking?page=0&size=10
```

Each item in `content`:
```json
{
  "id": 1,
  "date": "2026-08-03",
  "targetWaterCnt": 8,
  "waterCnt": 6,
  "stepsCnt": 8500,
  "stepsKcal": 352.0,
  "exerciseKcal": 1540.0,
  "exerciseMin": 30.0,
  "totalMealKcal": 2100,
  "mealCount": 3
}
```

Pagination wrapper fields: `totalElements`, `totalPages`, `number`, `first`, `last`, `empty`.

---

## Proposed Changes

### Component 1 — Data Layer: Update DTO

#### [MODIFY] [DailyTrackingDto.kt](file:///c:/Users/DELL/Documents/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/remote/dto/DailyTrackingDto.kt)

Update `DailyTrackingSummaryResponseDto` to include all new fields from the API:

```kotlin
@Serializable
data class DailyTrackingSummaryResponseDto(
    val id: Int? = null,
    val date: String? = null,
    val targetWaterCnt: Int? = null,
    val waterCnt: Int? = null,
    val stepsCnt: Int? = null,
    val stepsKcal: Double? = null,        // NEW
    val exerciseKcal: Double? = null,     // NEW
    val exerciseMin: Double? = null,      // NEW
    val totalMealKcal: Long? = null,      // NEW
    val mealCount: Int? = null,
)
```

Update `PageDailyTrackingSummaryResponseDto` to add pagination metadata:

```kotlin
@Serializable
data class PageDailyTrackingSummaryResponseDto(
    val content: List<DailyTrackingSummaryResponseDto> = emptyList(),
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val number: Int = 0,
    val last: Boolean = true,             // NEW — needed for pagination
)
```

---

### Component 2 — Domain Layer: Update Model

#### [MODIFY] [DailyTrackingSummary.kt](file:///c:/Users/DELL/Documents/NutriScan/domain/src/main/kotlin/iti/grad/nutriscan/domain/dailytracking/model/DailyTrackingSummary.kt)

Add the missing fields so the domain model carries everything the UI needs:

```kotlin
data class DailyTrackingSummary(
    val date: LocalDate,
    val targetWaterCnt: Int,
    val waterCnt: Int,
    val stepsCnt: Int,
    val stepsKcal: Int,          // NEW
    val exerciseKcal: Int,       // NEW
    val exerciseMinutes: Int,    // NEW
    val totalMealKcal: Int,      // NEW
    val mealCount: Int,
)
```

---

### Component 3 — Data Layer: Update Mapper

#### [MODIFY] [DailyTrackingMapper.kt](file:///c:/Users/DELL/Documents/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/repository/mapper/DailyTrackingMapper.kt)

Update `DailyTrackingSummaryResponseDto.toDomain()` to map the new fields:

```kotlin
fun DailyTrackingSummaryResponseDto.toDomain(): DailyTrackingSummary = DailyTrackingSummary(
    date = date?.let { LocalDate.parse(it) } ?: CairoDateProvider.today(),
    targetWaterCnt = targetWaterCnt ?: 0,
    waterCnt = waterCnt ?: 0,
    stepsCnt = stepsCnt ?: 0,
    stepsKcal = stepsKcal?.toInt() ?: 0,
    exerciseKcal = exerciseKcal?.toInt() ?: 0,
    exerciseMinutes = exerciseMin?.toInt() ?: 0,
    totalMealKcal = totalMealKcal?.toInt() ?: 0,
    mealCount = mealCount ?: 0,
)
```

---

### Component 4 — Domain Layer: Update Repository Interface

#### [MODIFY] [IDailyTrackingRepository.kt](file:///c:/Users/DELL/Documents/NutriScan/domain/src/main/kotlin/iti/grad/nutriscan/domain/dailytracking/repository/IDailyTrackingRepository.kt)

Update the return type for `getHistoryPage` to include pagination metadata:

```kotlin
suspend fun getHistoryPage(page: Int, size: Int): Result<DailyTrackingHistoryPage>
```

#### [NEW] DailyTrackingHistoryPage.kt
Path: `domain/.../dailytracking/model/DailyTrackingHistoryPage.kt`

```kotlin
data class DailyTrackingHistoryPage(
    val entries: List<DailyTrackingSummary>,
    val isLastPage: Boolean,
    val currentPage: Int,
)
```

---

### Component 5 — Data Layer: Update Repository Implementation

#### [MODIFY] [DailyTrackingRepositoryImpl.kt](file:///c:/Users/DELL/Documents/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/repository/DailyTrackingRepositoryImpl.kt)

Update `getHistoryPage` to return the new `DailyTrackingHistoryPage`:

```kotlin
override suspend fun getHistoryPage(page: Int, size: Int): Result<DailyTrackingHistoryPage> =
    withContext(ioDispatcher) {
        runCatchingCancellable {
            val response = api.getHistoryPage(page, size)
            DailyTrackingHistoryPage(
                entries = response.content.map { it.toDomain() },
                isLastPage = response.last,
                currentPage = response.number,
            )
        }
    }
```

---

### Component 6 — Domain Layer: New UseCase

#### [NEW] GetCaloriesHistoryUseCase.kt
Path: `domain/.../dailytracking/usecase/GetCaloriesHistoryUseCase.kt`

```kotlin
class GetCaloriesHistoryUseCase @Inject constructor(
    private val repository: IDailyTrackingRepository,
) {
    suspend operator fun invoke(page: Int, size: Int): Result<DailyTrackingHistoryPage> =
        repository.getHistoryPage(page, size)
}
```

---

### Component 7 — Presentation Layer: Update State

#### [MODIFY] [CaloriesHistoryState.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/calories_history/state/CaloriesHistoryState.kt)

Add pagination-related fields:

```kotlin
@Immutable
data class CaloriesHistoryState(
    val isLoading: Boolean = false,
    val entries: ImmutableList<CaloriesHistoryDayUiModel> = persistentListOf(),
    val currentPage: Int = 0,
    val isLastPage: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
)
```

#### [MODIFY] [CaloriesHistoryEvent.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/calories_history/state/CaloriesHistoryEvent.kt)

Add event for loading more:

```kotlin
sealed interface CaloriesHistoryEvent {
    data object NavigateBack : CaloriesHistoryEvent
    data object CalendarClicked : CaloriesHistoryEvent
    data object LoadMore : CaloriesHistoryEvent       // NEW — triggered when user scrolls to bottom
    data object Retry : CaloriesHistoryEvent           // NEW — retry on error
}
```

---

### Component 8 — Presentation Layer: Update ViewModel

#### [MODIFY] [CaloriesHistoryViewModel.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/calories_history/viewmodel/CaloriesHistoryViewModel.kt)

- Inject `GetCaloriesHistoryUseCase` via constructor.
- Replace `loadMockedData()` with `loadFirstPage()` that calls the UseCase.
- Add `loadMore()` for pagination — appends results to existing list.
- Map `DailyTrackingSummary` → `CaloriesHistoryDayUiModel` (date formatting: `dd-M-yyyy`).
- Handle loading/error states.

---

### Component 9 — Presentation Layer: Update Screen (Pagination Trigger)

#### [MODIFY] [CaloriesHistoryScreen.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/calories_history/view/CaloriesHistoryScreen.kt)

- Detect when the user scrolls near the bottom of the `LazyColumn` and fire `CaloriesHistoryEvent.LoadMore`.
- Show a loading indicator at the bottom of the list while `isLoadingMore` is true.
- Show an error state with a retry button when `errorMessage` is not null and the list is empty.

---

### Component 10 — DI Wiring

#### [MODIFY] [UseCaseModule.kt](file:///c:/Users/DELL/Documents/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/di/UseCaseModule.kt)

No change needed — the UseCase uses `@Inject constructor`, so Hilt can provide it automatically without a `@Provides` or `@Binds` method.

---

## Open Questions

> [!IMPORTANT]
> **Page size**: What page size should be used for each request? I'm going with `size = 10` as default — is that acceptable?

> [!IMPORTANT]
> **Error handling UX**: When the first page fails to load, should we show a full-screen error with a retry button, or show a Snackbar? I'm going with a full-screen error + retry for the first page, and a Snackbar for subsequent pages.

---

## Verification Plan

### Build Check
- Run `./gradlew assembleDebug` to verify the project compiles.

### Manual Verification
- Open the Calories History screen and verify real data loads from the backend.
- Scroll to the bottom and verify the next page loads automatically.
- Turn off network and verify the error state appears with a retry button.
- Verify the date format matches the expected `dd-M-yyyy` format.
