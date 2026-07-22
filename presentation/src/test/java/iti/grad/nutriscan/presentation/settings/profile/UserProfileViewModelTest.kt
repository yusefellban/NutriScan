package iti.grad.nutriscan.presentation.settings.profile

import app.cash.turbine.test
import iti.grad.nutriscan.presentation.common.model.BottomNavTab
import iti.grad.nutriscan.presentation.settings.profile.state.UserProfileEffect
import iti.grad.nutriscan.presentation.settings.profile.state.UserProfileEvent
import iti.grad.nutriscan.presentation.settings.profile.viewmodel.UserProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileViewModelTest {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    private lateinit var viewModel: UserProfileViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = UserProfileViewModel()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has correct dummy data and starts with no family members`() = runTest(testDispatcher) {
        val state = viewModel.state.value
        assertEquals("Osama Hosam", state.userName)
        assertEquals(15, state.streakDays)
        assertEquals(BottomNavTab.PROFILE, state.selectedTab)
        assertTrue(state.familyMembers.isEmpty())
        assertNull(state.memberPendingDeletion)
    }

    @Test
    fun `when EditProfileClicked, effect is NavigateToEditProfile`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(UserProfileEvent.EditProfileClicked)
            assertEquals(UserProfileEffect.NavigateToEditProfile, awaitItem())
        }
    }

    @Test
    fun `when AddMemberClicked, a new member is appended with no effect emitted`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(UserProfileEvent.AddMemberClicked)
            expectNoEvents()
        }
        val members = viewModel.state.value.familyMembers
        assertEquals(1, members.size)
        assertEquals("Ashraf Shrief", members[0].name)
    }

    @Test
    fun `when AddMemberClicked twice, both members get unique ids`() = runTest(testDispatcher) {
        viewModel.onEvent(UserProfileEvent.AddMemberClicked)
        viewModel.onEvent(UserProfileEvent.AddMemberClicked)

        val members = viewModel.state.value.familyMembers
        assertEquals(2, members.size)
        assertTrue(members[0].id != members[1].id)
    }

    @Test
    fun `when FamilyMemberDetailClicked, effect carries the clicked member id`() = runTest(testDispatcher) {
        viewModel.onEvent(UserProfileEvent.AddMemberClicked)
        val memberId = viewModel.state.value.familyMembers[0].id

        viewModel.effect.test {
            viewModel.onEvent(UserProfileEvent.FamilyMemberDetailClicked(memberId))
            assertEquals(UserProfileEffect.NavigateToFamilyMemberDetail(memberId), awaitItem())
        }
    }

    @Test
    fun `when FamilyMemberLongPressed, memberPendingDeletion is set to that member`() = runTest(testDispatcher) {
        viewModel.onEvent(UserProfileEvent.AddMemberClicked)
        val member = viewModel.state.value.familyMembers[0]

        viewModel.onEvent(UserProfileEvent.FamilyMemberLongPressed(member.id))

        assertEquals(member, viewModel.state.value.memberPendingDeletion)
    }

    @Test
    fun `when ConfirmRemoveMemberClicked, the pending member is removed and dialog clears`() = runTest(testDispatcher) {
        viewModel.onEvent(UserProfileEvent.AddMemberClicked)
        viewModel.onEvent(UserProfileEvent.AddMemberClicked)
        val members = viewModel.state.value.familyMembers
        viewModel.onEvent(UserProfileEvent.FamilyMemberLongPressed(members[0].id))

        viewModel.onEvent(UserProfileEvent.ConfirmRemoveMemberClicked)

        val state = viewModel.state.value
        assertEquals(1, state.familyMembers.size)
        assertEquals(members[1].id, state.familyMembers[0].id)
        assertNull(state.memberPendingDeletion)
    }

    @Test
    fun `when CancelRemoveMemberClicked, dialog clears and no member is removed`() = runTest(testDispatcher) {
        viewModel.onEvent(UserProfileEvent.AddMemberClicked)
        val member = viewModel.state.value.familyMembers[0]
        viewModel.onEvent(UserProfileEvent.FamilyMemberLongPressed(member.id))

        viewModel.onEvent(UserProfileEvent.CancelRemoveMemberClicked)

        val state = viewModel.state.value
        assertEquals(1, state.familyMembers.size)
        assertNull(state.memberPendingDeletion)
    }

    @Test
    fun `when ScanHistoryClicked, effect is NavigateToScanHistory`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(UserProfileEvent.ScanHistoryClicked)
            assertEquals(UserProfileEffect.NavigateToScanHistory, awaitItem())
        }
    }

    @Test
    fun `when NotificationsClicked, effect is NavigateToNotifications`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(UserProfileEvent.NotificationsClicked)
            assertEquals(UserProfileEffect.NavigateToNotifications, awaitItem())
        }
    }

    @Test
    fun `when SettingsClicked, effect is NavigateToSettings`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(UserProfileEvent.SettingsClicked)
            assertEquals(UserProfileEffect.NavigateToSettings, awaitItem())
        }
    }

    @Test
    fun `when BottomNavTabClicked to HOME, effect is NavigateToTab HOME and selectedTab stays PROFILE`() =
        runTest(testDispatcher) {
            viewModel.effect.test {
                viewModel.onEvent(UserProfileEvent.BottomNavTabClicked(BottomNavTab.HOME))
                assertEquals(UserProfileEffect.NavigateToTab(BottomNavTab.HOME), awaitItem())
            }
            assertEquals(BottomNavTab.PROFILE, viewModel.state.value.selectedTab)
        }

    @Test
    fun `when BottomNavTabClicked to SCAN, effect is NavigateToTab SCAN`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(UserProfileEvent.BottomNavTabClicked(BottomNavTab.SCAN))
            assertEquals(UserProfileEffect.NavigateToTab(BottomNavTab.SCAN), awaitItem())
        }
    }

    @Test
    fun `when BottomNavTabClicked to HISTORY, effect is NavigateToTab HISTORY`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(UserProfileEvent.BottomNavTabClicked(BottomNavTab.HISTORY))
            assertEquals(UserProfileEffect.NavigateToTab(BottomNavTab.HISTORY), awaitItem())
        }
    }

    @Test
    fun `when BottomNavTabClicked to SAVED, effect is NavigateToTab SAVED`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(UserProfileEvent.BottomNavTabClicked(BottomNavTab.SAVED))
            assertEquals(UserProfileEffect.NavigateToTab(BottomNavTab.SAVED), awaitItem())
        }
    }

    @Test
    fun `when BottomNavTabClicked to PROFILE, selectedTab stays PROFILE and no effect is emitted`() =
        runTest(testDispatcher) {
            viewModel.effect.test {
                viewModel.onEvent(UserProfileEvent.BottomNavTabClicked(BottomNavTab.PROFILE))
                expectNoEvents()
            }
            assertEquals(BottomNavTab.PROFILE, viewModel.state.value.selectedTab)
        }
}
