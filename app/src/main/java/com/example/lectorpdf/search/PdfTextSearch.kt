package com.example.lectorpdf.search

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.util.Locale
import java.util.concurrent.CancellationException

data class PdfTextMatch(
    val pageIndex: Int,
    val occurrenceOnPage: Int,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

class PdfTextIndex internal constructor(
    internal val pages: List<PdfTextPage>
)

internal data class PdfTextPage(
    val text: String,
    val positions: List<TextPosition>,
    val width: Float,
    val height: Float
)

object PdfTextSearch {

    fun buildIndex(
        context: Context,
        uri: Uri,
        onProgress: ((currentPage: Int, totalPages: Int) -> Unit)? = null,
        isCancelled: (() -> Boolean)? = null
    ): PdfTextIndex {
        PDFBoxResourceLoader.init(context.applicationContext)
        context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { document ->
                val totalPages = document.numberOfPages
                val texts = Array(totalPages) { StringBuilder() }
                val positions = Array(totalPages) { mutableListOf<TextPosition>() }
                val stripper = object : PDFTextStripper() {
                    private var lastReportedPage = 0

                    override fun writeString(
                        value: String,
                        textPositions: MutableList<TextPosition>
                    ) {
                        if (isCancelled?.invoke() == true) {
                            throw CancellationException("Búsqueda cancelada")
                        }
                        val pageIndex = (currentPageNo - 1).coerceIn(0, totalPages - 1)
                        textPositions.forEach { position ->
                            val unicode = position.unicode
                            texts[pageIndex].append(unicode)
                            repeat(unicode.length.coerceAtLeast(1)) {
                                positions[pageIndex].add(position)
                            }
                        }
                        if (pageIndex + 1 != lastReportedPage) {
                            lastReportedPage = pageIndex + 1
                            onProgress?.invoke(lastReportedPage, totalPages)
                        }
                        super.writeString(value, textPositions)
                    }
                }.apply {
                    startPage = 1
                    endPage = totalPages
                }
                stripper.getText(document)

                val pages = texts.indices.map { pageIndex ->
                    if (isCancelled?.invoke() == true) {
                        throw CancellationException("Búsqueda cancelada")
                    }
                    val page = document.getPage(pageIndex)
                    val cropBox = page.cropBox
                    val rotation = ((page.rotation % 360) + 360) % 360
                    val pageWidth = if (rotation == 90 || rotation == 270) {
                        cropBox.height
                    } else {
                        cropBox.width
                    }
                    val pageHeight = if (rotation == 90 || rotation == 270) {
                        cropBox.width
                    } else {
                        cropBox.height
                    }
                    PdfTextPage(
                        text = texts[pageIndex].toString(),
                        positions = positions[pageIndex].toList(),
                        width = pageWidth.coerceAtLeast(1f),
                        height = pageHeight.coerceAtLeast(1f)
                    )
                }
                return PdfTextIndex(pages)
            }
        } ?: throw IllegalStateException("No se pudo leer el PDF")
    }

    fun find(index: PdfTextIndex, query: String): List<PdfTextMatch> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return emptyList()

        val lowerQuery = normalizedQuery.lowercase(Locale.getDefault())
        val matches = mutableListOf<PdfTextMatch>()
        index.pages.forEachIndexed { pageIndex, page ->
            val lowerText = page.text.lowercase(Locale.getDefault())
            var fromIndex = 0
            var occurrence = 0
            while (true) {
                val found = lowerText.indexOf(lowerQuery, fromIndex)
                if (found < 0) break
                val end = (found + lowerQuery.length - 1).coerceAtMost(page.positions.lastIndex)
                if (found <= page.positions.lastIndex && end >= found) {
                    val matchedPositions = page.positions.subList(found, end + 1)
                    val left = matchedPositions.minOf { it.xDirAdj }.coerceAtLeast(0f)
                    val top = matchedPositions.minOf {
                        it.yDirAdj - it.heightDir
                    }.coerceAtLeast(0f)
                    val right = matchedPositions.maxOf {
                        it.xDirAdj + it.widthDirAdj
                    }.coerceAtMost(page.width)
                    val bottom = matchedPositions.maxOf {
                        it.yDirAdj
                    }.coerceAtMost(page.height)
                    matches += PdfTextMatch(
                        pageIndex = pageIndex,
                        occurrenceOnPage = occurrence++,
                        left = left / page.width,
                        top = top / page.height,
                        right = right / page.width,
                        bottom = bottom / page.height
                    )
                }
                fromIndex = found + lowerQuery.length
            }
        }
        return matches
    }
}
