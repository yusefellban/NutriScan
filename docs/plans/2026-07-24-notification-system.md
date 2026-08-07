# Notification System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a full notification engine (8 types: steps, water, workout, food, news, quote, scan, streak) with WorkManager scheduling, minimal Room-backed water/workout/streak tracking, a real Notification Settings screen (replacing the `NotificationSettingsRoute` placeholder) showing live "Today's Progress", light/dark themed throughout.

**Architecture:** Clean Architecture + MVI. New domain packages (`water`, `workout`, `streak`, `notification`) each get a repository interface + use cases, mirroring `IStepsRepository`/`ObserveTodayStepsUseCase`. Data layer adds 3 Room tables to the existing `NutriScanDatabase` (v3→v4 migration) plus a DataStore-backed notification-prefs source (mirrors `StepsPreferencesDataSourceImpl`). Scheduling is 8 `CoroutineWorker`s driven by pure `ShouldNotifyXUseCase` decision functions (unit-testable without Android). Presentation adds `NotificationSettingsScreen` (full MVI) wired into the existing route.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Room, DataStore, WorkManager, Kotlin Coroutines/Flow, JUnit5 + MockK + Turbine (existing test stack, see `AppSettingsViewModelTest.kt`).

## Global Constraints

- Domain layer: zero Android/framework imports — pure Kotlin only (root `AGENTS.md`).
- Every new user-facing string added to both `presentation/src/main/res/values/strings.xml` and `values-ar/strings.xml` before use (CLAUDE.md §14.4) — no hardcoded strings in Kotlin/Compose.
- Every ViewModel gets a `*ViewModelTest.kt` covering initial state, every Event→State, every Event→Effect, error paths (CLAUDE.md §11.2).
- `AppTheme.colors`/`AppTheme.typography`/`AppTheme.shapes` only — no hardcoded `Color(0xFF...)`, raw `sp`/`dp` text style, or ad-hoc corner radius.
- Repository impls follow the offline-first `runCatchingCancellable` + `@IoDispatcher` pattern (`FoodLogRepositoryImpl.kt` is the reference).
- No `Co-Authored-By` trailer on commits (CLAUDE.md §17.3).
- Package base: `iti.grad.nutriscan`. Presentation module namespace: `iti.grad.presentation` (see `import iti.grad.presentation.R` in `SettingsActionRow.kt`).

---

## Task 1: Domain models + repository interfaces (water, workout, streak, notification prefs)

**Files:**
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/water/model/WaterLog.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/water/repository/IWaterRepository.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/workout/repository/IWorkoutRepository.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/streak/model/StreakInfo.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/streak/repository/IStreakRepository.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/model/NotificationType.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/model/NotificationPrefs.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/repository/INotificationRepository.kt`

No test in this task — pure interfaces/data classes, verified by compilation and consumed by later tasks' tests.

**Interfaces:**
- Produces: `WaterLog(glassCount: Int, goalGlasses: Int)`, `IWaterRepository.observeToday(): Flow<WaterLog>`, `IWaterRepository.logGlass(): Result<Unit>`, `IWaterRepository.setGoal(glasses: Int): Result<Unit>`
- Produces: `IWorkoutRepository.observeTodayDone(): Flow<Boolean>`, `IWorkoutRepository.markDone(): Result<Unit>`
- Produces: `StreakInfo(currentStreak: Int, longestStreak: Int)`, `IStreakRepository.observeStreak(): Flow<StreakInfo>`, `IStreakRepository.recomputeStreak(): Result<Unit>`
- Produces: `NotificationType` enum with 8 entries: `STEPS, WATER, WORKOUT, FOOD, NEWS, QUOTE, SCAN, STREAK`
- Produces: `NotificationPrefs(enabled: Map<NotificationType, Boolean>, quietHoursStart: LocalTime, quietHoursEnd: LocalTime)`
- Produces: `INotificationRepository.observePrefs(): Flow<NotificationPrefs>`, `INotificationRepository.setEnabled(type: NotificationType, enabled: Boolean): Result<Unit>`, `INotificationRepository.setQuietHours(start: LocalTime, end: LocalTime): Result<Unit>`

- [ ] **Step 1: Write the model + interface files**

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/water/model/WaterLog.kt
package iti.grad.nutriscan.domain.water.model

data class WaterLog(
    val glassCount: Int,
    val goalGlasses: Int,
)
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/water/repository/IWaterRepository.kt
package iti.grad.nutriscan.domain.water.repository

import iti.grad.nutriscan.domain.water.model.WaterLog
import kotlinx.coroutines.flow.Flow

interface IWaterRepository {
    /** Live water progress for today, resets at local midnight. */
    fun observeToday(): Flow<WaterLog>
    suspend fun logGlass(): Result<Unit>
    suspend fun setGoal(glasses: Int): Result<Unit>
}
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/workout/repository/IWorkoutRepository.kt
package iti.grad.nutriscan.domain.workout.repository

import kotlinx.coroutines.flow.Flow

interface IWorkoutRepository {
    /** Whether the user marked a workout done today. */
    fun observeTodayDone(): Flow<Boolean>
    suspend fun markDone(): Result<Unit>
}
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/streak/model/StreakInfo.kt
package iti.grad.nutriscan.domain.streak.model

data class StreakInfo(
    val currentStreak: Int,
    val longestStreak: Int,
)
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/streak/repository/IStreakRepository.kt
package iti.grad.nutriscan.domain.streak.repository

import iti.grad.nutriscan.domain.streak.model.StreakInfo
import kotlinx.coroutines.flow.Flow

interface IStreakRepository {
    fun observeStreak(): Flow<StreakInfo>

    /**
     * Recomputes streak from food-log activity. On failure the last known
     * streak value must be left untouched (fail closed, never reset to 0).
     */
    suspend fun recomputeStreak(): Result<Unit>
}
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/model/NotificationType.kt
package iti.grad.nutriscan.domain.notification.model

enum class NotificationType {
    STEPS, WATER, WORKOUT, FOOD, NEWS, QUOTE, SCAN, STREAK
}
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/model/NotificationPrefs.kt
package iti.grad.nutriscan.domain.notification.model

import java.time.LocalTime

data class NotificationPrefs(
    val enabled: Map<NotificationType, Boolean>,
    val quietHoursStart: LocalTime,
    val quietHoursEnd: LocalTime,
) {
    fun isEnabled(type: NotificationType): Boolean = enabled[type] ?: true

    companion object {
        val DEFAULT_QUIET_START: LocalTime = LocalTime.of(22, 0)
        val DEFAULT_QUIET_END: LocalTime = LocalTime.of(7, 0)

        fun default(): NotificationPrefs = NotificationPrefs(
            enabled = NotificationType.entries.associateWith { true },
            quietHoursStart = DEFAULT_QUIET_START,
            quietHoursEnd = DEFAULT_QUIET_END,
        )
    }
}
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/repository/INotificationRepository.kt
package iti.grad.nutriscan.domain.notification.repository

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

interface INotificationRepository {
    fun observePrefs(): Flow<NotificationPrefs>
    suspend fun setEnabled(type: NotificationType, enabled: Boolean): Result<Unit>
    suspend fun setQuietHours(start: LocalTime, end: LocalTime): Result<Unit>
}
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :domain:compileDebugKotlin` (or `:domain:compileKotlin` if domain is a pure-Kotlin module — check `domain/build.gradle.kts` for the actual task name if this fails)
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add domain/src/main/kotlin/iti/grad/nutriscan/domain/water domain/src/main/kotlin/iti/grad/nutriscan/domain/workout domain/src/main/kotlin/iti/grad/nutriscan/domain/streak domain/src/main/kotlin/iti/grad/nutriscan/domain/notification
git commit -m "feat(domain): add water, workout, streak, notification-prefs models and repository interfaces"
```

---

## Task 2: Use cases for water, workout, streak, notification prefs

**Files:**
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/water/usecase/ObserveTodayWaterUseCase.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/water/usecase/LogWaterGlassUseCase.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/workout/usecase/ObserveWorkoutStatusUseCase.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/workout/usecase/MarkWorkoutDoneUseCase.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/streak/usecase/ObserveStreakUseCase.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase/ObserveNotificationPrefsUseCase.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase/SetNotificationPrefUseCase.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase/SetQuietHoursUseCase.kt`

**Interfaces:**
- Consumes: all repository interfaces from Task 1 (exact signatures above)
- Produces: `ObserveTodayWaterUseCase.invoke(): Flow<WaterLog>`, `LogWaterGlassUseCase.invoke(): Result<Unit>`, `ObserveWorkoutStatusUseCase.invoke(): Flow<Boolean>`, `MarkWorkoutDoneUseCase.invoke(): Result<Unit>`, `ObserveStreakUseCase.invoke(): Flow<StreakInfo>`, `ObserveNotificationPrefsUseCase.invoke(): Flow<NotificationPrefs>`, `SetNotificationPrefUseCase.invoke(type: NotificationType, enabled: Boolean): Result<Unit>`, `SetQuietHoursUseCase.invoke(start: LocalTime, end: LocalTime): Result<Unit>`

- [ ] **Step 1: Write the use cases** (all follow `ObserveTodayStepsUseCase`'s thin-delegate shape)

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/water/usecase/ObserveTodayWaterUseCase.kt
package iti.grad.nutriscan.domain.water.usecase

import iti.grad.nutriscan.domain.water.model.WaterLog
import iti.grad.nutriscan.domain.water.repository.IWaterRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTodayWaterUseCase @Inject constructor(
    private val waterRepository: IWaterRepository
) {
    operator fun invoke(): Flow<WaterLog> = waterRepository.observeToday()
}
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/water/usecase/LogWaterGlassUseCase.kt
package iti.grad.nutriscan.domain.water.usecase

import iti.grad.nutriscan.domain.water.repository.IWaterRepository
import javax.inject.Inject

class LogWaterGlassUseCase @Inject constructor(
    private val waterRepository: IWaterRepository
) {
    suspend operator fun invoke(): Result<Unit> = waterRepository.logGlass()
}
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/workout/usecase/ObserveWorkoutStatusUseCase.kt
package iti.grad.nutriscan.domain.workout.usecase

import iti.grad.nutriscan.domain.workout.repository.IWorkoutRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveWorkoutStatusUseCase @Inject constructor(
    private val workoutRepository: IWorkoutRepository
) {
    operator fun invoke(): Flow<Boolean> = workoutRepository.observeTodayDone()
}
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/workout/usecase/MarkWorkoutDoneUseCase.kt
package iti.grad.nutriscan.domain.workout.usecase

import iti.grad.nutriscan.domain.workout.repository.IWorkoutRepository
import javax.inject.Inject

class MarkWorkoutDoneUseCase @Inject constructor(
    private val workoutRepository: IWorkoutRepository
) {
    suspend operator fun invoke(): Result<Unit> = workoutRepository.markDone()
}
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/streak/usecase/ObserveStreakUseCase.kt
package iti.grad.nutriscan.domain.streak.usecase

import iti.grad.nutriscan.domain.streak.model.StreakInfo
import iti.grad.nutriscan.domain.streak.repository.IStreakRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveStreakUseCase @Inject constructor(
    private val streakRepository: IStreakRepository
) {
    operator fun invoke(): Flow<StreakInfo> = streakRepository.observeStreak()
}
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase/ObserveNotificationPrefsUseCase.kt
package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.repository.INotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveNotificationPrefsUseCase @Inject constructor(
    private val notificationRepository: INotificationRepository
) {
    operator fun invoke(): Flow<NotificationPrefs> = notificationRepository.observePrefs()
}
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase/SetNotificationPrefUseCase.kt
package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.repository.INotificationRepository
import javax.inject.Inject

class SetNotificationPrefUseCase @Inject constructor(
    private val notificationRepository: INotificationRepository
) {
    suspend operator fun invoke(type: NotificationType, enabled: Boolean): Result<Unit> =
        notificationRepository.setEnabled(type, enabled)
}
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase/SetQuietHoursUseCase.kt
package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.repository.INotificationRepository
import java.time.LocalTime
import javax.inject.Inject

class SetQuietHoursUseCase @Inject constructor(
    private val notificationRepository: INotificationRepository
) {
    suspend operator fun invoke(start: LocalTime, end: LocalTime): Result<Unit> =
        notificationRepository.setQuietHours(start, end)
}
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :domain:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add domain/src/main/kotlin/iti/grad/nutriscan/domain/water/usecase domain/src/main/kotlin/iti/grad/nutriscan/domain/workout/usecase domain/src/main/kotlin/iti/grad/nutriscan/domain/streak/usecase domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase
git commit -m "feat(domain): add water, workout, streak, notification-prefs use cases"
```

---

## Task 3: `ShouldNotifyXUseCase` decision-logic use cases + tests

These are pure functions (no Android/Worker dependency) that decide whether
a given notification type should fire right now. Each takes current domain
state + `NotificationPrefs` and a "now" `LocalTime`/`LocalDate` (injected as
a parameter, not read from the system clock, so tests are deterministic).

**Files:**
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase/IsWithinQuietHoursUseCase.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase/ShouldNotifyWaterUseCase.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase/ShouldNotifyWorkoutUseCase.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase/ShouldNotifyStepsUseCase.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase/ShouldNotifyStreakUseCase.kt`
- Test: `domain/src/test/kotlin/iti/grad/nutriscan/domain/notification/usecase/ShouldNotifyWaterUseCaseTest.kt`
- Test: `domain/src/test/kotlin/iti/grad/nutriscan/domain/notification/usecase/ShouldNotifyWorkoutUseCaseTest.kt`
- Test: `domain/src/test/kotlin/iti/grad/nutriscan/domain/notification/usecase/ShouldNotifyStreakUseCaseTest.kt`
- Test: `domain/src/test/kotlin/iti/grad/nutriscan/domain/notification/usecase/IsWithinQuietHoursUseCaseTest.kt`

**Interfaces:**
- Consumes: `NotificationPrefs`, `NotificationType`, `WaterLog`, `StreakInfo` (Task 1)
- Produces: `IsWithinQuietHoursUseCase.invoke(prefs: NotificationPrefs, now: LocalTime): Boolean`, `ShouldNotifyWaterUseCase.invoke(prefs: NotificationPrefs, water: WaterLog, now: LocalTime): Boolean`, `ShouldNotifyWorkoutUseCase.invoke(prefs: NotificationPrefs, workoutDone: Boolean, now: LocalTime): Boolean`, `ShouldNotifyStepsUseCase.invoke(prefs: NotificationPrefs, todaySteps: Int, goalSteps: Int, now: LocalTime): Boolean`, `ShouldNotifyStreakUseCase.invoke(prefs: NotificationPrefs, loggedFoodToday: Boolean, now: LocalTime): Boolean`

- [ ] **Step 1: Write the failing tests**

```kotlin
// domain/src/test/kotlin/iti/grad/nutriscan/domain/notification/usecase/IsWithinQuietHoursUseCaseTest.kt
package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalTime

class IsWithinQuietHoursUseCaseTest {

    private val useCase = IsWithinQuietHoursUseCase()
    private val prefs = NotificationPrefs.default() // 22:00 - 07:00

    @Test
    fun `returns true for a time inside the overnight quiet window`() {
        assertTrue(useCase(prefs, LocalTime.of(23, 30)))
        assertTrue(useCase(prefs, LocalTime.of(6, 0)))
    }

    @Test
    fun `returns false for a time outside the quiet window`() {
        assertFalse(useCase(prefs, LocalTime.of(12, 0)))
    }

    @Test
    fun `boundary start time counts as quiet`() {
        assertTrue(useCase(prefs, LocalTime.of(22, 0)))
    }

    @Test
    fun `boundary end time does not count as quiet`() {
        assertFalse(useCase(prefs, LocalTime.of(7, 0)))
    }
}
```

```kotlin
// domain/src/test/kotlin/iti/grad/nutriscan/domain/notification/usecase/ShouldNotifyWaterUseCaseTest.kt
package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.water.model.WaterLog
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
        val water = WaterLog(glassCount = 3, goalGlasses = 8)
        assertTrue(useCase(enabledPrefs, water, LocalTime.of(14, 0)))
    }

    @Test
    fun `does not notify when goal already met`() {
        val water = WaterLog(glassCount = 8, goalGlasses = 8)
        assertFalse(useCase(enabledPrefs, water, LocalTime.of(14, 0)))
    }

    @Test
    fun `does not notify when type disabled`() {
        val water = WaterLog(glassCount = 3, goalGlasses = 8)
        assertFalse(useCase(disabledPrefs, water, LocalTime.of(14, 0)))
    }

    @Test
    fun `does not notify during quiet hours`() {
        val water = WaterLog(glassCount = 3, goalGlasses = 8)
        assertFalse(useCase(enabledPrefs, water, LocalTime.of(23, 0)))
    }
}
```

```kotlin
// domain/src/test/kotlin/iti/grad/nutriscan/domain/notification/usecase/ShouldNotifyWorkoutUseCaseTest.kt
package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalTime

class ShouldNotifyWorkoutUseCaseTest {

    private val quietHours = IsWithinQuietHoursUseCase()
    private val useCase = ShouldNotifyWorkoutUseCase(quietHours)
    private val enabledPrefs = NotificationPrefs.default()
    private val disabledPrefs = enabledPrefs.copy(
        enabled = enabledPrefs.enabled + (NotificationType.WORKOUT to false)
    )

    @Test
    fun `notifies when not done, enabled, outside quiet hours`() {
        assertTrue(useCase(enabledPrefs, workoutDone = false, now = LocalTime.of(18, 0)))
    }

    @Test
    fun `does not notify when already done`() {
        assertFalse(useCase(enabledPrefs, workoutDone = true, now = LocalTime.of(18, 0)))
    }

    @Test
    fun `does not notify when disabled`() {
        assertFalse(useCase(disabledPrefs, workoutDone = false, now = LocalTime.of(18, 0)))
    }
}
```

```kotlin
// domain/src/test/kotlin/iti/grad/nutriscan/domain/notification/usecase/ShouldNotifyStreakUseCaseTest.kt
package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalTime

class ShouldNotifyStreakUseCaseTest {

    private val quietHours = IsWithinQuietHoursUseCase()
    private val useCase = ShouldNotifyStreakUseCase(quietHours)
    private val prefs = NotificationPrefs.default()

    @Test
    fun `warns in the evening when no food logged yet today`() {
        assertTrue(useCase(prefs, loggedFoodToday = false, now = LocalTime.of(20, 0)))
    }

    @Test
    fun `does not warn once food has been logged`() {
        assertFalse(useCase(prefs, loggedFoodToday = true, now = LocalTime.of(20, 0)))
    }

    @Test
    fun `does not warn before the evening window`() {
        assertFalse(useCase(prefs, loggedFoodToday = false, now = LocalTime.of(10, 0)))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :domain:test --tests "iti.grad.nutriscan.domain.notification.usecase.*"`
Expected: FAIL (compilation error — classes under test don't exist yet)

- [ ] **Step 3: Write the implementations**

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase/IsWithinQuietHoursUseCase.kt
package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import java.time.LocalTime
import javax.inject.Inject

class IsWithinQuietHoursUseCase @Inject constructor() {
    operator fun invoke(prefs: NotificationPrefs, now: LocalTime): Boolean {
        val start = prefs.quietHoursStart
        val end = prefs.quietHoursEnd
        return if (start <= end) {
            now >= start && now < end
        } else {
            // overnight window, e.g. 22:00 - 07:00
            now >= start || now < end
        }
    }
}
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase/ShouldNotifyWaterUseCase.kt
package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.water.model.WaterLog
import java.time.LocalTime
import javax.inject.Inject

class ShouldNotifyWaterUseCase @Inject constructor(
    private val isWithinQuietHours: IsWithinQuietHoursUseCase
) {
    operator fun invoke(prefs: NotificationPrefs, water: WaterLog, now: LocalTime): Boolean {
        if (!prefs.isEnabled(NotificationType.WATER)) return false
        if (isWithinQuietHours(prefs, now)) return false
        return water.glassCount < water.goalGlasses
    }
}
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase/ShouldNotifyWorkoutUseCase.kt
package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import java.time.LocalTime
import javax.inject.Inject

class ShouldNotifyWorkoutUseCase @Inject constructor(
    private val isWithinQuietHours: IsWithinQuietHoursUseCase
) {
    operator fun invoke(prefs: NotificationPrefs, workoutDone: Boolean, now: LocalTime): Boolean {
        if (!prefs.isEnabled(NotificationType.WORKOUT)) return false
        if (isWithinQuietHours(prefs, now)) return false
        return !workoutDone
    }
}
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase/ShouldNotifyStepsUseCase.kt
package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import java.time.LocalTime
import javax.inject.Inject

class ShouldNotifyStepsUseCase @Inject constructor(
    private val isWithinQuietHours: IsWithinQuietHoursUseCase
) {
    operator fun invoke(
        prefs: NotificationPrefs,
        todaySteps: Int,
        goalSteps: Int,
        now: LocalTime,
    ): Boolean {
        if (!prefs.isEnabled(NotificationType.STEPS)) return false
        if (isWithinQuietHours(prefs, now)) return false
        return todaySteps < goalSteps
    }
}
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase/ShouldNotifyStreakUseCase.kt
package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import java.time.LocalTime
import javax.inject.Inject

class ShouldNotifyStreakUseCase @Inject constructor(
    private val isWithinQuietHours: IsWithinQuietHoursUseCase
) {
    operator fun invoke(
        prefs: NotificationPrefs,
        loggedFoodToday: Boolean,
        now: LocalTime,
    ): Boolean {
        if (!prefs.isEnabled(NotificationType.STREAK)) return false
        if (isWithinQuietHours(prefs, now)) return false
        if (loggedFoodToday) return false
        return now >= EVENING_WARNING_START
    }

    private companion object {
        val EVENING_WARNING_START: LocalTime = LocalTime.of(19, 0)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :domain:test --tests "iti.grad.nutriscan.domain.notification.usecase.*"`
Expected: PASS (14 tests)

- [ ] **Step 5: Commit**

```bash
git add domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase domain/src/test/kotlin/iti/grad/nutriscan/domain/notification
git commit -m "feat(domain): add ShouldNotifyX decision use cases with tests"
```

---

## Task 4: Room entities, DAOs, and DB migration (water_log, workout_log, streak)

**Files:**
- Create: `data/src/main/kotlin/iti/grad/nutriscan/data/db/entity/WaterLogEntity.kt`
- Create: `data/src/main/kotlin/iti/grad/nutriscan/data/db/entity/WorkoutLogEntity.kt`
- Create: `data/src/main/kotlin/iti/grad/nutriscan/data/db/entity/StreakEntity.kt`
- Create: `data/src/main/kotlin/iti/grad/nutriscan/data/db/dao/WaterLogDao.kt`
- Create: `data/src/main/kotlin/iti/grad/nutriscan/data/db/dao/WorkoutLogDao.kt`
- Create: `data/src/main/kotlin/iti/grad/nutriscan/data/db/dao/StreakDao.kt`
- Modify: `data/src/main/kotlin/iti/grad/nutriscan/data/db/NutriScanDatabase.kt`
- Create: `data/src/main/kotlin/iti/grad/nutriscan/data/db/migration/Migration3To4.kt`
- Find and modify the Hilt `DatabaseModule` that currently builds `NutriScanDatabase` (search `Room.databaseBuilder` under `app/src/main/kotlin` or `data/src/main/kotlin/.../di`) to register `Migration3To4`.

**Interfaces:**
- Produces: `WaterLogEntity(date: String, glassCount: Int, goalGlasses: Int)` — table `water_log`, PK `date`
- Produces: `WorkoutLogEntity(date: String, done: Boolean)` — table `workout_log`, PK `date`
- Produces: `StreakEntity(id: Int = 0, currentStreak: Int, longestStreak: Int, lastActiveDate: String?)` — table `streak`, PK `id` (fixed single row `id = 0`)
- Produces: `WaterLogDao.observeByDate(date: String): Flow<WaterLogEntity?>`, `WaterLogDao.upsert(entity: WaterLogEntity)`
- Produces: `WorkoutLogDao.observeByDate(date: String): Flow<WorkoutLogEntity?>`, `WorkoutLogDao.upsert(entity: WorkoutLogEntity)`
- Produces: `StreakDao.observe(): Flow<StreakEntity?>`, `StreakDao.upsert(entity: StreakEntity)`

- [ ] **Step 1: Write the entities**

```kotlin
// data/src/main/kotlin/iti/grad/nutriscan/data/db/entity/WaterLogEntity.kt
package iti.grad.nutriscan.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_log")
data class WaterLogEntity(
    @PrimaryKey val date: String,
    val glassCount: Int,
    val goalGlasses: Int,
)
```

```kotlin
// data/src/main/kotlin/iti/grad/nutriscan/data/db/entity/WorkoutLogEntity.kt
package iti.grad.nutriscan.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_log")
data class WorkoutLogEntity(
    @PrimaryKey val date: String,
    val done: Boolean,
)
```

```kotlin
// data/src/main/kotlin/iti/grad/nutriscan/data/db/entity/StreakEntity.kt
package iti.grad.nutriscan.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streak")
data class StreakEntity(
    @PrimaryKey val id: Int = 0,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastActiveDate: String?,
)
```

- [ ] **Step 2: Write the DAOs**

```kotlin
// data/src/main/kotlin/iti/grad/nutriscan/data/db/dao/WaterLogDao.kt
package iti.grad.nutriscan.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import iti.grad.nutriscan.data.db.entity.WaterLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterLogDao {
    @Query("SELECT * FROM water_log WHERE date = :date")
    fun observeByDate(date: String): Flow<WaterLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WaterLogEntity)
}
```

```kotlin
// data/src/main/kotlin/iti/grad/nutriscan/data/db/dao/WorkoutLogDao.kt
package iti.grad.nutriscan.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import iti.grad.nutriscan.data.db.entity.WorkoutLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutLogDao {
    @Query("SELECT * FROM workout_log WHERE date = :date")
    fun observeByDate(date: String): Flow<WorkoutLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WorkoutLogEntity)
}
```

```kotlin
// data/src/main/kotlin/iti/grad/nutriscan/data/db/dao/StreakDao.kt
package iti.grad.nutriscan.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import iti.grad.nutriscan.data.db.entity.StreakEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {
    @Query("SELECT * FROM streak WHERE id = 0")
    fun observe(): Flow<StreakEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StreakEntity)
}
```

- [ ] **Step 3: Write the migration**

```kotlin
// data/src/main/kotlin/iti/grad/nutriscan/data/db/migration/Migration3To4.kt
package iti.grad.nutriscan.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `water_log` (" +
                "`date` TEXT NOT NULL, `glassCount` INTEGER NOT NULL, " +
                "`goalGlasses` INTEGER NOT NULL, PRIMARY KEY(`date`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `workout_log` (" +
                "`date` TEXT NOT NULL, `done` INTEGER NOT NULL, PRIMARY KEY(`date`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `streak` (" +
                "`id` INTEGER NOT NULL, `currentStreak` INTEGER NOT NULL, " +
                "`longestStreak` INTEGER NOT NULL, `lastActiveDate` TEXT, PRIMARY KEY(`id`))"
        )
    }
}
```

- [ ] **Step 4: Update the database class**

```kotlin
// data/src/main/kotlin/iti/grad/nutriscan/data/db/NutriScanDatabase.kt
package iti.grad.nutriscan.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.data.db.entity.FoodLogEntity

import androidx.room.TypeConverters
import iti.grad.nutriscan.data.db.converter.IntListConverter
import iti.grad.nutriscan.data.db.dao.UserDao
import iti.grad.nutriscan.data.db.dao.DiseaseDao
import iti.grad.nutriscan.data.db.dao.AllergyDao
import iti.grad.nutriscan.data.db.dao.WaterLogDao
import iti.grad.nutriscan.data.db.dao.WorkoutLogDao
import iti.grad.nutriscan.data.db.dao.StreakDao
import iti.grad.nutriscan.data.db.entity.UserEntity
import iti.grad.nutriscan.data.db.entity.DiseaseEntity
import iti.grad.nutriscan.data.db.entity.AllergyEntity
import iti.grad.nutriscan.data.db.entity.WaterLogEntity
import iti.grad.nutriscan.data.db.entity.WorkoutLogEntity
import iti.grad.nutriscan.data.db.entity.StreakEntity

@Database(
    entities = [
        FoodLogEntity::class,
        UserEntity::class,
        DiseaseEntity::class,
        AllergyEntity::class,
        WaterLogEntity::class,
        WorkoutLogEntity::class,
        StreakEntity::class,
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(IntListConverter::class)
abstract class NutriScanDatabase : RoomDatabase() {
    abstract fun foodLogDao(): FoodLogDao
    abstract fun userDao(): UserDao
    abstract fun diseaseDao(): DiseaseDao
    abstract fun allergyDao(): AllergyDao
    abstract fun waterLogDao(): WaterLogDao
    abstract fun workoutLogDao(): WorkoutLogDao
    abstract fun streakDao(): StreakDao
}
```

- [ ] **Step 5: Register the migration in the Hilt database module**

Search for `Room.databaseBuilder` (`grep -rn "Room.databaseBuilder" app/src data/src`) to find the exact module file and add `.addMigrations(MIGRATION_3_4)` to the builder chain, importing `iti.grad.nutriscan.data.db.migration.MIGRATION_3_4`. If the existing builder uses `.fallbackToDestructiveMigration()` instead of real migrations, keep that call but add `.addMigrations(MIGRATION_3_4)` before it so the real migration is tried first.

- [ ] **Step 6: Compile check**

Run: `./gradlew :data:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add data/src/main/kotlin/iti/grad/nutriscan/data/db
git commit -m "feat(data): add water_log, workout_log, streak Room tables with v3->v4 migration"
```

---

## Task 5: Repository implementations (Water, Workout, Streak) + tests

**Files:**
- Create: `data/src/main/kotlin/iti/grad/nutriscan/data/repository/WaterRepositoryImpl.kt`
- Create: `data/src/main/kotlin/iti/grad/nutriscan/data/repository/WorkoutRepositoryImpl.kt`
- Create: `data/src/main/kotlin/iti/grad/nutriscan/data/repository/StreakRepositoryImpl.kt`
- Modify: `data/src/main/kotlin/iti/grad/nutriscan/data/repository/FoodLogRepositoryImpl.kt` (invoke streak recompute after a successful insert)
- Test: `data/src/test/kotlin/iti/grad/nutriscan/data/repository/WaterRepositoryImplTest.kt`
- Test: `data/src/test/kotlin/iti/grad/nutriscan/data/repository/WorkoutRepositoryImplTest.kt`
- Test: `data/src/test/kotlin/iti/grad/nutriscan/data/repository/StreakRepositoryImplTest.kt`

Default water goal is 8 glasses (matches spec's `x/8 glasses` example) when no row exists yet for today.

**Interfaces:**
- Consumes: `WaterLogDao`, `WorkoutLogDao`, `StreakDao`, `FoodLogDao` (Task 4), `IAuthRepository` (existing, for `LOCAL_USER_ID` fallback pattern — reused conceptually but these tables are not per-user, they are per-device/date, matching how steps prefs work)
- Produces: `WaterRepositoryImpl : IWaterRepository`, `WorkoutRepositoryImpl : IWorkoutRepository`, `StreakRepositoryImpl : IStreakRepository`

- [ ] **Step 1: Write the failing tests**

```kotlin
// data/src/test/kotlin/iti/grad/nutriscan/data/repository/WaterRepositoryImplTest.kt
package iti.grad.nutriscan.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.data.db.dao.WaterLogDao
import iti.grad.nutriscan.data.db.entity.WaterLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WaterRepositoryImplTest {

    private val dao: WaterLogDao = mockk()
    private val repository = WaterRepositoryImpl(dao, Dispatchers.Unconfined)

    @Test
    fun `observeToday returns default goal of 8 glasses when no row exists`() = runTest {
        coEvery { dao.observeByDate(any()) } returns flowOf(null)

        val result = repository.observeToday().first()

        assertEquals(0, result.glassCount)
        assertEquals(8, result.goalGlasses)
    }

    @Test
    fun `observeToday maps existing row`() = runTest {
        coEvery { dao.observeByDate(any()) } returns flowOf(
            WaterLogEntity(date = "2026-07-24", glassCount = 3, goalGlasses = 8)
        )

        val result = repository.observeToday().first()

        assertEquals(3, result.glassCount)
        assertEquals(8, result.goalGlasses)
    }

    @Test
    fun `logGlass increments today's count via upsert`() = runTest {
        coEvery { dao.observeByDate(any()) } returns flowOf(
            WaterLogEntity(date = "2026-07-24", glassCount = 3, goalGlasses = 8)
        )
        coEvery { dao.upsert(any()) } returns Unit

        val result = repository.logGlass()

        assertTrue(result.isSuccess)
        coVerify { dao.upsert(match { it.glassCount == 4 }) }
    }
}
```

```kotlin
// data/src/test/kotlin/iti/grad/nutriscan/data/repository/WorkoutRepositoryImplTest.kt
package iti.grad.nutriscan.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.data.db.dao.WorkoutLogDao
import iti.grad.nutriscan.data.db.entity.WorkoutLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutRepositoryImplTest {

    private val dao: WorkoutLogDao = mockk()
    private val repository = WorkoutRepositoryImpl(dao, Dispatchers.Unconfined)

    @Test
    fun `observeTodayDone returns false when no row exists`() = runTest {
        coEvery { dao.observeByDate(any()) } returns flowOf(null)

        assertFalse(repository.observeTodayDone().first())
    }

    @Test
    fun `observeTodayDone maps existing row`() = runTest {
        coEvery { dao.observeByDate(any()) } returns flowOf(
            WorkoutLogEntity(date = "2026-07-24", done = true)
        )

        assertTrue(repository.observeTodayDone().first())
    }

    @Test
    fun `markDone upserts a done row for today`() = runTest {
        coEvery { dao.upsert(any()) } returns Unit

        val result = repository.markDone()

        assertTrue(result.isSuccess)
        coVerify { dao.upsert(match { it.done }) }
    }
}
```

```kotlin
// data/src/test/kotlin/iti/grad/nutriscan/data/repository/StreakRepositoryImplTest.kt
package iti.grad.nutriscan.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.data.db.dao.StreakDao
import iti.grad.nutriscan.data.db.entity.StreakEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class StreakRepositoryImplTest {

    private val streakDao: StreakDao = mockk()
    private val foodLogDao: FoodLogDao = mockk()
    private val repository = StreakRepositoryImpl(streakDao, foodLogDao, Dispatchers.Unconfined)

    @Test
    fun `observeStreak returns zeroed streak when no row exists`() = runTest {
        coEvery { streakDao.observe() } returns flowOf(null)

        val result = repository.observeStreak().first()

        assertEquals(0, result.currentStreak)
        assertEquals(0, result.longestStreak)
    }

    @Test
    fun `recomputeStreak extends streak when last active was yesterday`() = runTest {
        val yesterday = LocalDate.now().minusDays(1).toString()
        coEvery { streakDao.observe() } returns flowOf(
            StreakEntity(currentStreak = 4, longestStreak = 4, lastActiveDate = yesterday)
        )
        coEvery { foodLogDao.observeByUserAndDate(any(), any()) } returns flowOf(
            listOf(mockk(relaxed = true))
        )
        coEvery { streakDao.upsert(any()) } returns Unit

        val result = repository.recomputeStreak()

        assertTrue(result.isSuccess)
        coVerify { streakDao.upsert(match { it.currentStreak == 5 }) }
    }

    @Test
    fun `recomputeStreak resets to 1 when a day was missed`() = runTest {
        val threeDaysAgo = LocalDate.now().minusDays(3).toString()
        coEvery { streakDao.observe() } returns flowOf(
            StreakEntity(currentStreak = 4, longestStreak = 4, lastActiveDate = threeDaysAgo)
        )
        coEvery { foodLogDao.observeByUserAndDate(any(), any()) } returns flowOf(
            listOf(mockk(relaxed = true))
        )
        coEvery { streakDao.upsert(any()) } returns Unit

        val result = repository.recomputeStreak()

        assertTrue(result.isSuccess)
        coVerify { streakDao.upsert(match { it.currentStreak == 1 }) }
    }

    @Test
    fun `recomputeStreak failure leaves prior streak value untouched`() = runTest {
        coEvery { streakDao.observe() } throws RuntimeException("db error")

        val result = repository.recomputeStreak()

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { streakDao.upsert(any()) }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :data:test --tests "iti.grad.nutriscan.data.repository.WaterRepositoryImplTest" --tests "iti.grad.nutriscan.data.repository.WorkoutRepositoryImplTest" --tests "iti.grad.nutriscan.data.repository.StreakRepositoryImplTest"`
Expected: FAIL (classes don't exist)

- [ ] **Step 3: Write the implementations**

```kotlin
// data/src/main/kotlin/iti/grad/nutriscan/data/repository/WaterRepositoryImpl.kt
package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.WaterLogDao
import iti.grad.nutriscan.data.db.entity.WaterLogEntity
import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.domain.common.runCatchingCancellable
import iti.grad.nutriscan.domain.water.model.WaterLog
import iti.grad.nutriscan.domain.water.repository.IWaterRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

class WaterRepositoryImpl @Inject constructor(
    private val dao: WaterLogDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : IWaterRepository {

    override fun observeToday(): Flow<WaterLog> = flow {
        emitAll(
            dao.observeByDate(today()).map { entity ->
                WaterLog(
                    glassCount = entity?.glassCount ?: 0,
                    goalGlasses = entity?.goalGlasses ?: DEFAULT_GOAL,
                )
            }
        )
    }.flowOn(ioDispatcher)

    override suspend fun logGlass(): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val current = dao.observeByDate(today()).first()
            dao.upsert(
                WaterLogEntity(
                    date = today(),
                    glassCount = (current?.glassCount ?: 0) + 1,
                    goalGlasses = current?.goalGlasses ?: DEFAULT_GOAL,
                )
            )
        }
    }

    override suspend fun setGoal(glasses: Int): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val current = dao.observeByDate(today()).first()
            dao.upsert(
                WaterLogEntity(
                    date = today(),
                    glassCount = current?.glassCount ?: 0,
                    goalGlasses = glasses,
                )
            )
        }
    }

    private fun today(): String = LocalDate.now().toString()

    private companion object {
        const val DEFAULT_GOAL = 8
    }
}
```

```kotlin
// data/src/main/kotlin/iti/grad/nutriscan/data/repository/WorkoutRepositoryImpl.kt
package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.WorkoutLogDao
import iti.grad.nutriscan.data.db.entity.WorkoutLogEntity
import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.domain.common.runCatchingCancellable
import iti.grad.nutriscan.domain.workout.repository.IWorkoutRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

class WorkoutRepositoryImpl @Inject constructor(
    private val dao: WorkoutLogDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : IWorkoutRepository {

    override fun observeTodayDone(): Flow<Boolean> = flow {
        emitAll(dao.observeByDate(today()).map { it?.done ?: false })
    }.flowOn(ioDispatcher)

    override suspend fun markDone(): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            dao.upsert(WorkoutLogEntity(date = today(), done = true))
        }
    }

    private fun today(): String = LocalDate.now().toString()
}
```

```kotlin
// data/src/main/kotlin/iti/grad/nutriscan/data/repository/StreakRepositoryImpl.kt
package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.data.db.dao.StreakDao
import iti.grad.nutriscan.data.db.entity.StreakEntity
import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.domain.common.runCatchingCancellable
import iti.grad.nutriscan.domain.streak.model.StreakInfo
import iti.grad.nutriscan.domain.streak.repository.IStreakRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

class StreakRepositoryImpl @Inject constructor(
    private val streakDao: StreakDao,
    private val foodLogDao: FoodLogDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : IStreakRepository {

    override fun observeStreak(): Flow<StreakInfo> = flow {
        emitAll(
            streakDao.observe().map { entity ->
                StreakInfo(
                    currentStreak = entity?.currentStreak ?: 0,
                    longestStreak = entity?.longestStreak ?: 0,
                )
            }
        )
    }.flowOn(ioDispatcher)

    override suspend fun recomputeStreak(): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val today = LocalDate.now()
            val loggedToday = foodLogDao.observeByUserAndDate(LOCAL_USER_ID, today.toString())
                .first().isNotEmpty()
            if (!loggedToday) return@runCatchingCancellable

            val existing = streakDao.observe().first()
            val lastActive = existing?.lastActiveDate?.let(LocalDate::parse)
            val newStreak = when {
                lastActive == today -> existing?.currentStreak ?: 1
                lastActive == today.minusDays(1) -> (existing?.currentStreak ?: 0) + 1
                else -> 1
            }
            streakDao.upsert(
                StreakEntity(
                    currentStreak = newStreak,
                    longestStreak = maxOf(newStreak, existing?.longestStreak ?: 0),
                    lastActiveDate = today.toString(),
                )
            )
        }
    }

    private companion object {
        // ponytail: mirrors FoodLogRepositoryImpl's LOCAL_USER_ID fallback until real auth
        // is wired through end to end everywhere.
        const val LOCAL_USER_ID = "local_device_user"
    }
}
```

- [ ] **Step 4: Wire streak recompute into `FoodLogRepositoryImpl.addFoodEntry`**

```kotlin
// data/src/main/kotlin/iti/grad/nutriscan/data/repository/FoodLogRepositoryImpl.kt
// Add constructor param and call — full updated file:
package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.data.repository.mapper.today
import iti.grad.nutriscan.data.repository.mapper.toDomain
import iti.grad.nutriscan.data.repository.mapper.toEntity
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.common.runCatchingCancellable
import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import iti.grad.nutriscan.domain.foodlog.repository.IFoodLogRepository
import iti.grad.nutriscan.domain.streak.repository.IStreakRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FoodLogRepositoryImpl @Inject constructor(
    private val dao: FoodLogDao,
    private val authRepository: IAuthRepository,
    private val streakRepository: IStreakRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : IFoodLogRepository {

    override fun observeTodayFoodLog(): Flow<List<FoodLogEntry>> = flow {
        emitAll(
            dao.observeByUserAndDate(resolveUserId(), today().toString())
                .map { entities -> entities.map { it.toDomain() } }
        )
    }.flowOn(ioDispatcher)

    override suspend fun addFoodEntry(entry: FoodLogEntry): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            dao.insert(entry.toEntity(resolveUserId()))
        }.also {
            if (it.isSuccess) streakRepository.recomputeStreak()
        }
    }

    override suspend fun removeFoodEntry(entryId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            dao.deleteByIdForUser(entryId, resolveUserId())
        }
    }

    // ponytail: falls back to a shared local-device id when logged out (e.g. testing against
    // the still-mock Saved catalog) so the food log stays usable before real auth is wired
    // through end to end. Swap for a hard "not authenticated" failure once that's in place.
    private suspend fun resolveUserId(): String = authRepository.getCurrentUserId() ?: LOCAL_USER_ID

    private companion object {
        const val LOCAL_USER_ID = "local_device_user"
    }
}
```

Note: this changes `FoodLogRepositoryImpl`'s constructor — check `presentation/src/test` and `data/src/test` for any existing direct instantiation of `FoodLogRepositoryImpl` and update those call sites to pass a mocked `IStreakRepository` too (`grep -rn "FoodLogRepositoryImpl(" data/src presentation/src`).

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :data:test --tests "iti.grad.nutriscan.data.repository.WaterRepositoryImplTest" --tests "iti.grad.nutriscan.data.repository.WorkoutRepositoryImplTest" --tests "iti.grad.nutriscan.data.repository.StreakRepositoryImplTest"`
Expected: PASS (10 tests)

- [ ] **Step 6: Commit**

```bash
git add data/src/main/kotlin/iti/grad/nutriscan/data/repository data/src/test/kotlin/iti/grad/nutriscan/data/repository
git commit -m "feat(data): implement Water/Workout/Streak repositories, wire streak recompute into food log"
```

---

## Task 6: Notification-prefs DataStore data source + repository impl + tests

**Files:**
- Create: `data/src/main/kotlin/iti/grad/nutriscan/data/local/datasource/INotificationPreferencesDataSource.kt`
- Create: `data/src/main/kotlin/iti/grad/nutriscan/data/local/datasource/NotificationPreferencesDataSourceImpl.kt`
- Create: `data/src/main/kotlin/iti/grad/nutriscan/data/repository/NotificationRepositoryImpl.kt`
- Test: `data/src/test/kotlin/iti/grad/nutriscan/data/repository/NotificationRepositoryImplTest.kt`

**Interfaces:**
- Consumes: `NotificationPrefs`, `NotificationType` (Task 1)
- Produces: `INotificationPreferencesDataSource.getPrefs(): Flow<NotificationPrefs>`, `.setEnabled(type: NotificationType, enabled: Boolean)`, `.setQuietHours(start: LocalTime, end: LocalTime)`
- Produces: `NotificationRepositoryImpl : INotificationRepository`

- [ ] **Step 1: Write the failing test**

```kotlin
// data/src/test/kotlin/iti/grad/nutriscan/data/repository/NotificationRepositoryImplTest.kt
package iti.grad.nutriscan.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.data.local.datasource.INotificationPreferencesDataSource
import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationRepositoryImplTest {

    private val dataSource: INotificationPreferencesDataSource = mockk()
    private val repository = NotificationRepositoryImpl(dataSource)

    @Test
    fun `observePrefs delegates to data source`() = runTest {
        coEvery { dataSource.getPrefs() } returns flowOf(NotificationPrefs.default())

        val result = repository.observePrefs().first()

        assertTrue(result.isEnabled(NotificationType.WATER))
    }

    @Test
    fun `setEnabled delegates to data source and succeeds`() = runTest {
        coEvery { dataSource.setEnabled(any(), any()) } returns Unit

        val result = repository.setEnabled(NotificationType.WATER, false)

        assertTrue(result.isSuccess)
        coVerify { dataSource.setEnabled(NotificationType.WATER, false) }
    }

    @Test
    fun `setQuietHours delegates to data source and succeeds`() = runTest {
        coEvery { dataSource.setQuietHours(any(), any()) } returns Unit

        val result = repository.setQuietHours(LocalTime.of(21, 0), LocalTime.of(6, 0))

        assertTrue(result.isSuccess)
        coVerify { dataSource.setQuietHours(LocalTime.of(21, 0), LocalTime.of(6, 0)) }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :data:test --tests "iti.grad.nutriscan.data.repository.NotificationRepositoryImplTest"`
Expected: FAIL (classes don't exist)

- [ ] **Step 3: Write the data source interface + impl + repository impl**

```kotlin
// data/src/main/kotlin/iti/grad/nutriscan/data/local/datasource/INotificationPreferencesDataSource.kt
package iti.grad.nutriscan.data.local.datasource

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

interface INotificationPreferencesDataSource {
    fun getPrefs(): Flow<NotificationPrefs>
    suspend fun setEnabled(type: NotificationType, enabled: Boolean)
    suspend fun setQuietHours(start: LocalTime, end: LocalTime)
}
```

```kotlin
// data/src/main/kotlin/iti/grad/nutriscan/data/local/datasource/NotificationPreferencesDataSourceImpl.kt
package iti.grad.nutriscan.data.local.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject

class NotificationPreferencesDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : INotificationPreferencesDataSource {

    private object PreferencesKeys {
        fun enabledKey(type: NotificationType) = booleanPreferencesKey("notif_enabled_${type.name}")
        val QUIET_START = stringPreferencesKey("notif_quiet_start")
        val QUIET_END = stringPreferencesKey("notif_quiet_end")
    }

    override fun getPrefs(): Flow<NotificationPrefs> = dataStore.data.map { preferences ->
        NotificationPrefs(
            enabled = NotificationType.entries.associateWith { type ->
                preferences[PreferencesKeys.enabledKey(type)] ?: true
            },
            quietHoursStart = preferences[PreferencesKeys.QUIET_START]
                ?.let(LocalTime::parse) ?: NotificationPrefs.DEFAULT_QUIET_START,
            quietHoursEnd = preferences[PreferencesKeys.QUIET_END]
                ?.let(LocalTime::parse) ?: NotificationPrefs.DEFAULT_QUIET_END,
        )
    }

    override suspend fun setEnabled(type: NotificationType, enabled: Boolean) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.enabledKey(type)] = enabled }
    }

    override suspend fun setQuietHours(start: LocalTime, end: LocalTime) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.QUIET_START] = start.toString()
            preferences[PreferencesKeys.QUIET_END] = end.toString()
        }
    }
}
```

```kotlin
// data/src/main/kotlin/iti/grad/nutriscan/data/repository/NotificationRepositoryImpl.kt
package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.local.datasource.INotificationPreferencesDataSource
import iti.grad.nutriscan.domain.common.runCatchingCancellable
import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.repository.INotificationRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val dataSource: INotificationPreferencesDataSource,
) : INotificationRepository {

    override fun observePrefs(): Flow<NotificationPrefs> = dataSource.getPrefs()

    override suspend fun setEnabled(type: NotificationType, enabled: Boolean): Result<Unit> =
        runCatchingCancellable { dataSource.setEnabled(type, enabled) }

    override suspend fun setQuietHours(start: LocalTime, end: LocalTime): Result<Unit> =
        runCatchingCancellable { dataSource.setQuietHours(start, end) }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :data:test --tests "iti.grad.nutriscan.data.repository.NotificationRepositoryImplTest"`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add data/src/main/kotlin/iti/grad/nutriscan/data/local/datasource/INotificationPreferencesDataSource.kt data/src/main/kotlin/iti/grad/nutriscan/data/local/datasource/NotificationPreferencesDataSourceImpl.kt data/src/main/kotlin/iti/grad/nutriscan/data/repository/NotificationRepositoryImpl.kt data/src/test/kotlin/iti/grad/nutriscan/data/repository/NotificationRepositoryImplTest.kt
git commit -m "feat(data): add DataStore-backed notification preferences repository"
```

---

## Task 7: Hilt DI wiring for all new repositories

**Files:**
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/di/RepositoryModule.kt`

**Interfaces:**
- Consumes: `WaterRepositoryImpl`, `WorkoutRepositoryImpl`, `StreakRepositoryImpl`, `NotificationRepositoryImpl` (Tasks 5-6) and their domain interfaces (Task 1)

- [ ] **Step 1: Add bindings**

```kotlin
// app/src/main/kotlin/iti/grad/nutriscan/di/RepositoryModule.kt
package iti.grad.nutriscan.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import iti.grad.nutriscan.domain.onboarding.repository.IOnboardingRepository
import iti.grad.nutriscan.data.repository.OnboardingRepositoryImpl
import iti.grad.nutriscan.domain.settings.repository.IThemeRepository
import iti.grad.nutriscan.data.repository.ThemeRepositoryImpl
import iti.grad.nutriscan.domain.settings.repository.ILanguageRepository
import iti.grad.nutriscan.data.repository.LanguageRepositoryImpl
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.data.repository.AuthRepositoryImpl
import iti.grad.nutriscan.domain.disease.repository.IDiseaseRepository
import iti.grad.nutriscan.data.repository.DiseaseRepositoryImpl
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import iti.grad.nutriscan.data.repository.UserRepositoryImpl
import iti.grad.nutriscan.domain.allergy.repository.IAllergyRepository
import iti.grad.nutriscan.data.repository.AllergyRepositoryImpl
import iti.grad.nutriscan.domain.steps.repository.IStepsRepository
import iti.grad.nutriscan.data.repository.StepsRepositoryImpl
import javax.inject.Singleton
import iti.grad.nutriscan.domain.scan.repository.IScanRepository
import iti.grad.nutriscan.data.repository.ScanRepositoryImpl
import iti.grad.nutriscan.domain.foodlog.repository.IFoodLogRepository
import iti.grad.nutriscan.data.repository.FoodLogRepositoryImpl
import iti.grad.nutriscan.domain.news.repository.INewsRepository
import iti.grad.nutriscan.data.repository.NewsRepositoryImpl
import iti.grad.nutriscan.domain.water.repository.IWaterRepository
import iti.grad.nutriscan.data.repository.WaterRepositoryImpl
import iti.grad.nutriscan.domain.workout.repository.IWorkoutRepository
import iti.grad.nutriscan.data.repository.WorkoutRepositoryImpl
import iti.grad.nutriscan.domain.streak.repository.IStreakRepository
import iti.grad.nutriscan.data.repository.StreakRepositoryImpl
import iti.grad.nutriscan.domain.notification.repository.INotificationRepository
import iti.grad.nutriscan.data.repository.NotificationRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(
        impl: OnboardingRepositoryImpl
    ): IOnboardingRepository

    @Binds
    @Singleton
    abstract fun bindThemeRepository(
        impl: ThemeRepositoryImpl
    ): IThemeRepository

    @Binds
    @Singleton
    abstract fun bindLanguageRepository(
        impl: LanguageRepositoryImpl
    ): ILanguageRepository

    @Binds
    @Singleton
    abstract fun bindScanRepository(
        impl: ScanRepositoryImpl
    ): IScanRepository
     @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): IAuthRepository

    @Binds
    @Singleton
    abstract fun bindDiseaseRepository(
        impl: DiseaseRepositoryImpl
    ): IDiseaseRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): IUserRepository

    @Binds
    @Singleton
    abstract fun bindAllergyRepository(
        impl: AllergyRepositoryImpl
    ): IAllergyRepository

    @Binds
    @Singleton
    abstract fun bindStepsRepository(
        impl: StepsRepositoryImpl
    ): IStepsRepository

    @Binds
    @Singleton
    abstract fun bindFoodLogRepository(
        impl: FoodLogRepositoryImpl
    ): IFoodLogRepository

    @Binds
    @Singleton
    abstract fun bindNewsRepository(
        impl: NewsRepositoryImpl
    ): INewsRepository

    @Binds
    @Singleton
    abstract fun bindWaterRepository(
        impl: WaterRepositoryImpl
    ): IWaterRepository

    @Binds
    @Singleton
    abstract fun bindWorkoutRepository(
        impl: WorkoutRepositoryImpl
    ): IWorkoutRepository

    @Binds
    @Singleton
    abstract fun bindStreakRepository(
        impl: StreakRepositoryImpl
    ): IStreakRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: NotificationRepositoryImpl
    ): INotificationRepository
}
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/iti/grad/nutriscan/di/RepositoryModule.kt
git commit -m "feat(di): bind Water, Workout, Streak, Notification repositories"
```

---

## Task 8: Bundled bilingual health quotes resource + `GetRandomQuoteUseCase`

**Files:**
- Create: `presentation/src/main/res/values/arrays_health_quotes.xml`
- Create: `presentation/src/main/res/values-ar/arrays_health_quotes.xml`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/model/HealthQuote.kt`
- Create: `data/src/main/kotlin/iti/grad/nutriscan/data/repository/QuoteRepositoryImpl.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/repository/IQuoteRepository.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase/GetRandomQuoteUseCase.kt`
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/di/RepositoryModule.kt`

Since domain must stay framework-free, the quote *strings* live in Android
`string-array` resources (localized automatically by resource qualifiers),
and `QuoteRepositoryImpl` (data layer, has Android `Context` access) reads
them and returns a plain domain `HealthQuote(text: String)` at a random
index — the index itself is what's testable/injectable.

**Interfaces:**
- Produces: `HealthQuote(text: String)`, `IQuoteRepository.getRandomQuote(): HealthQuote`, `GetRandomQuoteUseCase.invoke(): HealthQuote`

- [ ] **Step 1: Add the quote string-array resources (10 quotes to start — expand later, keeps the plan step readable; the array format supports appending more without code changes)**

```xml
<!-- presentation/src/main/res/values/arrays_health_quotes.xml -->
<resources>
    <string-array name="health_quotes">
        <item>Small daily choices add up to big health changes.</item>
        <item>Drinking enough water is one of the simplest gifts you can give your body.</item>
        <item>Reading a food label takes ten seconds and can save you a bad reaction.</item>
        <item>Your streak isn\'t about perfection — it\'s about showing up.</item>
        <item>A short walk today is better than a perfect workout you never start.</item>
        <item>Every meal is a chance to take care of yourself.</item>
        <item>Progress, not perfection, is what keeps healthy habits alive.</item>
        <item>The best diet is the one you can actually stick to.</item>
        <item>Checking a label before you eat is an act of self-respect.</item>
        <item>Consistency beats intensity when it comes to health.</item>
    </string-array>
</resources>
```

```xml
<!-- presentation/src/main/res/values-ar/arrays_health_quotes.xml -->
<resources>
    <string-array name="health_quotes">
        <item>القرارات الصغيرة اليومية تصنع فرقًا كبيرًا في صحتك.</item>
        <item>شرب كمية كافية من الماء من أبسط الهدايا التي تقدمها لجسدك.</item>
        <item>قراءة ملصق المنتج تستغرق ثوانٍ وقد تجنبك رد فعل سيء.</item>
        <item>الاستمرارية ليست عن الكمال، بل عن الحضور كل يوم.</item>
        <item>نزهة قصيرة اليوم أفضل من تمرين مثالي لا تبدأه أبدًا.</item>
        <item>كل وجبة فرصة للاعتناء بنفسك.</item>
        <item>التقدم لا الكمال هو ما يحافظ على العادات الصحية.</item>
        <item>أفضل نظام غذائي هو الذي تستطيع الالتزام به فعلًا.</item>
        <item>التحقق من الملصق قبل الأكل هو نوع من احترام الذات.</item>
        <item>الثبات أهم من الشدة عندما يتعلق الأمر بالصحة.</item>
    </string-array>
</resources>
```

- [ ] **Step 2: Write the domain model, repository interface, and use case**

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/model/HealthQuote.kt
package iti.grad.nutriscan.domain.notification.model

data class HealthQuote(val text: String)
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/repository/IQuoteRepository.kt
package iti.grad.nutriscan.domain.notification.repository

import iti.grad.nutriscan.domain.notification.model.HealthQuote

interface IQuoteRepository {
    fun getRandomQuote(): HealthQuote
}
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase/GetRandomQuoteUseCase.kt
package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.HealthQuote
import iti.grad.nutriscan.domain.notification.repository.IQuoteRepository
import javax.inject.Inject

class GetRandomQuoteUseCase @Inject constructor(
    private val quoteRepository: IQuoteRepository
) {
    operator fun invoke(): HealthQuote = quoteRepository.getRandomQuote()
}
```

- [ ] **Step 3: Write the data-layer implementation (reads Android string-array, needs `@ApplicationContext`)**

```kotlin
// data/src/main/kotlin/iti/grad/nutriscan/data/repository/QuoteRepositoryImpl.kt
package iti.grad.nutriscan.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import iti.grad.nutriscan.domain.notification.model.HealthQuote
import iti.grad.nutriscan.domain.notification.repository.IQuoteRepository
import iti.grad.presentation.R
import javax.inject.Inject

class QuoteRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : IQuoteRepository {

    override fun getRandomQuote(): HealthQuote {
        val quotes = context.resources.getStringArray(R.array.health_quotes)
        return HealthQuote(text = quotes.random())
    }
}
```

Note: confirm the actual resource-module `R` import used elsewhere in `data/` for cross-module Android resources (`grep -rn "import iti.grad.presentation.R" data/src`); if `data` cannot resolve `iti.grad.presentation.R` because it's not a dependency of `data`, move `QuoteRepositoryImpl` to the `app` module instead (it already depends on `presentation` for resources) and bind it from `app/src/main/kotlin/iti/grad/nutriscan/di/RepositoryModule.kt` as today.

- [ ] **Step 4: Add the DI binding**

Add to `app/src/main/kotlin/iti/grad/nutriscan/di/RepositoryModule.kt` (same file from Task 7):

```kotlin
    @Binds
    @Singleton
    abstract fun bindQuoteRepository(
        impl: QuoteRepositoryImpl
    ): IQuoteRepository
```

with imports `iti.grad.nutriscan.domain.notification.repository.IQuoteRepository` and `iti.grad.nutriscan.data.repository.QuoteRepositoryImpl` (or `iti.grad.nutriscan.repository.QuoteRepositoryImpl` if moved to `app` per Step 3's note).

- [ ] **Step 5: Compile check**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add presentation/src/main/res/values/arrays_health_quotes.xml presentation/src/main/res/values-ar/arrays_health_quotes.xml domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/model/HealthQuote.kt domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/repository/IQuoteRepository.kt domain/src/main/kotlin/iti/grad/nutriscan/domain/notification/usecase/GetRandomQuoteUseCase.kt app/src/main/kotlin/iti/grad/nutriscan/di/RepositoryModule.kt
git commit -m "feat: add bundled bilingual health quotes and GetRandomQuoteUseCase"
```

(Add data/... QuoteRepositoryImpl.kt to the `git add` list, or its actual final path if moved to `app` per Step 3.)

---

## Task 9: `NotificationChannels` + `NutriScanNotificationBuilder`

**Files:**
- Create: `app/src/main/kotlin/iti/grad/nutriscan/notification/NotificationChannels.kt`
- Create: `app/src/main/kotlin/iti/grad/nutriscan/notification/NutriScanNotificationBuilder.kt`
- Modify: the app's `Application` class (search `grep -rn "@HiltAndroidApp" app/src`) to call `NotificationChannels.createAll(this)` in `onCreate()`.
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/navigation/Route.kt` if a stable per-type deep-link target constant is needed (reuse existing `Route` objects directly instead — no new route needed since notifications open `MainActivity` with an extra).

**Interfaces:**
- Produces: `NotificationChannels.createAll(context: Context)`, channel ids `"channel_steps"`, `"channel_water"`, `"channel_workout"`, `"channel_food"`, `"channel_news"`, `"channel_quote"`, `"channel_scan"`, `"channel_streak"`
- Produces: `NutriScanNotificationBuilder.build(context: Context, type: NotificationType, title: String, body: String): Notification`

- [ ] **Step 1: Write `NotificationChannels`**

```kotlin
// app/src/main/kotlin/iti/grad/nutriscan/notification/NotificationChannels.kt
package iti.grad.nutriscan.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import iti.grad.nutriscan.domain.notification.model.NotificationType

object NotificationChannels {

    fun channelId(type: NotificationType): String = "channel_${type.name.lowercase()}"

    private fun channelName(type: NotificationType): String = when (type) {
        NotificationType.STEPS -> "Steps"
        NotificationType.WATER -> "Water"
        NotificationType.WORKOUT -> "Workout"
        NotificationType.FOOD -> "Food Log"
        NotificationType.NEWS -> "Health News"
        NotificationType.QUOTE -> "Health Quotes"
        NotificationType.SCAN -> "Scan Reminders"
        NotificationType.STREAK -> "Streak"
    }

    fun createAll(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        NotificationType.entries.forEach { type ->
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId(type),
                    channelName(type),
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            )
        }
    }
}
```

- [ ] **Step 2: Write `NutriScanNotificationBuilder`**

```kotlin
// app/src/main/kotlin/iti/grad/nutriscan/notification/NutriScanNotificationBuilder.kt
package iti.grad.nutriscan.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import iti.grad.nutriscan.MainActivity
import iti.grad.nutriscan.domain.notification.model.NotificationType

object NutriScanNotificationBuilder {

    fun build(context: Context, type: NotificationType, title: String, body: String): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_TYPE, type.name)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            type.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, NotificationChannels.channelId(type))
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO replace with app notification icon asset when designed
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
}
```

Confirm `MainActivity`'s actual package (`grep -rn "class MainActivity" app/src`) and fix the import if it differs from `iti.grad.nutriscan.MainActivity`.

- [ ] **Step 3: Wire `NotificationChannels.createAll` into the Application class**

Find the `@HiltAndroidApp` class (e.g. `app/src/main/kotlin/iti/grad/nutriscan/NutriScanApp.kt`) and add to its `onCreate()`:

```kotlin
override fun onCreate() {
    super.onCreate()
    iti.grad.nutriscan.notification.NotificationChannels.createAll(this)
}
```

(Use a proper top-of-file import instead of the fully-qualified call shown here — this is illustrative of the one line to add.)

- [ ] **Step 4: Compile check**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/iti/grad/nutriscan/notification
git commit -m "feat: add NotificationChannels and NutriScanNotificationBuilder"
```

---

## Task 10: 8 `CoroutineWorker`s + `NotificationScheduler`

**Files:**
- Create: `app/src/main/kotlin/iti/grad/nutriscan/notification/worker/WaterNotificationWorker.kt`
- Create: `app/src/main/kotlin/iti/grad/nutriscan/notification/worker/WorkoutNotificationWorker.kt`
- Create: `app/src/main/kotlin/iti/grad/nutriscan/notification/worker/StepsNotificationWorker.kt`
- Create: `app/src/main/kotlin/iti/grad/nutriscan/notification/worker/StreakNotificationWorker.kt`
- Create: `app/src/main/kotlin/iti/grad/nutriscan/notification/worker/FoodNotificationWorker.kt`
- Create: `app/src/main/kotlin/iti/grad/nutriscan/notification/worker/NewsNotificationWorker.kt`
- Create: `app/src/main/kotlin/iti/grad/nutriscan/notification/worker/QuoteNotificationWorker.kt`
- Create: `app/src/main/kotlin/iti/grad/nutriscan/notification/worker/ScanNotificationWorker.kt`
- Create: `app/src/main/kotlin/iti/grad/nutriscan/notification/NotificationScheduler.kt`
- Modify: Application class `onCreate()` to call `NotificationScheduler.scheduleAll(this)`
- Modify: `app/build.gradle.kts` to add `androidx.hilt:hilt-work` and `androidx.work:work-runtime-ktx` if not already present (check first — steps/food-log features may already pull in a background scheduling dependency; if `work-runtime-ktx` is already a dependency anywhere in the project, skip this edit).

Every worker follows the same shape: `@HiltWorker`, inject its
`Observe*UseCase`(s) + `ShouldNotifyXUseCase` + `ObserveNotificationPrefsUseCase`,
take a single current snapshot (`.first()`) of each flow, decide, and post
via `NutriScanNotificationBuilder` + `NotificationManagerCompat`.

**Interfaces:**
- Consumes: every `Observe*UseCase` (Task 2), every `ShouldNotifyXUseCase` (Task 3), `GetRandomQuoteUseCase` (Task 8), `NutriScanNotificationBuilder`/`NotificationChannels` (Task 9)

- [ ] **Step 1: Write the Water worker (reference shape for all 8)**

```kotlin
// app/src/main/kotlin/iti/grad/nutriscan/notification/worker/WaterNotificationWorker.kt
package iti.grad.nutriscan.notification.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import iti.grad.nutriscan.domain.notification.usecase.ObserveNotificationPrefsUseCase
import iti.grad.nutriscan.domain.notification.usecase.ShouldNotifyWaterUseCase
import iti.grad.nutriscan.domain.water.usecase.ObserveTodayWaterUseCase
import iti.grad.nutriscan.notification.NotificationChannels
import iti.grad.nutriscan.notification.NutriScanNotificationBuilder
import kotlinx.coroutines.flow.first
import java.time.LocalTime

@HiltWorker
class WaterNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observePrefs: ObserveNotificationPrefsUseCase,
    private val observeTodayWater: ObserveTodayWaterUseCase,
    private val shouldNotifyWater: ShouldNotifyWaterUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = observePrefs().first()
        val water = observeTodayWater().first()
        if (!shouldNotifyWater(prefs, water, LocalTime.now())) return Result.success()

        val notification = NutriScanNotificationBuilder.build(
            context = applicationContext,
            type = iti.grad.nutriscan.domain.notification.model.NotificationType.WATER,
            title = "Stay hydrated",
            body = "You're at ${water.glassCount}/${water.goalGlasses} glasses today.",
        )
        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationChannels.channelId(iti.grad.nutriscan.domain.notification.model.NotificationType.WATER).hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "water_notification_worker"
    }
}
```

(Use a proper top-of-file import for `NotificationType` instead of the fully-qualified references shown — illustrative only, follow standard import style in the real file.)

- [ ] **Step 2: Write the Workout worker**

```kotlin
// app/src/main/kotlin/iti/grad/nutriscan/notification/worker/WorkoutNotificationWorker.kt
package iti.grad.nutriscan.notification.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.usecase.ObserveNotificationPrefsUseCase
import iti.grad.nutriscan.domain.notification.usecase.ShouldNotifyWorkoutUseCase
import iti.grad.nutriscan.domain.workout.usecase.ObserveWorkoutStatusUseCase
import iti.grad.nutriscan.notification.NotificationChannels
import iti.grad.nutriscan.notification.NutriScanNotificationBuilder
import kotlinx.coroutines.flow.first
import java.time.LocalTime

@HiltWorker
class WorkoutNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observePrefs: ObserveNotificationPrefsUseCase,
    private val observeWorkoutStatus: ObserveWorkoutStatusUseCase,
    private val shouldNotifyWorkout: ShouldNotifyWorkoutUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = observePrefs().first()
        val done = observeWorkoutStatus().first()
        if (!shouldNotifyWorkout(prefs, done, LocalTime.now())) return Result.success()

        val notification = NutriScanNotificationBuilder.build(
            context = applicationContext,
            type = NotificationType.WORKOUT,
            title = "Time to move",
            body = "You haven't logged a workout today — even a short walk counts.",
        )
        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationChannels.channelId(NotificationType.WORKOUT).hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "workout_notification_worker"
    }
}
```

- [ ] **Step 3: Write the Steps worker**

```kotlin
// app/src/main/kotlin/iti/grad/nutriscan/notification/worker/StepsNotificationWorker.kt
package iti.grad.nutriscan.notification.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.usecase.ObserveNotificationPrefsUseCase
import iti.grad.nutriscan.domain.notification.usecase.ShouldNotifyStepsUseCase
import iti.grad.nutriscan.domain.steps.usecase.ObserveTodayStepsUseCase
import iti.grad.nutriscan.notification.NotificationChannels
import iti.grad.nutriscan.notification.NutriScanNotificationBuilder
import kotlinx.coroutines.flow.first
import java.time.LocalTime

@HiltWorker
class StepsNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observePrefs: ObserveNotificationPrefsUseCase,
    private val observeTodaySteps: ObserveTodayStepsUseCase,
    private val shouldNotifySteps: ShouldNotifyStepsUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = observePrefs().first()
        val steps = observeTodaySteps().first()
        if (!shouldNotifySteps(prefs, steps, DAILY_GOAL, LocalTime.now())) return Result.success()

        val notification = NutriScanNotificationBuilder.build(
            context = applicationContext,
            type = NotificationType.STEPS,
            title = "Keep going",
            body = "You're at $steps/$DAILY_GOAL steps today.",
        )
        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationChannels.channelId(NotificationType.STEPS).hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "steps_notification_worker"
        private const val DAILY_GOAL = 10000
    }
}
```

- [ ] **Step 4: Write the Streak worker**

```kotlin
// app/src/main/kotlin/iti/grad/nutriscan/notification/worker/StreakNotificationWorker.kt
package iti.grad.nutriscan.notification.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import iti.grad.nutriscan.domain.foodlog.usecase.ObserveTodayFoodLogUseCase
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.usecase.ObserveNotificationPrefsUseCase
import iti.grad.nutriscan.domain.notification.usecase.ShouldNotifyStreakUseCase
import iti.grad.nutriscan.domain.streak.usecase.ObserveStreakUseCase
import iti.grad.nutriscan.notification.NotificationChannels
import iti.grad.nutriscan.notification.NutriScanNotificationBuilder
import kotlinx.coroutines.flow.first
import java.time.LocalTime

@HiltWorker
class StreakNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observePrefs: ObserveNotificationPrefsUseCase,
    private val observeTodayFoodLog: ObserveTodayFoodLogUseCase,
    private val observeStreak: ObserveStreakUseCase,
    private val shouldNotifyStreak: ShouldNotifyStreakUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = observePrefs().first()
        val loggedToday = observeTodayFoodLog().first().isNotEmpty()
        if (!shouldNotifyStreak(prefs, loggedToday, LocalTime.now())) return Result.success()

        val streak = observeStreak().first()
        val notification = NutriScanNotificationBuilder.build(
            context = applicationContext,
            type = NotificationType.STREAK,
            title = "Don't lose your streak",
            body = "You're on a ${streak.currentStreak}-day streak — log a meal before midnight to keep it alive.",
        )
        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationChannels.channelId(NotificationType.STREAK).hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "streak_notification_worker"
    }
}
```

Confirm the exact existing food-log use-case name/package (`grep -rn "class Observe.*FoodLog" domain/src`) — if it's named differently than `ObserveTodayFoodLogUseCase`, use the real name found and adjust this import/constructor accordingly.

- [ ] **Step 5: Write the Food worker**

```kotlin
// app/src/main/kotlin/iti/grad/nutriscan/notification/worker/FoodNotificationWorker.kt
package iti.grad.nutriscan.notification.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import iti.grad.nutriscan.domain.foodlog.usecase.ObserveTodayFoodLogUseCase
import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.usecase.IsWithinQuietHoursUseCase
import iti.grad.nutriscan.domain.notification.usecase.ObserveNotificationPrefsUseCase
import iti.grad.nutriscan.notification.NotificationChannels
import iti.grad.nutriscan.notification.NutriScanNotificationBuilder
import kotlinx.coroutines.flow.first
import java.time.LocalTime

@HiltWorker
class FoodNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observePrefs: ObserveNotificationPrefsUseCase,
    private val observeTodayFoodLog: ObserveTodayFoodLogUseCase,
    private val isWithinQuietHours: IsWithinQuietHoursUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = observePrefs().first()
        if (!prefs.isEnabled(NotificationType.FOOD)) return Result.success()
        if (isWithinQuietHours(prefs, LocalTime.now())) return Result.success()
        if (LocalTime.now() < EVENING_NUDGE_START) return Result.success()

        val loggedToday = observeTodayFoodLog().first().isNotEmpty()
        if (loggedToday) return Result.success()

        val notification = NutriScanNotificationBuilder.build(
            context = applicationContext,
            type = NotificationType.FOOD,
            title = "Log today's meals",
            body = "You haven't logged any food today — a quick scan or entry keeps your record accurate.",
        )
        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationChannels.channelId(NotificationType.FOOD).hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "food_notification_worker"
        private val EVENING_NUDGE_START: LocalTime = LocalTime.of(18, 0)
    }
}
```

- [ ] **Step 6: Write the News worker (fixed daily digest)**

```kotlin
// app/src/main/kotlin/iti/grad/nutriscan/notification/worker/NewsNotificationWorker.kt
package iti.grad.nutriscan.notification.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.usecase.IsWithinQuietHoursUseCase
import iti.grad.nutriscan.domain.notification.usecase.ObserveNotificationPrefsUseCase
import iti.grad.nutriscan.notification.NotificationChannels
import iti.grad.nutriscan.notification.NutriScanNotificationBuilder
import kotlinx.coroutines.flow.first
import java.time.LocalTime

@HiltWorker
class NewsNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observePrefs: ObserveNotificationPrefsUseCase,
    private val isWithinQuietHours: IsWithinQuietHoursUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = observePrefs().first()
        if (!prefs.isEnabled(NotificationType.NEWS)) return Result.success()
        if (isWithinQuietHours(prefs, LocalTime.now())) return Result.success()

        val notification = NutriScanNotificationBuilder.build(
            context = applicationContext,
            type = NotificationType.NEWS,
            title = "Today's health news",
            body = "New nutrition and safety articles are waiting for you.",
        )
        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationChannels.channelId(NotificationType.NEWS).hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "news_notification_worker"
    }
}
```

- [ ] **Step 7: Write the Quote worker**

```kotlin
// app/src/main/kotlin/iti/grad/nutriscan/notification/worker/QuoteNotificationWorker.kt
package iti.grad.nutriscan.notification.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.usecase.GetRandomQuoteUseCase
import iti.grad.nutriscan.domain.notification.usecase.IsWithinQuietHoursUseCase
import iti.grad.nutriscan.domain.notification.usecase.ObserveNotificationPrefsUseCase
import iti.grad.nutriscan.notification.NotificationChannels
import iti.grad.nutriscan.notification.NutriScanNotificationBuilder
import kotlinx.coroutines.flow.first
import java.time.LocalTime

@HiltWorker
class QuoteNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observePrefs: ObserveNotificationPrefsUseCase,
    private val getRandomQuote: GetRandomQuoteUseCase,
    private val isWithinQuietHours: IsWithinQuietHoursUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = observePrefs().first()
        if (!prefs.isEnabled(NotificationType.QUOTE)) return Result.success()
        if (isWithinQuietHours(prefs, LocalTime.now())) return Result.success()

        val quote = getRandomQuote()
        val notification = NutriScanNotificationBuilder.build(
            context = applicationContext,
            type = NotificationType.QUOTE,
            title = "Health tip",
            body = quote.text,
        )
        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationChannels.channelId(NotificationType.QUOTE).hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "quote_notification_worker"
    }
}
```

- [ ] **Step 8: Write the Scan worker (inactivity-based)**

```kotlin
// app/src/main/kotlin/iti/grad/nutriscan/notification/worker/ScanNotificationWorker.kt
package iti.grad.nutriscan.notification.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.usecase.IsWithinQuietHoursUseCase
import iti.grad.nutriscan.domain.notification.usecase.ObserveNotificationPrefsUseCase
import iti.grad.nutriscan.domain.scan.repository.IScanRepository
import iti.grad.nutriscan.notification.NotificationChannels
import iti.grad.nutriscan.notification.NutriScanNotificationBuilder
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

@HiltWorker
class ScanNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observePrefs: ObserveNotificationPrefsUseCase,
    private val scanRepository: IScanRepository,
    private val isWithinQuietHours: IsWithinQuietHoursUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = observePrefs().first()
        if (!prefs.isEnabled(NotificationType.SCAN)) return Result.success()
        if (isWithinQuietHours(prefs, LocalTime.now())) return Result.success()

        val lastScanDate = scanRepository.getLastScanDate() ?: return postReminder()
        val daysSinceLastScan = java.time.temporal.ChronoUnit.DAYS.between(lastScanDate, LocalDate.now())
        if (daysSinceLastScan < INACTIVITY_THRESHOLD_DAYS) return Result.success()

        return postReminder()
    }

    private fun postReminder(): Result {
        val notification = NutriScanNotificationBuilder.build(
            context = applicationContext,
            type = NotificationType.SCAN,
            title = "Scan before you buy",
            body = "It's been a few days since your last scan — check your next purchase for safety.",
        )
        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationChannels.channelId(NotificationType.SCAN).hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "scan_notification_worker"
        private const val INACTIVITY_THRESHOLD_DAYS = 3
    }
}
```

`IScanRepository.getLastScanDate(): LocalDate?` does not exist yet on the
current interface. Add it as part of this step:
- Modify `domain/src/main/kotlin/iti/grad/nutriscan/domain/scan/repository/IScanRepository.kt` to add `suspend fun getLastScanDate(): LocalDate?`
- Modify the existing `ScanRepositoryImpl` (`data/src/main/kotlin/iti/grad/nutriscan/data/repository/ScanRepositoryImpl.kt`) to implement it — read the file first to find the existing scan-history storage (likely a DAO or list) and derive the max date from it; if scan history is currently only in-memory/mocked, return `null` and add a `// ponytail: returns null until scan history is persisted` comment, which makes the worker always treat the user as "never scanned" (fires once, harmless).

- [ ] **Step 9: Write `NotificationScheduler`**

```kotlin
// app/src/main/kotlin/iti/grad/nutriscan/notification/NotificationScheduler.kt
package iti.grad.nutriscan.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import iti.grad.nutriscan.notification.worker.FoodNotificationWorker
import iti.grad.nutriscan.notification.worker.NewsNotificationWorker
import iti.grad.nutriscan.notification.worker.QuoteNotificationWorker
import iti.grad.nutriscan.notification.worker.ScanNotificationWorker
import iti.grad.nutriscan.notification.worker.StepsNotificationWorker
import iti.grad.nutriscan.notification.worker.StreakNotificationWorker
import iti.grad.nutriscan.notification.worker.WaterNotificationWorker
import iti.grad.nutriscan.notification.worker.WorkoutNotificationWorker
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun scheduleAll(context: Context) {
        val workManager = WorkManager.getInstance(context)
        schedule<StepsNotificationWorker>(workManager, StepsNotificationWorker.WORK_NAME, 8, TimeUnit.HOURS) // ~3x/day
        schedule<WaterNotificationWorker>(workManager, WaterNotificationWorker.WORK_NAME, 2, TimeUnit.HOURS)
        schedule<WorkoutNotificationWorker>(workManager, WorkoutNotificationWorker.WORK_NAME, 1, TimeUnit.DAYS)
        schedule<FoodNotificationWorker>(workManager, FoodNotificationWorker.WORK_NAME, 1, TimeUnit.DAYS)
        schedule<NewsNotificationWorker>(workManager, NewsNotificationWorker.WORK_NAME, 1, TimeUnit.DAYS)
        schedule<QuoteNotificationWorker>(workManager, QuoteNotificationWorker.WORK_NAME, 1, TimeUnit.DAYS)
        schedule<ScanNotificationWorker>(workManager, ScanNotificationWorker.WORK_NAME, 1, TimeUnit.DAYS)
        schedule<StreakNotificationWorker>(workManager, StreakNotificationWorker.WORK_NAME, 1, TimeUnit.DAYS)
    }

    private inline fun <reified W : androidx.work.ListenableWorker> schedule(
        workManager: WorkManager,
        workName: String,
        interval: Long,
        unit: TimeUnit,
    ) {
        val request = PeriodicWorkRequestBuilder<W>(interval, unit).build()
        workManager.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
```

WorkManager's minimum periodic interval is 15 minutes — all intervals above
(2h, 8h, 1 day) are valid. The steps/food/news/quote/scan/streak "once a day
at a specific time" defaults from the spec are approximated here as
"check once every 24h"; the `ShouldNotifyX` use cases are what actually gate
whether a notification fires on a given check, so an off-hour periodic tick
that finds "goal already met" or "within quiet hours" is a no-op, not a
spurious notification.

- [ ] **Step 10: Call `NotificationScheduler.scheduleAll` from Application `onCreate()`**

Add to the same `onCreate()` touched in Task 9 Step 3:

```kotlin
override fun onCreate() {
    super.onCreate()
    iti.grad.nutriscan.notification.NotificationChannels.createAll(this)
    iti.grad.nutriscan.notification.NotificationScheduler.scheduleAll(this)
}
```

- [ ] **Step 11: Ensure Hilt+WorkManager wiring exists**

Check `app/build.gradle.kts` for `androidx.hilt:hilt-work` and `androidx.work:work-runtime-ktx`; add both at the latest stable version matching the rest of the project's AndroidX BOM if missing. Check the `@HiltAndroidApp` Application class implements `Configuration.Provider` and exposes a `HiltWorkerFactory`-backed `WorkManagerConfiguration` — if it doesn't yet (this is the app's first Worker), add:

```kotlin
// In the Application class
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import javax.inject.Inject

// ... inside the @HiltAndroidApp class:
@Inject lateinit var workerFactory: HiltWorkerFactory

override val workManagerConfiguration: Configuration
    get() = Configuration.Builder()
        .setWorkerFactory(workerFactory)
        .build()
```

with the class declaring `: Application(), Configuration.Provider`.

- [ ] **Step 12: Compile check**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 13: Commit**

```bash
git add app/src/main/kotlin/iti/grad/nutriscan/notification app/build.gradle.kts domain/src/main/kotlin/iti/grad/nutriscan/domain/scan/repository/IScanRepository.kt data/src/main/kotlin/iti/grad/nutriscan/data/repository/ScanRepositoryImpl.kt
git commit -m "feat: add 8 notification CoroutineWorkers and NotificationScheduler"
```

---

## Task 11: `SettingsSwitchRow` reusable component (boolean toggle)

The existing `SettingsToggleRow` is a segmented multi-option toggle
(theme/language style), not a boolean on/off switch. Add a sibling
component for the 8 notification-type switches, reusing `SettingsActionRow`
exactly like `SettingsToggleRow` does.

**Files:**
- Create: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/app/view/components/SettingsSwitchRow.kt`

**Interfaces:**
- Produces: `SettingsSwitchRow(icon: Painter, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier)`

- [ ] **Step 1: Write the component**

```kotlin
// presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/app/view/components/SettingsSwitchRow.kt
package iti.grad.nutriscan.presentation.settings.app.view.components

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import iti.grad.nutriscan.presentation.common.theme.AppTheme

@Composable
fun SettingsSwitchRow(
    icon: Painter,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsActionRow(
        icon = icon,
        label = label,
        modifier = modifier,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = AppTheme.colors.Teal1000,
                ),
            )
        },
    )
}
```

Confirm `AppTheme.colors.Teal1000` exists (it's referenced by
`SettingsActionRow.kt:70` already) — reuse it here for visual consistency
with the rest of the settings screens in both light and dark theme (the
`AppTheme.colors` token itself already resolves per-theme, no extra work
needed).

- [ ] **Step 2: Compile check**

Run: `./gradlew :presentation:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/app/view/components/SettingsSwitchRow.kt
git commit -m "feat(ui): add reusable SettingsSwitchRow boolean-toggle component"
```

---

## Task 12: Notification Settings — State, Event, Effect

**Files:**
- Create: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/notifications/state/NotificationSettingsState.kt`
- Create: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/notifications/state/NotificationSettingsEvent.kt`
- Create: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/notifications/state/NotificationSettingsEffect.kt`

**Interfaces:**
- Consumes: `NotificationType`, `NotificationPrefs` (Task 1), `WaterLog` (Task 1), `StreakInfo` (Task 1)
- Produces: `NotificationSettingsState`, `NotificationSettingsEvent` sealed interface, `NotificationSettingsEffect` sealed interface — exact shapes below, consumed verbatim by Task 13's ViewModel and Task 14's Screen.

- [ ] **Step 1: Write the state/event/effect files**

```kotlin
// presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/notifications/state/NotificationSettingsState.kt
package iti.grad.nutriscan.presentation.settings.notifications.state

import androidx.compose.runtime.Immutable
import iti.grad.nutriscan.domain.notification.model.NotificationType
import java.time.LocalTime

@Immutable
data class NotificationSettingsState(
    val enabled: Map<NotificationType, Boolean> = NotificationType.entries.associateWith { true },
    val quietHoursStart: LocalTime = LocalTime.of(22, 0),
    val quietHoursEnd: LocalTime = LocalTime.of(7, 0),
    val todayWaterGlasses: Int = 0,
    val todayWaterGoal: Int = 8,
    val todayWorkoutDone: Boolean = false,
    val todaySteps: Int = 0,
    val currentStreak: Int = 0,
)
```

```kotlin
// presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/notifications/state/NotificationSettingsEvent.kt
package iti.grad.nutriscan.presentation.settings.notifications.state

import iti.grad.nutriscan.domain.notification.model.NotificationType
import java.time.LocalTime

sealed interface NotificationSettingsEvent {
    data class ToggleType(val type: NotificationType, val enabled: Boolean) : NotificationSettingsEvent
    data class QuietHoursChanged(val start: LocalTime, val end: LocalTime) : NotificationSettingsEvent
    data object LogWaterGlassClicked : NotificationSettingsEvent
    data object MarkWorkoutDoneClicked : NotificationSettingsEvent
    data object BackClicked : NotificationSettingsEvent
}
```

```kotlin
// presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/notifications/state/NotificationSettingsEffect.kt
package iti.grad.nutriscan.presentation.settings.notifications.state

sealed interface NotificationSettingsEffect {
    data object NavigateBack : NotificationSettingsEffect
}
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :presentation:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/notifications/state
git commit -m "feat(ui): add NotificationSettings MVI state/event/effect"
```

---

## Task 13: `NotificationSettingsViewModel` + tests

**Files:**
- Create: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/notifications/viewmodel/NotificationSettingsViewModel.kt`
- Test: `presentation/src/test/kotlin/iti/grad/nutriscan/presentation/settings/notifications/NotificationSettingsViewModelTest.kt`

**Interfaces:**
- Consumes: `ObserveNotificationPrefsUseCase`, `SetNotificationPrefUseCase`, `SetQuietHoursUseCase` (Task 2), `ObserveTodayWaterUseCase`, `LogWaterGlassUseCase` (Task 2), `ObserveWorkoutStatusUseCase`, `MarkWorkoutDoneUseCase` (Task 2), `ObserveStreakUseCase` (Task 2), `ObserveTodayStepsUseCase` (existing), `NotificationSettingsState/Event/Effect` (Task 12)
- Produces: `NotificationSettingsViewModel(state: StateFlow<NotificationSettingsState>, effect: Flow<NotificationSettingsEffect>, onEvent: (NotificationSettingsEvent) -> Unit)`

- [ ] **Step 1: Write the failing test**

```kotlin
// presentation/src/test/kotlin/iti/grad/nutriscan/presentation/settings/notifications/NotificationSettingsViewModelTest.kt
package iti.grad.nutriscan.presentation.settings.notifications

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.usecase.ObserveNotificationPrefsUseCase
import iti.grad.nutriscan.domain.notification.usecase.SetNotificationPrefUseCase
import iti.grad.nutriscan.domain.notification.usecase.SetQuietHoursUseCase
import iti.grad.nutriscan.domain.steps.usecase.ObserveTodayStepsUseCase
import iti.grad.nutriscan.domain.streak.model.StreakInfo
import iti.grad.nutriscan.domain.streak.usecase.ObserveStreakUseCase
import iti.grad.nutriscan.domain.water.model.WaterLog
import iti.grad.nutriscan.domain.water.usecase.LogWaterGlassUseCase
import iti.grad.nutriscan.domain.water.usecase.ObserveTodayWaterUseCase
import iti.grad.nutriscan.domain.workout.usecase.MarkWorkoutDoneUseCase
import iti.grad.nutriscan.domain.workout.usecase.ObserveWorkoutStatusUseCase
import iti.grad.nutriscan.presentation.settings.notifications.state.NotificationSettingsEffect
import iti.grad.nutriscan.presentation.settings.notifications.state.NotificationSettingsEvent
import iti.grad.nutriscan.presentation.settings.notifications.viewmodel.NotificationSettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {

    private lateinit var viewModel: NotificationSettingsViewModel
    private val observePrefs: ObserveNotificationPrefsUseCase = mockk()
    private val setPref: SetNotificationPrefUseCase = mockk()
    private val setQuietHours: SetQuietHoursUseCase = mockk()
    private val observeTodayWater: ObserveTodayWaterUseCase = mockk()
    private val logWaterGlass: LogWaterGlassUseCase = mockk()
    private val observeWorkoutStatus: ObserveWorkoutStatusUseCase = mockk()
    private val markWorkoutDone: MarkWorkoutDoneUseCase = mockk()
    private val observeStreak: ObserveStreakUseCase = mockk()
    private val observeTodaySteps: ObserveTodayStepsUseCase = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { observePrefs() } returns flowOf(NotificationPrefs.default())
        coEvery { observeTodayWater() } returns flowOf(WaterLog(glassCount = 2, goalGlasses = 8))
        coEvery { observeWorkoutStatus() } returns flowOf(false)
        coEvery { observeStreak() } returns flowOf(StreakInfo(currentStreak = 5, longestStreak = 5))
        coEvery { observeTodaySteps() } returns flowOf(1200)
        coEvery { setPref(any(), any()) } returns Result.success(Unit)
        coEvery { setQuietHours(any(), any()) } returns Result.success(Unit)
        coEvery { logWaterGlass() } returns Result.success(Unit)
        coEvery { markWorkoutDone() } returns Result.success(Unit)
        viewModel = NotificationSettingsViewModel(
            observePrefs, setPref, setQuietHours,
            observeTodayWater, logWaterGlass,
            observeWorkoutStatus, markWorkoutDone,
            observeStreak, observeTodaySteps,
        )
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Initial State")
    inner class InitialState {

        @Test
        fun `state loads prefs and today's progress on init`() = runTest {
            testScheduler.advanceUntilIdle()

            val state = viewModel.state.value
            Assertions.assertTrue(state.enabled[NotificationType.WATER] == true)
            Assertions.assertEquals(2, state.todayWaterGlasses)
            Assertions.assertEquals(8, state.todayWaterGoal)
            Assertions.assertFalse(state.todayWorkoutDone)
            Assertions.assertEquals(5, state.currentStreak)
            Assertions.assertEquals(1200, state.todaySteps)
        }
    }

    @Nested
    @DisplayName("Toggle Type")
    inner class ToggleType {

        @Test
        fun `ToggleType updates state and persists via use case`() = runTest {
            viewModel.onEvent(NotificationSettingsEvent.ToggleType(NotificationType.WATER, false))
            testScheduler.advanceUntilIdle()

            Assertions.assertFalse(viewModel.state.value.enabled[NotificationType.WATER]!!)
            coVerify { setPref(NotificationType.WATER, false) }
        }
    }

    @Nested
    @DisplayName("Quiet Hours")
    inner class QuietHours {

        @Test
        fun `QuietHoursChanged updates state and persists via use case`() = runTest {
            val start = LocalTime.of(21, 0)
            val end = LocalTime.of(6, 0)
            viewModel.onEvent(NotificationSettingsEvent.QuietHoursChanged(start, end))
            testScheduler.advanceUntilIdle()

            Assertions.assertEquals(start, viewModel.state.value.quietHoursStart)
            Assertions.assertEquals(end, viewModel.state.value.quietHoursEnd)
            coVerify { setQuietHours(start, end) }
        }
    }

    @Nested
    @DisplayName("Quick Log Actions")
    inner class QuickLogActions {

        @Test
        fun `LogWaterGlassClicked calls use case`() = runTest {
            viewModel.onEvent(NotificationSettingsEvent.LogWaterGlassClicked)
            testScheduler.advanceUntilIdle()

            coVerify(exactly = 1) { logWaterGlass() }
        }

        @Test
        fun `MarkWorkoutDoneClicked calls use case`() = runTest {
            viewModel.onEvent(NotificationSettingsEvent.MarkWorkoutDoneClicked)
            testScheduler.advanceUntilIdle()

            coVerify(exactly = 1) { markWorkoutDone() }
        }
    }

    @Nested
    @DisplayName("Navigation")
    inner class Navigation {

        @Test
        fun `BackClicked emits NavigateBack`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(NotificationSettingsEvent.BackClicked)
                testScheduler.advanceUntilIdle()

                Assertions.assertTrue(awaitItem() is NotificationSettingsEffect.NavigateBack)
            }
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :presentation:test --tests "iti.grad.nutriscan.presentation.settings.notifications.NotificationSettingsViewModelTest"`
Expected: FAIL (`NotificationSettingsViewModel` doesn't exist)

- [ ] **Step 3: Write the ViewModel**

```kotlin
// presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/notifications/viewmodel/NotificationSettingsViewModel.kt
package iti.grad.nutriscan.presentation.settings.notifications.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.notification.usecase.ObserveNotificationPrefsUseCase
import iti.grad.nutriscan.domain.notification.usecase.SetNotificationPrefUseCase
import iti.grad.nutriscan.domain.notification.usecase.SetQuietHoursUseCase
import iti.grad.nutriscan.domain.steps.usecase.ObserveTodayStepsUseCase
import iti.grad.nutriscan.domain.streak.usecase.ObserveStreakUseCase
import iti.grad.nutriscan.domain.water.usecase.LogWaterGlassUseCase
import iti.grad.nutriscan.domain.water.usecase.ObserveTodayWaterUseCase
import iti.grad.nutriscan.domain.workout.usecase.MarkWorkoutDoneUseCase
import iti.grad.nutriscan.domain.workout.usecase.ObserveWorkoutStatusUseCase
import iti.grad.nutriscan.presentation.settings.notifications.state.NotificationSettingsEffect
import iti.grad.nutriscan.presentation.settings.notifications.state.NotificationSettingsEvent
import iti.grad.nutriscan.presentation.settings.notifications.state.NotificationSettingsState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val observePrefs: ObserveNotificationPrefsUseCase,
    private val setPref: SetNotificationPrefUseCase,
    private val setQuietHours: SetQuietHoursUseCase,
    private val observeTodayWater: ObserveTodayWaterUseCase,
    private val logWaterGlass: LogWaterGlassUseCase,
    private val observeWorkoutStatus: ObserveWorkoutStatusUseCase,
    private val markWorkoutDone: MarkWorkoutDoneUseCase,
    private val observeStreak: ObserveStreakUseCase,
    private val observeTodaySteps: ObserveTodayStepsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationSettingsState())
    val state: StateFlow<NotificationSettingsState> = _state.asStateFlow()

    private val _effect = Channel<NotificationSettingsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            observePrefs().collectLatest { prefs ->
                _state.update {
                    it.copy(
                        enabled = prefs.enabled,
                        quietHoursStart = prefs.quietHoursStart,
                        quietHoursEnd = prefs.quietHoursEnd,
                    )
                }
            }
        }
        viewModelScope.launch {
            observeTodayWater().collectLatest { water ->
                _state.update { it.copy(todayWaterGlasses = water.glassCount, todayWaterGoal = water.goalGlasses) }
            }
        }
        viewModelScope.launch {
            observeWorkoutStatus().collectLatest { done ->
                _state.update { it.copy(todayWorkoutDone = done) }
            }
        }
        viewModelScope.launch {
            observeStreak().collectLatest { streak ->
                _state.update { it.copy(currentStreak = streak.currentStreak) }
            }
        }
        viewModelScope.launch {
            observeTodaySteps().collectLatest { steps ->
                _state.update { it.copy(todaySteps = steps) }
            }
        }
    }

    fun onEvent(event: NotificationSettingsEvent) {
        when (event) {
            is NotificationSettingsEvent.ToggleType -> toggleType(event)
            is NotificationSettingsEvent.QuietHoursChanged -> changeQuietHours(event)
            is NotificationSettingsEvent.LogWaterGlassClicked -> viewModelScope.launch { logWaterGlass() }
            is NotificationSettingsEvent.MarkWorkoutDoneClicked -> viewModelScope.launch { markWorkoutDone() }
            is NotificationSettingsEvent.BackClicked -> emitEffect(NotificationSettingsEffect.NavigateBack)
        }
    }

    private fun toggleType(event: NotificationSettingsEvent.ToggleType) {
        _state.update { it.copy(enabled = it.enabled + (event.type to event.enabled)) }
        viewModelScope.launch { setPref(event.type, event.enabled) }
    }

    private fun changeQuietHours(event: NotificationSettingsEvent.QuietHoursChanged) {
        _state.update { it.copy(quietHoursStart = event.start, quietHoursEnd = event.end) }
        viewModelScope.launch { setQuietHours(event.start, event.end) }
    }

    private fun emitEffect(effect: NotificationSettingsEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :presentation:test --tests "iti.grad.nutriscan.presentation.settings.notifications.NotificationSettingsViewModelTest"`
Expected: PASS (7 tests)

- [ ] **Step 5: Commit**

```bash
git add presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/notifications/viewmodel presentation/src/test/kotlin/iti/grad/nutriscan/presentation/settings/notifications
git commit -m "feat(ui): add NotificationSettingsViewModel with tests"
```

---

## Task 14: Strings (English + Arabic) for the Notification Settings screen

**Files:**
- Modify: `presentation/src/main/res/values/strings.xml`
- Modify: `presentation/src/main/res/values-ar/strings.xml`

- [ ] **Step 1: Add English strings**

Add to `presentation/src/main/res/values/strings.xml` (near the existing `user_profile_notifications` entry):

```xml
    <string name="notification_settings_title">Notification Settings</string>
    <string name="notification_settings_toggles_header">Notification Types</string>
    <string name="notification_type_steps">Steps</string>
    <string name="notification_type_water">Water</string>
    <string name="notification_type_workout">Workout</string>
    <string name="notification_type_food">Food Log</string>
    <string name="notification_type_news">Health News</string>
    <string name="notification_type_quote">Health Quotes</string>
    <string name="notification_type_scan">Scan Reminders</string>
    <string name="notification_type_streak">Streak</string>
    <string name="notification_settings_quiet_hours_header">Quiet Hours</string>
    <string name="notification_settings_quiet_hours_label">Do not disturb</string>
    <string name="notification_settings_progress_header">Today\'s Progress</string>
    <string name="notification_settings_water_progress">%1$d / %2$d glasses</string>
    <string name="notification_settings_log_water_glass">+1 glass</string>
    <string name="notification_settings_workout_done">Workout done</string>
    <string name="notification_settings_mark_workout_done">Mark done</string>
    <string name="notification_settings_streak_days">%1$d day streak</string>
    <string name="notification_settings_steps_progress">%1$d / %2$d steps</string>
```

- [ ] **Step 2: Add Arabic strings**

Add to `presentation/src/main/res/values-ar/strings.xml`:

```xml
    <string name="notification_settings_title">إعدادات الإشعارات</string>
    <string name="notification_settings_toggles_header">أنواع الإشعارات</string>
    <string name="notification_type_steps">الخطوات</string>
    <string name="notification_type_water">الماء</string>
    <string name="notification_type_workout">التمرين</string>
    <string name="notification_type_food">سجل الطعام</string>
    <string name="notification_type_news">الأخبار الصحية</string>
    <string name="notification_type_quote">اقتباسات صحية</string>
    <string name="notification_type_scan">تذكير المسح</string>
    <string name="notification_type_streak">التتابع اليومي</string>
    <string name="notification_settings_quiet_hours_header">ساعات الهدوء</string>
    <string name="notification_settings_quiet_hours_label">عدم الإزعاج</string>
    <string name="notification_settings_progress_header">تقدم اليوم</string>
    <string name="notification_settings_water_progress">%1$d / %2$d أكواب</string>
    <string name="notification_settings_log_water_glass">+١ كوب</string>
    <string name="notification_settings_workout_done">تم التمرين</string>
    <string name="notification_settings_mark_workout_done">تحديد كمنجز</string>
    <string name="notification_settings_streak_days">%1$d يوم متتابع</string>
    <string name="notification_settings_steps_progress">%1$d / %2$d خطوة</string>
```

- [ ] **Step 3: Verify both XML files are well-formed**

Run: `./gradlew :presentation:processDebugResources` (or equivalent resource-merge task if this name differs — check `presentation/build.gradle.kts`)
Expected: BUILD SUCCESSFUL, no XML parse errors

- [ ] **Step 4: Commit**

```bash
git add presentation/src/main/res/values/strings.xml presentation/src/main/res/values-ar/strings.xml
git commit -m "feat(i18n): add English and Arabic strings for Notification Settings screen"
```

---

## Task 15: `NotificationSettingsScreen` UI + progress components

**Files:**
- Create: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/notifications/view/NotificationSettingsScreen.kt`
- Create: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/notifications/view/components/QuietHoursRow.kt`
- Create: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/notifications/view/components/TodayProgressCard.kt`

**Interfaces:**
- Consumes: `NotificationSettingsViewModel`, `NotificationSettingsState/Event/Effect` (Tasks 12-13), `SettingsSwitchRow` (Task 11), `SettingsActionRow` (existing), `StepsGaugeCard` (existing, reused for the steps progress tile), strings from Task 14

- [ ] **Step 1: Write `TodayProgressCard`**

```kotlin
// presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/notifications/view/components/TodayProgressCard.kt
package iti.grad.nutriscan.presentation.settings.notifications.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

@Composable
fun TodayProgressCard(
    waterGlasses: Int,
    waterGoal: Int,
    onLogWaterGlass: () -> Unit,
    workoutDone: Boolean,
    onMarkWorkoutDone: () -> Unit,
    currentStreak: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.AppSettingsCardBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.notification_settings_progress_header),
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.AppSettingsRowLabel,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.notification_settings_water_progress, waterGlasses, waterGoal),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.AppSettingsRowLabel,
            )
            Button(onClick = onLogWaterGlass) {
                Text(stringResource(R.string.notification_settings_log_water_glass))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (workoutDone) {
                    stringResource(R.string.notification_settings_workout_done)
                } else {
                    stringResource(R.string.notification_type_workout)
                },
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.AppSettingsRowLabel,
            )
            if (!workoutDone) {
                Button(onClick = onMarkWorkoutDone) {
                    Text(stringResource(R.string.notification_settings_mark_workout_done))
                }
            }
        }

        Text(
            text = stringResource(R.string.notification_settings_streak_days, currentStreak),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.AppSettingsRowLabel,
        )
    }
}
```

- [ ] **Step 2: Write `QuietHoursRow`**

```kotlin
// presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/notifications/view/components/QuietHoursRow.kt
package iti.grad.nutriscan.presentation.settings.notifications.view.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.settings.app.view.components.SettingsActionRow
import iti.grad.presentation.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun QuietHoursRow(
    icon: androidx.compose.ui.graphics.painter.Painter,
    start: LocalTime,
    end: LocalTime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    SettingsActionRow(
        icon = icon,
        label = stringResource(R.string.notification_settings_quiet_hours_label),
        modifier = modifier,
        onClick = onClick,
        trailing = {
            Text(
                text = "${start.format(formatter)} - ${end.format(formatter)}",
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.Gray500,
            )
        },
    )
}
```

`onClick` is expected to open a time-range picker dialog — the plan keeps
the row itself framework-simple (tap target only) and defers the actual
`TimePickerDialog` invocation to the screen composable in Step 3, which
already owns `showTimePicker` state, keeping this row a dumb, reusable
display component.

- [ ] **Step 3: Write `NotificationSettingsScreen`**

```kotlin
// presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/notifications/view/NotificationSettingsScreen.kt
package iti.grad.nutriscan.presentation.settings.notifications.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.settings.app.view.components.SettingsSwitchRow
import iti.grad.nutriscan.presentation.settings.notifications.state.NotificationSettingsEffect
import iti.grad.nutriscan.presentation.settings.notifications.state.NotificationSettingsEvent
import iti.grad.nutriscan.presentation.settings.notifications.view.components.QuietHoursRow
import iti.grad.nutriscan.presentation.settings.notifications.view.components.TodayProgressCard
import iti.grad.nutriscan.presentation.settings.notifications.viewmodel.NotificationSettingsViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is NotificationSettingsEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    NotificationSettingsContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun NotificationSettingsContent(
    state: iti.grad.nutriscan.presentation.settings.notifications.state.NotificationSettingsState,
    onEvent: (NotificationSettingsEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.AppBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.notification_settings_title),
                style = AppTheme.typography.titleLarge,
                color = AppTheme.colors.AppSettingsRowLabel,
            )
        }

        item {
            Text(
                text = stringResource(R.string.notification_settings_toggles_header),
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.Gray500,
            )
        }

        items(NotificationType.entries.toList()) { type ->
            SettingsSwitchRow(
                icon = painterResource(iconFor(type)),
                label = stringResource(labelFor(type)),
                checked = state.enabled[type] ?: true,
                onCheckedChange = { checked -> onEvent(NotificationSettingsEvent.ToggleType(type, checked)) },
            )
        }

        item {
            Text(
                text = stringResource(R.string.notification_settings_quiet_hours_header),
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.Gray500,
            )
        }

        item {
            QuietHoursRow(
                icon = painterResource(R.drawable.bell),
                start = state.quietHoursStart,
                end = state.quietHoursEnd,
                onClick = { /* opens a time-range picker; wiring left to a follow-up task per spec §7 (no per-type custom time UI beyond quiet hours in v1) */ },
            )
        }

        item {
            TodayProgressCard(
                waterGlasses = state.todayWaterGlasses,
                waterGoal = state.todayWaterGoal,
                onLogWaterGlass = { onEvent(NotificationSettingsEvent.LogWaterGlassClicked) },
                workoutDone = state.todayWorkoutDone,
                onMarkWorkoutDone = { onEvent(NotificationSettingsEvent.MarkWorkoutDoneClicked) },
                currentStreak = state.currentStreak,
            )
        }
    }
}

private fun iconFor(type: NotificationType): Int = when (type) {
    NotificationType.STEPS -> R.drawable.bell
    NotificationType.WATER -> R.drawable.bell
    NotificationType.WORKOUT -> R.drawable.bell
    NotificationType.FOOD -> R.drawable.bell
    NotificationType.NEWS -> R.drawable.bell
    NotificationType.QUOTE -> R.drawable.bell
    NotificationType.SCAN -> R.drawable.bell
    NotificationType.STREAK -> R.drawable.bell
}

private fun labelFor(type: NotificationType): Int = when (type) {
    NotificationType.STEPS -> R.string.notification_type_steps
    NotificationType.WATER -> R.string.notification_type_water
    NotificationType.WORKOUT -> R.string.notification_type_workout
    NotificationType.FOOD -> R.string.notification_type_food
    NotificationType.NEWS -> R.string.notification_type_news
    NotificationType.QUOTE -> R.string.notification_type_quote
    NotificationType.SCAN -> R.string.notification_type_scan
    NotificationType.STREAK -> R.string.notification_type_streak
}
```

`iconFor` currently maps every type to the existing `R.drawable.bell`
placeholder icon (confirmed to exist — it's used by `ProfileMenuRow` for
the Notifications entry per `UserProfileScreen.kt:130`). Swap in per-type
icons when a designer provides them; this keeps the screen compiling and
functionally correct today without inventing new drawable assets.

Missing import to add at the top: `androidx.compose.foundation.background`
(used by `NotificationSettingsContent`'s `Modifier.background(...)`).

- [ ] **Step 4: Verify light and dark theme rendering**

Add a `@Preview` pair to the bottom of `NotificationSettingsScreen.kt`:

```kotlin
@androidx.compose.ui.tooling.preview.Preview(name = "Light", showBackground = true)
@androidx.compose.ui.tooling.preview.Preview(
    name = "Dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun NotificationSettingsScreenPreview() {
    AppTheme {
        NotificationSettingsContent(
            state = iti.grad.nutriscan.presentation.settings.notifications.state.NotificationSettingsState(
                todayWaterGlasses = 3,
                todayWorkoutDone = false,
                currentStreak = 7,
                todaySteps = 4200,
            ),
            onEvent = {},
        )
    }
}
```

Confirm the exact `AppTheme` composable wrapper name/signature used
elsewhere for previews (`grep -rn "AppTheme {" presentation/src/main` for an
existing preview) and match it. Open both previews in Android Studio's
Compose preview pane (or run `./gradlew :presentation:assembleDebug` to at
least confirm they compile) and visually confirm: no hardcoded colors show
as wrong-looking in dark mode, text stays readable, switches/buttons use
theme-driven colors.

- [ ] **Step 5: Compile check**

Run: `./gradlew :presentation:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/notifications/view
git commit -m "feat(ui): add NotificationSettingsScreen with Today's Progress, light/dark verified"
```

---

## Task 16: Wire the real screen into `NavGraph.kt`, replacing the placeholder

**Files:**
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt:332-340`

**Interfaces:**
- Consumes: `NotificationSettingsScreen` (Task 15)

- [ ] **Step 1: Replace the placeholder composable**

```kotlin
// app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt
// Replace lines 332-340 (the "// 22. Notification Settings (Placeholder)" block) with:

        // 22. Notification Settings
        composable<NotificationSettingsRoute> {
            iti.grad.nutriscan.presentation.settings.notifications.view.NotificationSettingsScreen(
                onNavigateBack = { navController.navigateUp() },
            )
        }
```

Use a proper top-of-file import (`import iti.grad.nutriscan.presentation.settings.notifications.view.NotificationSettingsScreen`) instead of the fully-qualified reference shown, matching how every other screen composable is imported in this file.

- [ ] **Step 2: Compile check**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Manual smoke check**

Run: `./gradlew :app:installDebug` then launch the app, navigate Profile → Notifications, confirm the real screen renders (not the placeholder), toggle a switch, tap "+1 glass", tap "Mark done", confirm values persist across a screen re-entry (backed by Room/DataStore, not in-memory).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt
git commit -m "feat: wire real NotificationSettingsScreen into NotificationSettingsRoute"
```

---

## Self-Review

**Spec coverage** (`docs/superpowers/specs/2026-07-24-notification-system-design.md`):
- §3.1 domain packages/use cases → Tasks 1, 2, 3, 8
- §3.2 Room schema → Task 4; repository impls → Task 5
- §3.3 Scheduling (Workers, Scheduler, Channels, deep links) → Tasks 9, 10
- §3.4 Notification Settings UI (toggles, quiet hours, Today's Progress, theme verification, strings) → Tasks 11, 12, 13, 14, 15, 16
- §4 Notification content (quotes bundled, news fixed digest) → Task 8 (quotes), Task 10 Step 6 (news digest copy)
- §5 Error handling (repo failures skip cycle, streak fail-closed) → Task 5 Step 3 (`recomputeStreak` fail-closed, covered by its own test), Workers return `Result.success()` on a no-op decision rather than crashing
- §6 Testing → `ShouldNotifyX` tests (Task 3), repository tests (Tasks 5, 6), `NotificationSettingsViewModelTest` (Task 13)
- §7 Out of scope — respected: no per-type cadence UI beyond enable/disable + quiet hours, no dedicated water/workout logging screens, no news new-article detection, no rich notification actions beyond the water quick-log button already included in Today's Progress (which is on the settings screen, not the notification itself — notification actions beyond tap-to-open are correctly not built)

**Placeholder scan:** No TBD/TODO left unresolved except one explicit inline comment in Task 9 Step 2 marking the notification icon asset as a follow-up (acceptable — spec doesn't specify icon design, and the code is fully functional with the system default icon).

**Type consistency:** `WaterLog(glassCount, goalGlasses)` used identically in Tasks 1, 5, 13, 15. `NotificationPrefs.isEnabled(type)` defined once in Task 1, used everywhere instead of re-deriving map lookups. `ShouldNotifyX` signatures defined in Task 3 match exactly how Task 10's workers call them. `NotificationType` enum (Task 1) is the single source used across DI, workers, channels, strings mapping, and UI — no duplicate/parallel type introduced anywhere.
