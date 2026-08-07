# Plan: Family Member Image PATCH Integration

## 1. Feature Summary
Integrate backend endpoint `PATCH /api/v1/users/family-member/{familyMemberId}/image` (multipart/form-data, `image` binary) into Add Family Member flow so user can add a family member with a photo from the same bottom sheet.

Because image endpoint requires `familyMemberId`, the add flow will be a safe two-step sequence:
1. Create family member first (existing add API flow).
2. If user selected photo, upload it immediately after successful create using returned member id.

Scope includes image selection UI in add sheet, create-then-upload orchestration, and mapping of server `imageUrl` to local/member UI.

Primary reference:
- Swagger endpoint: `PATCH /api/v1/users/family-member/{familyMemberId}/image`
- Response shape includes: `id`, `name`, `relation`, `imageUrl`, `allergies[]`, `diseases[]`

## 2. Files to Create
- `docs/plans/2026-08-07-family-member-image-patch-integration.md`: implementation plan artifact required before coding.
- `domain/src/main/kotlin/iti/grad/nutriscan/domain/family/usecase/UploadFamilyMemberImageUseCase.kt`: use case to upload a selected image for an existing family member.

## 3. Files to Modify
- `domain/src/main/kotlin/iti/grad/nutriscan/domain/family/model/FamilyMember.kt`: add `imageUrl: String?` to domain model.
- `domain/src/main/kotlin/iti/grad/nutriscan/domain/family/repository/IFamilyMemberRepository.kt`: add `uploadFamilyMemberImage(memberId: String, imageFile: File): Result<Unit>`.
- `data/src/main/kotlin/iti/grad/nutriscan/data/remote/api/UserApiService.kt`: add multipart PATCH API declaration for family member image.
- `data/src/main/kotlin/iti/grad/nutriscan/data/remote/datasource/IUserRemoteDataSource.kt`: add remote method signature for family member image upload.
- `data/src/main/kotlin/iti/grad/nutriscan/data/remote/datasource/UserRemoteDataSourceImpl.kt`: implement delegation to `UserApiService`.
- `data/src/main/kotlin/iti/grad/nutriscan/data/remote/dto/FamilyMemberDto.kt`: map response `imageUrl` and keep id/allergy/disease mapping compatibility.
- `data/src/main/kotlin/iti/grad/nutriscan/data/repository/FamilyMemberRepositoryImpl.kt`: implement multipart upload + optimistic reconciliation into Room-backed family member list.
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/profile/state/FamilyMemberUiModel.kt`: add `imageUrl` mapping source (backend URL).
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/profile/viewmodel/UserProfileViewModel.kt`: map member `imageUrl` from domain to UI model.
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/profile/add_member/state/AddFamilyMemberState.kt`: add upload-state fields needed for member image integration.
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/profile/add_member/state/AddFamilyMemberEvent.kt`: add image-related events (pick/retry/clear).
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/profile/add_member/state/AddFamilyMemberEffect.kt`: add one-shot effect for image picker launch if needed.
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/profile/add_member/viewmodel/AddFamilyMemberViewModel.kt`: orchestrate create-then-upload flow when adding new member with selected image, and upload for edit mode.
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/profile/view/components/AddFamilyMemberBottomSheet.kt`: connect image action UI callbacks to new events and loading/error states.
- `presentation/src/test/java/iti/grad/nutriscan/presentation/settings/profile/add_member/viewmodel/AddFamilyMemberViewModelTest.kt`: add tests for image upload event/state/effect flows.
- `data/src/test/kotlin/iti/grad/nutriscan/data/repository/FamilyMemberRepositoryImplTest.kt`: add repository tests for multipart upload success/failure and rollback behavior.

## 4. Layer Breakdown
### Domain
- Models to add/modify:
  - Extend `FamilyMember` with `imageUrl: String? = null`.
- UseCases to create:
  - `UploadFamilyMemberImageUseCase` with one public `operator fun invoke(memberId: String, imageFile: File): Result<Unit>`.
- Repository interface changes:
  - `IFamilyMemberRepository.uploadFamilyMemberImage(memberId, imageFile)`.

### Data
- DTOs to add/modify:
  - `FamilyMemberDto` add `imageUrl: String?` and preserve existing fallback extraction from ids/lists.
- Mapper functions:
  - `FamilyMemberDto.toEntity()` to persist `imageUrl` into `FamilyMemberEntity` (requires entity field alignment if missing).
  - Domain mapper in `FamilyMemberRepositoryImpl` to expose `imageUrl` to domain model.
- DAO changes:
  - If `FamilyMemberEntity` currently lacks `imageUrl`, add column and ensure persistence in user profile entity list conversion.
- Remote DataSource changes:
  - Add function that sends `MultipartBody.Part` to the new PATCH endpoint and returns updated `FamilyMemberDto` (or compatible payload mapping).
- Repository logic:
  - Compress image with existing `ImageCompressor` before upload.
  - Build multipart with field name `image` exactly.
  - On success: merge returned member into existing cached family-members list and persist.
  - On failure: keep previous cached state intact and return `Result.failure`.
  - Add flow contract for creation with image:
    - Keep existing `addFamilyMember` behavior.
    - Expose/derive created member id after successful create so presentation layer can call image upload immediately.
    - If repository cannot directly return created id now, add a repository method that can resolve created member deterministically (for example by latest inserted server id after sync) and document tie-break strategy.

### Presentation
- State properties (Add member flow):
  - `selectedImageUri: Uri?` (or internal image-pending marker).
  - `isImageUploading: Boolean`.
  - `imageUploadErrorMessage: String?` or localized message key handling.
  - `isCreatingMember: Boolean` if separate from generic `isSaving` improves UI clarity.
- Events:
  - `ImagePickRequested`.
  - `ImageSelected(uri)`.
  - `RetryImageUpload`.
  - `ClearSelectedImage`.
- Effects:
  - `OpenImagePicker` (if image picker launch is effect-driven).
  - Reuse existing `ShowError` for upload failure fallback.
- ViewModel logic outline:
  - For editing existing member (`editingMemberId != null`): upload selected image via `UploadFamilyMemberImageUseCase`.
  - For new member creation flow (`editingMemberId == null`):
    1. Validate fields and run add member use case.
    2. If add succeeds and no selected image: finish flow normally.
    3. If add succeeds and image exists: upload image using created member id.
    4. If image upload fails after successful create: keep member created, show non-blocking error and allow retry upload from UI.
    5. If user retries image upload, retry against same created member id, no duplicate member creation.
  - Reflect uploading/error state without blocking unrelated field edits.

## 5. Navigation Changes
- No new route is required.
- Integration remains inside existing bottom sheet flow under Profile screen.
- If picker is launched through activity result API, keep navigation graph unchanged.

## 6. Strings - MANDATORY (Zero Hardcoded Text)
New user-facing strings introduced by this feature:

| Key (R.string.xxx) | English Value | Arabic Value |
|---|---|---|
| `family_member_image_upload_failed` | "Couldn't upload the family member photo. Please try again." | "تعذر رفع صورة فرد العائلة. حاول مرة أخرى." |
| `family_member_image_upload_in_progress` | "Uploading photo..." | "جارٍ رفع الصورة..." |
| `family_member_image_change_action` | "Change photo" | "تغيير الصورة" |
| `family_member_image_add_action` | "Add photo" | "إضافة صورة" |
| `family_member_image_remove_action` | "Remove photo" | "إزالة الصورة" |
| `family_member_created_image_pending` | "Member saved. Photo upload is still pending." | "تم حفظ الفرد. رفع الصورة ما زال قيد الانتظار." |

## 7. Testing Plan
- `AddFamilyMemberViewModelTest`:
  - Initial state for image upload fields is idle/empty.
  - New member add without image: create succeeds and sheet closes.
  - New member add with image: create succeeds, then upload succeeds, then sheet closes.
  - New member add with image: create succeeds, upload fails, member remains added and user gets retry path.
  - Retry after upload failure does not recreate member and only retries image upload.
  - `ImageSelected` while editing member emits uploading state then success state.
  - `ImageSelected` failure emits error state and `ShowError` effect when applicable.
  - `RetryImageUpload` re-attempts upload with last selected URI.
  - Ensure `SaveClicked` flow still works with image-upload states present.
- `FamilyMemberRepositoryImplTest`:
  - Multipart request built with part name `image` and PATCH path uses provided `memberId`.
  - Success path updates cached family member `imageUrl`.
  - Failure path does not corrupt existing cached family-member list.

## 8. Edge Cases
- New member creation succeeds but created id cannot be resolved: keep member created, show explicit image-upload failure and retry affordance.
- `familyMemberId` is null/blank in edit flow: block upload and surface safe error.
- Selected URI cannot be opened (permission revoked or invalid): show upload failure state.
- Backend returns success with empty `imageUrl`: keep previous image URL and log warning.
- Network timeout/offline during upload: preserve existing image and allow retry.
- User closes sheet during upload: cancel coroutine safely, avoid leaking temp files.
- User taps save repeatedly: prevent duplicate create request while create/upload is in progress.

## 9. Definition of Done
- [ ] Endpoint contract integrated end-to-end (domain -> data -> presentation trigger path).
- [ ] `FamilyMember`/UI model expose backend `imageUrl` correctly.
- [ ] Multipart PATCH uses exact field name `image`.
- [ ] Add Family Member sheet supports selecting photo during creation flow.
- [ ] Create with photo follows create-then-upload sequence with retryable upload errors.
- [ ] ViewModel tests updated for image upload events, states, and effects.
- [ ] Repository tests cover success/failure cache behavior.
- [ ] All new strings added to `values/strings.xml` and `values-ar/strings.xml`.
- [ ] No hardcoded user-facing strings or inline colors in the added UI changes.
- [ ] `README.md` updated to reflect feature status once implementation is complete.