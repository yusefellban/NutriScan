package iti.grad.nutriscan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.settings.model.ThemeMode
import iti.grad.nutriscan.domain.settings.usecase.GetThemeModeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    getThemeModeUseCase: GetThemeModeUseCase
) : ViewModel() {

    private val _themeMode = MutableStateFlow<ThemeMode?>(null)
    val themeMode: StateFlow<ThemeMode?> = _themeMode.asStateFlow()

    init {
        viewModelScope.launch {
            _themeMode.value = getThemeModeUseCase()
        }
    }
}
