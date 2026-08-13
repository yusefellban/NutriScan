# Plan: Scans Query Params Alignment

## 1. Feature Summary
Align `/v1/scans` list query parameters with backend contract by supporting: `page`, `size`, `query`, `verdict`, `scanStatus`, `date`.

## 2. Files to Create
- docs/plans/2026-08-12-scans-query-params-alignment.md: Plan and verification notes.

## 3. Files to Modify
- data/src/main/kotlin/iti/grad/nutriscan/data/remote/api/ScanApiService.kt: add `query` and `scanStatus` query params to `getRecentScans`.
- domain/src/main/kotlin/iti/grad/nutriscan/domain/scan/repository/IScanRepository.kt: extend `getRecentScans` signature with optional `query` and `scanStatus`.
- domain/src/main/kotlin/iti/grad/nutriscan/domain/scan/usecase/GetRecentScansUseCase.kt: pass through optional `query` and `scanStatus`.
- data/src/main/kotlin/iti/grad/nutriscan/data/repository/ScanRepositoryImpl.kt: forward params to API and include them in filtering/caching condition.

## 4. Layer Breakdown
### Domain
- Repository contract supports `query` and `scanStatus` filters.
- Use case forwards the full filter set.

### Data
- Retrofit API method includes all backend query params.
- Repository passes filters unchanged and avoids using unfiltered cache when any filter is present.

### Presentation
- No UI behavior change required in this task.

## 5. Navigation Changes
- None.

## 6. Strings - MANDATORY (Zero Hardcoded Text)
- No new user-facing strings.

## 7. Testing Plan
- Compile-level verification for updated method signatures.
- Code-path verification that filters are forwarded through repository to API.

## 8. Edge Cases
- Any non-null filter should bypass unfiltered cache branch.
- Null filters should keep existing cache behavior for page 0.

## 9. Definition of Done
- [ ] API supports `query` and `scanStatus` query keys.
- [ ] Domain and repository signatures are aligned.
- [ ] Existing call sites continue to compile (via default args).
- [ ] No unrelated behavior regressions introduced.
