package com.eddy.webcrawler.ui.fragment

import android.os.Bundle
import android.view.*
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.eddy.webcrawler.R
import com.eddy.webcrawler.databinding.FragmentBrowseBinding
import com.eddy.webcrawler.ui.adapter.LinkIndexPagerAdapter
import com.eddy.webcrawler.ui.viewmodel.BrowseViewModel
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

        setupMenu()
        val linkIndexId = BrowseFragmentArgs.fromBundle(requireArguments()).linkIndexId

        adapter = LinkIndexPagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 1

        if (linkIndexId == 0L) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.allIndices.collect { indices ->
                        if (indices.isNotEmpty()) {
                            adapter.updateIndices(indices.map { it.id })
                            binding.tvEmptyState.visibility = View.GONE
                        } else {
                            adapter.updateIndices(emptyList())
                            binding.tvEmptyState.visibility = View.VISIBLE
                            binding.tvEmptyState.text = "尚未有爬取紀錄"
                        }
                    }
                }
            }
        } else {
            adapter.updateIndices(listOf(linkIndexId))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isEditMode.collect {
                        activity?.invalidateOptionsMenu()
                    }
                }
                launch {
                    viewModel.selectedIds.collect {
                        activity?.invalidateOptionsMenu()
                    }
                }
            }
        }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_browse, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                val isEditMode = viewModel.isEditMode.value
                val hasSelection = viewModel.selectedIds.value.isNotEmpty()

                menu.findItem(R.id.action_full_screen).isVisible = !isEditMode
                menu.findItem(R.id.action_edit).isVisible = !isEditMode
                menu.findItem(R.id.action_cancel_edit).isVisible = isEditMode
                menu.findItem(R.id.action_delete_selected).isVisible = isEditMode && hasSelection
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_full_screen -> {
                        val indexId = adapter.getItemId(binding.viewPager.currentItem)
                        if (indexId != 0L) {
                            val action = BrowseFragmentDirections.actionBrowseFragmentToFullScreenViewerFragment(
                                linkIndexId = indexId,
                                startPosition = 0
                            )
                            androidx.navigation.fragment.NavHostFragment.findNavController(this@BrowseFragment).navigate(action)
                        }
                        true
                    }
                    R.id.action_edit, R.id.action_cancel_edit -> {
                        viewModel.toggleEditMode()
                        true
                    }
                    R.id.action_delete_selected -> {
                        viewModel.deleteSelectedItems()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
