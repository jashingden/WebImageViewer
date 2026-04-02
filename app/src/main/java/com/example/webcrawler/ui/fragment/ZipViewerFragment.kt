package com.example.webcrawler.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.webcrawler.databinding.FragmentZipViewerBinding
import com.example.webcrawler.ui.adapter.ZipMediaAdapter
import com.example.webcrawler.ui.viewmodel.ZipViewerState
import com.example.webcrawler.ui.viewmodel.ZipViewerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ZipViewerFragment : Fragment() {

    private var _binding: FragmentZipViewerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ZipViewerViewModel by viewModels()
    private lateinit var adapter: ZipMediaAdapter
    private lateinit var player: ExoPlayer

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentZipViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        player = ExoPlayer.Builder(requireContext()).build()
        adapter = ZipMediaAdapter(player)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        val localPath = ZipViewerFragmentArgs.fromBundle(requireArguments()).localPath

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.zipViewerState.collect { state ->
                    when (state) {
                        is ZipViewerState.Loading -> {
                            binding.recyclerView.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.GONE
                        }
                        is ZipViewerState.Success -> {
                            binding.recyclerView.visibility = View.VISIBLE
                            binding.tvEmptyState.visibility = View.GONE
                            adapter.submitList(state.mediaItems)
                        }
                        is ZipViewerState.Error -> {
                            binding.recyclerView.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.VISIBLE
                            binding.tvEmptyState.text = state.message
                        }
                        is ZipViewerState.Idle -> {
                            binding.recyclerView.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.GONE
                        }
                    }
                }
            }
        }

        viewModel.scanMedia(localPath)
    }

    override fun onDestroyView() {
        player.release()
        super.onDestroyView()
        _binding = null
    }
}
