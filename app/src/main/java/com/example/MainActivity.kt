package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import com.example.data.Book
import com.example.ui.BookViewModel
import com.example.ui.BookViewModelFactory
import com.example.ui.screens.*
import com.example.ui.theme.LibrovaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Retrieve unified ViewModel
            val viewModel: BookViewModel by viewModels {
                BookViewModelFactory(application)
            }

            val currentThemeName by viewModel.currentThemeName.collectAsState()
            val currentLanguage by viewModel.currentLanguage.collectAsState()

            var activeTab by remember { mutableStateOf("Library") } // Library, Search, Notes, Stats, Settings
            val activeBook by viewModel.currentBook.collectAsState()

            LibrovaTheme(themeName = currentThemeName) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        
                        // Main Bottom Scaffolding layout
                        Scaffold(
                            bottomBar = {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.testTag("app_navigation_bar")
                                ) {
                                    NavigationBarItem(
                                        selected = activeTab == "Library",
                                        onClick = { activeTab = "Library" },
                                        icon = {
                                            Icon(
                                                imageVector = if (activeTab == "Library") Icons.Filled.Home else Icons.Outlined.Home,
                                                contentDescription = "Library"
                                            )
                                        },
                                        label = { Text(viewModel.translate("library"), fontSize = 10.sp) },
                                        modifier = Modifier.testTag("nav_btn_library")
                                    )

                                    NavigationBarItem(
                                        selected = activeTab == "Search",
                                        onClick = { activeTab = "Search" },
                                        icon = {
                                            Icon(
                                                imageVector = if (activeTab == "Search") Icons.Filled.Search else Icons.Outlined.Search,
                                                contentDescription = "Search"
                                            )
                                        },
                                        label = { Text(viewModel.translate("search"), fontSize = 10.sp) },
                                        modifier = Modifier.testTag("nav_btn_search")
                                    )

                                    NavigationBarItem(
                                        selected = activeTab == "Notes",
                                        onClick = { activeTab = "Notes" },
                                        icon = {
                                            Icon(
                                                imageVector = if (activeTab == "Notes") Icons.Filled.LibraryBooks else Icons.Outlined.LibraryBooks,
                                                contentDescription = "Knowledge"
                                            )
                                        },
                                        label = { Text(viewModel.translate("notes"), fontSize = 10.sp) },
                                        modifier = Modifier.testTag("nav_btn_notes")
                                    )

                                    NavigationBarItem(
                                        selected = activeTab == "Stats",
                                        onClick = { activeTab = "Stats" },
                                        icon = {
                                            Icon(
                                                imageVector = if (activeTab == "Stats") Icons.Filled.Assessment else Icons.Outlined.Assessment,
                                                contentDescription = "Stats"
                                            )
                                        },
                                        label = { Text(viewModel.translate("statistics"), fontSize = 10.sp) },
                                        modifier = Modifier.testTag("nav_btn_stats")
                                    )

                                    NavigationBarItem(
                                        selected = activeTab == "Settings",
                                        onClick = { activeTab = "Settings" },
                                        icon = {
                                            Icon(
                                                imageVector = if (activeTab == "Settings") Icons.Filled.Settings else Icons.Outlined.Settings,
                                                contentDescription = "Settings"
                                            )
                                        },
                                        label = { Text(viewModel.translate("settings"), fontSize = 10.sp) },
                                        modifier = Modifier.testTag("nav_btn_settings")
                                    )
                                }
                            }
                        ) { innerPadding ->
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                // Content frame switcher with crossfade transition animations
                                Crossfade(targetState = activeTab, label = "TabCrossfade") { tab ->
                                    when (tab) {
                                        "Library" -> LibraryScreen(
                                            viewModel = viewModel,
                                            onBookOpened = { book ->
                                                viewModel.openBook(book)
                                            }
                                        )
                                        "Search" -> SearchScreen(
                                            viewModel = viewModel,
                                            onBookOpened = { book ->
                                                viewModel.openBook(book)
                                            }
                                        )
                                        "Notes" -> KnowledgeScreen(
                                            viewModel = viewModel,
                                            onBookPageJump = { book, page ->
                                                viewModel.openBook(book)
                                                // Page index restore happens automatically during open via currentBook state
                                            }
                                        )
                                        "Stats" -> StatsScreen(
                                            viewModel = viewModel
                                        )
                                        "Settings" -> SettingsScreen(
                                            viewModel = viewModel
                                        )
                                    }
                                }
                            }
                        }

                        // --- EDGE-TO-EDGE DISTRACTION-FREE FULL READER CANVAS OVERLAY ---
                        AnimatedVisibility(
                            visible = activeBook != null,
                            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            ReaderScreen(
                                viewModel = viewModel,
                                onBack = {
                                    // Managed beautifully inside view backstack
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
