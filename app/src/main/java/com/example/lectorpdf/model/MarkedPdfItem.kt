package com.example.lectorpdf.model

import android.net.Uri

data class MarkedPdfItem(
    val title: String,
    val uri: Uri,
    val markedMillis: Long
)
