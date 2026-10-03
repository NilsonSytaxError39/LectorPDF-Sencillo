package com.example.lectorpdf.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.example.lectorpdf.model.PdfItem
import java.util.Locale
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

object PdfScanner {

    fun scanMediaStore(context: Context): List<PdfItem> {
        val collection = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        // Some devices/files report PDFs as octet-stream; add extension fallback.
        val selection = "(${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR lower(${MediaStore.Files.FileColumns.DISPLAY_NAME}) LIKE ?)"
        val selectionArgs = arrayOf("application/pdf", "%.pdf")
        val sort = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        val results = mutableListOf<PdfItem>()
        context.contentResolver.query(collection, projection, selection, selectionArgs, sort)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val name = (c.getString(nameCol) ?: "PDF").trim()
                val size = c.getLong(sizeCol)
                val dateModified = c.getLong(dateCol)
                val uri = ContentUris.withAppendedId(collection, id)
                results.add(PdfItem(title = name, uri = uri, sizeBytes = size, dateModifiedSeconds = dateModified))
            }
        }
        return results
    }

    suspend fun scanTree(context: Context, treeUri: Uri): List<PdfItem> {
        // Normalize to the tree document URI if needed
        val normalized = try {
            if (DocumentsContract.isTreeUri(treeUri)) treeUri else DocumentsContract.buildTreeDocumentUri(treeUri.authority!!, DocumentsContract.getTreeDocumentId(treeUri))
        } catch (_: Exception) {
            treeUri
        }

        val root = DocumentFile.fromTreeUri(context, normalized) ?: return emptyList()
        val out = mutableListOf<PdfItem>()
        walk(root, out, 0)
        out.sortByDescending { it.dateModifiedSeconds }
        return out
    }

    private suspend fun walk(dir: DocumentFile, out: MutableList<PdfItem>, depth: Int) {
        currentCoroutineContext().ensureActive()
        if (depth > 64) return
        if (!dir.isDirectory) return
        val children = try {
            dir.listFiles()
        } catch (_: RuntimeException) {
            return
        }
        for (child in children) {
            currentCoroutineContext().ensureActive()
            if (child.isDirectory) {
                walk(child, out, depth + 1)
            } else {
                val name = child.name ?: continue
                val lower = name.lowercase(Locale.getDefault())
                val isPdf = lower.endsWith(".pdf") || (child.type?.lowercase(Locale.getDefault()) == "application/pdf")
                if (isPdf) {
                    val size = try { child.length() } catch (_: Exception) { 0L }
                    val lastModifiedMs = try { child.lastModified() } catch (_: Exception) { 0L }
                    out.add(
                        PdfItem(
                            title = name,
                            uri = child.uri,
                            sizeBytes = size,
                            dateModifiedSeconds = (lastModifiedMs / 1000L)
                        )
                    )
                }
            }
        }
    }
}
