# Implementation Plan: Profile Name/Measurement Validation & Stale Profile-Flow Overwrite Fixes

**File:** `docs/plans/2026-08-14-profile-name-validation-and-stale-flow-bugfixes.md`
**Author:** AI Agent (retroactive documentation of an already-authored bugfix patch)
**Status:** Patch already implemented in `NutriScan-bugfixes(1).zip`; this document reconstructs the plan per the `docs/plans/` standard in `SKILL.md` §1, and serves as the review/verification record for the change.
**Source comparison:** `NutriScan.zip` (baseline) vs. `NutriScan-bugfixes(1).zip` (patch)

---

## 1. Goal Description

NutriScan's Sign-Up (Register) screen and Edit Profile screen both accept a user's first and last name as free-text input, and Edit Profile additionally accepts numeric height (cm) and weight (kg) input. Two independent, user-visible defects existed in this area of the app, and both are addressed by this patch:

### 1.1 Bug Cluster A — Missing name & measurement validation

- Neither Register nor Edit Profile validated that a name actually looked like a name. A user could type `"123"`, `"#$@"`, or any other digit/symbol string into First Name or Last Name and the app would accept it, only rejecting blank input.
- Edit Profile performed **no validation at all** on height or weight before attempting to save — a blank, negative, zero, or wildly out-of-range value (e.g. `999 cm`, `-70 kg`) would be sent straight to the save flow.
- Because there was no client-side validation gate, these bad values were only caught (if at all) by the backend, at which point the failure surfaced to the user as a generic, unhelpful **"No internet"**-style error, with no indication of which field was actually wrong or why. This is a poor UX and a support burden (users assume it's a connectivity problem and retry rather than fixing their input).

**Goal:** Add client-side validation for first name, last name, height, and weight on both Register and Edit Profile, so invalid values are caught immediately, field-by-field, with a clear inline error message, and never reach the network layer.

### 1.2 Bug Cluster B ("Bug-09") — Stale profile-Flow re-emission silently overwrites unsaved edits

- `EditProfileViewModel` observes a `Flow<User>` that is backed by the local Room database and therefore re-emits on **any** write to that user's row — not only writes caused by the user's own profile save. The concrete example that triggers this in practice is an **avatar upload**: uploading a new avatar image writes a new `avatarUrl` to the same user row, and that write re-triggers the same Flow the Edit Profile screen is observing for the rest of the profile fields.
- Previously, **every** emission from this Flow unconditionally overwrote the *entire* editable state — first name, last name, date of birth, email, height, weight, avatar, and selected disease/allergy chip selections — with whatever the Flow just emitted.
- Consequence: if a user opened Edit Profile, entered edit mode, and began making changes (e.g. toggling on a newly-diagnosed allergy) but had not yet tapped Save, and *any* unrelated background write happened to touch that same DB row (avatar upload being the most likely real-world trigger), the in-progress, unsaved edit would be **silently discarded** and replaced with the old, already-saved server snapshot. No error, no warning — the change simply vanished from the screen.

**Goal:** Ensure the profile screen never silently discards a user's in-progress, unsaved edits because of an unrelated background write to the same underlying data row, while still allowing genuinely live-updating fields (specifically the avatar, which has its own independent upload-and-poll flow) to keep reflecting the latest server state even while the rest of the screen is in edit mode.

### 1.3 Non-goals

- This plan does **not** change backend/API-side validation. Client-side validation added here is a UX improvement layered in front of whatever the server already enforces; the server remains the source of truth and should not be assumed to be redundant after this patch.
- This plan does not address the Calories or Water Tracker features. Four files related to those features (`WaterTrackerCard.kt`, `CaloriesViewModel.kt`, `CaloriesScreen.kt`, `CaloriesEvent.kt`) and one associated test file (`CaloriesViewModelTest.kt`) are present in the bugfix archive but are **byte-for-byte identical** to the baseline. They introduce no functional change and are excluded from the "Proposed Changes" section below; see §6 ("Out-of-Scope / No-Op Files") for the full accounting.
- This plan does not restructure the MVI event/state/effect architecture already in place; all changes are additive within the existing `Event -> ViewModel -> State -> Composable` pattern used throughout NutriScan.

---

## 2. User Review Required

The following decisions were made in the implementation and should be explicitly confirmed by a human reviewer (product owner / tech lead) before this patch is considered final, since they encode product judgment calls that are not purely technical:

1. **Name character set — `^[\p{L}][\p{L} '-]*$`.**
   The chosen regex allows: any Unicode letter as the first character (`\p{L}` — so non-Latin scripts such as Arabic, which is a first-class supported locale per `SKILL.md` §2, are accepted), followed by any mixture of Unicode letters, spaces, hyphens, and apostrophes. It rejects digits and all punctuation/symbols other than `-` and `'`.
   - **Please confirm:** should names with a leading hyphen/apostrophe (rare but real, e.g. some Irish/Scottish surnames written with a leading apostrophe in some transliteration systems) be supported? As written, the pattern requires the *first* character to be a letter, so `'t Hooft`-style leading-apostrophe names would be rejected.
   - **Please confirm:** are numeral suffixes (`"John III"`, `"Robert Jr. 2nd"`) something the product wants to support? As written they are rejected outright since digits are disallowed everywhere in the string, not just word-initial.
   - **Please confirm:** this regex is intentionally duplicated verbatim in both `RegisterViewModel.kt` and `EditProfileViewModel.kt` rather than extracted to a shared validation module. This was a pragmatic choice to keep the patch minimal and low-risk, but introduces a maintenance hazard — a future change to the rule in one file will not automatically apply to the other. **Recommend a follow-up refactor** to extract this into a shared `NameValidator`/`domain` utility, tracked separately from this bugfix.

2. **Last name is optional; first name is required — but only on Edit Profile.**
   `EditProfileViewModel.validateName(name, isRequired)` is called with `isRequired = true` for first name and `isRequired = false` for last name. `RegisterViewModel`, by contrast, treats **both** first and last name as required (`isBlank()` → error in both cases, unchanged from before this patch).
   - **Please confirm this asymmetry is intentional.** It means a user can register with both names required, but later go into Edit Profile and clear their last name entirely (as long as it stays blank, blank passes validation) — is that the desired account model, or should Edit Profile also require last name for consistency?

3. **Height range `[50, 250]` cm and weight range `[2, 300]` kg.**
   These bounds are hardcoded as literal `Double` comparisons in `EditProfileViewModel.validateHeight` / `validateWeight`. **Please confirm these exact bounds match backend-side validation.** If the server enforces a narrower or wider range, users could hit a mismatch where the client accepts a value the server then rejects (or vice versa — the client blocks a value the server would have accepted, e.g. for a support case with an unusual body measurement). Bounds should ideally be defined once and shared (or at minimum kept in a single well-documented constants location) rather than duplicated as inline magic numbers.

4. **Avatar is the only field exempted from the "frozen during edit mode" rule (Bug-09 fix).**
   The fix special-cases the avatar so it continues to update live even while the rest of the screen is frozen in edit mode, on the reasoning that avatar upload is a distinct, user-initiated, actively-watched async operation. **Please confirm no other field has a similar "own async flow the user is actively watching while otherwise editing" characteristic** that should receive the same live-update carve-out (email verification status, for instance, if such a thing exists elsewhere in the app).

5. **Exit-from-edit-mode path is not fully verified against the freeze logic.**
   The freeze/thaw logic added for Bug-09 is keyed entirely off `state.isEditMode`. The `BackClicked` event handler, as it exists in the diffed code, only emits `EditProfileEffect.NavigateBack` — it does **not** appear to explicitly flip `isEditMode` back to `false`. If the screen's Composable/NavHost destroys and recreates the ViewModel on back-navigation this is moot (a fresh ViewModel starts with `isEditMode = false`), but if the ViewModel instance is retained (e.g. scoped to a parent nav graph or restored from a `SavedStateHandle`/process-death scenario) re-entering the screen could still be stuck in the frozen state. **This needs an explicit answer from whoever owns the navigation/ViewModel-scoping architecture** — it is called out again in §5 (Verification Plan) as a targeted manual QA step, and is not something this document can resolve by inspection of the diffed files alone.

6. **String resource duplication between Register and Edit Profile.**
   Register uses a new `error_invalid_name` string; Edit Profile uses its own, separately-worded pair `edit_profile_error_name_required` / `edit_profile_error_name_invalid`. This was presumably intentional (to allow the two screens' copy to diverge, and because Edit Profile also needs a distinct "required" vs. "invalid" message pair that Register does not distinguish for the blank case, since Register already had `error_empty_field` and reused it). **Please confirm this is desired** rather than an oversight that should be consolidated.

---

## 3. Proposed Changes (by Module)

> Note: because this document is being written after the patch was authored, "Proposed Changes" below is presented as the change-set that a reviewer should walk through, file by file, exactly as if reviewing a pull request. Each entry includes the *why*, the *what*, and the exact before/after where useful for review.

### 3.1 Module: `presentation / auth / register` (Sign-Up screen)

#### 3.1.1 `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/auth/register/viewmodel/RegisterViewModel.kt`

**Why:** Sign-Up currently only checks for blank names; it needs to also reject digit/symbol-only or digit/symbol-containing names, using the same rule that Edit Profile now enforces, so a bad name can never enter the system at account-creation time in the first place.

**What:**

1. Add a new private regex property, placed alongside the existing `emailRegex`:

   ```kotlin
   // Pure Kotlin Regex to adhere to "No Android Imports in ViewModel" rule
   private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()

   // Only letters (any language), spaces, hyphens and apostrophes — no digits or
   // symbols like #$@%. Mirrors the same rule enforced on Edit Profile so bad names
   // can never enter the system in the first place, at registration time.
   private val namePattern = Regex("^[\\p{L}][\\p{L} '-]*$")
   ```

2. Change the name-validation portion of `handleSignUp()` from single blank-only checks to a `when` chain that also matches against `namePattern` on the trimmed value:

   **Before:**
   ```kotlin
   val firstNameError = if (currentState.firstName.isBlank()) R.string.error_empty_field else null
   val lastNameError  = if (currentState.lastName.isBlank())  R.string.error_empty_field else null
   ```

   **After:**
   ```kotlin
   val firstNameError = when {
       currentState.firstName.isBlank() -> R.string.error_empty_field
       !namePattern.matches(currentState.firstName.trim()) -> R.string.error_invalid_name
       else -> null
   }
   val lastNameError = when {
       currentState.lastName.isBlank() -> R.string.error_empty_field
       !namePattern.matches(currentState.lastName.trim()) -> R.string.error_invalid_name
       else -> null
   }
   ```

3. No changes to `RegisterState.kt`, `RegisterEvent.kt`, or `RegisterScreen.kt` — the `firstNameErrorResId` / `lastNameErrorResId` state fields and their UI wiring already existed prior to this patch (they were previously only ever set to `error_empty_field`), so this is a pure "widen the set of values these fields can take" change with no new state surface required on Register.

**Risk / blast radius:** Low. This only tightens validation on a form submit path; it cannot affect already-registered users, and the change is purely additive to an existing `when`-less `if` expression.

---

### 3.2 Module: `presentation / settings / profile / edit` (Edit Profile screen)

This is the larger module touched by the patch and contains both Bug Cluster A (validation) and Bug Cluster B (stale Flow) fixes. Sub-sections below are grouped by file.

#### 3.2.1 `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/profile/edit/state/EditProfileState.kt`

**Why:** The UI needs somewhere to hold per-field validation error state, following the exact same `Int?` string-resource-id pattern already used elsewhere in the codebase (e.g. Register's `firstNameErrorResId`).

**What:** Add four new nullable fields to the state data class, defaulting to `null` (meaning "no error"):

```kotlin
val heightCm: Double? = null,
val weightKg: Double? = null,
val isSaving: Boolean = false,
/** Client-side validation errors (string resource ids), shown under the relevant field. Null means the field is valid. */
val firstNameErrorResId: Int? = null,
val lastNameErrorResId: Int? = null,
val heightErrorResId: Int? = null,
val weightErrorResId: Int? = null,
val avatarUrl: String? = null,
```

**Risk / blast radius:** Low. Purely additive fields on a data class with existing defaults; does not break any existing `copy()` call site since all four have defaults.

---

#### 3.2.2 `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/profile/edit/viewmodel/EditProfileViewModel.kt`

This file receives the most substantial change of the entire patch: it contains fixes for **both** bug clusters. Presented as two independent sub-changes since they touch different regions of the file and are logically unrelated to each other.

##### 3.2.2.a Bug Cluster A fix — validators + save gate + error-clearing on edit

**Why:** Mirror Register's name check, add the height/weight bound checks that never existed at all, and gate the save-confirmation dialog behind all four checks passing — so an invalid save attempt is caught before it can reach the network layer and produce a confusing generic error.

**What — new private validator functions**, added near the bottom of the file (co-located with other private helpers such as the existing avatar file helper):

```kotlin
/** Only letters (any language), spaces, hyphens and apostrophes — no digits or symbols like #$@%. */
private val namePattern = Regex("^[\\p{L}][\\p{L} '-]*$")

private fun validateName(name: String, isRequired: Boolean): Int? = when {
    name.isBlank() -> if (isRequired) R.string.edit_profile_error_name_required else null
    !namePattern.matches(name.trim()) -> R.string.edit_profile_error_name_invalid
    else -> null
}

private fun validateHeight(heightCm: Double?): Int? = when {
    heightCm == null -> R.string.edit_profile_error_height_required
    heightCm < 50.0 || heightCm > 250.0 -> R.string.edit_profile_error_height_range
    else -> null
}

private fun validateWeight(weightKg: Double?): Int? = when {
    weightKg == null -> R.string.edit_profile_error_weight_required
    weightKg < 2.0 || weightKg > 300.0 -> R.string.edit_profile_error_weight_range
    else -> null
}
```

Notes on design of each validator:
- `validateName` takes an `isRequired` flag so it can be reused for both the required first name and the optional last name, rather than writing two near-duplicate functions.
- `validateHeight` / `validateWeight` treat `null` (i.e. the field was never filled in) as its own distinct "required" error, separate from the "out of range" error, so the user sees "Please enter your height" rather than a range message when the field is simply empty.
- Both range checks are inclusive-exclusive at the boundaries as written (`< 50.0 || > 250.0` means exactly `50.0` and exactly `250.0` are valid, boundary-inclusive) — worth double-checking against product's intended boundary semantics per §2 item 3.

**What — new `onSaveClicked()` orchestration function**, replacing the previous direct one-liner:

**Before**, inline in the `onEvent` `when` block:
```kotlin
EditProfileEvent.SaveClicked -> _state.update { it.copy(showSaveConfirmation = true) }
```

**After**, `onEvent` now delegates to a new function:
```kotlin
EditProfileEvent.SaveClicked -> onSaveClicked()
```

```kotlin
/**
 * Validates all editable fields before showing the save confirmation dialog.
 * Fixes bugs where invalid characters, numbers-as-names, and out-of-range or
 * negative/zero height & weight values were silently accepted or sent to the
 * server (surfacing as a confusing "No internet" error).
 */
private fun onSaveClicked() {
    val current = _state.value
    val firstNameError = validateName(current.firstName, isRequired = true)
    val lastNameError = validateName(current.lastName, isRequired = false)
    val heightError = validateHeight(current.heightCm)
    val weightError = validateWeight(current.weightKg)

    _state.update {
        it.copy(
            firstNameErrorResId = firstNameError,
            lastNameErrorResId = lastNameError,
            heightErrorResId = heightError,
            weightErrorResId = weightError
        )
    }

    val hasErrors = listOf(firstNameError, lastNameError, heightError, weightError).any { it != null }
    if (!hasErrors) {
        _state.update { it.copy(showSaveConfirmation = true) }
    }
}
```

This runs all four validators unconditionally (so all applicable errors surface simultaneously on one Save tap, rather than the user fixing one field at a time and re-discovering the next error on each subsequent attempt), writes all four `*ErrorResId` fields in a single state update, and only opens the save-confirmation dialog if none of the four produced an error.

**What — error-clearing wired into the existing field-update handlers.** Each `Update*` event handler now also clears that specific field's error the instant the user types into it again, rather than leaving a stale error message on screen until the next full Save attempt:

**Before:**
```kotlin
is EditProfileEvent.UpdateFirstName -> _state.update { it.copy(firstName = event.firstName) }
is EditProfileEvent.UpdateLastName -> _state.update { it.copy(lastName = event.lastName) }
...
is EditProfileEvent.UpdateHeight -> _state.update { it.copy(heightCm = event.heightCm) }
is EditProfileEvent.UpdateWeight -> _state.update { it.copy(weightKg = event.weightKg) }
```

**After:**
```kotlin
is EditProfileEvent.UpdateFirstName ->
    _state.update { it.copy(firstName = event.firstName, firstNameErrorResId = null) }
is EditProfileEvent.UpdateLastName ->
    _state.update { it.copy(lastName = event.lastName, lastNameErrorResId = null) }
...
is EditProfileEvent.UpdateHeight ->
    _state.update { it.copy(heightCm = event.heightCm, heightErrorResId = null) }
is EditProfileEvent.UpdateWeight ->
    _state.update { it.copy(weightKg = event.weightKg, weightErrorResId = null) }
```

Note this is an **optimistic clear** — it clears the error as soon as the user types *anything* different into the field, without re-running the validator against the new value. This means a field can show "no error" transiently even if the new value is still invalid, until the user taps Save again and validation re-runs. This is a reasonable, common UX pattern (don't nag the user mid-keystroke) but is worth confirming matches intent versus, say, live-revalidating on every keystroke.

##### 3.2.2.b Bug Cluster B ("Bug-09") fix — freeze editable fields against stale Flow re-emissions while in edit mode

**Why:** As described in §1.2, the Flow observer previously overwrote the entire editable state on every emission regardless of whether the user was actively editing, causing unrelated writes (avatar upload being the concrete trigger) to silently discard in-progress unsaved edits.

**What:** The Flow-collection block inside `syncData()` (or equivalent — the block that maps incoming `User` domain objects onto `_state`) now branches on `it.isEditMode`:

**Before** (single unconditional branch applied to every emission):
```kotlin
it.copy(
    firstName = user.firstName,
    lastName = user.lastName ?: "",
    dateOfBirth = user.dateOfBirth ?: "",
    email = user.email ?: "",
    heightCm = user.heightCm,
    weightKg = user.weightKg,
    avatarUrl = user.avatarUrl,
    avatarUpdatedAt = user.updatedAt,
    selectedDiseaseIds = user.diseaseIds.toPersistentList(),
    selectedAllergyIds = user.allergyIds.toPersistentList()
)
```

**After** (branches on edit-mode status):
```kotlin
// Bug-09 fix: this Flow re-emits on ANY write to the user's DB row —
// including unrelated ones like the avatar upload persisting a new
// URL — not just when the user's own edits are saved. While the user
// is actively editing, applying a stale snapshot here would silently
// wipe out in-progress changes (e.g. newly toggled diseases/allergies)
// seconds before they hit Save. Once in edit mode, only the avatar
// fields (which have their own independent upload flow) are still
// allowed to update live; every other editable field is frozen until
// the user leaves edit mode (cancel/save) and this Flow can safely
// re-sync from the source of truth again.
if (it.isEditMode) {
    it.copy(
        avatarUrl = user.avatarUrl,
        avatarUpdatedAt = user.updatedAt
    )
} else {
    it.copy(
        firstName = user.firstName,
        lastName = user.lastName ?: "",
        dateOfBirth = user.dateOfBirth ?: "",
        email = user.email ?: "",
        heightCm = user.heightCm,
        weightKg = user.weightKg,
        avatarUrl = user.avatarUrl,
        avatarUpdatedAt = user.updatedAt,
        selectedDiseaseIds = user.diseaseIds.toPersistentList(),
        selectedAllergyIds = user.allergyIds.toPersistentList()
    )
}
```

Design rationale, spelled out for reviewers:

- **Why key the branch off `isEditMode` rather than a timestamp or a dirty-bit per field?** Because `isEditMode` is already the ViewModel's own authoritative signal for "the user is actively changing things right now," it requires no new state, no clock dependency, and no per-field dirty-tracking machinery. It is simple to reason about: while `isEditMode == true`, this Flow's non-avatar fields are inert; the moment `isEditMode` flips back to `false`, full syncing resumes automatically on the next emission.
- **Why does avatar keep updating live even in edit mode?** Avatar upload (`uploadAvatar`, `RetryAvatarUpload`, `lastPickedAvatarUri`) is its own independent, user-initiated async pipeline that the user is actively watching for a result *while still nominally "in edit mode."* If avatar were frozen too, a successful upload would appear to silently fail from the user's point of view (they'd see the old avatar even though the upload succeeded), which is arguably a worse regression than the one being fixed. Freezing everything except avatar was a deliberate, narrow scope decision — not "block the Flow outright."
- **Why is this safe / not just moving the bug elsewhere?** Any Flow emission that arrives while `isEditMode` is `false` still applies in full immediately, exactly as it did before this patch — there is no change to behavior outside of an active edit session. The freeze is temporary and self-clearing: it only lasts as long as the user is in edit mode, and the very next emission after edit mode ends brings the screen back in sync with the true server state (including anything that happened to change server-side while the freeze was active, e.g. the disease/allergy list itself being edited elsewhere).

**Risk / blast radius:** Medium. This changes core data-flow behavior in one of the app's central mutable-state screens. It does not touch the save path, network calls, or the domain layer, but see §2 item 5 and §5.2 below — the interaction between this freeze and however `BackClicked`/navigation actually clears `isEditMode` (or doesn't) is not fully verifiable from the diff alone and needs explicit manual QA plus a code-level confirmation from whoever owns the navigation lifecycle for this screen.

---

#### 3.2.3 `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/profile/edit/view/EditProfileScreen.kt`

**Why:** Wire the new `*ErrorResId` state fields through to the two input composables so the user actually sees the validation errors described above.

**What:** Each of the four relevant field composable calls (`EditProfileInputField` for first/last name, `EditProfileMeasurementField` for height/weight) gains a new `errorMessage` argument, resolved from the corresponding state `Int?` via `stringResource(...)` only when non-null:

```kotlin
EditProfileInputField(
    value = state.firstName,
    onValueChange = { onEvent(EditProfileEvent.UpdateFirstName(it)) },
    hint = stringResource(R.string.edit_profile_first_name_hint),
    trailingIconRes = R.drawable.ic_edit,
    isReadOnly = !state.isEditMode,
    errorMessage = state.firstNameErrorResId?.let { stringResource(it) }
)
```

```kotlin
EditProfileInputField(
    value = state.lastName,
    onValueChange = { onEvent(EditProfileEvent.UpdateLastName(it)) },
    hint = stringResource(R.string.edit_profile_last_name_hint),
    trailingIconRes = R.drawable.ic_edit,
    isReadOnly = !state.isEditMode,
    errorMessage = state.lastNameErrorResId?.let { stringResource(it) }
)
```

```kotlin
EditProfileMeasurementField(
    /* height */
    ...
    unit = "cm",
    isReadOnly = !state.isEditMode,
    modifier = Modifier.weight(1f),
    errorMessage = state.heightErrorResId?.let { stringResource(it) }
)

EditProfileMeasurementField(
    /* weight */
    ...
    unit = "kg",
    isReadOnly = !state.isEditMode,
    modifier = Modifier.weight(1f),
    errorMessage = state.weightErrorResId?.let { stringResource(it) }
)
```

**Localization compliance note (per `SKILL.md` §2):** this satisfies the "No Hardcoded User-Facing Strings" rule correctly — every error message flows through `stringResource(id = R.string.key)`, never a literal string in the Composable. See §3.2.5 below for confirmation that the corresponding Arabic (`values-ar`) resources also need to be added, which the current patch **does not** appear to include (flagged as a gap — see §6.1).

**Risk / blast radius:** Low. Purely additive parameters on existing call sites with sensible defaults (`null` = no error, i.e. old behavior).

---

#### 3.2.4 `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/profile/edit/view/components/EditProfileInputField.kt` and `EditProfileMeasurementField.kt`

**Why:** Both composables need to (a) accept the new `errorMessage` parameter, (b) visually indicate an error state on the field border, and (c) render the error text underneath the field when present. Both files receive an **identical structural treatment**, so they are documented together.

**What, applied to both files:**

1. New optional trailing parameter added to the function signature:
   ```kotlin
   errorMessage: String? = null
   ```

2. Border color logic changed to switch to the theme's error color whenever an error is present, per the **Theming & Light/Dark Mode Compatibility** rule in `SKILL.md` §3 (no hardcoded hex — this correctly uses the existing semantic `AppTheme.colors.Error` token rather than introducing a new literal color):
   ```kotlin
   val borderColor = if (errorMessage != null) AppTheme.colors.Error else AppTheme.colors.EditProfileInputBorder
   ```

3. **Structural change — root container changed from a bare `Row` to a `Column` that wraps that same `Row`.** This is necessary because the error text needs to render as a second line *below* the input row, which a flat `Row` cannot accommodate. The `modifier` parameter — previously applied directly to the `Row` — is now applied to the new outer `Column` instead, and the inner `Row` takes a bare `Modifier`:

   **Before (schematic):**
   ```kotlin
   Row(modifier = modifier /* ...height/clip/background/border chain... */) {
       /* field content */
   }
   ```

   **After (schematic):**
   ```kotlin
   Column(modifier = modifier /* .fillMaxWidth() added on EditProfileInputField */) {
       Row(modifier = Modifier /* ...same height/clip/background/border chain, now on Modifier not modifier... */) {
           /* field content, unchanged */
       }
       if (errorMessage != null) {
           Text(
               text = errorMessage,
               fontFamily = PlusJakartaSans,
               fontWeight = FontWeight.Medium,
               fontSize = 12.sp,
               color = AppTheme.colors.Error,
               modifier = Modifier.padding(start = 4.dp, top = 4.dp)
           )
       }
   }
   ```

   This preserves the external layout contract: callers who pass e.g. `Modifier.weight(1f)` (as `EditProfileScreen.kt` does for the height/weight fields, since they sit side-by-side in a `Row`) continue to get correctly-sized fields, because the weight modifier is now honored by the outer `Column` instead of the inner `Row` — functionally equivalent from the caller's perspective, since the `Column` is now the element participating in the parent layout.

   `EditProfileInputField.kt` additionally appends `.fillMaxWidth()` to the outer `Column`'s modifier chain (`Column(modifier = modifier.fillMaxWidth())`), which `EditProfileMeasurementField.kt` does not need since its call sites already constrain width via `.weight(1f)` from the parent `Row`.

4. New required import added to both files: `androidx.compose.foundation.layout.Column`.

**Risk / blast radius:** Low-to-medium. This is a real structural change to shared, reusable form-field composables used across the Edit Profile screen (and potentially elsewhere — a repo-wide search for other call sites of these two composables is recommended before merging, since this document only has visibility into the files present in the diff). Any external caller relying on `RowScope`-specific modifier behavior (as opposed to the generic `Modifier.weight()` used here, which works because the parent `EditProfileScreen.kt` layout is itself a `Row`) should be spot-checked. See §5.4 for the corresponding QA step.

---

#### 3.2.5 `presentation/src/main/res/values/strings.xml`

**Why:** Supply the new user-facing copy needed by the validators and UI wiring above, per the string-resource pattern mandated in `SKILL.md` §2.

**What — new keys added:**

```xml
<string name="error_invalid_name">Only letters, spaces, hyphens and apostrophes are allowed.</string>
```
*(used by Register — added near the existing `error_password_mismatch` / `error_invalid_email` / `error_empty_field` block)*

```xml
<string name="edit_profile_error_name_required">This field cannot be empty</string>
<string name="edit_profile_error_name_invalid">Only letters, spaces, hyphens and apostrophes are allowed</string>
<string name="edit_profile_error_height_required">Please enter your height</string>
<string name="edit_profile_error_height_range">Height must be between 50 and 250 cm</string>
<string name="edit_profile_error_weight_required">Please enter your weight</string>
<string name="edit_profile_error_weight_range">Weight must be between 2 and 300 kg</string>
```
*(used by Edit Profile — added near the existing `edit_profile_height_label` / `edit_profile_weight_label` block)*

**⚠️ Localization gap identified:** `SKILL.md` §2 explicitly requires *"Add Arabic translations to `presentation/src/main/res/values-ar/strings.xml`"* as part of the standard implementation for any new string key. **The diffed patch only modifies `values/strings.xml` (English/default); it does not appear to touch `values-ar/strings.xml` at all.** This is a compliance gap against the project's own documented localization standard and should be treated as a required follow-up before this patch is considered complete — see §6.1 for the explicit action item.

**Risk / blast radius:** Low for the English resource addition itself (purely additive keys, no existing key renamed or removed). The missing Arabic strings are a completeness gap, not a regression risk, but block full compliance with `SKILL.md`.

---

### 3.3 Module: Tests

Per `SKILL.md` §4, ViewModel changes must be accompanied by unit tests using the JUnit/Turbine/Kotlin-Coroutines-Test pattern. The existing test suites for both affected ViewModels (`RegisterViewModelTest.kt`, `EditProfileViewModelTest.kt`) already follow a JUnit4 + `runTest` idiom (rather than the JUnit5 `@BeforeEach`/`@AfterEach` template shown in `SKILL.md` §4, which appears to be the *intended standard for new features* rather than a retrofit requirement for pre-existing suites) — the patch's new tests correctly follow the existing local convention of the file they're added to, rather than mixing test-runner styles within a single file, which is the right call for consistency even though it doesn't match the JUnit5 template verbatim.

#### 3.3.1 `presentation/src/test/kotlin/iti/grad/nutriscan/presentation/auth/register/viewmodel/RegisterViewModelTest.kt` — 3 new tests

1. **`SignUpClicked with symbols in first or last name sets error`** — submits `firstName = "#$@"`, `lastName = "#%#"` with an otherwise-valid email/password, asserts both `firstNameErrorResId` and `lastNameErrorResId` are non-null after `SignUpClicked`.
2. **`SignUpClicked with digits in first or last name sets error`** — same shape, with `firstName = "123"`, `lastName = "12345"`.
3. **`SignUpClicked with valid hyphenated name has no name error`** — positive-path regression test using `firstName = "Anne-Marie"`, `lastName = "O'Brien"`; mocks `registerUseCase("Anne-Marie", "O'Brien", email, password)` to return success and asserts both error fields remain `null`. This is the critical "did we accidentally over-tighten the regex" guard rail — without it, a future accidental regex tightening (e.g. someone "fixing" the apostrophe handling and breaking it) would go undetected.

#### 3.3.2 `presentation/src/test/java/iti/grad/nutriscan/presentation/settings/profile/edit/EditProfileViewModelTest.kt` — approximately 10 new tests plus 3 existing tests updated

**Bug-09 (stale Flow) regression coverage — 3 new tests:**

1. **`a profile Flow re-emission during edit mode does not wipe unsaved disease and allergy selections`** — the core regression test for the bug as originally reported. Sequence: seed a `User` with `diseaseIds = [1]`, `allergyIds = [2]`; enter edit mode; toggle on a brand-new disease (`99`) and allergy (`88`) that are not yet saved; then simulate the underlying Flow re-emitting with only `updatedAt` changed (modeling an unrelated write, e.g. the avatar-upload trigger, without changing any of the actual profile data) — advance the test dispatcher; assert the newly toggled `99`/`88` selections **and** the original `firstName` are all still present in state, i.e. nothing was silently reverted.
2. **`a profile Flow re-emission still updates the avatar live during edit mode`** — confirms the avatar carve-out specifically: while in edit mode, an emission carrying a new `avatarUrl` **does** propagate to `state.avatarUrl`, proving the freeze is correctly scoped to exclude avatar rather than accidentally freezing everything including avatar.
3. **`leaving edit mode lets the profile Flow re-sync normally again`** — attempts to verify the freeze correctly thaws once the user leaves edit mode. **Caveat, called out directly in the test's own inline comments and repeated here for visibility:** this test invokes `EditProfileEvent.BackClicked` to attempt to leave edit mode, but per §2 item 5 and §5.2, it is not confirmed that `BackClicked` actually resets `isEditMode` to `false` in the current implementation — the test's own final assertion, as authored, still expects the *frozen* value after `BackClicked`, which the test's comment explicitly flags as reflecting current (possibly incomplete) behavior rather than confirmed-correct behavior. **This test should be treated as documenting current behavior, not as proof the thaw path is correct — it needs a human follow-up**, not just a passing CI run.

**Bug-14/15/16/17 (field validation) regression coverage — 7 new tests:**

4. **`SaveClicked blocks and reports errors when first name is blank`** — height/weight filled in validly, first name left blank; asserts `showSaveConfirmation` stays `false` and `firstNameErrorResId` is set.
5. **`SaveClicked blocks names containing symbols`** — all fields valid except first/last name set to `"#$@"` / `"#%#"`; asserts both name errors set and dialog blocked.
6. **`SaveClicked blocks names containing digits`** — same shape with `"123"` / `"12345"`.
7. **`SaveClicked blocks negative, zero, and out-of-range height and weight`** — `height = 999.0`, `weight = -70.0`; asserts both range errors set.
8. **`SaveClicked blocks zero height and weight`** — `height = 0.0`, `weight = 0.0`; asserts both errors set (distinct test from #7 to explicitly cover the zero-boundary case rather than assuming it's covered by the "negative/out-of-range" test alone).
9. **`editing a field after a failed save clears that field's error`** — sets an invalid first name (`"123"`), triggers `SaveClicked` (fails, error set), then issues `UpdateFirstName("Ahmed")` and asserts `firstNameErrorResId` is immediately `null` again (validating the optimistic-clear-on-edit behavior described in §3.2.2.a) without needing another `SaveClicked` to clear it.

**Existing tests updated to accommodate the new validation gate — 3 tests modified, not added:**

10. **`SaveClicked shows the save confirmation dialog`** → renamed to **`SaveClicked shows the save confirmation dialog when all fields are valid`** and now calls a new private test helper `fillValidProfileFields()` (which issues `UpdateFirstName("Ahmed")`, `UpdateLastName("Ali")`, `UpdateHeight(180.0)`, `UpdateWeight(75.0)`) before `SaveClicked`, since the un-gated old test would now fail against the new validation logic if fields were left at their default/blank state.
11. **`ConfirmSave success clears the dialog, exits edit mode and stops saving`** — same `fillValidProfileFields()` call added before `SaveClicked`/`ConfirmSave`, for the same reason.
12. **`ConfirmSave failure ...` (network-failure path)** — same treatment.

**Risk / blast radius of test changes:** None to production code; strictly additive/corrective to the test suite. The one item needing follow-up is test #3 above, which is a documentation/verification gap rather than a broken test — it currently passes, but arguably passes by asserting *current, unverified* behavior rather than *intended* behavior.

---

## 4. Module Summary Table

| Module | Files Changed | Bug Cluster(s) Addressed |
|---|---|---|
| `auth/register` | `RegisterViewModel.kt`, `RegisterViewModelTest.kt` | A (Register name validation) |
| `settings/profile/edit` — state | `EditProfileState.kt` | A (new error fields) |
| `settings/profile/edit` — viewmodel | `EditProfileViewModel.kt` | A + B (validation gate, error-clearing, stale-Flow freeze) |
| `settings/profile/edit` — view | `EditProfileScreen.kt`, `EditProfileInputField.kt`, `EditProfileMeasurementField.kt` | A (error UI wiring + layout change) |
| `settings/profile/edit` — tests | `EditProfileViewModelTest.kt` | A + B (regression coverage) |
| Resources | `values/strings.xml` | A (new copy; **`values-ar/strings.xml` counterpart missing — see §6.1**) |

---

## 5. Verification Plan

### 5.1 Automated Verification

1. **Run the full `presentation` module unit test suite** (Gradle: `./gradlew :presentation:test`, or the project's equivalent test task) and confirm:
   - All pre-existing tests in `RegisterViewModelTest.kt` and `EditProfileViewModelTest.kt` still pass unmodified except for the three explicitly updated ones noted in §3.3.2.
   - All 3 new Register tests pass (§3.3.1).
   - All ~10 new/updated Edit Profile tests pass (§3.3.2), with special attention to test #3 (`leaving edit mode lets the profile Flow re-sync normally again`) — a passing result here does **not** by itself confirm the underlying `BackClicked`/`isEditMode` interaction is correct; see manual step 5.2.4 below.
2. **Static/lint check** that no new hardcoded string literals were introduced in any touched Composable, per `SKILL.md` §2 — specifically re-inspect `EditProfileInputField.kt` / `EditProfileMeasurementField.kt`'s new inline error `Text(...)` composables to confirm `text = errorMessage` is always sourced from a `stringResource(...)` call at the call site (`EditProfileScreen.kt`) and never a literal.
3. **Static/lint check** that no new hardcoded color hex values were introduced, per `SKILL.md` §3 — confirm the new border-color and error-text-color logic in both field composables references `AppTheme.colors.Error` exclusively, with no `Color(0xFF...)` literal anywhere in the diff.
4. **Regex unit-level sanity check** (can be added as a lightweight additional test if not already implicitly covered): run `namePattern` against a small matrix of inputs — `"Ahmed"`, `"Anne-Marie"`, `"O'Brien"`, `"محمد"` (Arabic script, to confirm `\p{L}` Unicode support given Arabic is a first-class supported locale per `SKILL.md` §2), `"123"`, `"#$@"`, `"John3"`, `""`, `" "`, `"-Jean"`, `"'t Hooft"` — and confirm the pass/fail result for each matches the intent confirmed in §2 item 1.
5. **Confirm no other call sites** of `EditProfileInputField` or `EditProfileMeasurementField` exist elsewhere in the codebase that were not part of this diff and might be affected by the `Row` → `Column`-wrapping-`Row` structural change (§3.2.4) — a simple repo-wide usage search (`grep -r "EditProfileInputField(" presentation/src` and equivalent for the measurement field) should be run as part of CI or manual review, since this document's visibility is limited to files actually present in the provided diff.

### 5.2 Manual QA

1. **Register — name validation walkthrough.**
   Attempt sign-up with each of: a digit-only name (`"123"`), a symbol-only name (`"#$@"`), a blank name, a valid hyphenated name (`"Anne-Marie"`), a valid apostrophe'd name (`"O'Brien"`), and (per §2 item 1) a non-Latin-script name if the app's locale/keyboard supports it. Confirm: invalid cases show the correct inline error and block submission; valid cases (including hyphen/apostrophe/non-Latin) submit successfully with no false-positive error.

2. **Edit Profile — validation walkthrough.**
   In edit mode, attempt to Save with each of: blank first name, invalid (digit/symbol) first name, invalid last name, blank height, blank weight, negative height/weight, zero height/weight, and an out-of-range value just past each boundary (e.g. `49.9` and `250.1` cm; `1.9` and `300.1` kg) as well as exactly on each boundary (`50.0`/`250.0` cm, `2.0`/`300.0` kg) to confirm the inclusive/exclusive boundary behavior matches what's confirmed in §2 item 3. For each invalid case, confirm: (a) the correct field(s) show the red/error border and the correct inline error text underneath, (b) the save-confirmation dialog does **not** open, (c) other, valid fields do **not** show a spurious error. Then confirm that editing an errored field to a valid value clears that field's error immediately (without needing another Save tap) and that a subsequent Save with all fields valid succeeds normally.

3. **Edit Profile — Bug-09 stale-Flow walkthrough (the most important and hardest-to-automate scenario).**
   Enter edit mode on Edit Profile. Toggle on a disease and/or allergy chip that was not previously selected (do **not** tap Save yet). While still in this unsaved state, trigger an avatar upload (or, if a test/debug hook is available, otherwise force a write to the same user DB row through another path) and let it complete. Confirm: (a) the newly toggled disease/allergy chip **remains toggled on** and was not silently reverted; (b) the avatar visibly updates to the newly uploaded image while this is happening; (c) none of the other in-progress unsaved field edits (name, height, weight if also mid-edit) were reverted either. Then tap Save and confirm the save succeeds and persists both the profile field changes and reflects the correct final avatar.

4. **Targeted follow-up on §2 item 5 / test §3.3.2#3 caveat — edit-mode exit correctness.**
   Enter edit mode, make an edit, then leave the screen via the back button/gesture **without saving**. Re-enter Edit Profile (from scratch, via normal navigation) and confirm the screen correctly reflects the latest true server state (i.e. the unsaved edit from the previous visit is gone, as expected, and nothing is stuck showing stale/frozen data from the previous session). Pay particular attention to whether this holds true after a process death / app-backgrounded-and-restored scenario, since that is the case most likely to retain a ViewModel instance across the "auto-cleared by fresh instantiation" assumption noted in §2 item 5. **This manual step should be treated as blocking** — it verifies a scenario that automated test coverage explicitly flagged as unconfirmed (§3.3.2, item 3's caveat).

5. **Cross-check with `SKILL.md` §2 localization requirement.**
   With the device/app locale set to Arabic, navigate to both Register and Edit Profile and trigger each new validation error. Confirm whether the new strings fall back to English (expected, given the gap identified in §3.2.5/§6.1) or whether Arabic strings were in fact added out-of-band and simply not visible in the diff provided for this review — either way, resolve the discrepancy explicitly before sign-off.

6. **Regression pass on unrelated, previously-working Edit Profile flows**, to confirm the `Row`→`Column` layout change (§3.2.4) introduced no visual regressions: confirm field height, spacing, alignment, and the existing edit/read-only visual states (border color, text color, trailing edit icon) all look identical to before the patch when `errorMessage` is `null`, in both light and dark theme (per `SKILL.md` §3, since the border/text color logic now branches on error state on top of the existing theme-aware color tokens).

7. **Confirm the four Calories/WaterTracker-related files** (`WaterTrackerCard.kt`, `CaloriesViewModel.kt`, `CaloriesScreen.kt`, `CaloriesEvent.kt`, `CaloriesViewModelTest.kt`) present in the bugfix archive are genuinely no-ops as determined by this review (§6) and were not meant to carry an additional, undelivered fix that got lost when the archive was assembled — confirm directly with whoever prepared `NutriScan-bugfixes(1).zip`.

---

## 6. Out-of-Scope / No-Op Files & Follow-Up Action Items

### 6.1 Confirmed no-op files (byte-for-byte identical to baseline)

The following files are present in `NutriScan-bugfixes(1).zip` but are **byte-for-byte identical** to their counterparts in `NutriScan.zip`. They introduce no functional change whatsoever and should be excluded from the code-review diff / PR description for this bugfix batch, to avoid reviewer confusion:

- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/components/WaterTrackerCard.kt`
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/main/calories/viewmodel/CaloriesViewModel.kt`
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/main/calories/view/CaloriesScreen.kt`
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/main/calories/state/CaloriesEvent.kt`
- `presentation/src/test/kotlin/iti/grad/nutriscan/presentation/main/calories/CaloriesViewModelTest.kt`

**Action item:** Confirm with whoever assembled the archive whether a Calories-related fix was intended but accidentally omitted before the zip was created, or whether these files were simply carried along incidentally (e.g. because they were touched in the same branch/commit for unrelated reasons) and can be safely dropped from this release's file list.

### 6.2 Required follow-up: Arabic string resources

Per `SKILL.md` §2, every new string key requires a corresponding entry in `presentation/src/main/res/values-ar/strings.xml`. The 7 new keys introduced by this patch (`error_invalid_name`, `edit_profile_error_name_required`, `edit_profile_error_name_invalid`, `edit_profile_error_height_required`, `edit_profile_error_height_range`, `edit_profile_error_weight_required`, `edit_profile_error_weight_range`) do not appear to have Arabic counterparts in the diffed archive.

**Action item:** Add Arabic translations for all 7 keys to `values-ar/strings.xml` before this patch is merged, or explicitly document why this is being deferred (e.g. translations pending from a localization vendor) if it cannot block the bugfix release.

### 6.3 Recommended (non-blocking) technical-debt follow-ups

1. Extract the duplicated `namePattern` regex (currently defined identically in both `RegisterViewModel.kt` and `EditProfileViewModel.kt`) into a single shared validation utility, to prevent the two rules silently drifting apart in a future change.
2. Extract the height/weight bound constants (`50.0`/`250.0`, `2.0`/`300.0`) out of inline magic numbers in `EditProfileViewModel.kt` into named constants (and ideally confirm/sync them against whatever the backend enforces, per §2 item 3).
3. Resolve the `BackClicked` / `isEditMode` interaction explicitly (§2 item 5) — either confirm the ViewModel is always freshly instantiated on re-entry (making this a non-issue) or add an explicit `isEditMode = false` reset to the `BackClicked` handler for defense-in-depth, and update test §3.3.2#3's assertion once the intended behavior is confirmed, so it verifies correct behavior rather than documenting current behavior with a caveat.
4. Consider whether `validateName`'s `isRequired` asymmetry between Register (both names required) and Edit Profile (last name optional) is intentional product behavior worth a one-line code comment explaining the divergence, so a future maintainer doesn't "fix" it into unwanted symmetry.
