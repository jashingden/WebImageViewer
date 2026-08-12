package com.eddy.webcrawler.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.eddy.webcrawler.databinding.FragmentZipViewerBinding
import com.eddy.webcrawler.ui.adapter.ZipMediaAdapter
import com.eddy.webcrawler.ui.viewmodel.ZipViewerState
import com.eddy.webcrawler.ui.viewmodel.ZipViewerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ZipViewerFragment : Fragment() {

    private var _binding: FragmentZipViewerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ZipViewerViewModel by viewModels()
    private lateinit var adapter: ZipMediaAdapter
    private lateinit var player: ExoPlayer

    private var hasAutoOpenedPicker = false

    private val loadArchiveLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            viewModel.importArchive(it)
        }
    }

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

        binding.btnSelectFolder.setOnClickListener {
            launchArchivePicker()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.zipViewerState.collect { state ->
                    when (state) {
                        is ZipViewerState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.recyclerView.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.GONE
                            binding.btnSelectFolder.visibility = View.GONE
                        }
                        is ZipViewerState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.recyclerView.visibility = View.VISIBLE
                            binding.tvEmptyState.visibility = View.GONE
                            binding.btnSelectFolder.visibility = View.GONE
                            adapter.submitList(state.mediaItems)
                        }
                        is ZipViewerState.ImportSuccess -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), "壓縮檔載入成功！", Toast.LENGTH_SHORT).show()
                            val action = ZipViewerFragmentDirections.actionZipViewerFragmentToBrowseFragment(state.linkIndexId)
                            findNavController().navigate(action)
                            viewModel.resetState()
                        }
                        is ZipViewerState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.recyclerView.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.VISIBLE
                            binding.tvEmptyState.text = state.message
                            binding.btnSelectFolder.visibility = View.VISIBLE
                            binding.btnSelectFolder.text = "選擇壓縮檔"
                        }
                        is ZipViewerState.Idle -> {
                            binding.progressBar.visibility = View.GONE
                            if (localPath != null) {
                                viewModel.scanMedia(localPath)
                            } else {
                                binding.recyclerView.visibility = View.GONE
                                binding.tvEmptyState.visibility = View.VISIBLE
                                binding.tvEmptyState.text = "請選擇要載入的壓縮檔 (zip, 7z, tar.gz)"
                                binding.btnSelectFolder.visibility = View.VISIBLE
                                binding.btnSelectFolder.text = "選擇壓縮檔"

                                if (!hasAutoOpenedPicker) {
                                    hasAutoOpenedPicker = true
                                    launchArchivePicker()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun launchArchivePicker() {
        val mimeTypes = arrayOf(
            "application/zip",
            "application/x-7z-compressed",
            "application/x-gzip",
            "application/x-tar",
            "application/x-gtar",
            "application/x-compressed-tar",
            "application/octet-stream",
            "*/*"
        )
        loadArchiveLauncher.launch(mimeTypes)
    }

    override fun onDestroyView() {
        player.release()
        super.onDestroyView()
        _binding = null
    }
}
