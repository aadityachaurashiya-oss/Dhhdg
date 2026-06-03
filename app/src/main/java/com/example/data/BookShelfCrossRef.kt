package com.example.data

import androidx.room.Entity

@Entity(tableName = "book_shelf_cross_ref", primaryKeys = ["bookId", "shelfId"])
data class BookShelfCrossRef(
    val bookId: String,
    val shelfId: String
)
