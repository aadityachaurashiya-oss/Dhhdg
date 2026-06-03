package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    // --- Book Operations ---
    @Query("SELECT * FROM books ORDER BY lastOpenedDate DESC")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    fun getBookByIdFlow(id: String): Flow<Book?>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getBookById(id: String): Book?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book)

    @Delete
    suspend fun deleteBook(book: Book)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: String)

    // --- Shelf Operations ---
    @Query("SELECT * FROM shelves ORDER BY isSystem DESC, name ASC")
    fun getAllShelves(): Flow<List<LibraryShelf>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShelf(shelf: LibraryShelf)

    @Delete
    suspend fun deleteShelf(shelf: LibraryShelf)

    // --- Book Shelf Mappings ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookShelfCrossRef(crossRef: BookShelfCrossRef)

    @Query("DELETE FROM book_shelf_cross_ref WHERE bookId = :bookId")
    suspend fun deleteCrossRefsForBook(bookId: String)

    @Query("DELETE FROM book_shelf_cross_ref WHERE bookId = :bookId AND shelfId = :shelfId")
    suspend fun deleteCrossRef(bookId: String, shelfId: String)

    @Query("""
        SELECT b.* FROM books b 
        INNER JOIN book_shelf_cross_ref r ON b.id = r.bookId 
        WHERE r.shelfId = :shelfId
    """)
    fun getBooksForShelf(shelfId: String): Flow<List<Book>>

    @Query("SELECT shelfId FROM book_shelf_cross_ref WHERE bookId = :bookId")
    fun getBookShelfIds(bookId: String): Flow<List<String>>

    // --- Highlight (Knowledge Database) Operations ---
    @Query("SELECT * FROM highlights ORDER BY createdDate DESC")
    fun getAllHighlights(): Flow<List<Highlight>>

    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY pageNumber ASC, createdDate ASC")
    fun getHighlightsForBook(bookId: String): Flow<List<Highlight>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: Highlight)

    @Delete
    suspend fun deleteHighlight(highlight: Highlight)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlightById(id: String)

    // --- Bookmark Operations ---
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY pageNumber ASC")
    fun getBookmarksForBook(bookId: String): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId AND pageNumber = :pageNumber")
    suspend fun deleteBookmarkAtPage(bookId: String, pageNumber: Int)

    // --- Reading Session Operations (Analytics) ---
    @Query("SELECT * FROM reading_sessions ORDER BY startTime DESC")
    fun getAllReadingSessions(): Flow<List<ReadingSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingSession(session: ReadingSession)
}
