package iti.grad.nutriscan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.settings.model.ThemeMode
import iti.grad.nutriscan.domain.settings.usecase.ObserveThemeModeUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    observeThemeModeUseCase: ObserveThemeModeUseCase
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode?> = observeThemeModeUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
