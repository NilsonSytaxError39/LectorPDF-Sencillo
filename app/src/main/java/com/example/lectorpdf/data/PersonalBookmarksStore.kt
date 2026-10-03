package com.example.lectorpdf.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.lectorpdf.model.PersonalBookmark

object PersonalBookmarksStore {

    private const val PREFS_NAME = "pdf_prefs"
    private const val KEY_BOOKMARKS = "personal_bookmarks"

    fun getForPdf(context: Context, uri: Uri): List<PersonalBookmark> {
        val uriKey = encode(uri.toString())
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_BOOKMARKS, emptySet()) ?: emptySet()
        return raw.mapNotNull { entry ->
            val parts = entry.split('|')
            if (parts.size < 4 || parts[1] != uriKey) return@mapNotNull null
            val page = parts[0].toIntOrNull() ?: return@mapNotNull null
            val created = parts[2].toLongOrNull() ?: return@mapNotNull null
            val title = runCatching { decode(parts[3]) }.getOrNull() ?: return@mapNotNull null
            PersonalBookmark(uri, page, title, created)
        }.sortedBy { it.pageIndex }
    }

    fun upsert(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        title: String
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriKey = encode(uri.toString())
        val current = (prefs.getStringSet(KEY_BOOKMARKS, emptySet()) ?: emptySet())
            .filterNot {
                val parts = it.split('|')
                parts.size >= 2 && parts[0] == pageIndex.toString() && parts[1] == uriKey
            }
            .toMutableSet()
        current += "$pageIndex|$uriKey|${System.currentTimeMillis()}|${encode(title)}"
        prefs.edit().putStringSet(KEY_BOOKMARKS, current).apply()
    }

    fun remove(context: Context, bookmark: PersonalBookmark) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriKey = encode(bookmark.uri.toString())
        val remaining = (prefs.getStringSet(KEY_BOOKMARKS, emptySet()) ?: emptySet())
            .filterNot {
                val parts = it.split('|')
                parts.size >= 2 &&
                    parts[0] == bookmark.pageIndex.toString() &&
                    parts[1] == uriKey
            }
            .toSet()
        prefs.edit().putStringSet(KEY_BOOKMARKS, remaining).apply()
    }

    private fun encode(value: String): String =
        Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    private fun decode(value: String): String =
        String(Base64.decode(value, Base64.NO_WRAP), Charsets.UTF_8)
}
