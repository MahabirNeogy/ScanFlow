package com.mahabirneogy.scanflow.ui.screens.detail

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.mahabirneogy.scanflow.data.repository.DocumentRepositoryImpl
import com.mahabirneogy.scanflow.domain.model.ScannedDocument
import com.mahabirneogy.scanflow.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DocumentDetailViewModel(
    application: Application,
    private val documentId: String
) : AndroidViewModel(application) {

    private val repository: DocumentRepository = DocumentRepositoryImpl(application)

    private val _document = MutableStateFlow<ScannedDocument?>(null)
    val document: StateFlow<ScannedDocument?> = _document.asStateFlow()

    private val _pageCount = MutableStateFlow(0)
    val pageCount: StateFlow<Int> = _pageCount.asStateFlow()

    private val _pageBitmaps = MutableStateFlow<Map<Int, Bitmap>>(emptyMap())
    val pageBitmaps: StateFlow<Map<Int, Bitmap>> = _pageBitmaps.asStateFlow()

    init {
        loadDocument()
    }

    fun loadDocument() {
        viewModelScope.launch(Dispatchers.IO) {
            val doc = repository.getDocument(documentId) ?: return@launch
            _document.value = doc
            val count = repository.getPageCount(doc.pdfPath)
            _pageCount.value = count
            val bitmaps = repository.renderPages(doc.pdfPath, (0 until count).toList(), 400)
            _pageBitmaps.value = bitmaps
        }
    }

    fun renameDocument(newTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _document.value ?: return@launch
            if (newTitle.isBlank() || newTitle == current.title) return@launch
            repository.renameDocument(documentId, newTitle)
            _document.value = repository.getDocument(documentId)
        }
    }

    companion object {
        fun Factory(documentId: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return DocumentDetailViewModel(application, documentId) as T
            }
        }
    }
}
