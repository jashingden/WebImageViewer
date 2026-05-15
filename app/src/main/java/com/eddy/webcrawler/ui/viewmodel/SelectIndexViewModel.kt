package com.eddy.webcrawler.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eddy.webcrawler.data.model.IndexWithThumbnail
import com.eddy.webcrawler.data.repository.CrawlerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectIndexViewModel @Inject constructor(
    private val repository: CrawlerRepository
) : ViewModel() {

    val indices: StateFlow<List<IndexWithThumbnail>> = repository.getIndicesWithThumbnails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteIndex(indexId: Long) {
        viewModelScope.launch {
            repository.deleteIndexWithFiles(indexId)
        }
    }
}
