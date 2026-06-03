package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class BookViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val bookDao = db.bookDao()
    private val repository = BookRepository(application, bookDao)

    // Language setting, decoupled from system locale for instant in-app hot swapping
    private val _currentLanguage = MutableStateFlow("en")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    // App-wide Theme setting
    private val _currentThemeName = MutableStateFlow("Follow Device")
    val currentThemeName: StateFlow<String> = _currentThemeName.asStateFlow()

    fun setThemeName(name: String) {
        _currentThemeName.value = name
    }

    // Base UI States
    val books: StateFlow<List<Book>> = repository.allBooks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val shelves: StateFlow<List<LibraryShelf>> = repository.allShelves.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val highlights: StateFlow<List<Highlight>> = repository.allHighlights.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val sessions: StateFlow<List<ReadingSession>> = repository.allSessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Active reading book and active reader settings state
    private val _currentBook = MutableStateFlow<Book?>(null)
    val currentBook: StateFlow<Book?> = _currentBook.asStateFlow()

    val currentBookShelfIds: StateFlow<List<String>> = _currentBook
        .flatMapLatest { book ->
            if (book != null) repository.getBookShelfIds(book.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val highlightsForCurrentBook: StateFlow<List<Highlight>> = _currentBook
        .flatMapLatest { book ->
            if (book != null) repository.getHighlightsForBook(book.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarksForCurrentBook: StateFlow<List<Bookmark>> = _currentBook
        .flatMapLatest { book ->
            if (book != null) repository.getBookmarksForBook(book.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search text query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Active list filters
    private val _selectedShelfId = MutableStateFlow<String?>(null)
    val selectedShelfId: StateFlow<String?> = _selectedShelfId.asStateFlow()

    init {
        viewModelScope.launch {
            repository.prepopulateShelvesIfNeeded()
            preloadSampleBooksIfNeeded()
        }
    }

    fun setLanguage(lang: String) {
        _currentLanguage.value = lang
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectShelf(shelfId: String?) {
        _selectedShelfId.value = shelfId
    }

    // --- Book Reading Session Management ---
    private var activeSessionStartTime: Long = 0L

    fun openBook(book: Book) {
        viewModelScope.launch {
            // Record opening date
            val updatedBook = book.copy(lastOpenedDate = System.currentTimeMillis())
            repository.insertBook(updatedBook)
            _currentBook.value = updatedBook
            activeSessionStartTime = System.currentTimeMillis()
        }
    }

    fun closeBook() {
        val startTime = activeSessionStartTime
        val curr = _currentBook.value
        if (curr != null && startTime > 0) {
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            if (duration > 2000) { // Log sessions longer than 2 seconds
                viewModelScope.launch {
                    repository.insertReadingSession(
                        ReadingSession(
                            bookId = curr.id,
                            startTime = startTime,
                            durationMs = duration,
                            pagesRead = 1 // Simplified increment
                        )
                    )
                }
            }
        }
        _currentBook.value = null
        activeSessionStartTime = 0L
    }

    fun updateBookProgress(page: Int, progress: Float) {
        val curr = _currentBook.value ?: return
        val pagesCount = curr.pageCount.coerceAtLeast(1)
        val readingStat = if (progress >= 0.95f) "Completed" else "Reading"
        val updated = curr.copy(
            currentPage = page,
            readingProgress = progress,
            readingStatus = readingStat,
            lastOpenedDate = System.currentTimeMillis()
        )
        _currentBook.value = updated
        viewModelScope.launch {
            repository.insertBook(updated)
        }
    }

    fun saveTypographySettings(
        fontFamily: String,
        fontSizeSp: Float,
        lineSpacing: Float,
        paragraphSpacing: Float,
        pageMargins: Float,
        alignment: Int
    ) {
        val curr = _currentBook.value ?: return
        val updated = curr.copy(
            fontFamily = fontFamily,
            fontSizeSp = fontSizeSp,
            lineSpacingMultiplier = lineSpacing,
            paragraphSpacingDp = paragraphSpacing,
            pageMarginsDp = pageMargins,
            textAlignment = alignment
        )
        _currentBook.value = updated
        viewModelScope.launch {
            repository.insertBook(updated)
        }
    }

    fun saveReaderSettings(
        pageFlippingMode: String,
        singleColumnMode: Boolean,
        landscapeDualPage: Boolean,
        pageMarginsVisible: Boolean,
        textDirectionRtl: Boolean,
        zoomLevel: Float
    ) {
        val curr = _currentBook.value ?: return
        val updated = curr.copy(
            pageFlippingMode = pageFlippingMode,
            singleColumnMode = singleColumnMode,
            landscapeDualPage = landscapeDualPage,
            pageMarginsVisible = pageMarginsVisible,
            textDirectionRtl = textDirectionRtl,
            zoomLevel = zoomLevel
        )
        _currentBook.value = updated
        viewModelScope.launch {
            repository.insertBook(updated)
        }
    }

    // --- Book Management ---
    fun importBook(uri: Uri, displayName: String?) {
        viewModelScope.launch {
            try {
                repository.importBookFile(uri, displayName)
            } catch (e: Exception) {
                Log.e("BookViewModel", "Failed to import e-book", e)
            }
        }
    }

    fun updateBookMetadata(
        bookId: String,
        title: String,
        author: String,
        series: String?,
        volumeNumber: String?,
        language: String,
        description: String?,
        readingStatus: String,
        customNotes: String?,
        rating: Float
    ) {
        viewModelScope.launch {
            val existing = repository.getBookByIdSync(bookId) ?: return@launch
            val updated = existing.copy(
                title = title,
                author = author,
                series = series,
                volumeNumber = volumeNumber,
                language = language,
                description = description,
                readingStatus = readingStatus,
                customNotes = customNotes,
                rating = rating
            )
            repository.insertBook(updated)
            if (_currentBook.value?.id == bookId) {
                _currentBook.value = updated
            }
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            val book = repository.getBookByIdSync(bookId)
            if (book != null) {
                // Delete files
                book.filePath?.let { File(it).delete() }
                book.customCoverPath?.let { File(it).delete() }
                repository.deleteBookById(bookId)
                repository.deleteCrossRefsForBook(bookId)
                if (_currentBook.value?.id == bookId) {
                    _currentBook.value = null
                }
            }
        }
    }

    fun toggleFavorite(bookId: String) {
        viewModelScope.launch {
            val existing = repository.getBookByIdSync(bookId) ?: return@launch
            val updated = existing.copy(isFavorite = !existing.isFavorite)
            repository.insertBook(updated)
        }
    }

    fun moveBookToShelf(bookId: String, shelfId: String) {
        viewModelScope.launch {
            repository.insertBookShelfCrossRef(BookShelfCrossRef(bookId, shelfId))
        }
    }

    fun removeBookFromShelf(bookId: String, shelfId: String) {
        viewModelScope.launch {
            repository.deleteCrossRef(bookId, shelfId)
        }
    }

    fun createCategoryShelf(name: String) {
        viewModelScope.launch {
            repository.insertShelf(LibraryShelf(name = name, isSystem = false))
        }
    }

    fun deleteCategoryShelf(shelfId: String) {
        viewModelScope.launch {
            db.bookDao().deleteShelf(LibraryShelf(id = shelfId, name = ""))
        }
    }

    // --- Annotation Highlights ---
    fun addHighlight(pageNumber: Int, snippet: String, colorHex: String, type: String = "highlight", note: String? = null) {
        val curr = _currentBook.value ?: return
        viewModelScope.launch {
            val annotation = Highlight(
                bookId = curr.id,
                pageNumber = pageNumber,
                snippetText = snippet,
                colorHex = colorHex,
                type = type,
                note = note
            )
            repository.insertHighlight(annotation)
        }
    }

    fun deleteHighlight(id: String) {
        viewModelScope.launch {
            repository.deleteHighlightById(id)
        }
    }

    // --- Bookmarks ---
    fun toggleBookmark(pageNumber: Int, chapterName: String? = null) {
        val curr = _currentBook.value ?: return
        viewModelScope.launch {
            val existing = bookmarksForCurrentBook.value.firstOrNull { it.pageNumber == pageNumber }
            if (existing != null) {
                repository.deleteBookmarkAtPage(curr.id, pageNumber)
            } else {
                repository.insertBookmark(
                    Bookmark(
                        bookId = curr.id,
                        pageNumber = pageNumber,
                        chapterName = chapterName
                    )
                )
            }
        }
    }

    // --- Localized Dictionary Maps ---
    private val translations = mapOf(
        "en" to mapOf(
            "app_title" to "Librova",
            "tagline" to "Library & Knowledge Hub",
            "library" to "Library Shelf",
            "search" to "Search Content",
            "notes" to "Knowledge Hub",
            "statistics" to "Analytics",
            "settings" to "Settings Hub",
            "currently_reading" to "Currently Reading",
            "continue_reading" to "Continue Reading",
            "recent_added" to "Recently Added Books",
            "import_book" to "Import Document",
            "import_desc" to "Drop PDF or TXT to add to your bookshelf",
            "bookshelf" to "Aesthetic Bookshelf",
            "pages" to "pages",
            "completed" to "Completed",
            "favorites" to "My Favorites",
            "want_to_read" to "Want to Read",
            "all_books" to "All Shelves",
            "notes_count" to "notes written",
            "hours_count" to "hours read",
            "empty_library" to "Your Shelf is Empty",
            "empty_prompt" to "Tap the import button to safely copy PDF/TXT files here.",
            "edit_metadata" to "Edit Book Information",
            "book_info" to "Book Information",
            "delete_book" to "Delete from Library",
            "share" to "Export / Share File",
            "move_library" to "Organize to Shelf",
            "custom_notes" to "Personal annotations",
            "saved_notes" to "Knowledge Bank Notes",
            "analytics_title" to "Progress Dashboard",
            "reading_streak" to "Active Streak",
            "longest_streak" to "Longest Streak",
            "total_pages_read" to "Indexed Pages Read",
            "highlights_saved" to "Saved Highlights",
            "average_speed" to "Est. Speed"
        ),
        "ne" to mapOf(
            "app_title" to "लिब्रोवा",
            "tagline" to "पुस्तकालय र ज्ञान केन्द्र",
            "library" to "मेरो दराज",
            "search" to "खोज्नुहोस्",
            "notes" to "ज्ञान केन्द्र",
            "statistics" to "तथ्याङ्क",
            "settings" to "सेटिङहरू",
            "currently_reading" to "पढिरहेको किताब",
            "continue_reading" to "पढ्न जारी राख्नुहोस्",
            "recent_added" to "हालै थपिएका पुस्तकहरू",
            "import_book" to "फाइल आयात गर्नुहोस्",
            "import_desc" to "आफ्नो दराजमा PDF वा TXT थप्नुहोस्",
            "bookshelf" to "पुस्तक दराज",
            "pages" to "पृष्ठहरू",
            "completed" to "पढिसकिएको",
            "favorites" to "मनपर्ने",
            "want_to_read" to "पढ्ने इच्छा",
            "all_books" to "सबै पुस्तकहरू",
            "notes_count" to "नोटहरू",
            "hours_count" to "घण्टा पढेको",
            "empty_library" to "दराज खाली छ",
            "empty_prompt" to "आफ्नो PDF/TXT पुस्तक आयात गर्न माथिको बटन थिच्नुहोस्।",
            "edit_metadata" to "पुस्तकको विवरण सम्पादन गर्नुहोस्",
            "book_info" to "पुस्तक विवरण",
            "delete_book" to "पुस्तकालयबाट हटाउनुहोस्",
            "share" to "साझेदारी गर्नुहोस्",
            "move_library" to "दराजमा सार्नुहोस्",
            "custom_notes" to "व्यक्तिगत टिप्पणी",
            "saved_notes" to "ज्ञान भण्डार",
            "analytics_title" to "प्रगति विवरण",
            "reading_streak" to "सक्रिय दिन",
            "longest_streak" to "अधिकतम सक्रियता",
            "total_pages_read" to "कुल पठित पृष्ठ",
            "highlights_saved" to "बचत गरिएका हाइलाइट",
            "average_speed" to "पढाइ गति"
        ),
        "hi" to mapOf(
            "app_title" to "लिब्रोवा",
            "tagline" to "पुस्तकालय और ज्ञान केंद्र",
            "library" to "मेरी अलमारी",
            "search" to "खोज केंद्र",
            "notes" to "ज्ञान संग्रह",
            "statistics" to "आंकड़े",
            "settings" to "सेटिंग्स",
            "currently_reading" to "वर्तमान में पढ़ रहे हैं",
            "continue_reading" to "पढ़ना जारी रखें",
            "recent_added" to "हाल ही में जोड़ी गई पुस्तकें",
            "import_book" to "किताब जोड़ें",
            "import_desc" to "अपनी अलमारी में PDF या TXT फाइल जोड़ें",
            "bookshelf" to "सजावटी बुकशेल्फ़",
            "pages" to "पृष्ठ",
            "completed" to "पूर्ण",
            "favorites" to "पसंदीदा",
            "want_to_read" to "पढ़ना चाहते हैं",
            "all_books" to "सभी अलमारियां",
            "notes_count" to "नोट बनाए गए",
            "hours_count" to "घंटे पढ़ा",
            "empty_library" to "आपकी अलमारी खाली है",
            "empty_prompt" to "PDF या TXT पुस्तक मंगाने के लिए इमपोर्ट बटन दबाएं।",
            "edit_metadata" to "पुस्तक विवरण बदलें",
            "book_info" to "पुस्तक जानकारी",
            "delete_book" to "लाइब्रेरी से हटाएं",
            "share" to "साझा करें",
            "move_library" to "अलमारी में सहेजें",
            "custom_notes" to "व्यक्तिगत नोट्स",
            "saved_notes" to "ज्ञान केंद्र के नोट्स",
            "analytics_title" to "प्रगति डैशबोर्ड",
            "reading_streak" to "सक्रिय सिलसिला",
            "longest_streak" to "उच्चतम रिकॉर्ड",
            "total_pages_read" to "कुल पढ़े गए पन्ने",
            "highlights_saved" to "सुरक्षित हाइलाइट्स",
            "average_speed" to "अनुमानित गति"
        ),
        "bho" to mapOf(
            "app_title" to "लिब्रोवा",
            "tagline" to "पुस्तकालय अउर ज्ञान केंद्र",
            "library" to "हमार अल्मारी",
            "search" to "खोजल जाव",
            "notes" to "ज्ञान संग्रह",
            "statistics" to "आंकड़ा",
            "settings" to "सेटिंग",
            "currently_reading" to "अभी पढ़त बानी",
            "continue_reading" to "पढ़ल चालू रखीं",
            "recent_added" to "नया जुड़ल किताब",
            "import_book" to "किताब मँगाईं",
            "import_desc" to "अल्मारी में PDF भा TXT किताब जोड़ीं",
            "bookshelf" to "हमार बुकशेल्फ़",
            "pages" to "पन्ना",
            "completed" to "पढ़ लिखल",
            "favorites" to "पसंदीदा",
            "want_to_read" to "पढ़े के मन बा",
            "all_books" to "सभ किताब",
            "notes_count" to "नोट लिखल बा",
            "hours_count" to "घंटा पढ़ल",
            "empty_library" to "अल्मारी खाली बा",
            "empty_prompt" to "किताब मँगावे खातिर ऊपर बटन दबाईं।",
            "edit_metadata" to "किताब के जानकारी सम्पादन",
            "book_info" to "किताब के जानकारी",
            "delete_book" to "किताब हटाईं",
            "share" to "शेयर करीं",
            "move_library" to "अल्मारी में सँभारीं",
            "custom_notes" to "हमार विचार",
            "saved_notes" to "ज्ञान कोठरी",
            "analytics_title" to "प्रगति कोठरी",
            "reading_streak" to "सक्रिय सिलसिला",
            "longest_streak" to "सबसे बड़ सिलसिला",
            "total_pages_read" to "कुल पढ़ल पन्ना",
            "highlights_saved" to "बचवल हाइलाइट",
            "average_speed" to "पढ़ाई के रफ्तार"
        ),
        "mai" to mapOf(
            "app_title" to "लिब्रोवा",
            "tagline" to "निजी पुस्तकालय एवं ज्ञान संग्रह",
            "library" to "हमार अलमारी",
            "search" to "खोजू",
            "notes" to "ज्ञान कोठी",
            "statistics" to "विवरण",
            "settings" to "सेटिंग",
            "currently_reading" to "एखन पढ़ि रहल छी",
            "continue_reading" to "पढ़ब जारी राखू",
            "recent_added" to "नव जोड़ल गेल किताब",
            "import_book" to "किताब आनी",
            "import_desc" to "अपन दराज में PDF वा TXT जोड़ू",
            "bookshelf" to "शृंगारित दराज",
            "pages" to "पृष्ठ",
            "completed" to "पढ़ल गेल",
            "favorites" to "मनपसन्द",
            "want_to_read" to "पढ़बाक इच्छा",
            "all_books" to "सभटा किताब",
            "notes_count" to "लिखल विचार",
            "hours_count" to "घंटा पढ़बाक",
            "empty_library" to "अलमारी खाली अछि",
            "empty_prompt" to "किताब आयात करबाक लेल ऊपर दबाउ।",
            "edit_metadata" to "विवरण सुधहरू",
            "book_info" to "किताबक परिचय",
            "delete_book" to "अलमारी सँ हटाउ",
            "share" to "साझा करू",
            "move_library" to "दराज में राखू",
            "custom_notes" to "हमार विचार",
            "saved_notes" to "ज्ञानक कोठी",
            "analytics_title" to "प्रगति बोर्ड",
            "reading_streak" to "सक्रियता",
            "longest_streak" to "उच्चतम रिकॉर्ड",
            "total_pages_read" to "कुल पठित पृष्ठ",
            "highlights_saved" to "सुरक्षित हाइलाइट",
            "average_speed" to "पढ़बाक गति"
        )
    )

    fun translate(key: String): String {
        val lang = _currentLanguage.value
        val dict = translations[lang] ?: translations["en"]!!
        return dict[key] ?: translations["en"]?.get(key) ?: key
    }

    // --- Preload simulated books for a flawless out-of-the-box user experience ---
    private suspend fun preloadSampleBooksIfNeeded() = withContext(Dispatchers.IO) {
        val existing = bookDao.getAllBooks().first()
        if (existing.isEmpty()) {
            val booksDir = File(getApplication<Application>().filesDir, "books")
            if (!booksDir.exists()) booksDir.mkdirs()

            val samples = listOf(
                Triple("The Republic by Plato", "Plato", "A fundamental dialogue in political philosophy outlining justice, the theory of forms, and the perfect city-state."),
                Triple("Meditations", "Marcus Aurelius", "Private Stoic philosophical reflections on nature, morality, self-discipline, and enduring hardship."),
                Triple("Introduction to Science", "Francis Bacon", "Scientific method overview outlining logic, empirical observation, and inductive reasoning for research."),
                Triple("Siddhartha", "Hermann Hesse", "A beautifully written novel exploring the spiritual journey of self-discovery during the rise of Buddhism.")
            )

            for ((title, author, desc) in samples) {
                val fileId = UUID.randomUUID().toString()
                val localFile = File(booksDir, "$fileId.txt")
                
                // Write rich dummy text pages to simulate loading a book
                val textBuilder = StringBuilder()
                textBuilder.append("--- CHAPTER 1: THE BEGINNING ---\n\n")
                textBuilder.append("This is an elegant preloaded version of $title written by $author. Let this be your starting point on an intellectual journey.\n\n")
                textBuilder.append("$desc\n\n")
                textBuilder.append("Reading is of the mind. Knowledge is the key that opens the doors of perception, leading to wisdom, freedom, and inner peacefulness.\n\n")
                textBuilder.append("Let us turn the pages of curiosity. In Librova, we study the universe. Think of karma, explore science, analyze philosophy, and preserve definitions.\n\n")
                textBuilder.append("--- CHAPTER 2: ESSENCE OF KNOWLEDGE ---\n\n")
                textBuilder.append("Every thought highlighted, every note drafted, enters a centralized knowledge database. This turns reading into a lifelong companion, an active learning garden rather than a static catalog of papers.\n")
                
                localFile.writeText(textBuilder.toString())

                val coverPath = repository.generatePdfPlaceholderCover(fileId, title)

                val sampleBook = Book(
                    id = fileId,
                    title = title,
                    author = author,
                    filePath = localFile.absolutePath,
                    fileSize = localFile.length(),
                    pageCount = 12, // simulated pages
                    readingStatus = "Want to Read",
                    language = "en",
                    description = desc,
                    customCoverPath = coverPath,
                    estimatedReadingTimeSeconds = 1800L
                )

                bookDao.insertBook(sampleBook)

                // Associate with default shelves directly
                val cat = when (title) {
                    "The Republic by Plato", "Meditations" -> "Philosophy"
                    "Introduction to Science" -> "Science"
                    "Siddhartha" -> "Fiction"
                    else -> "History"
                }
                delay(10)
                val shelfList = repository.allShelves.first()
                val matchingShelf = shelfList.firstOrNull { it.name.lowercase() == cat.lowercase() }
                if (matchingShelf != null) {
                    repository.insertBookShelfCrossRef(BookShelfCrossRef(sampleBook.id, matchingShelf.id))
                }
            }
        }
    }
}

class BookViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
