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
import com.eddy.webcrawler.R
import com.eddy.webcrawler.databinding.FragmentSelectIndexBinding
import com.eddy.webcrawler.ui.adapter.SelectIndexAdapter
import com.eddy.webcrawler.ui.viewmodel.SelectIndexViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SelectIndexFragment : Fragment() {

    private var _binding: FragmentSelectIndexBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SelectIndexViewModel by viewModels()
    private lateinit var adapter: SelectIndexAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectIndexBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
    }

    private fun setupRecyclerView() {
        adapter = SelectIndexAdapter(
            onDisplayClick = { indexId ->
                val action = SelectIndexFragmentDirections.actionSelectIndexFragmentToBrowseFragment(indexId)
                findNavController().navigate(action)
            },
            onDeleteClick = { indexId ->
                showDeleteConfirmation(indexId)
            }
        )
        binding.recyclerView.adapter = adapter
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.indices.collect { list ->
                    adapter.submitList(list)
                    binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun showDeleteConfirmation(indexId: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_index_confirm_title)
            .setMessage(R.string.delete_index_confirm_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteIndex(indexId)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
