package com.example.scanflow.domain.repository

import android.graphics.Bitmap
import com.example.scanflow.domain.model.ScannedDocument

interface DocumentRepository {
    fun listDocuments(): List<ScannedDocument>
    fun getDocument(id: String): ScannedDocument?
    fun saveDocument(bitmaps: List<Bitmap>, title: String, category: String): ScannedDocument
    fun deleteDocument(id: String): Boolean
    fun renameDocument(id: String, newTitle: String): Boolean
    fun addPage(id: String, bitmap: Bitmap): Boolean
    fun addPages(id: String, bitmaps: List<Bitmap>): Boolean
    fun updatePage(id: String, pageIndex: Int, bitmap: Bitmap): Boolean
    fun getPageCount(pdfPath: String): Int
    fun renderPage(pdfPath: String, pageIndex: Int, width: Int): Bitmap?
    fun renderPages(pdfPath: String, pageIndices: List<Int>, width: Int): Map<Int, Bitmap>
}
