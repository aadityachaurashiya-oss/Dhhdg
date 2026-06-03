package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "highlights")
data class Highlight(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val bookId: String,
    val pageNumber: Int,
    val snippetText: String,
    val colorHex: String, // Yellow (#FFEB3B), Blue (#2196F3), Green (#4CAF50), Red (#F44336)
    val note: String? = null,
    val createdDate: Long = System.currentTimeMillis(),
    val chapter: String? = null,
    val type: String = "highlight", // highlight or underline
    // Selection coordinate helpers for rendering custom overlay highlighters
    val startCharOffset: Int = 0,
    val endCharOffset: Int = 0
)
