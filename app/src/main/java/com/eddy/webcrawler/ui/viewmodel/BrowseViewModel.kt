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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PageState {
    object Idle : PageState()
    object Loading : PageState()
    data class Success(val title: String, val content: List<ContentItem>) : PageState()
    data class Error(val message: String) : PageState()
}

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val repository: CrawlerRepository
) : ViewModel() {

    private val _pageState = MutableStateFlow<PageState>(PageState.Idle)
    val pageState: StateFlow<PageState> = _pageState.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    val allIndices = repository.getAllIndices()

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
        if (!_isEditMode.value) {
            _selectedIds.value = emptySet()
        }
    }

    fun toggleSelection(id: String) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedIds.value = current
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            repository.deleteEntry(id.toLong())
        }
    }

    fun deleteSelectedItems() {
        viewModelScope.launch {
            val ids = _selectedIds.value.map { it.toLong() }
            repository.deleteEntries(ids)
            _selectedIds.value = emptySet()
            _isEditMode.value = false
        }
    }

    fun getContentFlow(indexId: Long): Flow<PageState> = flow {
        if (indexId == 0L) {
            emit(PageState.Idle)
            return@flow
        }
        emit(PageState.Loading)

        val indexInfo = repository.getIndexById(indexId)
        val title = indexInfo?.title ?: "未知頁面"

        emitAll(
            repository.getEntriesAsContentItems(indexId)
                .map { items ->
                    // Trigger image downloads for missing localPaths
                    items.forEach { item ->
                        if (item is ContentItem.ImageItem && item.localPath == null) {
                            viewModelScope.launch {
                                repository.downloadImage(indexId, item.stableId.toLong(), item.url)
                            }
                        }
                    }
                    PageState.Success(title, items) as PageState
                }
                .catch { error ->
                    emit(PageState.Error(error.message ?: "載入失敗"))
                }
        )
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
