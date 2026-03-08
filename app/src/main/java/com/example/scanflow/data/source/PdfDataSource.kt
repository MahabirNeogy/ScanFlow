package com.example.scanflow.data.source

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.example.scanflow.domain.model.ScannedDocument
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PdfDataSource(private val context: Context) {

    // Internal storage — only for small app-specific files (thumbnails, metadata)
    private fun getMetaDir(): File {
        val dir = File(context.filesDir, "ScanFlow")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // ── Save ────────────────────────────────────────────────────────────────

    fun savePdf(bitmaps: List<Bitmap>, title: String, category: String): ScannedDocument {
        val id = UUID.randomUUID().toString()
        val docDir = File(getMetaDir(), id)
        docDir.mkdirs()

        val safeTitle = title.replace(Regex("[^a-zA-Z0-9._\\- ]"), "_")

        // PDFs go to shared external storage on API 29+, internal fallback on older
        val pdfUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            savePdfToMediaStore(safeTitle, bitmaps)
        } else {
            savePdfInternal(safeTitle, bitmaps, docDir)
        }

        // Thumbnail stays internal (small, app-specific)
        val thumbFile = File(docDir, "thumb.jpg")
        saveThumbnail(bitmaps.first(), thumbFile)

        val now = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(now))
        val sizeStr = formatFileSize(getPdfSize(pdfUri))

        val meta = JSONObject().apply {
            put("id", id); put("title", title); put("category", category)
            put("dateCreated", dateStr); put("fileSize", sizeStr)
            put("timestamp", now); put("pdfUri", pdfUri)
        }
        File(docDir, "meta.json").writeText(meta.toString())

        return ScannedDocument(
            id = id, title = title, category = category,
            dateCreated = dateStr, fileSize = sizeStr,
            pdfPath = pdfUri, thumbnailPath = thumbFile.absolutePath, timestamp = now
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("NewApi")
    private fun savePdfToMediaStore(safeTitle: String, bitmaps: List<Bitmap>): String {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "$safeTitle.pdf")
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/ScanFlow")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = context.contentResolver.insert(collection, values)
            ?: throw IllegalStateException("MediaStore insert failed")

        val pdfDocument = buildPdfDocument(bitmaps)
        context.contentResolver.openOutputStream(uri)?.use { pdfDocument.writeTo(it) }
        pdfDocument.close()

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)

        return uri.toString()
    }

    private fun savePdfInternal(safeTitle: String, bitmaps: List<Bitmap>, docDir: File): String {
        val pdfFile = File(docDir, "$safeTitle.pdf")
        val pdfDocument = buildPdfDocument(bitmaps)
        pdfDocument.writeTo(FileOutputStream(pdfFile))
        pdfDocument.close()
        return pdfFile.absolutePath
    }

    private fun buildPdfDocument(bitmaps: List<Bitmap>): PdfDocument {
        val pdfDocument = PdfDocument()
        bitmaps.forEachIndexed { index, bitmap ->
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            pdfDocument.finishPage(page)
        }
        return pdfDocument
    }

    private fun saveThumbnail(source: Bitmap, thumbFile: File) {
        val scale = 200f / source.width.coerceAtLeast(1)
        val thumb = Bitmap.createScaledBitmap(source, 200, (source.height * scale).toInt().coerceAtLeast(1), true)
        FileOutputStream(thumbFile).use { out -> thumb.compress(Bitmap.CompressFormat.JPEG, 80, out) }
        thumb.recycle()
    }

    // ── Query ───────────────────────────────────────────────────────────────

    fun listDocuments(): List<ScannedDocument> {
        return getMetaDir().listFiles()
            ?.filter { it.isDirectory && File(it, "meta.json").exists() }
            ?.mapNotNull { docDir -> parseDocument(docDir) }
            ?.sortedByDescending { it.timestamp }
            ?: emptyList()
    }

    fun getDocument(id: String): ScannedDocument? {
        val docDir = File(getMetaDir(), id)
        return parseDocument(docDir)
    }

    private fun parseDocument(docDir: File): ScannedDocument? {
        val metaFile = File(docDir, "meta.json")
        if (!metaFile.exists()) return null
        return try {
            val meta = JSONObject(metaFile.readText())
            // Support both new "pdfUri" key and old "pdfPath" key for migration
            val pdfUri = meta.optString("pdfUri").ifEmpty {
                meta.optString("pdfPath").ifEmpty {
                    docDir.listFiles { f -> f.extension == "pdf" }?.firstOrNull()?.absolutePath ?: ""
                }
            }
            ScannedDocument(
                id = meta.getString("id"),
                title = meta.getString("title"),
                category = meta.getString("category"),
                dateCreated = meta.getString("dateCreated"),
                fileSize = meta.getString("fileSize"),
                pdfPath = pdfUri,
                thumbnailPath = File(docDir, "thumb.jpg").absolutePath,
                timestamp = meta.optLong("timestamp", 0L)
            )
        } catch (e: Exception) { null }
    }

    // ── Delete / Rename ─────────────────────────────────────────────────────

    fun deleteDocument(id: String): Boolean {
        val docDir = File(getMetaDir(), id)
        // Delete PDF from MediaStore (or filesystem for legacy)
        try {
            val metaFile = File(docDir, "meta.json")
            if (metaFile.exists()) {
                val pdfUri = JSONObject(metaFile.readText()).optString("pdfUri")
                if (pdfUri.startsWith("content://")) {
                    context.contentResolver.delete(Uri.parse(pdfUri), null, null)
                }
            }
        } catch (_: Exception) {}
        // Delete thumbnail + metadata dir
        return docDir.deleteRecursively()
    }

    fun renameDocument(id: String, newTitle: String): Boolean {
        val docDir = File(getMetaDir(), id)
        val metaFile = File(docDir, "meta.json")
        if (!metaFile.exists()) return false
        return try {
            val meta = JSONObject(metaFile.readText())
            val pdfUri = meta.optString("pdfUri")
            meta.put("title", newTitle)
            if (pdfUri.startsWith("content://")) {
                val safeTitle = newTitle.replace(Regex("[^a-zA-Z0-9._\\- ]"), "_")
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, "$safeTitle.pdf")
                }
                context.contentResolver.update(Uri.parse(pdfUri), values, null, null)
            } else if (pdfUri.isNotEmpty()) {
                val oldFile = File(pdfUri)
                if (oldFile.exists()) {
                    val safeTitle = newTitle.replace(Regex("[^a-zA-Z0-9._\\- ]"), "_")
                    val newFile = File(docDir, "$safeTitle.pdf")
                    oldFile.renameTo(newFile)
                    meta.put("pdfUri", newFile.absolutePath)
                }
            }
            metaFile.writeText(meta.toString())
            true
        } catch (e: Exception) { false }
    }

    // ── PDF Rendering ────────────────────────────────────────────────────────

    fun getPageCount(pdfPath: String): Int {
        return try {
            openPdfDescriptor(pdfPath, "r")?.use { fd ->
                val renderer = PdfRenderer(fd)
                val count = renderer.pageCount
                renderer.close()
                count
            } ?: 0
        } catch (e: Exception) { 0 }
    }

    fun renderPage(pdfPath: String, pageIndex: Int, width: Int): Bitmap? {
        return renderPages(pdfPath, listOf(pageIndex), width)[pageIndex]
    }

    fun renderPages(pdfPath: String, pageIndices: List<Int>, width: Int): Map<Int, Bitmap> {
        val result = mutableMapOf<Int, Bitmap>()
        try {
            openPdfDescriptor(pdfPath, "r")?.use { fd ->
                val renderer = PdfRenderer(fd)
                for (pageIndex in pageIndices) {
                    if (pageIndex >= renderer.pageCount) continue
                    val page = renderer.openPage(pageIndex)
                    val scale = width.toFloat() / page.width
                    val bitmap = Bitmap.createBitmap(width, (page.height * scale).toInt(), Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    result[pageIndex] = bitmap
                }
                renderer.close()
            }
        } catch (_: Exception) {}
        return result
    }

    // ── PDF Editing ──────────────────────────────────────────────────────────

    fun addPage(id: String, newPageBitmap: Bitmap): Boolean {
        return addPages(id, listOf(newPageBitmap))
    }

    fun addPages(id: String, newPageBitmaps: List<Bitmap>): Boolean {
        if (newPageBitmaps.isEmpty()) return true
        val pdfUri = getDocumentPdfUri(id) ?: return false
        return try {
            val existingBitmaps = renderAllPages(pdfUri)
            writePdfToUri(pdfUri, existingBitmaps + newPageBitmaps)
            existingBitmaps.forEach { it.recycle() }
            updateMetaFileSize(id, getPdfSize(pdfUri))
            // Update thumbnail if document was empty
            if (existingBitmaps.isEmpty()) {
                val docDir = File(getMetaDir(), id)
                saveThumbnail(newPageBitmaps.first(), File(docDir, "thumb.jpg"))
            }
            true
        } catch (e: Exception) { false }
    }

    fun updatePage(id: String, pageIndex: Int, newBitmap: Bitmap): Boolean {
        val pdfUri = getDocumentPdfUri(id) ?: return false
        return try {
            val pages = renderAllPages(pdfUri).toMutableList()
            if (pageIndex >= pages.size) return false
            pages[pageIndex] = newBitmap
            writePdfToUri(pdfUri, pages)
            // Update thumbnail if first page changed
            if (pageIndex == 0) {
                val docDir = File(getMetaDir(), id)
                saveThumbnail(newBitmap, File(docDir, "thumb.jpg"))
            }
            updateMetaFileSize(id, getPdfSize(pdfUri))
            true
        } catch (e: Exception) { false }
    }

    private fun renderAllPages(pdfUri: String): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        openPdfDescriptor(pdfUri, "r")?.use { fd ->
            val renderer = PdfRenderer(fd)
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()
                bitmaps.add(bmp)
            }
            renderer.close()
        }
        return bitmaps
    }

    private fun writePdfToUri(pdfUri: String, bitmaps: List<Bitmap>) {
        val pdfDocument = buildPdfDocument(bitmaps)
        if (pdfUri.startsWith("content://")) {
            val uri = Uri.parse(pdfUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val pending = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 1) }
                context.contentResolver.update(uri, pending, null, null)
            }
            context.contentResolver.openOutputStream(uri, "wt")?.use { pdfDocument.writeTo(it) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                context.contentResolver.update(uri, done, null, null)
            }
        } else {
            pdfDocument.writeTo(FileOutputStream(pdfUri))
        }
        pdfDocument.close()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun openPdfDescriptor(pdfPath: String, mode: String): ParcelFileDescriptor? {
        return if (pdfPath.startsWith("content://")) {
            context.contentResolver.openFileDescriptor(Uri.parse(pdfPath), mode)
        } else {
            val file = File(pdfPath)
            if (!file.exists()) return null
            ParcelFileDescriptor.open(
                file,
                if (mode == "r") ParcelFileDescriptor.MODE_READ_ONLY else ParcelFileDescriptor.MODE_READ_WRITE
            )
        }
    }

    private fun getDocumentPdfUri(id: String): String? {
        return try {
            val meta = JSONObject(File(File(getMetaDir(), id), "meta.json").readText())
            meta.optString("pdfUri").ifEmpty { null }
        } catch (e: Exception) { null }
    }

    private fun getPdfSize(pdfUri: String): Long {
        return if (pdfUri.startsWith("content://")) {
            try {
                val cursor = context.contentResolver.query(
                    Uri.parse(pdfUri), arrayOf(MediaStore.Downloads.SIZE), null, null, null
                )
                cursor?.use { if (it.moveToFirst()) it.getLong(0) else 0L } ?: 0L
            } catch (e: Exception) { 0L }
        } else {
            File(pdfUri).length()
        }
    }

    private fun updateMetaFileSize(id: String, sizeBytes: Long) {
        val metaFile = File(File(getMetaDir(), id), "meta.json")
        if (!metaFile.exists()) return
        try {
            val meta = JSONObject(metaFile.readText())
            meta.put("fileSize", formatFileSize(sizeBytes))
            meta.put("timestamp", System.currentTimeMillis())
            metaFile.writeText(meta.toString())
        } catch (_: Exception) {}
    }

    private fun formatFileSize(bytes: Long): String = when {
        bytes > 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
        bytes > 1024 -> "${"%.0f".format(bytes / 1024.0)} KB"
        else -> "$bytes B"
    }
}
