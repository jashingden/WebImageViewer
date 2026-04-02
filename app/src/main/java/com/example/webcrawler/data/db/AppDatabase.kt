package com.example.webcrawler.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.webcrawler.data.model.LinkEntry
import com.example.webcrawler.data.model.LinkIndex
import java.util.Date

@TypeConverters(Converters::class)
@Database(
    entities = [LinkIndex::class, LinkEntry::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun linkIndexDao(): LinkIndexDao
    abstract fun linkEntryDao(): LinkEntryDao
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}
