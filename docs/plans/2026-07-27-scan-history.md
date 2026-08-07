# View All History Plan

## Goal
Implement the `ScanHistoryScreen` which allows users to view a paginated list of all their previously scanned products. This screen will be accessed when the user clicks "View All" on the Home Screen.

## Proposed Changes

### 1. Refactor Common History UI Components
To avoid duplicating the UI logic and models between `HomeScreen` and `ScanHistoryScreen`, we will extract the history card component and its model to a common location.

- **[NEW] `presentation/common/model/HistoryItemUiModel.kt`**: Rename `HomeHistoryItem` to `HistoryItemUiModel` and move it here. Also move `VerdictType`.
- **[MODIFY] `presentation/home/view/components/HistoryItemCard.kt`**: Move this file to `presentation/common/components/HistoryItemCard.kt` and update its imports to use `HistoryItemUiModel`.
- **[MODIFY] `presentation/home/viewmodel/HomeViewModel.kt`**: Update `mapScansToUi` to use `HistoryItemUiModel` instead of `HomeHistoryItem`.
- **[MODIFY] `presentation/home/state/HomeState.kt`**: Update `recentHistory` to be `ImmutableList<HistoryItemUiModel>`.

### 2. Create Scan History Feature Architecture
- **[NEW] `presentation/scan_history/state/ScanHistoryState.kt`**: 
  - `historyItems: ImmutableList<HistoryItemUiModel>`
  - `isLoading: Boolean`
  - `isPaginationLoading: Boolean`
  - `error: String?`
  - `page: Int`
  - `isLastPage: Boolean`
- **[NEW] `presentation/scan_history/state/ScanHistoryEvent.kt`**: 
  - `LoadInitial`
  - `LoadMore`
  - `ItemClicked(scanId)`
  - `BackClicked`
- **[NEW] `presentation/scan_history/state/ScanHistoryEffect.kt`**:
  - `NavigateBack`
  - `NavigateToProductDetails(scanId)`

### 3. Implement Scan History ViewModel
- **[NEW] `presentation/scan_history/viewmodel/ScanHistoryViewModel.kt`**:
  - Inject `GetRecentScansUseCase`.
  - Handle `LoadInitial` to fetch `page = 0`.
  - Handle `LoadMore` to fetch `page + 1` when user scrolls to the bottom.
  - Convert `ScanHistoryEntry` from Domain to `HistoryItemUiModel` using a similar `mapScansToUi` function.
  - Append new items to the `historyItems` list upon successful pagination. If the fetched list size is less than the requested size (e.g. 10), set `isLastPage = true` to stop further pagination requests.

### 4. Implement Scan History UI
- **[NEW] `presentation/scan_history/view/ScanHistoryScreen.kt`**:
  - Use a `Scaffold` with a custom `TopAppBar` (Title: "Scan History", Back Arrow).
  - Use `LazyColumn` to render the list of `HistoryItemCard`.
  - Add a `LaunchedEffect` that triggers `LoadMore` when `LazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index` is near the end of the list.
  - Display a loading indicator at the bottom when `isPaginationLoading` is true.
  - Display an empty state if `historyItems` is empty and `isLoading` is false.

### 5. Update Navigation
- **[MODIFY] `app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt`**:
  - Replace the placeholder for `ScanHistoryRoute` with `ScanHistoryScreen(onNavigateBack, onNavigateToProductDetails)`.

## Open Questions
1. How many items should we load per page? (Suggesting `10` or `20`).
2. Do we need any filtering/sorting options on this screen, or is it strictly chronological? (Assuming chronological based on backend defaults for now).

## Verification Plan
- Build the project to verify refactored paths.
- Test "View All" from `HomeScreen` to ensure it navigates to the new screen.
- Verify that scrolling triggers pagination correctly and new items append smoothly.
- Verify clicking a card navigates properly to `ProductDetailsScreen`.
