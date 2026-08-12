package iti.grad.nutriscan.presentation.settings.profile.add_member.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.allergy.usecase.GetAllergiesUseCase
import iti.grad.nutriscan.domain.allergy.usecase.SyncAllergiesUseCase
import iti.grad.nutriscan.domain.disease.usecase.GetDiseasesUseCase
import iti.grad.nutriscan.domain.disease.usecase.SyncDiseasesUseCase
import iti.grad.nutriscan.domain.family.usecase.AddFamilyMemberUseCase
import iti.grad.nutriscan.domain.family.usecase.GetFamilyMembersUseCase
import iti.grad.nutriscan.domain.family.usecase.UploadFamilyMemberImageUseCase
import iti.grad.nutriscan.domain.family.usecase.UpdateFamilyMemberUseCase
import iti.grad.nutriscan.presentation.settings.profile.add_member.state.AddFamilyMemberEffect
import iti.grad.nutriscan.presentation.settings.profile.add_member.state.AddFamilyMemberEvent
import iti.grad.nutriscan.presentation.settings.profile.add_member.state.AddFamilyMemberState
import iti.grad.presentation.R
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for the Add Family Member bottom sheet. Scoped to the sheet's
 * composition (not shared with [iti.grad.nutriscan.presentation.settings.profile.viewmodel.UserProfileViewModel])
 * so it owns the allergy/disease-loading concerns the profile screen doesn't
 * otherwise need — cleared automatically when the sheet closes.
 *
 * Loading pattern for diseases/allergies mirrors
 * [iti.grad.nutriscan.presentation.profile_setup.viewmodel.ProfileSetupPagerViewModel]
 * exactly: sync from network, then collect the offline-first Room-backed flow.
 */
@HiltViewModel
class AddFamilyMemberViewModel @Inject constructor(
    private val getDiseasesUseCase: GetDiseasesUseCase,
    private val getAllergiesUseCase: GetAllergiesUseCase,
    private val syncDiseasesUseCase: SyncDiseasesUseCase,
    private val syncAllergiesUseCase: SyncAllergiesUseCase,
    private val addFamilyMemberUseCase: AddFamilyMemberUseCase,
    private val updateFamilyMemberUseCase: UpdateFamilyMemberUseCase,
    private val uploadFamilyMemberImageUseCase: UploadFamilyMemberImageUseCase,
    private val getFamilyMembersUseCase: GetFamilyMembersUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AddFamilyMemberState())
    val state: StateFlow<AddFamilyMemberState> = _state.asStateFlow()

    private val _effect = Channel<AddFamilyMemberEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadDiseases()
        loadAllergies()
    }

    fun onEvent(event: AddFamilyMemberEvent) {
        when (event) {
            is AddFamilyMemberEvent.Initialize -> initializeForm(event.memberId)

            is AddFamilyMemberEvent.NameChanged ->
                _state.update { it.copy(name = event.name, nameError = null) }

            is AddFamilyMemberEvent.RelationChanged ->
                _state.update { it.copy(relation = event.relation, relationError = null) }

            is AddFamilyMemberEvent.ToggleDisease -> _state.update {
                it.copy(selectedDiseaseIds = it.selectedDiseaseIds.toggle(event.id).toImmutableList())
            }

            is AddFamilyMemberEvent.ToggleAllergy -> _state.update {
                it.copy(selectedAllergyIds = it.selectedAllergyIds.toggle(event.id).toImmutableList())
            }

            is AddFamilyMemberEvent.ImageSelected -> _state.update {
                it.copy(
                    selectedImagePath = event.imageFilePath,
                    imageUploadErrorMessage = null
                )
            }

            AddFamilyMemberEvent.RemoveSelectedImage -> _state.update {
                it.copy(selectedImagePath = null, imageUploadErrorMessage = null)
            }

            AddFamilyMemberEvent.RetryImageUpload -> retryImageUpload()

            AddFamilyMemberEvent.RetryLoadDiseases -> loadDiseases()
            AddFamilyMemberEvent.RetryLoadAllergies -> loadAllergies()
            AddFamilyMemberEvent.SaveClicked -> save()
            AddFamilyMemberEvent.DismissRequested -> emitEffect(AddFamilyMemberEffect.Dismiss)
        }
    }

    private fun initializeForm(memberId: String?) {
        if (memberId == null) {
            _state.update {
                it.copy(
                    name = "",
                    nameError = null,
                    relation = "",
                    relationError = null,
                    selectedDiseaseIds = persistentListOf(),
                    selectedAllergyIds = persistentListOf(),
                    selectedImagePath = null,
                    currentImageUrl = null,
                    pendingImageUploadMemberId = null,
                    isImageUploading = false,
                    imageUploadErrorMessage = null,
                    editingMemberId = null
                )
            }
        } else {
            viewModelScope.launch {
                try {
                    val members = getFamilyMembersUseCase().first()
                    val member = members.find { it.id == memberId }
                    if (member != null) {
                        _state.update {
                            it.copy(
                                name = member.name,
                                nameError = null,
                                relation = member.relation,
                                relationError = null,
                                selectedDiseaseIds = member.diseaseIds.toImmutableList(),
                                selectedAllergyIds = member.allergyIds.toImmutableList(),
                                selectedImagePath = null,
                                currentImageUrl = member.imageUrl,
                                pendingImageUploadMemberId = null,
                                isImageUploading = false,
                                imageUploadErrorMessage = null,
                                editingMemberId = member.id
                            )
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    private fun loadDiseases() {
        viewModelScope.launch {
            getDiseasesUseCase().collectLatest { diseases ->
                _state.update { it.copy(diseases = diseases.toImmutableList()) }
            }
        }

        viewModelScope.launch {
            _state.update { it.copy(isDiseasesLoading = true, diseasesErrorMessage = null) }

            val syncResult = syncDiseasesUseCase()
            _state.update { state ->
                val error = if (state.diseases.isEmpty()) {
                    syncResult.exceptionOrNull()?.message ?: "Failed to load diseases"
                } else null

                state.copy(
                    isDiseasesLoading = false,
                    diseasesErrorMessage = if (syncResult.isFailure) error else null
                )
            }
        }
    }

    private fun loadAllergies() {
        viewModelScope.launch {
            getAllergiesUseCase().collectLatest { allergies ->
                _state.update { it.copy(allergies = allergies.toImmutableList()) }
            }
        }

        viewModelScope.launch {
            _state.update { it.copy(isAllergiesLoading = true, allergiesErrorMessage = null) }

            val syncResult = syncAllergiesUseCase()
            _state.update { state ->
                val error = if (state.allergies.isEmpty()) {
                    syncResult.exceptionOrNull()?.message ?: "Failed to load allergies"
                } else null

                state.copy(
                    isAllergiesLoading = false,
                    allergiesErrorMessage = if (syncResult.isFailure) error else null
                )
            }
        }
    }

    private fun save() {
        val name = _state.value.name.trim()
        val relation = _state.value.relation.trim()

        var hasError = false
        if (name.isBlank()) {
            _state.update { it.copy(nameError = R.string.error_name_required) }
            hasError = true
        }
        if (relation.isBlank()) {
            _state.update { it.copy(relationError = R.string.error_relation_required) }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val editingId = _state.value.editingMemberId
            val existingMemberIds = getFamilyMembersUseCase().first().map { it.id }.toSet()
            val selectedImagePath = _state.value.selectedImagePath
            val result = if (editingId != null) {
                updateFamilyMemberUseCase(
                    memberId = editingId,
                    name = name,
                    relation = relation,
                    allergyIds = _state.value.selectedAllergyIds,
                    diseaseIds = _state.value.selectedDiseaseIds,
                )
            } else {
                addFamilyMemberUseCase(
                    name = name,
                    relation = relation,
                    allergyIds = _state.value.selectedAllergyIds,
                    diseaseIds = _state.value.selectedDiseaseIds,
                )
            }

            result
                .onSuccess {
                    _state.update { it.copy(isSaving = false) }

                    if (selectedImagePath.isNullOrBlank()) {
                        emitEffect(AddFamilyMemberEffect.Dismiss)
                        return@onSuccess
                    }

                    val targetMemberId = editingId ?: resolveCreatedMemberId(
                        existingIds = existingMemberIds,
                        name = name,
                        relation = relation,
                    )

                    if (targetMemberId.isNullOrBlank()) {
                        _state.update {
                            it.copy(
                                imageUploadErrorMessage = null,
                                pendingImageUploadMemberId = null,
                            )
                        }
                        emitEffect(AddFamilyMemberEffect.ShowErrorRes(R.string.edit_profile_avatar_upload_error))
                        return@onSuccess
                    }

                    _state.update { it.copy(pendingImageUploadMemberId = targetMemberId) }
                    uploadImageForMember(memberId = targetMemberId, dismissOnSuccess = true)
                }
                .onFailure {
                    _state.update { state -> state.copy(isSaving = false) }
                    emitEffect(AddFamilyMemberEffect.ShowError(it.message ?: ""))
                }
        }
    }

    private fun retryImageUpload() {
        val memberId = _state.value.pendingImageUploadMemberId ?: _state.value.editingMemberId
        if (memberId.isNullOrBlank()) {
            emitEffect(AddFamilyMemberEffect.ShowErrorRes(R.string.edit_profile_avatar_upload_error))
            return
        }
        uploadImageForMember(memberId = memberId, dismissOnSuccess = true)
    }

    private fun uploadImageForMember(memberId: String, dismissOnSuccess: Boolean) {
        val imagePath = _state.value.selectedImagePath
        if (imagePath.isNullOrBlank()) {
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isImageUploading = true,
                    imageUploadErrorMessage = null,
                    pendingImageUploadMemberId = memberId,
                )
            }

            val uploadResult = uploadFamilyMemberImageUseCase(memberId, File(imagePath))

            uploadResult
                .onSuccess {
                    _state.update {
                        it.copy(
                            isImageUploading = false,
                            imageUploadErrorMessage = null,
                            pendingImageUploadMemberId = null,
                            currentImageUrl = null,
                            selectedImagePath = null,
                        )
                    }
                    if (dismissOnSuccess) {
                        emitEffect(AddFamilyMemberEffect.Dismiss)
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isImageUploading = false,
                            imageUploadErrorMessage = error.message,
                            pendingImageUploadMemberId = memberId,
                        )
                    }
                    emitEffect(AddFamilyMemberEffect.ShowErrorRes(R.string.edit_profile_avatar_upload_error))
                }
        }
    }

    private suspend fun resolveCreatedMemberId(
        existingIds: Set<String>,
        name: String,
        relation: String,
    ): String? {
        val members = getFamilyMembersUseCase().first()

        val newMembers = members.filterNot { existingIds.contains(it.id) }
        if (newMembers.size == 1) {
            return newMembers.first().id
        }

        return members.firstOrNull { member ->
            member.name.equals(name, ignoreCase = true) &&
                member.relation.equals(relation, ignoreCase = true)
        }?.id
    }

    private fun List<Int>.toggle(id: Int) = if (contains(id)) this - id else this + id

    private fun emitEffect(effect: AddFamilyMemberEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }
}
