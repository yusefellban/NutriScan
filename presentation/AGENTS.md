# NUTRISCAN AI — `presentation` Module — AI Agent Rules

> **MANDATORY:** Read this file completely before touching any file under `presentation/`.
> This is a **scoped extension** of the root `AGENTS.md`. It does not replace it —
> every root rule (tech stack, safety notice, module boundaries, planning protocol,
> testing requirements) still applies in full. This file exists to give an agent
> working *only* inside `presentation/` everything it needs without re-reading the
> entire 2000+ line root contract every time.
>
> If anything here appears to conflict with the root `AGENTS.md`, the root file wins —
> flag the discrepancy instead of silently picking one.

---

## 0. Module Identity

| Field           | Value                                                                 |
|-----------------|------------------------------------------------------------------------|
| **Module**      | `presentation`                                                        |
| **Contains**    | Compose screens, ViewModels, State/Event/Effect contracts, shared UI components, theme |
| **Language**    | 100% Kotlin, 100% Jetpack Compose — zero XML, zero Java                |
| **Imports**     | `domain` **only**. Never `data`.                                       |
| **Imported by** | `app` only (for Hilt `@Module` binding / NavGraph wiring)              |

```
       app
        │
    presentation ──► domain
```

**Hard boundary rules:**
- ❌ NEVER import anything from `data/` (no `*Dto`, no `*Entity`, no Retrofit/Room types) into `presentation/`.
- ❌ NEVER let a ViewModel call a `RepositoryImpl` directly — only UseCases from `domain`.
- ❌ NEVER put `android.util.Log`, `println`, or raw `Context` references inside a ViewModel.
  Use `Timber` (in Composables only, if ever needed) and `LocalContext.current` inside Composables.
- ✅ A ViewModel may depend on `SavedStateHandle` and `domain` UseCases — nothing else external.

---

## ⚠️ Safety Reminder (inherited from root — non-negotiable in this module too)

This is a health-safety app. In the presentation layer specifically:
- Never let a `State` render a verdict, ingredient list, or health-profile field that
  silently defaulted to empty/null when the underlying data failed to load — show an
  explicit error state instead (`state.error != null`), never a fabricated "safe" UI.
- Any screen that edits health conditions/allergies or performs a destructive action
  (delete family member, clear list, log out, delete account) **must** emit a
  `ShowConfirmDialog` effect before executing — see §6 below.
- `NutriGptScreen` must always render the medical-disclaimer banner; it must not be
  dismissible or conditionally hidden.
- Red/Yellow/Green verdicts must never be conveyed by color alone — always icon + text + color.

---

## 1. Folder Structure — This Module

```
presentation/src/main/kotlin/ iti.grad.nutriscan.presentation/
├── auth/                  (login, register)
├── common/
│   ├── theme/             AppTheme, AppColors, AppTypography, AppShapes
│   ├── components/        AppButton, AppTextField, AppLoadingOverlay, AppErrorWidget,
│   │                      AppSnackbar, ConfirmationDialog, EmptyStateWidget,
│   │                      VerdictBadge, LoadingShimmer
│   └── Validation.kt
├── onboarding/            (splash, carousel, profile_setup)
├── home/
├── scan/                  (camera, processing, result)
├── nutrigpt/
├── ingredient_detail/
├── receipt/               (capture, result)
├── history/
├── report/                (list, detail)
├── shopping/              (list, alternative)
└── settings/              (profile, family, conditions, notifications, app)

presentation/src/test/kotlin/ iti.grad.nutriscan.presentation/
└── <mirrors main/ structure>       ← one *ViewModelTest.kt per ViewModel, MANDATORY
```

**Per-screen folder — every screen MUST contain exactly these files (plus optional `components/`):**

```
<screen_name>/
├── <ScreenName>Screen.kt
├── <ScreenName>State.kt
├── <ScreenName>Event.kt
├── <ScreenName>Effect.kt
├── <ScreenName>ViewModel.kt
└── components/            ← screen-local composables only used by this screen
```

- ❌ No `UiIntent` naming anywhere — it's `Event`, full stop (legacy WearZone naming is banned).
- ❌ No merging State/Event/Effect into a single file "for convenience."
- ❌ No screen-local component belongs in `components/` if it's reused by 2+ screens —
  promote it to `common/components/` instead.

---

## 2. The MVI Contract — Non-Negotiable Shape

```
User Action (Event)
   → ViewModel.onEvent(event)
      → private fun calls a UseCase (suspend, from domain)
         → Result<T> returned
      → _state.update { ... }        (StateFlow, for rendering)
      → _effect.send(Effect)         (Channel, for one-shot events)
   → Composable's LaunchedEffect(Unit) collects effect → navigate / snackbar / dialog
```

### 2.1 `<Screen>State.kt`

```kotlin
data class ScanResultState(
    val isLoading: Boolean = true,
    val verdict: ScanVerdict? = null,
    val ingredients: ImmutableList<IngredientUiModel> = persistentListOf(),  // ✅ ImmutableList — MANDATORY
    val error: UiError? = null,
)
```

- ✅ Every collection field MUST be `ImmutableList`/`ImmutableMap` (`kotlinx.collections.immutable`).
  `List<T>` in a State class is a **rejected PR**, no exceptions.
- ✅ Map domain models to `*UiModel` types inside the ViewModel — never expose a raw domain
  model directly in State if it needs UI-only fields (e.g. localized display strings).
- ✅ Default every field so `FeatureState()` alone is a valid, renderable initial state.

### 2.2 `<Screen>Event.kt`

```kotlin
sealed interface ScanResultEvent {
    data object SaveResultClicked : ScanResultEvent
    data class IngredientClicked(val ingredientName: String) : ScanResultEvent
}
```
- One `data object` per parameterless user action, one `data class` per action needing data.
- Name events by what the user *did* (`...Clicked`, `...Confirmed`, `...Dismissed`), not by
  what the system should do.

### 2.3 `<Screen>Effect.kt`

```kotlin
sealed interface ScanResultEffect {
    data object NavigateBack : ScanResultEffect
    data class ShowSnackBarRes(val messageResId: Int) : ScanResultEffect
    data class ShowConfirmDialog(
        val titleResId: Int,
        val messageResId: Int,
        val onConfirm: ScanResultEvent,
    ) : ScanResultEffect
}
```
- ❌ NEVER use `MutableSharedFlow` for effects — it silently drops events with no subscriber.
  Always `Channel<Effect>(Channel.BUFFERED)`.
- Prefer `ShowSnackBarRes(messageResId: Int)` over `ShowSnackBar(message: String)` so the
  message stays localizable — only use the string variant for truly dynamic content.

### 2.4 `<Screen>ViewModel.kt`

```kotlin
@HiltViewModel
class ScanResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getScanResultUseCase: GetScanResultUseCase,
    private val saveScanResultUseCase: SaveScanResultUseCase,
    // ❌ NO CoroutineDispatcher here — ViewModel is threading-agnostic.
    //    Dispatchers are injected into the data layer only.
) : ViewModel() {

    private val _state = MutableStateFlow(ScanResultState())
    val state: StateFlow<ScanResultState> = _state.asStateFlow()

    private val _effect = Channel<ScanResultEffect>(Channel.BUFFERED)
    val effect: Flow<ScanResultEffect> = _effect.receiveAsFlow()

    private val route: ScanResultRoute = savedStateHandle.toRoute<ScanResultRoute>()

    init { loadInitialData() }

    fun onEvent(event: ScanResultEvent) {
        when (event) {
            is ScanResultEvent.SaveResultClicked   -> saveResult()
            is ScanResultEvent.IngredientClicked   -> navigateToIngredientDetail(event.ingredientName)
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {                    // ✅ no Dispatcher arg
            _state.update { it.copy(isLoading = true, error = null) }
            getScanResultUseCase(route.imageUri)
                .onSuccess { result -> _state.update { it.copy(isLoading = false, /* map */) } }
                .onFailure { error  -> _state.update { it.copy(isLoading = false, error = UiError(error.localizedMessage)) } }
        }
    }

    private fun saveResult() { /* ... */ }
    private fun navigateToIngredientDetail(name: String) {
        viewModelScope.launch { _effect.send(ScanResultEffect.NavigateToIngredientDetail(name)) }
    }
}
```

**Checklist for every ViewModel:**
- [ ] `onEvent` is a lean `when` dispatcher — all real logic lives in `private fun`s
- [ ] Route args extracted via `savedStateHandle.toRoute<Route>()` — never manual bundle parsing
- [ ] Zero direct repository calls — UseCases only
- [ ] Zero `Dispatchers.IO`/`Dispatchers.Default` in `viewModelScope.launch(...)`
- [ ] Every destructive/safety-critical action emits `ShowConfirmDialog` before executing (§6)
- [ ] Every mutation of health-profile-adjacent state validates before committing — no silent defaults

---

## 3. Composable Screen Structure

```kotlin
// ✅ Stateful root — the ONLY level that touches the ViewModel
@Composable
fun ScanResultScreen(
    viewModel: ScanResultViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToIngredientDetail: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ScanResultEffect.NavigateBack -> onNavigateBack()
                is ScanResultEffect.NavigateToIngredientDetail -> onNavigateToIngredientDetail(effect.ingredientName)
                is ScanResultEffect.ShowSnackBarRes -> snackbarHostState.showSnackbar(/* resolved string */)
            }
        }
    }

    ScanResultContent(state = state, onEvent = viewModel::onEvent, snackbarHostState = snackbarHostState)
}

// ✅ Stateless content — pure rendering, no ViewModel reference, fully previewable
@Composable
private fun ScanResultContent(
    state: ScanResultState,
    onEvent: (ScanResultEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
) { /* ... */ }
```

**Rules:**
- Exactly one "stateful root" Composable per screen — it is the only place `hiltViewModel()`
  or `viewModel.onEvent`/`viewModel.effect` may be referenced.
- Everything below the root is stateless: state flows down as parameters, events flow up
  as lambdas. A leaf component must never own its own `mutableStateOf` for anything the
  parent needs to control (see §4.2).
- Effects are collected exactly once via `LaunchedEffect(Unit)`.
- Loading / error / content branching happens in the stateless content composable via
  `when { state.isLoading -> ...; state.error != null -> ...; else -> ... }` — never inside
  the stateful root.

---

## 4. Compose State Management

### 4.1 Immutable Collections — MANDATORY

```kotlin
// ✅ CORRECT
val ingredients: ImmutableList<IngredientUiModel> = persistentListOf()

// ❌ BANNED — Compose treats plain List<T> as unstable → over-recomposition
val ingredients: List<IngredientUiModel> = emptyList()
```
Use `.toImmutableList()` when mapping domain results inside the ViewModel.

### 4.2 State Hoisting

- Leaf/shared components (`common/components/*`) are stateless — no local `remember { mutableStateOf(...) }`
  for anything that affects behavior the parent cares about.
- Purely cosmetic, component-private animation state (e.g. a ripple toggle) may stay local.

### 4.3 Recomposition Hygiene

```kotlin
LazyColumn {
    items(scanHistory, key = { it.id }) { entry -> ScanHistoryCard(entry, onEvent) }  // ✅ key required
}

val hasRiskyItems by remember(shoppingItems) {
    derivedStateOf { shoppingItems.any { it.verdict == ScanVerdict.RED } }             // ✅ derivedStateOf for computed values
}
```

### 4.4 Side Effects Cheat Sheet

| API                    | Use for                                                |
|------------------------|----------------------------------------------------------|
| `LaunchedEffect(Unit)` | Collecting `viewModel.effect` (runs once)                |
| `LaunchedEffect(key)`  | Re-running work when `key` changes                       |
| `SideEffect`           | Post-recomposition, non-Compose calls (e.g. analytics)    |
| `DisposableEffect`     | Lifecycle-bound resources (e.g. CameraX preview binding)  |

---

## 5. Navigation — This Module's Responsibility

- `presentation` screens receive navigation as **lambdas** (`onNavigateBack: () -> Unit`,
  `onNavigateToX: (Args) -> Unit`) — they never hold a `NavController` reference themselves.
- Route args are read via `savedStateHandle.toRoute<FeatureRoute>()` (Navigation Compose
  2.8.0+ type-safe API) inside the ViewModel — never parsed manually from a `Bundle`.
- Adding a new screen means: create the screen folder here in `presentation/`, **and**
  add its `@Serializable` route to `app/navigation/Route.kt` and wire it into
  `app/navigation/NavGraph.kt` — those two files live in `app/`, not `presentation/`, but
  every new screen implies edits there too. Call this out explicitly in the plan (§8).

---

## 6. Destructive Action Contract — HARD REQUIREMENT

Any action that deletes, clears, edits health conditions/allergies, or logs the user out
**must** emit a `ShowConfirmDialog` effect first; the actual mutation only runs after the
user confirms via a follow-up `Event`.

```kotlin
private fun requestDeleteFamilyMember(memberId: String) {
    viewModelScope.launch {
        _effect.send(
            ManageFamilyEffect.ShowConfirmDialog(
                titleResId   = R.string.dialog_delete_member_title,
                messageResId = R.string.dialog_delete_member_message,
                onConfirm    = ManageFamilyEvent.ConfirmDeleteMember(memberId),
            )
        )
    }
}
```

Applies to (non-exhaustive): delete shopping list item, clear shopping list, delete family
member, edit health conditions/allergies, log out, delete account.

---

## 7. Shared Component Catalogue — Use These, Never Reinvent

```
AppButton(text, onClick, modifier, isLoading, enabled, variant: Primary|Secondary|Destructive)
AppTextField(value, onValueChange, label, isError, errorMessage, modifier)
AppLoadingOverlay()
AppErrorWidget(message, onRetry)
AppSnackbar — via SnackbarHostState in Scaffold
ConfirmationDialog(titleResId, messageResId, confirmLabel, onConfirm, onDismiss)
EmptyStateWidget(titleResId, subtitleResId, illustrationRes)
VerdictBadge(verdict: ScanVerdict, modifier)      ← always icon + text + color, never color alone
IngredientChip(name, isFlagged, onClick)
SafeAlternativeCard(alternative: SafeAlternativeUiModel, onClick)
ProfileVerdictCard(profileVerdict: ProfileVerdictUiModel)
LoadingShimmer(modifier, shape)
ScanHistoryCard(entry, onEvent)
```

- ❌ FORBIDDEN: creating a new component duplicating one of the above.
- ❌ FORBIDDEN: using raw `Button(...)`, `TextField(...)`, `AlertDialog(...)` directly —
  always the `App*`-prefixed / catalogue equivalent.
- Before adding a screen-local component to `components/`, check this catalogue first;
  if it's a generalization of something here, extend the shared component instead.

---

## 8. Theming & Localization Rules (apply to every Composable in this module)

- Colors: only `AppColors.*` tokens (`presentation/common/theme/AppColors.kt`).
  Never `Color(0xFF...)` inline.
- Typography: only `MaterialTheme.typography.*` (backed by `AppTypography`). Never a
  hardcoded `fontSize`.
- Strings: only `stringResource(R.string.xxx)` / `pluralStringResource(...)`. Zero
  hardcoded user-facing text in any `.kt` file — this includes snackbar messages, dialog
  text, empty-state copy, and content descriptions.
- Every new string needs both `res/values/strings.xml` (English) and
  `res/values-ar/strings.xml` (Arabic) entries, listed in the task's plan file
  (`docs/plans/`) *before* the code is written — see root §12.3.
- RTL: use `start`/`end` padding and alignment, never `left`/`right`.
- Every `Image`/`AsyncImage` needs a non-empty `contentDescription`; every tappable
  element needs a minimum 48.dp × 48.dp touch target.

---

## 9. Testing Requirements — This Module

> A ViewModel change without an updated `*ViewModelTest.kt` is a **rejected PR**. No exceptions.

- **Tools:** JUnit5 + MockK + Turbine + `TestCoroutineRule` (`UnconfinedTestDispatcher`).
- **Coverage target:** 95% for ViewModels.
- **Every ViewModel test file must cover:**
  - Initial state emission
  - Each `Event` → correct `State` transition
  - Each `Event` → correct `Effect` emission (via Turbine on `viewModel.effect`)
  - Error/failure paths → correct error state
- **Prefer Fakes over Mocks** for repository-shaped test doubles (`FakeScanRepository`,
  etc.); Mocks (MockK) are acceptable for UseCases injected into a ViewModel test.
- Route args in tests must be set into `SavedStateHandle` under the navigation bundle key
  (`"androidx.navigation.NavStartDestinationArguments"`), not a raw `mapOf(...)`, or
  `toRoute<T>()` will throw at test runtime.
- Test file location mirrors screen location:
  `presentation/src/test/kotlin/.../scan/result/ScanResultViewModelTest.kt`.
- UI-level tests (Compose Testing / `composeTestRule`) are required only for key screens
  (scan result, health profile setup, NutriGPT) — not mandatory for every screen, unlike
  ViewModel tests.

---

## 10. Prohibited Patterns — Quick Reference (this module)

| Pattern                                       | Use Instead                                   |
|------------------------------------------------|------------------------------------------------|
| `List<T>` in a State data class                | `ImmutableList<T>`                              |
| `MutableSharedFlow` for effects                | `Channel(Channel.BUFFERED)`                     |
| `UiIntent` naming                              | `sealed interface FeatureEvent`                 |
| Dispatcher injected/used in ViewModel          | Inject at Repository level only (in `data`)     |
| `mutableStateOf` for ViewModel-level state     | `StateFlow` in the ViewModel                    |
| Hardcoded `Color(0xFF...)` in a Composable     | `AppColors.*`                                   |
| Hardcoded strings in a Composable              | `stringResource(R.string.xxx)`                  |
| Hardcoded dimensions                           | `dimensionResource` / theme tokens              |
| Default unthemed `Button`/`TextField`          | Shared `App*` component catalogue (§7)          |
| Direct repository call from a ViewModel        | Go through a `domain` UseCase                   |
| Data-layer imports (`*Dto`, `*Entity`) here    | Map to domain models before they reach this module |
| Destructive action with no confirm step        | `ShowConfirmDialog` effect first (§6)           |

---

## 11. Before Writing Any Code in This Module

1. Confirm the task is scoped to `presentation/` only. If it needs a new domain model,
   UseCase, or repository method, that's cross-module — follow root §2.1 boundaries and
   coordinate the `domain`/`data` changes too.
2. Save an implementation plan to `docs/plans/YYYY-MM-DD-feature-name.md` per root §12 —
   include the State/Event/Effect breakdown and the full string table (English + Arabic)
   before writing a single Composable.
3. Show the list of files to create/change before writing code.
4. Implement: contracts (`State`/`Event`/`Effect`) → ViewModel → Screen/Content composables
   → components → tests.
5. Write the `*ViewModelTest.kt` in the same PR — not as a follow-up.
6. Update `README.md` feature status and add a `docs/ai/YYYY-MM-DD-task-name.md` summary
   per root §17.

---

*Scoped extension of the root `AGENTS.md` · NutriScan AI — `presentation` module · Last updated: July 2026*
