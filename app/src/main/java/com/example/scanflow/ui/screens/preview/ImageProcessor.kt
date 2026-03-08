package com.example.scanflow.ui.screens.preview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint

val filterNames = listOf("Original", "B/W", "Lighten", "HD Clear", "Magic Color")

fun applyProcessing(source: Bitmap, degrees: Float, filter: String): Bitmap? {
    val rotated = if (degrees != 0f) {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    } else {
        source
    }

    val colorMatrix = when (filter) {
        "B/W"         -> ColorMatrix().apply { setSaturation(0f) }
        "Lighten"     -> ColorMatrix(floatArrayOf(
            1.2f, 0f,   0f,   0f, 30f,
            0f,   1.2f, 0f,   0f, 30f,
            0f,   0f,   1.2f, 0f, 30f,
            0f,   0f,   0f,   1f,  0f
        ))
        "HD Clear"    -> ColorMatrix(floatArrayOf(
            1.5f, 0f,   0f,   0f, -40f,
            0f,   1.5f, 0f,   0f, -40f,
            0f,   0f,   1.5f, 0f, -40f,
            0f,   0f,   0f,   1f,   0f
        ))
        "Magic Color" -> ColorMatrix().apply { setSaturation(1.8f) }
        else          -> null
    }

    if (colorMatrix == null) return rotated

    val filtered = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(filtered)
    val paint = Paint().apply { this.colorFilter = ColorMatrixColorFilter(colorMatrix) }
    canvas.drawBitmap(rotated, 0f, 0f, paint)
    return filtered
}
