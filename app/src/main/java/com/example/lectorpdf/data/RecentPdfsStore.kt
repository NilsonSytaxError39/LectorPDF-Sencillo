package com.example.lectorpdf.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.example.lectorpdf.model.RecentPdfItem

object RecentPdfsStore {

    private const val PREFS_NAME = "pdf_prefs"
    private const val KEY_RECENTS = "recent_entries"
    private const val MAX_RECENTS = 20

    /**
     * Stores/update a recent entry. Uses a StringSet under the hood, then we sort by timestamp.
     * Format: <ts>|<base64(uri)>|<base64(title)>
     */
    fun recordOpened(context: Context, uri: Uri, title: String?) {
        val safeTitle = (title?.trim()).takeUnless { it.isNullOrEmpty() }
            ?: resolveDisplayName(context, uri)
            ?: "PDF"
        val ts = System.currentTimeMillis()

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_RECENTS, emptySet())?.toMutableSet() ?: mutableSetOf()

        // Remove any existing entry for the same uri.
        val uriB64 = b64(uri.toString())
        current.removeAll { entry ->
            val parts = entry.split('|')
            parts.size >= 2 && parts[1] == uriB64
        }

        current.add("$ts|$uriB64|${b64(safeTitle)}")

        // Prune to MAX_RECENTS by timestamp.
        val pruned = current
            .mapNotNull { parseEntry(it) }
            .sortedByDescending { it.first }
            .take(MAX_RECENTS)
            .map { it.second }
            .toSet()

        prefs.edit().putStringSet(KEY_RECENTS, pruned).apply()
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                        ?.trim()
                        ?.takeUnless { it.isEmpty() }
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            uri.lastPathSegment?.substringAfterLast('/').orEmpty()
                .trim()
                .takeUnless { it.isEmpty() }
        }
    }

    fun getRecents(context: Context): List<RecentPdfItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getStringSet(KEY_RECENTS, emptySet()) ?: emptySet()

        return raw.mapNotNull { entry ->
            val parsed = parseEntry(entry) ?: return@mapNotNull null
            val ts = parsed.first
            val parts = parsed.second.split('|')
            if (parts.size < 3) return@mapNotNull null
            val uri = try {
                Uri.parse(unb64(parts[1]))
            } catch (_: Exception) {
                return@mapNotNull null
            }
            val storedTitle = try {
                unb64(parts[2])
            } catch (_: Exception) {
                "PDF"
            }
            val title = storedTitle
                .takeUnless { it.isBlank() || it == "PDF" }
                ?: resolveDisplayName(context, uri)
                ?: "PDF"
            RecentPdfItem(
                title = title,
                uri = uri,
                lastOpenedMillis = ts,
                isAvailable = isReadable(context, uri)
            )
        }.sortedByDescending { it.lastOpenedMillis }
    }

    fun remove(context: Context, uri: Uri) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriB64 = b64(uri.toString())
        val remaining = (prefs.getStringSet(KEY_RECENTS, emptySet()) ?: emptySet())
            .filterNot { entry ->
                val parts = entry.split('|')
                parts.size >= 2 && parts[1] == uriB64
            }
            .toSet()
        prefs.edit().putStringSet(KEY_RECENTS, remaining).remove(uri.toString()).apply()
    }

    private fun isReadable(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
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
