package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Book
import com.example.data.Highlight
import com.example.ui.BookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: BookViewModel,
    onBookOpened: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val books by viewModel.books.collectAsState()
    val highlights by viewModel.highlights.collectAsState()

    var activeFilter by remember { mutableStateOf("All") } // All, Titles, Highlights

    // Perform instant unified search query scanning titles, authors, and highlight snippet text
    val foundBooks = remember(books, searchQuery, activeFilter) {
        if (searchQuery.isBlank() || activeFilter == "Highlights") {
            emptyList()
        } else {
            books.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.author.contains(searchQuery, ignoreCase = true) ||
                (it.description?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    val foundHighlights = remember(highlights, books, searchQuery, activeFilter) {
        if (searchQuery.isBlank() || activeFilter == "Titles") {
            emptyList()
        } else {
            highlights.filter {
                it.snippetText.contains(searchQuery, ignoreCase = true) ||
                (it.note?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.translate("search"), fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Elegant search card with dynamic styling
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by title, author, quotes, or karma...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("global_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Hot Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Titles", "Highlights").forEach { filter ->
                    FilterChip(
                        selected = activeFilter == filter,
                        onClick = { activeFilter = filter },
                        label = { Text(filter) },
                        modifier = Modifier.testTag("search_filter_$filter")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search results list
            if (searchQuery.isBlank()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Type to search instantly",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                        Text(
                            "Scan indexed pages, notes & annotations",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        )
                    }
                }
            } else if (foundBooks.isEmpty() && foundHighlights.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No matches found for \"$searchQuery\"",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Try different keywords",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (foundBooks.isNotEmpty()) {
                        item {
                            Text(
                                "Matching Books (${foundBooks.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }

                        items(foundBooks) { book ->
                            ListItem(
                                headlineContent = { Text(book.title, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text(book.author, fontSize = 13.sp) },
                                leadingContent = {
                                    BookCoverVisual(
                                        coverPath = book.customCoverPath,
                                        title = book.title,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                },
                                trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onBookOpened(book) }
                                    .testTag("search_result_book_${book.id}")
                            )
                        }
                    }

                    if (foundHighlights.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Matching Annotations (${foundHighlights.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }

                        items(foundHighlights) { highlight ->
                            val matchedBook = books.firstOrNull { it.id == highlight.bookId }
                            val resolvedColor = try {
                                Color(android.graphics.Color.parseColor(highlight.colorHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }

                            ListItem(
                                headlineContent = { 
                                    Text(
                                        highlight.snippetText, 
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    ) 
                                },
                                supportingContent = {
                                    val bkTitle = matchedBook?.title ?: "Unknown Book"
                                    Text(
                                        "Page ${highlight.pageNumber + 1} of $bkTitle",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Edit, 
                                        contentDescription = null,
                                        tint = resolvedColor
                                    )
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        matchedBook?.let { onBookOpened(it) }
                                    }
                                    .testTag("search_result_highlight_${highlight.id}")
                            )
                        }
                    }
                }
            }
        }
    }
}
