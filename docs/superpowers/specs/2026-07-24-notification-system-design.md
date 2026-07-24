# Notification System — Design Spec

Date: 2026-07-24
Branch: `feature/notifications`

## 1. Purpose

Give users timely, personalized reminders across 8 areas: steps (10k goal),
water intake, workouts, food logging, health news, health quotes, scan
reminders, and daily streak preservation — driving engagement without being
spammy. Includes a full Notification Settings screen (replacing the existing
`NotificationSettingsRoute` placeholder) and a "Today's Progress" dashboard
backed by Room, so users see saved progress dynamically.

## 2. Scope

Full engine, all 8 types, in this spec. Includes new minimal domain/data
layers for water, workout, and streak (repository + use cases + Room), since
none exist today. Does **not** include dedicated water/workout logging
screens — logging happens via quick-actions on notifications and the
progress dashboard only.

Existing reusable data: `IStepsRepository` (steps), `IFoodLogRepository`
(food/streak source), `IScanRepository` (scan inactivity), `INewsRepository`
(digest content).

## 3. Architecture

### 3.1 New domain packages

- `domain/water/repository/IWaterRepository.kt` — `getTodayWater(): Flow<WaterLog>`,
  `logGlass(): Unit`, `setGoal(glasses: Int)`
- `domain/workout/repository/IWorkoutRepository.kt` — `getTodayWorkout(): Flow<Boolean>`,
  `markWorkoutDone(): Unit`
- `domain/streak/repository/IStreakRepository.kt` — `getStreak(): Flow<StreakInfo>`,
  `recomputeStreak(): Unit` (called after any food log write)
- `domain/notification/repository/INotificationRepository.kt` — per-type
  enabled flags + quiet-hour start/end, as `Flow<NotificationPrefs>` +
  suspend setters
- Use cases per package mirror `ObserveTodayStepsUseCase` / `CheckStepsPermissionUseCase`
  shape: `ObserveTodayWaterUseCase`, `LogWaterGlassUseCase`,
  `ObserveWorkoutStatusUseCase`, `MarkWorkoutDoneUseCase`,
  `ObserveStreakUseCase`, `ObserveNotificationPrefsUseCase`,
  `SetNotificationPrefUseCase`, `SetQuietHoursUseCase`.
- Decision-logic use cases (unit-testable, called from Workers):
  `ShouldNotifyWaterUseCase`, `ShouldNotifyWorkoutUseCase`,
  `ShouldNotifyStepsUseCase`, `ShouldNotifyFoodUseCase`,
  `ShouldNotifyScanUseCase`, `ShouldNotifyStreakUseCase`. Each takes current
  domain state + prefs (quiet hours, enabled) and returns `Boolean`.

### 3.2 Data layer (Room)

New tables, added to existing app Room database (reuse the DB instance
already used by `FoodLogRepositoryImpl` / steps — do not create a second
`RoomDatabase`):

- `water_log`: `date TEXT PRIMARY KEY`, `glassCount INT`, `goalGlasses INT`
- `workout_log`: `date TEXT PRIMARY KEY`, `done INT` (Boolean)
- `streak`: single row, id fixed — `currentStreak INT`, `longestStreak INT`, `lastActiveDate TEXT`

Repositories follow `FoodLogRepositoryImpl`'s offline-first shape:
`runCatchingCancellable` + `@IoDispatcher`. `StreakRepositoryImpl.recomputeStreak()`
is invoked from `FoodLogRepositoryImpl` after a successful food log insert
(streak = consecutive days with ≥1 food log entry).

Notification prefs live in DataStore (not Room), mirroring
`StepsPreferencesDataSourceImpl` — `NotificationPreferencesDataSourceImpl`.

### 3.3 Scheduling

- `NotificationScheduler` (Hilt singleton in `app` module): one
  `CoroutineWorker` subclass per type (8 total), each registered via
  `WorkManager.enqueueUniquePeriodicWork` at app start / on prefs change.
- Each worker: reads prefs (enabled + quiet hours) via the relevant
  `ObserveNotificationPrefsUseCase`, calls its `ShouldNotifyXUseCase`, and if
  true, builds + posts the notification via `NutriScanNotificationBuilder`.
  Quiet-hours check is a single shared helper (`isWithinQuietHours(prefs)`),
  not duplicated per worker.
- Default cadence: steps 3×/day, water every 2h during waking hours, workout
  1×/day, food evening nudge, news 1×/day digest, quote 1×/day, scan after 3
  days idle, streak evening warning if at risk. All defaults live in one
  `NotificationDefaults` object, overridable later per-type (not in v1 UI
  beyond enable/disable + quiet hours).
- `NotificationChannels.createAll(context)`: creates 8
  `NotificationChannel`s (one per type, so users get OS-level per-channel
  control too) — called once from `Application.onCreate()`.
- Deep links: each notification's `PendingIntent` navigates into the app via
  existing `Route` sealed types (e.g. water notification → `HomeRoute`,
  scan notification → scan screen route).

### 3.4 Notification Settings UI

Replaces the `PlaceholderScreen` currently wired at
`NavGraph.kt:333` (`NotificationSettingsRoute`).

New `presentation/settings/notifications/` package, full MVI
(ViewModel + State + Event + Effect), matching `UserProfileViewModel` shape:

- **Toggles section**: 8 rows (steps/water/workout/food/news/quote/scan/streak),
  each a switch bound to `NotificationPrefs`, dispatches
  `NotificationSettingsEvent.ToggleType(type, enabled)`.
- **Quiet hours row**: start/end time pickers, default 22:00–07:00,
  `Event.SetQuietHours(start, end)`.
- **Today's Progress section**: live cards from Room via Flow —
  water (`x/goal glasses` + "+1 glass" quick-log button reusing
  `LogWaterGlassUseCase`), workout (done/not-done toggle), streak (current
  count), steps (reuse `StepsGaugeCard`). All update reactively — no manual
  refresh.
- Styling: `AppTheme.colors` / `AppTheme.typography` / `AppTheme.shapes`
  only, verified in both light and dark theme (Compose preview or emulator
  screenshot) before considered done — no hardcoded `Color(0xFF...)`.
- All new user-facing strings added to `strings.xml` (English) and
  `values-ar/strings.xml` (Arabic) before use in Compose, per CLAUDE.md
  §14.4.

## 4. Notification content (v1 copy)

Quotes: local bundled resource (~50–100 bilingual health/nutrition quotes),
random/round-robin pick, no backend dependency.

News: fixed daily digest copy ("Check today's health news"), not
new-article detection — avoids building article-diff tracking in v1.

## 5. Error handling

- Water/workout/streak repository failures: `runCatchingCancellable` returns
  a `Result`; Worker treats failure as "skip this cycle, don't crash", logs
  via existing error-handling convention (no silent swallow of a *thrown*
  exception — it's surfaced to whatever crash-reporting the app already
  uses; only the notification itself is skipped).
- Streak recompute failure must never silently reset streak to 0 — if
  recompute fails, keep last known `currentStreak` value untouched (fail
  closed on the display, not on the data).

## 6. Testing

- `NotificationSettingsViewModelTest`: initial state, each toggle
  Event→State, quiet-hours Event→State, quick-log Event→Effect, error paths.
- Repository impl tests for Water/Workout/Streak, mirroring existing
  `FoodLogRepositoryImpl` test conventions.
- Each `ShouldNotifyXUseCase` gets a standalone unit test (pure function,
  no Worker/Android dependency) covering: enabled+due, disabled, within
  quiet hours, goal already met (no notify).
- Workers themselves are not unit tested beyond a thin
  "calls the use case and posts if true" smoke check — logic lives in the
  use cases per §3.1.

## 7. Out of scope (explicitly deferred)

- Per-type custom cadence/time UI (v1 ships fixed defaults, only
  enable/disable + quiet hours are user-editable)
- Dedicated water/workout logging screens beyond quick-actions
- News new-article detection (digest only)
- Rich notification actions beyond water quick-log (e.g. inline workout
  duration entry)
