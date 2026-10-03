package com.example.lectorpdf.viewer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.lectorpdf.search.PdfTextMatch

class PdfHighlightView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(110, 255, 213, 0)
        style = Paint.Style.FILL
    }
    private var matches: List<PdfTextMatch> = emptyList()

    fun setMatches(value: List<PdfTextMatch>) {
        matches = value
        visibility = if (value.isEmpty()) GONE else VISIBLE
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        matches.forEach { match ->
            canvas.drawRect(
                match.left * width,
                match.top * height,
                match.right * width,
                match.bottom * height,
                paint
            )
        }
    }
}
