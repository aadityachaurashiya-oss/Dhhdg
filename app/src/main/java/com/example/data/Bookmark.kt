package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val bookId: String,
    val pageNumber: Int, // 0-indexed page reference
    val chapterName: String? = null,
    val note: String? = null,
    val createdDate: Long = System.currentTimeMillis()
)
