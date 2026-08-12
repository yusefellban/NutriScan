# Plan: History Failed vs Unsafe Visual Separation

## 1. Feature Summary
Make FAILED and UNSAFE scans visually distinct in Scan History cards while keeping existing behavior elsewhere.

## 2. Files to Create
- docs/plans/2026-08-12-history-failed-unsafe-visual-separation.md: implementation plan.

## 3. Files to Modify
- presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/model/HistoryItemUiModel.kt: add `FAILED` to `VerdictType`.
- presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/components/HistoryItemCard.kt: add dedicated icon/color styling branch for `FAILED`.
- presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan_history/viewmodel/ScanHistoryViewModel.kt: map `ScanStatus.FAILED` to `VerdictType.FAILED`.

## 4. Layer Breakdown
### Domain
- No changes.

### Data
- No changes.

### Presentation
- Introduce explicit UI verdict type for failed state.
- Distinguish badge icon/tint/background between `UNSAFE` and `FAILED`.

## 5. Navigation Changes
- None.

## 6. Strings
- No new strings; reuse `scan_status_failed` and existing verdict labels.

## 7. Testing Plan
- Compile-time verification for exhaustive `when` branches.
- Manual visual check in Scan History: failed cards differ from unsafe cards.

## 8. Edge Cases
- If backend returns failed with verdict unsafe/safe/null, UI still uses FAILED style in history.

## 9. Definition of Done
- [ ] FAILED and UNSAFE look different in history cards.
- [ ] No regressions in Home/History mapping compilation.
