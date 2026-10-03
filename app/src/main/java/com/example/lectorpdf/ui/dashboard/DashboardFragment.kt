package com.example.lectorpdf.ui.dashboard

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.lectorpdf.PdfViewerActivity
import com.example.lectorpdf.data.RecentPdfsStore
import com.example.lectorpdf.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val emptyView: TextView = binding.recentEmptyView

        val recycler = binding.recentRecyclerView
        val adapter = RecentPdfListAdapter(emptyList()) { item ->
            val intent = Intent(requireContext(), PdfViewerActivity::class.java)
            intent.data = item.uri
            intent.putExtra(PdfViewerActivity.EXTRA_TITLE, item.title)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.clipData = ClipData.newUri(requireContext().contentResolver, item.title, item.uri)
            try {
                if (!item.isAvailable) {
                    Toast.makeText(requireContext(), "El PDF ya no está disponible", Toast.LENGTH_LONG).show()
                    return@RecentPdfListAdapter
                }
                startActivity(intent)
            } catch (t: Exception) {
                Toast.makeText(requireContext(), "No se pudo abrir el PDF (${t.javaClass.simpleName})", Toast.LENGTH_LONG).show()
            }
        }
        recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        recycler.adapter = adapter
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val removed = adapter.removeAt(position)
                if (removed != null) {
                    RecentPdfsStore.remove(requireContext(), removed.uri)
                }
            }
        }).attachToRecyclerView(recycler)

        refresh(adapter, emptyView)
        return root
    }

    override fun onResume() {
        super.onResume()
        val b = _binding ?: return
        val recycler = b.recentRecyclerView
        val adapter = recycler.adapter as? RecentPdfListAdapter ?: return
        refresh(adapter, b.recentEmptyView)
    }

    private fun refresh(adapter: RecentPdfListAdapter, emptyView: TextView) {
        viewLifecycleOwner.lifecycleScope.launch {
            val recents = withContext(Dispatchers.IO) {
                RecentPdfsStore.getRecents(requireContext())
            }
            if (!isAdded) return@launch
            if (recents.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                emptyView.text = "Aún no has leído PDFs"
                adapter.update(emptyList())
            } else {
                emptyView.visibility = View.GONE
                adapter.update(recents)
            }
        }
    }

    override fun onDestroyView() {
        (binding.recentRecyclerView.adapter as? RecentPdfListAdapter)?.shutdown()
        super.onDestroyView()
        _binding = null
    }
}