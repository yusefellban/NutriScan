package iti.grad.nutriscan.presentation.settings.profile.add_member.state

/**
 * One-shot side effects emitted by
 * [iti.grad.nutriscan.presentation.settings.profile.add_member.viewmodel.AddFamilyMemberViewModel].
 */
sealed interface AddFamilyMemberEffect {
    data object Dismiss : AddFamilyMemberEffect
    data class ShowError(val message: String) : AddFamilyMemberEffect
}
