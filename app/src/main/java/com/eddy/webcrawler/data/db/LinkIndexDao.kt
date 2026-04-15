package com.eddy.webcrawler.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.eddy.webcrawler.data.model.LinkIndex
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkIndexDao {

    @Query("SELECT * FROM linkindex ORDER BY crawlTimestamp DESC")
    fun getAllIndices(): Flow<List<LinkIndex>>

    @Query("SELECT * FROM linkindex WHERE id = :id")
    suspend fun getIndexById(id: Long): LinkIndex?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIndex(index: LinkIndex): Long

    @Update
    suspend fun updateIndex(index: LinkIndex)

    @Delete
    suspend fun deleteIndex(index: LinkIndex)
}
