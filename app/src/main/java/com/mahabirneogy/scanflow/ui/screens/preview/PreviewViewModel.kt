package com.mahabirneogy.scanflow.ui.screens.preview

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mahabirneogy.scanflow.data.repository.DocumentRepositoryImpl
import com.mahabirneogy.scanflow.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PreviewViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DocumentRepository = DocumentRepositoryImpl(application)

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun saveDocument(
        originalBitmaps: List<Bitmap?>,
        rotations: Map<Int, Float>,
        filters: Map<Int, String>,
        imagePaths: List<String>,
        title: String,
        category: String,
        onDone: () -> Unit
    ) {
        _isSaving.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val processedBitmaps = originalBitmaps.mapIndexedNotNull { idx, bmp ->
                bmp?.let { applyProcessing(it, rotations[idx] ?: 0f, filters[idx] ?: "Original") }
            }
            if (processedBitmaps.isNotEmpty()) {
                repository.saveDocument(processedBitmaps, title, category)
            }
            imagePaths.forEach { File(it).delete() }
            _isSaving.value = false
            withContext(Dispatchers.Main) { onDone() }
        }
    }
}
