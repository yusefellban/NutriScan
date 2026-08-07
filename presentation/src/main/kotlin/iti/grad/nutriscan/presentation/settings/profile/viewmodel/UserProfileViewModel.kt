package iti.grad.nutriscan.presentation.settings.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.family.repository.IFamilyMemberRepository
import iti.grad.nutriscan.domain.streak.usecase.ObserveStreakUseCase
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import iti.grad.nutriscan.presentation.common.model.BottomNavTab
import iti.grad.nutriscan.presentation.settings.profile.state.FamilyMemberUiModel
import iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState
import iti.grad.nutriscan.presentation.settings.profile.state.UserProfileEffect
import iti.grad.nutriscan.presentation.settings.profile.state.UserProfileEvent
import iti.grad.nutriscan.presentation.settings.profile.state.UserProfileState
import iti.grad.presentation.R
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the User Profile screen.
 *
 * Family members are backed by Room via [IFamilyMemberRepository] (single
 * source of truth) — see the family-members implementation plan. Injects the
 * repository interface directly rather than a use case, matching this
 * screen's existing local convention of injecting [IUserRepository] directly.
 */
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: IUserRepository,
    private val familyMemberRepository: IFamilyMemberRepository,
    private val observeStreak: ObserveStreakUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(createInitialState())
    val state: StateFlow<UserProfileState> = _state.asStateFlow()

    private val _effect = Channel<UserProfileEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadProfileData()
        viewModelScope.launch {
            userRepository.getUserData()
                .catch { /* Ignored */ }
                .collectLatest { user ->
                if (user != null) {
                    _state.update {
                        it.copy(
                            userName = "${user.firstName} ${user.lastName ?: ""}".trim(),
                            avatarUrl = user.avatarUrl,
                            avatarUpdatedAt = user.updatedAt,
                            bmi = user.bmi,
                            tdee = user.tdee,
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            observeStreak().collectLatest { streak ->
                _state.update { it.copy(streakDays = streak) }
            }
        }
        viewModelScope.launch {
            familyMemberRepository.getFamilyMembers().collectLatest { members ->
                _state.update {
                    it.copy(
                        familyMembers = members.map { member ->
                            FamilyMemberUiModel(id = member.id, name = member.name, avatarUrl = null)
                        }.toPersistentList()
                    )
                }
            }
        }
    }

    fun onEvent(event: UserProfileEvent) {
        when (event) {
            is UserProfileEvent.EditProfileClicked -> emitEffect(UserProfileEffect.NavigateToEditProfile)
            is UserProfileEvent.AddMemberClicked -> _state.update {
                it.copy(isAddMemberSheetVisible = true, editingMemberId = null)
            }
            is UserProfileEvent.AddMemberSheetDismissed -> _state.update {
                it.copy(isAddMemberSheetVisible = false, editingMemberId = null)
            }
            is UserProfileEvent.FamilyMemberDetailClicked -> _state.update {
                it.copy(isAddMemberSheetVisible = true, editingMemberId = event.memberId)
            }
            is UserProfileEvent.FamilyMemberLongPressed -> requestMemberRemoval(event.memberId)
            is UserProfileEvent.ConfirmRemoveMemberClicked -> confirmMemberRemoval()
            is UserProfileEvent.CancelRemoveMemberClicked -> _state.update { it.copy(memberPendingDeletion = null) }
            is UserProfileEvent.ScanHistoryClicked -> emitEffect(UserProfileEffect.NavigateToScanHistory)
            is UserProfileEvent.NotificationsClicked -> emitEffect(UserProfileEffect.NavigateToNotificationSettings)
            is UserProfileEvent.SettingsClicked -> emitEffect(UserProfileEffect.NavigateToSettings)
            is UserProfileEvent.CaloriesHistoryClicked -> emitEffect(UserProfileEffect.NavigateToCaloriesHistory)
            is UserProfileEvent.BottomNavTabClicked -> {
                // Profile is the only tab rendered by this screen; every other
                // tab is a separate destination pushed on top, so this retained
                // ViewModel's `selectedTab` is intentionally left at PROFILE —
                // mutating it here would leave the wrong tab highlighted when
                // the user navigates back.
                if (event.tab != BottomNavTab.PROFILE) {
                    emitEffect(UserProfileEffect.NavigateToTab(event.tab))
                }
            }
            UserProfileEvent.DismissAlert -> _state.update { it.copy(alertState = ProfileAlertState.None) }
            UserProfileEvent.RetryAction -> loadProfileData(isUserInitiated = true)
            UserProfileEvent.Refreshed -> refresh()
        }
    }

    private fun refresh() {
        if (_state.value.isRefreshing) return
        _state.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            userRepository.fetchAndSyncProfile()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun loadProfileData(isUserInitiated: Boolean = false) {
        viewModelScope.launch {
            if (isUserInitiated) {
                _state.update { it.copy(alertState = ProfileAlertState.None) }
            }
            userRepository.fetchAndSyncProfile()
                .onFailure { error ->
                    if (isUserInitiated) {
                        val newAlertState = when (error) {
                            is java.io.IOException -> ProfileAlertState.InternetError
                            else -> ProfileAlertState.Error(messageResId = R.string.profile_setup_load_error)
                        }
                        _state.update { it.copy(alertState = newAlertState) }
                    }
                }
        }
    }

    private fun requestMemberRemoval(memberId: String) {
        val member = _state.value.familyMembers.firstOrNull { it.id == memberId } ?: return
        _state.update { it.copy(memberPendingDeletion = member) }
    }

    private fun confirmMemberRemoval() {
        val pendingId = _state.value.memberPendingDeletion?.id ?: return
        _state.update { it.copy(memberPendingDeletion = null) }
        viewModelScope.launch {
            familyMemberRepository.removeFamilyMember(pendingId)
                .onFailure { emitEffect(UserProfileEffect.ShowError(it.message ?: "")) }
        }
    }

    private fun emitEffect(effect: UserProfileEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    private fun createInitialState(): UserProfileState = UserProfileState(
        familyMembers = persistentListOf(),
    )
}
