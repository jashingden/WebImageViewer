package com.eddy.webcrawler.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.eddy.webcrawler.data.model.ContentItem
import com.eddy.webcrawler.databinding.FragmentBrowsePageBinding
import com.eddy.webcrawler.ui.adapter.ContentAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class IndexPageFragment : Fragment() {

    companion object {
        private const val ARG_INDEX_ID = "arg_index_id"

        fun newInstance(indexId: Long): IndexPageFragment {
            val fragment = IndexPageFragment()
            val args = Bundle()
            args.putLong(ARG_INDEX_ID, indexId)
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: FragmentBrowsePageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: com.eddy.webcrawler.ui.viewmodel.BrowseViewModel by viewModels()
    private lateinit var adapter: ContentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrowsePageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val indexId = arguments?.getLong(ARG_INDEX_ID) ?: return

        adapter = ContentAdapter(
            onDownloadClick = { item ->
                if (item is ContentItem.DownloadItem) {
                    viewModel.startDownload(item.stableId.toLong(), indexId, item.url)
                }
            },
            onViewZipClick = { item ->
                if (item is ContentItem.DownloadItem && item.localPath != null) {
                    val action = BrowseFragmentDirections.actionBrowseFragmentToZipViewerFragment(item.localPath)
                    findNavController().navigate(action)
                }
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pageState.collect { state ->
                    when (state) {
                        is com.eddy.webcrawler.ui.viewmodel.PageState.Success -> {
                            adapter.submitList(state.content)
                        }
                        else -> {}
                    }
                }
            }
        }

        viewModel.loadContent(indexId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
