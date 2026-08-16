# Plan: Scan History Failed Status + SAFE Filter Validation

## 1. Feature Summary
Fix scan history and home recent-history mapping so items with backend `status=FAILED` are rendered as Failed (not Safe), and validate that SAFE/date filters are sent correctly to the scans endpoint.

## 2. Files to Create
- docs/plans/2026-08-12-scan-history-failed-status-and-safe-filter.md: Implementation and verification plan for this bug fix.

## 3. Files to Modify
- presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan_history/viewmodel/ScanHistoryViewModel.kt: Prioritize `ScanStatus.FAILED` in UI mapping (label + type) before verdict fallback.
- presentation/src/main/kotlin/iti/grad/nutriscan/presentation/home/viewmodel/HomeViewModel.kt: Apply the same failed-status mapping in home recent history cards.
- presentation/src/test/java/iti/grad/nutriscan/presentation/home/HomeViewModelTest.kt: Add/adjust test coverage for failed-status mapping in history card output.

## 4. Layer Breakdown
### Domain
- No domain model changes.
- No use case contract changes.
- No repository interface changes.

### Data
- No DTO changes.
- No mapper changes.
- No DAO changes.
- Validation only: confirm `ScanApiService.getRecentScans(page,size,date,verdict)` exists and repository forwards date/verdict parameters unchanged.

### Presentation
- State properties: no schema changes.
- Events: no changes.
- Effects: no changes.
- ViewModel logic outline:
  - In history mapping, if `entry.status == ScanStatus.FAILED`, map to Failed label and non-safe visual type.
  - Otherwise, keep existing verdict-based mapping.

## 5. Navigation Changes
- No navigation changes.

## 6. Strings - MANDATORY (Zero Hardcoded Text)
No new strings are introduced. Existing keys used:

| Key (R.string.xxx) | English Value | Arabic Value |
|---|---|---|
| scan_status_failed | Failed | فشل |
| verdict_safe | Safe | آمن |
| verdict_caution | Caution | حذر |
| verdict_unsafe | Unsafe | غير آمن |
| verdict_unknown | Unknown | غير معروف |

## 7. Testing Plan
- Update/add ViewModel test(s) to cover:
  - When a history item has `status=FAILED` and `verdict=null`, the resulting UI model uses `scan_status_failed` and does not map to Safe.
  - Existing safe/caution/unsafe verdict mapping remains unchanged for completed scans.

## 8. Edge Cases
- `status=FAILED` with `verdict=SAFE` (backend inconsistency): UI still shows Failed.
- `status=FAILED` with `verdict=null`: UI shows Failed.
- Date+verdict filters present: repository/API call should still forward both query params.

## 9. Definition of Done
- [ ] Failed scans in history/home no longer display Safe.
- [ ] SAFE/date query forwarding verified in code path.
- [ ] ViewModel test coverage updated for failed-status mapping.
- [ ] No new hardcoded strings/colors introduced.
- [ ] Targeted module checks/tests pass.
