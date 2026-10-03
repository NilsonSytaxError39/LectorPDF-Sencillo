package com.example.lectorpdf

import android.graphics.pdf.PdfRenderer
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lectorpdf.viewer.PdfPageAdapter
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.doOnLayout
import com.google.android.material.appbar.MaterialToolbar
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import com.example.lectorpdf.data.RecentPdfsStore
import com.example.lectorpdf.data.PersonalBookmarksStore
import com.example.lectorpdf.model.PersonalBookmark
import android.widget.Toast
import android.widget.EditText
import android.text.InputType
import androidx.appcompat.app.AlertDialog
import android.view.ScaleGestureDetector
import android.graphics.Color
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.FrameLayout
import kotlin.math.roundToInt
import com.example.lectorpdf.search.PdfTextIndex
import com.example.lectorpdf.search.PdfTextMatch
import com.example.lectorpdf.search.PdfTextSearch
import com.example.lectorpdf.search.PdfOutlineEntry
import com.example.lectorpdf.search.PdfOutlineReader
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class PdfViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TITLE = "extra_title"

        private const val STATE_PAGE_INDEX = "state_page_index"
    }

    private val logTag = "PdfViewerActivity"

    private var renderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var fallbackContainer: LinearLayout
    private lateinit var fallbackImage: ImageView
    private lateinit var fallbackText: TextView
    private lateinit var toolbar: MaterialToolbar

    private lateinit var pageControls: View
    private lateinit var pageCounterText: TextView
    private lateinit var pageSeekBar: SeekBar

    private var currentFirstVisiblePage = 0
    private var isHorizontal = false
    private var isAutoOrientation = false
    private var isDarkMode = false
    private var readingMode = "light"
    private var invertPdf = false
    private var keepScreenOn = false
    private var isFullscreen = false
    private var initialWindowBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    private var readingBrightness = 1f
    private var uiChromeVisible = true
    private var adapter: PdfPageAdapter? = null
    private var layoutManager: LinearLayoutManager? = null
    private var gestureDetector: GestureDetector? = null
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var zoomScale = 1f

    private var userSeeking = false
    private var isRestoringPosition = false
    private var lastReportedPage = -1
    private var pendingScrollTarget: Int? = null

    private var openedTitle: String? = null
    private var pendingRestorePage: Int? = null
    private val searchExecutor = Executors.newSingleThreadExecutor()
    private val searchCancellation = AtomicBoolean(false)
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var searchDialog: AlertDialog? = null
    private var searchMatches: List<PdfTextMatch> = emptyList()
    private var activeSearchMatch = -1
    private var searchStatus: TextView? = null
    private var searchPrevious: Button? = null
    private var searchNext: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            setContentView(R.layout.activity_pdf_viewer)

            val viewerRoot: View = findViewById(R.id.viewerRoot)
            toolbar = findViewById(R.id.viewerToolbar)
            initialWindowBrightness = window.attributes.screenBrightness
            readingBrightness = initialWindowBrightness.takeUnless { it < 0f }
                ?.coerceIn(0.2f, 1f)
                ?: 1f
            ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
                val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                view.setPadding(view.paddingLeft, topInset, view.paddingRight, view.paddingBottom)
                insets
            }
            ViewCompat.requestApplyInsets(toolbar)
            setSupportActionBar(toolbar)

            recyclerView = findViewById(R.id.pagesRecyclerView)
            fallbackContainer = findViewById(R.id.fallbackContainer)
            fallbackImage = findViewById(R.id.fallbackImage)
            fallbackText = findViewById(R.id.fallbackText)

            pageControls = findViewById(R.id.pageControls)
            pageCounterText = findViewById(R.id.pageCounterText)
            pageSeekBar = findViewById(R.id.pageSeekBar)
            val initialPageControlsPadding = intArrayOf(
                pageControls.paddingLeft,
                pageControls.paddingTop,
                pageControls.paddingRight,
                pageControls.paddingBottom
            )
            val initialRecyclerPadding = intArrayOf(
                recyclerView.paddingLeft,
                recyclerView.paddingTop,
                recyclerView.paddingRight,
                recyclerView.paddingBottom
            )
            val initialRootPadding = intArrayOf(
                viewerRoot.paddingLeft,
                viewerRoot.paddingTop,
                viewerRoot.paddingRight,
                viewerRoot.paddingBottom
            )
            ViewCompat.setOnApplyWindowInsetsListener(viewerRoot) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                viewerRoot.setPadding(
                    initialRootPadding[0] + systemBars.left,
                    initialRootPadding[1],
                    initialRootPadding[2] + systemBars.right,
                    initialRootPadding[3] + systemBars.bottom
                )
                pageControls.setPadding(
                    initialPageControlsPadding[0],
                    initialPageControlsPadding[1],
                    initialPageControlsPadding[2],
                    initialPageControlsPadding[3]
                )
                recyclerView.setPadding(
                    initialRecyclerPadding[0],
                    initialRecyclerPadding[1],
                    initialRecyclerPadding[2],
                    initialRecyclerPadding[3]
                )
                insets
            }
            ViewCompat.requestApplyInsets(viewerRoot)

            val uri = intent?.data
            if (uri == null) {
                finish()
                return
            }

            openedTitle = intent?.getStringExtra(EXTRA_TITLE)

            try {
                fileDescriptor = contentResolver.openFileDescriptor(uri, "r")
                fileDescriptor?.let { renderer = PdfRenderer(it) }
            } catch (e: Exception) {
                Log.e(logTag, "openFileDescriptor/PdfRenderer failed for uri=$uri", e)
                showFallback("No se pudo abrir el PDF")
                return
            }

            val r = renderer
            if (r == null || r.pageCount <= 0) {
                showFallback("PDF no compatible")
                return
            }

            RecentPdfsStore.recordOpened(this, uri, openedTitle)

            val prefs = getSharedPreferences("pdf_prefs", MODE_PRIVATE)
            val savedPage = savedInstanceState
                ?.takeIf { it.containsKey(STATE_PAGE_INDEX) }
                ?.getInt(STATE_PAGE_INDEX)
            val persistedPage = if (prefs.contains(uri.toString())) {
                prefs.getInt(uri.toString(), 0)
            } else {
                savedPage ?: 0
            }
            currentFirstVisiblePage = persistedPage.coerceIn(0, r.pageCount - 1)
            pendingRestorePage = currentFirstVisiblePage.takeIf { it > 0 }
            isDarkMode = prefs.getBoolean("viewer_dark_mode", false)
            readingMode = prefs.getString("viewer_reading_mode", if (isDarkMode) "dark" else "light") ?: "light"
            invertPdf = prefs.getBoolean("viewer_invert_pdf", false)
            keepScreenOn = prefs.getBoolean("viewer_keep_screen_on", false)
            applyReadingMode()
            applyKeepScreenOn()
            isFullscreen = prefs.getBoolean("viewer_fullscreen", false)

            val orientationMode = prefs.getString("viewer_orientation_mode", "auto") ?: "auto"
            isAutoOrientation = orientationMode == "auto"
            isHorizontal = orientationMode == "landscape"
            applyOrientationMode(orientationMode)

            setupRecycler(r)
            applyReadingMode()
            setupPageControls(pageCount = r.pageCount)
            applyFullscreen(isFullscreen)

            // Important for rotation: recompute target size after layout + restore page.
            isRestoringPosition = true
            recyclerView.doOnLayout {
                adapter?.setTargetPx(computeTargetPx())
                layoutManager?.scrollToPosition(currentFirstVisiblePage)
                pendingScrollTarget = currentFirstVisiblePage
                waitForScrollTarget(currentFirstVisiblePage, r.pageCount) {
                    pendingRestorePage?.let { page ->
                        Toast.makeText(this, "Retomando en página ${page + 1}", Toast.LENGTH_SHORT).show()
                        pendingRestorePage = null
                    }
                }
            }

            // Tap anywhere to toggle fullscreen.
            gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    isFullscreen = !isFullscreen
                    uiChromeVisible = !isFullscreen
                    applyFullscreen(isFullscreen)
                    invalidateOptionsMenu()
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    setZoom(if (zoomScale > 1f) 1f else 2f, e.x, e.y)
                    return true
                }
            })
            scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    setZoom(
                        (zoomScale * detector.scaleFactor).coerceIn(1f, 5f),
                        detector.focusX,
                        detector.focusY
                    )
                    return true
                }
            })

            recyclerView.setOnTouchListener { _, event ->
                scaleGestureDetector?.onTouchEvent(event)
                gestureDetector?.onTouchEvent(event)
                false
            }

            // Keep current position updated
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING || newState == RecyclerView.SCROLL_STATE_SETTLING) {
                        adapter?.invalidateRendering()
                    }
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING && !isRestoringPosition) {
                        pendingScrollTarget = null
                    }
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (isRestoringPosition || userSeeking) return
                    reportCurrentPage(pageCount = r.pageCount)
                }
            })
        } catch (t: Throwable) {
            Log.e(logTag, "Unexpected crash prevented in onCreate", t)
            try {
                showFallback("Error al abrir el PDF (${t.javaClass.simpleName})")
            } catch (_: Throwable) {
                // If views aren't ready, just finish to avoid a hard crash loop.
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.pdf_viewer_menu, menu)
        val pageJumpInput = menu.findItem(R.id.action_jump_page)
            ?.actionView as? EditText
        pageJumpInput?.setOnEditorActionListener { view, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                jumpToPageFromInput(view as EditText)
                true
            } else {
                false
            }
        }
        menu.findItem(R.id.action_dark_mode)?.isChecked = isDarkMode
        menu.findItem(R.id.action_light_mode)?.isChecked = readingMode == "light"
        menu.findItem(R.id.action_sepia_mode)?.isChecked = readingMode == "sepia"
        menu.findItem(R.id.action_invert_pdf)?.isChecked = invertPdf
        menu.findItem(R.id.action_keep_screen_on)?.isChecked = keepScreenOn
        menu.findItem(R.id.action_auto_orientation)?.isChecked = isAutoOrientation
        menu.findItem(R.id.action_portrait_orientation)?.isChecked =
            !isAutoOrientation && !isHorizontal
        menu.findItem(R.id.action_landscape_orientation)?.isChecked =
            !isAutoOrientation && isHorizontal
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_auto_orientation -> {
                setOrientationMode("auto")
                true
            }
            R.id.action_portrait_orientation -> {
                setOrientationMode("portrait")
                true
            }
            R.id.action_landscape_orientation -> {
                setOrientationMode("landscape")
                true
            }
            R.id.action_jump_page -> {
                (item.actionView as? EditText)?.requestFocus()
                true
            }
            R.id.action_search_text -> {
                showTextSearchDialog()
                true
            }
            R.id.action_save_personal_bookmark -> {
                showSavePersonalBookmarkDialog()
                true
            }
            R.id.action_personal_bookmarks -> {
                showPersonalBookmarksDialog()
                true
            }
            R.id.action_pdf_outline -> {
                showPdfOutlineDialog()
                true
            }
            R.id.action_reading_brightness,
            R.id.action_reset_reading_brightness -> {
                if (item.itemId == R.id.action_reading_brightness) {
                    showReadingBrightnessDialog()
                } else {
                    restoreInitialWindowBrightness(showError = true)
                }
                true
            }
            R.id.action_dark_mode -> {
                setReadingMode("dark")
                true
            }
            R.id.action_light_mode -> {
                setReadingMode("light")
                true
            }
            R.id.action_sepia_mode -> {
                setReadingMode("sepia")
                true
            }
            R.id.action_invert_pdf -> {
                invertPdf = !invertPdf
                getSharedPreferences("pdf_prefs", MODE_PRIVATE).edit()
                    .putBoolean("viewer_invert_pdf", invertPdf)
                    .apply()
                adapter?.setInvertPdf(invertPdf)
                invalidateOptionsMenu()
                true
            }
            R.id.action_keep_screen_on -> {
                keepScreenOn = !keepScreenOn
                getSharedPreferences("pdf_prefs", MODE_PRIVATE).edit()
                    .putBoolean("viewer_keep_screen_on", keepScreenOn)
                    .apply()
                applyKeepScreenOn()
                invalidateOptionsMenu()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecycler(r: PdfRenderer) {
        layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.layoutManager = layoutManager
        recyclerView.setHasFixedSize(false)
        val targetPx = computeTargetPx()
        adapter = PdfPageAdapter(r, targetPx, invertPdf)
        recyclerView.adapter = adapter

        // Background for dark mode readability
        recyclerView.setBackgroundColor(if (isDarkMode) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
    }

    private fun setupPageControls(pageCount: Int) {
        pageSeekBar.max = (pageCount - 1).coerceAtLeast(0)
        pageSeekBar.progress = currentFirstVisiblePage.coerceIn(0, pageSeekBar.max)
        pageSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                userSeeking = true
                updatePageCounterText(pageIndex = progress, pageCount = pageCount)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                userSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val target = (seekBar?.progress ?: 0).coerceIn(0, (pageCount - 1).coerceAtLeast(0))
                userSeeking = false
                requestPagePosition(target, pageCount) {
                    userSeeking = false
                }
            }
        })

        reportCurrentPage(pageCount)
    }

    private fun applyFullscreen(enabled: Boolean) {
        if (enabled) {
            uiChromeVisible = false
            toolbar.visibility = View.GONE
            pageControls.visibility = View.GONE
            WindowCompat.setDecorFitsSystemWindows(window, true)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            uiChromeVisible = true
            toolbar.visibility = View.VISIBLE
            pageControls.visibility = View.VISIBLE
            WindowCompat.setDecorFitsSystemWindows(window, true)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun setReadingMode(mode: String) {
            readingMode = mode
            isDarkMode = mode == "dark"
            getSharedPreferences("pdf_prefs", MODE_PRIVATE).edit()
                .putString("viewer_reading_mode", mode)
                .putBoolean("viewer_dark_mode", isDarkMode)
                .apply()
            applyReadingMode()
            invalidateOptionsMenu()
        }

    private fun applyReadingMode() {
            val background = when (readingMode) {
                "dark" -> Color.BLACK
                "sepia" -> Color.rgb(244, 236, 216)
                else -> Color.WHITE
            }
            val foreground = if (readingMode == "dark") Color.WHITE else Color.BLACK
            val toolbarBackground = when (readingMode) {
                "dark" -> Color.rgb(35, 35, 35)
                "sepia" -> Color.rgb(125, 90, 55)
                else -> getColor(R.color.purple_500)
            }
            recyclerView.setBackgroundColor(background)
            pageControls.setBackgroundColor(background)
            pageCounterText.setTextColor(foreground)
            toolbar.setBackgroundColor(toolbarBackground)
            toolbar.setTitleTextColor(Color.WHITE)
            toolbar.navigationIcon?.setTint(Color.WHITE)
            toolbar.overflowIcon?.setTint(Color.WHITE)
            window.statusBarColor = toolbarBackground
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        }

    private fun applyKeepScreenOn() {
            if (keepScreenOn) {
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun toggleChrome() {
        uiChromeVisible = !uiChromeVisible
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (uiChromeVisible) {
            toolbar.visibility = View.VISIBLE
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            toolbar.visibility = View.GONE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun setOrientationMode(mode: String) {
        val normalized = mode.takeIf { it == "auto" || it == "portrait" || it == "landscape" } ?: "auto"
        isAutoOrientation = normalized == "auto"
        isHorizontal = normalized == "landscape"
        getSharedPreferences("pdf_prefs", MODE_PRIVATE).edit()
            .putString("viewer_orientation_mode", normalized)
            .apply()
        applyOrientationMode(normalized)
        invalidateOptionsMenu()
    }

    private fun applyOrientationMode(mode: String) {
        requestedOrientation = when (mode) {
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    private fun setZoom(scale: Float, focusX: Float, focusY: Float) {
        val next = scale.coerceIn(1f, 5f)
        if (next == zoomScale) return
        val ratio = next / zoomScale
        recyclerView.pivotX = focusX.coerceIn(0f, recyclerView.width.toFloat())
        recyclerView.pivotY = focusY.coerceIn(0f, recyclerView.height.toFloat())
        recyclerView.scaleX = next
        recyclerView.scaleY = next
        recyclerView.translationX *= ratio
        recyclerView.translationY *= ratio
        zoomScale = next
    }

    private fun showPageJumpDialog(pageCount: Int) {
        if (pageCount <= 0) return
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "1-$pageCount"
            setSingleLine(true)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Ir a página")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Ir", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val page = input.text.toString().toIntOrNull()
                if (page == null || page !in 1..pageCount) {
                    input.error = "Introduce un número entre 1 y $pageCount"
                } else {
                    requestPagePosition(page - 1, pageCount)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun showSavePersonalBookmarkDialog() {
        val pageIndex = currentFirstVisiblePage
        val input = EditText(this).apply {
            hint = "Nombre opcional"
            setSingleLine(true)
        }
        AlertDialog.Builder(this)
            .setTitle("Guardar marcador")
            .setMessage("Página ${pageIndex + 1}")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val title = input.text.toString().trim()
                    .ifEmpty { "Página ${pageIndex + 1}" }
                intent.data?.let { uri ->
                    PersonalBookmarksStore.upsert(this, uri, pageIndex, title)
                    Toast.makeText(this, "Marcador guardado", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showPersonalBookmarksDialog() {
        val uri = intent.data ?: return
        val bookmarks = PersonalBookmarksStore.getForPdf(this, uri)
        if (bookmarks.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Mis marcadores")
                .setMessage("Este PDF no tiene marcadores personales")
                .setPositiveButton("Cerrar", null)
                .show()
            return
        }

        val labels = bookmarks.map { "${it.title} · página ${it.pageIndex + 1}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Mis marcadores")
            .setItems(labels) { _, which ->
                requestPagePosition(bookmarks[which].pageIndex, renderer?.pageCount ?: 1)
            }
            .setPositiveButton("Eliminar") { _, _ ->
                showDeletePersonalBookmarkDialog(bookmarks)
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun showPdfOutlineDialog() {
        val uri = intent.data ?: return
        val loadingDialog = AlertDialog.Builder(this)
            .setTitle("Índice del PDF")
            .setMessage("Leyendo índice...")
            .setNegativeButton("Cerrar", null)
            .create()
        loadingDialog.show()

        searchExecutor.execute {
            val result = try {
                PdfOutlineReader.read(this@PdfViewerActivity, uri)
            } catch (error: Exception) {
                Log.e(logTag, "PDF outline failed", error)
                null
            }
            mainHandler.post {
                if (isFinishing || isDestroyed || !loadingDialog.isShowing) return@post
                loadingDialog.dismiss()
                if (result == null) {
                    Toast.makeText(
                        this,
                        "No se pudo leer el índice del PDF",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    displayPdfOutline(result)
                }
            }
        }
    }

    private fun displayPdfOutline(entries: List<PdfOutlineEntry>) {
        if (entries.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Índice del PDF")
                .setMessage("Este PDF no tiene índice")
                .setPositiveButton("Cerrar", null)
                .show()
            return
        }

        val labels = entries.map { entry ->
            val indentation = "    ".repeat(entry.level.coerceAtMost(8))
            val destination = entry.pageIndex?.let { " · página ${it + 1}" }
                ?: " · página no disponible"
            indentation + entry.title + destination
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Índice del PDF")
            .setItems(labels) { _, which ->
                val pageIndex = entries[which].pageIndex ?: run {
                    Toast.makeText(
                        this,
                        "Esta entrada no tiene un destino disponible",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setItems
                }
                requestPagePosition(pageIndex, renderer?.pageCount ?: 1)
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun showReadingBrightnessDialog() {
        val brightnessLabel = TextView(this).apply {
            setPadding(0, 0, 0, 12)
        }
        val seekBar = SeekBar(this).apply {
            min = 20
            max = 100
            progress = (readingBrightness * 100f).roundToInt().coerceIn(min, max)
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 0, 32, 0)
            addView(brightnessLabel)
            addView(seekBar)
        }

        fun updateLabel() {
            brightnessLabel.text = getString(
                R.string.brightness_percentage,
                seekBar.progress
            )
        }

        updateLabel()
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                val percentage = progress.coerceIn(20, 100)
                readingBrightness = percentage / 100f
                updateLabel()
                applyReadingBrightness(readingBrightness)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
        })

        AlertDialog.Builder(this)
            .setTitle(R.string.brightness_dialog_title)
            .setView(container)
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun applyReadingBrightness(value: Float) {
        try {
            val attributes = window.attributes
            attributes.screenBrightness = value.coerceIn(0.2f, 1f)
            window.attributes = attributes
        } catch (error: RuntimeException) {
            Log.e(logTag, "Reading brightness apply failed", error)
            Toast.makeText(this, R.string.brightness_apply_error, Toast.LENGTH_LONG).show()
        }
    }

    private fun restoreInitialWindowBrightness(showError: Boolean) {
        try {
            val attributes = window.attributes
            attributes.screenBrightness = initialWindowBrightness
            window.attributes = attributes
            readingBrightness = initialWindowBrightness.takeUnless { it < 0f }
                ?.coerceIn(0.2f, 1f)
                ?: 1f
        } catch (error: RuntimeException) {
            Log.e(logTag, "Reading brightness restore failed", error)
            if (showError) {
                Toast.makeText(this, R.string.brightness_restore_error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showDeletePersonalBookmarkDialog(bookmarks: List<PersonalBookmark>) {
        val labels = bookmarks.map { "${it.title} · página ${it.pageIndex + 1}" }.toTypedArray()
        var selectedIndex = -1
        lateinit var dialog: AlertDialog
        dialog = AlertDialog.Builder(this)
            .setTitle("Selecciona un marcador para eliminar")
            .setSingleChoiceItems(labels, -1) { _, which ->
                selectedIndex = which
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
            }
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar", null)
            .create()
        dialog.setOnShowListener {
            val deleteButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            deleteButton.isEnabled = false
            deleteButton.setOnClickListener {
                if (selectedIndex !in bookmarks.indices) return@setOnClickListener
                val selectedBookmark = bookmarks[selectedIndex]
                PersonalBookmarksStore.remove(this, selectedBookmark)
                Toast.makeText(this, "Marcador eliminado", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                showPersonalBookmarksDialog()
            }
        }
        dialog.show()
    }

    private fun jumpToPageFromInput(input: EditText) {
        val pageCount = renderer?.pageCount ?: 0
        val page = input.text.toString().toIntOrNull()
        if (pageCount <= 0 || page == null || page !in 1..pageCount) {
            input.error = "1-${pageCount.coerceAtLeast(1)}"
            return
        }
        input.error = null
        requestPagePosition(page - 1, pageCount)
        input.clearFocus()
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
            as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(input.windowToken, 0)
    }

    private fun showTextSearchDialog() {
        searchCancellation.set(false)
        val input = EditText(this).apply {
            hint = "Texto a buscar"
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine(true)
        }
        val status = TextView(this).apply {
            text = "Escribe para buscar"
            setPadding(0, 12, 0, 4)
        }
        val previous = Button(this).apply { text = "Anterior"; isEnabled = false }
        val next = Button(this).apply { text = "Siguiente"; isEnabled = false }
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(previous, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(next, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 0, 32, 0)
            addView(input)
            addView(status)
            addView(buttons)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Buscar texto")
            .setView(container)
            .setNegativeButton("Cerrar") { _, _ -> clearSearchState() }
            .create()
        searchDialog = dialog
        searchStatus = status
        searchPrevious = previous
        searchNext = next
        var textIndex: PdfTextIndex? = null
        var latestQuery = ""
        var queryVersion = 0

        fun moveTo(index: Int) {
            if (index !in searchMatches.indices) return
            activeSearchMatch = index
            val match = searchMatches[index]
            requestPagePosition(match.pageIndex, renderer?.pageCount ?: 1)
            status.text = "${index + 1} / ${searchMatches.size} · página ${match.pageIndex + 1}"
            previous.isEnabled = index > 0
            next.isEnabled = index < searchMatches.lastIndex
        }

        previous.setOnClickListener { moveTo(activeSearchMatch - 1) }
        next.setOnClickListener { moveTo(activeSearchMatch + 1) }

        fun executeQuery(query: String, version: Int) {
            if (query.trim().isEmpty()) {
                status.text = "Escribe para buscar"
                return
            }
            val index = textIndex
            if (index == null) {
                status.text = "Preparando búsqueda..."
                return
            }
            status.text = "Buscando..."
            searchExecutor.execute {
                val result = PdfTextSearch.find(index, query)
                mainHandler.post {
                    if (searchDialog !== dialog || !dialog.isShowing || version != queryVersion) return@post
                    searchMatches = result
                    adapter?.setSearchMatches(result)
                    if (result.isEmpty()) {
                        status.text = "No se encontró texto"
                    } else {
                        moveTo(0)
                    }
                }
            }
        }

        input.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()
                latestQuery = query
                queryVersion++
                searchMatches = emptyList()
                activeSearchMatch = -1
                adapter?.setSearchMatches(emptyList())
                previous.isEnabled = false
                next.isEnabled = false
                executeQuery(query, queryVersion)
            }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
        dialog.setOnDismissListener { clearSearchState() }
        dialog.show()
        status.text = "Preparando búsqueda..."
        searchExecutor.execute {
            val index = try {
                PdfTextSearch.buildIndex(
                    this@PdfViewerActivity,
                    intent.data!!,
                    onProgress = { currentPage, totalPages ->
                    mainHandler.post {
                        if (searchDialog === dialog && dialog.isShowing) {
                            status.text = "Preparando búsqueda: $currentPage / $totalPages"
                        }
                    }
                    },
                    isCancelled = searchCancellation::get
                )
            } catch (_: CancellationException) {
                null
            } catch (error: Exception) {
                Log.e(logTag, "Text index failed", error)
                null
            }
            mainHandler.post {
                if (searchDialog !== dialog || !dialog.isShowing) return@post
                if (index == null) {
                    status.text = "No se pudo analizar el PDF"
                } else {
                    textIndex = index
                    executeQuery(latestQuery, queryVersion)
                }
            }
        }
    }

    private fun clearSearchState() {
        searchCancellation.set(true)
        searchDialog = null
        searchStatus = null
        searchPrevious = null
        searchNext = null
        searchMatches = emptyList()
        activeSearchMatch = -1
        adapter?.setSearchMatches(emptyList())
    }

    private fun computeTargetPx(): Int {
        // Pages are laid out vertically; use the available width in every device orientation.
        val measured = recyclerView.width - recyclerView.paddingLeft - recyclerView.paddingRight
        if (measured > 0) return measured

        val dm = resources.displayMetrics
        return dm.widthPixels
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && isFullscreen && !uiChromeVisible) {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun showFallback(message: String) {
        recyclerView.visibility = RecyclerView.GONE
        fallbackContainer.visibility = LinearLayout.VISIBLE
        fallbackImage.setImageResource(R.mipmap.ic_launcher)
        fallbackText.text = message
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // save last visible page + viewer settings
        intent?.data?.let { uri ->
            val prefs = getSharedPreferences("pdf_prefs", MODE_PRIVATE)
            val pageToSave = pendingScrollTarget ?: currentFirstVisiblePage
            prefs.edit()
                .putInt(uri.toString(), pageToSave)
                .putString(
                    "viewer_orientation_mode",
                    when {
                        isAutoOrientation -> "auto"
                        isHorizontal -> "landscape"
                        else -> "portrait"
                    }
                )
                .putBoolean("viewer_dark_mode", isDarkMode)
                .putString("viewer_reading_mode", readingMode)
                .putBoolean("viewer_invert_pdf", invertPdf)
                .putBoolean("viewer_keep_screen_on", keepScreenOn)
                .putBoolean("viewer_fullscreen", isFullscreen)
                .apply()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::recyclerView.isInitialized) {
            applyKeepScreenOn()
        }
    }

    override fun onStop() {
        restoreInitialWindowBrightness(showError = false)
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_PAGE_INDEX, pendingScrollTarget ?: currentFirstVisiblePage)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        restoreInitialWindowBrightness(showError = false)
        super.onDestroy()
        searchExecutor.shutdownNow()
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        adapter?.shutdown()
        renderer?.close()
        fileDescriptor?.close()
    }

    private fun scrollToPage(pageIndex: Int) {
        val lm = layoutManager
        if (lm != null) {
            lm.scrollToPositionWithOffset(pageIndex, 0)
        } else {
            recyclerView.scrollToPosition(pageIndex)
        }
    }

    private fun requestPagePosition(
            pageIndex: Int,
            pageCount: Int,
            onSettled: (() -> Unit)? = null
        ) {
            currentFirstVisiblePage = pageIndex
            pendingScrollTarget = pageIndex
            persistPage(pageIndex)
            isRestoringPosition = true
            updatePageCounterText(pageIndex, pageCount)
            pageSeekBar.progress = pageIndex
            scrollToPage(pageIndex)
            waitForScrollTarget(pageIndex, pageCount, onSettled = onSettled)
        }

    private fun waitForScrollTarget(
            pageIndex: Int,
            pageCount: Int,
            attempts: Int = 0,
            onSettled: (() -> Unit)? = null
        ) {
            val lm = layoutManager
            val targetVisible = lm != null &&
                lm.findFirstVisibleItemPosition() <= pageIndex &&
                lm.findLastVisibleItemPosition() >= pageIndex
            val targetIsFirstVisible = lm != null &&
                lm.findFirstVisibleItemPosition() == pageIndex
            if (targetIsFirstVisible || attempts >= 40) {
                currentFirstVisiblePage = pageIndex
                isRestoringPosition = false
                lastReportedPage = pageIndex
                updatePageCounterText(pageIndex, pageCount)
                if (!userSeeking) pageSeekBar.progress = pageIndex
                onSettled?.invoke()
                return
            }
            recyclerView.postDelayed(
                { waitForScrollTarget(pageIndex, pageCount, attempts + 1, onSettled) },
                50L
            )
    }

    private fun persistPage(pageIndex: Int) {
            intent?.data?.let { uri ->
                getSharedPreferences("pdf_prefs", MODE_PRIVATE)
                    .edit()
                    .putInt(uri.toString(), pageIndex)
                    .apply()
            }
    }

    private fun centerPage(pageIndex: Int) {
        val lm = layoutManager ?: return
        val view = lm.findViewByPosition(pageIndex)
        if (view == null) {
            lm.scrollToPosition(pageIndex)
            return
        }

        val isHoriz = lm.orientation == LinearLayoutManager.HORIZONTAL
        val parentSize = if (isHoriz) recyclerView.width else recyclerView.height
        val childSize = if (isHoriz) view.width else view.height
        val offset = ((parentSize - childSize) / 2).coerceAtLeast(0)
        lm.scrollToPositionWithOffset(pageIndex, offset)
    }

    private fun getCenteredVisiblePage(): Int {
        val lm = layoutManager ?: return currentFirstVisiblePage
        val first = lm.findFirstVisibleItemPosition()
        if (first == RecyclerView.NO_POSITION) return currentFirstVisiblePage
        val last = lm.findLastVisibleItemPosition().takeIf { it != RecyclerView.NO_POSITION } ?: first

        var bestPos = first
        var bestVisibleArea = -1
        for (pos in first..last) {
            val child = lm.findViewByPosition(pos) ?: continue
            val visibleTop = child.top.coerceAtLeast(recyclerView.paddingTop)
            val visibleBottom = child.bottom.coerceAtMost(recyclerView.height - recyclerView.paddingBottom)
            val visibleLeft = child.left.coerceAtLeast(recyclerView.paddingLeft)
            val visibleRight = child.right.coerceAtMost(recyclerView.width - recyclerView.paddingRight)
            val visibleArea = (visibleBottom - visibleTop).coerceAtLeast(0) *
                (visibleRight - visibleLeft).coerceAtLeast(0)
            if (visibleArea > bestVisibleArea) {
                bestVisibleArea = visibleArea
                bestPos = pos
            }
        }
        return bestPos
    }

    private fun reportCurrentPage(pageCount: Int) {
        pendingScrollTarget?.let { target ->
            currentFirstVisiblePage = target
            updatePageCounterText(target, pageCount)
            if (!userSeeking) pageSeekBar.progress = target
            return
        }
        val page = getCenteredVisiblePage().coerceIn(0, (pageCount - 1).coerceAtLeast(0))
        if (page == lastReportedPage) return
        lastReportedPage = page
        currentFirstVisiblePage = page
        updatePageCounterText(page, pageCount)
        if (!userSeeking) {
            pageSeekBar.progress = page
        }
    }

    private fun updatePageCounterText(pageIndex: Int, pageCount: Int) {
        val safeCount = pageCount.coerceAtLeast(1)
        val safeIndex = pageIndex.coerceIn(0, safeCount - 1)
        pageCounterText.text = "${safeIndex + 1} / $safeCount"
    }
}
