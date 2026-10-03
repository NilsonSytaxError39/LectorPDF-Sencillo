package com.example.lectorpdf.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.lectorpdf.model.MarkedPdfItem

object MarkedPdfsStore {

    private const val PREFS_NAME = "pdf_prefs"
    private const val KEY_MARKED = "marked_entries"
    private const val MAX_MARKED = 200

    /**
     * Format: <ts>|<base64(uri)>|<base64(title)>
     */
    fun setMarked(context: Context, uri: Uri, title: String?, marked: Boolean) {
        val safeTitle = (title?.trim()).takeUnless { it.isNullOrEmpty() } ?: "PDF"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_MARKED, emptySet())?.toMutableSet() ?: mutableSetOf()

        val uriB64 = b64(uri.toString())
        current.removeAll { entry ->
            val parts = entry.split('|')
            parts.size >= 2 && parts[1] == uriB64
        }

        if (marked) {
            val ts = System.currentTimeMillis()
            current.add("$ts|$uriB64|${b64(safeTitle)}")
        }

        val pruned = current
            .mapNotNull { parseEntry(it) }
            .sortedByDescending { it.first }
            .take(MAX_MARKED)
            .map { it.second }
            .toSet()

        prefs.edit().putStringSet(KEY_MARKED, pruned).apply()
    }

    fun isMarked(context: Context, uri: Uri): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getStringSet(KEY_MARKED, emptySet()) ?: emptySet()
        val uriB64 = b64(uri.toString())
        return raw.any { entry ->
            val parts = entry.split('|')
            parts.size >= 2 && parts[1] == uriB64
        }
    }

    fun getMarked(context: Context): List<MarkedPdfItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getStringSet(KEY_MARKED, emptySet()) ?: emptySet()

        return raw.mapNotNull { entry ->
            val parts = entry.split('|')
            if (parts.size < 3) return@mapNotNull null
            val ts = parts[0].toLongOrNull() ?: return@mapNotNull null
            val uri = try {
                Uri.parse(unb64(parts[1]))
            } catch (_: Exception) {
                return@mapNotNull null
            }
            val title = try {
                unb64(parts[2])
            } catch (_: Exception) {
                "PDF"
            }
            MarkedPdfItem(title = title, uri = uri, markedMillis = ts)
        }.sortedByDescending { it.markedMillis }
    }

    private fun parseEntry(entry: String): Pair<Long, String>? {
        val parts = entry.split('|')
        if (parts.size < 3) return null
        val ts = parts[0].toLongOrNull() ?: return null
        val normalized = "${parts[0]}|${parts[1]}|${parts[2]}"
        return ts to normalized
    }

    private fun b64(value: String): String =
        Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    private fun unb64(value: String): String =
        String(Base64.decode(value, Base64.NO_WRAP), Charsets.UTF_8)
}
