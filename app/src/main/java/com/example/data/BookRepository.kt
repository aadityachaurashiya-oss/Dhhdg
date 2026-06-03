package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class BookRepository(
    private val context: Context,
    private val bookDao: BookDao
) {
    val allBooks: Flow<List<Book>> = bookDao.getAllBooks()
    val allShelves: Flow<List<LibraryShelf>> = bookDao.getAllShelves()
    val allHighlights: Flow<List<Highlight>> = bookDao.getAllHighlights()
    val allSessions: Flow<List<ReadingSession>> = bookDao.getAllReadingSessions()

    suspend fun getBookByIdSync(id: String): Book? = bookDao.getBookById(id)
    fun getBookByIdFlow(id: String): Flow<Book?> = bookDao.getBookByIdFlow(id)

    suspend fun insertBook(book: Book) = bookDao.insertBook(book)
    suspend fun deleteBookById(id: String) = bookDao.deleteBookById(id)

    suspend fun insertShelf(shelf: LibraryShelf) = bookDao.insertShelf(shelf)
    suspend fun deleteShelf(shelf: LibraryShelf) = bookDao.deleteShelf(shelf)

    suspend fun insertBookShelfCrossRef(crossRef: BookShelfCrossRef) = bookDao.insertBookShelfCrossRef(crossRef)
    suspend fun deleteCrossRef(bookId: String, shelfId: String) = bookDao.deleteCrossRef(bookId, shelfId)
    suspend fun deleteCrossRefsForBook(bookId: String) = bookDao.deleteCrossRefsForBook(bookId)
    fun getBooksForShelf(shelfId: String): Flow<List<Book>> = bookDao.getBooksForShelf(shelfId)
    fun getBookShelfIds(bookId: String): Flow<List<String>> = bookDao.getBookShelfIds(bookId)

    fun getHighlightsForBook(bookId: String): Flow<List<Highlight>> = bookDao.getHighlightsForBook(bookId)
    suspend fun insertHighlight(highlight: Highlight) = bookDao.insertHighlight(highlight)
    suspend fun deleteHighlightById(id: String) = bookDao.deleteHighlightById(id)

    fun getBookmarksForBook(bookId: String): Flow<List<Bookmark>> = bookDao.getBookmarksForBook(bookId)
    suspend fun insertBookmark(bookmark: Bookmark) = bookDao.insertBookmark(bookmark)
    suspend fun deleteBookmarkAtPage(bookId: String, pageNumber: Int) = bookDao.deleteBookmarkAtPage(bookId, pageNumber)

    suspend fun insertReadingSession(session: ReadingSession) = bookDao.insertReadingSession(session)

    // Seed default collections if none exist
    suspend fun prepopulateShelvesIfNeeded() {
        val shelves = allShelves.first()
        if (shelves.isEmpty()) {
            val defaults = listOf(
                "Philosophy",
                "Science",
                "Fiction",
                "Hindu Scriptures",
                "Research Papers",
                "History",
                "Personal Documents"
            )
            for (name in defaults) {
                bookDao.insertShelf(LibraryShelf(name = name, isSystem = true))
            }
        }
    }

    /**
     * Import a PDF or TXT file into our local secure structure.
     * Extracts title/metadata, computes total pages, and renders the cover image natively.
     */
    suspend fun importBookFile(uri: Uri, displayName: String?): Book = withContext(Dispatchers.IO) {
        val resolvedName = displayName ?: uri.lastPathSegment ?: "Imported_Book_${System.currentTimeMillis()}"
        val sanitizedName = resolvedName.replace(".pdf", "", ignoreCase = true)
            .replace(".txt", "", ignoreCase = true)
            .replace("_", " ")

        // Make local copy inside internal files/books folder
        val booksDir = File(context.filesDir, "books")
        if (!booksDir.exists()) booksDir.mkdirs()

        val uniqueId = UUID.randomUUID().toString()
        val suffix = if (resolvedName.lowercase().endsWith(".txt")) ".txt" else ".pdf"
        val localBookFile = File(booksDir, "$uniqueId$suffix")

        var fileLength = 0L
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(localBookFile).use { output ->
                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    fileLength += bytesRead
                }
            }
        }

        var pages = 1
        var estReadingTime = 0L
        var coverPath: String? = null

        if (suffix == ".pdf") {
            try {
                val pfd = ParcelFileDescriptor.open(localBookFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val pdfRenderer = PdfRenderer(pfd)
                pages = pdfRenderer.pageCount
                // 150 seconds per page average reading speed
                estReadingTime = (pages * 150).toLong()

                // Generate Cover Image from Page 0
                if (pages > 0) {
                    val coverDir = File(context.filesDir, "covers")
                    if (!coverDir.exists()) coverDir.mkdirs()
                    val coverFile = File(coverDir, "$uniqueId.jpg")

                    val page0 = pdfRenderer.openPage(0)
                    // Scale down rendering size to optimize heap memory usage (e.g. max width 400px)
                    val scale = 400f / page0.width.coerceAtLeast(1)
                    val targetWidth = 400
                    val targetHeight = (page0.height * scale).toInt().coerceAtLeast(1)

                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    page0.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    FileOutputStream(coverFile).use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                    }
                    coverPath = coverFile.absolutePath
                    page0.close()
                }
                pdfRenderer.close()
                pfd.close()
            } catch (e: Exception) {
                Log.e("BookRepository", "Failed to extract PDF details dynamically, creating placeholder cover", e)
                coverPath = generatePdfPlaceholderCover(uniqueId, sanitizedName)
            }
        } else {
            // Text files
            try {
                val lines = localBookFile.readLines()
                val charCount = lines.sumOf { it.length }
                pages = (charCount / 1200).coerceAtLeast(1)
                estReadingTime = (pages * 120).toLong()
                coverPath = generatePdfPlaceholderCover(uniqueId, sanitizedName)
            } catch (e: Exception) {
                pages = 1
                coverPath = generatePdfPlaceholderCover(uniqueId, sanitizedName)
            }
        }

        val authorName = "Unknown Author"

        val importedBook = Book(
            id = uniqueId,
            title = sanitizedName,
            author = authorName,
            filePath = localBookFile.absolutePath,
            fileUriString = uri.toString(),
            fileSize = fileLength,
            pageCount = pages,
            readingStatus = "Want to Read",
            customCoverPath = coverPath,
            estimatedReadingTimeSeconds = estReadingTime,
            language = "en"
        )

        bookDao.insertBook(importedBook)
        return@withContext importedBook
    }

    /**
     * Fallback cover builder if no PDF pages can be physically extracted or for TXT documents.
     */
    fun generatePdfPlaceholderCover(id: String, title: String): String {
        val coverDir = File(context.filesDir, "covers")
        if (!coverDir.exists()) coverDir.mkdirs()
        val file = File(coverDir, "$id.jpg")

        val width = 300
        val height = 450
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Generate gorgeous styled slate gradient background
        val paint = Paint().apply {
            isAntiAlias = true
        }
        canvas.drawColor(Color.parseColor("#1E293B")) // Slate 800 deep charcoal

        // Subtle decorative borders
        paint.color = Color.parseColor("#334155")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 12f
        canvas.drawRect(6f, 6f, width.toFloat() - 6f, height.toFloat() - 6f, paint)

        // Draw title typography in warm white
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 24f
        paint.textAlign = Paint.Align.CENTER

        val textLines = if (title.length > 25) {
            listOf(title.substring(0, 20) + "...", "Digital Book")
        } else {
            listOf(title)
        }

        var startY = height / 2f - 20f
        for (line in textLines) {
            canvas.drawText(line, width / 2f, startY, paint)
            startY += 32f
        }

        // Draw small subtle accent logo
        paint.color = Color.parseColor("#6366F1") // Violet indigo
        canvas.drawCircle(width / 2f, height - 80f, 16f, paint)

        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            return file.absolutePath
        } catch (e: Exception) {
            Log.e("BookRepository", "Failed writing placeholder cover bitmap", e)
            return ""
        }
    }
}
