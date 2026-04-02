package com.example.webcrawler.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.webcrawler.data.repository.CrawlResult
import com.example.webcrawler.data.repository.CrawlerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val repository: CrawlerRepository
) : ViewModel() {

    private val _crawlState = MutableStateFlow<CrawlState>(CrawlState.Idle)
    val crawlState: StateFlow<CrawlState> = _crawlState.asStateFlow()

    fun crawl(url: String, pattern: String) {
        if (url.isBlank()) {
            _crawlState.value = CrawlState.Error("網址不能為空")
            return
        }

        if (!isValidUrl(url)) {
            _crawlState.value = CrawlState.Error("網址格式不正確")
            return
        }

        viewModelScope.launch {
            _crawlState.value = CrawlState.Loading
            repository.crawl(url, pattern)
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
