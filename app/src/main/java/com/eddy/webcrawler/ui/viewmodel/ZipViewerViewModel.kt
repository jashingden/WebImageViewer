package com.eddy.webcrawler.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eddy.webcrawler.data.model.MediaType
import com.eddy.webcrawler.data.model.ZipMediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class ZipViewerState {
    object Idle : ZipViewerState()
    object Loading : ZipViewerState()
    data class Success(val mediaItems: List<ZipMediaItem>) : ZipViewerState()
    data class Error(val message: String) : ZipViewerState()
}

@HiltViewModel
class ZipViewerViewModel @Inject constructor() : ViewModel() {

    private val _zipViewerState = MutableStateFlow<ZipViewerState>(ZipViewerState.Idle)
    val zipViewerState: StateFlow<ZipViewerState> = _zipViewerState.asStateFlow()

    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg")
    private val videoExtensions = setOf("mp4", "webm", "mkv", "avi")

    fun scanMedia(localPath: String?) {
        viewModelScope.launch {
            _zipViewerState.value = ZipViewerState.Loading
            try {
                if (localPath == null) {
                    _zipViewerState.value = ZipViewerState.Idle
                    return@launch
                }
                val directory = File(localPath)
                if (!directory.exists() || !directory.isDirectory) {
                    _zipViewerState.value = ZipViewerState.Error("目錄不存在")
                    return@launch
                }

                val mediaItems = scanDirectoryRecursively(directory)
                if (mediaItems.isEmpty()) {
                    _zipViewerState.value = ZipViewerState.Error("找不到任何媒体檔案")
                } else {
                    _zipViewerState.value = ZipViewerState.Success(mediaItems)
                }
            } catch (e: Exception) {
                _zipViewerState.value = ZipViewerState.Error(e.message ?: "掃描媒體失敗")
            }
        }
    }

    private fun scanDirectoryRecursively(directory: File): List<ZipMediaItem> {
        val items = mutableListOf<ZipMediaItem>()
        val files = directory.listFiles() ?: return items

        for (file in files) {
            if (file.isDirectory) {
                items.addAll(scanDirectoryRecursively(file))
            } else if (file.isFile) {
                val extension = file.extension.lowercase()
                when {
                    extension in imageExtensions -> {
                        items.add(ZipMediaItem(file.name, file.absolutePath, MediaType.IMAGE))
                    }
                    extension in videoExtensions -> {
                        items.add(ZipMediaItem(file.name, file.absolutePath, MediaType.VIDEO))
                    }
                }
            }
        }
        return items
    }
}
