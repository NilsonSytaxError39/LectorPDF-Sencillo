package com.example.lectorpdf.model

import android.net.Uri

data class PersonalBookmark(
    val uri: Uri,
    val pageIndex: Int,
    val title: String,
    val createdAtMillis: Long
)
