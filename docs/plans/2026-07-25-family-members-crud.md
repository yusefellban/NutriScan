# Family Members — Production Implementation Plan

## 1. Goal Description

Today, "Add Member" on the User Profile screen (`UserProfileViewModel.addMockMember()`) appends a
hardcoded, in-memory `FamilyMemberUiModel` ("Ashraf Shrief" + a pravatar URL). Nothing is persisted,
nothing is sent to the backend, and a process death loses all added members.

We need to replace this with a real, production-grade feature:

1. Tapping **Add Member** opens a **bottom sheet** that collects a family member's name, allergies,
   and diseases, built from our existing reusable selection components (`SelectableChip`, the
   Diseases/Allergies FlowRow sections already used in `HealthProfileContent`).
2. Submitting the sheet **persists the member to the backend** and to **Room** (offline-first,
   optimistic updates, matching the existing `UserRepositoryImpl` pattern).
3. The Family Members row on the Profile screen **reads from Room** (single source of truth), so it
   survives process death and works offline.
4. Full **localization** (EN/AR strings, no hardcoded copy) and **theme** (light/dark via
   `AppTheme.colors`, no hardcoded hex) support.
5. Clean Architecture consistent with the rest of the app: `domain` (models, repository interface,
   use cases) → `data` (DTOs, Room entity/DAO, remote data source, repository impl) → `presentation`
   (MVI: state/event/effect, ViewModel, Composables).

**Out of scope for this plan:** editing/removing a member's own avatar image (no upload endpoint was
found — see §4 Open Questions), and the "Family Member Detail" screen's own content (only the
navigation target `FamilyMemberDetailClicked` already exists and is unaffected).

---

## 2. ⚠️ User Review Required — Backend Contract Ambiguity

This is the single biggest risk in this plan and **must be resolved with the backend team before
implementation starts.** I read `Nutriscan.postman_collection2.json` in full. Findings:

- There is **no dedicated `family-members` REST resource** in the collection (no
  `POST/GET/PATCH/DELETE /api/v1/users/family-members` or similar).
- Family members only appear as a `familyMembers` array nested inside two existing calls:
  - `POST /api/v1/auth/register` — request body includes `familyMembers: [{ name, allergyIds,
    diseaseIds }]` (no `id` field — server presumably generates one on creation).
  - `PATCH /api/v1/users/profile` — request body includes the same `familyMembers` array shape.
- The corresponding `GET /api/v1/users/profile` ("get full user details") response is not captured
  in the collection (empty `"response": []`), so **we do not have a confirmed sample response
  showing whether returned family members include a server-generated `id`, an `avatarUrl` field, or
  are keyed some other way.**

**Working assumption for this plan** (must be confirmed with backend before coding):
- `PATCH /api/v1/users/profile` treats `familyMembers` as a **full replace** of the list (not a
  delta/patch) — i.e., to add one member you must resend the complete existing list plus the new
  entry; to remove one, resend the list without it.
- The response of `GET /api/v1/users/profile` (and the 200 body of the `PATCH`, if any) returns each
  family member as an object shaped like:
  ```json
  { "id": "string-or-int", "name": "string", "allergyIds": [1,2], "diseaseIds": [3] }
  ```
- Family members do **not** currently support an `avatarUrl` from the backend — the "person
  placeholder" vector asset is therefore the only avatar shown for now (see §3.5).

**Action item for the developer:** Before writing `data` layer code, confirm with backend:
1. Does `PATCH /api/v1/users/profile` fully replace `familyMembers`, or does it merge by `id`?
2. What is the exact JSON shape of a family member in the `GET /api/v1/users/profile` response
   (field names/casing — the rest of `UserDto` uses `snake_case` over the wire via `@SerialName`,
   e.g. `first_name`, but the Postman body samples for `familyMembers` show `camelCase` keys
   `allergyIds`/`diseaseIds` — this inconsistency needs to be clarified, not guessed)?
3. Is there a dedicated single-member endpoint (`POST .../family-members`,
   `DELETE .../family-members/{id}`) that isn't in this Postman export? If yes, **strongly prefer
   using it** instead of full-list PATCH — it is far safer for concurrent/offline edits and avoids
   accidentally overwriting members added from another device. If confirmed to exist, swap the
   "Proposed Changes" data-layer calls below for the dedicated endpoints (same domain/presentation
   layers apply unchanged).
4. Is there any avatar upload endpoint for a family member (mirroring how the user's own
   `avatarUrl` presumably gets set)? If not, confirm the placeholder-only approach is acceptable.

The rest of this plan is written against the **full-replace-via-PATCH** assumption since that's the
only thing evidenced in the collection, with the DTO/repository code isolated so it's a small,
contained change if the backend team instead confirms a dedicated endpoint.

---

## 3. Proposed Changes (by module)

### 3.1 `domain` module

**New file** `domain/family/model/FamilyMember.kt`
```kotlin
package iti.grad.nutriscan.domain.family.model

data class FamilyMember(
    val id: String,
    val name: String,
    val allergyIds: List<Int> = emptyList(),
    val diseaseIds: List<Int> = emptyList(),
)
```

**New file** `domain/family/model/FamilyMemberInput.kt` (payload for create/update; no `id` — the
name mirrors `ProfileUpdate`'s "input DTO for domain" pattern)
```kotlin
package iti.grad.nutriscan.domain.family.model

data class FamilyMemberInput(
    val name: String,
    val allergyIds: List<Int> = emptyList(),
    val diseaseIds: List<Int> = emptyList(),
)
```

**New file** `domain/family/repository/IFamilyMemberRepository.kt`
```kotlin
package iti.grad.nutriscan.domain.family.repository

import iti.grad.nutriscan.domain.family.model.FamilyMember
import iti.grad.nutriscan.domain.family.model.FamilyMemberInput
import kotlinx.coroutines.flow.Flow

interface IFamilyMemberRepository {
    /** Single source of truth — reads from Room, updated by profile sync. */
    fun getFamilyMembers(): Flow<List<FamilyMember>>

    /** Adds a member: optimistic local insert, then syncs the full list to backend. */
    suspend fun addFamilyMember(input: FamilyMemberInput): Result<Unit>

    /** Removes a member: optimistic local delete, then syncs the full list to backend. */
    suspend fun removeFamilyMember(memberId: String): Result<Unit>
}
```

**New file** `domain/family/usecase/GetFamilyMembersUseCase.kt` — wraps
`repository.getFamilyMembers()`, same one-liner style as `GetUserProfileUseCase`.

**New file** `domain/family/usecase/AddFamilyMemberUseCase.kt`
```kotlin
class AddFamilyMemberUseCase @Inject constructor(
    private val repository: IFamilyMemberRepository
) {
    suspend operator fun invoke(
        name: String,
        allergyIds: List<Int>,
        diseaseIds: List<Int>,
    ): Result<Unit> = repository.addFamilyMember(FamilyMemberInput(name, allergyIds, diseaseIds))
}
```

**New file** `domain/family/usecase/RemoveFamilyMemberUseCase.kt` — wraps
`repository.removeFamilyMember(memberId)`.

> Reuse existing `GetAllergiesUseCase` / `GetDiseasesUseCase` (already offline-first via Room) to
> populate the bottom sheet's chip lists — no new domain code needed for those.

---

### 3.2 `data` module

**New Room entity** `data/db/entity/FamilyMemberEntity.kt`
```kotlin
@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey val id: String,
    val ownerUserId: String,       // FK-style link to the users row; supports multi-account cache clears
    val name: String,
    val allergyIds: List<Int> = emptyList(),
    val diseaseIds: List<Int> = emptyList(),
)
```
Reuses the existing `IntListConverter` already registered on `NutriScanDatabase` — no new
`TypeConverter` needed.

**New DAO** `data/db/dao/FamilyMemberDao.kt`
```kotlin
@Dao
interface FamilyMemberDao {
    @Query("SELECT * FROM family_members WHERE ownerUserId = :ownerUserId")
    fun getFamilyMembersFlow(ownerUserId: String): Flow<List<FamilyMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMember(member: FamilyMemberEntity)

    @Query("SELECT * FROM family_members WHERE ownerUserId = :ownerUserId")
    suspend fun getFamilyMembersOnce(ownerUserId: String): List<FamilyMemberEntity>

    /** Full replace of the cached list for this user — used after a successful sync. */
    @Query("DELETE FROM family_members WHERE ownerUserId = :ownerUserId")
    suspend fun clearForUser(ownerUserId: String)

    @Query("DELETE FROM family_members WHERE id = :memberId")
    suspend fun deleteById(memberId: String)

    @Transaction
    suspend fun replaceAllForUser(ownerUserId: String, members: List<FamilyMemberEntity>) {
        clearForUser(ownerUserId)
        members.forEach { insertOrUpdateMember(it) }
    }
}
```

**Register in `NutriScanDatabase.kt`:**
- Add `FamilyMemberEntity::class` to `entities = [...]`.
- Add `abstract fun familyMemberDao(): FamilyMemberDao`.
- **Bump `version = 4` → `version = 5`.** Per SKILL.md's existing `fallbackToDestructiveMigration()`
  in `DatabaseModule`, no manual `Migration` object is required (matches how prior entities —
  `ExerciseEntity`, `AllergyEntity`, etc. — were added), but flag this in the PR description since
  destructive migration wipes local cache on upgrade.

**`DatabaseModule.kt`:** add
```kotlin
@Provides @Singleton
fun provideFamilyMemberDao(db: NutriScanDatabase) = db.familyMemberDao()
```

**DTOs** — reuse the existing field-name inconsistency pattern already in the codebase (`UserDto`
uses `@SerialName` snake_case, but `familyMembers` in the Postman bodies are camelCase — see §2).
`data/remote/dto/FamilyMemberDto.kt`:
```kotlin
@Serializable
data class FamilyMemberDto(
    val id: String? = null,          // null when sending a new member in the request
    val name: String,
    val allergyIds: List<Int> = emptyList(),
    val diseaseIds: List<Int> = emptyList(),
)
```

**Extend `UserDto.kt`** with:
```kotlin
@SerialName("family_members") val familyMembers: List<FamilyMemberDto>? = null
```
> ⚠️ Per §2, confirm the actual wire key/casing for this field with backend before merging — it may
> need to be `familyMembers` (no `@SerialName`) rather than `family_members`, since the Postman
> **request** bodies use camelCase while the rest of `UserDto` uses snake_case over the wire. Do not
> guess in code review; get written confirmation.

**Extend `UpdateUserProfileRequestDto.kt`** with:
```kotlin
val familyMembers: List<FamilyMemberDto>? = null
```

**No changes needed to `UserApiService`** — both `getProfile()` (`GET /v1/users/profile`) and
`updateProfile()` (`PATCH /v1/users/profile`) already carry the full `UserDto` /
`UpdateUserProfileRequestDto` payloads, so family members ride along automatically once the DTOs
above are added. No new Retrofit endpoints are required under the full-replace assumption.

**New file** `data/repository/FamilyMemberRepositoryImpl.kt`
```kotlin
class FamilyMemberRepositoryImpl @Inject constructor(
    private val familyMemberDao: FamilyMemberDao,
    private val userDao: UserDao,                       // to read current user id + resync trigger
    private val remoteDataSource: IUserRemoteDataSource, // reuse — updateProfile() already carries familyMembers
    private val json: Json,
) : IFamilyMemberRepository {

    override fun getFamilyMembers(): Flow<List<FamilyMember>> =
        userDao.getUserFlow().flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else familyMemberDao.getFamilyMembersFlow(user.id).map { entities ->
                entities.map { FamilyMember(it.id, it.name, it.allergyIds, it.diseaseIds) }
            }
        }

    override suspend fun addFamilyMember(input: FamilyMemberInput): Result<Unit> {
        val user = userDao.getUserFlow().firstOrNull()
            ?: return Result.failure(IllegalStateException("No local user; cannot add family member"))

        val existing = familyMemberDao.getFamilyMembersOnce(user.id)
        // OPTIMISTIC INSERT with a temporary client-side id so the UI updates instantly.
        val tempId = "local_${'$'}{System.currentTimeMillis()}"
        val optimisticEntity = FamilyMemberEntity(tempId, user.id, input.name, input.allergyIds, input.diseaseIds)
        familyMemberDao.insertOrUpdateMember(optimisticEntity)

        val fullList = existing + optimisticEntity
        return syncListToBackend(user.id, fullList, rollbackTo = existing)
    }

    override suspend fun removeFamilyMember(memberId: String): Result<Unit> {
        val user = userDao.getUserFlow().firstOrNull()
            ?: return Result.failure(IllegalStateException("No local user; cannot remove family member"))

        val existing = familyMemberDao.getFamilyMembersOnce(user.id)
        familyMemberDao.deleteById(memberId) // optimistic removal
        val fullList = existing.filterNot { it.id == memberId }
        return syncListToBackend(user.id, fullList, rollbackTo = existing)
    }

    /** Sends the full family-member list via PATCH /v1/users/profile, then reconciles Room
     *  with the server's response (so the temp id gets replaced with the real server id). */
    private suspend fun syncListToBackend(
        userId: String,
        fullList: List<FamilyMemberEntity>,
        rollbackTo: List<FamilyMemberEntity>,
    ): Result<Unit> {
        return try {
            val request = UpdateUserProfileRequestDto(
                familyMembers = fullList.map { FamilyMemberDto(
                    id = it.id.takeUnless { id -> id.startsWith("local_") },
                    name = it.name,
                    allergyIds = it.allergyIds,
                    diseaseIds = it.diseaseIds,
                ) }
            )
            val response = remoteDataSource.updateProfile(request)
            if (response.isSuccessful) {
                // Re-fetch full profile so server-assigned ids/fields are reconciled into Room.
                val refreshed = remoteDataSource.getProfile()
                val entities = refreshed.familyMembers.orEmpty().map {
                    FamilyMemberEntity(it.id ?: it.name, userId, it.name, it.allergyIds, it.diseaseIds)
                }
                familyMemberDao.replaceAllForUser(userId, entities)
                Result.success(Unit)
            } else {
                familyMemberDao.replaceAllForUser(userId, rollbackTo) // ROLLBACK
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            familyMemberDao.replaceAllForUser(userId, rollbackTo) // ROLLBACK (offline, timeout, etc.)
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(errorBody: String?): String { /* identical to UserRepositoryImpl's */ }
}
```
This mirrors `UserRepositoryImpl.updateProfile()`'s optimistic-update-then-rollback pattern exactly,
and reuses `IUserRemoteDataSource` rather than inventing a parallel remote data source — there is
only one backend call involved (`PATCH /v1/users/profile`).

**`UserRepositoryImpl.fetchAndSyncProfile()`** must also be extended to persist
`dto.familyMembers` into `FamilyMemberDao` (via injecting `FamilyMemberDao`), so that a normal
profile refresh (app launch, pull-to-refresh, etc.) keeps the family member cache in sync too, not
just the add/remove flows.

**DI registration** (`RepositoryModule.kt`):
```kotlin
@Binds @Singleton
abstract fun bindFamilyMemberRepository(impl: FamilyMemberRepositoryImpl): IFamilyMemberRepository
```
No `DataSourceModule` change needed (no new remote data source).

---

### 3.3 `presentation` module — Bottom Sheet

**New file** `presentation/settings/profile/view/components/AddFamilyMemberBottomSheet.kt`

Structure (mirrors `ExerciseInstructionsBottomSheet`'s `ModalBottomSheet` usage and
`HealthProfileContent`'s allergy/disease `FlowRow` + `SelectableChip` sections — **reuse those two
private composables' logic by extracting them**, see below):

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFamilyMemberBottomSheet(
    state: AddFamilyMemberState,
    onEvent: (AddFamilyMemberEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppTheme.colors.Surface,
        scrimColor = AppTheme.colors.ScrimOverlay, // NOT Color(0x66...) hardcoded — see §3.5
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Person placeholder avatar, centered
            Icon(
                painter = painterResource(R.drawable.ic_person_solid),
                contentDescription = null,
                tint = AppTheme.colors.Teal1000,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.Teal1600)
                    .padding(14.dp),
            )
            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.add_family_member_title),
                style = AppTheme.typography.headlineMedium,
                color = AppTheme.colors.Teal1000,
            )
            Spacer(Modifier.height(16.dp))

            // Name field — reuse existing reusable text field component (FigmaInputField),
            // not a raw OutlinedTextField, to match app style/theming.
            FigmaInputField(
                value = state.name,
                onValueChange = { onEvent(AddFamilyMemberEvent.NameChanged(it)) },
                label = stringResource(R.string.add_family_member_name_label),
                placeholder = stringResource(R.string.add_family_member_name_placeholder),
                errorMessage = state.nameError?.let { stringResource(it) },
            )
            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.profile_setup_chronic_conditions),
                style = AppTheme.typography.headlineMedium,
                color = AppTheme.colors.ProfileSetupSectionTitle,
            )
            Spacer(Modifier.height(12.dp))
            ChipSelectionFlowRow(
                items = state.diseases,
                selectedIds = state.selectedDiseaseIds,
                isLoading = state.isDiseasesLoading,
                errorMessage = state.diseasesErrorMessage,
                onToggle = { onEvent(AddFamilyMemberEvent.ToggleDisease(it)) },
                onRetry = { onEvent(AddFamilyMemberEvent.RetryLoadDiseases) },
            )
            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.profile_setup_allergies),
                style = AppTheme.typography.headlineMedium,
                color = AppTheme.colors.ProfileSetupSectionTitle,
            )
            Spacer(Modifier.height(12.dp))
            ChipSelectionFlowRow(
                items = state.allergies,
                selectedIds = state.selectedAllergyIds,
                isLoading = state.isAllergiesLoading,
                errorMessage = state.allergiesErrorMessage,
                onToggle = { onEvent(AddFamilyMemberEvent.ToggleAllergy(it)) },
                onRetry = { onEvent(AddFamilyMemberEvent.RetryLoadAllergies) },
            )
            Spacer(Modifier.height(32.dp))

            AppButton(
                textResId = R.string.action_save,
                isLoading = state.isSaving,
                enabled = state.name.isNotBlank(),
                onClick = { onEvent(AddFamilyMemberEvent.SaveClicked) },
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}
```

**Required refactor (not net-new code): extract a shared chip-selection composable.**
`HealthProfileContent.kt` currently has two near-identical private composables,
`DiseasesSection` and `AllergiesSection`, both just a `FlowRow` of `SelectableChip`s with the same
loading/error states. Per the "use all reusable components as possible" requirement, promote a
single generic version into `presentation/common/components/`:

**New file** `presentation/common/components/ChipSelectionFlowRow.kt`
```kotlin
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipSelectionFlowRow(
    items: List<T>,
    selectedIds: List<Int>,
    isLoading: Boolean,
    errorMessage: String?,
    idOf: (T) -> Int,
    labelOf: (T) -> String,
    onToggle: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        isLoading -> CircularProgressIndicator(
            modifier = Modifier.size(24.dp), color = AppTheme.colors.Primary, strokeWidth = 2.dp,
        )
        errorMessage != null -> Column {
            Text(errorMessage, style = AppTheme.typography.bodyMedium, color = AppTheme.colors.Error)
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.action_retry), style = AppTheme.typography.labelLarge, color = AppTheme.colors.Primary)
            }
        }
        else -> FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.fillMaxWidth(),
        ) {
            items.forEach { item ->
                SelectableChip(
                    text = labelOf(item),
                    isSelected = selectedIds.contains(idOf(item)),
                    onClick = { onToggle(idOf(item)) },
                )
            }
        }
    }
}
```
Then refactor `HealthProfileContent.kt`'s `DiseasesSection`/`AllergiesSection` to delegate to this
(`idOf = Disease::id`, etc.), deleting the duplicated logic — this is a small, low-risk cleanup that
directly serves both features and satisfies the "use reusable components as possible" requirement
without touching `ProfileSetupPagerViewModel`'s public contract. `SelectableChip` itself needs no
changes; `ChipSelectionFlowRow` is generic (`<T>`) specifically so both `Allergy` and `Disease`
plug in without duplicating the composable.

---

### 3.4 `presentation` module — State / Event / Effect / ViewModel

The sheet's own transient form state (name input, selected chip ids, chip lists, loading/error/
saving flags) is **local to the sheet**, not the parent `UserProfileState`, so it doesn't pollute
the profile screen's state with form-editing concerns — same separation `EditProfileViewModel`
already uses versus `UserProfileViewModel`.

**Option A (recommended): dedicated ViewModel for the sheet**, consistent with how
`EditProfileViewModel` is separate from `UserProfileViewModel` even though both touch `IUserRepository`.

**New file** `presentation/settings/profile/add_member/state/AddFamilyMemberState.kt`
```kotlin
data class AddFamilyMemberState(
    val name: String = "",
    val nameError: Int? = null, // string resource id, e.g. R.string.error_name_required
    val diseases: List<Disease> = emptyList(),
    val selectedDiseaseIds: List<Int> = emptyList(),
    val isDiseasesLoading: Boolean = false,
    val diseasesErrorMessage: String? = null,
    val allergies: List<Allergy> = emptyList(),
    val selectedAllergyIds: List<Int> = emptyList(),
    val isAllergiesLoading: Boolean = false,
    val allergiesErrorMessage: String? = null,
    val isSaving: Boolean = false,
)
```

**New file** `.../add_member/state/AddFamilyMemberEvent.kt`
```kotlin
sealed interface AddFamilyMemberEvent {
    data class NameChanged(val name: String) : AddFamilyMemberEvent
    data class ToggleDisease(val id: Int) : AddFamilyMemberEvent
    data class ToggleAllergy(val id: Int) : AddFamilyMemberEvent
    data object RetryLoadDiseases : AddFamilyMemberEvent
    data object RetryLoadAllergies : AddFamilyMemberEvent
    data object SaveClicked : AddFamilyMemberEvent
    data object DismissRequested : AddFamilyMemberEvent
}
```

**New file** `.../add_member/state/AddFamilyMemberEffect.kt`
```kotlin
sealed interface AddFamilyMemberEffect {
    data object Dismiss : AddFamilyMemberEffect
    data class ShowError(val message: String) : AddFamilyMemberEffect
}
```

**New file** `.../add_member/viewmodel/AddFamilyMemberViewModel.kt`
```kotlin
@HiltViewModel
class AddFamilyMemberViewModel @Inject constructor(
    private val getDiseasesUseCase: GetDiseasesUseCase,
    private val getAllergiesUseCase: GetAllergiesUseCase,
    private val syncDiseasesUseCase: SyncDiseasesUseCase,
    private val syncAllergiesUseCase: SyncAllergiesUseCase,
    private val addFamilyMemberUseCase: AddFamilyMemberUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AddFamilyMemberState())
    val state: StateFlow<AddFamilyMemberState> = _state.asStateFlow()

    private val _effect = Channel<AddFamilyMemberEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadDiseases()
        loadAllergies()
    }
    // loadDiseases()/loadAllergies(): identical shape to ProfileSetupPagerViewModel's —
    // set isLoading, call sync use case, collect getUseCase's Flow, clear isLoading, set
    // errorMessage on failure. (Copy pattern, don't invent a new one.)

    fun onEvent(event: AddFamilyMemberEvent) {
        when (event) {
            is AddFamilyMemberEvent.NameChanged -> _state.update { it.copy(name = event.name, nameError = null) }
            is AddFamilyMemberEvent.ToggleDisease -> _state.update {
                it.copy(selectedDiseaseIds = it.selectedDiseaseIds.toggle(event.id))
            }
            is AddFamilyMemberEvent.ToggleAllergy -> _state.update {
                it.copy(selectedAllergyIds = it.selectedAllergyIds.toggle(event.id))
            }
            AddFamilyMemberEvent.RetryLoadDiseases -> loadDiseases()
            AddFamilyMemberEvent.RetryLoadAllergies -> loadAllergies()
            AddFamilyMemberEvent.SaveClicked -> save()
            AddFamilyMemberEvent.DismissRequested -> emitEffect(AddFamilyMemberEffect.Dismiss)
        }
    }

    private fun save() {
        val name = _state.value.name.trim()
        if (name.isBlank()) {
            _state.update { it.copy(nameError = R.string.error_name_required) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val result = addFamilyMemberUseCase(
                name = name,
                allergyIds = _state.value.selectedAllergyIds,
                diseaseIds = _state.value.selectedDiseaseIds,
            )
            _state.update { it.copy(isSaving = false) }
            result.onSuccess { emitEffect(AddFamilyMemberEffect.Dismiss) }
                .onFailure { emitEffect(AddFamilyMemberEffect.ShowError(it.message ?: "")) }
        }
    }
    private fun List<Int>.toggle(id: Int) = if (contains(id)) this - id else this + id
    private fun emitEffect(e: AddFamilyMemberEffect) = viewModelScope.launch { _effect.send(e) }
}
```

**`UserProfileState.kt`:** add
```kotlin
val isAddMemberSheetVisible: Boolean = false,
```

**`UserProfileEvent.kt`:** `AddMemberClicked` stays (now just toggles sheet visibility instead of
calling `addMockMember()`); add:
```kotlin
data object AddMemberSheetDismissed : UserProfileEvent
```

**`UserProfileViewModel.kt` changes:**
- Inject `GetFamilyMembersUseCase` (or `IFamilyMemberRepository` directly, matching how it already
  injects `IUserRepository` directly rather than a use case — **follow existing local convention**,
  it currently injects the repository interface, not use cases, for this screen).
- Replace the `init` block's `familyMembers = persistentListOf()` default and the whole
  `addMockMember()` / mock id counter with:
  ```kotlin
  viewModelScope.launch {
      familyMemberRepository.getFamilyMembers().collectLatest { members ->
          _state.update {
              it.copy(familyMembers = members.map { m ->
                  FamilyMemberUiModel(id = m.id, name = m.name, avatarUrl = null)
              }.toPersistentList())
          }
      }
  }
  ```
- `AddMemberClicked` → `_state.update { it.copy(isAddMemberSheetVisible = true) }` (no more effect
  needed for this specific action, since the sheet is presented in-place inside
  `UserProfileScreen`, not via navigation).
- `AddMemberSheetDismissed` → `_state.update { it.copy(isAddMemberSheetVisible = false) }`.
- `confirmMemberRemoval()` → now calls
  `familyMemberRepository.removeFamilyMember(pendingId)` inside `viewModelScope.launch`, then
  clears `memberPendingDeletion`; on failure, emit a `UserProfileEffect.ShowError` (new effect,
  mirrors existing `AppErrorDialog`/`AppSnackbar` usage elsewhere in the app — check
  `AppSnackbar.kt`/`AppErrorDialog.kt` for the established error-surfacing convention and match it
  exactly rather than inventing a new one).

**`UserProfileScreen.kt` changes:**
```kotlin
if (state.isAddMemberSheetVisible) {
    val addMemberViewModel: AddFamilyMemberViewModel = hiltViewModel()
    val addMemberState by addMemberViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        addMemberViewModel.effect.collectLatest { effect ->
            when (effect) {
                AddFamilyMemberEffect.Dismiss -> onEvent(UserProfileEvent.AddMemberSheetDismissed)
                is AddFamilyMemberEffect.ShowError -> { /* surface via existing snackbar/dialog convention */ }
            }
        }
    }
    AddFamilyMemberBottomSheet(
        state = addMemberState,
        onEvent = addMemberViewModel::onEvent,
        onDismiss = { onEvent(UserProfileEvent.AddMemberSheetDismissed) },
    )
}
```
Using a scoped `hiltViewModel()` for the sheet (rather than folding everything into
`UserProfileViewModel`) keeps `UserProfileViewModel` from also owning allergy/disease-loading
concerns it doesn't otherwise need, and the sheet's ViewModel is naturally cleared when the sheet
closes and is removed from composition.

---

### 3.5 Localization & Theming — concrete additions required

**`presentation/src/main/res/values/strings.xml`** — add (placeholder English copy, refine wording
with design/product before merging):
```xml
<string name="add_family_member_title">Add Family Member</string>
<string name="add_family_member_name_label">Full Name</string>
<string name="add_family_member_name_placeholder">e.g. Ahmed Mostafa</string>
<string name="error_name_required">Please enter a name</string>
```
**`presentation/src/main/res/values-ar/strings.xml`** — matching Arabic translations, same keys,
inserted next to the existing `user_profile_*` block (line ~126) to keep related strings together
per file convention already established.

**`presentation/common/theme/AppColors.kt`** — the existing `ExerciseInstructionsBottomSheet` uses a
hardcoded `Color(0x660F474A)` scrim; per SKILL.md §3 ("No Hardcoded Color Hexes... under no
circumstances") this plan must **not** repeat that anti-pattern for the new sheet. Add a
semantic token, e.g. `ScrimOverlay`, to `AppColors.kt`'s light/dark color sets and reference
`AppTheme.colors.ScrimOverlay` in `AddFamilyMemberBottomSheet`. (Optional stretch goal, flag to
reviewer: consider fixing `ExerciseInstructionsBottomSheet`'s existing hardcoded scrim to use the
same new token while touching this area — separate small cleanup commit, not required for this
feature to ship.)

No new colors are otherwise needed — `Teal1000`, `Teal1600`, `Surface`, `Primary`, `Error`,
`ProfileSetupSectionTitle`, chip colors, etc. are all already defined and reused as-is.

**Person placeholder:** use the existing `R.drawable.ic_person_solid` vector (already used in
`FamilyMemberCard.kt` for members without an avatar) inside a circular colored background — do not
introduce a new drawable asset unless design provides one.

---

### 3.6 Dependency Injection summary (files touched)
- `RepositoryModule.kt` — bind `FamilyMemberRepositoryImpl` → `IFamilyMemberRepository`.
- `DatabaseModule.kt` — provide `FamilyMemberDao`.
- No `DataSourceModule.kt` or `NetworkModule.kt` changes (no new API service/remote data source).

### 3.7 Database migration checklist
- `NutriScanDatabase.kt`: add entity, add DAO accessor, bump `version` 4 → 5.
- Confirm `fallbackToDestructiveMigration()` in `DatabaseModule` is still acceptable for this bump
  (it is, per existing project convention — no prior entity addition wrote a real `Migration`
  either), but call this out explicitly in the PR description since it clears all local tables
  (food logs, cached allergies/diseases, user) on upgrade, not just the new table.

---

## 4. Verification Plan

### 4.1 Automated — Unit Tests (JUnit 5 + Turbine + Kotlin Coroutines Test, per SKILL.md §4)

Create `AddFamilyMemberViewModelTest.kt` using the **exact template from SKILL.md §4** (
`StandardTestDispatcher(testScheduler)`, `Dispatchers.setMain`/`resetMain` in `@BeforeEach`/
`@AfterEach`, flat `flow.test { }` blocks per SKILL.md §4.3):
- `when NameChanged event, state name is updated` — asserts `state.value.name`.
- `when SaveClicked with blank name, nameError is set and use case is not invoked`.
- `when SaveClicked succeeds, Dismiss effect is emitted` — Turbine on `effect`.
- `when SaveClicked fails, ShowError effect is emitted with the failure message`.
- `when ToggleDisease called twice with same id, selectedDiseaseIds returns to original` (toggle
  on/off).
- `when diseases sync fails, diseasesErrorMessage is set and isDiseasesLoading is false`.

Update `UserProfileViewModelTest.kt` (existing file):
- Remove/replace any assertions tied to `addMockMember()`'s hardcoded "Ashraf Shrief" behavior.
- `when AddMemberClicked, isAddMemberSheetVisible becomes true`.
- `when AddMemberSheetDismissed, isAddMemberSheetVisible becomes false`.
- `when repository emits family members, state.familyMembers reflects them` (fake/mock
  `IFamilyMemberRepository` returning a `Flow` via Turbine).
- `when ConfirmRemoveMemberClicked succeeds, member is removed from familyMemberRepository` (verify
  interaction on a mock).

New `FamilyMemberRepositoryImplTest.kt` (data module `test` source set, matching existing
`AllergyRepositoryImpl`-style tests if present, otherwise this establishes the pattern for `data`):
- `addFamilyMember` — success path persists optimistic entity then reconciles with server response.
- `addFamilyMember` — failure path rolls back Room to the pre-add list.
- `removeFamilyMember` — success and rollback-on-failure paths, mirroring
  `UserRepositoryImpl.updateProfile()`'s existing rollback tests if any exist as a reference.

### 4.2 Manual QA checklist
1. **Happy path:** Profile → Add Member → fill name, pick 2 allergies + 1 disease → Save → sheet
   dismisses → new card appears immediately in the Family Members row.
2. **Kill and reopen the app** (process death) → member persists (Room-backed, not in-memory).
3. **Airplane mode:** Add a member offline → card appears optimistically → re-enable network →
   confirm it syncs (or, if still failing after backend contract is confirmed, that it rolls back
   with a visible error — behavior depends on §2 resolution).
4. **Validation:** attempt Save with an empty name → inline error, no request sent.
5. **Remove flow:** long-press an existing member → confirm dialog → Confirm → member disappears
   from Room-backed list and does not reappear after a `fetchAndSyncProfile()` (i.e., backend really
   dropped it, not just local-only removal).
6. **Localization:** switch device/app language to Arabic → sheet title, labels, placeholder, error,
   and chip labels (from `Disease`/`Allergy` — confirm those come pre-localized from backend or are
   themselves a separate known limitation) all render in Arabic, RTL layout looks correct (sheet
   content, chips, button).
7. **Theme:** toggle dark mode → sheet background, text, chips, borders, scrim, person-placeholder
   icon all use correct dark-mode tokens, no washed-out or invisible text.
8. **Accessibility/rotation:** rotate device with sheet open → state (name, selections) is retained
   (survives via `rememberSaveable`/ViewModel, not lost on configuration change).
9. **Chip loading/error states:** simulate allergies/diseases API failure inside the sheet → verify
   the same retry-button UX already used in `HealthProfileContent` appears and works, unchanged.

---

## 5. File Change Summary (checklist for the implementing developer)

**New files**
- `domain/family/model/FamilyMember.kt`
- `domain/family/model/FamilyMemberInput.kt`
- `domain/family/repository/IFamilyMemberRepository.kt`
- `domain/family/usecase/GetFamilyMembersUseCase.kt`
- `domain/family/usecase/AddFamilyMemberUseCase.kt`
- `domain/family/usecase/RemoveFamilyMemberUseCase.kt`
- `data/db/entity/FamilyMemberEntity.kt`
- `data/db/dao/FamilyMemberDao.kt`
- `data/remote/dto/FamilyMemberDto.kt`
- `data/repository/FamilyMemberRepositoryImpl.kt`
- `presentation/common/components/ChipSelectionFlowRow.kt`
- `presentation/settings/profile/view/components/AddFamilyMemberBottomSheet.kt`
- `presentation/settings/profile/add_member/state/AddFamilyMemberState.kt`
- `presentation/settings/profile/add_member/state/AddFamilyMemberEvent.kt`
- `presentation/settings/profile/add_member/state/AddFamilyMemberEffect.kt`
- `presentation/settings/profile/add_member/viewmodel/AddFamilyMemberViewModel.kt`
- `presentation/.../add_member/viewmodel/AddFamilyMemberViewModelTest.kt` (test)
- `data/.../repository/FamilyMemberRepositoryImplTest.kt` (test)

**Modified files**
- `data/db/NutriScanDatabase.kt` (entity, DAO accessor, version bump)
- `app/di/DatabaseModule.kt` (provide `FamilyMemberDao`)
- `app/di/RepositoryModule.kt` (bind `IFamilyMemberRepository`)
- `data/remote/dto/UserDto.kt` (add `familyMembers`)
- `data/remote/dto/UpdateUserProfileRequestDto.kt` (add `familyMembers`)
- `data/repository/UserRepositoryImpl.kt` (persist family members on `fetchAndSyncProfile()`)
- `presentation/settings/profile/state/UserProfileState.kt` (`isAddMemberSheetVisible`)
- `presentation/settings/profile/state/UserProfileEvent.kt` (`AddMemberSheetDismissed`)
- `presentation/settings/profile/state/UserProfileEffect.kt` (error effect, if not already covered
  by an existing shared mechanism)
- `presentation/settings/profile/viewmodel/UserProfileViewModel.kt` (real repository wiring, remove
  mock logic)
- `presentation/settings/profile/viewmodel/UserProfileViewModelTest.kt` (update tests)
- `presentation/settings/profile/view/UserProfileScreen.kt` (host the sheet)
- `presentation/profile_setup/view/components/HealthProfileContent.kt` (delegate to
  `ChipSelectionFlowRow`, remove duplicated `DiseasesSection`/`AllergiesSection` bodies)
- `presentation/common/theme/AppColors.kt` (add `ScrimOverlay` token)
- `presentation/src/main/res/values/strings.xml` / `values-ar/strings.xml` (new keys)

**Do not modify:** `UserApiService.kt`, `IUserRemoteDataSource.kt`,
`UserRemoteDataSourceImpl.kt`, `NetworkModule.kt`, `DataSourceModule.kt` — no new endpoints are
introduced under the current assumption (§2); only DTO payload shapes grow.

---

## 6. Risks & Follow-ups
- **Primary risk:** §2's backend contract assumption. If backend confirms `familyMembers` is a
  delta/merge PATCH rather than full-replace, `syncListToBackend()` simplifies (send only the
  new/removed member) — isolated to `FamilyMemberRepositoryImpl`, no other layer changes.
- If a dedicated family-member endpoint exists but wasn't exported to this Postman collection, this
  plan's data layer should be redirected to use it instead of full-list PATCH before implementation
  begins — safer for concurrency and avoids clobbering members added elsewhere.
- Family member avatars are placeholder-only until/unless an upload endpoint is confirmed.
- `HealthProfileContent.kt` refactor is a small blast-radius change; write/keep its existing tests
  green before and after extracting `ChipSelectionFlowRow`.
