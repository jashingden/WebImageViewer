package com.eddy.webcrawler.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eddy.webcrawler.data.model.ContentItem
import com.eddy.webcrawler.data.model.DownloadStatus
import com.eddy.webcrawler.data.model.LinkEntry
import com.eddy.webcrawler.data.repository.CrawlerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PageState {
    object Idle : PageState()
    object Loading : PageState()
    data class Success(val content: List<ContentItem>) : PageState()
    data class Error(val message: String) : PageState()
}

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val repository: CrawlerRepository
) : ViewModel() {

    private val _pageState = MutableStateFlow<PageState>(PageState.Idle)
    val pageState: StateFlow<PageState> = _pageState.asStateFlow()

    fun loadContent(indexId: Long) {
        viewModelScope.launch {
            _pageState.value = PageState.Loading
            repository.getEntriesAsContentItems(indexId)
                .catch { error ->
                    _pageState.value = PageState.Error(error.message ?: "載入失敗")
                }
                .collectLatest { items ->
                    _pageState.value = PageState.Success(items)
                }
        }
    }

    fun startDownload(entryId: Long, indexId: Long, url: String) {
        viewModelScope.launch {
            val entry = LinkEntry(
                id = entryId,
                linkIndexId = indexId,
                displayName = "",
                url = url,
                type = "DOWNLOAD",
                downloadStatus = DownloadStatus.DOWNLOADING.name
            )
            repository.updateEntry(entry)
        }
    }

    fun markDownloadComplete(entryId: Long, indexId: Long, localPath: String) {
        viewModelScope.launch {
            val entry = LinkEntry(
                id = entryId,
                linkIndexId = indexId,
                displayName = "",
                url = "",
                type = "DOWNLOAD",
                downloadStatus = DownloadStatus.EXTRACTED.name,
                localPath = localPath
            )
            repository.updateEntry(entry)
        }
    }

    fun markDownloadFailed(entryId: Long, indexId: Long) {
        viewModelScope.launch {
            val entry = LinkEntry(
                id = entryId,
                linkIndexId = indexId,
                displayName = "",
                url = "",
                type = "DOWNLOAD",
                downloadStatus = DownloadStatus.FAILED.name
            )
            repository.updateEntry(entry)
        }
    }
}
