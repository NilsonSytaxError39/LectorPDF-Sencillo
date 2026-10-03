package com.example.lectorpdf.model

import android.net.Uri

data class RecentPdfItem(
    val title: String,
    val uri: Uri,
    val lastOpenedMillis: Long,
    val isAvailable: Boolean
)
