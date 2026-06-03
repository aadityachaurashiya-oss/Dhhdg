package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Book
import com.example.data.Highlight
import com.example.ui.BookViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen(
    viewModel: BookViewModel,
    onBookPageJump: (Book, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val highlights by viewModel.highlights.collectAsState()
    val books by viewModel.books.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedColorFilter by remember { mutableStateOf<String?>(null) }
    var selectedBookIdFilter by remember { mutableStateOf<String?>(null) }

    val formatter = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()) }

    // Color definitions for highlights legend
    val colorFilters = remember {
        listOf(
            Triple("#FFEB3B", "Yellow", "Important"),
            Triple("#2196F3", "Blue", "Concept"),
            Triple("#4CAF50", "Green", "Definition"),
            Triple("#F44336", "Red", "Critical")
        )
    }

    // Filters execution
    val filteredHighlights = remember(highlights, searchQuery, selectedColorFilter, selectedBookIdFilter) {
        highlights.filter { highlight ->
            val matchesQuery = highlight.snippetText.contains(searchQuery, ignoreCase = true) ||
                    (highlight.note?.contains(searchQuery, ignoreCase = true) == true)
            val matchesColor = selectedColorFilter == null || highlight.colorHex.lowercase() == selectedColorFilter!!.lowercase()
            val matchesBook = selectedBookIdFilter == null || highlight.bookId == selectedBookIdFilter
            matchesQuery && matchesColor && matchesBook
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.translate("saved_notes"), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = {
                            selectedColorFilter = null
                            selectedBookIdFilter = null
                            searchQuery = ""
                        },
                        modifier = Modifier.testTag("clear_filters_btn")
                    ) {
                        Icon(Icons.Default.List, contentDescription = "Reset Filters")
                    }
                }
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
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search highlights & your thoughts...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notes_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Horizon Color Legend Filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Theme:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                colorFilters.forEach { (hex, name, label) ->
                    val isSelected = selectedColorFilter == hex
                    val systemColor = Color(android.graphics.Color.parseColor(hex))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(systemColor, CircleShape)
                            .clip(CircleShape)
                            .clickable {
                                selectedColorFilter = if (isSelected) null else hex
                            }
                            .testTag("note_color_filter_$name"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(MaterialTheme.colorScheme.onBackground, CircleShape)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // If empty database
            if (filteredHighlights.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Annotations Logged",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Highlight active paragraphs inside the Reader to build your knowledge garden.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredHighlights, key = { it.id }) { highlight ->
                        val parentBook = books.firstOrNull { it.id == highlight.bookId }
                        val hexColor = Color(android.graphics.Color.parseColor(highlight.colorHex))

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            onClick = {
                                if (parentBook != null) {
                                    onBookPageJump(parentBook, highlight.pageNumber)
                                }
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("note_card_${highlight.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                // Source details header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(hexColor, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = parentBook?.title ?: "Unknown Source",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.widthIn(max = 180.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "• Page ${highlight.pageNumber + 1}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                // Share mock-up triggers
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Outlined.Share, contentDescription = "Export Quote", modifier = Modifier.size(14.dp))
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { viewModel.deleteHighlight(highlight.id) },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .testTag("delete_note_btn_${highlight.id}")
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Highlight", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Visual highlighted block Quote
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .heightIn(min = 24.dp)
                                            .background(hexColor, RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "\"${highlight.snippetText}\"",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Personal Thought attachment section
                                highlight.note?.let { userNote ->
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "Personal Reflection:",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary,
                                                letterSpacing = 0.5.sp
                                            )
                                            Text(
                                                text = userNote,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = formatter.format(Date(highlight.createdDate)),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
