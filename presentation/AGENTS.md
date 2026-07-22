# NUTRISCAN AI — `presentation` Module — AI Agent Rules & Contract

> **MANDATORY:** Read this file **completely** before writing a single line of code in the
> `presentation` module. This is a **scoped extension** of the root `AGENTS.md` — every rule
> in the root file still applies. This file exists so an agent working exclusively inside
> `presentation/` has everything it needs without re-reading the entire project contract.
> Violations will be rejected in code review without exception.
>
> If anything here appears to conflict with the root `AGENTS.md`, the root file wins —
> flag the conflict and ask before proceeding.

---

## 0. Module Identity

| Field                  | Value                                                                                     |
|------------------------|--------------------------------------------------------------------------------------------|
| **Module**             | `presentation`                                                                              |
| **Package root**       | `iti.grad.nutriscan.presentation`                                                          |
| **Contains**           | Compose screens, ViewModels, State/Event/Effect contracts, shared UI components, theme     |
| **Depends on**         | `domain` **only**                                                                           |
| **Never depends on**   | `data` (no DTOs, no Room entities, no Retrofit types — ever)                                |
| **Consumed by**        | `app` (for Hilt wiring + `NavGraph`) — `presentation` never depends on `app`                |
| **Architecture**       | Clean Architecture (UI layer) + MVI (`State` / `Event` / `Effect`)                          |
| **Root reference**     | See root `AGENTS.md` §2 (module structure), §3 (MVI), §5 (nav), §10 (Compose state), §11 (testing), §12 (planning), §13 (business rules), §14 (design system) |

This module is Android-facing but **its ViewModels must remain framework-agnostic** —
see §3.5 below. Only Composables are allowed to touch `Context`, `LocalContext`, or
Android UI APIs directly.

---

## 1. Hard Boundary Rules — Zero Tolerance

These are the module-specific instances of the root Layer Violation Rules (§3.5 in root):

| Rule                                                          | Violation Example                                                        |
|----------------------------------------------------------------|---------------------------------------------------------------------------|
| Zero `data` module imports anywhere in `presentation`         | `import iti.grad.nutriscan.data.remote.dto.ScanResultDto`                 |
| Zero references to DTOs, Room `@Entity`, or Retrofit services | `ScanResultDto` typed anywhere in a ViewModel, State, or Composable        |
| ViewModel never calls a Repository directly                   | `class ScanResultViewModel(private val repo: IScanRepository)`            |
| ViewModel never receives a `CoroutineDispatcher`               | Any `@IoDispatcher` qualifier used in a ViewModel constructor             |
| ViewModel has zero Android framework references               | `import android.content.Context` inside a ViewModel                      |
| `List<T>` never appears in a `State` data class                | `val ingredients: List<IngredientUiModel>`                                 |
| `MutableSharedFlow` never used for one-shot effects            | `_effect = MutableSharedFlow<ScanResultEffect>()`                          |
| `UiIntent` naming banned anywhere                              | `sealed interface ScanResultUiIntent`                                     |
| No hardcoded `Color(0xFF...)` in any Composable                | `Modifier.background(Color(0xFF388E3C))`                                  |
| No hardcoded strings in any `.kt` file under `presentation`    | `Text("Scan again")`                                                      |
| No default unthemed Compose components                        | Raw `Button(...)` / `TextField(...)` instead of `AppButton` / `AppTextField` |
| Health profile fields never silently defaulted in a UI mapper  | `condition ?: MedicalCondition.NONE` when mapping to a UiModel            |

> Any agent producing code that violates a rule above must self-correct before presenting
> the change — this is not a style preference, it is a merge blocker.

---

## 2. Folder Structure — This Module Only

```
presentation/src/main/kotlin/iti.grad.nutriscan.presentation/
├── auth/
│   ├── login/            {Screen, State, Event, Effect, ViewModel} + components/
│   └── register/         {Screen, State, Event, Effect, ViewModel} + components/
├── common/
│   ├── theme/            AppTheme.kt · AppColors.kt · AppTypography.kt · AppShapes.kt
│   ├── components/       AppButton, AppTextField, AppLoadingOverlay, AppErrorWidget,
│   │                     AppSnackbar, ConfirmationDialog, EmptyStateWidget,
│   │                     VerdictBadge, LoadingShimmer  ← the shared catalogue (§7)
│   └── Validation.kt
├── onboarding/            splash/ · carousel/ · profile_setup/
├── home/                  HomeScreen + components/ (HomeFeedCategoryRow, FeedProductCard, ProfileSwitcher)
├── scan/                  camera/ · processing/ · result/ (each with its own components/)
├── nutrigpt/              NutriGptScreen + components/ (ChatBubble, QuickQuestionChips)
├── ingredient_detail/
├── receipt/               capture/ · result/
├── history/               ScanHistoryScreen + components/ (ScanHistoryCard)
├── report/                list/ · detail/
├── shopping/              list/ · alternative/ (SmartAlternativeSheet — bottom sheet, not a route)
└── settings/              profile/ · family/ · conditions/ · notifications/ · app/
```

**Rule:** a new screen always gets its own folder with exactly the five files below (§3.2).
A new reusable widget always goes in that feature's local `components/` unless it is
genuinely cross-feature — in that case it belongs in `common/components/` and must be
added to the shared catalogue (§7).

---

## 3. The MVI Contract — Mandatory Per Screen

### 3.1 Five Files Per Screen (no exceptions)

```
<Feature>/
├── <Feature>Screen.kt        ← Composable entry point, hiltViewModel(), collects state + effects
├── <Feature>State.kt         ← data class, immutable, what the UI renders
├── <Feature>Event.kt         ← sealed interface, what the user does
├── <Feature>Effect.kt        ← sealed interface, one-shot side effects (nav, snackbar, dialogs)
└── <Feature>ViewModel.kt     ← @HiltViewModel, owns State + Effect, exposes onEvent(event)
```

Naming is fixed: `Event` / `Effect` / `State` — never `UiIntent`, never `UiEffect`, never `UiState`.

### 3.2 `State.kt` Rules

```kotlin
data class ScanResultState(
    val isLoading: Boolean = true,
    val verdict: ScanVerdict? = null,
    // ✅ ImmutableList mandatory — plain List<T> is banned in every State class
    val ingredients: ImmutableList<IngredientUiModel> = persistentListOf(),
    val error: UiError? = null,
)
```

- Every collection field is `ImmutableList` / `ImmutableMap` from `kotlinx.collections.immutable`.
- State holds **UiModels**, never domain models directly and never DTOs.
  A UiModel is a presentation-only shape mapped from the domain model inside the ViewModel
  (e.g. `IngredientUiModel`, `ProfileVerdictUiModel`) — this keeps Compose stability and
  keeps localization/formatting concerns out of `domain`.
- Nullable fields represent "not loaded yet" or "not applicable" — never use a sentinel
  value (empty string, `-1`) to mean "absent."

### 3.3 `Event.kt` Rules

```kotlin
sealed interface ScanResultEvent {
    data object SaveResultClicked : ScanResultEvent
    data class IngredientClicked(val ingredientName: String) : ScanResultEvent
    data object RetryClicked : ScanResultEvent
}
```

- One `data object` per parameterless user action, one `data class` when a payload is needed.
- Event names describe the **user's action**, not the resulting side effect
  (`SaveResultClicked`, not `SaveResult`).

### 3.4 `Effect.kt` Rules

```kotlin
sealed interface ScanResultEffect {
    data object NavigateBack : ScanResultEffect
    data class NavigateToIngredientDetail(val ingredientName: String) : ScanResultEffect
    data class ShowSnackBarRes(val messageResId: Int) : ScanResultEffect
    data class ShowConfirmDialog(
        val titleResId: Int,
        val messageResId: Int,
        val onConfirm: ScanResultEvent,
    ) : ScanResultEffect
}
```

- Effects are **always** delivered via `Channel(Channel.BUFFERED)`, never `MutableSharedFlow`.
- Any destructive action (delete, clear list, log out, edit health conditions) **must**
  emit a `ShowConfirmDialog` effect before executing — see root §13.9. This is a hard
  requirement, not a suggestion.
- Prefer `ShowSnackBarRes(Int)` over `ShowSnackBar(String)` so the message always resolves
  through `stringResource` at the Composable layer, keeping the ViewModel string-agnostic.

### 3.5 `ViewModel.kt` Rules

```kotlin
@HiltViewModel
class ScanResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getScanResultUseCase: GetScanResultUseCase,
    private val saveScanResultUseCase: SaveScanResultUseCase,
    // ❌ never a CoroutineDispatcher — repositories own dispatching
) : ViewModel() {

    private val _state = MutableStateFlow(ScanResultState())
    val state: StateFlow<ScanResultState> = _state.asStateFlow()

    private val _effect = Channel<ScanResultEffect>(Channel.BUFFERED)
    val effect: Flow<ScanResultEffect> = _effect.receiveAsFlow()

    // ✅ type-safe nav args only — never a raw Bundle/String key lookup
    private val route: ScanResultRoute = savedStateHandle.toRoute()

    init { analyzeLabel() }

    fun onEvent(event: ScanResultEvent) {
        when (event) {
            is ScanResultEvent.SaveResultClicked  -> saveResult()
            is ScanResultEvent.IngredientClicked  -> navigateToIngredientDetail(event.ingredientName)
            is ScanResultEvent.RetryClicked       -> analyzeLabel()
        }
    }

    // one private fun per event — never inline the logic in the `when` branch
    private fun analyzeLabel() {
        viewModelScope.launch {   // ✅ no Dispatchers.IO here — Main by default
            _state.update { it.copy(isLoading = true) }
            getScanResultUseCase(route.imageUri)
                .onSuccess { result -> _state.update { it.copy(isLoading = false, verdict = result.verdict) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = UiError(e.localizedMessage)) } }
        }
    }
}
```

**Checklist for every ViewModel:**
- [ ] `@HiltViewModel` + `@Inject constructor`
- [ ] Only UseCases injected — never a Repository or DAO directly
- [ ] No `CoroutineDispatcher` in the constructor
- [ ] `onEvent()` is a lean dispatcher — all logic lives in private functions, one per event
- [ ] Nav args extracted via `savedStateHandle.toRoute<FeatureRoute>()` — never manual `get("key")`
- [ ] Zero `android.*` imports except `androidx.lifecycle.*` / `androidx.navigation.*`

### 3.6 Screen Composable Rules

```kotlin
@Composable
fun ScanResultScreen(
    onNavigateBack: () -> Unit,
    onNavigateToIngredientDetail: (String) -> Unit,
    viewModel: ScanResultViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ScanResultEffect.NavigateBack -> onNavigateBack()
                is ScanResultEffect.NavigateToIngredientDetail -> onNavigateToIngredientDetail(effect.ingredientName)
                is ScanResultEffect.ShowSnackBarRes -> snackbarHostState.showSnackbar(
                    // string resolved here, not in the ViewModel
                    message = /* stringResource-backed lookup */ ""
                )
            }
        }
    }

    ScanResultContent(state = state, onEvent = viewModel::onEvent, snackbarHostState = snackbarHostState)
}
```

- The `<Feature>Screen` composable owns **navigation callbacks as lambdas** — it never
  holds a `NavController` directly. `NavGraph.kt` (in `app`) wires the lambdas.
- Always `collectAsStateWithLifecycle()`, never `collectAsState()` (avoids collecting
  while the screen is backgrounded).
- Split into `<Feature>Screen` (stateful, wiring) and a private `<Feature>Content`
  (stateless, pure rendering from `State` + `onEvent`) — this makes the Content function
  trivially previewable and testable.

---

## 4. Navigation — Presentation Side

- `presentation` never imports `NavController` logic beyond exposing lambda callbacks
  (`onNavigateBack: () -> Unit`, `onNavigateToX: (id: String) -> Unit`). Route wiring and
  the `NavHost` itself live in `app/navigation/` (see root §5) — not this module.
- Screens that take nav args extract them with `savedStateHandle.toRoute<FeatureRoute>()`.
  Route data classes (`@Serializable`) are defined in `app`, but the ViewModel references
  the route type to deserialize its own arguments.
- Never pass a `Uri` through a route — routes carry `String` only (see root §5.3 table).
- A destructive confirmation (`ConfirmationDialog`) or a bottom sheet
  (e.g. `SmartAlternativeSheet`) is **not** a navigation destination — it's UI state/effect
  local to the screen, not a new route.

---

## 5. Compose State Management (Recap — Enforced Here)

| Concern                  | Rule                                                                                   |
|---------------------------|-----------------------------------------------------------------------------------------|
| Collections in State      | `ImmutableList` / `ImmutableMap` only — `persistentListOf()` as default                |
| State hoisting            | Leaf composables are stateless; state is owned by the screen-level composable or VM    |
| List rendering            | `LazyColumn { items(list, key = { it.id }) { ... } }` — always provide a stable `key`   |
| Derived values             | `remember(key) { derivedStateOf { ... } }` — never recompute inline every recomposition |
| One-shot effects           | `LaunchedEffect(Unit)` to collect the Effect Channel — runs exactly once               |
| Keyed re-fetch             | `LaunchedEffect(someId) { viewModel.load(someId) }`                                     |
| Non-Compose side effects   | `SideEffect { analytics.setScreen(...) }`                                               |
| Lifecycle-bound resources  | `DisposableEffect` (e.g. CameraX preview binding) with `onDispose { }` cleanup          |

---

## 6. Theming, Strings & Accessibility — Non-Negotiable in Every Composable

### 6.1 Colors & Typography

- Only `AppColors.*` tokens — never inline `Color(0xFF...)`.
- Only `MaterialTheme.typography.*` mapped to `AppTypography` — never a hardcoded `fontSize`.
- If a needed color/type token doesn't exist yet, add it to `AppColors.kt` /
  `AppTypography.kt` in `common/theme/` — don't improvise inline.

### 6.2 Strings — Zero Hardcoded Text

- Every user-facing string: `stringResource(R.string.xxx)`. No exceptions — labels,
  buttons, dialogs, snackbars, empty states, disclaimers, ingredient descriptions.
- New strings must be declared in the feature's implementation plan (root §12.3 §6) with
  **both** English and Arabic values before they appear in code.
- Plurals use `pluralStringResource` — never manual string concatenation/`if/else`.
- Arabic copy must read naturally (RTL, local phrasing) — not a literal machine translation.

### 6.3 Verdict Colors (Special Rule)

The Red/Yellow/Green verdict must never rely on color alone — always pair color with an
icon and/or text label (`VerdictBadge` already does this — reuse it, don't rebuild it).

### 6.4 Accessibility Checklist — Every Screen

- [ ] Every `Image` / `AsyncImage` has a non-empty `contentDescription`
- [ ] Every tappable element has a minimum touch target of `48.dp × 48.dp`
- [ ] Custom interactive composables declare `semantics { role = Role.Button }` (or the
      appropriate role)
- [ ] Text-on-background contrast ratio ≥ 4.5:1
- [ ] RTL verified for Arabic — use `start`/`end` padding, never `left`/`right`

---

## 7. Shared Component Catalogue — Use, Don't Reinvent

```
AppButton(text, onClick, modifier, isLoading, enabled, variant: Primary|Secondary|Destructive)
AppTextField(value, onValueChange, label, isError, errorMessage, modifier)
AppLoadingOverlay()
AppErrorWidget(message, onRetry)
AppSnackbar — via SnackbarHostState in Scaffold
ConfirmationDialog(titleResId, messageResId, confirmLabel, onConfirm, onDismiss)
EmptyStateWidget(titleResId, subtitleResId, illustrationRes)
VerdictBadge(verdict: ScanVerdict, modifier)
IngredientChip(name, isFlagged, onClick)
SafeAlternativeCard(alternative: SafeAlternativeUiModel, onClick)
ProfileVerdictCard(profileVerdict: ProfileVerdictUiModel)
LoadingShimmer(modifier, shape)
ScanHistoryCard(entry, onEvent)
DashedActionCard(label, onClick, modifier, contentPadding)
FoodEntryCard(name, kcal, modifier)
CalorieGoalsCard(tdee, caloriesGained, modifier)
StepsGaugeCard(steps, stepsGoal, onClick, modifier)
ExerciseCard(exerciseKcal, exerciseMinutes, onAddClick, modifier)
WaterTrackerCard(waterConsumed, waterGoal, onAddWater, onCupClicked, onCupLongPressed, modifier)
```

- **Before building a new component**, check this list. Duplicating an existing
  component's function is forbidden.
- Adding a genuinely new cross-feature component → add it here **and** to this table in
  the same PR, so the catalogue stays authoritative.
- Never reach for raw `Button`, `TextField`, `Text` with inline styling when an
  `App*`-prefixed equivalent exists.

---

## 8. Testing — Mandatory for Every ViewModel

> A feature is **not complete** without its `*ViewModelTest.kt`. A PR touching a
> ViewModel without an accompanying test update is rejected without review (root §11.2).

**Coverage target:** 95% for `presentation`.

Every ViewModel test file must cover:
- [ ] Initial state emission(s)
- [ ] Each `Event` → correct `State` transition
- [ ] Each `Event` → correct `Effect` emission (via Channel)
- [ ] Error/failure paths → correct error state
- [ ] Any destructive event → `ShowConfirmDialog` effect fires **before** execution (root §13.9)

**Stack:** JUnit5 + MockK + Turbine, `TestCoroutineRule` (`UnconfinedTestDispatcher`) —
see root §11.3–§11.5 for the full template and fixture patterns. Prefer Fakes for
Repository substitutes reached via UseCases; MockK is acceptable for UseCases themselves.

Test files live under `presentation/src/test/<feature>/<screen>/<Screen>ViewModelTest.kt`,
mirroring the main source folder structure exactly.

Key screens additionally get Compose UI tests (`composeTestRule`) rendering the
`<Feature>Content` composable against a given `State` — no ViewModel needed for these.

---

## 9. Presentation-Layer Business Rules (Screen-Specific)

These are the UI-facing consequences of root §13 — read root §13 for the full domain
context. This section states what the **ViewModel/Composable** must specifically do.

| Feature              | Presentation-layer requirement                                                                                     |
|-----------------------|----------------------------------------------------------------------------------------------------------------------|
| Health Profile edit  | Emit `ShowConfirmDialog` before any save that changes an existing profile (root §13.2)                              |
| Scan Result          | Distinguish `DomainError.OcrLowConfidence` from generic errors — dedicated "retake photo" state, not a generic error banner (root §13.3) |
| Scan Result          | `safeAlternative` card renders only when non-null; never rendered for a `GREEN` verdict                             |
| Scan Result          | If family profiles are active, render `ProfileVerdictCard` per member — never collapse to one blanket verdict       |
| NutriGPT             | Conversation held as `ImmutableList<NutriGptMessageUiModel>` in State; optimistic user bubble → loading bubble → real response |
| NutriGPT             | Medical disclaimer banner is pinned and cannot be dismissed by the user                                             |
| Receipt Result       | Any line item with `matchConfidence < 0.7` renders as "Unrecognized" — never a fabricated verdict                   |
| Home Feed            | Each category has its own independent loading shimmer — never a full-screen blocker while categories load          |
| Home Feed            | `ProfileSwitcher` always visible when family profiles exist                                                         |
| Shopping List        | Flagged (`RED`/`YELLOW`) items show a visual risk indicator in the row                                              |
| Shopping List        | `SmartAlternativeSheet` is a bottom sheet, not a route — triggered from the list screen's local UI state             |
| Shopping List / Family | Delete/remove actions always go through `ShowConfirmDialog` first (root §13.9)                                    |
| Reports               | Read-only screens over pre-generated data — no write UseCases; "Share" emits `ExportAsPdf` / `ShareWithDoctor` effects |

---

## 10. Scope & Safety Rules (Presentation-Specific)

- ❌ Do NOT add a `data` module import to fix a "quick" type mismatch — map through a
  UseCase/domain model instead, or flag the gap in `domain`.
- ❌ Do NOT add a new screen without first listing every string it introduces in the plan
  (root §12.3 §6) — both Arabic and English values.
- ❌ Do NOT invent a new effect-delivery mechanism (no `LiveData`, no `SharedFlow` for
  one-shot events) — `Channel` only.
- ❌ Do NOT skip the `<Feature>Content` stateless split "to save time" — it's required for
  UI testability and previews.
- ✅ Before writing code for a new screen, save an implementation plan to `docs/plans/`
  per root §12 — it must include the State/Event/Effect breakdown and the string table.
- ✅ Every ViewModel ships with its test file in the same PR.
- ✅ Every new shared component is added to §7's catalogue table in the same PR.

---

## 11. Quick Reference

| I need to know...                              | Where                          |
|--------------------------------------------------|---------------------------------|
| Full module dependency graph                     | Root AGENTS.md §2.1             |
| MVI contract details (State/Event/Effect)        | This file §3 / Root §3          |
| Route definitions & `NavGraph` wiring             | Root AGENTS.md §5               |
| Dispatcher model (why VM has none)               | Root AGENTS.md §6.1             |
| ViewModel test template (Turbine + MockK)         | Root AGENTS.md §11.5            |
| Mandatory planning rule & plan template           | Root AGENTS.md §12              |
| Destructive action contract                       | This file §3.4 / Root §13.9     |
| Health profile safety rules                       | Root AGENTS.md §13.2            |
| Shared component catalogue                        | This file §7 / Root §14.3       |
| Localization rules                                | This file §6.2 / Root §14.4     |
| Verdict accessibility rule                        | This file §6.3 / Root §14.5     |

---

*Scoped to `presentation/`. Defers to root `AGENTS.md` on any conflict or omission.*
*Last updated: July 2026 · NutriScan AI — Android Track · v1.0*
