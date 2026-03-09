package com.example.scanflow.ui.screens.preview

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scanflow.ui.util.WindowSize
import com.example.scanflow.ui.util.isLandscape
import com.example.scanflow.ui.util.rememberWindowSize
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    imagePaths: List<String>,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    viewModel: PreviewViewModel = viewModel()
) {
    val windowSize = rememberWindowSize()
    val landscape = isLandscape()
    val isExpandedLayout = landscape && windowSize != WindowSize.Compact

    val originalBitmaps = remember(imagePaths) {
        mutableStateListOf(*imagePaths.map { BitmapFactory.decodeFile(it) }.toTypedArray())
    }

    if (originalBitmaps.isEmpty() || originalBitmaps.any { it == null }) {
        LaunchedEffect(Unit) { onBackClick() }
        return
    }

    val pagerState = rememberPagerState(pageCount = { imagePaths.size })
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }

    val rotations = remember { mutableStateMapOf<Int, Float>() }
    val filters   = remember { mutableStateMapOf<Int, String>() }

    val currentRotation = rotations[currentPage] ?: 0f
    val currentFilter   = filters[currentPage] ?: "Original"

    var showSaveDialog  by remember { mutableStateOf(false) }
    var showCropOverlay by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isSaving by viewModel.isSaving.collectAsState()

    val currentBitmap  = originalBitmaps[currentPage]!!
    val currentProcessed by remember(currentBitmap, currentRotation, currentFilter) {
        mutableStateOf(applyProcessing(currentBitmap, currentRotation, currentFilter))
    }

    val filterThumbnails = remember(currentBitmap) {
        val scale = 64f / currentBitmap.width.coerceAtLeast(1)
        val small = Bitmap.createScaledBitmap(
            currentBitmap, 64, (currentBitmap.height * scale).toInt().coerceAtLeast(1), true
        )
        filterNames.associateWith { name -> applyProcessing(small, 0f, name) }
    }

    if (showSaveDialog) {
        SaveDocumentDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { title, category ->
                showSaveDialog = false
                viewModel.saveDocument(
                    originalBitmaps = originalBitmaps.toList(),
                    rotations = rotations.toMap(),
                    filters = filters.toMap(),
                    imagePaths = imagePaths,
                    title = title,
                    category = category,
                    onDone = onSaveClick
                )
            },
            isSaving = isSaving
        )
    }

    // Controls panel content (shared between compact and expanded)
    @Composable
    fun ControlsPanel(modifier: Modifier = Modifier, isVertical: Boolean = false) {
        val controlsModifier = if (isVertical) {
            modifier.verticalScroll(rememberScrollState())
        } else modifier

        Surface(
            color = Color(0xFF1A1C24),
            shape = if (isVertical) RoundedCornerShape(topStart = 24.dp)
                    else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = if (isVertical) Modifier.fillMaxHeight().width(380.dp) else Modifier.fillMaxWidth()
        ) {
            Column(modifier = controlsModifier.padding(top = 24.dp, bottom = 32.dp, start = 16.dp, end = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    @Suppress("DEPRECATION")
                    UtilityButton(Icons.Default.RotateLeft, "Rotate L") {
                        rotations[currentPage] = currentRotation - 90f
                    }
                    @Suppress("DEPRECATION")
                    UtilityButton(Icons.Default.RotateRight, "Rotate R") {
                        rotations[currentPage] = currentRotation + 90f
                    }
                    UtilityButton(Icons.Default.Tune, "Adjust") {
                        scope.launch { snackbarHostState.showSnackbar("Adjust feature coming soon") }
                    }
                    UtilityButton(Icons.Default.Crop, "Crop") {
                        showCropOverlay = true
                    }
                    UtilityButton(Icons.Default.AutoFixHigh, "Enhance") {
                        filters[currentPage] = "HD Clear"
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(filterNames) { filter ->
                        FilterItem(
                            name = filter,
                            isSelected = filter == currentFilter,
                            thumbnail = filterThumbnails[filter],
                            onClick = { filters[currentPage] = filter }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (imagePaths.size == 1) "1 page"
                               else "${currentPage + 1} of ${imagePaths.size} pages",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = { showSaveDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save as PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Image pager content (shared)
    @Composable
    fun ImagePager(modifier: Modifier = Modifier) {
        Box(modifier = modifier) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val bitmap   = originalBitmaps[page]!!
                val rotation = rotations[page] ?: 0f
                val filter   = filters[page] ?: "Original"
                val processed = remember(bitmap, rotation, filter) {
                    applyProcessing(bitmap, rotation, filter)
                }
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                    ) {
                        processed?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Page ${page + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }

            if (imagePaths.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(imagePaths.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == currentPage) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentPage) Color.White
                                    else Color.White.copy(alpha = 0.4f)
                                )
                        )
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Scanner Preview", style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                    )
                )
            }
        ) { padding ->
            if (isExpandedLayout) {
                // Tablet: side-by-side layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color(0xFFF5F5F5))
                ) {
                    ImagePager(modifier = Modifier.weight(1f).fillMaxHeight())
                    ControlsPanel(isVertical = true)
                }
            } else {
                // Phone: stacked layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color(0xFFF5F5F5))
                ) {
                    ImagePager(modifier = Modifier.weight(1f).fillMaxWidth())
                    ControlsPanel()
                }
            }
        }

        if (showCropOverlay) {
            currentProcessed?.let { bmp ->
                CropOverlay(
                    bitmap = bmp,
                    onApply = { cropped ->
                        originalBitmaps[currentPage] = cropped
                        rotations[currentPage] = 0f
                        filters[currentPage] = "Original"
                        showCropOverlay = false
                    },
                    onDismiss = { showCropOverlay = false }
                )
            }
        }

        if (isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Saving PDF...", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun SaveDocumentDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, category: String) -> Unit,
    isSaving: Boolean
) {
    var docTitle by remember { mutableStateOf("Scan_${System.currentTimeMillis() / 1000}") }
    var selectedCategory by remember { mutableStateOf("Personal") }
    val categories = listOf("Work", "Personal", "ID Cards", "Receipts")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Document") },
        text = {
            Column {
                OutlinedTextField(
                    value = docTitle,
                    onValueChange = { docTitle = it },
                    label = { Text("Document Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = cat == selectedCategory,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(docTitle, selectedCategory) },
                enabled = !isSaving && docTitle.isNotBlank()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun UtilityButton(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
            Icon(icon, contentDescription = label, tint = Color.Gray)
        }
        Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun FilterItem(name: String, isSelected: Boolean, thumbnail: Bitmap? = null, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 80.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.DarkGray,
                    shape = RoundedCornerShape(8.dp)
                )
                .background(Color.White)
                .padding(if (isSelected) 2.dp else 0.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable(interactionSource = null, indication = null, onClick = onClick)
        ) {
            thumbnail?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } ?: Box(modifier = Modifier.fillMaxSize().background(Color.LightGray.copy(alpha = 0.3f)))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
