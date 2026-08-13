package iti.grad.nutriscan.presentation.saved.state

import iti.grad.nutriscan.presentation.common.model.AppErrorType
import iti.grad.nutriscan.presentation.common.model.ProductUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class SavedState(
    val searchQuery: String = "",
    val products: ImmutableList<ProductUiModel> = persistentListOf(),
    val filteredProducts: ImmutableList<ProductUiModel> = persistentListOf(),
    val isLoading: Boolean = true,
    /** Drives the pull-to-refresh indicator; [isLoading] stays for the first load only. */
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val errorType: AppErrorType = AppErrorType.UNKNOWN,
)
