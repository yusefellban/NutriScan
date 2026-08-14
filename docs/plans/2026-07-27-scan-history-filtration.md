# Add Query Parameter Filtration & DatePicker to Scan History

This plan outlines the changes required to move scan history filtration from local filtering to server-side query parameters (`date` and `verdict`). It also introduces a DatePicker and a Reset button to the screen.

## Proposed Changes

### Data Layer
#### [MODIFY] ScanApiService.kt
- Add `@Query("date") date: String?` and `@Query("verdict") verdict: String?` to the `getRecentScans` method.

#### [MODIFY] IScanRepository.kt
- Add `date: String?` and `verdict: String?` to `getRecentScans`.

#### [MODIFY] ScanRepositoryImpl.kt
- Modify `getRecentScans` to accept and pass the new parameters.
- Remove the `localRecentScans` cache to avoid bugs related to caching filtered lists and always rely on the backend.

### Domain Layer
#### [MODIFY] GetRecentScansUseCase.kt
- Accept `date: String? = null` and `verdict: String? = null` and pass them to the repository.

### Presentation Layer
#### [MODIFY] ScanHistoryState.kt
- Add `selectedDate: String? = null` (format: `yyyy-MM-dd`).
- Add `showDatePicker: Boolean = false`.
- Ensure `selectedFilter` correctly represents the query parameter values (e.g., `null` for ALL, `SAFE`, `CAUTION`, `UNSAFE`).

#### [MODIFY] ScanHistoryEvent.kt
- Add `DateSelected(val date: Long?)`
- Add `ShowDatePicker(val show: Boolean)`
- Add `ResetFilters`

#### [MODIFY] ScanHistoryViewModel.kt
- Remove local filtration logic from `updateDisplayedItems`.
- In `loadInitial()` and `loadMore()`, fetch using the new parameters: `getRecentScansUseCase(page, size, date = state.value.selectedDate, verdict = verdictParam)`.
- Handle the new events to reset and select dates, and trigger a fresh reload when they change.

#### [MODIFY] ScanHistoryScreen.kt
- Replace the current custom `FilterRow` with `SelectableChip` inside a `LazyRow` to match the `Exercises` screen.
- Add an action icon (calendar) to the `AppTopHeader` or just below it to trigger the `DatePickerDialog`.
- Add a "Reset" button when filters are active to clear the date and verdict.
- Implement a Material 3 `DatePickerDialog`.

## Verification Plan
### Manual Verification
- Open the Scan History screen.
- Select a date from the DatePicker and verify it appends `?date=YYYY-MM-DD` and triggers a reload.
- Select a verdict chip (e.g., UNSAFE) and verify it appends `?verdict=UNSAFE`.
- Combine both filters.
- Click the Reset button and verify it clears both filters and reloads all history.
