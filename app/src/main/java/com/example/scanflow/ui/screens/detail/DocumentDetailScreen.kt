package com.example.scanflow.ui.screens.detail

import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    documentId: String,
    onBackClick: () -> Unit,
    onPageClick: (Int) -> Unit = {},
    onAddPageClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: DocumentDetailViewModel = viewModel(factory = DocumentDetailViewModel.Factory(documentId))

    val document  by viewModel.document.collectAsState()
    val pageCount by viewModel.pageCount.collectAsState()
    val pageBitmaps by viewModel.pageBitmaps.collectAsState()

    var isEditing  by remember { mutableStateOf(false) }
    var editTitle  by remember { mutableStateOf("") }

    // Keep editTitle in sync with loaded document
    LaunchedEffect(document) {
        document?.let { editTitle = it.title }
    }

    // Refresh when returning from add-page / page-edit
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasResumedBefore by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (hasResumedBefore) viewModel.loadDocument()
                hasResumedBefore = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val doc = document
    if (doc == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val timeAgo = remember(doc.timestamp) {
        if (doc.timestamp > 0) {
            DateUtils.getRelativeTimeSpanString(
                doc.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
            ).toString()
        } else {
            doc.dateCreated
        }
    }

    fun shareDocument() {
        val shareUri: Uri = if (doc.pdfPath.startsWith("content://")) {
            // MediaStore URI — can be shared directly, no FileProvider needed
            Uri.parse(doc.pdfPath)
        } else {
            // Legacy internal file — use FileProvider
            val pdfFile = File(doc.pdfPath)
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
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = doc.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Edited $timeAgo · ${doc.fileSize}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { shareDocument() },
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = { shareDocument() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                Column {
                    Text(
                        text = "FILE DETAILS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isEditing,
                        singleLine = true,
                        label = { Text("File name") },
                        trailingIcon = {
                            IconButton(onClick = {
                                if (isEditing) {
                                    viewModel.renameDocument(editTitle.trim())
                                    isEditing = false
                                } else {
                                    isEditing = true
                                }
                            }) {
                                Icon(
                                    if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                                    contentDescription = if (isEditing) "Save" else "Edit"
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "$pageCount ${if (pageCount == 1) "Page" else "Pages"} Total",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            items(pageCount) { index ->
                Card(
                    onClick = { onPageClick(index) },
                    modifier = Modifier.fillMaxWidth().aspectRatio(0.75f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val bmp = pageBitmaps[index]
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Page ${index + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                            color = Color.Black.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "Page ${index + 1}",
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    onClick = { onAddPageClick() },
                    modifier = Modifier.fillMaxWidth().aspectRatio(0.75f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add Page",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
