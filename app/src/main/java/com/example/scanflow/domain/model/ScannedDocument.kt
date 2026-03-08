package com.example.scanflow.domain.model

data class ScannedDocument(
    val id: String,
    val title: String,
    val category: String,
    val dateCreated: String,
    val fileSize: String,
    val pdfPath: String,
    val thumbnailPath: String,
    val timestamp: Long = 0L
)
