# Exercises Feature — Backend Integration Plan

## 0. Document Purpose and Audience

This document is the implementation plan for wiring the existing Exercises UI to the
real production Exercises API. It is written for three audiences at once, and each
section below is written with that in mind:

- **Reviewers / product owners** — Section 2 ("User Review Required") is the section
  that matters most; it lists every place where the plan makes a judgment call that a
  human should sign off on before code is written.
- **Implementers** — Section 3 ("Proposed Changes") is the detailed, file-by-file,
  layer-by-layer specification that should require no further design decisions once
  approved. Every new file, every renamed file, and every touched file is listed
  explicitly, along with the reasoning for why it's structured the way it is.
- **QA / verification** — Section 4 ("Verification Plan") lists both the automated
  coverage that should exist before this is considered done, and the manual pass a
  human should walk through on a device before sign-off.

## 1. Goal Description

The Exercises UI (list screen + workout timer screen) is fully built but runs entirely on
`ExercisesMockData`. This plan wires it to the real, public **Exercises API**
(`https://exercises-dataset-mu.vercel.app`, no auth, no API key — see
`Exercises_API__Production__postman_collection.json`) following the exact layering,
naming and DI conventions already used by the `news` and `foodlog` features
(domain → data → app-DI → presentation), and the standards in `SKILL.md`
(no hardcoded user-facing strings, no hardcoded colors, JUnit5/Turbine/coroutines-test
unit tests for every ViewModel).

Scope: `ExercisesScreen` (list, search, category filter, instructions bottom sheet) and
`ExerciseWorkoutScreen` (timer, sets/reps, calorie calculation). Endpoints not used by
either screen today (`/batch`, `/random`, `/suggestions`, `/stats`, `/equipments`,
`/targets`, static image/gif proxying) are **not** implemented now — the repository
interface is designed so they can be added later without breaking anything already built.

### 1.1 In Scope

- **List screen browsing**: paginated fetch of exercises from `GET /exercises`, with
  server-side search (`q`) and server-side category filtering (`body_part`).
- **Category chip row**: sourced dynamically from `GET /exercises/categories` instead of
  the current hardcoded chip list.
- **Instructions bottom sheet**: real, English-language instructions and step-by-step
  breakdowns rendered from `instructions` / `instruction_steps`.
- **Workout screen**: fetching a single exercise by id via `GET /exercises/:id`, deriving
  `ExerciseType`, and computing calories burned using either the API-provided or
  fallback-constant kcal values.
- **Error and loading states** for both screens, replacing the current assumption of
  always-available, always-present mock data.
- **Test coverage** for both ViewModels and the new repository implementation.

### 1.2 Explicitly Out of Scope (this iteration)

- `/exercises/batch`, `/exercises/random`, `/exercises/suggestions`, `/exercises/stats` —
  no current screen consumes these; adding them now would be speculative.
- `/equipments` and `/targets` list endpoints — today's UI doesn't have an equipment or
  target-muscle filter, only a body-part filter, so these are left for a future feature.
- Any static image/gif proxying or caching strategy beyond whatever the existing image
  loading library already does for `NewsArticle` images — no new image pipeline is being
  introduced here.
- Arabic localization of exercise content — covered in the review item below, but worth
  restating here: this plan does not attempt to translate or otherwise localize
  server-provided exercise content.
- Any redesign of the visual layout of either screen — this is a data-source swap, not a
  UI redesign; existing Composables are touched only where the underlying model shape
  changes (e.g. `String` instead of `@StringRes`).

---

## 2. User Review Required — please confirm before implementation starts

1. **Arabic content gap.** The API returns `instructions` / `instruction_steps` in
   `en, it, tr, es, ru, zh, hi, pl, ko, fr` — **no `ar`**. The app is en/ar only.

   **Why this matters:** the app's core localization contract is "no hardcoded
   user-facing strings" — every string a user sees is expected to come from
   `strings.xml` and be translated for both supported locales. Server-sourced exercise
   content breaks that contract structurally, because there is no Arabic translation to
   fall back to, and machine-translating on the client would introduce a whole new set
   of quality, caching, and consistency problems that are out of scope here.

   **Plan:** always request/display `en` exercise content regardless of app locale (the
   screen chrome — labels, buttons, category chip "All" — stays fully localized via
   `strings.xml` as today). Exercise `name`, `equipment`, `target`, `instructions` become
   plain dynamic `String` fields (not `@StringRes`), the same pattern already used for
   `NewsArticle.title` — this is server content, not app-authored UI text, so it's exempt
   from the "no hardcoded strings" rule, which governs literals *written by us* in
   Composables.

   **What this means for Arabic-locale users specifically:** they will see exercise
   names, equipment, target muscles, and instructions in English, while every other
   piece of chrome around those screens (search hint, category chip labels, buttons,
   error messages, the "All" chip) remains in Arabic as normal. This is a acceptable
   trade-off given the dataset available, but is called out here explicitly because it's
   a visible, user-facing compromise rather than an invisible implementation detail.

   **Future follow-up (not blocking):** if Arabic instructions become a hard requirement
   later, the two realistic paths are (a) a follow-up request to the dataset maintainer
   or a switch to a different dataset with `ar` coverage, or (b) a machine-translation
   layer with caching — both are meaningfully larger efforts and are intentionally not
   bundled into this plan.
2. **Category chips no longer match the API taxonomy.** Today's chips are
   `all/warm_up/yoga/biceps/chest/back/legs` — the API's real `body_part` values are
   `back, cardio, chest, lower arms, lower legs, neck, shoulders, upper arms, upper legs,
   waist`.

   **Side-by-side comparison of old vs. new chip sets:**

   | Today (mock, hardcoded)      | Real API (`body_part` values)      |
   |-------------------------------|-------------------------------------|
   | all                           | (synthetic "All" chip, kept)         |
   | warm_up                       | *(no equivalent — dropped)*          |
   | yoga                          | *(no equivalent — dropped)*          |
   | biceps                        | *(folded into "upper arms")*         |
   | chest                         | chest                                |
   | back                          | back                                 |
   | legs                          | *(split into "lower legs"/"upper legs")* |
   | *(not present today)*         | cardio                               |
   | *(not present today)*         | lower arms                           |
   | *(not present today)*         | neck                                 |
   | *(not present today)*         | shoulders                            |
   | *(not present today)*         | waist                                |

   **Plan:** keep the "All" chip (localized, static), and build the rest of the chip row
   dynamically from `GET /exercises/categories`, using a `Locale`-aware
   `.replaceFirstChar { it.uppercase() }` for display. This removes `yoga`/`warm_up`/
   `biceps` chip semantics — confirm that's acceptable, since those don't exist as
   `body_part` values in the real dataset.

   **Downstream implications if approved:** the chip row becomes wider (10 categories
   instead of 6), so the horizontal scroll behavior of the chip row should be
   double-checked visually once real categories are wired in — this is a layout
   side-effect of a data change, not a design change, but worth a quick look during
   manual verification (see §4, step 3).
3. **Calorie fields are frequently `null`.** In the sample data `rep_kcal`/`min_kcal` are
   `null` for most exercises (e.g. the sample "3/4 sit-up" has both null).

   **Why a fallback is necessary rather than optional:** the workout screen's entire
   calorie-burned calculation, and therefore the congrats dialog shown at the end of a
   workout, depends on one of these two fields being present. If both are simply left
   `null` all the way through, the UI either has to hide the calorie figure entirely
   (a regression versus today's mock-backed experience, where every exercise has a
   calorie value) or crash/show a garbage number. Neither is acceptable, so some fallback
   is required regardless of which numbers are chosen.

   **Plan:** fall back to conservative flat estimates (`DEFAULT_MIN_KCAL = 0.15`,
   `DEFAULT_REP_KCAL = 0.20` — same order of magnitude as today's mock values) whenever
   the API value is `null`, applied once at the domain→UI mapping boundary and clearly
   named/documented as an estimate.

   **Where exactly the fallback is applied:** in `ExercisesRepositoryImpl.toDomain()`
   (§3.2) — this is deliberately a single choke point rather than scattered `?:`
   defaults in the ViewModel or UI layer, so that if the constants ever change, or if a
   real MET-table lookup replaces them later, there is exactly one place to edit.

   **Confirm these numbers are acceptable placeholders** (a nutritionist- or
   product-owner-sourced MET table, ideally keyed by `body_part` or `category` rather
   than one flat number for every exercise, would be a good future follow-up — out of
   scope here given the size of that undertaking relative to this integration).
4. **`ExerciseType` (CARDIO vs NORMAL_WORKOUT) derivation.** The API has no such field —
   `ExerciseType` is purely an app-side concept that determines which timer/workout UI
   variant is shown (duration-based for cardio vs. sets/reps-based for normal workouts).

   **Plan:** `type = if (category == "cardio" || minKcal != null) CARDIO else
   NORMAL_WORKOUT`. The reasoning behind the two-part condition: `category == "cardio"`
   catches anything the API itself tags as cardio, while `minKcal != null` catches
   exercises that are timer-driven in practice (calories-per-minute only makes sense for
   duration-based activity) even if the API didn't explicitly categorize them that way.

   **Known limitation of this heuristic:** it's a best-effort inference, not something
   backed by an explicit API field, so there may be edge cases in the dataset where an
   exercise is arguably cardio-like (e.g. high-rep circuit work) but doesn't meet either
   condition, or vice versa. Confirm this heuristic is acceptable as a first pass; a
   spot-check across a sample of categories during manual verification (§4) should catch
   any obviously wrong classifications before this ships.
5. **Exercise detail refetch vs. cache.** `ExercisesEffect.NavigateToExerciseWorkout`
   only carries an `exerciseId` (String). Plan: `ExerciseWorkoutViewModel` calls
   `GetExerciseByIdUseCase(id)` (i.e. `GET /exercises/:id`) rather than relying on an
   in-memory cache from the list screen — this survives process death / deep links and
   keeps the two screens fully decoupled (no shared mutable state beyond the existing
   `ExercisesSharedTracker`, which is unaffected by this plan).

If any of the above should be different, flag it and the plan below will be adjusted
before implementation.

---

## 3. Proposed Changes

### 3.0 Implementation Phases and Ordering

To keep the change reviewable and to avoid a single giant PR, implementation is broken
into four sequential phases. Each phase should compile and (where applicable) pass tests
on its own before moving to the next:

- **Phase A — Domain layer** (§3.1): pure Kotlin models, repository interface, and use
  cases. No Android dependencies, no network code yet. This phase can be reviewed purely
  on its own merits as a contract, independent of how it's implemented.
- **Phase B — Data layer** (§3.2): DTOs, Retrofit service interface, and the repository
  implementation, including the locale-fallback and calorie-fallback mapping logic. This
  is where the bulk of the new logic lives, and where the new `ExercisesRepositoryImplTest`
  (§4) is added.
- **Phase C — DI wiring** (§3.3): the two small, purely additive changes to
  `NetworkModule.kt` and `RepositoryModule.kt` that connect Phase A and Phase B into the
  existing dependency graph. Deliberately kept minimal and mechanical.
- **Phase D — Presentation layer** (§3.4): ViewModel changes, state/event shape changes,
  view-level mechanical updates, and updated tests. This is the phase most likely to
  surface UX questions (loading/error states, debounce behavior) and should be reviewed
  with the manual verification plan (§4) in mind.

Each phase is described in its own subsection below, in the order it should be built.

### 3.1 `domain` module — new `domain/exercises` package

```
domain/exercises/model/Exercise.kt
domain/exercises/model/ExercisePage.kt
domain/exercises/model/ExerciseQuery.kt
domain/exercises/repository/IExercisesRepository.kt
domain/exercises/usecase/GetExercisesUseCase.kt
domain/exercises/usecase/GetExerciseByIdUseCase.kt
domain/exercises/usecase/GetExerciseCategoriesUseCase.kt
```

**`Exercise.kt`** — pure domain model, no Android/UI types:
```kotlin
data class Exercise(
    val id: String,
    val name: String,
    val category: String,
    val bodyPart: String,
    val equipment: String,
    val target: String,
    val secondaryMuscles: List<String>,
    val instructions: String,
    val instructionSteps: List<String>,
    val imageUrl: String?,
    val gifUrl: String?,
    val repKcal: Double?,
    val minKcal: Double?,
)
```

**Field-by-field notes:**

- `id` — the API's exercise identifier, used both as the list's stable key and as the
  argument passed to `GetExerciseByIdUseCase` when navigating to the workout screen.
- `name` — display name of the exercise (e.g. "3/4 sit-up"); English only, per the
  locale decision in §2, item 1.
- `category` — the API's broad grouping (e.g. `cardio`); used as one input to the
  `ExerciseType` heuristic in §2, item 4.
- `bodyPart` — the API's `body_part` value; this is what the category chip row filters
  on, and what populates `GET /exercises/categories`.
- `equipment` — free-text equipment description (e.g. "body weight"); displayed as-is.
- `target` — primary muscle targeted; displayed in the instructions bottom sheet.
- `secondaryMuscles` — list of secondary muscles, if any; displayed as supporting detail
  alongside `target`.
- `instructions` — the flattened, English-only instructions string (locale fallback
  applied at the mapping boundary — see §3.2).
  `instructionSteps` — the same content pre-split into ordered steps, when the API
  provides a step list rather than one prose block.
- `imageUrl` / `gifUrl` — nullable; the UI already has fallback placeholder handling for
  missing images from other features, which this reuses rather than adding new logic.
- `repKcal` / `minKcal` — nullable in the domain model *before* the repository-level
  fallback is applied (see the distinction with `ExerciseUiModel` in §3.4, where these
  are guaranteed non-null after fallback).

**`ExercisePage.kt`**:
```kotlin
data class ExercisePage(
    val exercises: List<Exercise>,
    val currentPage: Int,
    val totalPages: Int,
    val hasNext: Boolean,
)
```
This mirrors the API's own pagination envelope (`meta.pagination`) one-to-one, so the
mapping from `PaginationDto` to `ExercisePage` is a straight passthrough with no
reinterpretation needed — see §3.2 for exactly how each field maps.

**`ExerciseQuery.kt`** — one centralized filter object instead of scattering raw params
through every layer (this is the "centralized" piece the search/category-filter UI binds
to). Centralizing here means the ViewModel constructs exactly one `ExerciseQuery` per
user action (search input change, chip tap, or "load more" scroll), and every layer below
it — use case, repository, API service — passes that same shape down, rather than each
layer inventing its own parameter list:
```kotlin
data class ExerciseQuery(
    val page: Int = 1,
    val limit: Int = 20,
    val bodyPart: String? = null,
    val query: String? = null,
)
```

**`IExercisesRepository.kt`**:
```kotlin
interface IExercisesRepository {
    suspend fun getExercises(query: ExerciseQuery): Result<ExercisePage>
    suspend fun getExerciseById(id: String): Result<Exercise>
    suspend fun getCategories(): Result<List<String>>
}
```

**Use cases** — thin, single-purpose, `operator fun invoke`, matching
`GetHealthHeadlinesUseCase` exactly:
```kotlin
class GetExercisesUseCase @Inject constructor(
    private val repository: IExercisesRepository,
) {
    suspend operator fun invoke(query: ExerciseQuery): Result<ExercisePage> =
        repository.getExercises(query)
}

class GetExerciseByIdUseCase @Inject constructor(
    private val repository: IExercisesRepository,
) {
    suspend operator fun invoke(id: String): Result<Exercise> = repository.getExerciseById(id)
}

class GetExerciseCategoriesUseCase @Inject constructor(
    private val repository: IExercisesRepository,
) {
    suspend operator fun invoke(): Result<List<String>> = repository.getCategories()
}
```

### 3.2 `data` module — DTOs, API service, and repository implementation

This is the layer where the network response shape (as documented in the Postman
collection) is translated into the app's domain model. Three things happen here, each
described in its own subsection below: (a) the raw DTOs that mirror the wire format
exactly, (b) the Retrofit service interface that declares the three endpoints this plan
uses, and (c) the repository implementation that ties DTOs, the use cases' domain
contracts, and the two fallback behaviors from §2 (locale and calorie) together.

#### 3.2.1 DTOs

`data/remote/dto/ExerciseDto.kt` (mirrors the Postman response exactly;
`kotlinx.serialization`, matching every other DTO in the codebase):
```kotlin
@Serializable
data class ExerciseDto(
    val id: String,
    val name: String,
    val category: String,
    @SerialName("body_part") val bodyPart: String,
    val equipment: String,
    val instructions: Map<String, String>? = null,
    @SerialName("instruction_steps") val instructionSteps: Map<String, List<String>>? = null,
    @SerialName("secondary_muscles") val secondaryMuscles: List<String>? = null,
    val target: String? = null,
    @SerialName("rep_kcal") val repKcal: Double? = null,
    @SerialName("min_kcal") val minKcal: Double? = null,
    val image: String? = null,
    @SerialName("gif_url") val gifUrl: String? = null,
)

@Serializable
data class ExercisesResponseDto(
    val success: Boolean,
    val meta: ExercisesMetaDto? = null,
    val data: List<ExerciseDto> = emptyList(),
)

@Serializable
data class ExercisesMetaDto(val pagination: PaginationDto)

@Serializable
data class PaginationDto(
    @SerialName("current_page") val currentPage: Int,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("has_next") val hasNext: Boolean,
)

@Serializable
data class SingleExerciseResponseDto(val success: Boolean, val data: ExerciseDto)

@Serializable
data class CategoriesResponseDto(val success: Boolean, val data: List<String> = emptyList())
```
*(`coerceInputValues = true` / `explicitNulls = false` are already set globally in
`NetworkModule.provideJson()`, so missing optional fields are safe.)*

#### 3.2.2 API Service Interface

`data/remote/api/ExercisesApiService.kt` — declares the three endpoints this plan
consumes (list/search/filter, get-by-id, categories); each maps directly to one use case
from §3.1:
```kotlin
interface ExercisesApiService {
    @GET("exercises")
    suspend fun getExercises(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("body_part") bodyPart: String?,
        @Query("q") q: String?,
    ): ExercisesResponseDto

    @GET("exercises/{id}")
    suspend fun getExerciseById(@Path("id") id: String): SingleExerciseResponseDto

    @GET("exercises/categories")
    suspend fun getCategories(): CategoriesResponseDto
}
```

#### 3.2.3 Repository Implementation

`data/repository/ExercisesRepositoryImpl.kt` — same shape as `NewsRepositoryImpl`
(`withContext(ioDispatcher) { runCatchingCancellable { ... } }`), with a private mapper
that centralizes the `ar`-missing fallback and the calorie-fallback constants from §2,
items 1 and 3 respectively. Keeping both fallbacks in this one mapper — rather than, say,
the locale fallback living in the DTO layer and the calorie fallback living in the
ViewModel — is a deliberate choice: anyone auditing "where does server data get adjusted
before the app sees it" only has one function to read, `toDomain()` below.
```kotlin
private const val FALLBACK_LOCALE = "en"
private const val DEFAULT_MIN_KCAL = 0.15
private const val DEFAULT_REP_KCAL = 0.20

class ExercisesRepositoryImpl @Inject constructor(
    private val api: ExercisesApiService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : IExercisesRepository {

    override suspend fun getExercises(query: ExerciseQuery): Result<ExercisePage> =
        withContext(ioDispatcher) {
            runCatchingCancellable {
                val response = api.getExercises(
                    page = query.page,
                    limit = query.limit,
                    bodyPart = query.bodyPart,
                    q = query.query,
                )
                ExercisePage(
                    exercises = response.data.map { it.toDomain() },
                    currentPage = response.meta?.pagination?.currentPage ?: query.page,
                    totalPages = response.meta?.pagination?.totalPages ?: 1,
                    hasNext = response.meta?.pagination?.hasNext ?: false,
                )
            }
        }

    override suspend fun getExerciseById(id: String): Result<Exercise> =
        withContext(ioDispatcher) {
            runCatchingCancellable { api.getExerciseById(id).data.toDomain() }
        }

    override suspend fun getCategories(): Result<List<String>> =
        withContext(ioDispatcher) {
            runCatchingCancellable { api.getCategories().data }
        }

    private fun ExerciseDto.toDomain() = Exercise(
        id = id,
        name = name,
        category = category,
        bodyPart = bodyPart,
        equipment = equipment,
        target = target.orEmpty(),
        secondaryMuscles = secondaryMuscles.orEmpty(),
        instructions = instructions?.get(FALLBACK_LOCALE).orEmpty(),
        instructionSteps = instructionSteps?.get(FALLBACK_LOCALE).orEmpty(),
        imageUrl = image,
        gifUrl = gifUrl,
        repKcal = repKcal ?: DEFAULT_REP_KCAL,
        minKcal = minKcal ?: DEFAULT_MIN_KCAL,
    )
}
```

### 3.3 `app` module — DI wiring (2 files touched, both purely additive)

Nothing in this phase changes existing behavior for any other feature — every change
here is an addition (`@Provides`, `@Named`, `@Binds`) alongside what already exists for
`news` and `foodlog`, following the exact same per-feature-network-client pattern
already established. There is no shared/global Retrofit instance being modified.

**`NetworkModule.kt`** — new `Named` Retrofit instance, following the exact
`NewsRetrofit`/`NewsOkHttpClient` pattern (own client because this API needs no
`AuthInterceptor`/`ErrorInterceptor`, same reasoning as News):
```kotlin
@Provides @Singleton @Named("ExercisesOkHttpClient")
fun provideExercisesOkHttpClient(): OkHttpClient { /* logging interceptor + timeouts only */ }

@Provides @Singleton @Named("ExercisesRetrofit")
fun provideExercisesRetrofit(
    @Named("ExercisesOkHttpClient") client: OkHttpClient,
    json: Json,
): Retrofit = Retrofit.Builder()
    .baseUrl(BuildConfig.EXERCISES_API_BASE_URL)
    .client(client)
    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .build()

@Provides @Singleton
fun provideExercisesApiService(@Named("ExercisesRetrofit") retrofit: Retrofit): ExercisesApiService =
    retrofit.create(ExercisesApiService::class.java)
```
Add `EXERCISES_API_BASE_URL = "https://exercises-dataset-mu.vercel.app/"` as a
`buildConfigField` next to `NEWS_API_BASE_URL` in `app/build.gradle.kts` — centralizes
the base URL the same way every other API's URL is centralized, instead of a string
literal in the module.

**`RepositoryModule.kt`** — one more `@Binds`:
```kotlin
@Binds @Singleton
abstract fun bindExercisesRepository(impl: ExercisesRepositoryImpl): IExercisesRepository
```

### 3.4 `presentation` module

This is the largest phase in terms of number of files touched, though most changes are
mechanical (swapping `stringResource(...)` calls for direct field reads). The
substantive changes — debounced search, pagination state, and error/loading states — are
concentrated in the two ViewModels. Files touched in this phase, in the order described
below: `ExerciseUiModel.kt`, `ExerciseCategoryUi.kt` (renamed), `ExercisesState.kt`,
`ExercisesEvent.kt`, `ExercisesViewModel.kt`, `ExerciseWorkoutViewModel.kt`, the three
view files (`ExercisesScreen.kt`, `ExerciseInstructionsBottomSheet.kt`,
`ExerciseWorkoutScreen.kt`), and the two test files.

**`common/model/ExerciseUiModel.kt`** — replace `@StringRes` fields with plain `String`
(server-driven content, same treatment as `NewsArticle`); keep `id`/`type`/kcal fields:
```kotlin
data class ExerciseUiModel(
    val id: String,
    val name: String,
    val equipment: String,
    val target: String,
    val instructions: String,
    val type: ExerciseType,
    val imageUrl: String?,
    val kcalPerMin: Double?,
    val kcalPerRep: Double?,
)
```
`exerciseCount` / `durationMin` are dropped from the model — they were mock-only
placeholders never sourced from real data; the workout screen already derives its own
duration from the live timer, and the list card's "N exercises • M min" copy will need a
product decision (flagged as a follow-up, not blocking — the card can show
`equipment • target` only until that's resolved).

**`exercises/model/ExerciseCategoryUi.kt`** (rename from `ExerciseCategory` to avoid
clashing with the new domain `Exercise` naming) — `id: String, label: String` (no more
`@StringRes` except the synthetic "All" entry, built in the ViewModel via
`context.getString(R.string.exercises_category_all)` once, not per-item).

**`exercises/state/ExercisesState.kt`** — add:
```kotlin
val currentPage: Int = 1,
val hasNextPage: Boolean = false,
val isLoadingMore: Boolean = false,
val errorMessageRes: Int? = null, // localized error string, e.g. R.string.generic_error
```
Drop nothing else — `visibleExercises` becomes simply the latest page's server-filtered
result (no more client-side `filterExercises()`).

**`exercises/state/ExercisesEvent.kt`** — add `data object OnRetryClick`,
`data object OnLoadMore`. Keep all existing events (`OnSearchQueryChange`,
`OnCategorySelected`, etc.) — their handlers change internally, not their shape.

**`exercises/viewmodel/ExercisesViewModel.kt`** — inject `GetExercisesUseCase` and
`GetExerciseCategoriesUseCase` instead of `Context`/mock data:
- `init {}` launches both `loadCategories()` and `loadExercises(reset = true)`.
- `OnSearchQueryChange` / `OnCategorySelected` debounce 300ms (cancel-and-relaunch a
  single `searchJob: Job?`, same idiom as `ExerciseWorkoutViewModel.timerJob`) then call
  `loadExercises(reset = true)` with the new `ExerciseQuery`.
- `loadExercises(reset)` sets `isLoading` (or `isLoadingMore` when paginating), calls the
  use case, and on `Result.success` updates `exercises`/`visibleExercises`/pagination
  fields, on `Result.failure` sets `errorMessageRes`.
- `OnExerciseClick` stays as a pure local lookup in the already-loaded list (no network
  call needed — the list already carries everything the bottom sheet displays).
- Remove `ExercisesMockData` usage entirely; delete the mock file once nothing else
  references it (also removes it from `presentation/src/main/kotlin/.../exercises/mock/`).

**`exercises/workout/viewmodel/ExerciseWorkoutViewModel.kt`** — inject
`GetExerciseByIdUseCase`:
- `InitExercise(id)` becomes a coroutine: `isLoading = true` → `useCase(id)` →
  on success, map `Exercise` → `ExerciseUiModel`, reset timer/sets/reps as today; on
  failure, surface an error state (`errorMessageRes`) with a retry affordance (new event
  `OnRetryInitClick` re-invoking `InitExercise` with the stored id).
- `calculateAndFinish()` is otherwise unchanged — it already reads
  `exercise.kcalPerMin` / `exercise.kcalPerRep`, which are now always non-null thanks to
  the repository-level fallback in §3.2.

**Views** (`ExercisesScreen.kt`, `ExerciseInstructionsBottomSheet.kt`,
`ExerciseWorkoutScreen.kt`) — mechanical updates only: read `exercise.name` /
`exercise.equipment` / `exercise.target` / `exercise.instructions` directly instead of
`stringResource(exercise.nameRes)` etc.; add a simple loading spinner + retry button state
for the two new `isLoading`/`errorMessageRes` cases (reuse whatever loading/error
composable pattern `NewsScreen` or `SavedScreen` already uses, if one exists, to stay
consistent rather than inventing a new one).

**Tests** — update the two existing skeleton test files to the `SKILL.md` template
(JUnit 5 + Turbine + `StandardTestDispatcher`), with a fake `IExercisesRepository`. Below
is the intended test-case breakdown for each file, not just a summary — this is meant to
be usable directly as the initial `@Test` method list:

- `ExercisesViewModelTest.kt`:
  - `initial load populates categories and exercises from the repository` — asserts the
    "All" chip is present alongside every category returned by
    `GetExerciseCategoriesUseCase`, and that `exercises`/`visibleExercises` reflect the
    first page from `GetExercisesUseCase`.
  - `search query change triggers a new call with the query set, after debounce` — uses
    `StandardTestDispatcher` + `advanceTimeBy` to confirm no call fires before the 300ms
    debounce window, and exactly one call fires after it, with `ExerciseQuery.query` set.
  - `rapid successive search query changes only trigger one call for the final query` —
    guards the cancel-and-relaunch `searchJob` behavior described in §3.4.
  - `category chip selection triggers a new call with bodyPart set` — and that selecting
    "All" again clears `bodyPart` back to `null`.
  - `repository failure on initial load surfaces errorMessageRes` — and that
    `OnRetryClick` re-triggers the same load.
  - `OnExerciseClick emits the expected state/effect exactly as today` — a pure
    local-lookup assertion, unchanged from the current mock-based test.
  - `OnStartWorkoutClick emits NavigateToExerciseWorkout with the correct id` — unchanged
    from the current mock-based test.
  - `OnLoadMore appends the next page without clearing the currently visible exercises` —
    new coverage for pagination, asserting `isLoadingMore` toggles correctly and
    `hasNextPage` reflects the repository response.

- `ExerciseWorkoutViewModelTest.kt`:
  - `InitExercise success populates exercise from the fake repository` — asserts the
    mapped `ExerciseUiModel` fields match what the fake repository returned, including
    the non-null kcal fields.
  - `InitExercise failure sets an error state with a retry affordance` — and that
    `OnRetryInitClick` re-invokes `InitExercise` with the originally stored id.
  - `timer start/pause/resume/finish behavior is unchanged from the current mock-based
    tests` — regression coverage carried over verbatim from the existing test file.
  - `sets/reps increment and decrement behavior is unchanged` — regression coverage
    carried over verbatim.
  - `calculateAndFinish uses kcalPerMin for CARDIO-type exercises` — new coverage
    specific to the `ExerciseType` heuristic from §2, item 4.
  - `calculateAndFinish uses kcalPerRep for NORMAL_WORKOUT-type exercises` — new coverage,
    same rationale.
  - `ExercisesSharedTracker totals update correctly after a completed workout` — carried
    over from the current test file, unchanged in assertion shape.

---

## 4. Verification Plan

This plan is considered complete once both the automated coverage below is green and the
manual pass has been walked through at least once on a real device or emulator with
network connectivity toggled on and off, since several of the new behaviors (loading
states, error/retry states) are only observable under real network conditions and won't
be exercised by a purely offline development loop.

**Automated**
- `./gradlew :domain:test :data:test :presentation:test` — new/updated unit tests above,
  plus a `ExercisesRepositoryImplTest` (MockWebServer or a fake `ExercisesApiService`)
  covering: successful mapping incl. the `en`-fallback and null-kcal-fallback logic,
  pagination fields passthrough, and error propagation (HTTP failure → `Result.failure`,
  never a thrown exception past the repository boundary).
- `./gradlew :app:assembleDebug` to confirm DI graph compiles (new `@Binds`/`@Provides`
  wired correctly, no missing bindings).

**Manual**
1. Open Exercises screen on a real device/emulator with network on — verify the list
   loads from the live API (not instant, so a loading state must be visible), categories
   chip row matches `/exercises/categories`, and scrolling/paging works.
2. Type a search query (e.g. "press") — verify results update (debounced, not one
   request per keystroke) and match `q=` search results from Postman.
3. Tap a category chip — verify `body_part` filtering matches the Postman sample for that
   category.
4. Tap an exercise — verify the instructions bottom sheet shows real English instructions
   and "Read more" expands the full text.
5. Start a workout for a NORMAL_WORKOUT-type exercise (no `rep_kcal` in the API) and a
   CARDIO-type exercise — verify calories still compute (via fallback constants) and the
   congrats dialog + `ExercisesSharedTracker` totals update as before.
6. Turn off network (airplane mode) and repeat steps 1 and 4's initial load — verify the
   error state + retry path works instead of an infinite spinner or crash.

---

## 5. Appendix A — API Endpoint Reference (endpoints used by this plan)

For quick reference during implementation and code review, the three endpoints this
plan actually consumes, summarized from `Exercises_API__Production__postman_collection.json`:

### `GET /exercises`
- **Purpose:** paginated list, with optional search and category filter.
- **Query params:** `page` (int, 1-indexed), `limit` (int, page size), `body_part`
  (string, optional — omit for "All"), `q` (string, optional — free-text search).
- **Response shape:** `{ success, meta: { pagination: { current_page, total_pages,
  has_next } }, data: [ExerciseDto, ...] }`.
- **Consumed by:** `GetExercisesUseCase` → `ExercisesViewModel.loadExercises()`.

### `GET /exercises/{id}`
- **Purpose:** fetch a single exercise by id, used when navigating into the workout
  screen.
- **Path param:** `id` (string).
- **Response shape:** `{ success, data: ExerciseDto }` — note this is *not* wrapped in a
  pagination envelope, hence the separate `SingleExerciseResponseDto`.
- **Consumed by:** `GetExerciseByIdUseCase` → `ExerciseWorkoutViewModel.InitExercise()`.

### `GET /exercises/categories`
- **Purpose:** the full list of valid `body_part` values, used to build the dynamic
  category chip row.
- **Query params:** none.
- **Response shape:** `{ success, data: [String, ...] }` — a flat list of category
  strings, e.g. `["back", "cardio", "chest", ...]`.
- **Consumed by:** `GetExerciseCategoriesUseCase` → `ExercisesViewModel.loadCategories()`.

### Endpoints intentionally not used by this plan (see §1.2)
`POST /exercises/batch`, `GET /exercises/random`, `GET /exercises/suggestions`,
`GET /exercises/stats`, `GET /equipments`, `GET /targets`, and any static image/gif
proxy routes. These exist in the Postman collection but have no current UI consumer;
the repository interface (§3.1) is intentionally left open to add methods for these
later without needing to restructure anything built in this plan.

## 6. Appendix B — Glossary

- **DTO (Data Transfer Object):** the class shape that mirrors the network response
  exactly, including `@SerialName` annotations for snake_case-to-camelCase mapping.
  DTOs never leak past the `data` module boundary.
- **Domain model:** the pure Kotlin representation of a concept (e.g. `Exercise`), with
  no network or Android framework dependencies, used by use cases and above.
- **UI model:** the presentation-layer representation (e.g. `ExerciseUiModel`), shaped
  specifically for what a Composable needs to render — this is where, for example, the
  calorie-fallback values are guaranteed non-null (unlike the domain model).
- **Use case:** a single-purpose, `operator fun invoke`-style class that wraps exactly
  one repository call, matching the existing `GetHealthHeadlinesUseCase` convention.
- **Fallback locale:** the hardcoded `"en"` constant used whenever server content is
  requested, regardless of the app's active UI locale (see §2, item 1).
- **`ExercisesSharedTracker`:** the existing cross-screen state holder that accumulates
  workout totals; explicitly unaffected by this plan (§2, item 5).

---

## 7. Risks and Rollback

**Risks:**
- The public dataset API has no documented SLA — if it becomes slow or unavailable in
  production, the Exercises feature degrades to its error/retry state rather than being
  unusable outright, but this is worth monitoring after rollout.
- The calorie fallback constants (§2, item 3) are placeholders; if a nutritionist-sourced
  MET table is adopted later, previously-computed workout totals won't retroactively
  change, which is expected but worth noting for anyone comparing historical vs. future
  calorie totals.
- The wider category chip row (§2, item 2) has not been visually verified against the
  full 10-category set on smaller screen sizes; flagged in manual verification step 3.

**Rollback:** because every change in this plan is additive at the DI layer (§3.3) and
scoped to the `exercises` feature packages, reverting is a matter of reverting the
feature's commits — no other feature's DI graph, network clients, or shared modules are
modified.