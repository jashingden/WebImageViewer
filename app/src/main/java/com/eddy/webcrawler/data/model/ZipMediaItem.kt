package com.eddy.webcrawler.data.model

data class ZipMediaItem(
    val name: String,
    val localPath: String,
    val mediaType: MediaType
)

enum class MediaType { IMAGE, VIDEO }
