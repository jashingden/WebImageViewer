package com.eddy.webcrawler.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eddy.webcrawler.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val lockEnabled: StateFlow<Boolean> = settingsRepository.lockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val lockPin: StateFlow<String?> = settingsRepository.lockPin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLockEnabled(enabled)
        }
    }

    fun setLockPin(pin: String?) {
        viewModelScope.launch {
            if (pin == null) {
                settingsRepository.setLockEnabled(false)
                settingsRepository.setLockPin("")
            } else {
                settingsRepository.setLockPin(pin)
                settingsRepository.setLockEnabled(true)
            }
        }
    }
}
