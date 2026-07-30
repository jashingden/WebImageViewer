package com.eddy.webcrawler.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eddy.webcrawler.data.repository.BackupRepository
import com.eddy.webcrawler.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _backupResult = MutableSharedFlow<Result<Unit>>()
    val backupResult: SharedFlow<Result<Unit>> = _backupResult.asSharedFlow()

    private val _restoreResult = MutableSharedFlow<Result<Unit>>()
    val restoreResult: SharedFlow<Result<Unit>> = _restoreResult.asSharedFlow()

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

    fun backupData(outputStream: OutputStream) {
        viewModelScope.launch {
            val result = backupRepository.backupToUri(outputStream)
            _backupResult.emit(result)
        }
    }

    fun restoreData(inputStream: InputStream) {
        viewModelScope.launch {
            val result = backupRepository.restoreFromUri(inputStream)
            _restoreResult.emit(result)
        }
    }
}
