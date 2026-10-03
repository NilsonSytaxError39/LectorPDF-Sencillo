package com.example.lectorpdf.ui.home

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.lectorpdf.PdfViewerActivity
import com.example.lectorpdf.databinding.FragmentHomeBinding
import com.example.lectorpdf.model.PdfItem
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private enum class ViewMode { GRID, LIST }
    private enum class SortMode { DATE_DESC, NAME_ASC, SIZE_DESC }

    private var viewMode: ViewMode = ViewMode.GRID
    private var sortMode: SortMode = SortMode.DATE_DESC
    private var lastScannedItems: List<PdfItem> = emptyList()
    private var currentQuery: String = ""
    private lateinit var openDocument: ActivityResultLauncher<Array<String>>
    private lateinit var openFolder: ActivityResultLauncher<Uri?>

    private fun prefs() = requireContext().getSharedPreferences("pdf_prefs", 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openDocument = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri == null) return@registerForActivityResult

            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers grant only temporary read access.
            }

            openPdf(uri)
        }
        openFolder = registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            if (uri == null) return@registerForActivityResult
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                prefs().edit().putString(KEY_PDF_FOLDER, uri.toString()).apply()
                scanFolder()
            } catch (_: SecurityException) {
                Toast.makeText(requireContext(), "No se pudo conceder acceso a la carpeta", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val emptyView: TextView = binding.emptyView
        val rescanButton: Button = binding.rescanButton as Button
        // Restore view/sort modes
        viewMode = if (prefs().getString("home_view_mode", "grid") == "list") ViewMode.LIST else ViewMode.GRID
        sortMode = when (prefs().getString("home_sort_mode", "date") ?: "date") {
            "name" -> SortMode.NAME_ASC
            "size" -> SortMode.SIZE_DESC
            else -> SortMode.DATE_DESC
        }

        val recycler = binding.pdfRecyclerView
        val adapter = PdfListAdapter(emptyList()) { item ->
            val intent = Intent(requireContext(), PdfViewerActivity::class.java)
            intent.data = item.uri
            intent.putExtra(PdfViewerActivity.EXTRA_TITLE, item.title)
            // Defensive: ensure the viewer can read the Uri even when it comes from SAF/MediaStore.
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.clipData = ClipData.newUri(requireContext().contentResolver, item.title, item.uri)
            try {
                startActivity(intent)
            } catch (t: Throwable) {
                Toast.makeText(requireContext(), "No se pudo abrir el PDF (${t.javaClass.simpleName})", Toast.LENGTH_LONG).show()
            }
        }
        val gridLayoutManager = GridLayoutManager(requireContext(), if (viewMode == ViewMode.LIST) 1 else 2)
        recycler.layoutManager = gridLayoutManager
        recycler.adapter = adapter

        setupSearch(adapter, gridLayoutManager)

        rescanButton.setOnClickListener { openFolder.launch(null) }
        rescanButton.text = "Elegir carpeta de PDFs"
        emptyView.visibility = View.VISIBLE
        emptyView.text = "Elige una carpeta para buscar PDFs"
        loadSavedFolder(adapter, emptyView)
        return root
    }

    private fun loadSavedFolder(adapter: PdfListAdapter, emptyView: TextView) {
        if (prefs().getString(KEY_PDF_FOLDER, null) == null) return
        scanFolder(adapter, emptyView)
    }

    private fun scanFolder(
        adapter: PdfListAdapter? = _binding?.pdfRecyclerView?.adapter as? PdfListAdapter,
        emptyView: TextView? = _binding?.emptyView
    ) {
        val folder = prefs().getString(KEY_PDF_FOLDER, null)?.let(Uri::parse) ?: return
        emptyView?.visibility = View.VISIBLE
        emptyView?.text = "Buscando PDFs..."
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { com.example.lectorpdf.data.PdfScanner.scanTree(requireContext(), folder) }
            }
            val currentAdapter = adapter ?: return@launch
            val items = result.getOrElse {
                Toast.makeText(requireContext(), "No se pudo leer la carpeta", Toast.LENGTH_LONG).show()
                emptyList()
            }
            lastScannedItems = items
            currentAdapter.update(sortItems(items))
            currentAdapter.filterByTitle(currentQuery)
            emptyView?.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            if (items.isEmpty()) emptyView?.text = "No se encontraron PDFs en esta carpeta"
        }
    }

    private fun openPdf(uri: Uri) {
        val intent = Intent(requireContext(), PdfViewerActivity::class.java).apply {
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri("PDF", uri)
        }
        try {
            startActivity(intent)
        } catch (exception: SecurityException) {
            Toast.makeText(
                requireContext(),
                "No se pudo conceder acceso al PDF",
                Toast.LENGTH_LONG
            ).show()
        } catch (exception: RuntimeException) {
            Toast.makeText(
                requireContext(),
                "No se pudo abrir el PDF",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setupSearch(adapter: PdfListAdapter, gridLayoutManager: GridLayoutManager) {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(com.example.lectorpdf.R.menu.home_menu, menu)
                val searchItem: MenuItem = menu.findItem(com.example.lectorpdf.R.id.action_search)
                val searchView = (searchItem.actionView as SearchView)
                searchView.queryHint = "Buscar PDF"
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        currentQuery = query.orEmpty()
                        adapter.filterByTitle(currentQuery)
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        currentQuery = newText.orEmpty()
                        adapter.filterByTitle(currentQuery)
                        return true
                    }
                })

                // Update menu titles to reflect current modes
                menu.findItem(com.example.lectorpdf.R.id.action_toggle_view)?.title =
                    if (viewMode == ViewMode.LIST) "Lista" else "Cuadrícula"
                menu.findItem(com.example.lectorpdf.R.id.action_sort)?.title = when (sortMode) {
                    SortMode.DATE_DESC -> "Fecha"
                    SortMode.NAME_ASC -> "Nombre"
                    SortMode.SIZE_DESC -> "Tamaño"
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    com.example.lectorpdf.R.id.action_rescan -> {
                        openFolder.launch(null)
                        true
                    }
                    com.example.lectorpdf.R.id.action_toggle_view -> {
                        viewMode = if (viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
                        prefs().edit().putString("home_view_mode", if (viewMode == ViewMode.LIST) "list" else "grid").apply()
                        gridLayoutManager.spanCount = if (viewMode == ViewMode.LIST) 1 else 2
                        menuItem.title = if (viewMode == ViewMode.LIST) "Lista" else "Cuadrícula"
                        true
                    }
                    com.example.lectorpdf.R.id.action_sort -> {
                        sortMode = when (sortMode) {
                            SortMode.DATE_DESC -> SortMode.NAME_ASC
                            SortMode.NAME_ASC -> SortMode.SIZE_DESC
                            SortMode.SIZE_DESC -> SortMode.DATE_DESC
                        }
                        val stored = when (sortMode) {
                            SortMode.DATE_DESC -> "date"
                            SortMode.NAME_ASC -> "name"
                            SortMode.SIZE_DESC -> "size"
                        }
                        prefs().edit().putString("home_sort_mode", stored).apply()
                        menuItem.title = when (sortMode) {
                            SortMode.DATE_DESC -> "Fecha"
                            SortMode.NAME_ASC -> "Nombre"
                            SortMode.SIZE_DESC -> "Tamaño"
                        }
                        // Re-apply to already scanned items.
                        val sorted = sortItems(lastScannedItems)
                        adapter.update(sorted)
                        adapter.filterByTitle(currentQuery)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun sortItems(items: List<PdfItem>): List<PdfItem> {
        return when (sortMode) {
            SortMode.DATE_DESC -> items.sortedByDescending { it.dateModifiedSeconds }
            SortMode.NAME_ASC -> items.sortedBy { it.title.lowercase(Locale.getDefault()) }
            SortMode.SIZE_DESC -> items.sortedByDescending { it.sizeBytes }
        }
    }

    override fun onDestroyView() {
        (binding.pdfRecyclerView.adapter as? PdfListAdapter)?.shutdown()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_PDF_FOLDER = "pdf_library_folder"
    }
}