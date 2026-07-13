# NUTRISCAN AI — `data` Module — AI Agent Rules

> **MANDATORY:** Read this file completely before touching any file under `data/`.
> This is a **scoped extension** of the root `AGENTS.md` — every rule in the root file
> still applies. This file exists so an agent working exclusively inside `data/` has
> everything it needs without re-reading the full 2000+ line project contract.
> If anything here ever appears to conflict with the root `AGENTS.md`, the root file wins —
> flag the discrepancy instead of silently picking one.

---

## 0. Module Identity

| Field           | Value                                                                 |
|------------------|------------------------------------------------------------------------|
| **Module**      | `data`                                                                |
| **Package root**| ` iti.grad.nutriscan.data`                                            |
| **Type**        | Android library module                                               |
| **Role**        | Implements `domain` repository interfaces; owns all I/O — Room, Retrofit, DataStore |
| **Consumed by** | `app` only (for Hilt `@Module` binding) — **never** by `presentation` |

---

## ⚠️ 1. Read This First — Health & Safety Data Lives Here

This module is where the app's most safety-critical data physically lives:
`HealthProfileEntity`, `HealthProfileDto`, `FamilyMemberEntity`, and every mapper that
touches them. The root domain notice applies with extra force inside this module:

- **Never silently default, truncate, or drop a condition/allergy field** in any
  Entity↔Domain or DTO↔Domain mapper — a nullable field collapsing to `null` or an
  empty list here is a **severity-1 bug**, not a style issue.
- **Verdicts must be grounded in a complete, current profile.** If a repository method
  that feeds the scan/verdict pipeline receives a stale or incomplete profile, it must
  surface a validation error — it must never let the caller silently get a "safe" verdict
  built on missing data.
- **Receipt OCR match failures must not be papered over.** Any `ReceiptLineItem` with
  `matchConfidence < 0.7` is mapped through as-is (unrecognized) — a repository/mapper
  must never invent a verdict for it.
- **Auth token, health profile data, and session state never go into Preferences
  DataStore.** Proto DataStore only (see §6.3).

An agent that writes a mapper or repository method that swallows, defaults, or drops
health/allergy data must stop and flag it — this is rejected in review with no exceptions.

---

## 2. Position in the Architecture — Hard Dependency Rules

```
       ┌─────────────────────────────────────┐
       │               app                   │  ← only module seeing both data & presentation
       └───────────┬─────────────┬───────────┘
                   ▼             ▼
           presentation        data          ← you are here
                   │             │
                   └──────┬──────┘
                          ▼
                        domain           ← knows nothing; pure Kotlin
```

- `data` imports **`domain` only**. No imports from `presentation`, ever.
- `data` implements `domain` repository interfaces (`IScanRepository`,
  `IHealthProfileRepository`, etc.) — it never defines its own repository contracts.
- **Domain Models never enter DTOs or Entities**, and DTOs/Entities **never cross the
  repository boundary** — every public repository function returns a `domain` model
  (or `Result<domain model>` / `Flow<domain model>`), never a `Dto` or `Entity`.
- Dispatchers (`@IoDispatcher`, `@DefaultDispatcher`) are injected **exclusively** at
  the `RepositoryImpl` / `DataSourceImpl` level in this module — nowhere else in the
  codebase should a dispatcher be injected.

---

## 3. Folder Structure — STRICTLY ENFORCED

```
data/src/main/kotlin/ iti.grad.nutriscan.data/
├── db/
│   ├── NutriScanDatabase.kt
│   ├── entity/
│   │   ├── ScanHistoryEntity.kt
│   │   ├── HealthProfileEntity.kt
│   │   ├── FamilyMemberEntity.kt
│   │   ├── ShoppingListItemEntity.kt
│   │   └── WeeklyReportEntity.kt
│   └── dao/
│       ├── ScanHistoryDao.kt
│       ├── HealthProfileDao.kt
│       ├── FamilyMemberDao.kt
│       ├── ShoppingListDao.kt
│       └── WeeklyReportDao.kt
├── di/
│   └── DispatcherQualifiers.kt
├── local/
│   └── datasource/
│       ├── IOnboardingPreferencesDataSource.kt
│       ├── OnboardingPreferencesDataSourceImpl.kt
│       ├── IScanHistoryLocalDataSource.kt
│       └── ScanHistoryLocalDataSourceImpl.kt
├── remote/
│   ├── api/
│   │   ├── ScanApiService.kt
│   │   ├── ProfileApiService.kt
│   │   ├── ReportApiService.kt
│   │   └── ShoppingListApiService.kt
│   ├── datasource/
│   │   ├── IScanRemoteDataSource.kt
│   │   ├── ScanRemoteDataSourceImpl.kt
│   │   ├── IProfileRemoteDataSource.kt
│   │   ├── ProfileRemoteDataSourceImpl.kt
│   │   ├── IReportRemoteDataSource.kt
│   │   └── ReportRemoteDataSourceImpl.kt
│   ├── dto/
│   │   ├── ScanResultDto.kt
│   │   ├── IngredientDto.kt
│   │   ├── SafeAlternativeDto.kt
│   │   ├── HealthProfileDto.kt
│   │   ├── FamilyMemberDto.kt
│   │   ├── ReceiptAnalysisDto.kt
│   │   ├── NutriGptMessageDto.kt
│   │   └── WeeklyReportDto.kt
│   └── interceptor/
│       ├── AuthInterceptor.kt
│       ├── ErrorInterceptor.kt
│       └── LoggingInterceptor.kt        ← DEBUG builds only
└── repository/
    ├── ScanRepositoryImpl.kt
    ├── HealthProfileRepositoryImpl.kt
    ├── NutriGptRepositoryImpl.kt
    ├── ReceiptRepositoryImpl.kt
    ├── ShoppingListRepositoryImpl.kt
    └── ReportRepositoryImpl.kt
```

**Placement rules:**
- A new persisted table → `db/entity/` + matching DAO in `db/dao/` + register in
  `NutriScanDatabase.kt`'s `entities` list.
- A new server endpoint → `remote/api/<Feature>ApiService.kt` (suspend functions only).
- A new remote-backed feature → interface in `remote/datasource/I<Feature>RemoteDataSource.kt`
  + impl in the same folder.
- A new wire payload → `remote/dto/<Name>Dto.kt`, paired with a mapper (see §8).
- Every `domain` repository interface gets exactly one `*RepositoryImpl.kt` in `repository/`.
- Do not invent new top-level folders under `data/` without discussion (§10 of this file).

---

## 4. Room Database

```kotlin
// data/db/NutriScanDatabase.kt
@Database(
    entities = [
        ScanHistoryEntity::class,
        HealthProfileEntity::class,
        FamilyMemberEntity::class,
        ShoppingListItemEntity::class,
        WeeklyReportEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class NutriScanDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun healthProfileDao(): HealthProfileDao
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun weeklyReportDao(): WeeklyReportDao
}
```

- **Annotation processing is KSP only** — `ksp("androidx.room:room-compiler:$roomVersion")`.
  `kapt` is banned; do not add a `kapt` block anywhere in `data/build.gradle.kts`.
- Every DAO method is **either `suspend fun` or returns `Flow<>`** — no blocking calls,
  no `LiveData`.
- `exportSchema = true` — schema changes are tracked; do not disable this.
- Room is bound as `@Singleton` via Hilt (the `@Provides` module itself lives in
  `app/di/DatabaseModule.kt` — `data` only defines the `@Database` class and DAOs).

---

## 5. Local Preferences & Proto DataStore

| Storage                | Use for                                                        | Never store here                          |
|-------------------------|-----------------------------------------------------------------|--------------------------------------------|
| **Preferences DataStore** | Selected language, notification toggles — simple key-value    | `auth_token`, health profile data, session state |
| **Proto DataStore**       | Auth token, user id, `has_profile` flag, locale — structured, must survive process death | —                                          |

```kotlin
class AppPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val selectedLanguage: Flow<String> = dataStore.data.map { it[LANGUAGE_KEY] ?: "ar" }

    suspend fun setLanguage(code: String) {
        dataStore.edit { it[LANGUAGE_KEY] = code }
    }

    companion object {
        val LANGUAGE_KEY = stringPreferencesKey("language_code")
    }
}
```

```proto
// data/src/main/proto/user_preferences.proto
syntax = "proto3";
option java_package = " iti.grad.nutriscan.data.local.proto";

message UserPreferences {
    string auth_token      = 1;
    string user_id         = 2;
    bool   has_profile     = 3;    // used by SplashViewModel to decide start destination
    string locale          = 4;
}
```

`SplashViewModel` reads `has_profile` from this store (via the domain-layer contract) to
decide the app's start destination — do not duplicate this flag anywhere else.

---

## 6. Network Layer

### 6.1 API Services — suspend only

```kotlin
interface ScanApiService {
    @Multipart
    @POST("scan/label")
    suspend fun submitLabelImage(
        @Part image: MultipartBody.Part,
        @Part("profile_ids") profileIds: RequestBody,
    ): ScanResultDto

    @Multipart
    @POST("scan/receipt")
    suspend fun submitReceiptImage(
        @Part image: MultipartBody.Part,
        @Part("profile_ids") profileIds: RequestBody,
    ): ReceiptAnalysisDto
}

interface ProfileApiService {
    @GET("profiles")
    suspend fun getHealthProfiles(): List<HealthProfileDto>

    @POST("profiles")
    suspend fun saveHealthProfile(@Body dto: HealthProfileDto): HealthProfileDto
}
```

- No `Call<T>` return types anywhere — suspend functions only.
- Serialization is `kotlinx.serialization` exclusively — no `Gson`, no `Moshi`.
- Retrofit converter: `retrofit2-kotlinx-serialization-converter`.

### 6.2 Interceptor Stack — fixed execution order

```
1. AuthInterceptor    → adds Bearer token to every request
2. ErrorInterceptor   → maps HTTP codes to typed domain exceptions
3. LoggingInterceptor → DEBUG builds only (Level.BODY)
```

```kotlin
// data/remote/interceptor/AuthInterceptor.kt
class AuthInterceptor @Inject constructor(
    private val authTokenProvider: IAuthTokenProvider,   // reads from Proto DataStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { authTokenProvider.getToken() }
        val request = chain.request().newBuilder().apply {
            if (token != null) header("Authorization", "Bearer $token")
        }.build()
        return chain.proceed(request)
    }
}
```

```kotlin
// data/remote/interceptor/ErrorInterceptor.kt
sealed class NutriScanHttpException(message: String) : IOException(message)
class UnauthorizedException : NutriScanHttpException("401 — token invalid or expired")
class ForbiddenException    : NutriScanHttpException("403 — insufficient permissions")
class NotFoundException     : NutriScanHttpException("404 — resource not found")
class OcrLowConfidenceException : NutriScanHttpException("422 — OCR confidence too low; retake required")
class ServerException(code: Int) : NutriScanHttpException("5xx Server Error: $code")

class ErrorInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        when (response.code) {
            401         -> throw UnauthorizedException()
            403         -> throw ForbiddenException()
            404         -> throw NotFoundException()
            422         -> throw OcrLowConfidenceException()
            in 500..599 -> throw ServerException(response.code)
        }
        return response
    }
}
```

> ⚠️ **OCR low-confidence (422) is a special domain error, not a generic failure.**
> A `*RepositoryImpl` catching `OcrLowConfidenceException` must map it to
> `DomainError.OcrLowConfidence` before returning — never let it surface as an
> undifferentiated network error. The Result Screen depends on this distinction to
> show "retake photo" instead of a generic error state.

### 6.3 Secrets & Timeouts

- Base URL comes from `local.properties` → `BuildConfig.NUTRISCAN_BASE_URL`.
  `local.properties` is **never committed to git**.
- `OkHttpClient` connect/read timeouts are **30s** — the OCR+LLM pipeline can take 2–4s
  and needs headroom; do not shrink this without discussion.

---

## 7. Dispatcher Injection — This Is Where It Happens

Dispatchers are injected **only** in this module, at the `RepositoryImpl` /
`DataSourceImpl` level — never in `domain` (UseCases) or `presentation` (ViewModels).

```kotlin
// data/di/DispatcherQualifiers.kt
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class IoDispatcher
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class DefaultDispatcher
```

```kotlin
class ScanRepositoryImpl @Inject constructor(
    private val localDataSource: IScanHistoryLocalDataSource,
    private val remoteDataSource: IScanRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,   // ✅ injected here only
) : IScanRepository {

    override suspend fun analyzeLabelImage(imageUri: String, profileIds: List<String>): Result<ScanResult> =
        withContext(ioDispatcher) {
            runCatchingCancellable {
                val dto = remoteDataSource.submitLabelImage(imageUri, profileIds)
                dto.toDomain()
            }
        }
}
```

The actual `@Provides` for `Dispatchers.IO` / `Dispatchers.Default` live in
`app/di/DispatcherModule.kt` — this module only declares the qualifiers and consumes them.

---

## 8. Exception Handling — Mandatory Pattern

> ⚠️ **Never use bare `runCatching { }` in a suspend function.** It catches
> `CancellationException` too, silently breaking structured concurrency.

```kotlin
// domain/common/RunCatchingCancellable.kt — defined in domain, used everywhere in data
suspend fun <T> runCatchingCancellable(block: suspend () -> T): Result<T> =
    runCatching { block() }.also { result ->
        result.exceptionOrNull()?.let { e ->
            if (e is CancellationException) throw e   // re-propagate cancellation
        }
    }
```

```kotlin
// ✅ Every suspend repository function follows this shape
override suspend fun analyzeLabelImage(imageUri: String, profileIds: List<String>): Result<ScanResult> =
    withContext(ioDispatcher) {
        runCatchingCancellable {
            remoteDataSource.submitLabelImage(imageUri, profileIds).toDomain()
        }
    }

// ✅ Flow chains use .catch — never runCatching inside a flow builder
fun observeScanHistory(): Flow<List<ScanHistoryEntry>> = scanHistoryDao
    .observeHistory()
    .map { it.map { entity -> entity.toDomain() } }
    .catch { e ->
        Timber.e(e, "Scan history observation error")
        emit(emptyList())
    }

// ❌ BANNED
runCatching { remoteDataSource.submitLabelImage(imageUri, profileIds) }   // bare runCatching
try { ... } catch (e: Exception) { /* empty */ }                          // silent catch
```

---

## 9. Offline-First / SSOT — Repository Pattern

Room is the single source of truth for any feature that persists data (Scan History,
Shopping List, Reports). The UI always collects from Room; remote calls only refresh it.

```kotlin
// ✅ CORRECT — channelFlow: local emits immediately; remote refresh is concurrent
class ScanHistoryRepositoryImpl @Inject constructor(
    private val localDs: IScanHistoryLocalDataSource,
    private val remoteDs: IScanRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : IScanHistoryRepository {

    override fun observeScanHistory(): Flow<List<ScanHistoryEntry>> = channelFlow {
        launch {
            runCatchingCancellable { remoteDs.fetchScanHistory() }
                .onSuccess { dtos -> localDs.cacheScanHistory(dtos.map { it.toEntity() }) }
        }
        localDs.observeScanHistory()
            .map { list -> list.map { it.toDomain() } }
            .collect { send(it) }
    }.flowOn(ioDispatcher)
}

// ❌ WRONG — onStart blocks local emission until the network call finishes
override fun observeScanHistory(): Flow<List<ScanHistoryEntry>> =
    localDs.observeScanHistory()
        .map { it.map { entity -> entity.toDomain() } }
        .onStart { refreshFromRemote() }   // ← blocks; emit is delayed
```

**Rules:**
- Network failure while offline → serve cached Room data silently, don't propagate the
  error up through the observed `Flow`.
- **Scan results are written to Room immediately on success** — before the user taps
  "Save". "Save" is a separate, explicit bookmark action on top of this auto-persist.
  Losing a scan to a crash after analysis but before an explicit save is not acceptable.

---

## 10. Mapper Conventions

- Every `Dto` gets a `toDomain()` extension mapping to its `domain` model.
- Every `Entity` gets a `toDomain()` extension, and every `domain` model destined for
  Room gets a `toEntity()` extension.
- Mapper functions are the enforcement point for §1 above — when writing a mapper for
  `HealthProfileDto`/`HealthProfileEntity` or `FamilyMemberDto`/`FamilyMemberEntity`,
  every condition/allergy field must map through explicitly. Do not use a bare `?:`
  default that silently turns "unknown" into "none."
- Domain models never appear inside a `Dto` or `Entity` class, and `Dto`/`Entity` types
  never leak out of `repository/` into `domain` or `presentation`.

---

## 11. Testing — Data Layer Target: 90%

| What to test                                   | Tools                     |
|-------------------------------------------------|---------------------------|
| Repository SSOT logic (local-first, refresh)    | JUnit5 + MockK + Turbine  |
| DTO → Domain and Entity → Domain mappers        | JUnit5                    |
| DAO operations                                  | JUnit5 (in-memory Room)   |

```
data/src/test/
├── repository/
│   ├── ScanRepositoryImplTest.kt
│   └── HealthProfileRepositoryImplTest.kt
├── datasource/
└── mapper/
    ├── ScanResultMapperTest.kt
    └── HealthProfileMapperTest.kt
```

- Use `TestCoroutineRule` (`UnconfinedTestDispatcher`) at the test boundary — production
  dispatcher-injection code never changes for tests.
- Prefer **fakes** for the local/remote data source interfaces a `*RepositoryImpl` test
  depends on (deterministic, no magic); MockK is acceptable for one-off collaborators.
- A `*RepositoryImpl` implementing a repository interface consumed by `presentation`
  tests should have a corresponding `Fake*Repository` available for those tests —
  keep its behavior in sync with the real impl (e.g. `FakeScanRepository` must reflect
  real error cases like `OcrLowConfidenceException`).

---

## 12. Code Quality — Prohibited Patterns in `data/`

| Pattern                                        | Use Instead                                       |
|-------------------------------------------------|-----------------------------------------------------|
| `kapt`                                          | KSP                                                |
| `Gson` / `Moshi`                               | `kotlinx.serialization`                            |
| `RxJava` / `RxKotlin`                          | Kotlin Flow                                        |
| `android.util.Log` / `println`                 | `Timber.d()` / `Timber.e()`                        |
| Bare `runCatching` in suspend functions         | `runCatchingCancellable`                           |
| Empty `catch` blocks                            | At minimum log with Timber                         |
| `!!` without a comment                          | `?: return`, `let`, `requireNotNull(message)`      |
| Functions > 40 lines                            | Extract private functions                          |
| Files > 300 lines                               | Extract mappers/helpers into their own file        |
| Dispatcher injected anywhere outside this module | Inject at Repository/DataSource level only        |
| Silently defaulting/truncating health profile fields | Validate and surface error explicitly         |
| `Call<T>` return types on API services           | `suspend fun`                                     |
| Any global `object` with mutable state           | Inject via Hilt                                   |

---

## 13. Scope & Safety Rules for This Module

- ❌ Do not modify `domain` or `presentation` files while working in `data/` — flag the
  need instead of reaching across the boundary.
- ❌ Do not add a new Gradle dependency to `data/build.gradle.kts` without explicit approval.
- ❌ Do not rename existing Entities, DTOs, or DAOs unless explicitly asked (Room schema
  and API contracts depend on stable names).
- ❌ Do not write code before saving an implementation plan to `docs/plans/` (see §14).
- ✅ Work on one repository/feature slice at a time.
- ✅ Show the list of files to create/change before writing code.
- ✅ Every new `*RepositoryImpl` method needs a corresponding test before the task is
  marked done.

---

## 14. Planning — Data Section of the Root Plan Template

Per the root `AGENTS.md` §12, no code is written before a plan is saved to
`docs/plans/YYYY-MM-DD-feature-name.md`. The **Data** subsection of that plan must list,
specifically for this module:

```markdown
### Data
- DTOs to add/modify (field-by-field, noting nullability — call out anything touching
  health profile or allergy data explicitly)
- Mapper functions to add (DTO → Domain, Entity → Domain) and what each maps
- DAO changes (new queries, new entities, migration impact)
- Remote DataSource / API service changes
- Repository method(s) touched and their offline-first behavior (does this read from
  Room, and does a remote failure need to fail silently or surface an error?)
```

---

## 15. Quick Reference — Where Things Live in the Root Doc

| I need to know...                        | Root `AGENTS.md` section |
|--------------------------------------------|---------------------------|
| Module dependency rules                    | §2.1                       |
| Full project folder structure              | §2.2                       |
| Dispatcher responsibility model            | §6.1                       |
| Exception handling (`runCatchingCancellable`) | §6.4                    |
| Auth interceptor & secrets                 | §7.1                       |
| OCR low-confidence error contract          | §7.3                       |
| Offline-first SSOT pattern                 | §9                         |
| Testing strategy overview                  | §11                        |
| Mandatory planning rule                    | §12                        |
| Health profile safety rules                | §13.2                      |
| Scope & safety rules (project-wide)        | §16                        |

---

*Scoped from the NutriScan AI root `AGENTS.md` · Data Module Track · v1.0*
