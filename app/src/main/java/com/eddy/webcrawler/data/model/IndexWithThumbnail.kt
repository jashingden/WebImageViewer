package com.eddy.webcrawler.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class IndexWithThumbnail(
    @Embedded
    val linkIndex: LinkIndex,
    
    @ColumnInfo(name = "thumbnailUrl")
    val thumbnailUrl: String?,
    
    @ColumnInfo(name = "localThumbnailPath")
    val localThumbnailPath: String?
)
