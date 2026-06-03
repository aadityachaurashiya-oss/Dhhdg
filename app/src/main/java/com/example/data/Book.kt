package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val author: String,
    val series: String? = null,
    val volumeNumber: String? = null,
    val language: String = "en", // English, Nepali, Hindi, Bhojpuri, Maithili
    val description: String? = null,
    val filePath: String? = null, // Path to local stored file
    val fileUriString: String? = null,
    val fileSize: Long = 0L,
    val pageCount: Int = 0,
    val importDate: Long = System.currentTimeMillis(),
    val lastOpenedDate: Long = System.currentTimeMillis(),
    val readingProgress: Float = 0f, // 0.0 to 1.0
    val currentPage: Int = 0, // 0-indexed page number
    val scrollOffset: Int = 0,
    val estimatedReadingTimeSeconds: Long = 0L,
    val customNotes: String? = null,
    val isFavorite: Boolean = false,
    val readingStatus: String = "Want to Read", // Reading, Completed, Want to Read
    val customCoverPath: String? = null, // Extracted bitmap or custom image
    val rating: Float = 0f,
    // Typography settings
    val fontFamily: String = "System", // System, Inter, Geist, SF Pro, IBM Plex Serif
    val fontSizeSp: Float = 18f,
    val lineSpacingMultiplier: Float = 1.4f,
    val paragraphSpacingDp: Float = 14f,
    val pageMarginsDp: Float = 20f,
    val textAlignment: Int = 3, // 1: Left, 3: Justify
    // Reading settings
    val pageFlippingMode: String = "Vertical Scroll", // Vertical Scroll, Horizontal Scroll, Page-by-Page
    val singleColumnMode: Boolean = true,
    val landscapeDualPage: Boolean = false,
    val pageMarginsVisible: Boolean = true,
    val textDirectionRtl: Boolean = false,
    val zoomLevel: Float = 1.0f
)
