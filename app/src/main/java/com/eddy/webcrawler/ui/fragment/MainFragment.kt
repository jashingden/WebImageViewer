package com.eddy.webcrawler.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.eddy.webcrawler.R
import com.eddy.webcrawler.databinding.FragmentMainBinding
import com.eddy.webcrawler.ui.viewmodel.CrawlState
import com.eddy.webcrawler.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.crawlState.collect { state ->
                    when (state) {
                        is CrawlState.Idle -> showIdleState()
                        is CrawlState.Loading -> showLoadingState()
                        is CrawlState.Success -> showSuccessState(state.content)
                        is CrawlState.Error -> showErrorState(state.message)
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnCrawl.setOnClickListener {
            val url = binding.etUrl.text?.toString()?.trim() ?: ""
            val pattern = binding.etPattern.text?.toString()?.trim() ?: ".*"
            viewModel.crawl(url, pattern)
        }

        binding.btnRetry.setOnClickListener {
            val url = binding.etUrl.text?.toString()?.trim() ?: ""
            val pattern = binding.etPattern.text?.toString()?.trim() ?: ".*"
            viewModel.crawl(url, pattern)
        }
    }

    private fun showIdleState() {
        binding.progressBar.visibility = View.GONE
        binding.btnCrawl.isEnabled = true
        binding.tvError.visibility = View.GONE
        binding.btnRetry.visibility = View.GONE
    }

    private fun showLoadingState() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnCrawl.isEnabled = false
        binding.tvError.visibility = View.GONE
        binding.btnRetry.visibility = View.GONE
    }

    private fun showSuccessState(result: com.eddy.webcrawler.data.repository.CrawlResult) {
        binding.progressBar.visibility = View.GONE
        binding.btnCrawl.isEnabled = true
        binding.tvError.visibility = View.GONE
        binding.btnRetry.visibility = View.GONE

        Toast.makeText(
            requireContext(),
            "爬取完成：找到 ${result.entries.size} 個連結",
            Toast.LENGTH_SHORT
        ).show()

        val action = MainFragmentDirections.actionMainFragmentToBrowseFragment(result.linkIndexId)
        findNavController().navigate(action)
    }

    private fun showErrorState(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.btnCrawl.isEnabled = true
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
        binding.btnRetry.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
