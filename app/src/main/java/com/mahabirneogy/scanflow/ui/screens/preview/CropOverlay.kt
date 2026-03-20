package com.mahabirneogy.scanflow.ui.screens.preview

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp

private enum class CropHandle { TopLeft, TopRight, BottomLeft, BottomRight }

@Composable
fun CropOverlay(
    bitmap: Bitmap,
    onApply: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    var cropLeft   by remember { mutableFloatStateOf(0f) }
    var cropTop    by remember { mutableFloatStateOf(0f) }
    var cropRight  by remember { mutableFloatStateOf(1f) }
    var cropBottom by remember { mutableFloatStateOf(1f) }

    var activeHandle by remember { mutableStateOf<CropHandle?>(null) }
    var imageRect by remember { mutableStateOf(Rect.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerPx ->
                val cw = containerPx.width.toFloat()
                val ch = containerPx.height.toFloat()
                if (cw == 0f || ch == 0f) return@onSizeChanged
                val bitmapAspect    = bitmap.width.toFloat() / bitmap.height
                val containerAspect = cw / ch
                val (iw, ih) = if (bitmapAspect > containerAspect) {
                    cw to (cw / bitmapAspect)
                } else {
                    (ch * bitmapAspect) to ch
                }
                val il = (cw - iw) / 2f
                val it = (ch - ih) / 2f
                imageRect = Rect(il, it, il + iw, it + ih)
            }
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val ir = imageRect
                            if (ir == Rect.Zero) return@detectDragGestures
                            val tl = Offset(ir.left + cropLeft  * ir.width, ir.top + cropTop    * ir.height)
                            val tr = Offset(ir.left + cropRight * ir.width, ir.top + cropTop    * ir.height)
                            val bl = Offset(ir.left + cropLeft  * ir.width, ir.top + cropBottom * ir.height)
                            val br = Offset(ir.left + cropRight * ir.width, ir.top + cropBottom * ir.height)
                            val handles = listOf(
                                CropHandle.TopLeft to tl, CropHandle.TopRight to tr,
                                CropHandle.BottomLeft to bl, CropHandle.BottomRight to br
                            )
                            val closest = handles.minByOrNull { (_, pos) -> (pos - offset).getDistance() }
                            activeHandle = if (closest != null && (closest.second - offset).getDistance() < 80f)
                                closest.first else null
                        },
                        onDrag = { change, delta ->
                            change.consume()
                            val ir = imageRect
                            if (ir.width == 0f || ir.height == 0f || activeHandle == null) return@detectDragGestures
                            val dx = delta.x / ir.width
                            val dy = delta.y / ir.height
                            val minSize = 0.05f
                            when (activeHandle) {
                                CropHandle.TopLeft -> {
                                    cropLeft = (cropLeft + dx).coerceIn(0f, cropRight  - minSize)
                                    cropTop  = (cropTop  + dy).coerceIn(0f, cropBottom - minSize)
                                }
                                CropHandle.TopRight -> {
                                    cropRight = (cropRight + dx).coerceIn(cropLeft + minSize, 1f)
                                    cropTop   = (cropTop   + dy).coerceIn(0f, cropBottom - minSize)
                                }
                                CropHandle.BottomLeft -> {
                                    cropLeft   = (cropLeft   + dx).coerceIn(0f, cropRight - minSize)
                                    cropBottom = (cropBottom + dy).coerceIn(cropTop + minSize, 1f)
                                }
                                CropHandle.BottomRight -> {
                                    cropRight  = (cropRight  + dx).coerceIn(cropLeft + minSize, 1f)
                                    cropBottom = (cropBottom + dy).coerceIn(cropTop  + minSize, 1f)
                                }
                                null -> {}
                            }
                        },
                        onDragEnd    = { activeHandle = null },
                        onDragCancel = { activeHandle = null }
                    )
                }
        ) {
            val ir = imageRect
            if (ir == Rect.Zero) return@Canvas
            val cL = ir.left + cropLeft   * ir.width
            val cT = ir.top  + cropTop    * ir.height
            val cR = ir.left + cropRight  * ir.width
            val cB = ir.top  + cropBottom * ir.height
            val cW = cR - cL
            val cH = cB - cT

            val dimColor = Color.Black.copy(alpha = 0.6f)
            drawRect(dimColor, Offset(0f, 0f),  Size(size.width, cT))
            drawRect(dimColor, Offset(0f, cB),  Size(size.width, size.height - cB))
            drawRect(dimColor, Offset(0f, cT),  Size(cL, cH))
            drawRect(dimColor, Offset(cR, cT),  Size(size.width - cR, cH))

            drawRect(Color.White, topLeft = Offset(cL, cT), size = Size(cW, cH), style = Stroke(2.dp.toPx()))

            val gridColor  = Color.White.copy(alpha = 0.35f)
            val thinStroke = 1.dp.toPx()
            drawLine(gridColor, Offset(cL + cW / 3, cT),     Offset(cL + cW / 3, cB),     thinStroke)
            drawLine(gridColor, Offset(cL + 2*cW/3, cT),     Offset(cL + 2*cW/3, cB),     thinStroke)
            drawLine(gridColor, Offset(cL, cT + cH / 3),     Offset(cR, cT + cH / 3),     thinStroke)
            drawLine(gridColor, Offset(cL, cT + 2*cH/3),     Offset(cR, cT + 2*cH/3),     thinStroke)

            val handleLen    = 24.dp.toPx()
            val handleStroke = 3.dp.toPx()
            listOf(
                Triple(Offset(cL, cT), CropHandle.TopLeft,     Pair( 1f,  1f)),
                Triple(Offset(cR, cT), CropHandle.TopRight,    Pair(-1f,  1f)),
                Triple(Offset(cL, cB), CropHandle.BottomLeft,  Pair( 1f, -1f)),
                Triple(Offset(cR, cB), CropHandle.BottomRight, Pair(-1f, -1f)),
            ).forEach { (corner, handle, dirs) ->
                val color = if (activeHandle == handle) Color.Cyan else Color.White
                drawLine(color, corner, corner + Offset(dirs.first  * handleLen, 0f), handleStroke)
                drawLine(color, corner, corner + Offset(0f, dirs.second * handleLen), handleStroke)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Cancel crop", tint = Color.White)
            }
            Text("Crop", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Row {
                IconButton(onClick = { cropLeft = 0f; cropTop = 0f; cropRight = 1f; cropBottom = 1f }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset crop", tint = Color.White.copy(alpha = 0.7f))
                }
                IconButton(onClick = {
                    val bw = bitmap.width; val bh = bitmap.height
                    val x = (cropLeft * bw).toInt().coerceIn(0, bw - 1)
                    val y = (cropTop  * bh).toInt().coerceIn(0, bh - 1)
                    val w = ((cropRight  - cropLeft) * bw).toInt().coerceIn(1, bw - x)
                    val h = ((cropBottom - cropTop ) * bh).toInt().coerceIn(1, bh - y)
                    onApply(Bitmap.createBitmap(bitmap, x, y, w, h))
                }) {
                    Icon(Icons.Default.Check, contentDescription = "Apply crop", tint = Color.White)
                }
            }
        }
    }
}
