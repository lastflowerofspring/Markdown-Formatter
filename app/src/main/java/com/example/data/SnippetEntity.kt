package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "formatted_snippets")
data class SnippetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val previewText: String,
    val wordCount: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val sourceName: String = "Manual Input"
)
