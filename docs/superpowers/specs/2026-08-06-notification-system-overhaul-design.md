# Notification System Overhaul — Design Spec

Date: 2026-08-06
Branch: `fix/linkCaloriesWithBackend` (or a new branch off it)
Supersedes scheduling decisions in `docs/superpowers/specs/2026-07-24-notification-system-design.md`

## 1. Purpose

The notification engine shipped in the 2026-07-24 spec works, but its scheduling
and data wiring are wrong in six ways that make it feel like spam and show
numbers that contradict the app. Separately, the step counter itself lags,
occasionally reports `0`, and sometimes only recovers after a process restart —
which corrupts the very number the notifications are meant to report.

This spec fixes both: the step counter first, then the notification scheduling
and data sources that depend on it.

## 2. Confirmed defects

### 2.1 Step counter (`StepsRepositoryImpl`)

| # | Defect | Location |
|---|--------|----------|
| S1 | Persisted daily total is **not date-keyed**. `ensureTracking` seeds `dailySteps` from `preferences.getDailySteps()`, which on a new day is yesterday's total and on a fresh install is `0`. That stale value is what the UI shows until a sensor event arrives. | `StepsPreferencesDataSourceImpl.kt:20`, `StepsRepositoryImpl.kt:67` |
| S2 | `registerListener(listener, sensor, SENSOR_DELAY_NORMAL, mainHandler)` leaves **hardware batching enabled**. `TYPE_STEP_COUNTER` only reports on change, and with batching the first event can be minutes late. | `StepsRepositoryImpl.kt:103-108` |
| S3 | `isTracking` is a latching `AtomicBoolean` that is **never reset on the failure paths**. If `getDefaultSensor` returns `null`, or the `scope.launch` throws before `registerListener`, the flow is pinned at `0`/`null` forever. Only a process restart clears it. | `StepsRepositoryImpl.kt:64-73` |
| S4 | **Day rollover is only handled inside `onSensorChanged`.** With no sensor event after midnight, the baseline never rolls and yesterday's count stands. | `StepsRepositoryImpl.kt:87-91` |

S1 + S2 together are the reported "lags / shows old count". S1 alone is the
"shows 0". S3 is the "needs a restart".

### 2.2 Notification system

| # | Defect | Location |
|---|--------|----------|
| N1 | `NotificationScheduler.scheduleAll` runs unconditionally in `Application.onCreate` — notifications fire while logged out. No worker checks auth either. | `NutriScanApplication.kt:39` |
| N2 | `PeriodicWorkRequestBuilder` is built with **no `setInitialDelay`**, so WorkManager runs the first execution almost immediately. All nine works are enqueued in the same instant, so all nine fire together on first launch. | `NotificationScheduler.kt:40` |
| N3 | Water and Break are both literally `2, TimeUnit.HOURS` from the same start instant — they fire together, twelve times a day. | `NotificationScheduler.kt:24,31` |
| N4 | `WaterNotificationWorker` reads `IWaterRepository` (`water_log` table). Nothing writes to that table anymore — the app reads and writes `daily_tracking.waterCnt`/`targetWaterCnt`. The worker therefore reports `0/8` permanently. | `WaterNotificationWorker.kt:31`, `CaloriesViewModel.kt:86-87` |
| N5 | `StepsNotificationWorker` reads the sensor singleton; the app reads `daily_tracking.stepsCnt`. Two sources, no reconciliation. Goal is a hardcoded `10000` in the worker. | `StepsNotificationWorker.kt:31,47` |
| N6 | `DebugNotificationReceiver` is declared in `app/src/main/AndroidManifest.xml` with `android:exported="true"`, so it ships in release builds. It is runtime-guarded by `BuildConfig.DEBUG`, but the exported component is still present, and `sendAllTypesNow()` bypasses both per-type prefs and quiet hours. | `AndroidManifest.xml:44-50` |

**Not a defect:** quiet hours. `IsWithinQuietHoursUseCase` handles the overnight
window correctly, the default is enabled at 22:00–07:00, and every one of the
nine workers checks it. The 3–6 AM bursts in the reported screenshots fall
inside that window, so they were almost certainly produced by
`DebugNotificationReceiver`'s `sendAllTypesNow()`, which skips the check. N2's
fresh-install burst is real and independent, but would have been muted at that
hour.

## 3. Design

### 3.1 Phase 1 — Step counter accuracy

`IStepsPreferencesDataSource` gains a date alongside the daily total:

```kotlin
suspend fun getDailySteps(date: String): Int   // 0 if the stored date != date
suspend fun saveDailySteps(date: String, steps: Int)
```

Implementation stores `steps_daily_date` next to `steps_daily_total` and
returns `0` when they disagree. Fixes S1.

`StepsRepositoryImpl.ensureTracking` changes in four ways:

1. Seeds with `preferences.getDailySteps(CairoDateProvider.today().toString())`,
   so a new day starts at `0` rather than yesterday's total. (S1)
2. Registers with the batching-disabled overload —
   `registerListener(listener, sensor, SENSOR_DELAY_NORMAL, 0, mainHandler)`,
   where the fourth argument is `maxReportLatencyUs = 0`. Events are then
   delivered as the hardware detects them. (S2)
3. Wraps the `scope.launch` body in `try`/`catch`, and resets
   `isTracking.set(false)` on both the `sensor == null` path and the catch, so
   a later `observeTodaySteps()` collection retries instead of being pinned
   forever. (S3)
4. Performs the baseline-rollover check at seed time, not only inside
   `onSensorChanged`: if `preferences.getBaselineDate() != today`, the stored
   baseline is stale and today's count seeds to `0` pending the next sensor
   event. (S4)

The listener stays registered for the process lifetime — that existing decision
is correct and unchanged.

**Non-goal:** a foreground service. `TYPE_STEP_COUNTER` is a low-power batched
hardware sensor and the existing 15-minute `StepsSyncWorker` is enough to keep
the persisted value fresh across process death.

### 3.2 Phase 2 — One source of truth for steps

`StepsSyncWorker` already wakes the process every 15 minutes to re-register the
sensor and settle a fresh reading. It gains one responsibility: after the
settle delay, persist the value via `UpdateStepsCntUseCase(steps)`.

After this, `daily_tracking.stepsCnt` is authoritative and refreshed every 15
minutes regardless of whether the Calories screen was ever opened. The app, the
notification worker, and the backend sync all read the same number.

`StepsNotificationWorker` then reads `ObserveTodayDailyTrackingUseCase().first().stepsCnt`
instead of the sensor flow. The `10000` goal stays a constant — no
user-configurable steps goal exists anywhere in the domain, backend, or
settings, and adding one is out of scope.

`CaloriesViewModel` keeps its live sensor collection for instant gauge feedback;
that is a UI-responsiveness concern and is unaffected.

### 3.3 Phase 3 — Water reads `daily_tracking`, dead water code deleted

`WaterNotificationWorker` reads `ObserveTodayDailyTrackingUseCase().first()` and
reports `waterCnt`/`targetWaterCnt`. `ShouldNotifyWaterUseCase` changes its
parameter from `WaterLog` to two `Int`s (`consumed`, `goal`) so it stays a pure,
unit-testable decision function without depending on the tracking model.

Deleted, in dependency order:

- `WaterNotificationWorker`'s `ObserveTodayWaterUseCase` dependency
- `domain/water/usecase/`: `ObserveTodayWaterUseCase`, `LogWaterGlassUseCase`,
  `UnlogWaterGlassUseCase`, `SetWaterGoalUseCase`
- `domain/water/repository/IWaterRepository`, `domain/water/model/WaterLog`
- `data/repository/WaterRepositoryImpl` and its Hilt binding in `RepositoryModule`
- `data/db/dao/WaterLogDao`, `data/db/entity/WaterLogEntity`, and their
  registration in `NutriScanDatabase`
- `data/src/test/.../WaterRepositoryImplTest`
- `domain/src/test/.../ShouldNotifyWaterUseCaseTest` is rewritten, not deleted

`NutriScanDatabase` bumps `version = 14` → `15`. Per the existing comment in
that file and established project convention, `fallbackToDestructiveMigration()`
in `DatabaseModule` covers the bump — no hand-written `Migration` object. This
clears local tables on upgrade, which is acceptable pre-release and consistent
with every prior bump.

### 3.4 Phase 4 — Auth gate

A domain interface, mirroring the existing `ITestNotificationSender`
domain-interface/app-implementation precedent:

```kotlin
// domain/notification/repository/INotificationScheduler.kt
interface INotificationScheduler {
    fun scheduleAll()
    fun cancelAll()
}
```

Implemented by `NotificationSchedulerImpl` in `:app` (the current
`NotificationScheduler` object becomes this class), bound in a Hilt module.

Wiring, three call sites plus a guard:

- `LoginWithEmailUseCase` and `SaveGoogleLoginTokensUseCase` call `scheduleAll()`
  on success. These are the two login paths.
- `LogoutUseCase` calls `cancelAll()`.
- `NutriScanApplication.onCreate` no longer schedules unconditionally. It
  launches a coroutine that calls `CheckIfUserIsLoggedInUseCase()` and schedules
  or cancels accordingly. This is required for the already-logged-in cold-start
  case, and `enqueueUniquePeriodicWork(..., KEEP, ...)` makes it idempotent.
- Every worker gains `if (!checkIfUserIsLoggedIn()) return Result.success()` as
  its first line. This is the safety net for work already queued before this
  change ships, and for token clears that bypass `LogoutUseCase` (e.g. a 401
  refresh failure).

`cancelAll()` is `WorkManager.cancelUniqueWork(name)` for every work name the
scheduler owns — including the per-slot Water and Break names introduced in
§3.5, so the two lists must stay derived from one shared declaration rather than
written out twice.

### 3.5 Phase 5 — Fixed clock-time schedule

`NotificationSchedulerImpl` replaces interval-based periodic work with
daily work anchored to a wall-clock time:

```kotlin
PeriodicWorkRequestBuilder<W>(1, TimeUnit.DAYS)
    .setInitialDelay(minutesUntil(slot, now), TimeUnit.MINUTES)
    .build()
```

A pure helper carries the arithmetic and is the unit-test seam:

```kotlin
// NotificationSlots.kt
fun minutesUntilNext(slot: LocalTime, now: LocalDateTime): Long
```

Returns the minutes from `now` to the next occurrence of `slot`, rolling to
tomorrow when the slot has already passed today. This is what makes the
fresh-install burst impossible: nothing is ever scheduled with a zero delay.

Timetable:

| Time | Type | Condition |
|------|------|-----------|
| 08:30 | Quote | Mon / Wed / Fri only |
| 09:00 | Water | under goal |
| 11:00 | Break | — |
| 13:00 | Water | under goal |
| 15:00 | Break | — |
| 17:00 | Water | under goal |
| 17:30 | News | Tue / Thu only |
| 18:30 | Workout | none logged today |
| 19:00 | Break | — |
| 19:30 | Food | nothing logged today |
| 20:30 | Steps | under 10 000 |
| 21:00 | Water | under goal |
| 21:15 | Scan | 3+ days since last scan |
| 21:30 | Streak | nothing logged today |

Water fires four times at exactly four-hour spacing. Break fires three times at
exactly four-hour spacing, offset two hours from water. They can never collide
again. Quiet hours (22:00–07:00) sit outside every slot, so the existing check
becomes a backstop rather than the primary defence.

Water and Break need multiple slots per day, so each gets one unique work name
per slot (`water_notification_worker_0900`, `_1300`, …), each a 24-hour
periodic work with its own initial delay.

Weekday-restricted types (Quote, News) are enqueued daily; the worker returns
`Result.success()` early when `LocalDate.now().dayOfWeek` is not in its allowed
set. WorkManager has no weekday scheduling primitive and this needs no extra
machinery.

**Food/Streak overlap:** both fire on "nothing logged today", 19:30 and 21:30.
`StreakNotificationWorker` additionally requires `streak.currentStreak > 0` —
there is no streak to preserve otherwise, and the copy ("keep your streak
alive") is nonsense at zero. A user with no streak gets the 19:30 Food nudge
only; a user with a live streak gets both, which is the case where the second
reminder actually has stakes. This needs no new persistence, unlike gating on
"did Food already fire today".

**Drift:** WorkManager is best-effort and Doze can delay execution. Each worker
returns early if `LocalTime.now()` is more than 90 minutes past its slot, so a
delayed job is skipped rather than delivered at a nonsensical hour. The slot
time is passed to the worker as `inputData`.

### 3.6 Phase 6 — Debug receiver containment

`DebugNotificationReceiver` and its `<receiver>` manifest entry move from
`app/src/main/` to `app/src/debug/`. The runtime `BuildConfig.DEBUG` check stays
as defence in depth. Release builds then have no exported notification-trigger
component at all.

## 4. Execution order

Phases are dependency-ordered and each leaves the build green.

1. Step counter accuracy (3.1) — everything downstream reads this number
2. Steps single source of truth (3.2) — requires 1 to be correct
3. Water source + dead-code deletion (3.3) — independent of 1–2
4. Auth gate (3.4) — must precede 5 so the new schedule never runs logged out
5. Fixed clock-time schedule (3.5) — requires 4
6. Debug receiver containment (3.6) — independent

## 5. Testing

Per root `AGENTS.md` §11, new and changed logic needs tests. No ViewModel
changes are in scope, so no `*ViewModelTest` updates are required.

| Unit | Test |
|------|------|
| `NotificationSlots.minutesUntilNext` | Slot later today; slot already passed (rolls to tomorrow); slot exactly now; midnight boundary. New `NotificationSlotsTest`. |
| `StepsPreferencesDataSourceImpl` | `getDailySteps` returns `0` when the stored date differs; returns the stored value when it matches. New `StepsPreferencesDataSourceImplTest` — no steps test exists anywhere today. |
| `ShouldNotifyWaterUseCase` | Rewritten for the `(consumed: Int, goal: Int)` signature — existing quiet-hours and enabled-flag cases preserved. |
| `ShouldNotifyStepsUseCase` | Unchanged signature, unchanged tests. |
| Worker auth guard | One test per worker is disproportionate. A single test asserting the guard on `WaterNotificationWorker` documents the pattern. |

`StepsRepositoryImpl` itself gets no unit test. It is a thin shell around
`SensorManager`, a `Handler`, and a real `Looper`; mocking all three costs more
than it proves, and the logic worth asserting (date-keyed seeding) lives in the
data source, which *is* tested. The sensor-registration and retry changes are
verified by hand on device instead — see §5.1.

`./gradlew test` must pass before the branch is considered done.

## 5.1 Manual device verification

The step-counter fixes cannot be fully asserted in unit tests. Before merge:

1. Fresh install, log in, walk ~50 steps with the app closed. Open Calories —
   the gauge must show the real count within a few seconds, not `0`.
2. Leave the app overnight (or change the device date). The count must read `0`
   the next morning, not yesterday's total.
3. On a device or emulator with no step-counter sensor, confirm the Calories
   screen does not hang and recovers on a later visit rather than pinning at `0`.

## 6. Strings

No new user-facing strings. Every notification title and body already exists in
`strings.xml` (English + Arabic) and its copy is unchanged. The water and steps
bodies already take count/goal format arguments — only the values passed in
change.

## 7. Out of scope

- A user-configurable steps goal (no domain, backend, or settings field exists)
- A foreground service for step counting
- Notification history / in-app notification centre
- Any change to notification copy, icons, channels, or the Notification
  Settings screen
- Backend push notifications — everything here is device-local WorkManager
