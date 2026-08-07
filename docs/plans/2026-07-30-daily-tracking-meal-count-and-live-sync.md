# Daily Tracking: Real Meal Counts + Live Sync — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the gap between the already-built daily-tracking sync (see `docs/plans/2026-07-27-daily-tracking-sync.md`) and the 8 backend endpoints it targets: make `PUT /daily-tracking/{date}/meals/{scanId}` (currently dead code) the real quantity-decrement path, push water/steps/target changes to the backend immediately instead of waiting up to 6h, and stop two DTO fields from being able to crash parsing.

**Architecture:** No new files, no new layers — this is corrective work inside the existing `FoodLogRepositoryImpl` / `DailyTrackingRepositoryImpl` / `DailyTrackingSyncEngine` / `CaloriesViewModel` that Task list in the 2026-07-27 plan already built. `FoodLogEntity` gains two columns (`mealCnt`, `backendCreated`) so one row per product per day can track its true server-side count instead of one row per "add" tap. `fallbackToDestructiveMigration()` (already in place, see `NutriScanDatabase.kt:47-51`) covers the schema bump — no manual `Migration` needed.

**Tech Stack:** Kotlin, Room, Retrofit + kotlinx.serialization, JUnit5 + MockK + coroutines-test + Turbine (existing test stack, no new dependencies).

## Global Constraints

- Zero hardcoded user-facing strings — this plan adds no new UI copy. If a step tempts you to add a string, stop and check `strings.xml` (en + ar) first.
- `AppTheme.colors`/`typography`/`shapes` — not touched (no new Composables).
- Every ViewModel change needs its test file updated in the same task (`CaloriesViewModelTest`) — non-negotiable per root `AGENTS.md` §11.2.
- `domain` module has zero Android/framework imports — `FoodLogEntry.mealCnt` is a plain `Int`, no framework types.
- Every repository method returns `Result<T>` (or a `Flow`), never throws past its boundary — use `runCatchingCancellable`, matching the rest of the file.
- Commands: unit tests for a single module via `./gradlew :data:test`, `./gradlew :app:test`, `./gradlew :presentation:test`; single class via `./gradlew :<module>:test --tests "*.ClassName"`.
- Full background/context: `docs/plans/2026-07-27-daily-tracking-sync.md` and `docs/plans/2026-07-27-daily-tracking-sync-implementation.md` — read those for why the offline-first shape exists; this document only covers the delta.

---

### Task 1: FoodLogEntity + FoodLogEntry gain `mealCnt`/`backendCreated`

**Files:**
- Modify: `data/src/main/kotlin/iti/grad/nutriscan/data/db/entity/FoodLogEntity.kt`
- Modify: `domain/src/main/kotlin/iti/grad/nutriscan/domain/foodlog/model/FoodLogEntry.kt`
- Modify: `data/src/main/kotlin/iti/grad/nutriscan/data/repository/mapper/FoodLogMapper.kt`
- Modify: `data/src/main/kotlin/iti/grad/nutriscan/data/db/NutriScanDatabase.kt:52`
- Test: `data/src/test/kotlin/iti/grad/nutriscan/data/repository/mapper/FoodLogMapperTest.kt` (new file)

**Interfaces:**
- Produces: `FoodLogEntity.mealCnt: Int` (default `1`), `FoodLogEntity.backendCreated: Boolean` (default `false`); `FoodLogEntry.mealCnt: Int` (default `1`). Every later task in this plan reads/writes these two fields.

- [ ] **Step 1: Write the failing mapper test**

Create `data/src/test/kotlin/iti/grad/nutriscan/data/repository/mapper/FoodLogMapperTest.kt`:

```kotlin
package iti.grad.nutriscan.data.repository.mapper

import iti.grad.nutriscan.data.db.entity.FoodLogEntity
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class FoodLogMapperTest {

    @Test
    fun `toDomain carries mealCnt through`() {
        val entity = FoodLogEntity(
            id = "entry-1",
            userId = "user-1",
            productId = "product-1",
            name = "Apple",
            calories = 95,
            imageUrl = null,
            verdict = "SAFE",
            loggedDate = "2026-07-30",
            addedAtEpochMillis = 0L,
            mealCnt = 3,
        )

        assertEquals(3, entity.toDomain().mealCnt)
    }

    @Test
    fun `toEntity defaults backendCreated to false regardless of mealCnt`() {
        val entry = FoodLogEntry(
            id = "entry-1",
            productId = "product-1",
            name = "Apple",
            calories = 95,
            imageUrl = null,
            verdict = ProductVerdict.SAFE,
            loggedDate = LocalDate.parse("2026-07-30"),
            addedAt = Instant.EPOCH,
            mealCnt = 2,
        )

        val entity = entry.toEntity("user-1")

        assertEquals(2, entity.mealCnt)
        assertEquals(false, entity.backendCreated)
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :data:test --tests "*.FoodLogMapperTest" -q`
Expected: FAIL — `mealCnt`/`backendCreated` unresolved references on `FoodLogEntity`/`FoodLogEntry`.

- [ ] **Step 3: Add the fields**

In `data/src/main/kotlin/iti/grad/nutriscan/data/db/entity/FoodLogEntity.kt`, add after the `deleted` field:

```kotlin
    /** True server-side quantity for this product on this day — one row per (userId,
     * loggedDate, productId) now, incremented/decremented in place instead of one row per add.
     * Defaults to 1 so a fresh add starts life the same as before this column existed. */
    val mealCnt: Int = 1,
    /** True once a POST addMeal for this row has succeeded — tells the sync retry loop
     * (DailyTrackingSyncEngine) whether a pending push should replay as POST or PUT. */
    val backendCreated: Boolean = false,
```

In `domain/src/main/kotlin/iti/grad/nutriscan/domain/foodlog/model/FoodLogEntry.kt`, add after `addedAt`:

```kotlin
    val mealCnt: Int = 1,
```

- [ ] **Step 4: Update the mapper**

In `data/src/main/kotlin/iti/grad/nutriscan/data/repository/mapper/FoodLogMapper.kt`, add `mealCnt = mealCnt` to `toDomain()` and `mealCnt = mealCnt` to `toEntity()` (leave `backendCreated` unset in `toEntity()` — it keeps its `false` default, matching how `pendingSync`/`deleted` are already handled there):

```kotlin
fun FoodLogEntity.toDomain(): FoodLogEntry = FoodLogEntry(
    id = id,
    productId = productId,
    name = name,
    calories = calories,
    imageUrl = imageUrl,
    verdict = runCatching { ProductVerdict.valueOf(verdict) }.getOrDefault(ProductVerdict.SAFE),
    loggedDate = LocalDate.parse(loggedDate),
    addedAt = Instant.ofEpochMilli(addedAtEpochMillis),
    mealCnt = mealCnt,
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
    mealCnt = mealCnt,
)
```

- [ ] **Step 5: Bump the DB version**

In `data/src/main/kotlin/iti/grad/nutriscan/data/db/NutriScanDatabase.kt`, change `version = 11` to `version = 12` and extend the comment above it:

```kotlin
    // v3 -> v4: MIGRATION_3_4 (water_log/workout_log/streak). v4 -> v5: MIGRATION_4_5
    // (users.bmi/tdee). Everything after (family_members, exercises, saved_scan,
    // daily_tracking, FoodLogEntity's pendingSync/deleted/mealCnt/backendCreated columns,
    // SavedScanEntity's userId/pendingSync/deleted columns) relies on
    // fallbackToDestructiveMigration() in DatabaseModule — this clears all local tables on
    // upgrade.
    version = 12,
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew :data:test --tests "*.FoodLogMapperTest" -q`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add data/src/main/kotlin/iti/grad/nutriscan/data/db/entity/FoodLogEntity.kt data/src/main/kotlin/iti/grad/nutriscan/data/db/NutriScanDatabase.kt domain/src/main/kotlin/iti/grad/nutriscan/domain/foodlog/model/FoodLogEntry.kt data/src/main/kotlin/iti/grad/nutriscan/data/repository/mapper/FoodLogMapper.kt data/src/test/kotlin/iti/grad/nutriscan/data/repository/mapper/FoodLogMapperTest.kt
git commit -m "Add mealCnt/backendCreated to FoodLogEntity and FoodLogEntry"
```

---

### Task 2: FoodLogDao gains lookup-by-product and count-update queries

**Files:**
- Modify: `data/src/main/kotlin/iti/grad/nutriscan/data/db/dao/FoodLogDao.kt`

**Interfaces:**
- Consumes: `FoodLogEntity` from Task 1.
- Produces: `FoodLogDao.getByUserProductAndDate(userId, productId, date): FoodLogEntity?`, `FoodLogDao.updateMealCnt(id, mealCnt)`, `FoodLogDao.markBackendCreated(id)` — Task 3 (`FoodLogRepositoryImpl`) and Task 4 (`DailyTrackingSyncEngine`) call these directly.

This task has no isolated unit test of its own — `@Dao` interfaces are exercised through `FoodLogRepositoryImplTest`'s fake in Task 3, which is where behavior gets verified end-to-end. Room compiles the generated implementation at build time, so a build-and-compile check stands in for "does this query even parse" here.

- [ ] **Step 1: Add the queries**

In `data/src/main/kotlin/iti/grad/nutriscan/data/db/dao/FoodLogDao.kt`, add after `getByIdForUser`:

```kotlin
    @Query(
        "SELECT * FROM food_log WHERE userId = :userId AND productId = :productId " +
            "AND loggedDate = :date AND deleted = 0 LIMIT 1"
    )
    suspend fun getByUserProductAndDate(userId: String, productId: String, date: String): FoodLogEntity?

    @Query("UPDATE food_log SET mealCnt = :mealCnt, pendingSync = 1 WHERE id = :id")
    suspend fun updateMealCnt(id: String, mealCnt: Int)

    @Query("UPDATE food_log SET backendCreated = 1 WHERE id = :id")
    suspend fun markBackendCreated(id: String)
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :data:compileDebugKotlin -q`
Expected: succeeds (Room annotation processor validates the new `@Query` SQL against the `food_log` schema at compile time).

- [ ] **Step 3: Commit**

```bash
git add data/src/main/kotlin/iti/grad/nutriscan/data/db/dao/FoodLogDao.kt
git commit -m "Add product-lookup and mealCnt-update queries to FoodLogDao"
```

---

### Task 3: FoodLogRepositoryImpl — increment on add, decrement on remove

**Files:**
- Modify: `data/src/main/kotlin/iti/grad/nutriscan/data/repository/FoodLogRepositoryImpl.kt`
- Modify: `data/src/test/kotlin/iti/grad/nutriscan/data/repository/FoodLogRepositoryImplTest.kt`

**Interfaces:**
- Consumes: `FoodLogDao.getByUserProductAndDate/updateMealCnt/markBackendCreated` (Task 2), `IDailyTrackingRepository.updateMeal(date, scanId, mealCnt): Result<Unit>` (already exists, `DailyTrackingRepositoryImpl.kt:167-172`).
- Produces: no signature changes to `IFoodLogRepository` — `addFoodEntry`/`removeFoodEntry` keep their existing signatures, only their internals change. Safe for `SavedViewModel`/`CaloriesViewModel` callers to stay untouched.

- [ ] **Step 1: Update the fake DAO and write the failing tests**

In `data/src/test/kotlin/iti/grad/nutriscan/data/repository/FoodLogRepositoryImplTest.kt`, replace `FakeFoodLogDao` with a version implementing the two new queries, and add `dailyTrackingRepository.updateMeal` stubs to `setup()`:

```kotlin
private class FakeFoodLogDao : FoodLogDao {
    private val entries = MutableStateFlow<List<FoodLogEntity>>(emptyList())

    override fun observeByUserAndDate(userId: String, date: String): Flow<List<FoodLogEntity>> =
        MutableStateFlow(entries.value.filter { it.userId == userId && it.loggedDate == date && !it.deleted })

    override suspend fun insert(entity: FoodLogEntity) {
        entries.value = entries.value.filterNot { it.id == entity.id } + entity
    }

    override suspend fun getByIdForUser(id: String, userId: String): FoodLogEntity? =
        entries.value.find { it.id == id && it.userId == userId }

    override suspend fun getByUserProductAndDate(userId: String, productId: String, date: String): FoodLogEntity? =
        entries.value.find {
            it.userId == userId && it.productId == productId && it.loggedDate == date && !it.deleted
        }

    override suspend fun updateMealCnt(id: String, mealCnt: Int) {
        entries.value = entries.value.map {
            if (it.id == id) it.copy(mealCnt = mealCnt, pendingSync = true) else it
        }
    }

    override suspend fun markBackendCreated(id: String) {
        entries.value = entries.value.map {
            if (it.id == id) it.copy(backendCreated = true) else it
        }
    }

    override suspend fun markDeletedForUser(id: String, userId: String) {
        entries.value = entries.value.map {
            if (it.id == id && it.userId == userId) it.copy(deleted = true, pendingSync = true) else it
        }
    }

    override suspend fun getPendingSyncEntries(userId: String): List<FoodLogEntity> =
        entries.value.filter { it.pendingSync && it.userId == userId }

    override suspend fun clearPendingSync(id: String) {
        entries.value = entries.value.map {
            if (it.id == id) it.copy(pendingSync = false) else it
        }
    }

    override suspend fun hardDelete(id: String) {
        entries.value = entries.value.filterNot { it.id == id }
    }
}
```

Add `coEvery { dailyTrackingRepository.updateMeal(any(), any(), any()) } returns Result.success(Unit)` to `setup()` alongside the existing `pushMeal`/`deleteMeal` stubs.

Then add these tests at the end of the class, before the closing `}`:

```kotlin
    @Test
    fun `adding the same product twice increments mealCnt and calls updateMeal, not pushMeal again`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"

        repository.addFoodEntry(entry(id = "entry-1"))
        repository.addFoodEntry(entry(id = "entry-2"))

        val entries = repository.observeTodayFoodLog().first()
        assertEquals(1, entries.size)
        assertEquals(2, entries.first().mealCnt)
        coVerify(exactly = 1) { dailyTrackingRepository.pushMeal(any(), "product-1", 1) }
        coVerify(exactly = 1) { dailyTrackingRepository.updateMeal(any(), "product-1", 2) }
    }

    @Test
    fun `removeFoodEntry decrements mealCnt via updateMeal when the count stays above zero`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        repository.addFoodEntry(entry(id = "entry-1"))
        repository.addFoodEntry(entry(id = "entry-2"))

        val result = repository.removeFoodEntry("entry-1")

        assertTrue(result.isSuccess)
        val entries = repository.observeTodayFoodLog().first()
        assertEquals(1, entries.size)
        assertEquals(1, entries.first().mealCnt)
        coVerify(exactly = 1) { dailyTrackingRepository.updateMeal(any(), "product-1", 1) }
        coVerify(exactly = 0) { dailyTrackingRepository.deleteMeal(any(), any()) }
    }

    @Test
    fun `removeFoodEntry deletes and calls deleteMeal only once mealCnt reaches zero`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        repository.addFoodEntry(entry(id = "entry-1"))

        val result = repository.removeFoodEntry("entry-1")

        assertTrue(result.isSuccess)
        assertTrue(repository.observeTodayFoodLog().first().isEmpty())
        coVerify(exactly = 1) { dailyTrackingRepository.deleteMeal(any(), "product-1") }
    }
```

- [ ] **Step 2: Run the new tests to verify they fail**

Run: `./gradlew :data:test --tests "*.FoodLogRepositoryImplTest" -q`
Expected: FAIL — compile error (`getByUserProductAndDate`/`updateMealCnt`/`markBackendCreated` not yet overridden meaningfully in prod code, or behavior assertions fail since `addFoodEntry` doesn't increment yet).

- [ ] **Step 3: Rewrite `addFoodEntry`/`removeFoodEntry`**

In `data/src/main/kotlin/iti/grad/nutriscan/data/repository/FoodLogRepositoryImpl.kt`, replace both methods:

```kotlin
    override suspend fun addFoodEntry(entry: FoodLogEntry): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val userId = resolveUserId()
            // The backend's scanId is FoodLogEntry.productId (the scanned product's id) — id is a
            // locally-generated UUID, never sent to the backend. See SavedViewModel.addToFoodLog,
            // which sets id = UUID.randomUUID() and productId = the scanned product's own id.
            val scanId = entry.productId ?: entry.id
            val existing = dao.getByUserProductAndDate(userId, scanId, entry.loggedDate.toString())

            if (existing != null) {
                val newCnt = existing.mealCnt + 1
                dao.updateMealCnt(existing.id, newCnt)
                val pushResult = dailyTrackingRepository.updateMeal(entry.loggedDate, scanId, newCnt)
                if (pushResult.isSuccess) dao.clearPendingSync(existing.id)
            } else {
                dao.insert(entry.toEntity(userId).copy(pendingSync = true))
                val pushResult = dailyTrackingRepository.pushMeal(entry.loggedDate, scanId, mealCnt = 1)
                if (pushResult.isSuccess) {
                    dao.markBackendCreated(entry.id)
                    dao.clearPendingSync(entry.id)
                }
            }
        }
    }

    override suspend fun addFoodEntryLocalOnly(entry: FoodLogEntry): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            dao.insert(entry.toEntity(resolveUserId()))
        }.also {
            if (it.isSuccess) streakRepository.recomputeStreak()
        }
    }

    override suspend fun removeFoodEntry(entryId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val userId = resolveUserId()
            val existing = dao.getByIdForUser(entryId, userId)
            val scanId = existing?.productId ?: entryId
            val newCnt = (existing?.mealCnt ?: 1) - 1

            if (newCnt > 0) {
                dao.updateMealCnt(entryId, newCnt)
                val updateResult = dailyTrackingRepository.updateMeal(today(), scanId, newCnt)
                if (updateResult.isSuccess) dao.clearPendingSync(entryId)
            } else {
                dao.markDeletedForUser(entryId, userId)
                val deleteResult = dailyTrackingRepository.deleteMeal(today(), scanId)
                if (deleteResult.isSuccess) dao.hardDelete(entryId)
            }
        }
    }
```

(`addFoodEntryLocalOnly` is unchanged — shown for context only, don't re-type it if your editor can leave it alone.)

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :data:test --tests "*.FoodLogRepositoryImplTest" -q`
Expected: PASS (all — the 5 pre-existing tests plus the 3 new ones).

- [ ] **Step 5: Commit**

```bash
git add data/src/main/kotlin/iti/grad/nutriscan/data/repository/FoodLogRepositoryImpl.kt data/src/test/kotlin/iti/grad/nutriscan/data/repository/FoodLogRepositoryImplTest.kt
git commit -m "Increment/decrement mealCnt via PUT instead of duplicating POST/DELETE per add"
```

---

### Task 4: DailyTrackingSyncEngine retries POST vs PUT correctly

**Files:**
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/work/DailyTrackingSyncEngine.kt`
- Modify: `app/src/test/kotlin/iti/grad/nutriscan/work/DailyTrackingSyncEngineTest.kt`

**Interfaces:**
- Consumes: `FoodLogEntity.mealCnt`/`backendCreated` (Task 1), `FoodLogDao.markBackendCreated` (Task 2), `IDailyTrackingRepository.updateMeal` (existing).
- Produces: no change to `DailyTrackingSyncEngine.sync(date): Boolean`'s signature — `DailyTrackingSyncWorker` needs no changes.

- [ ] **Step 1: Update the test factory and write the failing tests**

In `app/src/test/kotlin/iti/grad/nutriscan/work/DailyTrackingSyncEngineTest.kt`, change `pendingEntry` to accept the two new fields:

```kotlin
    private fun pendingEntry(id: String, deleted: Boolean, backendCreated: Boolean = false, mealCnt: Int = 1) =
        FoodLogEntity(
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
            backendCreated = backendCreated,
            mealCnt = mealCnt,
        )
```

Add `coEvery { foodLogDao.markBackendCreated("push-me") } returns Unit` next to the existing `foodLogDao.clearPendingSync("push-me")` stub in the first test (`syncs today and retries pending meal and saved-scan pushes and deletes`), and add `coVerify { foodLogDao.markBackendCreated("push-me") }` next to its other `coVerify` calls (the entry defaults to `backendCreated = false`, so this exercises the POST-then-mark-created branch).

Then add a new test for the PUT branch, after that first test:

```kotlin
    @Test
    fun `a pending entry that already exists backend-side retries as PUT, not POST`() = runTest {
        val syncPendingDailyTracking = mockk<SyncPendingDailyTrackingUseCase>()
        val dailyTrackingRepository = mockk<IDailyTrackingRepository>()
        val foodLogDao = mockk<FoodLogDao>()
        val authRepository = mockk<IAuthRepository>()
        val savedScanRepository = mockk<ISavedScanRepository>()
        val yesterday = LocalDate.of(2026, 7, 26)

        coEvery { syncPendingDailyTracking(yesterday) } returns Result.success(Unit)
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { foodLogDao.getPendingSyncEntries("user-1") } returns listOf(
            pendingEntry("update-me", deleted = false, backendCreated = true, mealCnt = 3),
        )
        coEvery { dailyTrackingRepository.updateMeal(any(), "update-me", 3) } returns Result.success(Unit)
        coEvery { foodLogDao.clearPendingSync("update-me") } returns Unit
        coEvery { savedScanRepository.retryPendingSync() } returns Result.success(Unit)

        val engine = DailyTrackingSyncEngine(
            syncPendingDailyTracking, dailyTrackingRepository, foodLogDao, authRepository, savedScanRepository
        )
        val succeeded = engine.sync(yesterday)

        assertTrue(succeeded)
        coVerify(exactly = 0) { dailyTrackingRepository.pushMeal(any(), any(), any()) }
        coVerify { dailyTrackingRepository.updateMeal(LocalDate.of(2026, 7, 26), "update-me", 3) }
        coVerify { foodLogDao.clearPendingSync("update-me") }
        coVerify(exactly = 0) { foodLogDao.markBackendCreated(any()) }
    }
```

- [ ] **Step 2: Run to verify the new test fails**

Run: `./gradlew :app:test --tests "*.DailyTrackingSyncEngineTest" -q`
Expected: FAIL — engine still always calls `pushMeal(..., mealCnt = 1)` regardless of `backendCreated`.

- [ ] **Step 3: Update the engine's retry branch**

In `app/src/main/kotlin/iti/grad/nutriscan/work/DailyTrackingSyncEngine.kt`, replace the `for` loop body:

```kotlin
        var anyMealRetryFailed = false
        for (entry in foodLogDao.getPendingSyncEntries(userId)) {
            val loggedDate = LocalDate.parse(entry.loggedDate)
            val scanId = entry.productId ?: entry.id
            val syncResult = when {
                entry.deleted -> dailyTrackingRepository.deleteMeal(loggedDate, scanId)
                entry.backendCreated -> dailyTrackingRepository.updateMeal(loggedDate, scanId, entry.mealCnt)
                else -> dailyTrackingRepository.pushMeal(loggedDate, scanId, entry.mealCnt)
            }
            if (syncResult.isSuccess) {
                when {
                    entry.deleted -> foodLogDao.hardDelete(entry.id)
                    entry.backendCreated -> foodLogDao.clearPendingSync(entry.id)
                    else -> {
                        foodLogDao.markBackendCreated(entry.id)
                        foodLogDao.clearPendingSync(entry.id)
                    }
                }
            } else {
                anyMealRetryFailed = true
            }
        }
```

- [ ] **Step 4: Run all 5 tests to verify they pass**

Run: `./gradlew :app:test --tests "*.DailyTrackingSyncEngineTest" -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/iti/grad/nutriscan/work/DailyTrackingSyncEngine.kt app/src/test/kotlin/iti/grad/nutriscan/work/DailyTrackingSyncEngineTest.kt
git commit -m "Retry pending meal syncs as POST or PUT based on backendCreated"
```

---

### Task 5: Seed the real backend mealCnt during login/app-start reconciliation

**Files:**
- Modify: `domain/src/main/kotlin/iti/grad/nutriscan/domain/dailytracking/model/RemoteMealSnapshot.kt`
- Modify: `data/src/main/kotlin/iti/grad/nutriscan/data/repository/mapper/DailyTrackingMapper.kt`
- Modify: `domain/src/main/kotlin/iti/grad/nutriscan/domain/dailytracking/usecase/ReconcileTodayUseCase.kt`
- Modify: `data/src/test/kotlin/iti/grad/nutriscan/data/repository/DailyTrackingRepositoryImplTest.kt`

**Interfaces:**
- Consumes: `DailyTrackingMealResponseDto.mealCnt` (already exists, `DailyTrackingDto.kt:39`).
- Produces: `RemoteMealSnapshot.mealCnt: Int` — used by `ReconcileTodayUseCase` so a reinstall (or a second phone, same email) seeds the real count instead of hardcoding 1.

- [ ] **Step 1: Extend the failing test**

In `data/src/test/kotlin/iti/grad/nutriscan/data/repository/DailyTrackingRepositoryImplTest.kt`, change the `fetchAndSeedToday seeds Room only when no row exists yet` test's meal to a count above 1, and assert on it:

```kotlin
    @Test
    fun `fetchAndSeedToday seeds Room only when no row exists yet`() = runTest(testDispatcher.scheduler) {
        coEvery { api.getToday() } returns DailyTrackingResponseDto(
            date = CairoDateProvider.today().toString(),
            targetWaterCnt = 6,
            waterCnt = 2,
            stepsCnt = 500,
            meals = listOf(DailyTrackingMealResponseDto(scanId = "scan-1", mealCnt = 3)),
        )

        val result = repository.fetchAndSeedToday()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().meals.size)
        assertEquals(3, result.getOrThrow().meals.first().mealCnt)
        val today = repository.observeToday().first()
        assertEquals(2, today.waterCnt)
        assertTrue(today.syncedToBackend)
    }
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :data:test --tests "*.DailyTrackingRepositoryImplTest" -q`
Expected: FAIL — `RemoteMealSnapshot` has no `mealCnt` property.

- [ ] **Step 3: Add the field and wire the mapper**

In `domain/src/main/kotlin/iti/grad/nutriscan/domain/dailytracking/model/RemoteMealSnapshot.kt`, add:

```kotlin
data class RemoteMealSnapshot(
    val scanId: String,
    val productName: String?,
    val imageUrl: String?,
    val calories: Int,
    val mealCnt: Int,
)
```

In `data/src/main/kotlin/iti/grad/nutriscan/data/repository/mapper/DailyTrackingMapper.kt`, add `mealCnt = it.mealCnt` inside the `RemoteMealSnapshot(...)` construction in `toRemoteSnapshot()`:

```kotlin
    meals = meals.map {
        RemoteMealSnapshot(
            scanId = it.scanId,
            productName = it.productName,
            imageUrl = it.imageUrl,
            calories = it.nutritionFacts?.calories?.toInt() ?: 0,
            mealCnt = it.mealCnt,
        )
    },
```

In `domain/src/main/kotlin/iti/grad/nutriscan/domain/dailytracking/usecase/ReconcileTodayUseCase.kt`, add `mealCnt = meal.mealCnt` to the `FoodLogEntry(...)` construction:

```kotlin
                FoodLogEntry(
                    id = "remote-${meal.scanId}",
                    productId = meal.scanId,
                    name = meal.productName.orEmpty(),
                    calories = meal.calories,
                    imageUrl = meal.imageUrl,
                    verdict = ProductVerdict.SAFE,
                    loggedDate = snapshot.date,
                    addedAt = Instant.now(),
                    mealCnt = meal.mealCnt,
                )
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :data:test --tests "*.DailyTrackingRepositoryImplTest" -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add domain/src/main/kotlin/iti/grad/nutriscan/domain/dailytracking/model/RemoteMealSnapshot.kt data/src/main/kotlin/iti/grad/nutriscan/data/repository/mapper/DailyTrackingMapper.kt domain/src/main/kotlin/iti/grad/nutriscan/domain/dailytracking/usecase/ReconcileTodayUseCase.kt data/src/test/kotlin/iti/grad/nutriscan/data/repository/DailyTrackingRepositoryImplTest.kt
git commit -m "Seed the backend's real mealCnt during login/app-start reconciliation"
```

---

### Task 6: CaloriesViewModel shows mealCnt directly instead of grouping rows

**Files:**
- Modify: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/main/calories/viewmodel/CaloriesViewModel.kt`
- Modify: `presentation/src/test/kotlin/iti/grad/nutriscan/presentation/main/calories/CaloriesViewModelTest.kt`

**Interfaces:**
- Consumes: `FoodLogEntry.mealCnt` (Task 1).
- Produces: no change to `CaloriesViewModel`'s constructor or `CaloriesEvent`/`CaloriesEffect` — `ProductUiModel.quantity`/`logEntryId` keep their existing meaning (one card, badge = count), just sourced differently now that the repository guarantees one row per product per day.

- [ ] **Step 1: Rewrite the two grouping-specific tests to fail against the new behavior**

In `presentation/src/test/kotlin/iti/grad/nutriscan/presentation/main/calories/CaloriesViewModelTest.kt`, add `mealCnt` to the `foodEntry` factory:

```kotlin
    private fun foodEntry(
        id: String = "entry-1",
        calories: Int = 95,
        productId: String = "product-1",
        mealCnt: Int = 1,
    ) = FoodLogEntry(
        id = id,
        productId = productId,
        name = "Apple",
        calories = calories,
        imageUrl = null,
        verdict = ProductVerdict.SAFE,
        loggedDate = LocalDate.now(),
        addedAt = Instant.now(),
        mealCnt = mealCnt,
    )
```

Replace `logging the same product twice groups into one card with a quantity badge` with:

```kotlin
        @Test
        fun `a single entry with mealCnt above one shows the quantity badge and multiplies calories gained`() = runTest {
            val entry = foodEntry(id = "entry-1", calories = 95, mealCnt = 2)
            val vm = createViewModel(flowOf(listOf(entry)))
            testScheduler.runCurrent()

            Assertions.assertEquals(1, vm.state.value.addedFoods.size)
            val card = vm.state.value.addedFoods.first()
            Assertions.assertEquals(2, card.quantity)
            Assertions.assertEquals("entry-1", card.logEntryId)
            Assertions.assertEquals("95", card.calories)
            Assertions.assertEquals(190, vm.state.value.caloriesGained)
        }
```

Replace `swiping a grouped card targets the most recent entry and decrements the quantity` with:

```kotlin
        @Test
        fun `swiping a card with quantity above one calls removeFoodEntry and reflects the next emission`() = runTest {
            val entry = foodEntry(id = "entry-1", calories = 95, mealCnt = 2)
            val entriesFlow = MutableStateFlow(listOf(entry))
            val foodLogUseCase = mockk<ObserveTodayFoodLogUseCase>()
            every { foodLogUseCase() } returns entriesFlow
            coEvery { removeFoodEntry("entry-1") } returns Result.success(Unit)
            val vm = CaloriesViewModel(
                checkStepsPermission,
                observeTodaySteps,
                foodLogUseCase,
                removeFoodEntry,
                observeTodayDailyTracking,
                updateWaterCnt,
                updateTargetWaterCnt,
                updateStepsCnt,
                userRepository,
            )
            testScheduler.runCurrent()

            val card = vm.state.value.addedFoods.first()
            vm.onEvent(CaloriesEvent.FoodItemSwipedToRemove(card.logEntryId!!))
            vm.onEvent(CaloriesEvent.RemoveFoodConfirmed)
            testScheduler.runCurrent()

            coVerify(exactly = 1) { removeFoodEntry("entry-1") }

            // Simulate the repository's next emission once the PUT decrement is confirmed —
            // the ViewModel does no count math itself, it just re-renders what it's given.
            entriesFlow.value = listOf(entry.copy(mealCnt = 1))
            testScheduler.runCurrent()

            val remaining = vm.state.value.addedFoods.first()
            Assertions.assertEquals(1, vm.state.value.addedFoods.size)
            Assertions.assertEquals(1, remaining.quantity)
            Assertions.assertEquals("entry-1", remaining.logEntryId)
        }
```

- [ ] **Step 2: Run to verify these two fail**

Run: `./gradlew :presentation:test --tests "*.CaloriesViewModelTest" -q`
Expected: FAIL on the two rewritten tests (quantity comes out as `1` — the ViewModel still groups by row count, not `mealCnt`).

- [ ] **Step 3: Simplify `observeFoodLog` and drop the grouping function**

In `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/main/calories/viewmodel/CaloriesViewModel.kt`, replace `observeFoodLog()`:

```kotlin
    /** Collects today's food log (Room, offline-first). One row per product per day — the
     * repository increments/decrements mealCnt in place (see FoodLogRepositoryImpl) — so no
     * grouping is needed here, unlike before that guarantee existed. */
    private fun observeFoodLog() {
        viewModelScope.launch {
            observeTodayFoodLog().collect { entries ->
                val products = entries.map { it.toProductUiModel() }.toImmutableList()
                _state.update {
                    it.copy(
                        addedFoods = products,
                        caloriesGained = entries.sumOf { entry -> entry.calories * entry.mealCnt },
                    )
                }
            }
        }
    }
```

Replace `toGroupedProductUiModel()` at the bottom of the class:

```kotlin
    private fun FoodLogEntry.toProductUiModel(): ProductUiModel = ProductUiModel(
        id = productId ?: id,
        productName = name,
        imageUrl = imageUrl,
        verdict = verdict,
        calories = calories.toString(),
        quantity = mealCnt,
        logEntryId = id,
    )
```

- [ ] **Step 4: Run all CaloriesViewModelTest cases to verify they pass**

Run: `./gradlew :presentation:test --tests "*.CaloriesViewModelTest" -q`
Expected: PASS (all tests in the file, including the untouched `Water Tracking`/`Steps Tracking`/`Navigation` nested classes).

- [ ] **Step 5: Commit**

```bash
git add presentation/src/main/kotlin/iti/grad/nutriscan/presentation/main/calories/viewmodel/CaloriesViewModel.kt presentation/src/test/kotlin/iti/grad/nutriscan/presentation/main/calories/CaloriesViewModelTest.kt
git commit -m "Show mealCnt directly on the food log card instead of grouping rows"
```

---

### Task 7: Push water/steps/target changes immediately, not just nightly

**Files:**
- Modify: `data/src/main/kotlin/iti/grad/nutriscan/data/repository/DailyTrackingRepositoryImpl.kt`
- Modify: `data/src/test/kotlin/iti/grad/nutriscan/data/repository/DailyTrackingRepositoryImplTest.kt`

**Interfaces:**
- Consumes: `syncPendingDay(date): Result<Unit>` (already exists, `DailyTrackingRepositoryImpl.kt:178-195`), `repositoryScope` (already exists, `DailyTrackingRepositoryImpl.kt:58`).
- Produces: no signature changes — `updateWaterCnt`/`updateTargetWaterCnt`/`updateStepsCnt` keep returning `Result<Unit>` describing only the local write; the backend push stays fire-and-forget and never fails the caller.

- [ ] **Step 1: Write the failing tests**

Add to `data/src/test/kotlin/iti/grad/nutriscan/data/repository/DailyTrackingRepositoryImplTest.kt`:

```kotlin
    @Test
    fun `updateWaterCnt pushes to the backend immediately instead of waiting for the nightly worker`() =
        runTest(testDispatcher.scheduler) {
            val date = CairoDateProvider.today()
            coEvery { api.updateDay(date.toString(), any()) } returns DailyTrackingResponseDto(date = date.toString())

            repository.updateWaterCnt(4)
            testScheduler.runCurrent()

            coVerify(exactly = 1) { api.updateDay(date.toString(), any()) }
            val today = repository.observeToday().first()
            assertTrue(today.syncedToBackend)
        }

    @Test
    fun `updateWaterCnt keeps the row unsynced locally when the immediate push fails`() =
        runTest(testDispatcher.scheduler) {
            val date = CairoDateProvider.today()
            coEvery { api.updateDay(date.toString(), any()) } throws RuntimeException("offline")

            repository.updateWaterCnt(4)
            testScheduler.runCurrent()

            val today = repository.observeToday().first()
            assertEquals(4, today.waterCnt)
            assertFalse(today.syncedToBackend)
        }
```

- [ ] **Step 2: Run to verify the first one fails**

Run: `./gradlew :data:test --tests "*.DailyTrackingRepositoryImplTest" -q`
Expected: FAIL — `api.updateDay` is never called by `updateWaterCnt` today (only the nightly worker calls `syncPendingDay`).

- [ ] **Step 3: Fire a background push after each local write**

In `data/src/main/kotlin/iti/grad/nutriscan/data/repository/DailyTrackingRepositoryImpl.kt`, add a private helper right above `updateWaterCnt`:

```kotlin
    /** Fires [syncPendingDay] in the background right after a local water/steps/target write,
     * instead of waiting for [DailyTrackingSyncScheduler]'s next 6h run — otherwise a second
     * device on the same account could show stale data for up to 6h. Runs on [repositoryScope]
     * (not the caller's coroutine) so a slow/failed push never blocks or fails the local write;
     * [syncPendingDay] already wraps its own network call in [runCatchingCancellable], so a
     * failure here is silently retried by the nightly worker as before. */
    private fun pushDayInBackground(date: LocalDate) {
        repositoryScope.launch { syncPendingDay(date) }
    }
```

Then call it at the end of the three update methods (after the existing `dao.upsert(...)` and any streak/weight logic already there):

```kotlin
    override suspend fun updateWaterCnt(waterCnt: Int): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val current = currentOrDefault()
            dao.upsert(current.copy(waterCnt = waterCnt, syncedToBackend = false).toEntity(resolveUserId()))
            if (waterCnt > 0) streakRepository.recomputeStreak()
            pushDayInBackground(CairoDateProvider.today())
        }
    }

    override suspend fun updateTargetWaterCnt(targetWaterCnt: Int): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val current = currentOrDefault()
            dao.upsert(current.copy(targetWaterCnt = targetWaterCnt, syncedToBackend = false).toEntity(resolveUserId()))
            pushDayInBackground(CairoDateProvider.today())
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
            if (stepsCnt > 0) streakRepository.recomputeStreak()
            pushDayInBackground(CairoDateProvider.today())
        }
    }
```

(`addExerciseWorkout` is intentionally left untouched — no backend field exists for exercise yet, per the existing doc comment on `IDailyTrackingRepository.addExerciseWorkout`.)

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :data:test --tests "*.DailyTrackingRepositoryImplTest" -q`
Expected: PASS (all — including the pre-existing `updateWaterCnt persists locally and marks unsynced` test, since with `api.updateDay` unstubbed there it throws inside the background push, gets swallowed by `runCatchingCancellable` inside `syncPendingDay`, and the row correctly stays unsynced).

- [ ] **Step 5: Commit**

```bash
git add data/src/main/kotlin/iti/grad/nutriscan/data/repository/DailyTrackingRepositoryImpl.kt data/src/test/kotlin/iti/grad/nutriscan/data/repository/DailyTrackingRepositoryImplTest.kt
git commit -m "Push water/steps/target changes immediately instead of waiting for the 6h worker"
```

---

### Task 8: Fix the two DTO fields that can crash parsing

**Files:**
- Modify: `data/src/main/kotlin/iti/grad/nutriscan/data/remote/dto/DailyTrackingDto.kt`
- Modify: `data/src/main/kotlin/iti/grad/nutriscan/data/repository/mapper/DailyTrackingMapper.kt`
- Modify: `data/src/test/kotlin/iti/grad/nutriscan/data/repository/DailyTrackingRepositoryImplTest.kt`

**Interfaces:**
- Produces: `DailyTrackingResponseDto.date: String?`, `DailyTrackingSummaryResponseDto.date: String?`, `DailyTrackingMealResponseDto.scanId: String?`, `DailyTrackingMealResponseDto.mealCnt: Int?`, `DailyTrackingSummaryResponseDto.mealCount: Int?` (all nullable, default `null`) — `toRemoteSnapshot()`/`toDomain()` become the only code that has to handle the null case.

Note on scope: `targetWaterCnt`/`waterCnt`/`stepsCnt`/`id` are already nullable with `?: 0` mapper fallbacks (unaffected by this task). `mealCnt`/`mealCount` currently have non-null `Int` defaults (`= 1`/`= 0`) — those defaults already cover a backend that *omits* the key, but not one that sends the key with an explicit JSON `null`, which is the gap this task closes for every remaining field, per the "make every field nullable" call.

- [ ] **Step 1: Write the failing tests**

Add to `data/src/test/kotlin/iti/grad/nutriscan/data/repository/DailyTrackingRepositoryImplTest.kt`:

```kotlin
    @Test
    fun `fetchAndSeedToday falls back to today's date when the backend omits it`() =
        runTest(testDispatcher.scheduler) {
            coEvery { api.getToday() } returns DailyTrackingResponseDto(date = null, waterCnt = 2)

            val result = repository.fetchAndSeedToday()

            assertTrue(result.isSuccess)
            assertEquals(CairoDateProvider.today(), result.getOrThrow().date)
        }

    @Test
    fun `fetchAndSeedToday drops meals with a null scanId instead of failing to parse`() =
        runTest(testDispatcher.scheduler) {
            coEvery { api.getToday() } returns DailyTrackingResponseDto(
                date = CairoDateProvider.today().toString(),
                meals = listOf(
                    DailyTrackingMealResponseDto(scanId = null, mealCnt = 1),
                    DailyTrackingMealResponseDto(scanId = "scan-1", mealCnt = 2),
                ),
            )

            val result = repository.fetchAndSeedToday()

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrThrow().meals.size)
            assertEquals("scan-1", result.getOrThrow().meals.first().scanId)
        }

    @Test
    fun `fetchAndSeedToday falls back to mealCnt 1 when the backend sends it as explicit null`() =
        runTest(testDispatcher.scheduler) {
            coEvery { api.getToday() } returns DailyTrackingResponseDto(
                date = CairoDateProvider.today().toString(),
                meals = listOf(DailyTrackingMealResponseDto(scanId = "scan-1", mealCnt = null)),
            )

            val result = repository.fetchAndSeedToday()

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrThrow().meals.first().mealCnt)
        }
```

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew :data:test --tests "*.DailyTrackingRepositoryImplTest" -q`
Expected: FAIL — `DailyTrackingResponseDto(date = null, ...)` doesn't compile yet (`date` is non-nullable `String`).

- [ ] **Step 3: Make the two fields nullable**

In `data/src/main/kotlin/iti/grad/nutriscan/data/remote/dto/DailyTrackingDto.kt`, change:

```kotlin
@Serializable
data class DailyTrackingResponseDto(
    val id: Int? = null,
    val date: String? = null,
    val targetWaterCnt: Int? = null,
    val waterCnt: Int? = null,
    val stepsCnt: Int? = null,
    val meals: List<DailyTrackingMealResponseDto> = emptyList(),
)
```

```kotlin
@Serializable
data class DailyTrackingMealResponseDto(
    val scanId: String? = null,
    val productName: String? = null,
    val imageUrl: String? = null,
    val mealCnt: Int? = null,
    val nutritionFacts: NutritionFactsDto? = null,
)
```

```kotlin
@Serializable
data class DailyTrackingSummaryResponseDto(
    val id: Int? = null,
    val date: String? = null,
    val targetWaterCnt: Int? = null,
    val waterCnt: Int? = null,
    val stepsCnt: Int? = null,
    val mealCount: Int? = null,
)
```

(`DailyTrackingRequestDto.date` stays non-nullable — that's the outgoing PATCH body, where the app always knows the date it's sending.)

- [ ] **Step 4: Update the mapper's null handling**

In `data/src/main/kotlin/iti/grad/nutriscan/data/repository/mapper/DailyTrackingMapper.kt`, replace `toRemoteSnapshot()` and `toDomain()`:

```kotlin
fun DailyTrackingResponseDto.toRemoteSnapshot(): DailyTrackingRemoteSnapshot = DailyTrackingRemoteSnapshot(
    date = date?.let { LocalDate.parse(it) } ?: CairoDateProvider.today(),
    targetWaterCnt = targetWaterCnt ?: 0,
    waterCnt = waterCnt ?: 0,
    stepsCnt = stepsCnt ?: 0,
    meals = meals.mapNotNull { meal ->
        val scanId = meal.scanId ?: return@mapNotNull null
        RemoteMealSnapshot(
            scanId = scanId,
            productName = meal.productName,
            imageUrl = meal.imageUrl,
            calories = meal.nutritionFacts?.calories?.toInt() ?: 0,
            mealCnt = meal.mealCnt ?: 1,
        )
    },
)

fun DailyTrackingSummaryResponseDto.toDomain(): DailyTrackingSummary = DailyTrackingSummary(
    date = date?.let { LocalDate.parse(it) } ?: CairoDateProvider.today(),
    targetWaterCnt = targetWaterCnt ?: 0,
    waterCnt = waterCnt ?: 0,
    stepsCnt = stepsCnt ?: 0,
    mealCount = mealCount ?: 0,
)
```

Add the import needed for `CairoDateProvider`:

```kotlin
import iti.grad.nutriscan.domain.common.CairoDateProvider
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :data:test --tests "*.DailyTrackingRepositoryImplTest" -q`
Expected: PASS (all tests in the file — the three new ones plus every earlier test in this plan, since existing calls like `DailyTrackingResponseDto(date = date.toString())` or `DailyTrackingMealResponseDto(scanId = "scan-1", mealCnt = 1)` still compile fine against nullable parameters — a non-null literal always satisfies a nullable type).

- [ ] **Step 6: Run the full data module test suite once, to catch anything this plan's earlier tasks touched**

Run: `./gradlew :data:test -q`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add data/src/main/kotlin/iti/grad/nutriscan/data/remote/dto/DailyTrackingDto.kt data/src/main/kotlin/iti/grad/nutriscan/data/repository/mapper/DailyTrackingMapper.kt data/src/test/kotlin/iti/grad/nutriscan/data/repository/DailyTrackingRepositoryImplTest.kt
git commit -m "Make date/scanId nullable in DailyTracking DTOs so a sparse response never fails to parse"
```

---

### Task 9: Full-repo verification

**Files:** none (verification only)

- [ ] **Step 1: Run every touched module's test suite**

Run: `./gradlew :domain:test :data:test :app:test :presentation:test -q`
Expected: PASS across all four modules.

- [ ] **Step 2: Run lint**

Run: `./gradlew lint -q`
Expected: no new warnings introduced by this plan (existing baseline warnings, if any, are out of scope).

- [ ] **Step 3: Assemble the debug APK to catch any Room schema/annotation-processing issue the unit tests can't see**

Run: `./gradlew assembleDebug -q`
Expected: BUILD SUCCESSFUL — confirms the `mealCnt`/`backendCreated` column additions and the DB version bump compile cleanly through Room's KSP processor.
