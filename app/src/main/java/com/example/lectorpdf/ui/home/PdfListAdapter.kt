package com.example.lectorpdf.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.LruCache
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lectorpdf.R
import com.example.lectorpdf.model.PdfItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class PdfListAdapter(
    items: List<PdfItem>,
    private val onClick: (PdfItem) -> Unit
) : RecyclerView.Adapter<PdfListAdapter.VH>() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newFixedThreadPool(2)
    private val thumbCache = LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 32).toInt())

    private val allItems = items.toMutableList()
    private val visibleItems = items.toMutableList()
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.pdfTitle)
        val thumb: ImageView = view.findViewById(R.id.pdfThumb)
        val meta: TextView = view.findViewById(R.id.pdfMeta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_pdf, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = visibleItems[position]
        val uri = item.uri
        holder.title.text = item.title
        holder.meta.text = "${formatDate(item.dateModifiedSeconds)} · ${formatBytes(item.sizeBytes)}"
        holder.thumb.setImageResource(R.mipmap.ic_launcher)
        holder.thumb.tag = uri.toString()

        val key = uri.toString()
        val cached = thumbCache.get(key)
        if (cached != null) {
            holder.thumb.setImageBitmap(cached)
        } else {
            executor.execute {
                try {
                    val bmp = loadThumbnail(holder.itemView.context, uri)
                    if (bmp != null) {
                        thumbCache.put(key, bmp)
                        mainHandler.post {
                            if (holder.thumb.tag == key) {
                                holder.thumb.setImageBitmap(bmp)
                            }
                        }
                    }
                } catch (_: Throwable) {
                    // Never crash the app due to thumbnail rendering failures.
                }
            }
        }
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = visibleItems.size

    fun update(newItems: List<PdfItem>) {
        allItems.clear()
        allItems.addAll(newItems)
        visibleItems.clear()
        visibleItems.addAll(newItems)
        notifyDataSetChanged()
    }

    fun filterByTitle(query: String) {
        val q = query.trim().lowercase(Locale.getDefault())
        visibleItems.clear()
        if (q.isBlank()) {
            visibleItems.addAll(allItems)
        } else {
            visibleItems.addAll(allItems.filter { it.title.lowercase(Locale.getDefault()).contains(q) })
        }
        notifyDataSetChanged()
    }

    fun shutdown() {
        executor.shutdownNow()
    }

    private fun loadThumbnail(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                context.contentResolver.loadThumbnail(uri, Size(200, 200), null)
            } else {
                renderFirstPageThumbnail(context, uri)
            }
        } catch (_: OutOfMemoryError) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun renderFirstPageThumbnail(context: Context, uri: Uri): Bitmap? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd == null) return null
            renderer = PdfRenderer(pfd)
            if (renderer.pageCount <= 0) return null
            val page = renderer.openPage(0)

            val maxSize = 200
            val ratio = page.width.toFloat() / page.height.toFloat()
            val width: Int
            val height: Int
            if (ratio >= 1f) {
                width = maxSize
                height = (maxSize / ratio).toInt().coerceAtLeast(1)
            } else {
                height = maxSize
                width = (maxSize * ratio).toInt().coerceAtLeast(1)
            }

            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bmp
        } catch (_: Exception) {
            null
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    private fun formatDate(dateModifiedSeconds: Long): String {
        if (dateModifiedSeconds <= 0) return ""
        return dateFormat.format(Date(dateModifiedSeconds * 1000L))
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        return when {
            bytes >= gb -> String.format(Locale.getDefault(), "%.1f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.getDefault(), "%.1f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.getDefault(), "%.0f KB", bytes / kb)
            else -> "$bytes B"
        }
    }
}
