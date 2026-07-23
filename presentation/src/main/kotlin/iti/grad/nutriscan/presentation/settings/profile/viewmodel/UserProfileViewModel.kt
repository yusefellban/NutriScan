package iti.grad.nutriscan.presentation.settings.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.presentation.settings.profile.state.FamilyMemberUiModel
import iti.grad.nutriscan.presentation.settings.profile.state.UserProfileEffect
import iti.grad.nutriscan.presentation.settings.profile.state.UserProfileEvent
import iti.grad.nutriscan.presentation.settings.profile.state.UserProfileState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import iti.grad.nutriscan.domain.user.repository.IUserRepository
import kotlinx.coroutines.flow.collectLatest

/**
 * ViewModel for the User Profile screen.
 *
 * Family members are mocked entirely in-memory here (add/remove) — there is
 * no domain use case yet, matching [iti.grad.nutriscan.presentation.home.viewmodel.HomeViewModel]'s
 * current no-usecase pattern. In a future sprint this will inject real
 * family-member use cases backed by the API.
 */
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: IUserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(createInitialState())
    val state: StateFlow<UserProfileState> = _state.asStateFlow()

    private val _effect = Channel<UserProfileEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadProfileData()
        viewModelScope.launch {
            userRepository.getUserData().collectLatest { user ->
                if (user != null) {
                    _state.update {
                        it.copy(
                            userName = "${user.firstName} ${user.lastName ?: ""}".trim(),
                            avatarUrl = user.avatarUrl
                        )
                    }
                }
            }
        }
    }

    /** Deterministic id source for mock-added members (also used in unit tests). */
    private var nextMemberId = 1

    fun onEvent(event: UserProfileEvent) {
        when (event) {
            is UserProfileEvent.EditProfileClicked -> emitEffect(UserProfileEffect.NavigateToEditProfile)
            is UserProfileEvent.AddMemberClicked -> addMockMember()
            is UserProfileEvent.FamilyMemberDetailClicked -> emitEffect(
                UserProfileEffect.NavigateToFamilyMemberDetail(event.memberId)
            )
            is UserProfileEvent.FamilyMemberLongPressed -> requestMemberRemoval(event.memberId)
            is UserProfileEvent.ConfirmRemoveMemberClicked -> confirmMemberRemoval()
            is UserProfileEvent.CancelRemoveMemberClicked -> _state.update { it.copy(memberPendingDeletion = null) }
            is UserProfileEvent.ScanHistoryClicked -> emitEffect(UserProfileEffect.NavigateToScanHistory)
            is UserProfileEvent.NotificationsClicked -> emitEffect(UserProfileEffect.NavigateToNotifications)
            is UserProfileEvent.SettingsClicked -> emitEffect(UserProfileEffect.NavigateToSettings)
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
            UserProfileEvent.DismissAlert -> _state.update { it.copy(alertState = iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState.None) }
            UserProfileEvent.RetryAction -> loadProfileData(isUserInitiated = true)
        }
    }

    private fun loadProfileData(isUserInitiated: Boolean = false) {
        viewModelScope.launch {
            if (isUserInitiated) {
                _state.update { it.copy(alertState = iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState.None) }
            }
            userRepository.fetchAndSyncProfile()
                .onFailure { error ->
                    if (isUserInitiated) {
                        val newAlertState = when (error) {
                            is java.io.IOException -> iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState.InternetError
                            else -> iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState.Error(messageResId = iti.grad.presentation.R.string.profile_setup_load_error)
                        }
                        _state.update { it.copy(alertState = newAlertState) }
                    }
                }
        }
    }

    private fun addMockMember() {
        val id = nextMemberId++
        val newMember = FamilyMemberUiModel(
            id = "member_$id",
            name = "Ashraf Shrief",
            avatarUrl = "https://i.pravatar.cc/200?u=family-member-$id",
        )
        _state.update { it.copy(familyMembers = (it.familyMembers + newMember).toPersistentList()) }
    }

    private fun requestMemberRemoval(memberId: String) {
        val member = _state.value.familyMembers.firstOrNull { it.id == memberId } ?: return
        _state.update { it.copy(memberPendingDeletion = member) }
    }

    private fun confirmMemberRemoval() {
        val pendingId = _state.value.memberPendingDeletion?.id ?: return
        _state.update {
            it.copy(
                familyMembers = it.familyMembers.filterNot { member -> member.id == pendingId }.toPersistentList(),
                memberPendingDeletion = null,
            )
        }
    }

    private fun emitEffect(effect: UserProfileEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    private fun createInitialState(): UserProfileState = UserProfileState(
        streakDays = 15,
        familyMembers = persistentListOf(),
    )
}
