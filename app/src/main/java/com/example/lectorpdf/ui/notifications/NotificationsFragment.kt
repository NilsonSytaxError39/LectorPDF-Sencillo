package com.example.lectorpdf.ui.notifications

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.lectorpdf.PdfViewerActivity
import com.example.lectorpdf.data.MarkedPdfsStore
import com.example.lectorpdf.databinding.FragmentNotificationsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val emptyView: TextView = binding.markedEmptyView
        val recycler = binding.markedRecyclerView
        val adapter = MarkedPdfListAdapter(emptyList()) { item ->
            val intent = Intent(requireContext(), PdfViewerActivity::class.java)
            intent.data = item.uri
            intent.putExtra(PdfViewerActivity.EXTRA_TITLE, item.title)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.clipData = ClipData.newUri(requireContext().contentResolver, item.title, item.uri)
            try {
                startActivity(intent)
            } catch (t: Throwable) {
                Toast.makeText(requireContext(), "No se pudo abrir el PDF (${t.javaClass.simpleName})", Toast.LENGTH_LONG).show()
            }
        }
        recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        recycler.adapter = adapter

        refresh(adapter, emptyView)
        return root
    }

    override fun onResume() {
        super.onResume()
        val b = _binding ?: return
        val adapter = b.markedRecyclerView.adapter as? MarkedPdfListAdapter ?: return
        refresh(adapter, b.markedEmptyView)
    }

    private fun refresh(adapter: MarkedPdfListAdapter, emptyView: TextView) {
        viewLifecycleOwner.lifecycleScope.launch {
            val marked = withContext(Dispatchers.IO) {
                MarkedPdfsStore.getMarked(requireContext())
            }

            if (!isAdded) return@launch

            if (marked.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                emptyView.text = "No tienes PDFs marcados"
                adapter.update(emptyList())
            } else {
                emptyView.visibility = View.GONE
                adapter.update(marked)
            }
        }
    }

    override fun onDestroyView() {
        (binding.markedRecyclerView.adapter as? MarkedPdfListAdapter)?.shutdown()
        super.onDestroyView()
        _binding = null
    }
}