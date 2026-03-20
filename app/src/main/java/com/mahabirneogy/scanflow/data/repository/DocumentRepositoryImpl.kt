package com.mahabirneogy.scanflow.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.mahabirneogy.scanflow.data.source.PdfDataSource
import com.mahabirneogy.scanflow.domain.model.ScannedDocument
import com.mahabirneogy.scanflow.domain.repository.DocumentRepository

class DocumentRepositoryImpl(context: Context) : DocumentRepository {

    private val dataSource = PdfDataSource(context)

    override fun listDocuments(): List<ScannedDocument> = dataSource.listDocuments()
    override fun getDocument(id: String): ScannedDocument? = dataSource.getDocument(id)
    override fun saveDocument(bitmaps: List<Bitmap>, title: String, category: String): ScannedDocument =
        dataSource.savePdf(bitmaps, title, category)
    override fun deleteDocument(id: String): Boolean = dataSource.deleteDocument(id)
    override fun renameDocument(id: String, newTitle: String): Boolean = dataSource.renameDocument(id, newTitle)
    override fun addPage(id: String, bitmap: Bitmap): Boolean = dataSource.addPage(id, bitmap)
    override fun addPages(id: String, bitmaps: List<Bitmap>): Boolean = dataSource.addPages(id, bitmaps)
    override fun updatePage(id: String, pageIndex: Int, bitmap: Bitmap): Boolean =
        dataSource.updatePage(id, pageIndex, bitmap)
    override fun getPageCount(pdfPath: String): Int = dataSource.getPageCount(pdfPath)
    override fun renderPage(pdfPath: String, pageIndex: Int, width: Int): Bitmap? =
        dataSource.renderPage(pdfPath, pageIndex, width)
    override fun renderPages(pdfPath: String, pageIndices: List<Int>, width: Int): Map<Int, Bitmap> =
        dataSource.renderPages(pdfPath, pageIndices, width)
}
