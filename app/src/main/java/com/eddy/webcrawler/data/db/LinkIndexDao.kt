package com.eddy.webcrawler.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.eddy.webcrawler.data.model.IndexWithThumbnail
import com.eddy.webcrawler.data.model.LinkIndex
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkIndexDao {

    @Query("SELECT * FROM linkindex ORDER BY crawlTimestamp DESC")
    fun getAllIndices(): Flow<List<LinkIndex>>

    @Query("""
        SELECT li.*, 
               (SELECT url FROM linkentry WHERE linkIndexId = li.id AND type = 'IMAGE' AND (fileExtension IS NULL OR LOWER(fileExtension) != 'png') LIMIT 1) as thumbnailUrl,
               (SELECT localPath FROM linkentry WHERE linkIndexId = li.id AND type = 'IMAGE' AND (fileExtension IS NULL OR LOWER(fileExtension) != 'png') LIMIT 1) as localThumbnailPath
        FROM linkindex li
        ORDER BY li.crawlTimestamp DESC
    """)
    fun getIndicesWithThumbnails(): Flow<List<IndexWithThumbnail>>

    @Query("SELECT * FROM linkindex WHERE id = :id")
    suspend fun getIndexById(id: Long): LinkIndex?

    @Query("SELECT * FROM linkindex WHERE sourceUrl = :url")
    suspend fun getIndexByUrl(url: String): LinkIndex?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIndex(index: LinkIndex): Long

    @Update
    suspend fun updateIndex(index: LinkIndex)

    @Delete
    suspend fun deleteIndex(index: LinkIndex)
}
