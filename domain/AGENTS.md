# NUTRISCAN AI — `domain` Module — AI Agent Rules

> **MANDATORY:** Read this file completely before touching any file under `domain/`.
> This is a **scoped extension** of the root `AGENTS.md` — every rule in the root file
> still applies. This file exists so an agent working exclusively inside `domain/` has
> everything it needs without re-reading the full 2000+ line project contract.
> If anything here ever appears to conflict with the root `AGENTS.md`, the root file wins —
> flag the discrepancy instead of silently picking one.

---

## 0. Module Identity

| Field            | Value                                                                              |
|-------------------|--------------------------------------------------------------------------------------|
| **Module**       | `domain`                                                                          |
| **Package root** | ` iti.grad.nutriscan.domain`                                                       |
| **Type**         | Pure Kotlin library module — **no Android Gradle plugin applied**                  |
| **Role**         | Owns business rules: domain Models, Repository interfaces (contracts only), UseCases |
| **Consumed by**  | `data` and `presentation` — **never** consumes either of them back                 |

---

## ⚠️ 1. Read This First — This Module Defines What "Safe" Means

`domain` is where the shape of a health verdict, a medical condition, and an allergy
is defined for the entire app. It doesn't own storage or network — but every rule that
protects a user from a wrong verdict starts here, in the models and UseCases this module
exposes to `data` and `presentation`.

- **A domain Model is the contract.** If `HealthProfile` or `FamilyMember` makes a field
  nullable or defaults it, every layer above and below inherits that ambiguity. Don't
  make a conditions/allergies field nullable or give it a silent default "for convenience" —
  that ambiguity is exactly what causes a mapper somewhere to collapse "unknown" into "none."
- **A UseCase is the last checkpoint before a repository call.** If a UseCase such as
  `AnalyzeLabelImageUseCase` or `SaveHealthProfileUseCase` receives an incomplete or empty
  profile/condition list where the feature requires one, it must return a validation
  failure — it must never silently forward incomplete safety data to the repository and
  let a "Green" verdict come back ungrounded.
  Prefer to over-block (e.g. surfacing "no consequential ingredients to compare against
  a complete profile") rather than under-block.
- **`DomainError.OcrLowConfidence` is a distinct type, not a generic failure.** UseCases
  and repository interfaces that touch the label/receipt scan pipeline must preserve this
  distinction end-to-end so the Result Screen can show "retake photo" instead of a generic
  error state (see §7.3 of the root doc).
- **Repository interfaces in this module are the only contracts `data` may implement.**
  A method signature here that quietly allows a partial/optional health profile to satisfy
  a scan- or verdict-producing call is a severity-1 design bug, not a style issue.

An agent that adds a nullable safety field without a documented reason, or a UseCase that
forwards an incomplete profile to a repository without validating it first, must stop and
flag it — this is rejected in review with no exceptions.

---

## 2. Position in the Architecture — Hard Dependency Rules

```
       ┌─────────────────────────────────────┐
       │               app                   │  ← only module seeing both data & presentation
       └───────────┬─────────────┬───────────┘
                   ▼             ▼
           presentation        data
                   │             │
                   └──────┬──────┘
                          ▼
                        domain           ← you are here; knows nothing; pure Kotlin
```

- `domain` imports **nothing outside the Kotlin stdlib and coroutines core**. No Android
  SDK, no Room, no Retrofit, no Hilt annotations beyond `@Inject constructor` (Hilt's
  `javax.inject.Inject` is the one dependency-injection primitive allowed here — it has
  no Android dependency).
- `domain` is consumed by **both** `data` (which implements its repository interfaces)
  and `presentation` (which calls its UseCases). `domain` never imports from either.
- `domain` defines repository **interfaces only** (`IScanRepository`,
  `IHealthProfileRepository`, etc.) — implementations (`ScanRepositoryImpl`, …) live
  exclusively in `data`.
- **Domain Models are the only type that may cross the repository boundary.** Every
  public repository method returns a `domain` model (or `Result<domain model>` /
  `Flow<domain model>`) — never a `Dto` or `Entity`. `Dto`/`Entity` types must never be
  imported into this module, and this module has no visibility of them anyway (`domain`
  is not on `data`'s dependency list, so this is structurally enforced, not just a
  convention).
- Dispatchers (`@IoDispatcher`, `@DefaultDispatcher`) are **never** injected in this
  module. UseCases are completely dispatcher-agnostic — that responsibility belongs to
  `RepositoryImpl` / `DataSourceImpl` in `data` (root §6.1).

---

## 3. Folder Structure — STRICTLY ENFORCED

```
domain/src/main/kotlin/ iti.grad.nutriscan.domain/
├── auth/
│   ├── model/
│   │   └── AuthUser.kt
│   ├── repository/
│   │   └── IAuthRepository.kt
│   └── usecase/
│       ├── LoginWithEmailUseCase.kt
│       ├── LoginWithGoogleUseCase.kt
│       ├── RegisterUseCase.kt
│       └── GetCurrentUserUseCase.kt
├── common/
│   ├── DataResult.kt                    ← sealed class DataResult<T>
│   ├── DomainError.kt                   ← sealed class DomainError
│   ├── RunCatchingCancellable.kt
│   └── ValidationError.kt
├── profile/
│   ├── model/
│   │   ├── HealthProfile.kt
│   │   ├── FamilyMember.kt
│   │   ├── MedicalCondition.kt          ← enum: DIABETES, HYPERTENSION, CELIAC, etc.
│   │   └── AllergyItem.kt
│   ├── repository/
│   │   └── IHealthProfileRepository.kt
│   └── usecase/
│       ├── GetActiveProfilesUseCase.kt
│       ├── SaveHealthProfileUseCase.kt
│       ├── AddFamilyMemberUseCase.kt
│       ├── EditFamilyMemberUseCase.kt
│       └── DeleteFamilyMemberUseCase.kt
├── scan/
│   ├── model/
│   │   ├── ScanResult.kt
│   │   ├── Ingredient.kt
│   │   ├── SafeAlternative.kt
│   │   ├── ScanVerdict.kt               ← enum: RED, YELLOW, GREEN
│   │   ├── ProfileVerdict.kt            ← per-family-member verdict
│   │   └── ScanHistoryEntry.kt
│   ├── repository/
│   │   └── IScanRepository.kt
│   └── usecase/
│       ├── AnalyzeLabelImageUseCase.kt
│       ├── GetScanHistoryUseCase.kt
│       └── SaveScanResultUseCase.kt
├── nutrigpt/
│   ├── model/
│   │   ├── NutriGptMessage.kt
│   │   └── NutriGptConversation.kt
│   ├── repository/
│   │   └── INutriGptRepository.kt
│   └── usecase/
│       └── SendNutriGptMessageUseCase.kt
├── receipt/
│   ├── model/
│   │   ├── ReceiptScanResult.kt
│   │   └── ReceiptLineItem.kt
│   ├── repository/
│   │   └── IReceiptRepository.kt
│   └── usecase/
│       └── AnalyzeReceiptImageUseCase.kt
├── shopping/
│   ├── model/
│   │   ├── ShoppingListItem.kt
│   │   └── SmartAlternative.kt
│   ├── repository/
│   │   └── IShoppingListRepository.kt
│   └── usecase/
│       ├── GetShoppingListUseCase.kt
│       ├── AddShoppingItemUseCase.kt
│       ├── RemoveShoppingItemUseCase.kt
│       └── CheckShoppingItemSafetyUseCase.kt
├── report/
│   ├── model/
│   │   └── WeeklyReport.kt
│   ├── repository/
│   │   └── IReportRepository.kt
│   └── usecase/
│       ├── GetLatestReportUseCase.kt
│       └── GetReportHistoryUseCase.kt
└── home/
    ├── model/
    │   └── HomeFeedCategory.kt
    ├── repository/
    │   └── IHomeFeedRepository.kt
    └── usecase/
        └── GetHomeFeedUseCase.kt
```

**Placement rules:**
- A new feature area → its own top-level package (`model/`, `repository/`, `usecase/`
  subfolders), following the pattern above. Don't add feature code to `common/`.
- A new domain concept → `<feature>/model/<Name>.kt`. If it's shared across ≥ 2 features
  (e.g. an error type, a shared value object), it goes in `common/` instead — but only
  after confirming it's truly cross-cutting, not just convenient.
- A new business operation → exactly one `usecase/<Verb><Noun>UseCase.kt` per operation
  (see §5 — one `invoke()` per class, never a multi-method "manager" class).
- A new capability that `data` must implement → add or extend the matching
  `repository/I<Feature>Repository.kt` interface first; `data` follows.
- Do not invent new top-level folders under `domain/` without discussion (§10 of this file).

---

## 4. Domain Models — Rules

Domain models are plain Kotlin `data class` / `enum class` / `sealed interface`
declarations. Nothing else.

```kotlin
// domain/scan/model/ScanResult.kt
data class ScanResult(
    val id: String,
    val verdict: ScanVerdict,
    val explanation: String,
    val ingredients: List<Ingredient>,
    val profileVerdicts: List<ProfileVerdict>,
    val safeAlternative: SafeAlternative?,
)

enum class ScanVerdict { RED, YELLOW, GREEN }

data class Ingredient(
    val name: String,
    val localName: String?,
    val isFlagged: Boolean,
    val riskReason: String?,
)
```

```kotlin
// domain/profile/model/HealthProfile.kt
data class HealthProfile(
    val id: String,
    val ownerName: String,
    val conditions: List<MedicalCondition>,   // ✅ never nullable — empty list is the
    val allergies: List<AllergyItem>,         //    explicit, honest representation of "none"
)
```

**Hard rules:**
- **No Android imports.** No `android.*`, no `Parcelable`, no `Context`.
- **No persistence or wire annotations.** No `@Entity`, `@ColumnInfo`, `@Serializable`,
  `@SerializedName`. A domain Model must compile with zero knowledge of Room or Retrofit.
- **Collections are plain `List<T>` / `Map<K, V>`**, never `ImmutableList` (that's a
  presentation-layer/State concern, see root §10.1) and never a Room/Retrofit collection
  type.
- **Conditions and allergies are never nullable.** Represent "none" as an empty `List`,
  not `null`. A nullable conditions field anywhere in this module is the seed of the
  exact silent-default bug the root Domain Notice warns about.
- **Model classes carry no logic beyond simple derived properties.** Business rules that
  need to run (validation, cross-field checks) belong in a UseCase, not in the model's
  `init {}` block throwing exceptions — a UseCase can return a typed `ValidationError`,
  a model constructor can only crash.
- Every model that is materially the same shape as its `Dto`/`Entity` counterpart is
  still a **separate declaration** — `data` owns the mapping (`toDomain()` /
  `toEntity()`), `domain` never imports the type it's being mapped from.

---

## 5. UseCase Conventions — STRICTLY ENFORCED

### 5.1 One class, one `invoke()`, one job

Per root §3.5: *"UseCase has exactly ONE public `invoke()` operator."* A UseCase is never
a multi-method "manager" or "service" class. If two operations are related but distinct,
they are two UseCase classes.

```kotlin
// domain/scan/usecase/AnalyzeLabelImageUseCase.kt
class AnalyzeLabelImageUseCase @Inject constructor(
    private val scanRepository: IScanRepository,
) {
    suspend operator fun invoke(
        imageUri: String,
        profileIds: List<String>,
    ): Result<ScanResult> {
        if (profileIds.isEmpty()) {
            return Result.failure(DomainError.Validation(ValidationError.NoActiveProfile))
        }
        return scanRepository.analyzeLabelImage(imageUri, profileIds)
    }
}
```

```kotlin
// domain/scan/usecase/GetScanHistoryUseCase.kt — Flow-returning UseCase, no suspend needed
class GetScanHistoryUseCase @Inject constructor(
    private val scanRepository: IScanRepository,
) {
    operator fun invoke(): Flow<List<ScanHistoryEntry>> = scanRepository.observeScanHistory()
}
```

- `suspend operator fun invoke(...)` for a single async result → `Result<T>`.
- `operator fun invoke(...): Flow<T>` for an observed stream — no `suspend` keyword on
  the function itself; the repository's cold `Flow` carries the async work.
- No `@Inject` on the constructor's parameters beyond the repository interface(s) it
  needs — a UseCase's only collaborators are `domain`-layer types (repository
  interfaces, other UseCases where genuinely needed, `common/` utilities).
- A caller invokes it like a function: `getScanResultUseCase(imageUri, profileIds)` —
  never `.invoke(...)` explicitly, and never exposed as `.execute(...)` or `.run(...)`.

### 5.2 Where validation belongs

A UseCase is the correct place to reject a request **before** it reaches `data` when the
rule is a pure business rule (not an I/O concern). Examples of what belongs in a UseCase:

- `AnalyzeLabelImageUseCase` rejecting an empty `profileIds` list before submitting a
  scan — there is nothing to ground a verdict against.
- `SaveHealthProfileUseCase` rejecting a save where `conditions`/`allergies` were dropped
  or truncated between load and save (compare against the loaded profile's field
  count/shape before delegating to the repository), consistent with the root's "no
  partial saves" rule (root §13.2).
- `RegisterUseCase` / `EditFamilyMemberUseCase` rejecting structurally invalid input
  (e.g. empty name) via `ValidationError`, before a network round-trip is wasted on it.

What does **not** belong in a UseCase: retry logic, caching decisions, offline-first
behavior, dispatcher selection — all of that is `data`'s responsibility (root §6.1, §9).
A UseCase orchestrates and validates; it does not know how its repository gets an answer.

### 5.3 Exception handling inside a UseCase

Most UseCases are a thin, single-line delegation to a repository call and need no
`try`/`catch` — the repository already returns `Result<T>`. If a UseCase itself performs
async work beyond a single repository call (e.g. sequencing two suspend calls), use the
same `runCatchingCancellable` utility defined in `common/` — never a bare `runCatching`:

```kotlin
// ✅ CORRECT — reuses the shared, cancellation-safe wrapper
suspend operator fun invoke(memberId: String): Result<Unit> = runCatchingCancellable {
    val member = getFamilyMemberUseCase(memberId).getOrThrow()
    validateMember(member)
    healthProfileRepository.deleteFamilyMember(memberId).getOrThrow()
}

// ❌ BANNED — bare runCatching swallows CancellationException
runCatching { healthProfileRepository.deleteFamilyMember(memberId) }
```

---

## 6. Repository Interfaces — Contract Rules

```kotlin
// domain/scan/repository/IScanRepository.kt
interface IScanRepository {
    suspend fun analyzeLabelImage(imageUri: String, profileIds: List<String>): Result<ScanResult>
    fun observeScanHistory(): Flow<List<ScanHistoryEntry>>
    suspend fun saveScanResult(result: ScanResult): Result<Unit>
}
```

- Naming: `I<Feature>Repository` — the `I` prefix is mandatory and consistent across the
  codebase (matches `data`'s `<Feature>RepositoryImpl`).
- One repository interface per feature package — keep it focused (Interface Segregation:
  `IScanRepository` never grows a profile- or report-related method just because it's
  convenient — root §3.6).
- Return types are always one of: `Result<DomainModel>` (single async op),
  `Flow<DomainModel>` / `Flow<List<DomainModel>>` (observed stream), or plain
  `DomainModel` only for pure synchronous derivations (rare — prefer the above two).
- Never a `suspend fun` returning a raw model with no `Result` wrapper for anything that
  can fail (network, disk, validation) — an unwrapped throw crossing the repository
  boundary defeats the `Result`-based error contract the rest of the app relies on.
- Parameters and return types are domain Models or primitives only — never a `Dto` or
  `Entity` (§2), and never an Android type.

---

## 7. `common/` — Shared Domain Utilities

### 7.1 `RunCatchingCancellable.kt`

Defined once here, used everywhere a coroutine needs safe exception handling — in this
module's UseCases (§5.3) and throughout `data`'s repositories (root §6.4).

```kotlin
// domain/common/RunCatchingCancellable.kt
suspend fun <T> runCatchingCancellable(block: suspend () -> T): Result<T> =
    runCatching { block() }.also { result ->
        result.exceptionOrNull()?.let { e ->
            if (e is CancellationException) throw e   // re-propagate cancellation
        }
    }
```

### 7.2 `DomainError.kt`

The typed failure channel that lets a UseCase or `RepositoryImpl` distinguish a specific,
UI-relevant failure from a generic one. `data`'s `ErrorInterceptor` maps HTTP codes to
`NutriScanHttpException` subtypes (root §7.3); a `*RepositoryImpl` then maps those onto
`DomainError` before returning `Result.failure(...)` — a `Dto`-layer or HTTP-layer
exception must never leak past the repository boundary undifferentiated.

```kotlin
// domain/common/DomainError.kt
sealed class DomainError(message: String) : Throwable(message) {
    data object OcrLowConfidence : DomainError("OCR confidence too low — retake required")
    data object Unauthorized : DomainError("Session expired — please log in again")
    data object NetworkUnavailable : DomainError("No network connection")
    data class Validation(val error: ValidationError) : DomainError("Validation failed")
    data class Unexpected(val cause: Throwable) : DomainError(cause.message ?: "Unknown error")
}
```

- `DomainError.OcrLowConfidence` is the one every scan/receipt UseCase and the Result
  Screen must special-case — root §13.3 requires the UI to prompt "retake photo" for this
  specific case instead of a generic error state. Don't collapse it into `Unexpected`.
- Add a new `DomainError` case rather than reusing `Unexpected` whenever the UI needs to
  branch on the failure type (per root §3.5's "silently defaulting" prohibition — a UI
  that can't tell OCR failure from network failure ends up guessing, and guessing near
  health data is unacceptable).

### 7.3 `ValidationError.kt`

The typed reason a UseCase rejected input before it reached a repository (§5.2).

```kotlin
// domain/common/ValidationError.kt
sealed interface ValidationError {
    data object NoActiveProfile : ValidationError
    data object EmptyConditionsAndAllergiesMismatch : ValidationError   // save doesn't match load
    data object InvalidEmail : ValidationError
    data object PasswordTooShort : ValidationError
    data object EmptyMemberName : ValidationError
}
```

### 7.4 `DataResult.kt` — ⚠️ flag before use

The folder structure declares `DataResult.kt` as a `sealed class DataResult<T>`, but
**every code sample in the root `AGENTS.md`** (UseCase return types, repository
signatures, ViewModel `.onSuccess { } .onFailure { }` handling) uses Kotlin's **stdlib**
`Result<T>` exclusively. There is no example anywhere in the root doc of `DataResult`
being constructed, returned, or consumed.

- **Default to `kotlin.Result<T>`** for every UseCase and repository signature in this
  module, matching every example in §4–§6 above and in the root doc.
- If `DataResult<T>` is meant to add a third `Loading` state for `Flow`-based UseCases
  (distinct from `Result<T>`'s two-state Success/Failure), that's a reasonable use — but
  it isn't specified anywhere in the current contract. **Confirm with the team before
  introducing `DataResult` into any new code** rather than guessing at its shape; using
  two different result-wrapper conventions side by side in the same module is worse than
  picking one and documenting the choice.

---

## 8. Testing — Domain Layer Target: 100%

| What to test                                   | Tools           |
|-------------------------------------------------|-----------------|
| UseCase logic (happy path, validation rejects)  | JUnit5 + MockK  |
| Domain model transformations / derived logic    | JUnit5          |
| Health profile / condition-allergy mapping rules| JUnit5 + MockK  |

```
domain/src/test/
├── auth/usecase/
│   ├── LoginWithEmailUseCaseTest.kt
│   └── RegisterUseCaseTest.kt
├── profile/usecase/
│   ├── GetActiveProfilesUseCaseTest.kt
│   └── SaveHealthProfileUseCaseTest.kt
├── scan/usecase/
│   ├── AnalyzeLabelImageUseCaseTest.kt
│   └── GetScanHistoryUseCaseTest.kt
└── nutrigpt/usecase/
    └── SendNutriGptMessageUseCaseTest.kt
```

- **100% coverage is the stated target for this layer** (root §11.1) — every UseCase
  needs at minimum: a success path test, a repository-failure-propagates test, and — for
  any UseCase with validation (§5.2) — one test per rejected `ValidationError` case.
- Repository dependencies are mocked with **MockK** in UseCase tests (root §11.4: "Mocks
  acceptable for UseCases"). Prefer a `Fake<Feature>Repository` when one already exists
  for `presentation` tests, so the same fake's behavior is exercised from both sides —
  keep it in sync with `data`'s real impl (e.g. it must also be able to return
  `DomainError.OcrLowConfidence`).
- Use `runTest { }` (Kotlin Coroutines Test) for suspend UseCase tests — no
  `TestCoroutineRule`/`Dispatchers.setMain()` needed here, since UseCases never touch a
  dispatcher (§2) — that machinery is only required in `presentation` and `data` tests.
- A UseCase test asserting a validation rejection must assert on the specific
  `ValidationError` value, not just "returns failure" — a test that only checks
  `result.isFailure` would pass even if the wrong error type were returned, which is
  exactly the kind of ambiguity §1 of this file warns against.

---

## 9. Code Quality — Prohibited Patterns in `domain/`

| Pattern                                          | Use Instead                                          |
|----------------------------------------------------|---------------------------------------------------------|
| `import android.*` anywhere in this module         | Nothing — domain has zero Android awareness           |
| `@Entity`, `@ColumnInfo`, `@Serializable` on a model | Keep the model plain; mapping lives in `data`         |
| A `Dto` or `Entity` type imported into `domain`    | Not possible/needed — `domain` isn't `data`'s dependency; if you find yourself wanting this, the model belongs in `data` |
| Nullable `conditions` / `allergies` fields         | Non-nullable `List<T>`, empty list means "none"        |
| A UseCase with more than one public method         | Split into separate UseCases, one `invoke()` each      |
| Bare `runCatching` inside a UseCase                | `runCatchingCancellable`                                |
| `ImmutableList<T>` in a domain Model                | Plain `List<T>` — `ImmutableList` is a State-layer type |
| Dispatcher (`@IoDispatcher`, etc.) injected here    | Inject at `data`'s Repository/DataSource level only    |
| Business validation skipped, error deferred to `data`| Reject with a `ValidationError` in the UseCase (§5.2) |
| Collapsing `OcrLowConfidence` into a generic error  | Preserve as `DomainError.OcrLowConfidence` explicitly   |
| `!!` without a comment                              | `?: return`, `let`, `requireNotNull(message)`          |
| Functions > 40 lines                                | Extract private functions                                |
| Files > 300 lines                                   | Split into more focused model/usecase files             |

---

## 10. Scope & Safety Rules for This Module

- ❌ Do not modify `data` or `presentation` files while working in `domain/` — flag the
  need instead of reaching across the boundary.
- ❌ Do not add a new Gradle dependency to `domain/build.gradle.kts` without explicit
  approval — this module should stay dependency-light (Kotlin stdlib, coroutines-core,
  `javax.inject` only; adding anything else is a signal the code belongs elsewhere).
- ❌ Do not rename an existing domain Model, repository interface method, or UseCase
  class unless explicitly asked — both `data` (implements the interfaces) and
  `presentation` (calls the UseCases) depend on these names staying stable.
- ❌ Do not make a repository interface method return a `Dto`/`Entity`, and do not accept
  one as a parameter — this collapses the whole point of the module boundary.
- ❌ Do not write code before saving an implementation plan to `docs/plans/` (see §11).
- ✅ Work on one feature package (`scan/`, `profile/`, `nutrigpt/`, …) at a time.
- ✅ Show the list of files to create/change before writing code.
- ✅ Every new UseCase needs a corresponding test (§8) before the task is marked done.
- ✅ When adding a repository interface method, check whether `data`'s corresponding
  `*RepositoryImpl` and any `Fake*Repository` used in `presentation` tests need updating —
  flag this even though you don't edit those files yourself.

---

## 11. Planning — Domain Section of the Root Plan Template

Per the root `AGENTS.md` §12, no code is written before a plan is saved to
`docs/plans/YYYY-MM-DD-feature-name.md`. The **Domain** subsection of that plan must
list, specifically for this module:

```markdown
### Domain
- Models to add/modify (field-by-field; call out anything touching health profile,
  condition, or allergy data explicitly, and confirm none of it is nullable)
- Repository interface method(s) to add/change — exact signature, `Result<T>` vs `Flow<T>`
- UseCases to create (one per `invoke()`) — for each, note:
  - its single responsibility
  - any `ValidationError` cases it can return, and why
  - which repository interface method(s) it calls
- New `DomainError` / `ValidationError` cases needed, if any
```

---

## 12. Quick Reference — Where Things Live in the Root Doc

| I need to know...                          | Root `AGENTS.md` section |
|----------------------------------------------|---------------------------|
| Module dependency rules                      | §2.1                       |
| Full project folder structure                | §2.2                       |
| Layer violation rules (zero tolerance)        | §3.5                       |
| SOLID checklist before writing a class        | §3.6                       |
| Dispatcher responsibility model               | §6.1                       |
| Exception handling (`runCatchingCancellable`) | §6.4                       |
| OCR low-confidence error contract             | §7.3                       |
| Offline-first SSOT pattern (context for how `data` uses domain models) | §9 |
| Testing strategy overview                     | §11.1                      |
| Mandatory planning rule                       | §12                        |
| Health profile safety rules                   | §13.2                      |
| Core label scan business rules                | §13.3                      |
| Destructive action contract (confirm-before-execute lives in presentation, not domain) | §13.9 |
| Scope & safety rules (project-wide)           | §16                        |

---

*Scoped from the NutriScan AI root `AGENTS.md` · Domain Module Track · v1.0*
