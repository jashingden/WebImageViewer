package com.eddy.webcrawler.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "linkentry",
    indices = [
        Index(value = ["linkIndexId", "url"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = LinkIndex::class,
            parentColumns = ["id"],
            childColumns = ["linkIndexId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LinkEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val linkIndexId: Long,
    val displayName: String,
    val url: String,
    val type: String,
    val fileExtension: String? = null,
    val downloadStatus: String? = null,
    val localPath: String? = null
)
