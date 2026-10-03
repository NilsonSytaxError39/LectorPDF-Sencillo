package com.example.lectorpdf.search

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem

data class PdfOutlineEntry(
    val title: String,
    val level: Int,
    val pageIndex: Int?
)

object PdfOutlineReader {

    fun read(context: Context, uri: Uri): List<PdfOutlineEntry> {
        PDFBoxResourceLoader.init(context.applicationContext)
        context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { document ->
                val outline = document.documentCatalog.documentOutline ?: return emptyList()
                val entries = mutableListOf<PdfOutlineEntry>()
                appendChildren(document, outline.children(), 0, entries)
                return entries
            }
        } ?: throw IllegalStateException("No se pudo leer el PDF")
    }

    private fun appendChildren(
        document: PDDocument,
        children: Iterable<PDOutlineItem>,
        level: Int,
        entries: MutableList<PdfOutlineEntry>
    ) {
        for (item in children) {
            val destinationPage = try {
                item.findDestinationPage(document)
            } catch (_: RuntimeException) {
                null
            }
            val pageIndex = destinationPage?.let { page ->
                document.pages.indexOf(page).takeIf { it >= 0 }
            }
            entries += PdfOutlineEntry(
                title = item.title?.trim().orEmpty().ifEmpty { "Sin título" },
                level = level,
                pageIndex = pageIndex
            )
            appendChildren(document, item.children(), level + 1, entries)
        }
    }
}
