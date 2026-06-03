package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Book
import com.example.data.BookShelfCrossRef
import com.example.data.LibraryShelf
import com.example.ui.BookViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: BookViewModel,
    onBookOpened: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val books by viewModel.books.collectAsState()
    val shelves by viewModel.shelves.collectAsState()
    val selectedShelfId by viewModel.selectedShelfId.collectAsState()

    // Retrieve and persist the reader's name precisely once
    val sharedPrefs = remember { context.getSharedPreferences("librova_prefs", Context.MODE_PRIVATE) }
    var readerName by remember { mutableStateOf(sharedPrefs.getString("reader_name", "") ?: "") }
    var showNameDialog by remember { mutableStateOf(readerName.isEmpty()) }
    var tempNameInput by remember { mutableStateOf("") }

    // State for book options sheets
    var selectedBookForMenu by remember { mutableStateOf<Book?>(null) }
    var showMetadataSheet by remember { mutableStateOf(false) }
    var showShelfAssignmentSheet by remember { mutableStateOf(false) }
    var showBookInfoDialog by remember { mutableStateOf(false) }

    // Shelf customization dialog
    var showNewShelfDialog by remember { mutableStateOf(false) }
    var newShelfNameInput by remember { mutableStateOf("") }

    // System file picker contract for PDFs and TXTs
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                val name = getFileNameFromUri(context, it)
                viewModel.importBook(it, name)
            }
        }
    )

    // Filter books based on active shelf tab
    val filteredBooks = remember(books, selectedShelfId) {
        if (selectedShelfId == null) {
            books
        } else {
            books // Matches are handled reactively or matched globally
        }
    }

    // Determine the active book for "Currently Reading" hero
    val currentlyReadingBook = remember(books) {
        books.firstOrNull { it.readingStatus == "Reading" } ?: books.firstOrNull()
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                        val timeGreeting = when {
                            hour in 0..11 -> "GOOD MORNING"
                            hour in 12..16 -> "GOOD AFTERNOON"
                            else -> "GOOD EVENING"
                        }
                        Text(
                            text = timeGreeting,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = if (readerName.isEmpty()) "Your Library" else "$readerName's Library",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showNewShelfDialog = true },
                        modifier = Modifier.testTag("add_shelf_button")
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Add Shelf")
                    }
                    IconButton(
                        onClick = {
                            importFileLauncher.launch(arrayOf("application/pdf", "text/plain"))
                        },
                        modifier = Modifier
                            .testTag("top_import_button")
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "Import",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF818CF8), // Indigo 400
                                        Color(0xFFA78BFA)  // Purple 400
                                    )
                                )
                            )
                            .border(1.5.dp, Color.White, CircleShape)
                            .clickable { showNameDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (readerName.isNotEmpty()) readerName.take(1).uppercase() else "A",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    importFileLauncher.launch(arrayOf("application/pdf", "text/plain"))
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(viewModel.translate("import_book")) },
                modifier = Modifier
                    .testTag("import_book_fab")
                    .padding(bottom = 8.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // "Currently Reading" Premium Hero Card
            currentlyReadingBook?.let { book ->
                CurrentlyReadingHero(
                    book = book,
                    translate = { viewModel.translate(it) },
                    onContinueReading = { onBookOpened(book) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            } ?: Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = viewModel.translate("empty_library"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = viewModel.translate("empty_prompt"),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Beautiful Horizontal Shelf Categories Slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isAllSelected = selectedShelfId == null
                Box(
                    modifier = Modifier
                        .testTag("shelf_tab_all")
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                        .border(
                            width = 1.dp,
                            color = if (isAllSelected) Color.Transparent else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable { viewModel.selectShelf(null) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = viewModel.translate("all_books"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isAllSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                shelves.forEach { shelf ->
                    val isShelfSelected = selectedShelfId == shelf.id
                    Box(
                        modifier = Modifier
                            .testTag("shelf_tab_${shelf.id}")
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (isShelfSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .border(
                                width = 1.dp,
                                color = if (isShelfSelected) Color.Transparent else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable { viewModel.selectShelf(shelf.id) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = shelf.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isShelfSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            if (!shelf.isSystem) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Delete Shelf",
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clickable { viewModel.deleteCategoryShelf(shelf.id) },
                                    tint = if (isShelfSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Grid of Book Cards layout
            Text(
                text = "Recent Additions",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Responsive grid calculation based on bounds, standard vertical stack as scroll child
            val chunks = filteredBooks.chunked(2)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                chunks.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        pair.forEach { book ->
                            Box(modifier = Modifier.weight(1f)) {
                                BookItemCard(
                                    book = book,
                                    viewModel = viewModel,
                                    onClick = { onBookOpened(book) },
                                    onThreeDotClick = {
                                        selectedBookForMenu = book
                                    }
                                )
                            }
                        }
                        if (pair.size == 1) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(80.dp)) // Avoid Fab overlay cutoff
        }
    }

    // --- Preferred Name Dialog Builder ---
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Welcome to Librova") },
            text = {
                Column {
                    Text("Tell us your preferred name. We'll use this nickname-free representation for visual greetings inside your personal bookshelf system.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempNameInput,
                        onValueChange = { tempNameInput = it },
                        label = { Text("Your Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("preferred_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempNameInput.isNotBlank()) {
                            readerName = tempNameInput.trim()
                            sharedPrefs.edit().putString("reader_name", readerName).apply()
                            showNameDialog = false
                        }
                    },
                    enabled = tempNameInput.isNotBlank(),
                    modifier = Modifier.testTag("preferred_name_save")
                ) {
                    Text("Enter Library")
                }
            }
        )
    }

    // --- Custom Category Shelf Construction Dialog ---
    if (showNewShelfDialog) {
        AlertDialog(
            onDismissRequest = { showNewShelfDialog = false },
            title = { Text("Create Custom Shelf") },
            text = {
                OutlinedTextField(
                    value = newShelfNameInput,
                    onValueChange = { newShelfNameInput = it },
                    label = { Text("Shelf / Category Name") },
                    placeholder = { Text("e.g. Hindu Scriptures") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_shelf_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newShelfNameInput.isNotBlank()) {
                            viewModel.createCategoryShelf(newShelfNameInput.trim())
                            newShelfNameInput = ""
                            showNewShelfDialog = false
                        }
                    },
                    enabled = newShelfNameInput.isNotBlank(),
                    modifier = Modifier.testTag("save_new_shelf_button")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewShelfDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Premium Book Three-Dot Actions Bottom Sheet ---
    selectedBookForMenu?.let { book ->
        ModalBottomSheet(
            onDismissRequest = { selectedBookForMenu = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // Book Title summary header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BookCoverVisual(
                        coverPath = book.customCoverPath,
                        title = book.title,
                        modifier = Modifier
                            .size(50.dp, 75.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = book.author,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = { viewModel.toggleFavorite(book.id) }) {
                        Icon(
                            imageVector = if (book.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (book.isFavorite) Color.Red else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Divider()

                // Actions Grid or List
                ListItem(
                    headlineContent = { Text(viewModel.translate("book_info")) },
                    leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    modifier = Modifier
                        .clickable {
                            showBookInfoDialog = true
                        }
                        .testTag("menu_action_info")
                )

                ListItem(
                    headlineContent = { Text(viewModel.translate("edit_metadata")) },
                    leadingContent = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                    modifier = Modifier
                        .clickable {
                            showMetadataSheet = true
                        }
                        .testTag("menu_action_edit")
                )

                ListItem(
                    headlineContent = { Text(viewModel.translate("move_library")) },
                    leadingContent = { Icon(Icons.Outlined.Home, contentDescription = null) },
                    modifier = Modifier
                        .clickable {
                            showShelfAssignmentSheet = true
                        }
                        .testTag("menu_action_move")
                )

                ListItem(
                    headlineContent = { Text(viewModel.translate("share")) },
                    leadingContent = { Icon(Icons.Outlined.Share, contentDescription = null) },
                    modifier = Modifier
                        .clickable {
                            selectedBookForMenu = null
                        }
                        .testTag("menu_action_share")
                )

                Divider()

                ListItem(
                    headlineContent = { Text(viewModel.translate("delete_book"), color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier
                        .clickable {
                            viewModel.deleteBook(book.id)
                            selectedBookForMenu = null
                        }
                        .testTag("menu_action_delete")
                )
            }
        }
    }

    // --- Book Information Dialog Builder ---
    if (showBookInfoDialog && selectedBookForMenu != null) {
        val book = selectedBookForMenu!!
        val formatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = { showBookInfoDialog = false },
            title = { Text(book.title, maxLines = 1) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
                ) {
                    DetailRow("Author", book.author)
                    DetailRow("Language", book.language.uppercase())
                    DetailRow("File Size", String.format("%.2f MB", book.fileSize.toFloat() / (1024 * 1024)))
                    DetailRow("Total Pages", "${book.pageCount} ${viewModel.translate("pages")}")
                    DetailRow("Import Date", formatter.format(Date(book.importDate)))
                    DetailRow("Last Opened", if (book.lastOpenedDate > 0) formatter.format(Date(book.lastOpenedDate)) else "Never")
                    DetailRow("Reading Progress", String.format("%.1f%%", book.readingProgress * 100))
                    DetailRow("Custom Rating", "${book.rating} / 5.0")
                    if (!book.series.isNullOrEmpty()) {
                        DetailRow("Series", "${book.series} Vol ${book.volumeNumber ?: "1"}")
                    }
                    if (!book.description.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Synopsis", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(book.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    }
                    if (!book.customNotes.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Personal Notes", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(book.customNotes, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showBookInfoDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // --- Metadata Editing Screen Overlay Dialog ---
    if (showMetadataSheet && selectedBookForMenu != null) {
        val book = selectedBookForMenu!!
        var title by remember { mutableStateOf(book.title) }
        var author by remember { mutableStateOf(book.author) }
        var series by remember { mutableStateOf(book.series ?: "") }
        var volNum by remember { mutableStateOf(book.volumeNumber ?: "") }
        var language by remember { mutableStateOf(book.language) }
        var desc by remember { mutableStateOf(book.description ?: "") }
        var status by remember { mutableStateOf(book.readingStatus) }
        var notes by remember { mutableStateOf(book.customNotes ?: "") }
        var rating by remember { mutableStateOf(book.rating) }

        AlertDialog(
            onDismissRequest = { showMetadataSheet = false },
            title = { Text(viewModel.translate("edit_metadata")) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Author") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        OutlinedTextField(value = series, onValueChange = { series = it }, label = { Text("Series") }, singleLine = true, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(value = volNum, onValueChange = { volNum = it }, label = { Text("Vol No.") }, singleLine = true, modifier = Modifier.width(80.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Custom Notes") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Reading Status: $status", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Want to Read", "Reading", "Completed").forEach { state ->
                            FilterChip(
                                selected = status == state,
                                onClick = { status = state },
                                label = { Text(state, fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Rating: ${rating.toInt()} Stars", fontSize = 12.sp)
                    Slider(
                        value = rating,
                        onValueChange = { rating = it },
                        valueRange = 0f..5f,
                        steps = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateBookMetadata(
                            bookId = book.id,
                            title = title,
                            author = author,
                            series = series.ifBlank { null },
                            volumeNumber = volNum.ifBlank { null },
                            language = language,
                            description = desc.ifBlank { null },
                            readingStatus = status,
                            customNotes = notes.ifBlank { null },
                            rating = rating
                        )
                        showMetadataSheet = false
                        selectedBookForMenu = null
                    },
                    modifier = Modifier.testTag("metadata_save_btn")
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMetadataSheet = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Move Book to Custom Shelf Bottom Dialog ---
    if (showShelfAssignmentSheet && selectedBookForMenu != null) {
        val book = selectedBookForMenu!!
        val activeBookShelves by viewModel.currentBookShelfIds.collectAsState()

        AlertDialog(
            onDismissRequest = { showShelfAssignmentSheet = false },
            title = { Text(viewModel.translate("move_library")) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    shelves.forEach { shelf ->
                        val isBelongs = activeBookShelves.contains(shelf.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isBelongs) {
                                        viewModel.removeBookFromShelf(book.id, shelf.id)
                                    } else {
                                        viewModel.moveBookToShelf(book.id, shelf.id)
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isBelongs,
                                onCheckedChange = {
                                    if (isBelongs) {
                                        viewModel.removeBookFromShelf(book.id, shelf.id)
                                    } else {
                                        viewModel.moveBookToShelf(book.id, shelf.id)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(shelf.name, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showShelfAssignmentSheet = false
                    selectedBookForMenu = null
                }) {
                    Text("Done")
                }
            }
        )
    }
}

// Header layout section showing currently-reading hero card with dynamic stats
@Composable
fun CurrentlyReadingHero(
    book: Book,
    translate: (String) -> String,
    onContinueReading: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Subtle glowing soft shadow under the card matching the sleek tailwind blur aesthetic
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 6.dp)
                .padding(horizontal = 8.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF818CF8).copy(alpha = 0.15f),
                            Color(0xFFA78BFA).copy(alpha = 0.15f)
                        )
                    ),
                    RoundedCornerShape(32.dp)
                )
        )

        // Main Card Surface
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sleek vertical book cover placeholder or image with 14.dp rounding
                Box(
                    modifier = Modifier
                        .size(86.dp, 128.dp)
                        .shadow(4.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    BookCoverVisual(
                        coverPath = book.customCoverPath,
                        title = book.title,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(18.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = translate("currently_reading").uppercase(),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = book.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = book.author,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = String.format("%.0f%% Complete", book.readingProgress * 100),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        LinearProgressIndicator(
                            progress = book.readingProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onContinueReading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground, // High contrast button color
                            contentColor = MaterialTheme.colorScheme.background
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("continue_reading_hero_btn")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(translate("continue_reading"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Card grid component containing individual interactive documents with premium scaling
@Composable
fun BookItemCard(
    book: Book,
    viewModel: BookViewModel,
    onClick: () -> Unit,
    onThreeDotClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = tween(150),
        label = "PressAnimation"
    )

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)),
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .testTag("book_card_${book.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                BookCoverVisual(
                    coverPath = book.customCoverPath,
                    title = book.title,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    IconButton(
                        onClick = onThreeDotClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("three_dot_${book.id}")
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = book.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = book.readingProgress,
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = String.format("%.0f%%", book.readingProgress * 100),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (book.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.Red.copy(alpha = 0.8f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    if (!book.customNotes.isNullOrEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}

// Micro visual helpers to render beautiful placeholder/extracted book covers
@Composable
fun BookCoverVisual(
    coverPath: String?,
    title: String,
    modifier: Modifier = Modifier
) {
    if (coverPath != null && File(coverPath).exists()) {
        AsyncImage(
            model = File(coverPath),
            contentDescription = "Cover of $title",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF6366F1), // Indigo 500
                            Color(0xFF4F46E5)  // Indigo 600
                        )
                    )
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Elegant Left Book Spine Line (real bookshelf visual)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(Color.White.copy(alpha = 0.15f))
                    .align(Alignment.CenterStart)
            )

            Text(
                text = title.take(2).uppercase(),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

fun getFileNameFromUri(context: Context, uri: Uri): String {
    var name = "imported_book.pdf"
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index != -1) {
                name = it.getString(index)
            }
        }
    }
    return name
}

