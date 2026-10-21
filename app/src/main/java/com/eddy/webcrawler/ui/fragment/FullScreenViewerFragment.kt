package com.eddy.webcrawler.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.eddy.webcrawler.data.model.ContentItem
import com.eddy.webcrawler.databinding.FragmentFullScreenViewerBinding
import com.eddy.webcrawler.ui.adapter.FullScreenImageAdapter
import com.eddy.webcrawler.ui.viewmodel.BrowseViewModel
import com.eddy.webcrawler.ui.viewmodel.PageState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FullScreenViewerFragment : Fragment() {

    private var _binding: FragmentFullScreenViewerBinding? = null
    private val binding get() = _binding!!

    // Using activityViewModels if we want to share the viewmodel, but it's better to use viewModels and load the specific index.
    // However, BrowseViewModel is currently used. Let's create a new instance of BrowseViewModel.
    private val viewModel: BrowseViewModel by viewModels()

    private lateinit var adapter: FullScreenImageAdapter
    private var isMenuVisible = true
    private var pendingPosition: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFullScreenViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val indexId = FullScreenViewerFragmentArgs.fromBundle(requireArguments()).linkIndexId
        val startPosition = savedInstanceState?.getInt("saved_position") 
            ?: FullScreenViewerFragmentArgs.fromBundle(requireArguments()).startPosition
        
        pendingPosition = startPosition

        setupFullScreen()

        adapter = FullScreenImageAdapter { x, width ->
            handleRegionClick(x, width)
        }
        binding.viewPager.adapter = adapter

        // ViewPager change listener to update title and seekbar
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateMenuState(position)
            }
        })

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.viewPager.currentItem = progress
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getContentFlow(indexId).collect { state ->
                    if (state is PageState.Success) {
                        val images = state.content.filterIsInstance<ContentItem.ImageItem>()
                        adapter.submitList(images) {
                            if (pendingPosition != -1 && images.isNotEmpty() && pendingPosition < images.size) {
                                binding.viewPager.setCurrentItem(pendingPosition, false)
                                pendingPosition = -1
                            }
                            binding.seekBar.max = if (images.isNotEmpty()) images.size - 1 else 0
                            updateMenuState(binding.viewPager.currentItem)
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (_binding != null) {
            outState.putInt("saved_position", binding.viewPager.currentItem)
        }
    }

    private fun handleRegionClick(x: Float, width: Float) {
        val currentItem = binding.viewPager.currentItem
        val totalItems = adapter.itemCount

        when {
            x < width / 4 -> {
                // Left 1/4 clicked -> Next image (assuming RTL ViewPager)
                if (currentItem < totalItems - 1) {
                    binding.viewPager.currentItem = currentItem + 1
                }
            }
            x > width * 3 / 4 -> {
                // Right 1/4 clicked -> Previous image (assuming RTL ViewPager)
                if (currentItem > 0) {
                    binding.viewPager.currentItem = currentItem - 1
                }
            }
            else -> {
                // Center 2/4 clicked -> Toggle menu
                toggleMenu()
            }
        }
    }

    private fun updateMenuState(position: Int) {
        if (position in 0 until adapter.itemCount) {
            binding.seekBar.progress = position
            val item = adapter.currentList[position]
            binding.tvTitle.text = "${position + 1} / ${adapter.itemCount} - ${item.displayName}"
        }
    }

    private fun toggleMenu() {
        isMenuVisible = !isMenuVisible
        binding.menuOverlay.visibility = if (isMenuVisible) View.VISIBLE else View.GONE
        if (isMenuVisible) {
            showSystemUI()
        } else {
            hideSystemUI()
        }
    }

    private fun setupFullScreen() {
        hideSystemUI()
        isMenuVisible = false
        binding.menuOverlay.visibility = View.GONE
    }

    private fun hideSystemUI() {
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.hide()
        val window = requireActivity().window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showSystemUI() {
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.show()
        val window = requireActivity().window
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, binding.root).show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        showSystemUI() // Restore system UI when leaving
        _binding = null
    }
}
