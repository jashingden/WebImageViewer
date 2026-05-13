package com.eddy.webcrawler.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "linkindex",
    indices = [Index(value = ["sourceUrl"], unique = true)],
    foreignKeys = []
)
data class LinkIndex(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceUrl: String,
    val filterPattern: String,
    val title: String,
    val crawlTimestamp: Long,
    val status: String,
    val errorMessage: String? = null
)
