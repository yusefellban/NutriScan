# Implementation Plan: Connect Saved Screen to Real Data

## Overview
This plan outlines the steps required to remove the dummy data from the Saved screen (bookmarks/scan history) and replace it with real scanned items from the local database. The "Save" button in the Camera Scan Screen is already triggering the `SaveScanUseCase`, so the focus is primarily on fetching and displaying the saved scans.

## Proposed Changes

### 1. Domain Layer (`domain/scan`)
- **Modify** `ISavedScanRepository.kt`: Add `fun getSavedScans(): Flow<List<ScanResult>>` to allow observing the saved scans.
- **Create** `GetSavedScansUseCase.kt`: Create a new use case in `domain/scan/usecase` that calls `ISavedScanRepository.getSavedScans()`.

### 2. Data Layer (`data/repository`)
- **Modify** `SavedScanRepositoryImpl.kt`: 
  - Implement `getSavedScans()` which returns `savedScanDao.getAllSavedScans()`.
  - Apply a `.map { ... }` on the Flow to convert each `SavedScanEntity` back into a `ScanResult` domain model. This involves deserializing the `flaggedIngredientsJson` and `nutritionFactsJson` back into their respective models.

### 3. Presentation Layer (`presentation/saved`)
- **Modify** `SavedViewModel.kt`:
  - Remove `loadMockData()` and the hardcoded list of `ProductUiModel` objects.
  - Inject `GetSavedScansUseCase`.
  - In `init`, collect the flow from `GetSavedScansUseCase()`.
  - Map the incoming `List<ScanResult>` to a `List<ProductUiModel>` to update the UI state.
    - Set the `productName` to `"Scanned Product"` or extract from summary.
    - Extract `calories` from the decoded nutrition facts.
    - Map the verdict strings (SAFE, CAUTION, UNSAFE) to `ProductVerdict` enums.
  - Apply the search filter logic onto the newly fetched list.

## Verification
- Run the app and ensure the Saved screen starts empty (or shows actual saved scans).
- Go to the Camera scan screen, mock a scan, and hit the Bookmark/Save button.
- Return to the Saved screen and verify the newly saved scan appears in the list.
