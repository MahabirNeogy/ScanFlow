package com.mahabirneogy.scanflow.ui.screens.files

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mahabirneogy.scanflow.R
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mahabirneogy.scanflow.data.repository.DocumentRepositoryImpl
import com.mahabirneogy.scanflow.ui.components.BottomNavigationBar
import com.mahabirneogy.scanflow.ui.components.DocumentCard
import com.mahabirneogy.scanflow.ui.util.WindowSize
import com.mahabirneogy.scanflow.ui.util.rememberWindowSize
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    onScanClick: () -> Unit,
    onDocumentClick: (String) -> Unit = {},
    onInfoClick: () -> Unit = {},
    viewModel: FilesViewModel = viewModel()
) {
    val windowSize = rememberWindowSize()
    val gridColumns = when (windowSize) {
        WindowSize.Compact -> 1
        WindowSize.Medium -> 2
        WindowSize.Expanded -> 3
    }

    val categories = listOf("All Docs", "Work", "Personal", "ID Cards", "Receipts")
    var selectedCategory by remember { mutableStateOf("All Docs") }
    val context = LocalContext.current
    val repository = remember { DocumentRepositoryImpl(context) }
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

    fun shareDocument(id: String) {
        val document = repository.getDocument(id) ?: return
        val shareUri: Uri = if (document.pdfPath.startsWith("content://")) {
            Uri.parse(document.pdfPath)
        } else {
            val pdfFile = File(document.pdfPath)
            if (!pdfFile.exists()) return
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF"))
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
                            Image(
                                painter = painterResource(id = R.drawable.app_icon),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
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

            if (displayedDocuments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedDocuments, key = { it.id }) { doc ->
                        DocumentCard(
                            doc = doc,
                            onClick = { onDocumentClick(doc.id) },
                            onDelete = { id -> viewModel.deleteDocument(id) },
                            onShare = { id -> shareDocument(id) }
                        )
                    }
                }
            }
        }
    }
}
