package com.example.lectorpdf.ui.notifications

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.lectorpdf.R
import com.example.lectorpdf.model.MarkedPdfItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MarkedPdfListAdapter(
    items: List<MarkedPdfItem>,
    private val onClick: (MarkedPdfItem) -> Unit
) : RecyclerView.Adapter<MarkedPdfListAdapter.VH>() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newFixedThreadPool(2)
    private val thumbCache = object : LruCache<String, Bitmap>(CACHE_SIZE_KB) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    private val visibleItems = items.toMutableList()
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.pdfTitle)
        val thumb: ImageView = view.findViewById(R.id.pdfThumb)
        val meta: TextView = view.findViewById(R.id.pdfMeta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_pdf, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = visibleItems.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = visibleItems[position]
        val uri = item.uri

        holder.title.text = item.title
        holder.meta.text = "Marcado: ${dateFormat.format(Date(item.markedMillis))}"
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
                    // ignore
                }
            }
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }

    fun update(newItems: List<MarkedPdfItem>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = visibleItems.size
            override fun getNewListSize(): Int = newItems.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return visibleItems[oldItemPosition].uri == newItems[newItemPosition].uri
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return visibleItems[oldItemPosition] == newItems[newItemPosition]
            }
        })

        visibleItems.clear()
        visibleItems.addAll(newItems)
        diff.dispatchUpdatesTo(this)
    }

    fun shutdown() {
        executor.shutdownNow()
    }

    private fun loadThumbnail(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                context.contentResolver.loadThumbnail(uri, Size(200, 200), null)
            } else {
                null
            }
        } catch (_: OutOfMemoryError) {
            null
        } catch (_: Exception) {
            null
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        executor.shutdownNow()
        thumbCache.evictAll()
        mainHandler.removeCallbacksAndMessages(null)
    }

    private companion object {
        // Use ~1/8th of the available memory for the bitmap cache (in KB).
        private val CACHE_SIZE_KB = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    }
}
