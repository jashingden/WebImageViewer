package com.eddy.webcrawler.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eddy.webcrawler.data.repository.CrawlResult
import com.eddy.webcrawler.data.repository.CrawlerRepository
import com.eddy.webcrawler.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CrawlState {
    object Idle : CrawlState()
    object Loading : CrawlState()
    data class Success(val content: CrawlResult) : CrawlState()
    data class Error(val message: String) : CrawlState()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: CrawlerRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    var isAppUnlocked: Boolean = false
        private set

    fun setUnlocked() {
        isAppUnlocked = true
    }

    private val _crawlState = MutableStateFlow<CrawlState>(CrawlState.Idle)
    val crawlState: StateFlow<CrawlState> = _crawlState.asStateFlow()

    val lastUrl: StateFlow<String> = settingsRepository.lastUrl.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val lastPattern: StateFlow<String> = settingsRepository.lastPattern.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val lastRule: StateFlow<String> = settingsRepository.lastRule.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    fun resetCrawlState() {
        _crawlState.value = CrawlState.Idle
    }

    fun crawl(url: String, pattern: String, rule: String) {
        if (url.isBlank()) {
            _crawlState.value = CrawlState.Error("網址不能為空")
            return
        }

        if (!isValidUrl(url)) {
            _crawlState.value = CrawlState.Error("網址格式不正確")
            return
        }

        viewModelScope.launch {
            settingsRepository.saveLastCrawlSettings(url, pattern, rule)
            _crawlState.value = CrawlState.Loading
            repository.crawl(url, pattern, rule)
                .fold(
                    onSuccess = { result ->
                        _crawlState.value = CrawlState.Success(result)
                    },
                    onFailure = { error ->
                        _crawlState.value = CrawlState.Error(error.message ?: "發生錯誤，請重試")
                    }
                )
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            java.net.URL(url)
            true
        } catch (e: Exception) {
            false
        }
    }
}
