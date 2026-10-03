package com.example.lectorpdf.model

import android.net.Uri

data class PdfItem(
    val title: String,
    val uri: Uri,
    val sizeBytes: Long,
    /** seconds since epoch as provided by MediaStore DATE_MODIFIED */
    val dateModifiedSeconds: Long
)
