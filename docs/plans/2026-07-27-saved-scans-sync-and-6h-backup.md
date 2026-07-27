# Plan: Saved-scan backend sync + 6-hour daily-tracking backup

**Status:** approved via direct user Q&A (backend swagger confirmed endpoints), implementing directly.

## 1. Context

Two confirmed bugs from user report (uninstall/reinstall data loss):

1. **Saved scans never synced to backend.** `SavedScanRepositoryImpl` writes only
   to a local Room table (`saved_scans`), with no `userId` column (mixes across
   accounts on the same device) and no backend call at all. Scan *history*
   survives reinstall (real backend-backed), but the "Saved" bookmark flag
   doesn't. Backend already exposes what's needed (confirmed via
   `https://nutriscan.dev/v3/api-docs`):
   - `PATCH /api/v1/scans/{scanId}` body `{name, favorite}` → toggles favorite.
   - `GET /api/v1/scans/favorites` → paginated favorited scans.
2. **Water/steps backup was once-nightly, all-or-nothing.** User wants the
   in-progress data for *today* backed up every 6 hours, not just yesterday's
   finalized totals pushed once at Cairo midnight.

## 2. Saved-scan fix

- `ISavedScanRepository` gains `retryPendingSync(): Result<Unit>` for the
  periodic worker to retry failed pushes/deletes (mirrors `FoodLogRepositoryImpl`'s
  `pendingSync` pattern).
- `SavedScanEntity` gains `userId`, `pendingSync`, `deleted` columns (destructive
  migration already in effect for this table per `NutriScanDatabase` — version
  bump only, no explicit `Migration`).
- `SavedScanDao` gains userId-scoped queries + `getPendingSyncEntries` +
  `clearPendingSync` + `hardDelete` + `deleteStaleSynced` (removes local rows
  no longer present in the backend's favorites list, so un-favoriting on
  another device is reflected here — device-to-device dynamic sync).
- `SavedScanRepositoryImpl`: offline-first like `FoodLogRepositoryImpl` —
  `saveScan`/`deleteScan` write Room first (instant UI), then best-effort
  `PATCH favorite=true/false`; failure just flags `pendingSync = true` instead
  of failing the user action. `getSavedScans()` reconciles from
  `GET /scans/favorites` once per collection (seeds Room after reinstall,
  removes rows unfavorited elsewhere), then emits the Room flow.
- `ScanApiService` gains the two new Retrofit methods; new `UpdateScanDto`.

## 3. 6-hour daily-tracking backup

- `DailyTrackingSyncScheduler`: `PeriodicWorkRequestBuilder(6, HOURS)`, drop the
  Cairo-midnight-anchored initial delay (dead code once not needed).
- `DailyTrackingSyncWorker`/`DailyTrackingSyncEngine`: sync
  `CairoDateProvider.today()` instead of `.minusDays(1)` — `syncPendingDay`
  already only pushes when `syncedToBackend == false`, and every water/steps
  write already resets that flag, so pointing the periodic job at *today*
  gives incremental backup with no other logic change needed.
- Engine also calls `savedScanRepository.retryPendingSync()` each run — reuses
  this same periodic job instead of adding a second WorkManager job.

## 4. Files touched

Domain: `ISavedScanRepository.kt`.
Data: `SavedScanEntity.kt`, `SavedScanDao.kt`, `NutriScanDatabase.kt` (version
bump), `ScanApiService.kt`, new `UpdateScanDto.kt`, `SavedScanRepositoryImpl.kt`.
App: `DailyTrackingSyncEngine.kt`, `DailyTrackingSyncWorker.kt`,
`DailyTrackingSyncScheduler.kt`.
Tests: new `SavedScanRepositoryImplTest.kt`, update `DailyTrackingSyncEngineTest.kt`.

## 5. Known limitation (accepted, ponytail-flagged)

Favorites reconciliation fetches one page (`size = 200`) — a user with 200+
favorited scans won't get the rest mirrored locally. Add real pagination if
that ever becomes real usage; not worth it now.
