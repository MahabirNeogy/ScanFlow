package com.example.scanflow.ui.screens.files

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.scanflow.data.repository.DocumentRepositoryImpl
import com.example.scanflow.domain.model.ScannedDocument
import com.example.scanflow.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FilesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DocumentRepository = DocumentRepositoryImpl(application)

    private val _documents = MutableStateFlow<List<ScannedDocument>>(emptyList())
    val documents: StateFlow<List<ScannedDocument>> = _documents.asStateFlow()

    init {
        loadDocuments()
    }

    fun loadDocuments() {
        viewModelScope.launch(Dispatchers.IO) {
            _documents.value = repository.listDocuments()
        }
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDocument(id)
            _documents.value = repository.listDocuments()
        }
    }
}
