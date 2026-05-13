package com.eddy.webcrawler.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.eddy.webcrawler.data.model.LinkEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkEntryDao {

    @Query("SELECT * FROM linkentry WHERE linkIndexId = :indexId ORDER BY id ASC")
    fun getEntriesByIndexId(indexId: Long): Flow<List<LinkEntry>>

    @Query("SELECT * FROM linkentry WHERE linkIndexId = :indexId AND type = 'IMAGE'")
    fun getImagesByIndexId(indexId: Long): Flow<List<LinkEntry>>

    @Query("SELECT * FROM linkentry WHERE linkIndexId = :indexId AND type = 'DOWNLOAD'")
    fun getDownloadsByIndexId(indexId: Long): Flow<List<LinkEntry>>

    @Query("SELECT * FROM linkentry WHERE id = :id")
    suspend fun getEntryById(id: Long): LinkEntry?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntries(entries: List<LinkEntry>)

    @Update
    suspend fun updateEntry(entry: LinkEntry)

    @Query("DELETE FROM linkentry WHERE linkIndexId = :indexId")
    suspend fun deleteEntriesByIndexId(indexId: Long)
}
