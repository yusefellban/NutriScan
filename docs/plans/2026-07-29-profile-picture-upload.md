# Profile Picture Upload — Implementation Plan

## Goal Description

The backend now exposes a real image-upload endpoint for the user's avatar:

```
POST /api/v1/users/profile/image      (multipart/form-data, field: "image") → full UserResponse
GET  /api/v1/users/profile            → full UserResponse (already used)
PATCH /api/v1/users/profile           → full UserResponse (already used)
```

**Current state of the app (bug):** `EditProfileViewModel.saveProfileData()` never
uploads image bytes to the backend at all. When the user picks a photo, the
`content://` URI is copied to `filesDir/profile_avatar.jpg`, turned into a
`file://` URI, and that **local file path string** is sent as `avatarUrl` inside
the `PATCH /v1/users/profile` JSON body. This "works" only by accident, only on
the device that picked the photo, only until the app's storage is cleared, and
it is never visible to any other device, to family members, or to the backend's
own `imageUrl` field. It also means `dto.avatarUrl` coming back from `GET
/v1/users/profile` is always `null` today, so `UserRepositoryImpl` silently
falls back to the stale local value (`dto.avatarUrl ?: localUser?.avatarUrl`).

**Goal of this feature:** Replace that hack with a real upload flow using the
new endpoint, store the **server-returned `imageUrl`** as the single source of
truth for the avatar, and add professional, centralized image caching so the
avatar loads instantly and refreshes correctly after every change — all while
following the existing Clean Architecture (`domain` / `data` / `presentation`),
MVI, DI, localization, and theming conventions already used throughout
NutriScan.

Two additional field-name items to confirm with backend before/while
implementing (see "Open Questions" at the end) since the Swagger sample body
does not label the URL field consistently: `UserDto.avatarUrl` in our code vs.
`imageUrl` in the pasted `/api/v1/users/me` and image-upload sample response.

## User Review Required

Please confirm before implementation begins:

1. **Response field name.** The pasted Swagger examples show the user
   object's picture field as `"imageUrl"` in some payload samples, while our
   existing `UserDto`/`User`/`UserEntity` models use `avatarUrl`. I will treat
   these as the same concept and keep our internal domain name `avatarUrl`
   (no other screens need to change), mapping from whatever key the backend
   actually serializes (`imageUrl` per the shared spec) via `@SerialName`. **Please
   confirm the backend's real field name is `imageUrl`** (the spec you pasted
   is authoritative over my assumption) so I map it correctly in one place.
2. **Upload endpoint response.** Per the Swagger sample, `POST
   /api/v1/users/profile/image` returns the **entire updated user profile**
   (not just a URL string). The plan below treats it exactly like `GET
   /v1/users/profile` / `PATCH /v1/users/profile` — same DTO, reuse the same
   parsing/mapping code. Confirm this is correct.
3. **Removal/reset.** There is no `DELETE` avatar endpoint in what was
   shared. This plan does **not** add a "remove profile picture" action. If
   product wants that, we'll need the backend to add an endpoint (or confirm
   that `PATCH .../profile` with `imageUrl: null` clears it).
4. **Upload timing/UX.** Proposed UX: as soon as the user picks a photo (in
   edit mode), we upload it immediately in the background and show a
   loading spinner over the avatar; we do **not** wait for "Save" to be
   pressed, since the endpoint is independent of the rest of the profile
   PATCH. Please confirm this matches the desired UX vs. "only upload when
   Save is tapped."
5. **Max file size / dimensions.** No constraints were provided by backend.
   Plan defaults to downscaling to a max of 1024×1024px and JPEG quality 85
   client-side before upload (same compression pattern already used for
   scan images), purely for upload speed/bandwidth. Confirm acceptable, or
   provide backend limits so we can match them exactly.

---

## Proposed Changes

### 0. Architectural decision

We will **not** reuse `updateProfile()`/`PATCH .../profile` for the picture.
The picture gets its own dedicated flow end-to-end, mirroring how `Scan`
already has its own `submitScanImage(file)` multipart pipeline:

```
domain:  IUserRepository.uploadAvatar(imageFile: File): Result<Unit>
         UploadAvatarUseCase
data:    UserApiService.uploadProfileImage(part): UserDto
         IUserRemoteDataSource.uploadProfileImage(part): UserDto
         UserRepositoryImpl.uploadAvatar(file) — compress, upload, persist
             the returned DTO into Room (single source of truth), same
             mapping code already used by fetchAndSyncProfile()
presentation: EditProfileViewModel drives a small dedicated "avatar upload"
         sub-state (idle / uploading / error) independent from the rest of
         the edit-profile form fields, so a slow network on the picture
         upload never blocks Save on the other text fields, and Save can no
         longer accidentally send a `content://`/`file://` string as
         `avatarUrl` in the JSON PATCH body (that field is removed from the
         PATCH request entirely — see §3).
```

This keeps each concern single-purpose (Single Responsibility), matches the
existing Scan module's proven multipart pattern, and removes the fragile
`content://` → copy-to-`filesDir` → `file://` workaround completely.

---

### 1. `domain` module

**`domain/src/main/kotlin/.../user/repository/IUserRepository.kt`** — add:

```kotlin
/**
 * Uploads a new avatar image to the backend. On success, the backend's
 * returned profile (including the new permanent image URL) is persisted
 * to the local database, exactly like [fetchAndSyncProfile].
 */
suspend fun uploadAvatar(imageFile: File): Result<Unit>
```

**`domain/src/main/kotlin/.../user/usecase/UploadAvatarUseCase.kt`** — new file:

```kotlin
package iti.grad.nutriscan.domain.user.usecase

import iti.grad.nutriscan.domain.user.repository.IUserRepository
import java.io.File
import javax.inject.Inject

class UploadAvatarUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(imageFile: File): Result<Unit> =
        userRepository.uploadAvatar(imageFile)
}
```

**`ProfileUpdate.kt` / `UpdateUserProfileUseCase.kt`** — remove the
`avatarUrl` parameter from both. Text-field profile edits (`PATCH
.../profile`) should no longer carry an avatar value at all now that the
picture has its own dedicated endpoint; this also removes the last code path
that could send a bogus local URI to the backend.

---

### 2. `data` module

**`data/src/main/kotlin/.../remote/dto/UserDto.kt`** — rename the
serialized key to match the confirmed backend field (see Open
Question #1). Example if backend uses `imageUrl`:

```kotlin
@Serializable
data class UserDto(
    ...
    @SerialName("imageUrl") val avatarUrl: String? = null,
    val updatedAt: String? = null, // NEW — see §5 cache-busting
)
```

Adding `updatedAt` (already present in every sample response you pasted) is
required for cache-busting — see §5.

**`data/src/main/kotlin/.../remote/api/UserApiService.kt`** — add:

```kotlin
@Multipart
@POST("v1/users/profile/image")
suspend fun uploadProfileImage(
    @Part image: MultipartBody.Part
): UserDto
```

**`data/src/main/kotlin/.../remote/datasource/IUserRemoteDataSource.kt`** /
**`UserRemoteDataSourceImpl.kt`** — add matching
`suspend fun uploadProfileImage(image: MultipartBody.Part): UserDto`,
delegating straight to `userApiService.uploadProfileImage(image)`, same
one-line-delegation style already used for `getProfile()`/`updateProfile()`.

**`data/src/main/kotlin/.../repository/UserRepositoryImpl.kt`** — add:

```kotlin
override suspend fun uploadAvatar(imageFile: File): Result<Unit> {
    return withContext(ioDispatcher) {
        try {
            val compressedFile = compressAvatarImage(imageFile)
            val requestFile = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData(
                "image", compressedFile.name, requestFile
            )
            val dto = remoteDataSource.uploadProfileImage(part)
            if (compressedFile.absolutePath != imageFile.absolutePath) {
                compressedFile.delete()
            }
            persistProfileDto(dto)   // extracted, shared with fetchAndSyncProfile()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Avatar upload failed")
            Result.failure(e)
        }
    }
}
```

Refactor: extract the DTO→Entity merge logic that currently lives inline
inside `fetchAndSyncProfile()` into a private `persistProfileDto(dto:
UserDto)` function, and call it from **both** `fetchAndSyncProfile()` and
the new `uploadAvatar()`. This avoids duplicating the null-coalescing /
local-fallback logic in two places (DRY) and guarantees the freshly
uploaded `imageUrl` is written through the exact same path as every other
profile sync.

Add a private `compressAvatarImage(file: File): File` helper. This can
literally reuse the compression approach from `ScanRepositoryImpl`
(`BitmapFactory.decode` → `Bitmap.scale` to max 1024×1024, keeping aspect
ratio → `compress(JPEG, 85)`); consider extracting a small shared
`ImageCompressor` utility class in `data/src/main/kotlin/.../data/util/` so
both `ScanRepositoryImpl` and `UserRepositoryImpl` call the same code
instead of two near-duplicate copies. (Nice-to-have refactor, flagged as
optional cleanup in the Verification Plan, not required to ship this
feature.)

**Local-file fallback removal:** delete the `content://`/`file://` copy
logic entirely from the client — it lived in
`EditProfileViewModel.saveProfileData()` (presentation layer) and is
removed there in §3, not here.

---

### 3. `presentation` module

**`EditProfileState.kt`** — add a dedicated avatar-upload sub-state so it's
visually and logically decoupled from the text-field "isSaving" flag:

```kotlin
val avatarUploadState: AvatarUploadState = AvatarUploadState.Idle
```

```kotlin
sealed interface AvatarUploadState {
    data object Idle : AvatarUploadState
    data object Uploading : AvatarUploadState
    data object Error : AvatarUploadState
}
```

Keep `avatarUrl: String?` as-is — it now only ever holds a server URL
(never a local URI) once this change ships.

**`EditProfileEvent.kt`** — replace `SelectAvatar(avatarUrl: String)` with
`SelectAvatar(uri: Uri)` (pass the picked content URI, not a pre-stringified
value, so the ViewModel — not the Composable — owns all file I/O per Clean
Architecture) and add `RetryAvatarUpload`.

**`EditProfileViewModel.kt`** — changes:
- Inject `UploadAvatarUseCase`.
- `SelectAvatar(uri)` handler: launches a coroutine that
  1. Sets `avatarUploadState = Uploading`.
  2. Copies the picked `content://` URI to a temp cache file
     (`context.cacheDir`, **not** `filesDir` — this is a transient upload
     source now, not permanent storage, since the server URL is the
     permanent source of truth after upload).
  3. Calls `uploadAvatarUseCase(tempFile)`.
  4. On success: deletes the temp file, sets `avatarUploadState = Idle`
     (the new `avatarUrl` arrives automatically via the existing
     `getUserProfileUseCase()` Flow collector in `init{}`, since
     `uploadAvatar()` writes through Room — no manual state patch needed,
     consistent with the existing single-source-of-truth pattern).
  5. On failure: deletes the temp file, sets `avatarUploadState = Error`,
     surfaces a snackbar/alert reusing the existing
     `ProfileAlertState.Error`/`InternetError` pattern already used for
     save failures.
- `RetryAvatarUpload`: re-runs the last picked URI (retain it in state
  while `Uploading`/`Error`).
- `saveProfileData()`: **remove** all the `content://`/`file://`
  copy-to-`filesDir` code and the `avatarUrl` parameter from the
  `updateUserProfileUseCase(...)` call — the picture is no longer part of
  the text-field PATCH at all (see §1).

**`EditProfileScreen.kt`** — changes:
- `photoPickerLauncher` result handler now calls
  `viewModel.onEvent(EditProfileEvent.SelectAvatar(uri))` directly (no
  `.toString()` — pass the `Uri`).
- Avatar `Box`: overlay a `CircularProgressIndicator` (reusing
  `AppTheme.colors.Teal1000`, same as other loading spinners in this file)
  centered over the avatar circle when `state.avatarUploadState is
  Uploading`; disable the pencil-badge click while uploading.
- On `Error`, reuse the existing `ErrorAlert` composable already wired up
  in this screen for consistency, with a new localized string (see §4)
  and a retry action wired to `RetryAvatarUpload`.
- `AsyncImage` `ImageRequest` gets a stable Coil cache key including
  `updatedAt` — see §5 (this is the only other Compose-level change).

**`UserProfileViewModel.kt` / `ProfileHeaderSection.kt` (read-only Profile
screen avatar)** — no logic changes needed; they already read `avatarUrl`
from the same `GetUserProfileUseCase` Flow, so they'll automatically show
the new server URL once Room is updated. Only the `ImageRequest` cache-key
change in §5 touches this file's `AsyncImage` call.

---

### 4. Localization (per SKILL.md §2 — no hardcoded strings)

Add to `presentation/src/main/res/values/strings.xml`:

```xml
<string name="edit_profile_avatar_uploading">Uploading photo…</string>
<string name="edit_profile_avatar_upload_error">Couldn\'t upload your photo. Please try again.</string>
<string name="edit_profile_avatar_upload_retry">Retry upload</string>
```

Add matching Arabic translations to
`presentation/src/main/res/values-ar/strings.xml` (to be written with a
native/reviewed AR translation — plan reserves the keys; exact AR copy to
be filled in during implementation, following the existing tone of
`profile_setup_load_error` / `action_retry` in that file).

All new UI copy is accessed via `stringResource(id = R.string.key)`,
consistent with `EditProfileScreen.kt`'s existing pattern — no literal
strings introduced anywhere.

---

### 5. Theming, dark/light mode, and caching (Coil)

**Theming:** No new colors needed — the uploading spinner reuses
`AppTheme.colors.Teal1000` and the error alert reuses the existing
`ErrorAlert` component, which is already theme-aware. No hardcoded hex
values introduced (per SKILL.md §3).

**Professional image caching (the "better user experience" ask):**

Today `NutriScanApplication.newImageLoader()` builds a Coil `ImageLoader`
with only an `OkHttpNetworkFetcherFactory` and `GifDecoder` — no explicit
memory/disk cache configuration, so Coil falls back to small defaults and,
more importantly, **there is no cache-busting strategy**, so if the backend
ever serves the new avatar at the *same* URL as the old one (common with
CDN-backed "profile.jpg"-style static URLs), Coil/OkHttp would show the
stale cached image after an upload.

Two-part fix:

**(a) Explicit, generous cache configuration** in
`NutriScanApplication.newImageLoader()`:

```kotlin
override fun newImageLoader(context: PlatformContext): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            add(OkHttpNetworkFetcherFactory())
            add(GifDecoder.Factory())
        }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, 0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizePercent(0.02)
                .build()
        }
        .crossfade(true)
        .build()
}
```

**(b) Cache-key busting tied to `updatedAt`** so a re-uploaded avatar is
never served stale from cache even if the URL is otherwise identical. Since
`UserDto`/`User`/`UserEntity` already track (or, per §2, will track)
`updatedAt`, every `AsyncImage` that shows the avatar (`EditProfileScreen`,
`ProfileHeaderSection`, and anywhere else `user.avatarUrl` is rendered)
builds its `ImageRequest` like:

```kotlin
ImageRequest.Builder(LocalContext.current)
    .data(avatarUrl)
    .memoryCacheKey("avatar_${avatarUrl}_$updatedAt")
    .diskCacheKey("avatar_${avatarUrl}_$updatedAt")
    .crossfade(true)
    .build()
```

To avoid repeating this in three+ places, add one small shared helper in
`presentation/src/main/kotlin/.../common/components/`:

```kotlin
// AvatarImageRequest.kt
@Composable
fun rememberAvatarImageRequest(avatarUrl: String?, updatedAt: String?): ImageRequest?
```

and have `EditProfileScreen` and `ProfileHeaderSection` both call it,
keeping the caching strategy centralized in one file (per the user's
"centralized files" requirement) instead of duplicated per-screen.

This gives us: fast subsequent loads (memory + disk cache), correct
invalidation the moment the avatar actually changes, and no reliance on
HTTP cache headers the backend may or may not set correctly.

---

### 6. Files touched — summary checklist

| Layer | File | Change |
|---|---|---|
| domain | `user/repository/IUserRepository.kt` | + `uploadAvatar()` |
| domain | `user/usecase/UploadAvatarUseCase.kt` | new |
| domain | `user/model/ProfileUpdate.kt` | − `avatarUrl` |
| domain | `user/usecase/UpdateUserProfileUseCase.kt` | − `avatarUrl` param |
| data | `remote/dto/UserDto.kt` | rename/confirm avatar field + add `updatedAt` |
| data | `remote/dto/UpdateUserProfileRequestDto.kt` | − `avatarUrl` |
| data | `remote/api/UserApiService.kt` | + `uploadProfileImage()` |
| data | `remote/datasource/IUserRemoteDataSource.kt` / `Impl.kt` | + `uploadProfileImage()` |
| data | `repository/UserRepositoryImpl.kt` | + `uploadAvatar()`, extract `persistProfileDto()`, extract `compressAvatarImage()` |
| data | `db/entity/UserEntity.kt` | + `updatedAt: String?` |
| data | `db/dao/UserDao.kt` | no change (already selects `*`) |
| presentation | `settings/profile/edit/state/EditProfileState.kt` | + `AvatarUploadState` |
| presentation | `settings/profile/edit/state/EditProfileEvent.kt` | `SelectAvatar(Uri)`, + `RetryAvatarUpload` |
| presentation | `settings/profile/edit/viewmodel/EditProfileViewModel.kt` | upload flow, remove local-file hack |
| presentation | `settings/profile/edit/view/EditProfileScreen.kt` | spinner/error UI, pass `Uri` |
| presentation | `settings/profile/view/components/ProfileHeaderSection.kt` | use shared cache-key helper |
| presentation | `common/components/AvatarImageRequest.kt` | new — centralized Coil request builder |
| app (DI) | `di/RepositoryModule.kt` | no change (already binds `IUserRepository`) |
| app | `NutriScanApplication.kt` | explicit memory/disk cache config |
| res | `values/strings.xml`, `values-ar/strings.xml` | 3 new keys, EN + AR |

---

## Verification Plan

### Automated

1. **`UploadAvatarUseCase` unit test** (new,
   `domain/src/test/.../UploadAvatarUseCaseTest.kt`): verifies it delegates
   to `IUserRepository.uploadAvatar(file)` and forwards
   `Result.success`/`Result.failure` unchanged (mirrors existing
   `GetUserProfileUseCase`/`UpdateUserProfileUseCase` test style).
2. **`UserRepositoryImpl` test additions**
   (`data/src/test/.../UserRepositoryImplTest.kt` if one exists, else new):
   - `uploadAvatar()` success path persists the returned DTO to
     `UserDao.insertOrUpdateUser` with the new `avatarUrl`/`updatedAt`.
   - `uploadAvatar()` failure path (network exception) returns
     `Result.failure` and does **not** touch the DAO (no optimistic update
     needed here since there's nothing to roll back — unlike text-field
     edits, the old avatar is simply still shown until upload succeeds).
   - `fetchAndSyncProfile()` and `uploadAvatar()` both route through the
     same `persistProfileDto()` — a shared test can assert identical
     entity output for the same input DTO.
3. **`EditProfileViewModel` test additions**
   (`presentation/src/test/.../EditProfileViewModelTest.kt`, following the
   template in `SKILL.md` §4 — JUnit 5 + Turbine +
   `StandardTestDispatcher`):
   - `SelectAvatar(uri)` → state transitions `Idle → Uploading → Idle` on
     success, and `avatarUrl` in state ends up equal to the new
     server-returned URL (via the `getUserProfileUseCase()` flow, using a
     fake/in-memory repository or fake use case per existing test doubles
     in this test file).
   - `SelectAvatar(uri)` → `Idle → Uploading → Error` on
     `uploadAvatarUseCase` failure; confirm `ProfileAlertState`/error
     surface fires exactly once (flat assertions, no nested `flow.test`,
     per SKILL.md §4.3).
   - `RetryAvatarUpload` re-invokes the use case with the same file.
   - `saveProfileData()` no longer includes `avatarUrl` in the
     `updateUserProfileUseCase` invocation (regression guard against the
     old bug reappearing).
4. Run the full existing `EditProfileViewModelTest.kt` /
   `UserProfileViewModelTest.kt` suites to confirm no regressions from the
   `ProfileUpdate`/event signature changes.

### Manual QA

1. **Happy path:** Edit Profile → tap pencil badge → pick a photo →
   spinner shows over avatar → photo appears, persists after killing and
   reopening the app (confirms Room + server round-trip, not just local
   cache).
2. **Cache correctness:** upload photo A, background the app, upload photo
   B from another device/Postman directly against the same account, pull
   down/reopen Profile screen — confirm B is shown, not a stale cached A
   (validates the `updatedAt` cache-busting in §5, not just "does Coil show
   something").
3. **Offline/error path:** enable airplane mode, pick a photo → error alert
   with retry appears, old avatar remains visible/unchanged, retry after
   re-enabling network succeeds.
4. **Both other screens using avatar** (`ProfileHeaderSection` on the main
   Profile screen, and anywhere else `User.avatarUrl` renders, e.g. a Home
   greeting header if applicable) reflect the new photo without needing a
   full app restart, since they observe the same Room-backed Flow.
5. **Dark/light mode:** verify uploading spinner and error alert render
   correctly in both themes (no hardcoded colors).
6. **RTL/Arabic:** switch app language to Arabic, verify new copy displays
   correctly and layout mirrors correctly (existing RTL support already
   covers layout direction; only need to confirm the 3 new strings read
   correctly).
7. **Large image handling:** pick a very large (e.g., 12MP) photo, confirm
   compression keeps upload snappy and doesn't OOM on lower-end devices.
8. **Regression:** confirm editing text fields (name, height, weight,
   diseases, allergies) and saving still works correctly now that
   `avatarUrl` has been removed from that PATCH payload entirely.

---

## Open Questions (for backend / product, blocking a couple of details only)

1. Confirm exact JSON key backend serializes for the avatar URL
   (`imageUrl` vs `avatarUrl`) — assumed `imageUrl` per the shared Swagger
   sample; only affects one `@SerialName` line, doesn't block the rest of
   the plan.
2. Confirm `POST /api/v1/users/profile/image` really returns the full
   `UserResponse` (per your pasted sample) and not just a bare URL string.
3. Any server-side max upload size / allowed MIME types we should validate
   client-side before uploading (to fail fast with a friendly error instead
   of a generic network error)?
4. Confirm no "remove avatar" requirement for this iteration (see User
   Review Required §3).
