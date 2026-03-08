package com.example.scanflow.ui.screens.edit

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scanflow.data.repository.DocumentRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class DrawingTool { PENCIL, MARKER, HIGHLIGHT }

private data class DrawingPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val alpha: Float
)

private enum class PageCropHandle { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, MOVE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageEditScreen(
    documentId: String,
    pageIndex: Int,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val repository = remember { DocumentRepositoryImpl(context) }

    var baseBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val paths = remember { mutableStateListOf<DrawingPath>() }
    val redoStack = remember { mutableStateListOf<DrawingPath>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    var selectedTool by remember { mutableStateOf(DrawingTool.PENCIL) }
    var selectedColor by remember { mutableStateOf(Color(0xFF2196F3)) }
    var isEraserActive by remember { mutableStateOf(false) }

    var isCropMode by remember { mutableStateOf(false) }
    var cropLeft   by remember { mutableFloatStateOf(0f) }
    var cropTop    by remember { mutableFloatStateOf(0f) }
    var cropRight  by remember { mutableFloatStateOf(0f) }
    var cropBottom by remember { mutableFloatStateOf(0f) }

    val presetColors = remember {
        listOf(
            Color(0xFFE53935), Color(0xFF2196F3), Color(0xFF4CAF50),
            Color(0xFFFFEB3B), Color(0xFF9C27B0), Color(0xFFFF9800), Color(0xFF000000)
        )
    }

    LaunchedEffect(documentId, pageIndex) {
        withContext(Dispatchers.IO) {
            val doc = repository.getDocument(documentId)
            if (doc != null) baseBitmap = repository.renderPage(doc.pdfPath, pageIndex, 1080)
        }
    }

    val toolState    = rememberUpdatedState(selectedTool)
    val colorState   = rememberUpdatedState(selectedColor)
    val eraserState  = rememberUpdatedState(isEraserActive)
    val primaryColor = MaterialTheme.colorScheme.primary

    fun handleSave() {
        val bmp = baseBitmap ?: return
        isSaving = true
        scope.launch {
            withContext(Dispatchers.IO) {
                val finalBitmap = createFinalBitmap(bmp, paths.toList(), canvasSize.width.toFloat(), canvasSize.height.toFloat())
                repository.updatePage(documentId, pageIndex, finalBitmap)
                if (finalBitmap !== bmp) finalBitmap.recycle()
            }
            onClose()
        }
    }

    fun handleRotate() {
        val bmp = baseBitmap ?: return
        val current = if (paths.isNotEmpty() && canvasSize.width > 0) {
            createFinalBitmap(bmp, paths.toList(), canvasSize.width.toFloat(), canvasSize.height.toFloat())
        } else bmp
        val matrix = Matrix()
        matrix.postRotate(90f)
        baseBitmap = Bitmap.createBitmap(current, 0, 0, current.width, current.height, matrix, true)
        if (current !== bmp) current.recycle()
        paths.clear(); redoStack.clear()
    }

    fun enterCropMode() {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return
        val bmp = baseBitmap ?: return
        if (paths.isNotEmpty()) {
            baseBitmap = createFinalBitmap(bmp, paths.toList(), canvasSize.width.toFloat(), canvasSize.height.toFloat())
            paths.clear(); redoStack.clear()
        }
        isEraserActive = false
        isCropMode = true
        val insetX = canvasSize.width * 0.1f
        val insetY = canvasSize.height * 0.1f
        cropLeft = insetX; cropTop = insetY
        cropRight = canvasSize.width.toFloat() - insetX
        cropBottom = canvasSize.height.toFloat() - insetY
    }

    fun handleApplyCrop() {
        val bmp = baseBitmap ?: return
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return
        val scaleX = bmp.width.toFloat() / canvasSize.width
        val scaleY = bmp.height.toFloat() / canvasSize.height
        val x = (cropLeft  * scaleX).toInt().coerceIn(0, bmp.width  - 1)
        val y = (cropTop   * scaleY).toInt().coerceIn(0, bmp.height - 1)
        val w = ((cropRight  - cropLeft) * scaleX).toInt().coerceIn(1, bmp.width  - x)
        val h = ((cropBottom - cropTop ) * scaleY).toInt().coerceIn(1, bmp.height - y)
        baseBitmap = Bitmap.createBitmap(bmp, x, y, w, h)
        paths.clear(); redoStack.clear()
        isCropMode = false
    }

    val bmp = baseBitmap
    if (bmp == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val imageBitmap = remember(bmp) { bmp.asImageBitmap() }
    val aspectRatio = bmp.width.toFloat() / bmp.height.toFloat()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isCropMode) {
                TopAppBar(
                    navigationIcon = {
                        TextButton(onClick = { isCropMode = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    },
                    title = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Crop", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        TextButton(onClick = { handleApplyCrop() }) {
                            Text("Apply", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            } else {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    title = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Edit PDF", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        TextButton(onClick = { handleSave() }, enabled = !isSaving) {
                            Text("Done", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFE8EAF0))
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSaving) {
                    CircularProgressIndicator()
                } else {
                    Card(
                        modifier = Modifier.aspectRatio(aspectRatio),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { canvasSize = it }
                                .pointerInput(isCropMode) {
                                    if (isCropMode) {
                                        var activeHandle: PageCropHandle? = null
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                val hitRadius = 48f
                                                activeHandle = when {
                                                    (offset - Offset(cropLeft, cropTop)).getDistance()     < hitRadius -> PageCropHandle.TOP_LEFT
                                                    (offset - Offset(cropRight, cropTop)).getDistance()    < hitRadius -> PageCropHandle.TOP_RIGHT
                                                    (offset - Offset(cropLeft, cropBottom)).getDistance()  < hitRadius -> PageCropHandle.BOTTOM_LEFT
                                                    (offset - Offset(cropRight, cropBottom)).getDistance() < hitRadius -> PageCropHandle.BOTTOM_RIGHT
                                                    offset.x in cropLeft..cropRight && offset.y in cropTop..cropBottom -> PageCropHandle.MOVE
                                                    else -> null
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                if (activeHandle == null) return@detectDragGestures
                                                val dx = dragAmount.x; val dy = dragAmount.y
                                                val minSize = 60f
                                                val maxW = size.width.toFloat(); val maxH = size.height.toFloat()
                                                when (activeHandle) {
                                                    PageCropHandle.TOP_LEFT -> {
                                                        cropLeft = (cropLeft + dx).coerceIn(0f, cropRight  - minSize)
                                                        cropTop  = (cropTop  + dy).coerceIn(0f, cropBottom - minSize)
                                                    }
                                                    PageCropHandle.TOP_RIGHT -> {
                                                        cropRight = (cropRight + dx).coerceIn(cropLeft + minSize, maxW)
                                                        cropTop   = (cropTop   + dy).coerceIn(0f, cropBottom - minSize)
                                                    }
                                                    PageCropHandle.BOTTOM_LEFT -> {
                                                        cropLeft   = (cropLeft   + dx).coerceIn(0f, cropRight - minSize)
                                                        cropBottom = (cropBottom + dy).coerceIn(cropTop + minSize, maxH)
                                                    }
                                                    PageCropHandle.BOTTOM_RIGHT -> {
                                                        cropRight  = (cropRight  + dx).coerceIn(cropLeft + minSize, maxW)
                                                        cropBottom = (cropBottom + dy).coerceIn(cropTop + minSize, maxH)
                                                    }
                                                    PageCropHandle.MOVE -> {
                                                        val w = cropRight - cropLeft; val h = cropBottom - cropTop
                                                        val newLeft = (cropLeft + dx).coerceIn(0f, maxW - w)
                                                        val newTop  = (cropTop  + dy).coerceIn(0f, maxH - h)
                                                        cropLeft = newLeft; cropTop = newTop
                                                        cropRight = newLeft + w; cropBottom = newTop + h
                                                    }
                                                    null -> {}
                                                }
                                            },
                                            onDragEnd = { activeHandle = null }
                                        )
                                    } else {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                if (!eraserState.value) {
                                                    currentPoints = listOf(offset)
                                                    redoStack.clear()
                                                }
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                val pos = change.position
                                                if (eraserState.value) {
                                                    val toRemove = paths.filter { path ->
                                                        path.points.any { pt -> (pt - pos).getDistance() < 30f }
                                                    }
                                                    if (toRemove.isNotEmpty()) paths.removeAll(toRemove.toSet())
                                                } else {
                                                    currentPoints = currentPoints + pos
                                                }
                                            },
                                            onDragEnd = {
                                                if (!eraserState.value && currentPoints.size > 1) {
                                                    val (sw, a) = getToolProperties(toolState.value)
                                                    paths.add(DrawingPath(currentPoints.toList(), colorState.value, sw, a))
                                                }
                                                currentPoints = emptyList()
                                            }
                                        )
                                    }
                                }
                        ) {
                            drawImage(image = imageBitmap, dstSize = IntSize(size.width.toInt(), size.height.toInt()))

                            for (dp in paths) {
                                drawPath(
                                    path = buildSmoothPath(dp.points),
                                    color = dp.color,
                                    alpha = dp.alpha,
                                    style = Stroke(width = dp.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }

                            if (isCropMode) {
                                val cl = cropLeft; val ct = cropTop; val cr = cropRight; val cb = cropBottom
                                val overlay = Color.Black.copy(alpha = 0.5f)
                                drawRect(overlay, Offset.Zero, Size(size.width, ct))
                                drawRect(overlay, Offset(0f, cb), Size(size.width, size.height - cb))
                                drawRect(overlay, Offset(0f, ct), Size(cl, cb - ct))
                                drawRect(overlay, Offset(cr, ct), Size(size.width - cr, cb - ct))
                                drawRect(Color.White, Offset(cl, ct), Size(cr - cl, cb - ct), style = Stroke(2f))
                                val thirdW = (cr - cl) / 3; val thirdH = (cb - ct) / 3
                                val gridColor = Color.White.copy(alpha = 0.3f)
                                for (i in 1..2) {
                                    drawLine(gridColor, Offset(cl + thirdW * i, ct), Offset(cl + thirdW * i, cb), 0.5f)
                                    drawLine(gridColor, Offset(cl, ct + thirdH * i), Offset(cr, ct + thirdH * i), 0.5f)
                                }
                                val handleR = 10f
                                listOf(Offset(cl, ct), Offset(cr, ct), Offset(cl, cb), Offset(cr, cb)).forEach { pos ->
                                    drawCircle(Color.White, handleR + 2, pos)
                                    drawCircle(primaryColor, handleR, pos)
                                }
                            } else {
                                if (currentPoints.size >= 2 && !eraserState.value) {
                                    val (sw, a) = getToolProperties(toolState.value)
                                    drawPath(
                                        path = buildSmoothPath(currentPoints),
                                        color = colorState.value,
                                        alpha = a,
                                        style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!isCropMode) {
                Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFFF5F5F5))
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                DrawingTool.entries.forEach { tool ->
                                    val isSelected = selectedTool == tool && !isEraserActive
                                    val icon = when (tool) {
                                        DrawingTool.PENCIL    -> Icons.Default.Edit
                                        DrawingTool.MARKER    -> Icons.Default.Brush
                                        DrawingTool.HIGHLIGHT -> Icons.Default.FormatPaint
                                    }
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        onClick = { selectedTool = tool; isEraserActive = false },
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(50),
                                        shadowElevation = if (isSelected) 2.dp else 0.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = if (isSelected) primaryColor else Color.Gray
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = tool.name.lowercase().replaceFirstChar { it.uppercase() },
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (isSelected) primaryColor else Color.Gray
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            IconButton(
                                onClick = { if (paths.isNotEmpty()) redoStack.add(paths.removeLast()) },
                                enabled = paths.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Undo",
                                    tint = if (paths.isNotEmpty()) Color.DarkGray else Color.LightGray
                                )
                            }
                            IconButton(
                                onClick = { if (redoStack.isNotEmpty()) paths.add(redoStack.removeLast()) },
                                enabled = redoStack.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Redo,
                                    contentDescription = "Redo",
                                    tint = if (redoStack.isNotEmpty()) Color.DarkGray else Color.LightGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            presetColors.forEach { color ->
                                val isSelected = selectedColor == color
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .then(if (isSelected) Modifier.border(2.5.dp, primaryColor, CircleShape) else Modifier)
                                        .clip(CircleShape)
                                        .clickable { selectedColor = color; isEraserActive = false }
                                        .padding(if (isSelected) 4.dp else 0.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                scope.launch { snackbarHostState.showSnackbar("Custom colors coming soon") }
                            }) {
                                Icon(Icons.Default.Palette, contentDescription = "Color picker", tint = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { enterCropMode() }
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Crop, contentDescription = "Crop", tint = Color.DarkGray)
                                Text("Crop", fontSize = 12.sp, color = Color.DarkGray)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { handleRotate() }
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate", tint = Color.DarkGray)
                                Text("Rotate", fontSize = 12.sp, color = Color.DarkGray)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isEraserActive = !isEraserActive }
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.EditOff,
                                    contentDescription = "Eraser",
                                    tint = if (isEraserActive) primaryColor else Color.DarkGray
                                )
                                Text(
                                    "Eraser",
                                    fontSize = 12.sp,
                                    color = if (isEraserActive) primaryColor else Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getToolProperties(tool: DrawingTool): Pair<Float, Float> = when (tool) {
    DrawingTool.PENCIL    -> 4f to 1f
    DrawingTool.MARKER    -> 10f to 1f
    DrawingTool.HIGHLIGHT -> 24f to 0.35f
}

private fun buildSmoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.size < 2) return path
    path.moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size) {
        val prev = points[i - 1]; val curr = points[i]
        path.quadraticTo(prev.x, prev.y, (prev.x + curr.x) / 2, (prev.y + curr.y) / 2)
    }
    path.lineTo(points.last().x, points.last().y)
    return path
}

private fun createFinalBitmap(
    baseBitmap: Bitmap,
    paths: List<DrawingPath>,
    canvasWidth: Float,
    canvasHeight: Float
): Bitmap {
    val result = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
    if (paths.isEmpty() || canvasWidth <= 0 || canvasHeight <= 0) return result

    val canvas = android.graphics.Canvas(result)
    val scaleX = result.width.toFloat() / canvasWidth
    val scaleY = result.height.toFloat() / canvasHeight

    for (dp in paths) {
        val paint = android.graphics.Paint().apply {
            color = dp.color.toArgb()
            alpha = (dp.alpha * 255).toInt()
            strokeWidth = dp.strokeWidth * scaleX
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            isAntiAlias = true
        }
        val gfxPath = android.graphics.Path()
        val pts = dp.points
        if (pts.size >= 2) {
            gfxPath.moveTo(pts[0].x * scaleX, pts[0].y * scaleY)
            for (i in 1 until pts.size) {
                val prev = pts[i - 1]; val curr = pts[i]
                gfxPath.quadTo(
                    prev.x * scaleX, prev.y * scaleY,
                    (prev.x + curr.x) / 2 * scaleX, (prev.y + curr.y) / 2 * scaleY
                )
            }
            gfxPath.lineTo(pts.last().x * scaleX, pts.last().y * scaleY)
        }
        canvas.drawPath(gfxPath, paint)
    }
    return result
}
