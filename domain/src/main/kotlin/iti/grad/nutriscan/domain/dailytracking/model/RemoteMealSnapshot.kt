package iti.grad.nutriscan.domain.dailytracking.model

/** One meal as returned by the backend for a given day — used only for the
 * login/app-start reconciliation flow (Task 15), not the live add/remove path. */
data class RemoteMealSnapshot(
    val scanId: String,
    val productName: String?,
    val imageUrl: String?,
    val calories: Int,
)
