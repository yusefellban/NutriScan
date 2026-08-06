package iti.grad.nutriscan.presentation.settings.profile

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import iti.grad.nutriscan.domain.family.model.FamilyMember
import iti.grad.nutriscan.domain.family.repository.IFamilyMemberRepository
import iti.grad.nutriscan.domain.streak.model.StreakInfo
import iti.grad.nutriscan.domain.streak.usecase.ObserveStreakUseCase
import iti.grad.nutriscan.domain.user.model.User
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import iti.grad.nutriscan.presentation.settings.profile.state.FamilyMemberUiModel
import iti.grad.nutriscan.presentation.settings.profile.state.UserProfileEffect
import iti.grad.nutriscan.presentation.settings.profile.state.UserProfileEvent
import iti.grad.nutriscan.presentation.settings.profile.viewmodel.UserProfileViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileViewModelTest {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    private lateinit var viewModel: UserProfileViewModel
    private val userData = MutableStateFlow<User?>(null)
    private val familyMembersFlow = MutableStateFlow<List<FamilyMember>>(emptyList())
    private val streakFlow = MutableStateFlow(StreakInfo(currentStreak = 0, longestStreak = 0))

    private val userRepository: IUserRepository = mockk {
        coEvery { fetchAndSyncProfile() } returns Result.success(Unit)
        coEvery { getUserData() } returns userData
    }

    private val familyMemberRepository: IFamilyMemberRepository = mockk {
        coEvery { getFamilyMembers() } returns familyMembersFlow
        coEvery { removeFamilyMember(any()) } returns Result.success(Unit)
    }

    private val observeStreak: ObserveStreakUseCase = mockk {
        every { this@mockk() } returns streakFlow
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = UserProfileViewModel(userRepository, familyMemberRepository, observeStreak)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty user name and starts with no family members`() = runTest(testDispatcher) {
        val state = viewModel.state.value
        assertEquals("", state.userName)
        assertEquals(0, state.streakDays)
        assertTrue(state.familyMembers.isEmpty())
        assertNull(state.memberPendingDeletion)
        assertFalse(state.isAddMemberSheetVisible)
    }

    @Test
    fun `when the streak use case emits, streakDays updates`() = runTest(testDispatcher) {
        streakFlow.value = StreakInfo(currentStreak = 7, longestStreak = 12)
        testScheduler.advanceUntilIdle()

        assertEquals(7, viewModel.state.value.streakDays)
    }

    @Test
    fun `when repository emits a user, userName and avatarUrl update`() = runTest(testDispatcher) {
        userData.value = User(
            id = "1",
            firstName = "Osama",
            lastName = "Hosam",
            email = "osama@example.com",
            gender = null,
            dateOfBirth = null,
            heightCm = null,
            weightKg = null,
            diseaseIds = emptyList(),
            allergyIds = emptyList(),
            avatarUrl = "https://example.com/avatar.png",
        )
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Osama Hosam", state.userName)
        assertEquals("https://example.com/avatar.png", state.avatarUrl)
    }

    @Test
    fun `when EditProfileClicked, effect is NavigateToEditProfile`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(UserProfileEvent.EditProfileClicked)
            assertEquals(UserProfileEffect.NavigateToEditProfile, awaitItem())
        }
    }

    @Test
    fun `when AddMemberClicked, isAddMemberSheetVisible is set to true`() = runTest(testDispatcher) {
        viewModel.onEvent(UserProfileEvent.AddMemberClicked)
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.isAddMemberSheetVisible)
    }

    @Test
    fun `when AddMemberSheetDismissed, isAddMemberSheetVisible is set to false`() = runTest(testDispatcher) {
        viewModel.onEvent(UserProfileEvent.AddMemberClicked)
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.isAddMemberSheetVisible)

        viewModel.onEvent(UserProfileEvent.AddMemberSheetDismissed)
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.isAddMemberSheetVisible)
    }

    @Test
    fun `when familyMemberRepository emits members, state reflects them`() = runTest(testDispatcher) {
        val mockMembers = listOf(
            FamilyMember("1", "Alice", relation = "", allergyIds = listOf(1), diseaseIds = listOf(2)),
            FamilyMember("2", "Bob", relation = "", allergyIds = emptyList(), diseaseIds = emptyList())
        )
        familyMembersFlow.value = mockMembers
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.familyMembers.size)
        assertEquals("1", state.familyMembers[0].id)
        assertEquals("Alice", state.familyMembers[0].name)
        assertEquals("2", state.familyMembers[1].id)
        assertEquals("Bob", state.familyMembers[1].name)
    }

    @Test
    fun `when FamilyMemberDetailClicked, add member bottom sheet is displayed with that member id`() = runTest(testDispatcher) {
        val mockMembers = listOf(FamilyMember("123", "Alice"))
        familyMembersFlow.value = mockMembers
        testScheduler.advanceUntilIdle()

        viewModel.onEvent(UserProfileEvent.FamilyMemberDetailClicked("123"))
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.isAddMemberSheetVisible)
        assertEquals("123", state.editingMemberId)
    }

    @Test
    fun `when FamilyMemberLongPressed, memberPendingDeletion is set to that member`() = runTest(testDispatcher) {
        val mockMembers = listOf(FamilyMember("123", "Alice"))
        familyMembersFlow.value = mockMembers
        testScheduler.advanceUntilIdle()

        viewModel.onEvent(UserProfileEvent.FamilyMemberLongPressed("123"))
        testScheduler.advanceUntilIdle()

        val pending = viewModel.state.value.memberPendingDeletion
        assertTrue(pending != null)
        assertEquals("123", pending?.id)
        assertEquals("Alice", pending?.name)
    }

    @Test
    fun `when ConfirmRemoveMemberClicked succeeds, removeFamilyMember is invoked and state is cleared`() = runTest(testDispatcher) {
        val mockMembers = listOf(FamilyMember("123", "Alice"))
        familyMembersFlow.value = mockMembers
        testScheduler.advanceUntilIdle()

        viewModel.onEvent(UserProfileEvent.FamilyMemberLongPressed("123"))
        testScheduler.advanceUntilIdle()

        coEvery { familyMemberRepository.removeFamilyMember("123") } returns Result.success(Unit)

        viewModel.onEvent(UserProfileEvent.ConfirmRemoveMemberClicked)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { familyMemberRepository.removeFamilyMember("123") }
        assertNull(viewModel.state.value.memberPendingDeletion)
    }

    @Test
    fun `when ConfirmRemoveMemberClicked fails, ShowError effect is emitted`() = runTest(testDispatcher) {
        val mockMembers = listOf(FamilyMember("123", "Alice"))
        familyMembersFlow.value = mockMembers
        testScheduler.advanceUntilIdle()

        viewModel.onEvent(UserProfileEvent.FamilyMemberLongPressed("123"))
        testScheduler.advanceUntilIdle()

        coEvery { familyMemberRepository.removeFamilyMember("123") } returns Result.failure(Exception("Removal failed"))

        viewModel.effect.test {
            viewModel.onEvent(UserProfileEvent.ConfirmRemoveMemberClicked)
            testScheduler.advanceUntilIdle()
            assertEquals(UserProfileEffect.ShowError("Removal failed"), awaitItem())
        }
    }

    @Test
    fun `when CancelRemoveMemberClicked, memberPendingDeletion is cleared`() = runTest(testDispatcher) {
        val mockMembers = listOf(FamilyMember("123", "Alice"))
        familyMembersFlow.value = mockMembers
        testScheduler.advanceUntilIdle()

        viewModel.onEvent(UserProfileEvent.FamilyMemberLongPressed("123"))
        testScheduler.advanceUntilIdle()

        viewModel.onEvent(UserProfileEvent.CancelRemoveMemberClicked)
        testScheduler.advanceUntilIdle()

        assertNull(viewModel.state.value.memberPendingDeletion)
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
            assertEquals(UserProfileEffect.NavigateToNotificationSettings, awaitItem())
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
    fun `Refreshed re-syncs the profile and clears isRefreshing when it finishes`() = runTest(testDispatcher) {
        viewModel.onEvent(UserProfileEvent.Refreshed)
        testScheduler.advanceUntilIdle()

        assertEquals(false, viewModel.state.value.isRefreshing)
        coVerify { userRepository.fetchAndSyncProfile() }
    }
}
