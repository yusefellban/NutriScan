# Scan History — Search with Autocomplete Suggestions

## Overview

Add a **search bar** to the Scan History screen that lets users filter by product name.
As the user types, the app hits `/api/v1/scans/suggestions?query=<text>` with a **300 ms debounce**
to show autocomplete suggestions in a dropdown. Selecting a suggestion (or pressing search)
triggers a fresh paginated load of `GET /api/v1/scans?query=<text>` with all other active filters preserved.

---

## Proposed Changes

### ─── data layer ───────────────────────────────────────────────

#### [MODIFY] ScanApiService.kt
- Add new endpoint:
```kotlin
@GET("v1/scans/suggestions")
suspend fun getScanSuggestions(
    @Query("query") query: String
): List<String>
```

#### [MODIFY] IScanRepository.kt
- Add:
```kotlin
suspend fun getScanSuggestions(query: String): Result<List<String>>
```

#### [MODIFY] ScanRepositoryImpl.kt
- Implement `getScanSuggestions()` — calls `scanApiService.getScanSuggestions(query)` on `ioDispatcher`, no caching needed.

---

### ─── domain layer ─────────────────────────────────────────────

#### [NEW] GetScanSuggestionsUseCase.kt
- Path: `domain/.../scan/usecase/GetScanSuggestionsUseCase.kt`
- Simple delegator to `IScanRepository.getScanSuggestions(query)`.

---

### ─── presentation layer ───────────────────────────────────────

#### [MODIFY] ScanHistoryState.kt
New fields:
```kotlin
val searchQuery: String = "",
val suggestions: ImmutableList<String> = persistentListOf(),
val isSuggestionsLoading: Boolean = false,
val isSearchActive: Boolean = false,   // controls dropdown visibility
```

#### [MODIFY] ScanHistoryEvent.kt
New events:
```kotlin
data class SearchQueryChanged(val query: String) : ScanHistoryEvent
data class SuggestionSelected(val suggestion: String) : ScanHistoryEvent
object SearchSubmitted : ScanHistoryEvent
object SearchCleared : ScanHistoryEvent
```

#### [MODIFY] ScanHistoryViewModel.kt
- Inject `GetScanSuggestionsUseCase`.
- Add a private `MutableStateFlow<String>` for the raw query; debounce 300 ms with `debounce()` + `distinctUntilChanged()` in `init {}` to call suggestions.
- `SearchQueryChanged` → update `searchQuery` in state, emit to debounce flow.
- `SearchSubmitted` / `SuggestionSelected` → set `searchQuery`, clear suggestions dropdown, call `loadInitial()` which already passes `query` param to `getRecentScansUseCase`.
- `SearchCleared` → reset `searchQuery = ""`, clear suggestions, reload.
- Pass `state.searchQuery` into `loadInitial()` and `loadMore()` alongside existing filters.

#### [MODIFY] ScanHistoryScreen.kt
- Add a `SearchBar` composable between the top header and the `FilterRow`.
- Show a `DropdownMenu` anchored to the search bar when `state.suggestions` is non-empty and `state.isSearchActive`.
- Tapping a suggestion fires `SuggestionSelected`.
- "✕" icon inside the bar fires `SearchCleared`.
- IME action `Search` fires `SearchSubmitted`.

#### [NEW] ScanHistorySearchBar.kt (component inside `scan_history/view/`)
- Self-contained `@Composable` that receives `query`, `suggestions`, `isLoading`, and callbacks.
- Styled with `AppTheme` — rounded corners, teal focus ring, matching the app design.
- Suggestion dropdown items show the matching text with the typed portion highlighted.

---

## Debounce Flow (ViewModel)

```
User types  →  SearchQueryChanged  →  _searchQueryFlow.emit(query)
                                           ↓  debounce(300ms)
                                     getScanSuggestionsUseCase(query)
                                           ↓
                              state.suggestions = result  (shown in dropdown)

User picks suggestion / presses Search
                  →  clear dropdown  →  loadInitial(query = currentQuery)
```

---

## Key Decisions

| Decision | Rationale |
|---|---|
| Debounce 300 ms via `Flow.debounce()` in ViewModel | Keeps ViewModel threading-agnostic; no handler/timer leaks |
| `query` passed to `loadInitial()` — not stored separately | Reuses existing `getRecentScansUseCase` `query` param; no new API call |
| Suggestions are **not cached** | Short-lived, typed UX; stale cache would be confusing |
| `isSearchActive` flag in state | Prevents dropdown re-appearing on recomposition after selection |
| Preserve existing `date` + `verdict` filters when searching | User expects filters to stack, not replace each other |

---

## Verification Plan

### Automated Tests
- Update `ScanHistoryViewModelTest` with cases:
  - Query debounces → suggestions fetched only once for rapid typing
  - `SearchCleared` resets query and reloads all items
  - `SuggestionSelected` triggers `loadInitial` with correct query

### Manual Verification
- Type in search bar → suggestions appear after ~300 ms
- Select suggestion → list filters correctly, dropdown closes
- Clear → full list restored
- Combined filter (date + search) works together
- Empty suggestions → no dropdown shown
