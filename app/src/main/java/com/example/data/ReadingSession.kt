package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "reading_sessions")
data class ReadingSession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val bookId: String,
    val startTime: Long,
    val durationMs: Long,
    val pagesRead: Int = 0
)
