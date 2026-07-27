# Daily Tracking Backend Sync — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire the backend's 8-endpoint Daily Tracking controller to a Room-backed, offline-first local store, replacing the Calories screen's in-memory water/steps state, syncing meals live and water/steps once nightly at Cairo midnight.

**Architecture:** New `domain/dailytracking` + `data` layer (models, repository, Retrofit service, Room entity/DAO) following the exact shape of the existing `FoodLogRepositoryImpl`/`ExercisesRepositoryImpl` (`runCatchingCancellable` + `@IoDispatcher`). `FoodLogRepositoryImpl` gains best-effort backend sync side effects. A new Cairo-anchored date utility replaces device-local `LocalDate.now()`. A WorkManager job pushes unsynced days nightly.

**Tech Stack:** Kotlin, Room, Retrofit + kotlinx.serialization, Hilt, WorkManager (new dependency), JUnit5 + MockK + coroutines-test + Turbine (existing test stack).

## Global Constraints

- Zero hardcoded user-facing strings — any new UI-visible string goes in `strings.xml` (en + ar). This plan touches `CaloriesState`/`CaloriesViewModel` only for data wiring, not new UI copy, but flag any new string immediately if one comes up.
- `AppTheme.colors`/`typography`/`shapes` only — not touched in this plan (no new Composables).
- Every ViewModel change needs a test update (`CaloriesViewModelTest`).
- Layer boundaries: `domain` has zero Android/framework imports. `CairoDateProvider` must be pure `java.time`, no `android.*` import.
- Every new repository method returns `Result<T>` (or a `Flow`), never throws past its boundary — matches `runCatchingCancellable` convention.
- Full spec: `docs/plans/2026-07-27-daily-tracking-sync.md` — read it for the "why" behind every decision below; this document is the "how."

---

### Task 1: Add WorkManager + Hilt Worker dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Produces: `libs.androidx.work.runtime.ktx`, `libs.androidx.hilt.work`, `libs.androidx.hilt.compiler` version catalog aliases, available to every later task that needs WorkManager/`@HiltWorker`.

- [ ] **Step 1: Add version + library entries to the catalog**

In `gradle/libs.versions.toml`, add to `[versions]` (after `androidxBrowser`):
```toml
work = "2.10.0"
hiltWork = "1.2.0"
```
Add to `[libraries]` (after `hilt-navigation-compose`):
```toml
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "work" }
androidx-hilt-work = { group = "androidx.hilt", name = "hilt-work", version.ref = "hiltWork" }
androidx-hilt-compiler = { group = "androidx.hilt", name = "hilt-compiler", version.ref = "hiltWork" }
```

- [ ] **Step 2: Add the dependencies to `app/build.gradle.kts`**

In the `dependencies { ... }` block, after `implementation(libs.hilt.android)` / `ksp(libs.hilt.compiler)`:
```kotlin
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
```

- [ ] **Step 3: Verify it builds**

Run: `./gradlew :app:assembleDebug -q`
Expected: build succeeds (this only adds unused dependencies so far — nothing consumes them yet).

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "Add WorkManager + Hilt Worker dependencies"
```

---

### Task 2: CairoDateProvider

**Files:**
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/common/CairoDateProvider.kt`
- Test: `domain/src/test/kotlin/iti/grad/nutriscan/domain/common/CairoDateProviderTest.kt`

**Interfaces:**
- Produces: `CairoDateProvider.today(): LocalDate`, `CairoDateProvider.ZONE: ZoneId` — used by `StepsRepositoryImpl`, `FoodLogMapper.today()`, `DailyTrackingRepositoryImpl`, `DailyTrackingSyncWorker` in later tasks.

- [ ] **Step 1: Write the failing test**

```kotlin
package iti.grad.nutriscan.domain.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class CairoDateProviderTest {

    @Test
    fun `an instant just before Cairo midnight is still yesterday in Cairo`() {
        // 2026-07-27T21:59:00Z = 2026-07-27T23:59:00+02:00 (Cairo, UTC+2, no DST since 2015)
        val instant = Instant.parse("2026-07-27T21:59:00Z")
        assertEquals(LocalDate.parse("2026-07-27"), CairoDateProvider.todayAt(instant))
    }

    @Test
    fun `an instant just after Cairo midnight is already the next day in Cairo`() {
        // 2026-07-27T22:01:00Z = 2026-07-28T00:01:00+02:00 (Cairo)
        val instant = Instant.parse("2026-07-27T22:01:00Z")
        assertEquals(LocalDate.parse("2026-07-28"), CairoDateProvider.todayAt(instant))
    }

    @Test
    fun `today delegates to todayAt with the current instant`() {
        val today = CairoDateProvider.today()
        val expected = CairoDateProvider.todayAt(Instant.now())
        // Same call within the same second — allow either today's or (extremely rarely,
        // right at a day boundary) the next day's date rather than asserting exact equality.
        assert(today == expected || today == expected.plusDays(1))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :domain:test --tests "*.CairoDateProviderTest" -q`
Expected: FAIL — `CairoDateProvider` unresolved reference.

- [ ] **Step 3: Write the implementation**

```kotlin
package iti.grad.nutriscan.domain.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Single source of truth for "what day is it" across the app. Every day-boundary
 * concern (food log, steps reset, water reset, the nightly sync job) is
 * Cairo-anchored regardless of device timezone — see
 * docs/plans/2026-07-27-daily-tracking-sync.md for why.
 */
object CairoDateProvider {
    val ZONE: ZoneId = ZoneId.of("Africa/Cairo")

    fun today(): LocalDate = todayAt(Instant.now())

    fun todayAt(instant: Instant): LocalDate = LocalDate.ofInstant(instant, ZONE)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :domain:test --tests "*.CairoDateProviderTest" -q`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add domain/src/main/kotlin/iti/grad/nutriscan/domain/common/CairoDateProvider.kt domain/src/test/kotlin/iti/grad/nutriscan/domain/common/CairoDateProviderTest.kt
git commit -m "Add Cairo-anchored date provider"
```

---

### Task 3: Domain models + IDailyTrackingRepository

**Files:**
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/dailytracking/model/DailyTracking.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/dailytracking/model/DailyTrackingSummary.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/dailytracking/model/RemoteMealSnapshot.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/dailytracking/model/DailyTrackingRemoteSnapshot.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/dailytracking/repository/IDailyTrackingRepository.kt`

**Interfaces:**
- Produces: every type/method name below, consumed by Tasks 4, 9, 11, 13, 14, 15. No test in this task — it's pure interface/data-class declarations, exercised indirectly by `DailyTrackingRepositoryImplTest` in Task 9.

- [ ] **Step 1: `DailyTracking.kt`**

```kotlin
package iti.grad.nutriscan.domain.dailytracking.model

import java.time.LocalDate

data class DailyTracking(
    val date: LocalDate,
    val targetWaterCnt: Int,
    val waterCnt: Int,
    val stepsCnt: Int,
    val caloriesBurnedSteps: Int,
    val syncedToBackend: Boolean,
)
```

- [ ] **Step 2: `DailyTrackingSummary.kt`**

```kotlin
package iti.grad.nutriscan.domain.dailytracking.model

import java.time.LocalDate

data class DailyTrackingSummary(
    val date: LocalDate,
    val targetWaterCnt: Int,
    val waterCnt: Int,
    val stepsCnt: Int,
    val mealCount: Int,
)
```

- [ ] **Step 3: `RemoteMealSnapshot.kt` and `DailyTrackingRemoteSnapshot.kt`**

```kotlin
package iti.grad.nutriscan.domain.dailytracking.model

/** One meal as returned by the backend for a given day — used only for the
 * login/app-start reconciliation flow (Task 15), not the live add/remove path. */
data class RemoteMealSnapshot(
    val scanId: String,
    val productName: String?,
    val imageUrl: String?,
    val calories: Int,
)
```

```kotlin
package iti.grad.nutriscan.domain.dailytracking.model

import java.time.LocalDate

data class DailyTrackingRemoteSnapshot(
    val date: LocalDate,
    val targetWaterCnt: Int,
    val waterCnt: Int,
    val stepsCnt: Int,
    val meals: List<RemoteMealSnapshot>,
)
```

- [ ] **Step 4: `IDailyTrackingRepository.kt`**

```kotlin
package iti.grad.nutriscan.domain.dailytracking.repository

import iti.grad.nutriscan.domain.dailytracking.model.DailyTracking
import iti.grad.nutriscan.domain.dailytracking.model.DailyTrackingRemoteSnapshot
import iti.grad.nutriscan.domain.dailytracking.model.DailyTrackingSummary
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface IDailyTrackingRepository {
    /** Local Room state for today, Cairo-anchored. Never null — a missing row maps to defaults. */
    fun observeToday(): Flow<DailyTracking>

    suspend fun getByDate(date: LocalDate): Result<DailyTracking>

    suspend fun getHistoryPage(page: Int, size: Int): Result<List<DailyTrackingSummary>>

    /** Updates local Room only (`syncedToBackend = false`) — no live network call. */
    suspend fun updateWaterCnt(waterCnt: Int): Result<Unit>

    suspend fun updateTargetWaterCnt(targetWaterCnt: Int): Result<Unit>

    /** Updates stepsCnt and derives+persists caloriesBurnedSteps from the user's weightKg. */
    suspend fun updateStepsCnt(stepsCnt: Int): Result<Unit>

    suspend fun deleteDay(date: LocalDate): Result<Unit>

    /** Live, best-effort backend call — no local state of its own (FoodLogEntity owns that). */
    suspend fun pushMeal(date: LocalDate, scanId: String, mealCnt: Int): Result<Unit>

    suspend fun updateMeal(date: LocalDate, scanId: String, mealCnt: Int): Result<Unit>

    suspend fun deleteMeal(date: LocalDate, scanId: String): Result<Unit>

    /** Nightly job entry point: PATCHes [date]'s water/steps if unsynced. No-op if already synced. */
    suspend fun syncPendingDay(date: LocalDate): Result<Unit>

    /** Login/app-start entry point: fetches GET /daily-tracking/today, seeds local Room water/steps
     * only if no row exists yet for today. Always returns the fetched snapshot on success so the
     * caller (ReconcileTodayUseCase, Task 4) can separately reconcile meals against FoodLogEntity. */
    suspend fun fetchAndSeedToday(): Result<DailyTrackingRemoteSnapshot>
}
```

- [ ] **Step 5: Commit**

```bash
git add domain/src/main/kotlin/iti/grad/nutriscan/domain/dailytracking
git commit -m "Add daily-tracking domain models and repository interface"
```

---

### Task 4: Domain use cases

**Files:**
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/dailytracking/usecase/ObserveTodayDailyTrackingUseCase.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/dailytracking/usecase/UpdateWaterCntUseCase.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/dailytracking/usecase/UpdateTargetWaterCntUseCase.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/dailytracking/usecase/UpdateStepsCntUseCase.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/dailytracking/usecase/SyncPendingDailyTrackingUseCase.kt`
- Create: `domain/src/main/kotlin/iti/grad/nutriscan/domain/dailytracking/usecase/ReconcileTodayUseCase.kt`

**Interfaces:**
- Consumes: `IDailyTrackingRepository` (Task 3), `IFoodLogRepository.addFoodEntryLocalOnly` (Task 11 — this task is written first but `ReconcileTodayUseCase` won't compile until Task 11 lands; that's fine, it's earlier in file order but later in the dependency graph — see note in Step 6).
- Produces: `ObserveTodayDailyTrackingUseCase()`, `UpdateWaterCntUseCase(waterCnt: Int)`, `UpdateTargetWaterCntUseCase(targetWaterCnt: Int)`, `UpdateStepsCntUseCase(stepsCnt: Int)`, `SyncPendingDailyTrackingUseCase(date: LocalDate)`, `ReconcileTodayUseCase()` — consumed by Task 13 (worker), Task 14 (CaloriesViewModel), Task 15 (HomeViewModel).

- [ ] **Step 1: The four thin wrappers**

```kotlin
package iti.grad.nutriscan.domain.dailytracking.usecase

import iti.grad.nutriscan.domain.dailytracking.model.DailyTracking
import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTodayDailyTrackingUseCase @Inject constructor(
    private val repository: IDailyTrackingRepository,
) {
    operator fun invoke(): Flow<DailyTracking> = repository.observeToday()
}
```

```kotlin
package iti.grad.nutriscan.domain.dailytracking.usecase

import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import javax.inject.Inject

class UpdateWaterCntUseCase @Inject constructor(
    private val repository: IDailyTrackingRepository,
) {
    suspend operator fun invoke(waterCnt: Int): Result<Unit> = repository.updateWaterCnt(waterCnt)
}
```

```kotlin
package iti.grad.nutriscan.domain.dailytracking.usecase

import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import javax.inject.Inject

class UpdateTargetWaterCntUseCase @Inject constructor(
    private val repository: IDailyTrackingRepository,
) {
    suspend operator fun invoke(targetWaterCnt: Int): Result<Unit> = repository.updateTargetWaterCnt(targetWaterCnt)
}
```

```kotlin
package iti.grad.nutriscan.domain.dailytracking.usecase

import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import javax.inject.Inject

class UpdateStepsCntUseCase @Inject constructor(
    private val repository: IDailyTrackingRepository,
) {
    suspend operator fun invoke(stepsCnt: Int): Result<Unit> = repository.updateStepsCnt(stepsCnt)
}
```

```kotlin
package iti.grad.nutriscan.domain.dailytracking.usecase

import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import java.time.LocalDate
import javax.inject.Inject

class SyncPendingDailyTrackingUseCase @Inject constructor(
    private val repository: IDailyTrackingRepository,
) {
    suspend operator fun invoke(date: LocalDate): Result<Unit> = repository.syncPendingDay(date)
}
```

- [ ] **Step 2: `ReconcileTodayUseCase.kt`**

Note: this references `IFoodLogRepository.addFoodEntryLocalOnly`, `FoodLogEntry`, and `ProductVerdict` — all exist by the time Task 11 lands. If implementing tasks strictly in order, this file simply won't compile until Task 11 is done; that's expected for this one file (flagged so a reviewer doesn't mistake it for a mistake). If using subagent-driven execution, land this file in the same commit as Task 11's changes, or stub-then-fill; either is fine as long as the final state matches this code.

```kotlin
package iti.grad.nutriscan.domain.dailytracking.usecase

import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import iti.grad.nutriscan.domain.foodlog.repository.IFoodLogRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

/**
 * Login/app-start reconciliation: pulls today's backend state once, seeds local
 * water/steps if Room has no row yet, and inserts any backend-known meal not already
 * present locally (matched by productId == scanId) — see
 * docs/plans/2026-07-27-daily-tracking-sync.md §"Startup/login reconciliation".
 */
class ReconcileTodayUseCase @Inject constructor(
    private val dailyTrackingRepository: IDailyTrackingRepository,
    private val foodLogRepository: IFoodLogRepository,
) {
    suspend operator fun invoke(): Result<Unit> {
        val snapshot = dailyTrackingRepository.fetchAndSeedToday().getOrElse { return Result.failure(it) }

        val localScanIds = foodLogRepository.observeTodayFoodLog().first()
            .mapNotNull { it.productId }
            .toSet()

        snapshot.meals
            .filter { it.scanId !in localScanIds }
            .forEach { meal ->
                foodLogRepository.addFoodEntryLocalOnly(
                    FoodLogEntry(
                        id = "remote-${meal.scanId}",
                        productId = meal.scanId,
                        name = meal.productName.orEmpty(),
                        calories = meal.calories,
                        imageUrl = meal.imageUrl,
                        verdict = ProductVerdict.SAFE,
                        loggedDate = snapshot.date,
                        addedAt = Instant.now(),
                    )
                )
            }

        return Result.success(Unit)
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add domain/src/main/kotlin/iti/grad/nutriscan/domain/dailytracking/usecase
git commit -m "Add daily-tracking use cases"
```

---

### Task 5: DTOs

**Files:**
- Create: `data/src/main/kotlin/iti/grad/nutriscan/data/remote/dto/DailyTrackingDto.kt`

**Interfaces:**
- Consumes: existing `iti.grad.nutriscan.data.remote.dto.NutritionFactsDto` (reused as-is, from `ScanResultResponseDto.kt` — do not create a second one).
- Produces: every DTO class below, consumed by Task 6 (API service) and Task 8 (mapper).

- [ ] **Step 1: Write the DTOs**

```kotlin
package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DailyTrackingRequestDto(
    val date: String,
    val targetWaterCnt: Int? = null,
    val waterCnt: Int? = null,
    val stepsCnt: Int? = null,
)

@Serializable
data class DailyTrackingResponseDto(
    val id: Int? = null,
    val date: String,
    val targetWaterCnt: Int? = null,
    val waterCnt: Int? = null,
    val stepsCnt: Int? = null,
    val meals: List<DailyTrackingMealResponseDto> = emptyList(),
)

@Serializable
data class DailyTrackingMealRequestDto(
    val scanId: String,
    val mealCnt: Int,
)

@Serializable
data class UpdateMealRequestDto(
    val mealCnt: Int,
)

@Serializable
data class DailyTrackingMealResponseDto(
    val scanId: String,
    val productName: String? = null,
    val imageUrl: String? = null,
    val mealCnt: Int = 1,
    val nutritionFacts: NutritionFactsDto? = null,
)

@Serializable
data class DailyTrackingSummaryResponseDto(
    val id: Int? = null,
    val date: String,
    val targetWaterCnt: Int? = null,
    val waterCnt: Int? = null,
    val stepsCnt: Int? = null,
    val mealCount: Int = 0,
)

@Serializable
data class PageDailyTrackingSummaryResponseDto(
    val content: List<DailyTrackingSummaryResponseDto> = emptyList(),
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val number: Int = 0,
)
```

- [ ] **Step 2: Commit**

```bash
git add data/src/main/kotlin/iti/grad/nutriscan/data/remote/dto/DailyTrackingDto.kt
git commit -m "Add daily-tracking DTOs"
```

---

### Task 6: DailyTrackingApiService

**Files:**
- Create: `data/src/main/kotlin/iti/grad/nutriscan/data/remote/api/DailyTrackingApiService.kt`

**Interfaces:**
- Consumes: DTOs from Task 5.
- Produces: `DailyTrackingApiService` interface, consumed by Task 9 (`DailyTrackingRepositoryImpl`) and Task 10 (DI).

- [ ] **Step 1: Write the service interface**

```kotlin
package iti.grad.nutriscan.data.remote.api

import iti.grad.nutriscan.data.remote.dto.DailyTrackingMealRequestDto
import iti.grad.nutriscan.data.remote.dto.DailyTrackingRequestDto
import iti.grad.nutriscan.data.remote.dto.DailyTrackingResponseDto
import iti.grad.nutriscan.data.remote.dto.PageDailyTrackingSummaryResponseDto
import iti.grad.nutriscan.data.remote.dto.UpdateMealRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface DailyTrackingApiService {

    @GET("api/v1/daily-tracking/today")
    suspend fun getToday(): DailyTrackingResponseDto

    @GET("api/v1/daily-tracking/{date}")
    suspend fun getByDate(@Path("date") date: String): DailyTrackingResponseDto

    @GET("api/v1/daily-tracking")
    suspend fun getHistoryPage(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): PageDailyTrackingSummaryResponseDto

    @PATCH("api/v1/daily-tracking/{date}")
    suspend fun updateDay(
        @Path("date") date: String,
        @Body body: DailyTrackingRequestDto,
    ): DailyTrackingResponseDto

    @DELETE("api/v1/daily-tracking/{date}")
    suspend fun deleteDay(@Path("date") date: String)

    @POST("api/v1/daily-tracking/{date}/meals")
    suspend fun addMeal(
        @Path("date") date: String,
        @Body body: DailyTrackingMealRequestDto,
    )

    @PUT("api/v1/daily-tracking/{date}/meals/{scanId}")
    suspend fun updateMeal(
        @Path("date") date: String,
        @Path("scanId") scanId: String,
        @Body body: UpdateMealRequestDto,
    )

    @DELETE("api/v1/daily-tracking/{date}/meals/{scanId}")
    suspend fun deleteMeal(
        @Path("date") date: String,
        @Path("scanId") scanId: String,
    )
}
```

- [ ] **Step 2: Commit**

```bash
git add data/src/main/kotlin/iti/grad/nutriscan/data/remote/api/DailyTrackingApiService.kt
git commit -m "Add DailyTrackingApiService"
```

---

### Task 7: Room entity/DAO changes

**Files:**
- Create: `data/src/main/kotlin/iti/grad/nutriscan/data/db/entity/DailyTrackingEntity.kt`
- Create: `data/src/main/kotlin/iti/grad/nutriscan/data/db/dao/DailyTrackingDao.kt`
- Modify: `data/src/main/kotlin/iti/grad/nutriscan/data/db/entity/FoodLogEntity.kt`
- Modify: `data/src/main/kotlin/iti/grad/nutriscan/data/db/dao/FoodLogDao.kt`
- Modify: `data/src/main/kotlin/iti/grad/nutriscan/data/db/NutriScanDatabase.kt`

**Interfaces:**
- Produces: `DailyTrackingEntity`, `DailyTrackingDao` (consumed by Task 9); `FoodLogEntity.pendingSync`/`.deleted` fields and `FoodLogDao.getByIdForUser`/`markDeletedForUser`/`getPendingSyncEntries`/`hardDelete`/`clearPendingSync` (consumed by Task 11 and Task 13).

- [ ] **Step 1: `DailyTrackingEntity.kt`**

```kotlin
package iti.grad.nutriscan.data.db.entity

import androidx.room.Entity

@Entity(tableName = "daily_tracking", primaryKeys = ["userId", "date"])
data class DailyTrackingEntity(
    val userId: String,
    val date: String,
    val targetWaterCnt: Int,
    val waterCnt: Int,
    val stepsCnt: Int,
    val caloriesBurnedSteps: Int,
    val syncedToBackend: Boolean,
)
```

- [ ] **Step 2: `DailyTrackingDao.kt`**

```kotlin
package iti.grad.nutriscan.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import iti.grad.nutriscan.data.db.entity.DailyTrackingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyTrackingDao {

    @Query("SELECT * FROM daily_tracking WHERE userId = :userId AND date = :date")
    fun observeByUserAndDate(userId: String, date: String): Flow<DailyTrackingEntity?>

    @Query("SELECT * FROM daily_tracking WHERE userId = :userId AND date = :date")
    suspend fun getByUserAndDate(userId: String, date: String): DailyTrackingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyTrackingEntity)

    @Query("UPDATE daily_tracking SET syncedToBackend = 1 WHERE userId = :userId AND date = :date")
    suspend fun markSynced(userId: String, date: String)

    @Query("DELETE FROM daily_tracking WHERE userId = :userId AND date = :date")
    suspend fun deleteByUserAndDate(userId: String, date: String)
}
```

- [ ] **Step 3: `FoodLogEntity.kt` — add columns**

```kotlin
package iti.grad.nutriscan.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_log")
data class FoodLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val productId: String?,
    val name: String,
    val calories: Int,
    val imageUrl: String?,
    val verdict: String,
    val loggedDate: String,
    val addedAtEpochMillis: Long,
    /** True while a POST/PUT/DELETE meal sync to the backend is still owed. */
    val pendingSync: Boolean = false,
    /** Soft-delete tombstone: true means the user removed this locally but the
     * backend DELETE hasn't been confirmed yet — kept out of observeByUserAndDate
     * results, physically removed once the sync succeeds. */
    val deleted: Boolean = false,
)
```

- [ ] **Step 4: `FoodLogDao.kt` — new queries, filter soft-deletes**

```kotlin
package iti.grad.nutriscan.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import iti.grad.nutriscan.data.db.entity.FoodLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {

    @Query("SELECT * FROM food_log WHERE userId = :userId AND loggedDate = :date AND deleted = 0 ORDER BY addedAtEpochMillis DESC")
    fun observeByUserAndDate(userId: String, date: String): Flow<List<FoodLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FoodLogEntity)

    @Query("SELECT * FROM food_log WHERE id = :id AND userId = :userId")
    suspend fun getByIdForUser(id: String, userId: String): FoodLogEntity?

    @Query("UPDATE food_log SET deleted = 1, pendingSync = 1 WHERE id = :id AND userId = :userId")
    suspend fun markDeletedForUser(id: String, userId: String)

    @Query("SELECT * FROM food_log WHERE pendingSync = 1")
    suspend fun getPendingSyncEntries(): List<FoodLogEntity>

    @Query("UPDATE food_log SET pendingSync = 0 WHERE id = :id")
    suspend fun clearPendingSync(id: String)

    @Query("DELETE FROM food_log WHERE id = :id")
    suspend fun hardDelete(id: String)
}
```

- [ ] **Step 5: `NutriScanDatabase.kt` — register the new entity/DAO, bump version**

```kotlin
package iti.grad.nutriscan.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.data.db.dao.SavedScanDao
import iti.grad.nutriscan.data.db.dao.DailyTrackingDao
import iti.grad.nutriscan.data.db.entity.FoodLogEntity
import iti.grad.nutriscan.data.db.entity.SavedScanEntity
import iti.grad.nutriscan.data.db.entity.DailyTrackingEntity

import androidx.room.TypeConverters
import iti.grad.nutriscan.data.db.converter.IntListConverter
import iti.grad.nutriscan.data.db.converter.FamilyMemberListConverter
import iti.grad.nutriscan.data.db.converter.JsonTypeConverters
import iti.grad.nutriscan.data.db.dao.UserDao
import iti.grad.nutriscan.data.db.dao.DiseaseDao
import iti.grad.nutriscan.data.db.dao.AllergyDao
import iti.grad.nutriscan.data.db.dao.ExercisesDao
import iti.grad.nutriscan.data.db.entity.UserEntity
import iti.grad.nutriscan.data.db.entity.DiseaseEntity
import iti.grad.nutriscan.data.db.entity.AllergyEntity
import iti.grad.nutriscan.data.db.entity.ExerciseEntity
import iti.grad.nutriscan.data.db.entity.ExerciseCategoryEntity
import iti.grad.nutriscan.data.db.MIGRATION_4_5

@Database(
    entities = [
        FoodLogEntity::class,
        UserEntity::class,
        DiseaseEntity::class,
        AllergyEntity::class,
        ExerciseEntity::class,
        ExerciseCategoryEntity::class,
        SavedScanEntity::class,
        DailyTrackingEntity::class,
    ],
    // Bumped 6 -> 7 for FoodLogEntity's new pendingSync/deleted columns and the new
    // daily_tracking table. Relies on fallbackToDestructiveMigration() in
    // DatabaseModule, same as the 5 -> 6 bump — this clears all local tables on upgrade.
    version = 7,
    exportSchema = false
)
@TypeConverters(IntListConverter::class, FamilyMemberListConverter::class, JsonTypeConverters::class)
abstract class NutriScanDatabase : RoomDatabase() {
    abstract fun foodLogDao(): FoodLogDao
    abstract fun userDao(): UserDao
    abstract fun diseaseDao(): DiseaseDao
    abstract fun allergyDao(): AllergyDao
    abstract fun exercisesDao(): ExercisesDao
    abstract fun savedScanDao(): SavedScanDao
    abstract fun dailyTrackingDao(): DailyTrackingDao
}
```

- [ ] **Step 6: Compile check**

Run: `./gradlew :data:compileDebugKotlin -q`
Expected: succeeds (KSP regenerates the Room schema).

- [ ] **Step 7: Commit**

```bash
git add data/src/main/kotlin/iti/grad/nutriscan/data/db
git commit -m "Add DailyTrackingEntity/DAO, soft-delete + pendingSync columns on FoodLogEntity"
```

---

### Task 8: DailyTrackingMapper

**Files:**
- Create: `data/src/main/kotlin/iti/grad/nutriscan/data/repository/mapper/DailyTrackingMapper.kt`

**Interfaces:**
- Consumes: DTOs (Task 5), `DailyTrackingEntity` (Task 7), domain models (Task 3).
- Produces: `DailyTrackingEntity.toDomain()`, `DailyTracking.toEntity(userId, syncedToBackend)`, `DailyTrackingResponseDto.toRemoteSnapshot()`, `DailyTrackingSummaryResponseDto.toDomain()` — consumed by Task 9.

- [ ] **Step 1: Write the mapper**

```kotlin
package iti.grad.nutriscan.data.repository.mapper

import iti.grad.nutriscan.data.db.entity.DailyTrackingEntity
import iti.grad.nutriscan.data.remote.dto.DailyTrackingResponseDto
import iti.grad.nutriscan.data.remote.dto.DailyTrackingSummaryResponseDto
import iti.grad.nutriscan.domain.dailytracking.model.DailyTracking
import iti.grad.nutriscan.domain.dailytracking.model.DailyTrackingRemoteSnapshot
import iti.grad.nutriscan.domain.dailytracking.model.DailyTrackingSummary
import iti.grad.nutriscan.domain.dailytracking.model.RemoteMealSnapshot
import java.time.LocalDate

fun DailyTrackingEntity.toDomain(): DailyTracking = DailyTracking(
    date = LocalDate.parse(date),
    targetWaterCnt = targetWaterCnt,
    waterCnt = waterCnt,
    stepsCnt = stepsCnt,
    caloriesBurnedSteps = caloriesBurnedSteps,
    syncedToBackend = syncedToBackend,
)

fun DailyTracking.toEntity(userId: String): DailyTrackingEntity = DailyTrackingEntity(
    userId = userId,
    date = date.toString(),
    targetWaterCnt = targetWaterCnt,
    waterCnt = waterCnt,
    stepsCnt = stepsCnt,
    caloriesBurnedSteps = caloriesBurnedSteps,
    syncedToBackend = syncedToBackend,
)

fun DailyTrackingResponseDto.toRemoteSnapshot(): DailyTrackingRemoteSnapshot = DailyTrackingRemoteSnapshot(
    date = LocalDate.parse(date),
    targetWaterCnt = targetWaterCnt ?: 0,
    waterCnt = waterCnt ?: 0,
    stepsCnt = stepsCnt ?: 0,
    meals = meals.map {
        RemoteMealSnapshot(
            scanId = it.scanId,
            productName = it.productName,
            imageUrl = it.imageUrl,
            calories = it.nutritionFacts?.calories?.toInt() ?: 0,
        )
    },
)

fun DailyTrackingSummaryResponseDto.toDomain(): DailyTrackingSummary = DailyTrackingSummary(
    date = LocalDate.parse(date),
    targetWaterCnt = targetWaterCnt ?: 0,
    waterCnt = waterCnt ?: 0,
    stepsCnt = stepsCnt ?: 0,
    mealCount = mealCount,
)
```

- [ ] **Step 2: Commit**

```bash
git add data/src/main/kotlin/iti/grad/nutriscan/data/repository/mapper/DailyTrackingMapper.kt
git commit -m "Add DailyTrackingMapper"
```

---

### Task 9: DailyTrackingRepositoryImpl

**Files:**
- Create: `data/src/main/kotlin/iti/grad/nutriscan/data/repository/DailyTrackingRepositoryImpl.kt`
- Test: `data/src/test/kotlin/iti/grad/nutriscan/data/repository/DailyTrackingRepositoryImplTest.kt`

**Interfaces:**
- Consumes: `DailyTrackingDao` (Task 7), `DailyTrackingApiService` (Task 6), `IAuthRepository.getCurrentUserId()` (existing), `IUserRepository.getUserData()` (existing, for `weightKg`), `CairoDateProvider.today()` (Task 2), mapper functions (Task 8).
- Produces: `DailyTrackingRepositoryImpl` implementing `IDailyTrackingRepository` — bound in Task 10.

- [ ] **Step 1: Write the failing test**

```kotlin
package iti.grad.nutriscan.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import iti.grad.nutriscan.data.db.dao.DailyTrackingDao
import iti.grad.nutriscan.data.db.entity.DailyTrackingEntity
import iti.grad.nutriscan.data.remote.api.DailyTrackingApiService
import iti.grad.nutriscan.data.remote.dto.DailyTrackingMealResponseDto
import iti.grad.nutriscan.data.remote.dto.DailyTrackingRequestDto
import iti.grad.nutriscan.data.remote.dto.DailyTrackingResponseDto
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.user.model.User
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

private class FakeDailyTrackingDao : DailyTrackingDao {
    private val rows = MutableStateFlow<List<DailyTrackingEntity>>(emptyList())

    override fun observeByUserAndDate(userId: String, date: String): Flow<DailyTrackingEntity?> =
        MutableStateFlow(rows.value.find { it.userId == userId && it.date == date })

    override suspend fun getByUserAndDate(userId: String, date: String): DailyTrackingEntity? =
        rows.value.find { it.userId == userId && it.date == date }

    override suspend fun upsert(entity: DailyTrackingEntity) {
        rows.value = rows.value.filterNot { it.userId == entity.userId && it.date == entity.date } + entity
    }

    override suspend fun markSynced(userId: String, date: String) {
        rows.value = rows.value.map {
            if (it.userId == userId && it.date == date) it.copy(syncedToBackend = true) else it
        }
    }

    override suspend fun deleteByUserAndDate(userId: String, date: String) {
        rows.value = rows.value.filterNot { it.userId == userId && it.date == date }
    }
}

class DailyTrackingRepositoryImplTest {

    private lateinit var dao: FakeDailyTrackingDao
    private lateinit var api: DailyTrackingApiService
    private lateinit var authRepository: IAuthRepository
    private lateinit var userRepository: IUserRepository
    private lateinit var repository: DailyTrackingRepositoryImpl

    private fun user(weightKg: Double? = 70.0) = User(
        id = "user-1",
        firstName = "Test",
        lastName = null,
        email = "test@test.com",
        gender = null,
        dateOfBirth = null,
        heightCm = null,
        weightKg = weightKg,
        diseaseIds = emptyList(),
        allergyIds = emptyList(),
    )

    @BeforeEach
    fun setup() {
        dao = FakeDailyTrackingDao()
        api = mockk()
        authRepository = mockk()
        userRepository = mockk()
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { userRepository.getUserData() } returns MutableStateFlow(user())
        repository = DailyTrackingRepositoryImpl(dao, api, authRepository, userRepository, UnconfinedTestDispatcher())
    }

    @Test
    fun `observeToday returns defaults when no row exists yet`() = runTest {
        val today = repository.observeToday().first()

        assertEquals(0, today.waterCnt)
        assertEquals(8, today.targetWaterCnt)
        assertEquals(0, today.stepsCnt)
        assertFalse(today.syncedToBackend)
    }

    @Test
    fun `updateWaterCnt persists locally and marks unsynced`() = runTest {
        repository.updateWaterCnt(3)

        val today = repository.observeToday().first()
        assertEquals(3, today.waterCnt)
        assertFalse(today.syncedToBackend)
    }

    @Test
    fun `updateStepsCnt derives caloriesBurnedSteps from the user's weight`() = runTest {
        repository.updateStepsCnt(1000)

        val today = repository.observeToday().first()
        assertEquals(1000, today.stepsCnt)
        // 1000 steps * 70kg * 0.0005 = 35
        assertEquals(35, today.caloriesBurnedSteps)
    }

    @Test
    fun `syncPendingDay calls PATCH and marks synced on success`() = runTest {
        val date = LocalDate.parse("2026-07-27")
        repository.updateWaterCnt(4)
        coEvery { api.updateDay(date.toString(), any()) } returns DailyTrackingResponseDto(date = date.toString())

        val result = repository.syncPendingDay(date)

        assertTrue(result.isSuccess)
        val today = repository.observeToday().first()
        assertTrue(today.syncedToBackend)
    }

    @Test
    fun `syncPendingDay leaves the row unsynced when the API call fails`() = runTest {
        val date = LocalDate.parse("2026-07-27")
        repository.updateWaterCnt(4)
        coEvery { api.updateDay(date.toString(), any()) } throws RuntimeException("network error")

        val result = repository.syncPendingDay(date)

        assertTrue(result.isFailure)
        val today = repository.observeToday().first()
        assertFalse(today.syncedToBackend)
    }

    @Test
    fun `fetchAndSeedToday seeds Room only when no row exists yet`() = runTest {
        coEvery { api.getToday() } returns DailyTrackingResponseDto(
            date = "2026-07-27",
            targetWaterCnt = 6,
            waterCnt = 2,
            stepsCnt = 500,
            meals = listOf(DailyTrackingMealResponseDto(scanId = "scan-1", mealCnt = 1)),
        )

        val result = repository.fetchAndSeedToday()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().meals.size)
        val today = repository.observeToday().first()
        assertEquals(2, today.waterCnt)
        assertTrue(today.syncedToBackend)
    }

    @Test
    fun `fetchAndSeedToday does not overwrite an existing local row`() = runTest {
        repository.updateWaterCnt(9)
        coEvery { api.getToday() } returns DailyTrackingResponseDto(date = "2026-07-27", waterCnt = 2)

        repository.fetchAndSeedToday()

        val today = repository.observeToday().first()
        assertEquals(9, today.waterCnt)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :data:test --tests "*.DailyTrackingRepositoryImplTest" -q`
Expected: FAIL — `DailyTrackingRepositoryImpl` unresolved reference.

- [ ] **Step 3: Write the implementation**

```kotlin
package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.DailyTrackingDao
import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.data.remote.api.DailyTrackingApiService
import iti.grad.nutriscan.data.remote.dto.DailyTrackingMealRequestDto
import iti.grad.nutriscan.data.remote.dto.DailyTrackingRequestDto
import iti.grad.nutriscan.data.remote.dto.UpdateMealRequestDto
import iti.grad.nutriscan.data.repository.mapper.toDomain
import iti.grad.nutriscan.data.repository.mapper.toEntity
import iti.grad.nutriscan.data.repository.mapper.toRemoteSnapshot
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.common.CairoDateProvider
import iti.grad.nutriscan.domain.common.runCatchingCancellable
import iti.grad.nutriscan.domain.dailytracking.model.DailyTracking
import iti.grad.nutriscan.domain.dailytracking.model.DailyTrackingRemoteSnapshot
import iti.grad.nutriscan.domain.dailytracking.model.DailyTrackingSummary
import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import iti.grad.nutriscan.domain.user.repository.IUserRepository
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
import kotlin.math.roundToInt

private const val DEFAULT_TARGET_WATER_CNT = 8
private const val DEFAULT_WEIGHT_KG = 70.0
private const val STEP_KCAL_FACTOR = 0.0005

class DailyTrackingRepositoryImpl @Inject constructor(
    private val dao: DailyTrackingDao,
    private val api: DailyTrackingApiService,
    private val authRepository: IAuthRepository,
    private val userRepository: IUserRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : IDailyTrackingRepository {

    override fun observeToday(): Flow<DailyTracking> = flow {
        val userId = resolveUserId()
        emitAll(
            dao.observeByUserAndDate(userId, CairoDateProvider.today().toString())
                .map { it?.toDomain() ?: defaultDailyTracking(CairoDateProvider.today()) }
        )
    }.flowOn(ioDispatcher)

    override suspend fun getByDate(date: LocalDate): Result<DailyTracking> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val userId = resolveUserId()
            dao.getByUserAndDate(userId, date.toString())?.toDomain() ?: defaultDailyTracking(date)
        }
    }

    override suspend fun getHistoryPage(page: Int, size: Int): Result<List<DailyTrackingSummary>> =
        withContext(ioDispatcher) {
            runCatchingCancellable { api.getHistoryPage(page, size).content.map { it.toDomain() } }
        }

    override suspend fun updateWaterCnt(waterCnt: Int): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val current = currentOrDefault()
            dao.upsert(current.copy(waterCnt = waterCnt, syncedToBackend = false).toEntity(resolveUserId()))
        }
    }

    override suspend fun updateTargetWaterCnt(targetWaterCnt: Int): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val current = currentOrDefault()
            dao.upsert(current.copy(targetWaterCnt = targetWaterCnt, syncedToBackend = false).toEntity(resolveUserId()))
        }
    }

    override suspend fun updateStepsCnt(stepsCnt: Int): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val current = currentOrDefault()
            val weightKg = userRepository.getUserData().first()?.weightKg ?: DEFAULT_WEIGHT_KG
            val caloriesBurnedSteps = (stepsCnt * weightKg * STEP_KCAL_FACTOR).roundToInt()
            dao.upsert(
                current.copy(
                    stepsCnt = stepsCnt,
                    caloriesBurnedSteps = caloriesBurnedSteps,
                    syncedToBackend = false,
                ).toEntity(resolveUserId())
            )
        }
    }

    override suspend fun deleteDay(date: LocalDate): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            api.deleteDay(date.toString())
            dao.deleteByUserAndDate(resolveUserId(), date.toString())
        }
    }

    override suspend fun pushMeal(date: LocalDate, scanId: String, mealCnt: Int): Result<Unit> =
        withContext(ioDispatcher) {
            runCatchingCancellable {
                api.addMeal(date.toString(), DailyTrackingMealRequestDto(scanId = scanId, mealCnt = mealCnt))
            }
        }

    override suspend fun updateMeal(date: LocalDate, scanId: String, mealCnt: Int): Result<Unit> =
        withContext(ioDispatcher) {
            runCatchingCancellable {
                api.updateMeal(date.toString(), scanId, UpdateMealRequestDto(mealCnt = mealCnt))
            }
        }

    override suspend fun deleteMeal(date: LocalDate, scanId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable { api.deleteMeal(date.toString(), scanId) }
    }

    override suspend fun syncPendingDay(date: LocalDate): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val userId = resolveUserId()
            val entity = dao.getByUserAndDate(userId, date.toString()) ?: return@runCatchingCancellable
            if (entity.syncedToBackend) return@runCatchingCancellable

            api.updateDay(
                date.toString(),
                DailyTrackingRequestDto(
                    date = date.toString(),
                    targetWaterCnt = entity.targetWaterCnt,
                    waterCnt = entity.waterCnt,
                    stepsCnt = entity.stepsCnt,
                ),
            )
            dao.markSynced(userId, date.toString())
        }
    }

    override suspend fun fetchAndSeedToday(): Result<DailyTrackingRemoteSnapshot> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val userId = resolveUserId()
            val response = api.getToday()
            val snapshot = response.toRemoteSnapshot()

            if (dao.getByUserAndDate(userId, snapshot.date.toString()) == null) {
                dao.upsert(
                    DailyTracking(
                        date = snapshot.date,
                        targetWaterCnt = snapshot.targetWaterCnt.takeIf { it > 0 } ?: DEFAULT_TARGET_WATER_CNT,
                        waterCnt = snapshot.waterCnt,
                        stepsCnt = snapshot.stepsCnt,
                        caloriesBurnedSteps = 0,
                        syncedToBackend = true,
                    ).toEntity(userId)
                )
            }

            snapshot
        }
    }

    private suspend fun currentOrDefault(): DailyTracking =
        dao.getByUserAndDate(resolveUserId(), CairoDateProvider.today().toString())?.toDomain()
            ?: defaultDailyTracking(CairoDateProvider.today())

    private fun defaultDailyTracking(date: LocalDate) = DailyTracking(
        date = date,
        targetWaterCnt = DEFAULT_TARGET_WATER_CNT,
        waterCnt = 0,
        stepsCnt = 0,
        caloriesBurnedSteps = 0,
        syncedToBackend = false,
    )

    private suspend fun resolveUserId(): String = authRepository.getCurrentUserId() ?: LOCAL_USER_ID

    private companion object {
        const val LOCAL_USER_ID = "local_device_user"
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :data:test --tests "*.DailyTrackingRepositoryImplTest" -q`
Expected: PASS (7 tests).

- [ ] **Step 5: Commit**

```bash
git add data/src/main/kotlin/iti/grad/nutriscan/data/repository/DailyTrackingRepositoryImpl.kt data/src/test/kotlin/iti/grad/nutriscan/data/repository/DailyTrackingRepositoryImplTest.kt
git commit -m "Add DailyTrackingRepositoryImpl"
```

---

### Task 10: DI wiring

**Files:**
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/di/NetworkModule.kt`
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/di/DatabaseModule.kt`
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/di/RepositoryModule.kt`

**Interfaces:**
- Consumes: `DailyTrackingApiService` (Task 6), `DailyTrackingDao`/`NutriScanDatabase.dailyTrackingDao()` (Task 7), `DailyTrackingRepositoryImpl`/`IDailyTrackingRepository` (Tasks 3, 9).

- [ ] **Step 1: `NetworkModule.kt` — add the API service provider**

Add after `provideScanApiService` (uses the same unnamed, authenticated `Retrofit` — the backend requires auth, unlike News/Exercises):
```kotlin
    @Provides
    @Singleton
    fun provideDailyTrackingApiService(retrofit: Retrofit): iti.grad.nutriscan.data.remote.api.DailyTrackingApiService {
        return retrofit.create(iti.grad.nutriscan.data.remote.api.DailyTrackingApiService::class.java)
    }
```
(Using the fully-qualified name inline to avoid an import-ordering diff in this heavily-edited file; a normal top-of-file `import iti.grad.nutriscan.data.remote.api.DailyTrackingApiService` alongside the existing `ScanApiService` import is equally correct — either is fine.)

- [ ] **Step 2: `DatabaseModule.kt` — provide the DAO**

Add after `provideSavedScanDao`:
```kotlin
    @Provides
    @Singleton
    fun provideDailyTrackingDao(db: NutriScanDatabase): iti.grad.nutriscan.data.db.dao.DailyTrackingDao =
        db.dailyTrackingDao()
```

- [ ] **Step 3: `RepositoryModule.kt` — bind the repository**

Add imports `iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository` and `iti.grad.nutriscan.data.repository.DailyTrackingRepositoryImpl`, then add after `bindSavedScanRepository`:
```kotlin
    @Binds
    @Singleton
    abstract fun bindDailyTrackingRepository(
        impl: DailyTrackingRepositoryImpl
    ): IDailyTrackingRepository
```

- [ ] **Step 4: Verify the DI graph compiles**

Run: `./gradlew :app:assembleDebug -q`
Expected: succeeds — confirms Hilt can construct `DailyTrackingRepositoryImpl` (all its constructor params are now bound).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/iti/grad/nutriscan/di/NetworkModule.kt app/src/main/kotlin/iti/grad/nutriscan/di/DatabaseModule.kt app/src/main/kotlin/iti/grad/nutriscan/di/RepositoryModule.kt
git commit -m "Wire DailyTracking DI graph"
```

---

### Task 11: FoodLogRepositoryImpl sync side effects

**Files:**
- Modify: `data/src/main/kotlin/iti/grad/nutriscan/data/repository/FoodLogRepositoryImpl.kt`
- Modify: `domain/src/main/kotlin/iti/grad/nutriscan/domain/foodlog/repository/IFoodLogRepository.kt`
- Modify: `data/src/test/kotlin/iti/grad/nutriscan/data/repository/FoodLogRepositoryImplTest.kt`
- Modify: `data/src/main/kotlin/iti/grad/nutriscan/data/repository/mapper/FoodLogMapper.kt` (Cairo date — folded in here since it's a one-line change to the same file family; see Task 12 for the other Cairo-date call site)

**Interfaces:**
- Consumes: `IDailyTrackingRepository.pushMeal/updateMeal/deleteMeal` (Task 3/9), `FoodLogDao.markDeletedForUser/hardDelete` (Task 7).
- Produces: `IFoodLogRepository.addFoodEntryLocalOnly(entry)` — consumed by `ReconcileTodayUseCase` (Task 4).

- [ ] **Step 1: `IFoodLogRepository.kt` — add the local-only method**

```kotlin
package iti.grad.nutriscan.domain.foodlog.repository

import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import kotlinx.coroutines.flow.Flow

interface IFoodLogRepository {
    /** Empty flow if there's no authenticated user (see IAuthRepository.getCurrentUserId). */
    fun observeTodayFoodLog(): Flow<List<FoodLogEntry>>

    /** Writes locally and best-effort pushes to the backend (see DailyTrackingRepositoryImpl.pushMeal). */
    suspend fun addFoodEntry(entry: FoodLogEntry): Result<Unit>

    /** Writes locally only — no backend push. Used by ReconcileTodayUseCase to seed entries the
     * backend already knows about, avoiding a redundant/duplicate POST. */
    suspend fun addFoodEntryLocalOnly(entry: FoodLogEntry): Result<Unit>

    suspend fun removeFoodEntry(entryId: String): Result<Unit>
}
```

- [ ] **Step 2: `FoodLogMapper.kt` — Cairo date**

```kotlin
package iti.grad.nutriscan.data.repository.mapper

import iti.grad.nutriscan.data.db.entity.FoodLogEntity
import iti.grad.nutriscan.domain.common.CairoDateProvider
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import java.time.Instant
import java.time.LocalDate

fun FoodLogEntity.toDomain(): FoodLogEntry = FoodLogEntry(
    id = id,
    productId = productId,
    name = name,
    calories = calories,
    imageUrl = imageUrl,
    verdict = runCatching { ProductVerdict.valueOf(verdict) }.getOrDefault(ProductVerdict.SAFE),
    loggedDate = LocalDate.parse(loggedDate),
    addedAt = Instant.ofEpochMilli(addedAtEpochMillis),
)

fun FoodLogEntry.toEntity(userId: String): FoodLogEntity = FoodLogEntity(
    id = id,
    userId = userId,
    productId = productId,
    name = name,
    calories = calories,
    imageUrl = imageUrl,
    verdict = verdict.name,
    loggedDate = loggedDate.toString(),
    addedAtEpochMillis = addedAt.toEpochMilli(),
)

fun today(): LocalDate = CairoDateProvider.today()
```

- [ ] **Step 3: Update the existing test's fake DAO for the new interface shape**

Replace the `FakeFoodLogDao` class and the two delete-related tests in
`FoodLogRepositoryImplTest.kt` (everything else in the file is unchanged):

```kotlin
private class FakeFoodLogDao : FoodLogDao {
    private val entries = MutableStateFlow<List<FoodLogEntity>>(emptyList())

    override fun observeByUserAndDate(userId: String, date: String): Flow<List<FoodLogEntity>> =
        MutableStateFlow(
            entries.value.filter { it.userId == userId && it.loggedDate == date && !it.deleted }
        )

    override suspend fun insert(entity: FoodLogEntity) {
        entries.value = entries.value.filterNot { it.id == entity.id } + entity
    }

    override suspend fun getByIdForUser(id: String, userId: String): FoodLogEntity? =
        entries.value.find { it.id == id && it.userId == userId }

    override suspend fun markDeletedForUser(id: String, userId: String) {
        entries.value = entries.value.map {
            if (it.id == id && it.userId == userId) it.copy(deleted = true, pendingSync = true) else it
        }
    }

    override suspend fun getPendingSyncEntries(): List<FoodLogEntity> =
        entries.value.filter { it.pendingSync }

    override suspend fun clearPendingSync(id: String) {
        entries.value = entries.value.map { if (it.id == id) it.copy(pendingSync = false) else it }
    }

    override suspend fun hardDelete(id: String) {
        entries.value = entries.value.filterNot { it.id == id }
    }
}
```

Replace `removeFoodEntry deletes a previously added entry` with:
```kotlin
    @Test
    fun `removeFoodEntry soft-deletes then hard-deletes once the backend confirms`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { dailyTrackingRepository.deleteMeal(any(), "product-1") } returns Result.success(Unit)
        repository.addFoodEntry(entry())

        val removeResult = repository.removeFoodEntry("entry-1")
        assertTrue(removeResult.isSuccess)

        val entries = repository.observeTodayFoodLog().first()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `addFoodEntry sets pendingSync when the backend push fails`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { dailyTrackingRepository.pushMeal(any(), "product-1", any()) } returns Result.failure(RuntimeException("offline"))

        repository.addFoodEntry(entry())

        assertEquals(1, dao.getPendingSyncEntries().size)
    }
```
Add `private lateinit var dailyTrackingRepository: IDailyTrackingRepository` to the test class fields, initialize it in `@BeforeEach` with `mockk()` (plus `coEvery { dailyTrackingRepository.pushMeal(any(), any(), any()) } returns Result.success(Unit)` as the default so the other, already-passing tests don't need individual stubs), pass it into the `FoodLogRepositoryImpl(...)` constructor call, and add the two imports (`iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository`, `java.time.LocalDate` is already imported).

- [ ] **Step 4: Run test to verify it fails**

Run: `./gradlew :data:test --tests "*.FoodLogRepositoryImplTest" -q`
Expected: FAIL — `FoodLogRepositoryImpl` constructor doesn't yet accept `IDailyTrackingRepository`, and `addFoodEntryLocalOnly`/soft-delete behavior don't exist yet.

- [ ] **Step 5: Write the implementation**

```kotlin
package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.data.repository.mapper.today
import iti.grad.nutriscan.data.repository.mapper.toDomain
import iti.grad.nutriscan.data.repository.mapper.toEntity
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.common.runCatchingCancellable
import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import iti.grad.nutriscan.domain.foodlog.repository.IFoodLogRepository
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
    private val dailyTrackingRepository: IDailyTrackingRepository,
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
            val userId = resolveUserId()
            // The backend's scanId is FoodLogEntry.productId (the scanned product's id) — id is a
            // locally-generated UUID, never sent to the backend. See SavedViewModel.addToFoodLog,
            // which sets id = UUID.randomUUID() and productId = the scanned product's own id.
            val scanId = entry.productId ?: entry.id
            dao.insert(entry.toEntity(userId))

            val pushResult = dailyTrackingRepository.pushMeal(entry.loggedDate, scanId, mealCnt = 1)
            if (pushResult.isFailure) {
                dao.insert(entry.toEntity(userId).copy(pendingSync = true))
            }
        }
    }

    override suspend fun addFoodEntryLocalOnly(entry: FoodLogEntry): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            dao.insert(entry.toEntity(resolveUserId()))
        }
    }

    override suspend fun removeFoodEntry(entryId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val userId = resolveUserId()
            val existing = dao.getByIdForUser(entryId, userId)
            val scanId = existing?.productId ?: entryId
            dao.markDeletedForUser(entryId, userId)

            val deleteResult = dailyTrackingRepository.deleteMeal(today(), scanId)
            if (deleteResult.isSuccess) {
                dao.hardDelete(entryId)
            }
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

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :data:test --tests "*.FoodLogRepositoryImplTest" -q`
Expected: PASS (7 tests: the 5 original plus the 2 replaced/added in Step 3).

- [ ] **Step 7: Commit**

```bash
git add data/src/main/kotlin/iti/grad/nutriscan/data/repository/FoodLogRepositoryImpl.kt domain/src/main/kotlin/iti/grad/nutriscan/domain/foodlog/repository/IFoodLogRepository.kt data/src/test/kotlin/iti/grad/nutriscan/data/repository/FoodLogRepositoryImplTest.kt data/src/main/kotlin/iti/grad/nutriscan/data/repository/mapper/FoodLogMapper.kt
git commit -m "Sync food-log adds/removes to the backend, soft-delete tombstone"
```

---

### Task 12: Cairo date in StepsRepositoryImpl

**Files:**
- Modify: `data/src/main/kotlin/iti/grad/nutriscan/data/repository/StepsRepositoryImpl.kt`

**Interfaces:**
- Consumes: `CairoDateProvider.today()` (Task 2).

- [ ] **Step 1: Swap the day-rollover check**

In `StepsRepositoryImpl.kt`, replace:
```kotlin
import java.time.LocalDate
```
with:
```kotlin
import iti.grad.nutriscan.domain.common.CairoDateProvider
```
and replace the one call site:
```kotlin
                    val today = LocalDate.now().toString()
```
with:
```kotlin
                    val today = CairoDateProvider.today().toString()
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :data:compileDebugKotlin -q`
Expected: succeeds.

- [ ] **Step 3: Commit**

```bash
git add data/src/main/kotlin/iti/grad/nutriscan/data/repository/StepsRepositoryImpl.kt
git commit -m "Steps baseline rollover uses Cairo-anchored day"
```

---

### Task 13: WorkManager sync job

**Files:**
- Create: `app/src/main/kotlin/iti/grad/nutriscan/work/DailyTrackingSyncWorker.kt`
- Create: `app/src/main/kotlin/iti/grad/nutriscan/work/DailyTrackingSyncScheduler.kt`
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/NutriScanApplication.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `app/src/test/kotlin/iti/grad/nutriscan/work/DailyTrackingSyncWorkerTest.kt`

**Interfaces:**
- Consumes: `SyncPendingDailyTrackingUseCase` (Task 4), `FoodLogDao.getPendingSyncEntries/clearPendingSync/hardDelete` (Task 7), `IDailyTrackingRepository.pushMeal/deleteMeal` (Task 3/9), `CairoDateProvider` (Task 2).

- [ ] **Step 1: Add the `app` module's test dependencies**

`app/build.gradle.kts` currently only has `testImplementation(libs.junit)` (JUnit4). Add JUnit5 + MockK +
coroutines-test + WorkManager's test artifact, matching the `data`/`domain` module test stack:
```kotlin
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("androidx.work:work-testing:2.10.0")
```
Add `tasks.withType<Test> { useJUnitPlatform() }` at the bottom of `app/build.gradle.kts` (outside the
`android { }` block, same as `data/build.gradle.kts`).

- [ ] **Step 2: Write the failing test**

```kotlin
package iti.grad.nutriscan.work

import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.data.db.entity.FoodLogEntity
import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import iti.grad.nutriscan.domain.dailytracking.usecase.SyncPendingDailyTrackingUseCase
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DailyTrackingSyncWorkerTest {

    private fun pendingEntry(id: String, deleted: Boolean) = FoodLogEntity(
        id = id,
        userId = "user-1",
        productId = id,
        name = "Test food",
        calories = 100,
        imageUrl = null,
        verdict = "SAFE",
        loggedDate = "2026-07-26",
        addedAtEpochMillis = 0L,
        pendingSync = true,
        deleted = deleted,
    )

    @Test
    fun `worker syncs yesterday and retries pending meal pushes and deletes`() = runTest {
        val syncPendingDailyTracking = mockk<SyncPendingDailyTrackingUseCase>()
        val dailyTrackingRepository = mockk<IDailyTrackingRepository>()
        val foodLogDao = mockk<FoodLogDao>()

        coEvery { syncPendingDailyTracking(any()) } returns Result.success(Unit)
        coEvery { foodLogDao.getPendingSyncEntries() } returns listOf(
            pendingEntry("push-me", deleted = false),
            pendingEntry("delete-me", deleted = true),
        )
        coEvery { dailyTrackingRepository.pushMeal(any(), "push-me", any()) } returns Result.success(Unit)
        coEvery { dailyTrackingRepository.deleteMeal(any(), "delete-me") } returns Result.success(Unit)
        coEvery { foodLogDao.clearPendingSync("push-me") } returns Unit
        coEvery { foodLogDao.hardDelete("delete-me") } returns Unit

        val worker = TestListenableWorkerBuilder<DailyTrackingSyncWorker>(ApplicationProvider.getApplicationContext())
            .setWorkerFactory(FakeWorkerFactory(syncPendingDailyTracking, dailyTrackingRepository, foodLogDao))
            .build()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify { syncPendingDailyTracking(iti.grad.nutriscan.domain.common.CairoDateProvider.today().minusDays(1)) }
        coVerify { foodLogDao.clearPendingSync("push-me") }
        coVerify { foodLogDao.hardDelete("delete-me") }
    }
}
```

Note: `TestListenableWorkerBuilder` needs a `WorkerFactory` that constructs `DailyTrackingSyncWorker`
with fakes instead of Hilt injection — write `FakeWorkerFactory` as a small private test helper in the
same file:
```kotlin
private class FakeWorkerFactory(
    private val syncPendingDailyTracking: SyncPendingDailyTrackingUseCase,
    private val dailyTrackingRepository: IDailyTrackingRepository,
    private val foodLogDao: FoodLogDao,
) : androidx.work.WorkerFactory() {
    override fun createWorker(
        appContext: android.content.Context,
        workerClassName: String,
        workerParameters: androidx.work.WorkerParameters,
    ) = DailyTrackingSyncWorker(appContext, workerParameters, syncPendingDailyTracking, dailyTrackingRepository, foodLogDao)
}
```

This test needs Robolectric to provide `ApplicationProvider`. Add
`testImplementation("org.robolectric:robolectric:4.13")` alongside the other `app` test deps from Step 1,
and add `@RunWith(RobolectricTestRunner::class)`-equivalent JUnit5 support — since this module now uses
JUnit5 (Step 1), use `@ExtendWith(RobolectricExtension::class)` is not a real thing for Robolectric+JUnit5
out of the box; instead keep this one test file on JUnit4 (`androidx.test.ext.junit.runners.AndroidJUnitRunner`
isn't needed for a pure Robolectric unit test) by annotating the class `@RunWith(RobolectricTestRunner::class)`
from `org.robolectric.RobolectricTestRunner` and importing `org.junit.Test`/`org.junit.runner.RunWith`
(JUnit4) instead of `org.junit.jupiter.api.Test` for this file only — Gradle's `useJUnitPlatform()` still
runs vintage JUnit4 tests via the JUnit Vintage engine, so add
`testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")` to `app/build.gradle.kts` as well.
Adjust the imports in the test above from `org.junit.jupiter.api.Test`/`Assertions` to `org.junit.Test`
(JUnit4) and `org.junit.Assert.assertEquals`, and add the `@RunWith(RobolectricTestRunner::class)`
annotation to the class.

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*.DailyTrackingSyncWorkerTest" -q`
Expected: FAIL — `DailyTrackingSyncWorker` unresolved reference.

- [ ] **Step 4: Write `DailyTrackingSyncWorker.kt`**

```kotlin
package iti.grad.nutriscan.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import iti.grad.nutriscan.domain.dailytracking.usecase.SyncPendingDailyTrackingUseCase
import java.time.LocalDate

@HiltWorker
class DailyTrackingSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncPendingDailyTracking: SyncPendingDailyTrackingUseCase,
    private val dailyTrackingRepository: IDailyTrackingRepository,
    private val foodLogDao: FoodLogDao,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val yesterday = iti.grad.nutriscan.domain.common.CairoDateProvider.today().minusDays(1)
        val daySyncResult = syncPendingDailyTracking(yesterday)

        val pendingMeals = foodLogDao.getPendingSyncEntries()
        var anyMealRetryFailed = false
        for (entry in pendingMeals) {
            val loggedDate = LocalDate.parse(entry.loggedDate)
            val syncResult = if (entry.deleted) {
                dailyTrackingRepository.deleteMeal(loggedDate, entry.productId ?: entry.id)
            } else {
                dailyTrackingRepository.pushMeal(loggedDate, entry.productId ?: entry.id, mealCnt = 1)
            }
            if (syncResult.isSuccess) {
                if (entry.deleted) foodLogDao.hardDelete(entry.id) else foodLogDao.clearPendingSync(entry.id)
            } else {
                anyMealRetryFailed = true
            }
        }

        return if (daySyncResult.isSuccess && !anyMealRetryFailed) Result.success() else Result.retry()
    }
}
```

(Uses `CairoDateProvider` rather than `LocalDate.now()` for consistency with every other "what day is
it" call site in this plan — the day-boundary decision in the spec was Cairo-anchored everywhere, not
just for the scheduler's trigger time.)

- [ ] **Step 5: Write `DailyTrackingSyncScheduler.kt`**

```kotlin
package iti.grad.nutriscan.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import iti.grad.nutriscan.domain.common.CairoDateProvider
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object DailyTrackingSyncScheduler {
    private const val WORK_NAME = "daily_tracking_sync"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<DailyTrackingSyncWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntilNextCairoMidnight(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun millisUntilNextCairoMidnight(): Long {
        val now = ZonedDateTime.now(CairoDateProvider.ZONE)
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(CairoDateProvider.ZONE)
        return java.time.Duration.between(now, nextMidnight).toMillis()
    }
}
```

- [ ] **Step 6: Wire Hilt's `WorkerFactory` and call the scheduler from `NutriScanApplication`**

```kotlin
package iti.grad.nutriscan

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.gif.GifDecoder
import iti.grad.nutriscan.work.DailyTrackingSyncScheduler
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class NutriScanApplication : Application(), SingletonImageLoader.Factory, Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        DailyTrackingSyncScheduler.schedule(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory())
                add(GifDecoder.Factory())
            }
            .build()
    }
}
```

- [ ] **Step 7: Disable WorkManager's default initializer in the manifest**

`Configuration.Provider` requires removing the default `androidx.startup` WorkManager initializer. Add
inside `<application>` in `app/src/main/AndroidManifest.xml`, right after the opening `<application ...>`
tag's attributes:
```xml
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge">
            <meta-data
                android:name="androidx.work.WorkManagerInitializer"
                android:value="androidx.startup"
                tools:node="remove" />
        </provider>
```

- [ ] **Step 8: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*.DailyTrackingSyncWorkerTest" -q`
Expected: PASS.

- [ ] **Step 9: Manual verification**

Run: `./gradlew :app:assembleDebug -q` then install on a device/emulator and force the job immediately
(bypassing the real midnight wait) with:
```bash
adb shell cmd jobscheduler run -f iti.grad.nutriscan <job-id-from-dumpsys-jobscheduler>
```
or simpler, temporarily change `PeriodicWorkRequestBuilder<DailyTrackingSyncWorker>(1, TimeUnit.DAYS)` to
`(15, TimeUnit.MINUTES)` (WorkManager's minimum period) locally while testing, then revert before commit.
Confirm via a REST client that `PATCH /daily-tracking/{yesterday}` actually landed.

- [ ] **Step 10: Commit**

```bash
git add app/src/main/kotlin/iti/grad/nutriscan/work app/src/main/kotlin/iti/grad/nutriscan/NutriScanApplication.kt app/src/main/AndroidManifest.xml app/src/test/kotlin/iti/grad/nutriscan/work/DailyTrackingSyncWorkerTest.kt app/build.gradle.kts gradle/libs.versions.toml
git commit -m "Add nightly WorkManager job to push unsynced water/steps and retry pending meal syncs"
```

---

### Task 14: CaloriesViewModel/State wiring

**Files:**
- Modify: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/main/calories/state/CaloriesState.kt`
- Modify: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/main/calories/viewmodel/CaloriesViewModel.kt`
- Modify: `presentation/src/test/kotlin/iti/grad/nutriscan/presentation/main/calories/CaloriesViewModelTest.kt`

**Interfaces:**
- Consumes: `ObserveTodayDailyTrackingUseCase`, `UpdateWaterCntUseCase`, `UpdateTargetWaterCntUseCase`, `UpdateStepsCntUseCase` (Task 4).

- [ ] **Step 1: `CaloriesState.kt` — water/steps defaults come from the real model now**

```kotlin
package iti.grad.nutriscan.presentation.main.calories.state

import androidx.compose.runtime.Immutable
import iti.grad.nutriscan.presentation.common.model.ProductUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class CaloriesState(
    val tdee: Int = 2350,
    val caloriesGained: Int = 0,
    val addedFoods: ImmutableList<ProductUiModel> = persistentListOf(),
    /** Food-log entry pending user confirmation before removal — gates the ConfirmationDialog. */
    val pendingRemoveFoodId: String? = null,
    val steps: Int = 0,
    val stepsGoal: Int = 10000,
    val stepsPermissionGranted: Boolean = false,
    val exerciseKcal: Int = 250,
    val exerciseMinutes: Int = 45,
    val waterConsumed: Int = 0,
    val waterGoal: Int = 8,
    val isLoading: Boolean = false,
)
```
(Only `waterConsumed`'s default changes, `0` was `4` — everything else here is unchanged; `DailyTracking`'s
own defaults, Task 9, already return `targetWaterCnt = 8` matching the existing `waterGoal = 8` default,
so no behavior changes for a brand-new user.)

- [ ] **Step 2: Run the existing test to see the target failures**

Run: `./gradlew :presentation:test --tests "*.CaloriesViewModelTest" -q`
Expected: still passes at this point (state default change alone doesn't break anything) — this step
just confirms the baseline before Step 3's ViewModel change.

- [ ] **Step 3: `CaloriesViewModel.kt` — replace in-memory water/steps with the real flow**

```kotlin
package iti.grad.nutriscan.presentation.main.calories.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.dailytracking.usecase.ObserveTodayDailyTrackingUseCase
import iti.grad.nutriscan.domain.dailytracking.usecase.UpdateStepsCntUseCase
import iti.grad.nutriscan.domain.dailytracking.usecase.UpdateTargetWaterCntUseCase
import iti.grad.nutriscan.domain.dailytracking.usecase.UpdateWaterCntUseCase
import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import iti.grad.nutriscan.domain.foodlog.usecase.ObserveTodayFoodLogUseCase
import iti.grad.nutriscan.domain.foodlog.usecase.RemoveFoodEntryUseCase
import iti.grad.nutriscan.domain.steps.usecase.CheckStepsPermissionUseCase
import iti.grad.nutriscan.domain.steps.usecase.ObserveTodayStepsUseCase
import iti.grad.nutriscan.presentation.common.model.ProductUiModel
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEffect
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEvent
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesState
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.exercises.tracker.ExercisesSharedTracker
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaloriesViewModel @Inject constructor(
    private val checkStepsPermission: CheckStepsPermissionUseCase,
    private val observeTodaySteps: ObserveTodayStepsUseCase,
    private val observeTodayFoodLog: ObserveTodayFoodLogUseCase,
    private val removeFoodEntry: RemoveFoodEntryUseCase,
    private val observeTodayDailyTracking: ObserveTodayDailyTrackingUseCase,
    private val updateWaterCnt: UpdateWaterCntUseCase,
    private val updateTargetWaterCnt: UpdateTargetWaterCntUseCase,
    private val updateStepsCnt: UpdateStepsCntUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CaloriesState())
    val state: StateFlow<CaloriesState> = _state.asStateFlow()

    private val _effect = Channel<CaloriesEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var stepsObservationJob: Job? = null

    init {
        observeFoodLog()
        observeWorkoutStats()
        observeDailyTracking()
    }

    private fun observeWorkoutStats() {
        viewModelScope.launch {
            ExercisesSharedTracker.exerciseKcal.collect { kcal ->
                _state.update { it.copy(exerciseKcal = kcal) }
            }
        }
        viewModelScope.launch {
            ExercisesSharedTracker.exerciseMinutes.collect { mins ->
                _state.update { it.copy(exerciseMinutes = mins) }
            }
        }
    }

    /** Collects today's Room-backed water/steps (offline-first, synced nightly) — see
     * DailyTrackingRepositoryImpl. Replaces the previous pure in-memory mutation. */
    private fun observeDailyTracking() {
        viewModelScope.launch {
            observeTodayDailyTracking().collect { tracking ->
                _state.update {
                    it.copy(
                        waterConsumed = tracking.waterCnt,
                        waterGoal = tracking.targetWaterCnt,
                        steps = tracking.stepsCnt,
                    )
                }
            }
        }
    }

    fun onEvent(event: CaloriesEvent) {
        when (event) {
            CaloriesEvent.AddFoodClicked -> navigate(CaloriesEffect.NavigateToSavedProducts)
            CaloriesEvent.AddExerciseClicked -> navigate(CaloriesEffect.NavigateToExercises)
            CaloriesEvent.AddWaterClicked -> addWaterCup()
            is CaloriesEvent.WaterCupClicked -> toggleWaterCup(event.index)
            is CaloriesEvent.WaterCupLongPressed -> removeWaterCup(event.index)
            CaloriesEvent.StepsCardClicked -> checkStepsAccess()
            is CaloriesEvent.StepsPermissionResult -> handleStepsPermissionResult(event.granted)
            is CaloriesEvent.FoodItemSwipedToRemove -> {
                _state.update { it.copy(pendingRemoveFoodId = event.entryId) }
            }
            CaloriesEvent.RemoveFoodConfirmed -> confirmRemoveFood()
            CaloriesEvent.RemoveFoodDismissed -> {
                _state.update { it.copy(pendingRemoveFoodId = null) }
            }
            is CaloriesEvent.FoodItemClicked -> navigate(CaloriesEffect.NavigateToProductDetail(event.product))
        }
    }

    /** Collects today's food log (Room, offline-first) and keeps addedFoods/caloriesGained in sync. */
    private fun observeFoodLog() {
        viewModelScope.launch {
            observeTodayFoodLog().collect { entries ->
                val products = entries.map { it.toProductUiModel() }.toImmutableList()
                _state.update {
                    it.copy(
                        addedFoods = products,
                        caloriesGained = entries.sumOf { entry -> entry.calories },
                    )
                }
            }
        }
    }

    private fun confirmRemoveFood() {
        val entryId = _state.value.pendingRemoveFoodId ?: return
        viewModelScope.launch {
            removeFoodEntry(entryId)
                .onFailure { navigate(CaloriesEffect.ShowSnackbar(R.string.food_log_remove_error)) }
            _state.update { it.copy(pendingRemoveFoodId = null) }
        }
    }

    /** Checks the step-counter permission and either starts live tracking or asks the screen to request it. */
    private fun checkStepsAccess() {
        viewModelScope.launch {
            if (checkStepsPermission()) {
                handleStepsPermissionResult(granted = true)
            } else {
                navigate(CaloriesEffect.RequestStepsPermission)
            }
        }
    }

    private fun handleStepsPermissionResult(granted: Boolean) {
        _state.update { it.copy(stepsPermissionGranted = granted) }
        if (granted) startObservingSteps()
    }

    /** Collects the live sensor-backed steps flow, mirrors each new value into Room via
     * [updateStepsCnt] so it's there for the nightly sync job, and still updates local state
     * directly (rather than waiting on [observeDailyTracking]'s round-trip) so the gauge feels
     * instant. */
    private fun startObservingSteps() {
        if (stepsObservationJob?.isActive == true) return
        stepsObservationJob = viewModelScope.launch {
            observeTodaySteps().collect { steps ->
                _state.update { it.copy(steps = steps) }
                updateStepsCnt(steps)
            }
        }
    }

    private fun addWaterCup() {
        viewModelScope.launch { updateTargetWaterCnt(_state.value.waterGoal + 1) }
    }

    /**
     * Only the boundary cups respond: tapping the next empty cup fills it,
     * tapping the last filled cup unfills it — every other index is a no-op,
     * so cups always fill/unfill strictly in order.
     */
    private fun toggleWaterCup(index: Int) {
        val state = _state.value
        val newWaterCnt = when {
            index == state.waterConsumed && index < state.waterGoal -> state.waterConsumed + 1
            index == state.waterConsumed - 1 && state.waterConsumed > 0 -> state.waterConsumed - 1
            else -> return
        }
        viewModelScope.launch { updateWaterCnt(newWaterCnt) }
    }

    /** Only the last cup can be deleted, same ordering rule as [toggleWaterCup]. */
    private fun removeWaterCup(index: Int) {
        val state = _state.value
        if (index != state.waterGoal - 1) return
        val newGoal = state.waterGoal - 1
        val newWaterCnt = state.waterConsumed.coerceAtMost(newGoal)
        viewModelScope.launch {
            updateTargetWaterCnt(newGoal)
            updateWaterCnt(newWaterCnt)
        }
        navigate(CaloriesEffect.ShowSnackbar(R.string.cup_removed))
    }


    private fun navigate(effect: CaloriesEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    private fun FoodLogEntry.toProductUiModel() = ProductUiModel(
        id = productId ?: id,
        productName = name,
        imageUrl = imageUrl,
        verdict = verdict,
        calories = calories.toString(),
    )
}
```

Note: `stepsGoal` (10000, the goal shown by `StepsGaugeCard`) stays exactly as it was — a hardcoded
`CaloriesState` default, not touched by this plan. The spec flagged making it "persisted/configurable"
as a nice-to-have, but no screen currently lets the user change it, so persisting it now would be
speculative (YAGNI) — leave it for whichever future plan adds a steps-goal setting.

- [ ] **Step 4: Update the test**

`CaloriesViewModelTest.kt` needs: the 4 new constructor params (mocked, matching the fake pattern already
in that file); a fake/mocked `ObserveTodayDailyTrackingUseCase` returning a `MutableStateFlow<DailyTracking>`
seeded with `DailyTracking(date = LocalDate.now(), targetWaterCnt = 8, waterCnt = 0, stepsCnt = 0,
caloriesBurnedSteps = 0, syncedToBackend = false)` by default; assertions on `AddWaterClicked`/
`WaterCupClicked`/`WaterCupLongPressed` now verify the mocked `UpdateWaterCntUseCase`/
`UpdateTargetWaterCntUseCase` were invoked with the right values (via MockK `coVerify`), instead of
asserting on `_state.value.waterConsumed` directly after the event — since the real update now round-trips
through the use case and back through `observeDailyTracking()`'s collected flow. Read the existing file
first (it wasn't re-read while writing this plan) and adapt its existing "Water" nested test class
in place, keeping its existing test names/descriptions where the assertion style allows.

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :presentation:test --tests "*.CaloriesViewModelTest" -q`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add presentation/src/main/kotlin/iti/grad/nutriscan/presentation/main/calories presentation/src/test/kotlin/iti/grad/nutriscan/presentation/main/calories/CaloriesViewModelTest.kt
git commit -m "Wire Calories screen water/steps to the real Room-backed daily tracking flow"
```

---

### Task 15: Login/app-start reconciliation

**Files:**
- Modify: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/home/viewmodel/HomeViewModel.kt`

**Interfaces:**
- Consumes: `ReconcileTodayUseCase` (Task 4).

- [ ] **Step 1: Inject and call it in `init`, mirroring the existing `fetchAndSyncProfile()` fire-and-forget call**

Add `private val reconcileTodayUseCase: ReconcileTodayUseCase` to the constructor (after
`getRecentScansUseCase`), add the import
`iti.grad.nutriscan.domain.dailytracking.usecase.ReconcileTodayUseCase`, and add this block inside
`init { }` alongside the existing `userRepository.fetchAndSyncProfile()` launch:
```kotlin
        viewModelScope.launch {
            reconcileTodayUseCase()
        }
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :presentation:compileDebugKotlin -q`
Expected: succeeds.

- [ ] **Step 3: Commit**

```bash
git add presentation/src/main/kotlin/iti/grad/nutriscan/presentation/home/viewmodel/HomeViewModel.kt
git commit -m "Reconcile today's daily-tracking state from the backend on app start"
```

---

## Final Verification

- [ ] `./gradlew test` — all new and existing tests green.
- [ ] `./gradlew :app:assembleDebug` — full app compiles, DI graph resolves.
- [ ] Manual pass (see Task 13 Step 9 and the parent spec's Definition of Done): add/remove food while
  online and offline, toggle water cups, force the nightly job, restart the app to confirm login-time
  reconciliation doesn't duplicate meals.
