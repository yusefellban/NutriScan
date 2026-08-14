
# Delete Scan History Implementation Plan

Add support for deleting scan items from the user's history list. This requires implementing the DELETE endpoint in the data layer, adding a domain UseCase, and wiring up a long-press interaction in the UI to trigger a custom confirmation dialog.

## Proposed Changes

---

### Data Layer
Implement the deletion endpoint in the network interface and repository.

#### [MODIFY] [ScanApiService.kt](file:///c:/Users/DELL/Documents/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/remote/api/ScanApiService.kt)
- Add `@DELETE("v1/scans/{scanId}") suspend fun deleteScan(@Path("scanId") scanId: String)`

#### [MODIFY] [IScanRemoteDataSource.kt](file:///c:/Users/DELL/Documents/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/remote/datasource/IScanRemoteDataSource.kt) & [ScanRemoteDataSourceImpl.kt](file:///c:/Users/DELL/Documents/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/remote/datasource/ScanRemoteDataSourceImpl.kt)
- Add `deleteScan(scanId: String)` to both the interface and its implementation.

#### [MODIFY] [ScanRepositoryImpl.kt](file:///c:/Users/DELL/Documents/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/repository/ScanRepositoryImpl.kt)
- Add `deleteScan(scanId: String): Result<Unit>` method calling the remote data source.

---

### Domain Layer
Define the business logic for deleting a scan.

#### [MODIFY] [IScanRepository.kt](file:///c:/Users/DELL/Documents/NutriScan/domain/src/main/kotlin/iti/grad/nutriscan/domain/scan/repository/IScanRepository.kt)
- Add `suspend fun deleteScan(scanId: String): Result<Unit>`

#### [NEW] [DeleteScanUseCase.kt](file:///c:/Users/DELL/Documents/NutriScan/domain/src/main/kotlin/iti/grad/nutriscan/domain/scan/usecase/DeleteScanUseCase.kt)
- Create a new UseCase that takes a `scanId` and calls `repository.deleteScan(scanId)`.

---

### Presentation Layer
Implement the long-press interaction, dialog state, and success/error handling.

#### [MODIFY] [HistoryItemCard.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/components/HistoryItemCard.kt)
- Change `clickable` to `combinedClickable` (Opt-in to `ExperimentalFoundationApi`).
- Add `onLongClick: () -> Unit` parameter and bind it to the `combinedClickable`.

#### [MODIFY] [ScanHistoryState.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan_history/state/ScanHistoryState.kt)
- Add `val itemToDelete: HistoryItemUiModel? = null` to hold the state of the dialog.

#### [MODIFY] [ScanHistoryEvent.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan_history/state/ScanHistoryEvent.kt)
- Add `OnHoldItem(val item: HistoryItemUiModel)`
- Add `ConfirmDelete`
- Add `DismissDeleteDialog`

#### [MODIFY] [ScanHistoryEffect.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan_history/state/ScanHistoryEffect.kt)
- Add `ShowSuccessMessage` and `ShowErrorMessage` to show snackbars on result.

#### [MODIFY] [ScanHistoryViewModel.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan_history/viewmodel/ScanHistoryViewModel.kt)
- Inject `DeleteScanUseCase`.
- Handle the new events.
- On `ConfirmDelete`, invoke the UseCase, and if successful, remove the item from `allHistoryItems` locally (to update the UI instantly without re-fetching from page 0), emit success effect, and set `itemToDelete = null`.

#### [MODIFY] [ScanHistoryScreen.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan_history/view/ScanHistoryScreen.kt)
- Pass `onLongClick` to `HistoryItemCard`.
- Render the `ConfirmationDialog` if `state.itemToDelete != null` using new string resources (`delete_scan_title`, `delete_scan_message`, `delete`, `cancel`).

#### [MODIFY] [strings.xml](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/res/values/strings.xml)
- Add localization strings for the delete dialog.

---

## Verification Plan

### Automated Tests
- Run `./gradlew compileDebugKotlin` to ensure domain/data/presentation compiles properly.

### Manual Verification
- Long press on an item in the Scan History screen.
- Confirm the custom dialog appears.
- Click cancel -> dialog dismisses.
- Click confirm -> dialog dismisses, item disappears from list, and success snackbar appears.
