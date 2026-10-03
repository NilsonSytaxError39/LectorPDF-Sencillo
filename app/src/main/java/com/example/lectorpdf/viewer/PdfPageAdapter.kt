package com.example.lectorpdf.viewer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.example.lectorpdf.R
import kotlin.math.sqrt
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import com.example.lectorpdf.search.PdfTextMatch

class PdfPageAdapter(
    private val renderer: PdfRenderer,
    private var targetPx: Int,
    private var invertPdf: Boolean
) : RecyclerView.Adapter<PdfPageAdapter.VH>() {

    private val executor = Executors.newSingleThreadExecutor()
    private val requestGeneration = AtomicInteger(0)
    private val cache = object : LruCache<String, Bitmap>(
        ((Runtime.getRuntime().maxMemory() / 1024L).toInt() / 8).coerceAtLeast(8 * 1024)
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return (value.byteCount / 1024).coerceAtLeast(1)
        }
    }

    private val invertFilter = ColorMatrixColorFilter(
        ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.pageImage)
        val highlights: PdfHighlightView = view.findViewById(R.id.pageHighlights)
        val loading: ProgressBar = view.findViewById(R.id.pageLoading)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_pdf_page, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = renderer.pageCount

    override fun onBindViewHolder(holder: VH, position: Int) {
        val key = "$position-$targetPx"
        holder.image.tag = key
        holder.image.setImageDrawable(null)
        holder.image.colorFilter = if (invertPdf) invertFilter else null
        holder.highlights.setMatches(searchMatches.filter { it.pageIndex == position })
        holder.loading.visibility = View.VISIBLE

        val cached = cache.get(key)
        if (cached != null) {
            holder.image.setImageBitmap(cached)
            holder.loading.visibility = View.GONE
            return
        }

        val myGen = requestGeneration.get()

        executor.execute {
            try {
                if (myGen != requestGeneration.get()) return@execute
                val bmp = renderPage(position)
                if (bmp != null) {
                    cache.put(key, bmp)
                    holder.image.post {
                        if (holder.image.tag == key) {
                            holder.image.setImageBitmap(bmp)
                            holder.loading.visibility = View.GONE
                        }
                    }
                }
            } catch (_: Throwable) {
                // Best-effort rendering: never crash the app on background rendering failures.
            }
        }
    }

    fun setTargetPx(px: Int) {
        if (px <= 0) return
        if (px == targetPx) return
        targetPx = px
        requestGeneration.incrementAndGet()
        cache.evictAll()
        notifyDataSetChanged()
    }

    fun setInvertPdf(enabled: Boolean) {
        if (invertPdf == enabled) return
        invertPdf = enabled
        notifyDataSetChanged()
    }

    private var searchMatches: List<PdfTextMatch> = emptyList()

    fun setSearchMatches(matches: List<PdfTextMatch>) {
        searchMatches = matches
        notifyDataSetChanged()
    }

    fun invalidateRendering() {
        requestGeneration.incrementAndGet()
    }

    private fun renderPage(index: Int): Bitmap? {
        return try {
            val page = renderer.openPage(index)
            val pageWidth = page.width
            val pageHeight = page.height

            val scale = if (pageWidth > 0) (targetPx.toFloat() / pageWidth.toFloat()) else 1f
            var bmpW = (pageWidth * scale).toInt().coerceAtLeast(1)
            var bmpH = (pageHeight * scale).toInt().coerceAtLeast(1)

            // Defensive cap to avoid OOM on very large pages.
            val maxPixels = 6_000_000L
            val pixels = bmpW.toLong() * bmpH.toLong()
            if (pixels > maxPixels && pixels > 0L) {
                val factor = sqrt(maxPixels.toDouble() / pixels.toDouble()).toFloat().coerceAtMost(1f)
                bmpW = (bmpW * factor).toInt().coerceAtLeast(1)
                bmpH = (bmpH * factor).toInt().coerceAtLeast(1)
            }

            val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
            // PdfRenderer may render with transparent background; ensure a white base for proper inversion.
            bitmap.eraseColor(0xFFFFFFFF.toInt())
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            bitmap
        } catch (_: OutOfMemoryError) {
            null
        } catch (_: Exception) {
            null
        }
    }

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        holder.image.setImageDrawable(null)
        holder.image.colorFilter = null
        holder.image.tag = null
        holder.highlights.setMatches(emptyList())
        holder.loading.visibility = View.GONE
    }

    fun shutdown() {
        executor.shutdownNow()
    }
}
