package com.eddy.webcrawler.data.model

sealed class ContentItem {
    abstract val stableId: String

    data class ImageItem(
        override val stableId: String,
        val url: String,
        val displayName: String,
        val fileExtension: String? = null,
        val localPath: String? = null
    ) : ContentItem()

    data class LinkItem(
        override val stableId: String,
        val url: String,
        val displayName: String
    ) : ContentItem()

    data class DownloadItem(
        override val stableId: String,
        val url: String,
        val displayName: String,
        val fileExtension: String,
        val downloadStatus: DownloadStatus,
        val localPath: String?
    ) : ContentItem()
}

enum class DownloadStatus {
    NOT_DOWNLOADED, DOWNLOADING, DOWNLOADED, EXTRACTED, FAILED
}
