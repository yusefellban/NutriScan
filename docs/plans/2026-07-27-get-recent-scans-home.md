# Get Recent Scans — Home Screen Integration

## Goal Description
Replace the **hardcoded dummy data** currently powering the Home screen's "Recent History" section with **real data** from the backend `GET /v1/scans?page=0&size=3` endpoint. When the user taps "View All", navigate to a **full scan history screen** that fetches all scans with proper pagination.

The API returns a Spring Boot `Page<T>` response. The list endpoint's items have a **different shape** from the single-scan detail endpoint (`GET /v1/scans/{scanId}`), so we need a new DTO for the list item and a lightweight domain model for the summary.

---

## User Review Required

> [!IMPORTANT]
> 1. The API's list items have a **different shape** from `ScanResultResponseDto` — the list items have `verdict` at the top level (not nested in `foodSafetyResponse`) and include `calories` directly. We need a **new DTO** (`ScanHistoryItemDto`) for these list items, plus a **page wrapper DTO** (`PageDto<T>`). The existing `ScanResultResponseDto` is for the detail endpoint and will not be changed.
> 2. A **new domain model** `ScanHistoryEntry` will be created (as specified in `AGENTS.md` §2.2). This is a lightweight summary model distinct from the full `ScanResult`.
> 3. Since `ProductVerdict` is `@Serializable`, we use it **directly** as the `verdict` field type in `ScanHistoryItemDto` — no manual `mapVerdict()` string-to-enum conversion needed for the list item mapper.
> 4. The **`VerdictType` enum** in presentation currently has `GREEN, CYAN, YELLOW, RED`. The mapping in the ViewModel will be: `SAFE → GREEN`, `CAUTION → YELLOW`, `UNSAFE → RED`. `CYAN` remains available for future use.
> 5. The `HomeHistoryItem.verdictLabelResId` will use the existing strings: `verdict_safe`, `verdict_caution`, `verdict_unsafe`.
> 6. `HomeState` will get an `isHistoryLoading` field and a `historyError` field to handle loading/error states independently from the rest of the home screen.

---

## Existing Code Inventory

| Concern | Exists? | File | Notes |
|---------|---------|------|-------|
| `ScanApiService` | ✅ | `ScanApiService.kt` | Has `submitScan` & `getScanResult` — needs new `getRecentScans()` method |
| `ScanResultResponseDto` | ✅ | `ScanResultResponseDto.kt` | For detail endpoint — **NOT reusable** for list items (different shape) |
| `IScanRepository` | ✅ | `IScanRepository.kt` | Needs new `getRecentScans()` method |
| `ScanRepositoryImpl` | ✅ | `ScanRepositoryImpl.kt` | Needs implementation of new method |
| `ScanMapper.kt` | ✅ | `ScanMapper.kt` | Needs new mapper for list item DTO → domain |
| `ProductVerdict` | ✅ | `ProductVerdict.kt` | `SAFE, CAUTION, UNSAFE` — reused directly |
| `HomeViewModel` | ✅ | `HomeViewModel.kt` | Currently uses dummy data — will be refactored |
| `HomeState` | ✅ | `HomeState.kt` | Needs `isHistoryLoading` + `historyError` fields |
| `HomeHistoryItem` | ✅ | `HomeHistoryItem.kt` | Reused as-is — populated from real data |
| `VerdictType` | ✅ | `VerdictType.kt` | `GREEN, CYAN, YELLOW, RED` — mapping logic in VM |
| `HistoryItemCard` | ✅ | `HistoryItemCard.kt` | No changes needed |
| String resources | ✅ | `strings.xml` | `verdict_safe`, `verdict_caution`, `verdict_unsafe` already exist |
| `RepositoryModule` | ✅ | `RepositoryModule.kt` | `IScanRepository` already bound — **no changes** |
| `NetworkModule` | ✅ | `NetworkModule.kt` | `ScanApiService` already provided — **no changes** |

---

## Proposed Changes

### Phase 1 — Data Layer (DTOs + API + Mapper + Repository)

#### [NEW] data/src/main/kotlin/.../data/remote/dto/ScanHistoryItemDto.kt
New DTO matching the list item shape from `GET /v1/scans`:
```kotlin
@Serializable
data class ScanHistoryItemDto(
    val scanId: String,
    val imageUrl: String? = null,
    val verdict: ProductVerdict? = null,  // @Serializable enum — deserialized directly
    val scannedAt: String? = null,
    val productName: String? = null,
    val calories: Long? = null,
    val status: String,
)
```

#### [NEW] data/src/main/kotlin/.../data/remote/dto/PageDto.kt
Generic page wrapper matching Spring Boot's `Page<T>` response:
```kotlin
@Serializable
data class PageDto<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,       // current page number (0-based)
    val size: Int,         // page size
    val first: Boolean,
    val last: Boolean,
    val empty: Boolean,
)
```
> **Note:** We type-alias `PageDto<ScanHistoryItemDto>` as the return type from the API. The `pageable` and `sort` nested objects are ignored via `ignoreUnknownKeys = true` in our Json config.

#### [MODIFY] data/src/main/kotlin/.../data/remote/api/ScanApiService.kt
Add the list endpoint:
```kotlin
@GET("v1/scans")
suspend fun getRecentScans(
    @Query("page") page: Int,
    @Query("size") size: Int,
): PageDto<ScanHistoryItemDto>
```

---

#### [NEW] domain/src/main/kotlin/.../domain/scan/model/ScanHistoryEntry.kt
Lightweight domain model for a scan summary in the history list:
```kotlin
data class ScanHistoryEntry(
    val scanId: String,
    val imageUrl: String?,
    val verdict: ProductVerdict?,   // directly from DTO — no mapping needed
    val scannedAt: String?,
    val productName: String?,
    val calories: Long?,
    val status: ScanStatus,
)
```

#### [MODIFY] domain/src/main/kotlin/.../domain/scan/repository/IScanRepository.kt
Add:
```kotlin
suspend fun getRecentScans(page: Int, size: Int): Result<List<ScanHistoryEntry>>
```

#### [MODIFY] data/src/main/kotlin/.../data/repository/mapper/ScanMapper.kt
Add mapper for the list item:
```kotlin
fun ScanHistoryItemDto.toDomain(): ScanHistoryEntry {
    return ScanHistoryEntry(
        scanId = scanId,
        imageUrl = imageUrl,
        verdict = verdict,             // ProductVerdict enum — no mapping needed
        scannedAt = scannedAt,
        productName = productName,
        calories = calories,
        status = mapStatus(status),
    )
}
```
> The `mapStatus()` helper already exists (currently `private`) — it will be made `internal` so the new mapper can reuse it. `mapVerdict()` is **not needed** here since `verdict` is already a `ProductVerdict` enum.

#### [MODIFY] data/src/main/kotlin/.../data/repository/ScanRepositoryImpl.kt
Implement:
```kotlin
override suspend fun getRecentScans(page: Int, size: Int): Result<List<ScanHistoryEntry>> {
    return withContext(ioDispatcher) {
        try {
            val response = scanApiService.getRecentScans(page, size)
            Result.success(response.content.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

### Phase 2 — Domain Layer (Use Case)

#### [NEW] domain/src/main/kotlin/.../domain/scan/usecase/GetRecentScansUseCase.kt
```kotlin
class GetRecentScansUseCase @Inject constructor(
    private val scanRepository: IScanRepository
) {
    suspend operator fun invoke(page: Int, size: Int): Result<List<ScanHistoryEntry>> {
        return scanRepository.getRecentScans(page, size)
    }
}
```

---

### Phase 3 — Presentation Layer (Home Screen Integration)

#### [MODIFY] presentation/.../home/state/HomeState.kt
Add loading and error fields for the history section:
```kotlin
data class HomeState(
    val userName: String = "",
    val firstName: String = "",
    val avatarUrl: String? = null,
    val isLoading: Boolean = false,
    val isHistoryLoading: Boolean = false,
    val historyError: String? = null,
    val recentHistory: ImmutableList<HomeHistoryItem> = persistentListOf(),
)
```

#### [MODIFY] presentation/.../home/state/HomeEvent.kt
Add a retry event:
```kotlin
sealed interface HomeEvent {
    // ... existing events ...
    data object RetryLoadHistory : HomeEvent
}
```

#### [MODIFY] presentation/.../home/viewmodel/HomeViewModel.kt
- **Inject** `GetRecentScansUseCase`.
- **Remove** the `createInitialState()` dummy data — initial state starts with empty `recentHistory` and `isHistoryLoading = true`.
- **Add** `loadRecentScans()` called from `init {}`:
  - Calls `getRecentScansUseCase(page = 0, size = 3)`
  - Maps each `ScanHistoryEntry` → `HomeHistoryItem` using:
    - `verdict` → `VerdictType` mapping (`SAFE→GREEN`, `CAUTION→YELLOW`, `UNSAFE→RED`)
    - `verdict` → `verdictLabelResId` mapping (`SAFE→R.string.verdict_safe`, etc.)
    - `scannedAt` ISO timestamp → human-readable relative date string (e.g., "Today, 9:24 AM")
    - `productName` → capitalized, fallback to `"Unknown"` string resource
  - Updates `_state` with the mapped list
  - Handles error by setting `historyError`
- **Handle** `RetryLoadHistory` event → calls `loadRecentScans()` again.

#### [MODIFY] presentation/.../home/view/HomeScreen.kt
- Show a `LoadingShimmer` or `CircularProgressIndicator` when `isHistoryLoading` is true.
- Show an error state with retry when `historyError` is not null.
- Show an `EmptyStateWidget` when the list is empty and not loading.

---

## Open Questions

> [!IMPORTANT]
> 1. The `scannedAt` format from the API is ISO 8601 (`2026-07-26T17:03:29.248150Z`). Should we show it as a relative date ("Today, 5:03 PM" / "Yesterday, 4:15 PM") or as an absolute date ("Jul 26, 2026")?  **My recommendation:** relative date, matching the current dummy data format.
> 2. When `productName` is `"unknown"` from the API — should we display it as "Unknown Product" (localized) or just show "unknown" as-is? **My recommendation:** Show a localized "Unknown Product" string.
> 3. The `calories` field can be `null` from the API. The `HomeHistoryItem` doesn't currently show calories. Should we add it to the card or skip it for now? **My recommendation:** Skip for now — the `HistoryItemCard` doesn't display calories in the Figma.

---

## Verification Plan

### Build Verification
```bash
./gradlew :domain:assembleDebug :data:assembleDebug :presentation:assembleDebug :app:assembleDebug
```

### Manual Verification
- Deploy to device/emulator with a logged-in user that has scan history.
- Verify the Home screen loads the 3 most recent scans from the API.
- Verify the verdict badge colors match: SAFE → green, CAUTION → yellow, UNSAFE → red.
- Verify the product images load from the Cloudinary URLs.
- Verify "View All" still navigates to the history screen.
- Verify loading state shows while the API call is in-flight.
- Verify error state shows and retry works when the API call fails (e.g., airplane mode).

---

## Definition of Done
- [ ] `ScanHistoryItemDto` and `PageDto` created in data/remote/dto/.
- [ ] `ScanApiService` updated with `getRecentScans()` method.
- [ ] `ScanHistoryEntry` domain model created.
- [ ] `IScanRepository` updated with `getRecentScans()`.
- [ ] `ScanRepositoryImpl` implements the new method.
- [ ] `ScanMapper` extended with list-item mapping.
- [ ] `GetRecentScansUseCase` created.
- [ ] `HomeState` updated with `isHistoryLoading` and `historyError`.
- [ ] `HomeEvent.RetryLoadHistory` added.
- [ ] `HomeViewModel` wired to real API data (no dummy data).
- [ ] `HomeScreen` handles loading/error/empty states for history section.
- [ ] Project builds without errors.
- [ ] No hardcoded strings or colors.
