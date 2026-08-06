# Notification System Overhaul Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stop the notification engine from firing at install time, while logged out, or with numbers that contradict the app — and fix the step counter it reads from, which lags, reports `0`, and sometimes only recovers after a process restart.

**Architecture:** Six dependency-ordered phases. The step counter is fixed first (date-keyed persistence + batching-disabled sensor registration + failure-path retry), then `daily_tracking` becomes the single source of truth for both steps and water, then the dead `water_log` stack is deleted, then scheduling moves behind a domain-level `INotificationScheduler` gated on auth, and finally interval-based `PeriodicWorkRequest`s are replaced with 24-hour works anchored to fixed wall-clock slots.

**Tech Stack:** Kotlin, WorkManager, Room, DataStore Preferences, Hilt, JUnit 5 + MockK + kotlinx-coroutines-test.

**Spec:** `docs/superpowers/specs/2026-08-06-notification-system-overhaul-design.md`

## Global Constraints

- Branch is `fix/notifications`, based on `development`. Commit after every task.
- Layer boundaries are strict: `presentation → domain ← data`. `domain` has zero Android imports. `:app` may depend on all three.
- No AI co-author trailer on any commit (root `AGENTS.md` §17.3).
- No new user-facing strings. Every notification title/body already exists in `strings.xml` (English + Arabic) and its copy is unchanged — only the values passed as format arguments change.
- Day-boundary logic uses `CairoDateProvider.today()`, never `LocalDate.now()`. Time-of-day slot logic uses device-local `LocalTime`/`LocalDateTime`, because a slot means "9 AM where the user is".
- Tests are JUnit 5 (`org.junit.jupiter.api.Test`), assertions from `org.junit.jupiter.api.Assertions`, test names in backticks.
- `./gradlew test` must pass at the end of every task.

---

### Task 1: Date-key the persisted step total

Today's step total is persisted as a bare `Int` with no date. On a new day `ensureTracking` seeds the flow with yesterday's total; on a fresh process it seeds `0`. That stale seed is what the UI shows until the first sensor event lands. Adding the date makes a mismatch resolvable.

**Files:**
- Modify: `data/src/main/kotlin/iti/grad/nutriscan/data/local/datasource/IStepsPreferencesDataSource.kt`
- Modify: `data/src/main/kotlin/iti/grad/nutriscan/data/local/datasource/StepsPreferencesDataSourceImpl.kt`
- Test: `data/src/test/kotlin/iti/grad/nutriscan/data/local/datasource/StepsPreferencesDataSourceImplTest.kt` (create)

**Interfaces:**
- Consumes: nothing.
- Produces: `IStepsPreferencesDataSource.getDailySteps(date: String): Int` and `saveDailySteps(date: String, steps: Int)`. Task 2 is the only caller.

- [ ] **Step 1: Write the failing test**

Create `data/src/test/kotlin/iti/grad/nutriscan/data/local/datasource/StepsPreferencesDataSourceImplTest.kt`:

```kotlin
package iti.grad.nutriscan.data.local.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class StepsPreferencesDataSourceImplTest {

    @TempDir
    lateinit var tempDir: File

    private fun dataSource(scope: TestScope): StepsPreferencesDataSourceImpl {
        val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = TestScope(StandardTestDispatcher(scope.testScheduler)),
            produceFile = { File(tempDir, "steps_test.preferences_pb") },
        )
        return StepsPreferencesDataSourceImpl(store)
    }

    @Test
    fun `returns the saved total when the date matches`() = runTest {
        val dataSource = dataSource(this)

        dataSource.saveDailySteps(date = "2026-08-06", steps = 4200)

        assertEquals(4200, dataSource.getDailySteps("2026-08-06"))
    }

    @Test
    fun `returns zero when the saved total belongs to another day`() = runTest {
        val dataSource = dataSource(this)

        dataSource.saveDailySteps(date = "2026-08-05", steps = 4200)

        assertEquals(0, dataSource.getDailySteps("2026-08-06"))
    }

    @Test
    fun `returns zero when nothing has ever been saved`() = runTest {
        val dataSource = dataSource(this)

        assertEquals(0, dataSource.getDailySteps("2026-08-06"))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :data:test --tests "*StepsPreferencesDataSourceImplTest*"`
Expected: FAIL to compile — `getDailySteps` currently takes no arguments and `saveDailySteps` takes only `steps`.

- [ ] **Step 3: Update the interface**

In `IStepsPreferencesDataSource.kt`, replace the two daily-steps declarations:

```kotlin
    /** Last known step total for [date] (ISO-8601). Returns 0 when the persisted total belongs
     * to a different day — a stale total is worse than no total, since it is shown to the user
     * before the first sensor event of the day arrives. */
    suspend fun getDailySteps(date: String): Int

    suspend fun saveDailySteps(date: String, steps: Int)
```

- [ ] **Step 4: Update the implementation**

In `StepsPreferencesDataSourceImpl.kt`, add the date key to `PreferencesKeys`:

```kotlin
        val DAILY_DATE = stringPreferencesKey("steps_daily_date")
```

and replace the two daily-steps functions:

```kotlin
    override suspend fun getDailySteps(date: String): Int {
        return dataStore.data.map { preferences ->
            if (preferences[PreferencesKeys.DAILY_DATE] == date) {
                preferences[PreferencesKeys.DAILY_STEPS] ?: 0
            } else {
                0
            }
        }.first()
    }

    override suspend fun saveDailySteps(date: String, steps: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_DATE] = date
            preferences[PreferencesKeys.DAILY_STEPS] = steps
        }
    }
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :data:test --tests "*StepsPreferencesDataSourceImplTest*"`
Expected: PASS (3 tests). `StepsRepositoryImpl` will not compile yet — that is Task 2.

- [ ] **Step 6: Commit**

```bash
git add data/src/main/kotlin/iti/grad/nutriscan/data/local/datasource/IStepsPreferencesDataSource.kt \
        data/src/main/kotlin/iti/grad/nutriscan/data/local/datasource/StepsPreferencesDataSourceImpl.kt \
        data/src/test/kotlin/iti/grad/nutriscan/data/local/datasource/StepsPreferencesDataSourceImplTest.kt
git commit -m "Key the persisted step total by date

Storing today's steps as a bare Int meant a new day started by showing
yesterday's number until the sensor got around to reporting. The date
sits next to the total now, and a mismatch reads as zero."
```

---

### Task 2: Make the step counter real-time and self-healing

Four defects in one file: stale seed (S1), hardware batching left enabled so the first event can be minutes late (S2), a latching `isTracking` flag that pins the flow at `0` forever if registration fails (S3), and day-rollover handled only inside the sensor callback (S4).

**Files:**
- Modify: `data/src/main/kotlin/iti/grad/nutriscan/data/repository/StepsRepositoryImpl.kt`

**Interfaces:**
- Consumes: `IStepsPreferencesDataSource.getDailySteps(date)` / `saveDailySteps(date, steps)` from Task 1.
- Produces: no signature change. `observeTodaySteps(): Flow<Int>` behaves as before, but emits a correct value promptly and retries after a failed start.

- [ ] **Step 1: Replace `ensureTracking` and add `startTracking`**

In `StepsRepositoryImpl.kt`, replace the whole `ensureTracking` function with:

```kotlin
    private fun ensureTracking() {
        if (!isTracking.compareAndSet(false, true)) return

        scope.launch {
            val started = runCatchingCancellable { startTracking() }
                .onFailure { Timber.e(it, "Step tracking failed to start") }
                .getOrDefault(false)
            // Releasing the latch on failure is what lets a later collection retry. Without
            // it a single failed start pinned the flow at 0 until the process was killed.
            if (!started) isTracking.set(false)
        }
    }

    /** Returns true once the sensor listener is registered. False means the caller should
     * release [isTracking] so a later [observeTodaySteps] collection can try again. */
    private suspend fun startTracking(): Boolean {
        val today = CairoDateProvider.today().toString()
        var baselineDate = preferences.getBaselineDate()
        var baselineSteps = preferences.getBaselineSteps()

        // A baseline captured on an earlier day makes every persisted total stale, so today
        // starts at 0 until the next sensor event re-baselines. Doing this here and not only
        // in onSensorChanged means a day with no sensor event still reads 0 rather than
        // yesterday's total.
        dailySteps.value = if (baselineDate == today) preferences.getDailySteps(today) else 0

        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (sensor == null) {
            dailySteps.value = 0
            return false
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val eventDay = CairoDateProvider.today().toString()
                val cumulative = event.values[0]

                // New day, first run, or the device rebooted since the baseline was
                // captured (the sensor resets to 0 on reboot) — re-baseline to now.
                if (baselineDate != eventDay || cumulative < baselineSteps) {
                    baselineDate = eventDay
                    baselineSteps = cumulative
                    scope.launch { preferences.saveBaseline(date = eventDay, steps = cumulative) }
                }

                val newDailySteps = (cumulative - baselineSteps).toInt().coerceAtLeast(0)
                if (newDailySteps != dailySteps.value) {
                    dailySteps.value = newDailySteps
                    scope.launch { preferences.saveDailySteps(date = eventDay, steps = newDailySteps) }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        // maxReportLatencyUs = 0 disables hardware batching. With batching on, the first
        // event after registration could be minutes late, which is what made the count
        // appear frozen or zero right after opening the app.
        sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL,
            0,
            mainHandler
        )
        return true
    }
```

- [ ] **Step 2: Fix the imports**

Add to the import block at the top of the file:

```kotlin
import iti.grad.nutriscan.domain.common.runCatchingCancellable
import timber.log.Timber
```

`CairoDateProvider` is already imported. Nothing is removed.

- [ ] **Step 3: Update the class KDoc**

Replace the sentence "The listener is intentionally never unregistered — it stays live for the process lifetime so the count keeps updating even while the Calories screen isn't visible, and `TYPE_STEP_COUNTER` is a low-power, hardware-batched sensor that's cheap to keep registered." with:

```
 * The listener is intentionally never unregistered — it stays live for the process lifetime so
 * the count keeps updating even while the Calories screen isn't visible, and `TYPE_STEP_COUNTER`
 * is a low-power sensor that's cheap to keep registered. It is registered with
 * `maxReportLatencyUs = 0` so the hardware reports each change immediately rather than batching.
```

- [ ] **Step 4: Compile**

Run: `./gradlew :data:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Run the data module tests**

Run: `./gradlew :data:test`
Expected: PASS. `StepsRepositoryImpl` gets no unit test of its own — it is a shell around `SensorManager`, a `Handler`, and a real `Looper`, and the logic worth asserting (date-keyed seeding) is covered by Task 1. The sensor behaviour is verified by hand in Task 13.

- [ ] **Step 6: Commit**

```bash
git add data/src/main/kotlin/iti/grad/nutriscan/data/repository/StepsRepositoryImpl.kt
git commit -m "Make the step counter report in real time and recover from a failed start

Three things were wrong. The flow was seeded from a persisted total that
could belong to yesterday. The listener was registered with batching left
on, so the first reading after opening the app could be minutes late. And
the tracking latch was never released, so one failed registration pinned
the count at zero until the process died — which is why restarting the app
'fixed' it.

Rollover is also checked when tracking starts, not only inside the sensor
callback, so a day with no steps yet reads 0 instead of yesterday's total."
```

---

### Task 3: Persist the sensor reading into `daily_tracking`

`StepsSyncWorker` already wakes every 15 minutes to settle a fresh sensor reading, but only persists it into the steps DataStore. The app's Calories screen and the backend sync read `daily_tracking.stepsCnt`, which is only written while the Calories screen is open. Writing it here makes one table authoritative.

**Files:**
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/steps/StepsSyncWorker.kt`

**Interfaces:**
- Consumes: `UpdateStepsCntUseCase.invoke(stepsCnt: Int): Result<Unit>` (exists at `domain/dailytracking/usecase/UpdateStepsCntUseCase.kt`).
- Produces: `daily_tracking.stepsCnt` refreshed at most 15 minutes stale, regardless of whether any screen was opened. Tasks 4 and 5 rely on this.

- [ ] **Step 1: Add the use case dependency**

In `StepsSyncWorker.kt`, add the import:

```kotlin
import iti.grad.nutriscan.domain.dailytracking.usecase.UpdateStepsCntUseCase
```

and add the constructor parameter after `observeTodaySteps`:

```kotlin
    private val updateStepsCnt: UpdateStepsCntUseCase,
```

- [ ] **Step 2: Persist the settled reading**

Replace the body of `doWork` with:

```kotlin
    override suspend fun doWork(): Result {
        if (!checkStepsPermission()) return Result.success()

        val steps = observeTodaySteps()
        steps.first() // triggers sensor listener (re-)registration for this process
        delay(SENSOR_SETTLE_DELAY_MS) // give the hardware a moment to report a fresh cumulative value
        // Mirror into daily_tracking so the Calories screen, the notification workers, and the
        // backend sync all read the same number even if no screen was ever opened today.
        updateStepsCnt(steps.first())
        return Result.success()
    }
```

- [ ] **Step 3: Update the class KDoc**

Append to the existing KDoc block, before the closing `*/`:

```
 * It also mirrors the settled reading into `daily_tracking` via [UpdateStepsCntUseCase], which is
 * what the Calories screen and the notification workers read.
```

- [ ] **Step 4: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/iti/grad/nutriscan/steps/StepsSyncWorker.kt
git commit -m "Mirror the synced step reading into daily_tracking

daily_tracking.stepsCnt was only written while the Calories screen was
open, so anything else reading it — the notification worker, the backend
sync — saw zero for users who never opened that tab. The 15-minute sync
worker already has a fresh reading in hand; it writes it now."
```

---

### Task 4: Point the steps notification at `daily_tracking`

The worker reads the sensor singleton while the app reads `daily_tracking.stepsCnt`. After Task 3 the table is authoritative, so the worker should read it too.

**Files:**
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/notification/worker/StepsNotificationWorker.kt`

**Interfaces:**
- Consumes: `ObserveTodayDailyTrackingUseCase.invoke(): Flow<DailyTracking>`; `DailyTracking.stepsCnt: Int`.
- Produces: nothing new.

- [ ] **Step 1: Swap the data source**

In `StepsNotificationWorker.kt`, replace the import

```kotlin
import iti.grad.nutriscan.domain.steps.usecase.ObserveTodayStepsUseCase
```

with

```kotlin
import iti.grad.nutriscan.domain.dailytracking.usecase.ObserveTodayDailyTrackingUseCase
```

replace the constructor parameter

```kotlin
    private val observeTodaySteps: ObserveTodayStepsUseCase,
```

with

```kotlin
    private val observeTodayDailyTracking: ObserveTodayDailyTrackingUseCase,
```

and replace the line

```kotlin
        val steps = observeTodaySteps().first()
```

with

```kotlin
        // daily_tracking is the shared source of truth — the Calories screen shows this exact
        // number, and StepsSyncWorker keeps it fresh every 15 minutes.
        val steps = observeTodayDailyTracking().first().stepsCnt
```

- [ ] **Step 2: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/iti/grad/nutriscan/notification/worker/StepsNotificationWorker.kt
git commit -m "Read the steps notification count from daily_tracking

The notification was reading the sensor singleton while the screen read
daily_tracking, so the two disagreed — the notification said 0 steps for
someone the app showed as having walked."
```

---

### Task 5: Point the water notification at `daily_tracking`

`WaterNotificationWorker` reads `IWaterRepository`, backed by the `water_log` table that nothing writes to anymore. It reports `0/8` permanently. `ShouldNotifyWaterUseCase` takes a `WaterLog`, a model that Task 6 deletes, so its signature changes to two plain `Int`s.

**Files:**
- Modify: `domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase/ShouldNotifyWaterUseCase.kt`
- Modify: `domain/src/test/kotlin/iti/grad/nutriscan/domain/notification/usecase/ShouldNotifyWaterUseCaseTest.kt`
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/notification/worker/WaterNotificationWorker.kt`

**Interfaces:**
- Consumes: `ObserveTodayDailyTrackingUseCase`; `DailyTracking.waterCnt: Int`, `DailyTracking.targetWaterCnt: Int`.
- Produces: `ShouldNotifyWaterUseCase.invoke(prefs: NotificationPrefs, consumed: Int, goal: Int, now: LocalTime): Boolean`.

- [ ] **Step 1: Rewrite the test**

Replace the entire contents of `ShouldNotifyWaterUseCaseTest.kt`:

```kotlin
package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalTime

class ShouldNotifyWaterUseCaseTest {

    private val quietHours = IsWithinQuietHoursUseCase()
    private val useCase = ShouldNotifyWaterUseCase(quietHours)
    private val enabledPrefs = NotificationPrefs.default()
    private val disabledPrefs = enabledPrefs.copy(
        enabled = enabledPrefs.enabled + (NotificationType.WATER to false)
    )

    @Test
    fun `notifies when behind goal, enabled, and outside quiet hours`() {
        assertTrue(useCase(enabledPrefs, consumed = 3, goal = 8, now = LocalTime.of(14, 0)))
    }

    @Test
    fun `does not notify when goal already met`() {
        assertFalse(useCase(enabledPrefs, consumed = 8, goal = 8, now = LocalTime.of(14, 0)))
    }

    @Test
    fun `does not notify when goal exceeded`() {
        assertFalse(useCase(enabledPrefs, consumed = 9, goal = 8, now = LocalTime.of(14, 0)))
    }

    @Test
    fun `does not notify when type disabled`() {
        assertFalse(useCase(disabledPrefs, consumed = 3, goal = 8, now = LocalTime.of(14, 0)))
    }

    @Test
    fun `does not notify during quiet hours`() {
        assertFalse(useCase(enabledPrefs, consumed = 3, goal = 8, now = LocalTime.of(23, 0)))
    }

    @Test
    fun `does not notify when no goal is set`() {
        assertFalse(useCase(enabledPrefs, consumed = 0, goal = 0, now = LocalTime.of(14, 0)))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :domain:test --tests "*ShouldNotifyWaterUseCaseTest*"`
Expected: FAIL to compile — the use case still takes a `WaterLog`.

- [ ] **Step 3: Change the use case signature**

Replace the entire contents of `ShouldNotifyWaterUseCase.kt`:

```kotlin
package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import java.time.LocalTime
import javax.inject.Inject

class ShouldNotifyWaterUseCase @Inject constructor(
    private val isWithinQuietHours: IsWithinQuietHoursUseCase
) {
    /** [goal] of 0 means the user has no water target yet, which reads as "nothing to fall
     * behind on" — no nudge. */
    operator fun invoke(prefs: NotificationPrefs, consumed: Int, goal: Int, now: LocalTime): Boolean {
        if (!prefs.isEnabled(NotificationType.WATER)) return false
        if (isWithinQuietHours(prefs, now)) return false
        return consumed < goal
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :domain:test --tests "*ShouldNotifyWaterUseCaseTest*"`
Expected: PASS (6 tests).

- [ ] **Step 5: Update the worker**

In `WaterNotificationWorker.kt`, replace the import

```kotlin
import iti.grad.nutriscan.domain.water.usecase.ObserveTodayWaterUseCase
```

with

```kotlin
import iti.grad.nutriscan.domain.dailytracking.usecase.ObserveTodayDailyTrackingUseCase
```

replace the constructor parameter

```kotlin
    private val observeTodayWater: ObserveTodayWaterUseCase,
```

with

```kotlin
    private val observeTodayDailyTracking: ObserveTodayDailyTrackingUseCase,
```

and replace these lines

```kotlin
        val water = observeTodayWater().first()
        if (!shouldNotifyWater(prefs, water, LocalTime.now())) return Result.success()

        val title = applicationContext.getString(R.string.notification_push_water_title)
        val body = applicationContext.getString(
            R.string.notification_push_water_body,
            water.glassCount,
            water.goalGlasses,
        )
```

with

```kotlin
        // daily_tracking is what the Calories screen shows. The old water_log table this used
        // to read was orphaned and always reported 0/8.
        val tracking = observeTodayDailyTracking().first()
        if (!shouldNotifyWater(prefs, tracking.waterCnt, tracking.targetWaterCnt, LocalTime.now())) {
            return Result.success()
        }

        val title = applicationContext.getString(R.string.notification_push_water_title)
        val body = applicationContext.getString(
            R.string.notification_push_water_body,
            tracking.waterCnt,
            tracking.targetWaterCnt,
        )
```

- [ ] **Step 6: Compile and test**

Run: `./gradlew :app:compileDebugKotlin :domain:test`
Expected: BUILD SUCCESSFUL, tests PASS.

- [ ] **Step 7: Commit**

```bash
git add domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase/ShouldNotifyWaterUseCase.kt \
        domain/src/test/kotlin/iti/grad/nutriscan/domain/notification/usecase/ShouldNotifyWaterUseCaseTest.kt \
        app/src/main/kotlin/iti/grad/nutriscan/notification/worker/WaterNotificationWorker.kt
git commit -m "Read the water notification count from daily_tracking

The worker was reading water_log, a table nothing has written to since
water moved into daily_tracking. That is why every hydration reminder
said 0/8 no matter how much the user had actually logged."
```

---

### Task 6: Delete the orphaned water stack

`water_log`, `IWaterRepository`, its four use cases, the DAO and entity are now unreferenced. Leaving them is what let Task 5's bug exist in the first place.

**Files:**
- Delete: `domain/src/main/kotlin/iti/grad/nutriscan/domain/water/` (whole package: `model/WaterLog.kt`, `repository/IWaterRepository.kt`, `usecase/ObserveTodayWaterUseCase.kt`, `usecase/LogWaterGlassUseCase.kt`, `usecase/UnlogWaterGlassUseCase.kt`, `usecase/SetWaterGoalUseCase.kt`)
- Delete: `data/src/main/kotlin/iti/grad/nutriscan/data/repository/WaterRepositoryImpl.kt`
- Delete: `data/src/test/kotlin/iti/grad/nutriscan/data/repository/WaterRepositoryImplTest.kt`
- Delete: `data/src/main/kotlin/iti/grad/nutriscan/data/db/dao/WaterLogDao.kt`
- Delete: `data/src/main/kotlin/iti/grad/nutriscan/data/db/entity/WaterLogEntity.kt`
- Modify: `data/src/main/kotlin/iti/grad/nutriscan/data/db/NutriScanDatabase.kt`
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/di/RepositoryModule.kt`
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/di/DatabaseModule.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: nothing. Pure deletion.

- [ ] **Step 1: Confirm nothing else references the package**

Run: `grep -rn "domain.water\|WaterLogDao\|WaterLogEntity" app/src data/src domain/src presentation/src --include=*.kt`
Expected: only the files listed above. `presentation` has zero references — its `waterGoal`/`waterConsumed` are plain `Int`s on `CaloriesState`, unrelated. If anything else appears, stop and report it before deleting.

- [ ] **Step 2: Delete the files**

```bash
git rm -r domain/src/main/kotlin/iti/grad/nutriscan/domain/water
git rm data/src/main/kotlin/iti/grad/nutriscan/data/repository/WaterRepositoryImpl.kt \
       data/src/test/kotlin/iti/grad/nutriscan/data/repository/WaterRepositoryImplTest.kt \
       data/src/main/kotlin/iti/grad/nutriscan/data/db/dao/WaterLogDao.kt \
       data/src/main/kotlin/iti/grad/nutriscan/data/db/entity/WaterLogEntity.kt
```

- [ ] **Step 3: Remove the Hilt bindings**

In `app/src/main/kotlin/iti/grad/nutriscan/di/RepositoryModule.kt`, delete these two imports:

```kotlin
import iti.grad.nutriscan.domain.water.repository.IWaterRepository
import iti.grad.nutriscan.data.repository.WaterRepositoryImpl
```

and delete this binding (around line 126-130):

```kotlin
    @Binds
    @Singleton
    abstract fun bindWaterRepository(
        impl: WaterRepositoryImpl
    ): IWaterRepository
```

In `app/src/main/kotlin/iti/grad/nutriscan/di/DatabaseModule.kt`, delete the provider (around line 54):

```kotlin
    fun provideWaterLogDao(db: NutriScanDatabase) = db.waterLogDao()
```

along with its `@Provides` annotation line directly above it, and the now-unused `WaterLogDao` import if one is present.

- [ ] **Step 4: Drop the table from the schema**

In `data/src/main/kotlin/iti/grad/nutriscan/data/db/NutriScanDatabase.kt`:

- delete the import `import iti.grad.nutriscan.data.db.entity.WaterLogEntity`
- delete the import `import iti.grad.nutriscan.data.db.dao.WaterLogDao`
- delete `WaterLogEntity::class,` from the `entities` list
- delete `abstract fun waterLogDao(): WaterLogDao` from the class body
- bump the version and extend the comment:

```kotlin
    // v3 -> v4: MIGRATION_3_4 (water_log/workout_log/streak). v4 -> v5: MIGRATION_4_5
    // (users.bmi/tdee). Everything after (family_members, exercises, saved_scan,
    // daily_tracking, FoodLogEntity's pendingSync/deleted/mealCnt/backendCreated columns,
    // StreakEntity's re-key from a single global row to per-userId,
    // SavedScanEntity's userId/pendingSync/deleted columns, and the v14 -> v15 removal of
    // water_log now that water lives in daily_tracking) relies on
    // fallbackToDestructiveMigration() in DatabaseModule — this clears all local tables on
    // upgrade.
    version = 15,
```

Leave `MIGRATION_3_4` registered in `DatabaseModule` — it still applies to installs coming from v3.

- [ ] **Step 5: Build and test**

Run: `./gradlew :domain:test :data:test :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL, all tests PASS.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "Delete the orphaned water_log stack

Nothing had written to water_log since water moved into daily_tracking —
the only remaining reader was the notification worker fixed in the previous
commit, which is exactly how it went unnoticed. Repository, use cases, DAO,
entity and table all go.

DB bumps 14 -> 15 on the existing fallbackToDestructiveMigration(), same as
every bump since v5. Local tables are cleared on upgrade."
```

---

### Task 7: Put scheduling behind a domain interface

`NotificationScheduler` is an `:app` object called directly from `Application.onCreate`. To gate it on auth from domain use cases, it needs a domain-level interface — the same shape as the existing `ITestNotificationSender`.

**Files:**
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/repository/INotificationScheduler.kt`
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/notification/NotificationScheduler.kt` (object → injectable class)
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/di/RepositoryModule.kt`
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/NutriScanApplication.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `INotificationScheduler` with `fun scheduleAll()` and `fun cancelAll()`. Tasks 8 and 10 both depend on it.

- [ ] **Step 1: Create the domain interface**

Create `domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/repository/INotificationScheduler.kt`:

```kotlin
package iti.grad.nutriscan.domain.notification.repository

/** Device-local reminder scheduling. Implemented in `:app` over WorkManager, which domain
 * cannot see — same arrangement as [ITestNotificationSender]. */
interface INotificationScheduler {
    /** Idempotent: safe to call on every login and every cold start. */
    fun scheduleAll()

    fun cancelAll()
}
```

- [ ] **Step 2: Convert the scheduler to an injectable class**

Replace the entire contents of `app/src/main/kotlin/iti/grad/nutriscan/notification/NotificationScheduler.kt`. Keep the same file name; the class is renamed:

```kotlin
package iti.grad.nutriscan.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import iti.grad.nutriscan.domain.notification.repository.INotificationScheduler
import iti.grad.nutriscan.notification.worker.BreakNotificationWorker
import iti.grad.nutriscan.notification.worker.FoodNotificationWorker
import iti.grad.nutriscan.notification.worker.NewsNotificationWorker
import iti.grad.nutriscan.notification.worker.QuoteNotificationWorker
import iti.grad.nutriscan.notification.worker.ScanNotificationWorker
import iti.grad.nutriscan.notification.worker.StepsNotificationWorker
import iti.grad.nutriscan.notification.worker.StreakNotificationWorker
import iti.grad.nutriscan.notification.worker.WaterNotificationWorker
import iti.grad.nutriscan.notification.worker.WorkoutNotificationWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : INotificationScheduler {

    override fun scheduleAll() {
        val workManager = WorkManager.getInstance(context)
        schedule<StepsNotificationWorker>(workManager, StepsNotificationWorker.WORK_NAME, 8, TimeUnit.HOURS) // ~3x/day
        schedule<WaterNotificationWorker>(workManager, WaterNotificationWorker.WORK_NAME, 2, TimeUnit.HOURS)
        schedule<WorkoutNotificationWorker>(workManager, WorkoutNotificationWorker.WORK_NAME, 1, TimeUnit.DAYS)
        schedule<FoodNotificationWorker>(workManager, FoodNotificationWorker.WORK_NAME, 1, TimeUnit.DAYS)
        schedule<NewsNotificationWorker>(workManager, NewsNotificationWorker.WORK_NAME, 1, TimeUnit.DAYS)
        schedule<QuoteNotificationWorker>(workManager, QuoteNotificationWorker.WORK_NAME, 1, TimeUnit.DAYS)
        schedule<ScanNotificationWorker>(workManager, ScanNotificationWorker.WORK_NAME, 1, TimeUnit.DAYS)
        schedule<StreakNotificationWorker>(workManager, StreakNotificationWorker.WORK_NAME, 1, TimeUnit.DAYS)
        schedule<BreakNotificationWorker>(workManager, BreakNotificationWorker.WORK_NAME, 2, TimeUnit.HOURS)
    }

    override fun cancelAll() {
        val workManager = WorkManager.getInstance(context)
        LEGACY_WORK_NAMES.forEach(workManager::cancelUniqueWork)
    }

    private inline fun <reified W : ListenableWorker> schedule(
        workManager: WorkManager,
        workName: String,
        interval: Long,
        unit: TimeUnit,
    ) {
        val request = PeriodicWorkRequestBuilder<W>(interval, unit).build()
        workManager.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private companion object {
        /** The pre-slot work names. Task 10 replaces the schedule wholesale; these stay listed
         * so already-installed apps get their old works cancelled rather than orphaned. */
        val LEGACY_WORK_NAMES = listOf(
            StepsNotificationWorker.WORK_NAME,
            WaterNotificationWorker.WORK_NAME,
            WorkoutNotificationWorker.WORK_NAME,
            FoodNotificationWorker.WORK_NAME,
            NewsNotificationWorker.WORK_NAME,
            QuoteNotificationWorker.WORK_NAME,
            ScanNotificationWorker.WORK_NAME,
            StreakNotificationWorker.WORK_NAME,
            BreakNotificationWorker.WORK_NAME,
        )
    }
}
```

The scheduling bodies are still the old interval-based ones — Task 10 replaces them. This task only changes *who owns* the scheduler.

- [ ] **Step 3: Bind it**

In `app/src/main/kotlin/iti/grad/nutriscan/di/RepositoryModule.kt`, add the imports:

```kotlin
import iti.grad.nutriscan.domain.notification.repository.INotificationScheduler
import iti.grad.nutriscan.notification.NotificationSchedulerImpl
```

and add the binding next to the existing `ITestNotificationSender` one:

```kotlin
    @Binds
    @Singleton
    abstract fun bindNotificationScheduler(
        impl: NotificationSchedulerImpl
    ): INotificationScheduler
```

- [ ] **Step 4: Gate the cold-start call on auth**

In `NutriScanApplication.kt`, replace the import

```kotlin
import iti.grad.nutriscan.notification.NotificationScheduler
```

with

```kotlin
import iti.grad.nutriscan.domain.auth.usecase.CheckIfUserIsLoggedInUseCase
import iti.grad.nutriscan.domain.notification.repository.INotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
```

add the injections next to `workerFactory`:

```kotlin
    @Inject
    lateinit var notificationScheduler: INotificationScheduler

    @Inject
    lateinit var checkIfUserIsLoggedIn: CheckIfUserIsLoggedInUseCase

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
```

and replace the line `NotificationScheduler.scheduleAll(this)` in `onCreate` with:

```kotlin
        // Reminders belong to a logged-in user. Scheduling on every cold start (rather than only
        // at login) is what covers the already-signed-in case; enqueueUniquePeriodicWork with
        // KEEP makes the repeat harmless.
        applicationScope.launch {
            if (checkIfUserIsLoggedIn()) notificationScheduler.scheduleAll()
            else notificationScheduler.cancelAll()
        }
```

- [ ] **Step 5: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/repository/INotificationScheduler.kt \
        app/src/main/kotlin/iti/grad/nutriscan/notification/NotificationScheduler.kt \
        app/src/main/kotlin/iti/grad/nutriscan/di/RepositoryModule.kt \
        app/src/main/kotlin/iti/grad/nutriscan/NutriScanApplication.kt
git commit -m "Only schedule reminders for a logged-in user

Application.onCreate scheduled everything unconditionally, so a signed-out
device still got nudged about its water intake. Scheduling moves behind an
INotificationScheduler the domain can see, and the cold-start call now
checks whether anyone is actually signed in."
```

---

### Task 8: Schedule on login, cancel on logout, guard inside the workers

The cold-start gate from Task 7 handles an app that is already signed in. Login and logout also need to act immediately, and already-enqueued work from previous installs needs a guard that does not depend on either.

**Files:**
- Modify: `domain/src/main/kotlin/iti/grad/nutriscan/domain/auth/usecase/LoginWithEmailUseCase.kt`
- Modify: `domain/src/main/kotlin/iti/grad/nutriscan/domain/auth/usecase/SaveGoogleLoginTokensUseCase.kt`
- Modify: `domain/src/main/kotlin/iti/grad/nutriscan/domain/auth/usecase/LogoutUseCase.kt`
- Modify: `domain/src/test/kotlin/iti/grad/nutriscan/domain/auth/usecase/LoginWithEmailUseCaseTest.kt`
- Modify: all nine workers in `app/src/main/kotlin/iti/grad/nutriscan/notification/worker/`

**Interfaces:**
- Consumes: `INotificationScheduler` (Task 7); `CheckIfUserIsLoggedInUseCase.invoke(): Boolean`.
- Produces: nothing new.

- [ ] **Step 1: Schedule on email login**

Replace the contents of `LoginWithEmailUseCase.kt`:

```kotlin
package iti.grad.nutriscan.domain.auth.usecase

import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.notification.repository.INotificationScheduler
import javax.inject.Inject

class LoginWithEmailUseCase @Inject constructor(
    private val authRepository: IAuthRepository,
    private val notificationScheduler: INotificationScheduler,
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        return authRepository.loginWithEmail(email, password)
            .onSuccess { notificationScheduler.scheduleAll() }
    }
}
```

- [ ] **Step 2: Schedule on Google login**

Replace the contents of `SaveGoogleLoginTokensUseCase.kt`:

```kotlin
package iti.grad.nutriscan.domain.auth.usecase

import iti.grad.nutriscan.domain.auth.model.AuthTokens
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.notification.repository.INotificationScheduler
import javax.inject.Inject

class SaveGoogleLoginTokensUseCase @Inject constructor(
    private val authRepository: IAuthRepository,
    private val notificationScheduler: INotificationScheduler,
) {
    suspend operator fun invoke(authTokens: AuthTokens): Result<Unit> {
        return authRepository.saveTokens(authTokens)
            .onSuccess { notificationScheduler.scheduleAll() }
    }
}
```

- [ ] **Step 3: Cancel on logout**

Replace the contents of `LogoutUseCase.kt`:

```kotlin
package iti.grad.nutriscan.domain.auth.usecase

import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.notification.repository.INotificationScheduler
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: IAuthRepository,
    private val notificationScheduler: INotificationScheduler,
) {
    suspend operator fun invoke(): Result<Unit> {
        // Cancelled unconditionally: AuthRepositoryImpl.logout() clears tokens even when the
        // server call fails, so the user is signed out either way.
        notificationScheduler.cancelAll()
        return authRepository.logout()
    }
}
```

- [ ] **Step 4: Update the login use case test**

`domain/src/test/kotlin/iti/grad/nutriscan/domain/auth/usecase/LoginWithEmailUseCaseTest.kt` constructs the real use case, so it stops compiling. Update it and assert the new behaviour — this is the runnable check for the auth gate.

Replace the imports block and the `setup`/tests with:

```kotlin
package iti.grad.nutriscan.domain.auth.usecase

import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.notification.repository.INotificationScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LoginWithEmailUseCaseTest {

    private lateinit var authRepository: IAuthRepository
    private lateinit var notificationScheduler: INotificationScheduler
    private lateinit var useCase: LoginWithEmailUseCase

    @BeforeEach
    fun setup() {
        authRepository = mockk()
        notificationScheduler = mockk(relaxed = true)
        useCase = LoginWithEmailUseCase(authRepository, notificationScheduler)
    }

    @Test
    fun `invoke with valid credentials should return success`() = runTest {
        val email = "test@example.com"
        val password = "Password123"
        coEvery { authRepository.loginWithEmail(email, password) } returns Result.success(Unit)

        val result = useCase(email, password)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { authRepository.loginWithEmail(email, password) }
    }

    @Test
    fun `invoke with failure from repository should return failure`() = runTest {
        val email = "test@example.com"
        val password = "wrong_password"
        val exception = Exception("Invalid credentials")
        coEvery { authRepository.loginWithEmail(email, password) } returns Result.failure(exception)

        val result = useCase(email, password)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { authRepository.loginWithEmail(email, password) }
    }

    @Test
    fun `schedules notifications on successful login`() = runTest {
        coEvery { authRepository.loginWithEmail(any(), any()) } returns Result.success(Unit)

        useCase("test@example.com", "Password123")

        verify(exactly = 1) { notificationScheduler.scheduleAll() }
    }

    @Test
    fun `does not schedule notifications when login fails`() = runTest {
        coEvery { authRepository.loginWithEmail(any(), any()) } returns Result.failure(Exception("nope"))

        useCase("test@example.com", "wrong")

        verify(exactly = 0) { notificationScheduler.scheduleAll() }
    }
}
```

- [ ] **Step 4b: Confirm the ViewModel tests still pass**

`AppSettingsViewModelTest` mocks `LogoutUseCase` itself and `LoginViewModelTest` mocks `LoginWithEmailUseCase`, so neither constructs the real class and the constructor changes do not reach them.

Run: `./gradlew :presentation:test --tests "*AppSettingsViewModelTest*" --tests "*LoginViewModelTest*"`
Expected: PASS. If either fails because MockK cannot construct the class, add `relaxed = true` to that `mockk()` call.

- [ ] **Step 5: Add the auth guard to every worker**

For each of the nine files in `app/src/main/kotlin/iti/grad/nutriscan/notification/worker/` — `BreakNotificationWorker.kt`, `FoodNotificationWorker.kt`, `NewsNotificationWorker.kt`, `QuoteNotificationWorker.kt`, `ScanNotificationWorker.kt`, `StepsNotificationWorker.kt`, `StreakNotificationWorker.kt`, `WaterNotificationWorker.kt`, `WorkoutNotificationWorker.kt` — apply the same three edits.

Add the import:

```kotlin
import iti.grad.nutriscan.domain.auth.usecase.CheckIfUserIsLoggedInUseCase
```

Add the constructor parameter as the last one, after `historyRecorder`:

```kotlin
    private val checkIfUserIsLoggedIn: CheckIfUserIsLoggedInUseCase,
```

Add this as the very first line of `doWork()`, above the existing `val prefs = observePrefs().first()`:

```kotlin
        // Belt and braces: the scheduler cancels on logout, but work enqueued by an older
        // build — or a session ended by a failed token refresh — can still fire.
        if (!checkIfUserIsLoggedIn()) return Result.success()
```

- [ ] **Step 6: Compile and test**

Run: `./gradlew :app:compileDebugKotlin :domain:test :presentation:test`
Expected: BUILD SUCCESSFUL, tests PASS.

- [ ] **Step 7: Commit**

```bash
git add domain/src/main/kotlin/iti/grad/nutriscan/domain/auth/usecase/ \
        domain/src/test/kotlin/iti/grad/nutriscan/domain/auth/usecase/LoginWithEmailUseCaseTest.kt \
        app/src/main/kotlin/iti/grad/nutriscan/notification/worker/
git commit -m "Start and stop reminders with the session

Login schedules, logout cancels, and every worker checks for a session
before it posts anything. The worker-level check covers the cases the other
two miss: work enqueued by an older build, and a session dropped by a
failed token refresh rather than a real logout."
```

---

### Task 9: A slot-time helper

The fix for the fresh-install burst is that no work is ever enqueued with a zero initial delay. That arithmetic is the one genuinely testable piece of the new scheduler, so it lives on its own.

**Files:**
- Create: `app/src/main/kotlin/iti/grad/nutriscan/notification/NotificationSlots.kt`
- Test: `app/src/test/kotlin/iti/grad/nutriscan/notification/NotificationSlotsTest.kt` (create)

**Interfaces:**
- Consumes: nothing.
- Produces: `NotificationSlots.minutesUntilNext(slot: LocalTime, now: LocalDateTime): Long` and `NotificationSlots.isTooLate(slotMinuteOfDay: Int, now: LocalTime): Boolean`, plus `NotificationSlots.KEY_SLOT_MINUTE_OF_DAY`. Task 10 and Task 11 consume all three.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/kotlin/iti/grad/nutriscan/notification/NotificationSlotsTest.kt`:

```kotlin
package iti.grad.nutriscan.notification

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.LocalTime

class NotificationSlotsTest {

    @Test
    fun `counts forward to a slot later today`() {
        val now = LocalDateTime.of(2026, 8, 6, 9, 0)

        assertEquals(240, NotificationSlots.minutesUntilNext(LocalTime.of(13, 0), now))
    }

    @Test
    fun `rolls to tomorrow when the slot already passed today`() {
        val now = LocalDateTime.of(2026, 8, 6, 15, 0)

        assertEquals(1080, NotificationSlots.minutesUntilNext(LocalTime.of(9, 0), now))
    }

    @Test
    fun `never returns zero when the slot is exactly now`() {
        val now = LocalDateTime.of(2026, 8, 6, 9, 0)

        assertEquals(1440, NotificationSlots.minutesUntilNext(LocalTime.of(9, 0), now))
    }

    @Test
    fun `crosses midnight correctly`() {
        val now = LocalDateTime.of(2026, 8, 6, 23, 30)

        assertEquals(540, NotificationSlots.minutesUntilNext(LocalTime.of(8, 30), now))
    }

    @Test
    fun `is not too late when running on time`() {
        assertFalse(NotificationSlots.isTooLate(slotMinuteOfDay = 9 * 60, now = LocalTime.of(9, 5)))
    }

    @Test
    fun `is not too late when running early`() {
        assertFalse(NotificationSlots.isTooLate(slotMinuteOfDay = 9 * 60, now = LocalTime.of(8, 55)))
    }

    @Test
    fun `is too late beyond the grace window`() {
        assertTrue(NotificationSlots.isTooLate(slotMinuteOfDay = 9 * 60, now = LocalTime.of(11, 0)))
    }

    @Test
    fun `is never too late when no slot was supplied`() {
        assertFalse(NotificationSlots.isTooLate(slotMinuteOfDay = -1, now = LocalTime.of(23, 0)))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*NotificationSlotsTest*"`
Expected: FAIL to compile — `NotificationSlots` does not exist.

- [ ] **Step 3: Write the implementation**

Create `app/src/main/kotlin/iti/grad/nutriscan/notification/NotificationSlots.kt`:

```kotlin
package iti.grad.nutriscan.notification

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/** Wall-clock scheduling arithmetic for the reminder slots. Kept separate from the scheduler
 * so it can be tested without WorkManager. */
object NotificationSlots {

    /** Set on every slot's `inputData` so a worker can tell how late WorkManager ran it. */
    const val KEY_SLOT_MINUTE_OF_DAY = "slot_minute_of_day"

    private const val MAX_LATE_MINUTES = 90

    /**
     * Minutes from [now] to the next occurrence of [slot], rolling to tomorrow when the slot has
     * already passed. A slot falling exactly on [now] counts as tomorrow — returning 0 here is
     * what made a fresh install fire every reminder the moment it was enqueued.
     */
    fun minutesUntilNext(slot: LocalTime, now: LocalDateTime): Long {
        val todaysSlot = now.toLocalDate().atTime(slot)
        val next = if (todaysSlot.isAfter(now)) todaysSlot else todaysSlot.plusDays(1)
        return Duration.between(now, next).toMinutes()
    }

    /**
     * True when WorkManager ran a slot more than [MAX_LATE_MINUTES] past its time — Doze and
     * batching can defer work for hours, and a "time for a break" nudge is noise once the moment
     * has passed. A [slotMinuteOfDay] below zero means no slot was supplied, which never skips.
     */
    fun isTooLate(slotMinuteOfDay: Int, now: LocalTime): Boolean {
        if (slotMinuteOfDay < 0) return false
        return (now.toSecondOfDay() / 60) - slotMinuteOfDay > MAX_LATE_MINUTES
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*NotificationSlotsTest*"`
Expected: PASS (8 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/iti/grad/nutriscan/notification/NotificationSlots.kt \
        app/src/test/kotlin/iti/grad/nutriscan/notification/NotificationSlotsTest.kt
git commit -m "Add slot arithmetic for clock-anchored reminders

Two small pure functions: how long until a slot next comes round, and
whether WorkManager ran one so late it should be skipped. A slot landing
exactly on 'now' counts as tomorrow — a zero delay is what made a fresh
install fire everything at once."
```

---

### Task 10: Replace the interval schedule with fixed clock slots

Nine works enqueued in the same instant, two of them every two hours, none with an initial delay. Replaced with fourteen 24-hour works, each anchored to a wall-clock time, each with its own unique name.

**Files:**
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/notification/NotificationScheduler.kt`

**Interfaces:**
- Consumes: `NotificationSlots.minutesUntilNext`, `NotificationSlots.KEY_SLOT_MINUTE_OF_DAY` (Task 9); `INotificationScheduler` (Task 7).
- Produces: `inputData` carrying `KEY_SLOT_MINUTE_OF_DAY` on every scheduled work. Task 11 reads it.

- [ ] **Step 1: Rewrite the scheduler**

Replace the entire contents of `app/src/main/kotlin/iti/grad/nutriscan/notification/NotificationScheduler.kt`:

```kotlin
package iti.grad.nutriscan.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import iti.grad.nutriscan.domain.notification.repository.INotificationScheduler
import iti.grad.nutriscan.notification.worker.BreakNotificationWorker
import iti.grad.nutriscan.notification.worker.FoodNotificationWorker
import iti.grad.nutriscan.notification.worker.NewsNotificationWorker
import iti.grad.nutriscan.notification.worker.QuoteNotificationWorker
import iti.grad.nutriscan.notification.worker.ScanNotificationWorker
import iti.grad.nutriscan.notification.worker.StepsNotificationWorker
import iti.grad.nutriscan.notification.worker.StreakNotificationWorker
import iti.grad.nutriscan.notification.worker.WaterNotificationWorker
import iti.grad.nutriscan.notification.worker.WorkoutNotificationWorker
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Every reminder is a 24-hour periodic work anchored to a wall-clock slot via an initial delay,
 * not an interval counted from whenever the app happened to start. That is what stops a fresh
 * install from firing all of them at once, and what keeps Water and Break from colliding.
 *
 * Slots are device-local: "9 AM" means 9 AM where the user is. Day-boundary logic elsewhere
 * stays Cairo-anchored via CairoDateProvider — these are different questions.
 */
@Singleton
class NotificationSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : INotificationScheduler {

    override fun scheduleAll() {
        val workManager = WorkManager.getInstance(context)
        // Installs from before the slot schedule have works under the bare type names. They are
        // never re-enqueued, so cancel them or they keep firing on the old two-hour interval.
        LEGACY_WORK_NAMES.forEach(workManager::cancelUniqueWork)

        val now = LocalDateTime.now()
        SLOTS.forEach { slot ->
            val request = PeriodicWorkRequest.Builder(slot.worker, 1, TimeUnit.DAYS)
                .setInitialDelay(NotificationSlots.minutesUntilNext(slot.time, now), TimeUnit.MINUTES)
                .setInputData(
                    workDataOf(NotificationSlots.KEY_SLOT_MINUTE_OF_DAY to slot.time.toSecondOfDay() / 60)
                )
                .build()
            // KEEP, not UPDATE: re-anchoring on every cold start would push the next run forward
            // for anyone who opens the app daily, and they would never get a reminder at all.
            workManager.enqueueUniquePeriodicWork(slot.workName, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }

    override fun cancelAll() {
        val workManager = WorkManager.getInstance(context)
        (LEGACY_WORK_NAMES + SLOTS.map { it.workName }).forEach(workManager::cancelUniqueWork)
    }

    private data class Slot(
        val workName: String,
        val worker: Class<out ListenableWorker>,
        val time: LocalTime,
    )

    private companion object {

        private fun slot(
            worker: Class<out ListenableWorker>,
            baseName: String,
            hour: Int,
            minute: Int,
        ) = Slot(
            workName = "%s_%02d%02d".format(baseName, hour, minute),
            worker = worker,
            time = LocalTime.of(hour, minute),
        )

        /**
         * Water runs four times at exactly four-hour spacing; Break three times at four-hour
         * spacing offset two hours from Water, so the two can never land together. Everything
         * else is once a day and mostly conditional, so a typical day is 7-9 actual posts.
         * Quiet hours (22:00-07:00) sit outside every slot and are now a backstop, not the
         * main defence.
         */
        val SLOTS = listOf(
            slot(QuoteNotificationWorker::class.java, QuoteNotificationWorker.WORK_NAME, 8, 30),
            slot(WaterNotificationWorker::class.java, WaterNotificationWorker.WORK_NAME, 9, 0),
            slot(BreakNotificationWorker::class.java, BreakNotificationWorker.WORK_NAME, 11, 0),
            slot(WaterNotificationWorker::class.java, WaterNotificationWorker.WORK_NAME, 13, 0),
            slot(BreakNotificationWorker::class.java, BreakNotificationWorker.WORK_NAME, 15, 0),
            slot(WaterNotificationWorker::class.java, WaterNotificationWorker.WORK_NAME, 17, 0),
            slot(NewsNotificationWorker::class.java, NewsNotificationWorker.WORK_NAME, 17, 30),
            slot(WorkoutNotificationWorker::class.java, WorkoutNotificationWorker.WORK_NAME, 18, 30),
            slot(BreakNotificationWorker::class.java, BreakNotificationWorker.WORK_NAME, 19, 0),
            slot(FoodNotificationWorker::class.java, FoodNotificationWorker.WORK_NAME, 19, 30),
            slot(StepsNotificationWorker::class.java, StepsNotificationWorker.WORK_NAME, 20, 30),
            slot(WaterNotificationWorker::class.java, WaterNotificationWorker.WORK_NAME, 21, 0),
            slot(ScanNotificationWorker::class.java, ScanNotificationWorker.WORK_NAME, 21, 15),
            slot(StreakNotificationWorker::class.java, StreakNotificationWorker.WORK_NAME, 21, 30),
        )

        val LEGACY_WORK_NAMES = listOf(
            StepsNotificationWorker.WORK_NAME,
            WaterNotificationWorker.WORK_NAME,
            WorkoutNotificationWorker.WORK_NAME,
            FoodNotificationWorker.WORK_NAME,
            NewsNotificationWorker.WORK_NAME,
            QuoteNotificationWorker.WORK_NAME,
            ScanNotificationWorker.WORK_NAME,
            StreakNotificationWorker.WORK_NAME,
            BreakNotificationWorker.WORK_NAME,
        )
    }
}
```

- [ ] **Step 2: Compile and run the app tests**

Run: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, tests PASS.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/iti/grad/nutriscan/notification/NotificationScheduler.kt
git commit -m "Anchor every reminder to a wall-clock slot

Reminders were periodic works with no initial delay, all enqueued in the
same instant, two of them on a two-hour interval. So a fresh install fired
nine notifications at once and hydration and break reminders arrived
together twelve times a day.

Each slot is now its own 24-hour work with an initial delay to the next
occurrence of its time. Water lands at 09:00/13:00/17:00/21:00 and Break at
11:00/15:00/19:00 — four hours apart, two hours offset from each other.
Works from older installs get cancelled by name on the way through."
```

---

### Task 11: Per-slot conditions — weekdays, drift, and the streak overlap

Three refinements the new schedule needs: Quote and News are not daily, a slot delivered hours late is noise, and Food at 19:30 plus Streak at 21:30 say nearly the same thing to a user who has logged nothing.

**Files:**
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/notification/worker/QuoteNotificationWorker.kt`
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/notification/worker/NewsNotificationWorker.kt`
- Modify: all nine workers (drift guard)
- Modify: `domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase/ShouldNotifyStreakUseCase.kt`
- Modify: `domain/src/test/kotlin/iti/grad/nutriscan/domain/notification/usecase/ShouldNotifyStreakUseCaseTest.kt`
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/notification/worker/StreakNotificationWorker.kt`

**Interfaces:**
- Consumes: `NotificationSlots.isTooLate`, `NotificationSlots.KEY_SLOT_MINUTE_OF_DAY` (Task 9).
- Produces: `ShouldNotifyStreakUseCase.invoke(prefs, loggedFoodToday, hasStreak, now)`.

- [ ] **Step 1: Add the drift guard to every worker**

For each of the nine workers, add the import:

```kotlin
import iti.grad.nutriscan.notification.NotificationSlots
```

and add this immediately after the auth guard added in Task 8:

```kotlin
        // WorkManager is best-effort — Doze can defer a slot for hours, and a reminder for a
        // moment that has passed is just noise.
        if (NotificationSlots.isTooLate(
                inputData.getInt(NotificationSlots.KEY_SLOT_MINUTE_OF_DAY, -1),
                LocalTime.now(),
            )
        ) {
            return Result.success()
        }
```

`LocalTime` is already imported in every worker.

- [ ] **Step 2: Restrict Quote to Mon/Wed/Fri**

In `QuoteNotificationWorker.kt`, add the imports:

```kotlin
import java.time.DayOfWeek
import java.time.LocalDate
```

add after the quiet-hours check:

```kotlin
        // WorkManager has no weekday scheduling, so the work is enqueued daily and filtered here.
        if (LocalDate.now().dayOfWeek !in QUOTE_DAYS) return Result.success()
```

and add to the `companion object`:

```kotlin
        private val QUOTE_DAYS = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
```

- [ ] **Step 3: Restrict News to Tue/Thu**

In `NewsNotificationWorker.kt`, add the imports:

```kotlin
import java.time.DayOfWeek
import java.time.LocalDate
```

add after the quiet-hours check:

```kotlin
        // WorkManager has no weekday scheduling, so the work is enqueued daily and filtered here.
        if (LocalDate.now().dayOfWeek !in NEWS_DAYS) return Result.success()
```

and add to the `companion object`:

```kotlin
        private val NEWS_DAYS = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)
```

- [ ] **Step 4: Write the failing streak test**

Add these two tests to `domain/src/test/kotlin/iti/grad/nutriscan/domain/notification/usecase/ShouldNotifyStreakUseCaseTest.kt`, and update every existing call in that file to pass `hasStreak = true`:

```kotlin
    @Test
    fun `does not notify when there is no streak to preserve`() {
        assertFalse(
            useCase(enabledPrefs, loggedFoodToday = false, hasStreak = false, now = LocalTime.of(21, 30))
        )
    }

    @Test
    fun `notifies when a live streak is at risk`() {
        assertTrue(
            useCase(enabledPrefs, loggedFoodToday = false, hasStreak = true, now = LocalTime.of(21, 30))
        )
    }
```

- [ ] **Step 5: Run the test to verify it fails**

Run: `./gradlew :domain:test --tests "*ShouldNotifyStreakUseCaseTest*"`
Expected: FAIL to compile — `invoke` has no `hasStreak` parameter.

- [ ] **Step 6: Add the parameter**

In `ShouldNotifyStreakUseCase.kt`, replace the `invoke` function:

```kotlin
    operator fun invoke(
        prefs: NotificationPrefs,
        loggedFoodToday: Boolean,
        hasStreak: Boolean,
        now: LocalTime,
    ): Boolean {
        if (!prefs.isEnabled(NotificationType.STREAK)) return false
        if (isWithinQuietHours(prefs, now)) return false
        if (loggedFoodToday) return false
        // The Food reminder at 19:30 already covers "you have logged nothing today". This one
        // only earns its place when there is an actual streak on the line — and its copy reads
        // as nonsense at zero.
        if (!hasStreak) return false
        return now >= EVENING_WARNING_START
    }
```

- [ ] **Step 7: Update the streak worker**

In `StreakNotificationWorker.kt`, the streak has to be read before the decision instead of after. Replace these lines

```kotlin
        val loggedToday = observeTodayFoodLog().first().isNotEmpty()
        if (!shouldNotifyStreak(prefs, loggedToday, LocalTime.now())) return Result.success()

        val streak = observeStreak().first()
```

with

```kotlin
        val loggedToday = observeTodayFoodLog().first().isNotEmpty()
        val streak = observeStreak().first()
        if (!shouldNotifyStreak(prefs, loggedToday, streak.currentStreak > 0, LocalTime.now())) {
            return Result.success()
        }
```

- [ ] **Step 8: Run the tests to verify they pass**

Run: `./gradlew :domain:test :app:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 9: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 10: Commit**

```bash
git add app/src/main/kotlin/iti/grad/nutriscan/notification/worker/ \
        domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase/ShouldNotifyStreakUseCase.kt \
        domain/src/test/kotlin/iti/grad/nutriscan/domain/notification/usecase/ShouldNotifyStreakUseCaseTest.kt
git commit -m "Thin out the daily reminders

Quotes drop to Mon/Wed/Fri and news to Tue/Thu — neither was worth a daily
interruption. Any slot WorkManager delivers more than 90 minutes late gets
skipped rather than arriving at a nonsensical hour.

The streak reminder now needs an actual streak. Without one it was just the
19:30 'log your meals' nudge again two hours later, and 'keep your streak
alive' reads as nonsense at zero."
```

---

### Task 12: Keep the debug broadcast out of release builds

`DebugNotificationReceiver` is declared in the main manifest with `android:exported="true"`, so the component ships in release. Its `sendAllTypesNow()` bypasses both per-type preferences and quiet hours — it is the most likely source of the all-at-once bursts in the reported screenshots.

**Files:**
- Move: `app/src/main/kotlin/iti/grad/nutriscan/notification/DebugNotificationReceiver.kt` → `app/src/debug/kotlin/iti/grad/nutriscan/notification/DebugNotificationReceiver.kt`
- Create: `app/src/debug/AndroidManifest.xml`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: nothing.
- Produces: nothing.

- [ ] **Step 1: Move the receiver**

```bash
mkdir -p app/src/debug/kotlin/iti/grad/nutriscan/notification
git mv app/src/main/kotlin/iti/grad/nutriscan/notification/DebugNotificationReceiver.kt \
       app/src/debug/kotlin/iti/grad/nutriscan/notification/DebugNotificationReceiver.kt
```

The `if (!BuildConfig.DEBUG) return` line stays — the source set makes it redundant, but a redundant guard on a component that fires every notification at once is not worth removing.

- [ ] **Step 2: Remove the receiver from the main manifest**

In `app/src/main/AndroidManifest.xml`, delete this block:

```xml
        <receiver
            android:name=".notification.DebugNotificationReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="iti.grad.nutriscan.DEBUG_SEND_ALL_NOTIFICATIONS" />
            </intent-filter>
        </receiver>
```

- [ ] **Step 3: Create the debug manifest**

Create `app/src/debug/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application>
        <!-- Exported so `adb shell am broadcast` can reach it. Debug source set only, so the
             component does not exist in a release build. -->
        <receiver
            android:name=".notification.DebugNotificationReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="iti.grad.nutriscan.DEBUG_SEND_ALL_NOTIFICATIONS" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

- [ ] **Step 4: Verify both variants build**

Run: `./gradlew :app:assembleDebug :app:assembleRelease`
Expected: BUILD SUCCESSFUL for both. If `assembleRelease` fails on signing config, run `./gradlew :app:compileReleaseKotlin :app:processReleaseManifest` instead and confirm no `DebugNotificationReceiver` appears in `app/build/intermediates/merged_manifests/release/AndroidManifest.xml`.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Keep the debug notification broadcast out of release builds

The receiver that fires every notification type at once — ignoring both the
per-type toggles and quiet hours — was declared in the main manifest as an
exported component, so it shipped in release. It lives in the debug source
set now."
```

---

### Task 13: Verify on device

The step-counter and scheduling fixes cannot be fully asserted in unit tests. Run these before opening the PR.

**Files:** none.

- [ ] **Step 1: Full test suite**

Run: `./gradlew test`
Expected: PASS across `:domain`, `:data`, `:presentation`, `:app`.

- [ ] **Step 2: Lint**

Run: `./gradlew lint`
Expected: no new errors introduced by this branch.

- [ ] **Step 3: Fresh-install burst**

Uninstall, install debug, launch, and log in. Expected: no notifications at any point during onboarding. The first reminder arrives at whichever slot comes next on the clock.

- [ ] **Step 4: Logged-out silence**

Log out. Expected: no notifications afterwards. Reinstall without logging in and leave the app closed past a slot time — still nothing.

- [ ] **Step 5: Step counter freshness**

With the app closed, walk roughly 50 steps, then open the Calories screen. Expected: the real count within a few seconds, not `0` and not a stale number. Then trigger the steps reminder's slot and confirm the notification quotes the same number the screen shows.

- [ ] **Step 6: Water figures match**

Log 3 glasses on the Calories screen. Expected: the next hydration reminder reads `3/<your goal>`, not `0/8`.

- [ ] **Step 7: Day rollover**

Change the device date forward one day (or leave it overnight) and open the Calories screen without walking. Expected: `0` steps, not yesterday's total.

- [ ] **Step 8: No sensor**

On an emulator without a step-counter sensor, open the Calories screen twice. Expected: no hang, no crash, and the screen recovers rather than being stuck — the retry path from Task 2.

---

## Notes for the reviewer

- **Slot times are device-local, day boundaries are Cairo.** `NotificationSlots` uses `LocalTime`/`LocalDateTime` because a 9 AM reminder should mean 9 AM where the user is. `CairoDateProvider` still owns "what day is it" for the step baseline, food log, and sync. These are deliberately different.
- **`ExistingPeriodicWorkPolicy.KEEP` in Task 10 is load-bearing.** `UPDATE` would re-anchor the initial delay on every cold start, so a user who opens the app daily would never reach a slot.
- **DB 14 → 15 clears local tables** via the existing `fallbackToDestructiveMigration()`. Consistent with every bump since v5, but worth calling out in the PR description.
- **`targetWaterCnt` of 0** means no hydration reminders for that user. That is the intended read of "no goal set", covered by a test in Task 5.
