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

data class SelectableLink(
    val displayName: String,
    val url: String,
    val isChecked: Boolean = true
)

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

    private val _homeLinks = MutableStateFlow<List<SelectableLink>>(emptyList())
    val homeLinks: StateFlow<List<SelectableLink>> = _homeLinks.asStateFlow()

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

    fun fetchHomeLinks(url: String) {
        if (url.isBlank()) {
            _crawlState.value = CrawlState.Error("網址不能為空")
            return
        }

        if (!isValidUrl(url)) {
            _crawlState.value = CrawlState.Error("網址格式不正確")
            return
        }

        viewModelScope.launch {
            settingsRepository.saveLastCrawlSettings(url, "", "")
            _crawlState.value = CrawlState.Loading
            repository.fetchHomeLinks(url).fold(
                onSuccess = { entries ->
                    if (entries.isEmpty()) {
                        _homeLinks.value = emptyList()
                        _crawlState.value = CrawlState.Error("該網頁找不到任何連結")
                    } else {
                        _homeLinks.value = entries.map { entry ->
                            SelectableLink(
                                displayName = entry.displayName,
                                url = entry.url,
                                isChecked = true
                            )
                        }
                        _crawlState.value = CrawlState.Idle
                    }
                },
                onFailure = { error ->
                    _crawlState.value = CrawlState.Error(error.message ?: "讀取連結失敗")
                }
            )
        }
    }

    fun toggleAllLinks(checked: Boolean) {
        _homeLinks.value = _homeLinks.value.map { item ->
            item.copy(isChecked = checked)
        }
    }

    fun toggleLink(position: Int, checked: Boolean) {
        val currentList = _homeLinks.value.toMutableList()
        if (position in currentList.indices) {
            currentList[position] = currentList[position].copy(isChecked = checked)
            _homeLinks.value = currentList
        }
    }

    fun crawlSelectedGeneralLinks(url: String) {
        val currentLinks = _homeLinks.value
        if (currentLinks.isEmpty()) {
            fetchHomeLinks(url)
            return
        }

        val selectedUrls = currentLinks.filter { it.isChecked }.map { it.url }
        if (selectedUrls.isEmpty()) {
            _crawlState.value = CrawlState.Error("請至少勾選一個連結")
            return
        }

        viewModelScope.launch {
            _crawlState.value = CrawlState.Loading
            repository.crawlSelectedLinks(selectedUrls).fold(
                onSuccess = { result ->
                    _crawlState.value = CrawlState.Success(result)
                },
                onFailure = { error ->
                    _crawlState.value = CrawlState.Error(error.message ?: "發生錯誤，請重試")
                }
            )
        }
    }

    fun crawlAdvanced(url: String, pattern: String, rule: String) {
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
