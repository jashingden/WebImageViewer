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
import com.eddy.webcrawler.databinding.FragmentBrowseBinding
import com.eddy.webcrawler.ui.adapter.LinkIndexPagerAdapter
import com.eddy.webcrawler.ui.viewmodel.BrowseViewModel
import com.eddy.webcrawler.ui.viewmodel.PageState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BrowseFragment : Fragment() {

    private var _binding: FragmentBrowseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BrowseViewModel by viewModels()
    private lateinit var adapter: LinkIndexPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrowseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val linkIndexId = BrowseFragmentArgs.fromBundle(requireArguments()).linkIndexId

        if (linkIndexId == 0L) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.allIndices.collect { indices ->
                        if (indices.isNotEmpty()) {
                            adapter = LinkIndexPagerAdapter(this@BrowseFragment, indices.map { it.id })
                            binding.viewPager.adapter = adapter
                            binding.tvEmptyState.visibility = View.GONE
                        } else {
                            binding.tvEmptyState.visibility = View.VISIBLE
                            binding.tvEmptyState.text = "尚未有爬取紀錄"
                        }
                    }
                }
            }
        } else {
            adapter = LinkIndexPagerAdapter(this, listOf(linkIndexId))
            binding.viewPager.adapter = adapter
            viewModel.loadContent(linkIndexId)
        }

        binding.viewPager.offscreenPageLimit = 1

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pageState.collect { state ->
                    when (state) {
                        is PageState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.tvEmptyState.visibility = View.GONE
                        }
                        is PageState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.GONE
                        }
                        is PageState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.VISIBLE
                            binding.tvEmptyState.text = state.message
                        }
                        is PageState.Idle -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
