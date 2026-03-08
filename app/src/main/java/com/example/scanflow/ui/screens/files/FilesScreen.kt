package com.example.scanflow.ui.screens.files

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scanflow.ui.components.BottomNavigationBar
import com.example.scanflow.ui.components.DocumentCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    onScanClick: () -> Unit,
    onDocumentClick: (String) -> Unit = {},
    onInfoClick: () -> Unit = {},
    viewModel: FilesViewModel = viewModel()
) {
    val categories = listOf("All Docs", "Work", "Personal", "ID Cards", "Receipts")
    var selectedCategory by remember { mutableStateOf("All Docs") }
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val documents by viewModel.documents.collectAsState()

    val filteredDocuments = remember(documents, selectedCategory) {
        if (selectedCategory == "All Docs") documents
        else documents.filter { it.category == selectedCategory }
    }

    val displayedDocuments = remember(filteredDocuments, searchQuery) {
        if (searchQuery.isBlank()) filteredDocuments
        else filteredDocuments.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search documents...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("All Documents", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearching = !isSearching
                        if (!isSearching) searchQuery = ""
                    }) {
                        Icon(
                            if (isSearching) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                    if (!isSearching) {
                        IconButton(onClick = onInfoClick) {
                            Icon(Icons.Default.Info, contentDescription = "About")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onScanClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Now", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        },
        bottomBar = {
            BottomNavigationBar(currentRoute = "files", onScanClick = onScanClick)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(percent = 50)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (displayedDocuments.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.DocumentScanner,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No documents yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Tap 'Scan Now' to get started",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray
                            )
                        }
                    }
                } else {
                    items(displayedDocuments, key = { it.id }) { doc ->
                        DocumentCard(
                            doc = doc,
                            onClick = { onDocumentClick(doc.id) },
                            onDelete = { id -> viewModel.deleteDocument(id) }
                        )
                    }
                }
            }
        }
    }
}
