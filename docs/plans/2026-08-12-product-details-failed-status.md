# Plan: Product Details Failed Status Rendering

## 1. Feature Summary
Ensure Product Details renders failed scans as Failed when backend returns `status=FAILED` and `foodSafetyResponse.verdict=null`, instead of showing Safe.

## 2. Files to Create
- docs/plans/2026-08-12-product-details-failed-status.md: Implementation plan and verification notes for this fix.

## 3. Files to Modify
- domain/src/main/kotlin/iti/grad/nutriscan/domain/scan/model/ProductDetail.kt: Add scan status to the domain detail model so the UI can render failure state explicitly.
- presentation/src/main/kotlin/iti/grad/nutriscan/presentation/product_details/viewmodel/ProductDetailsViewModel.kt: Map and persist product detail status from `ScanResult`.
- presentation/src/main/kotlin/iti/grad/nutriscan/presentation/product_details/view/components/ProductInfoHeader.kt: Render Failed badge when status is failed; render verdict badge only otherwise.
- presentation/src/main/kotlin/iti/grad/nutriscan/presentation/product_details/view/ProductDetailsScreen.kt: Pass status to header component.

## 4. Layer Breakdown
### Domain
- Extend `ProductDetail` model with `status: ScanStatus`.

### Data
- No DTO/API/DAO changes.

### Presentation
- Update Product Details mapping in ViewModel to avoid implicit safe fallback for failed scans.
- Update Product header UI decision:
  - `FAILED` -> show `scan_status_failed` label.
  - otherwise -> show existing verdict badge.

## 5. Navigation Changes
- No navigation changes.

## 6. Strings - MANDATORY (Zero Hardcoded Text)
No new strings needed; reuse existing key.

| Key (R.string.xxx) | English Value | Arabic Value |
|---|---|---|
| scan_status_failed | Failed | فشل |

## 7. Testing Plan
- Validate with a `ScanResult` containing:
  - `status=FAILED`
  - `foodSafetyResponse.verdict=null`
- Confirm Product Details header shows Failed label (not Safe).

## 8. Edge Cases
- Failed status with non-null verdict from backend inconsistency: UI still prioritizes Failed.
- Completed status with null verdict: retains Unknown fallback behavior from prior fix where applicable.

## 9. Definition of Done
- [ ] Product Details shows Failed for failed scans.
- [ ] No safe fallback on failed scan details.
- [ ] No hardcoded strings introduced.
- [ ] Modified files compile cleanly.
