package com.eddy.webcrawler.ui.fragment

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.eddy.webcrawler.R
import com.eddy.webcrawler.data.repository.CrawlResult
import com.eddy.webcrawler.data.repository.SettingsRepository
import com.eddy.webcrawler.databinding.FragmentMainBinding
import com.eddy.webcrawler.ui.adapter.GeneralLinkAdapter
import com.eddy.webcrawler.ui.viewmodel.CrawlState
import com.eddy.webcrawler.ui.viewmodel.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : Fragment() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()
    private lateinit var generalLinkAdapter: GeneralLinkAdapter
    private var isSyncingUrl = false
    private var isUpdatingSelectAll = false

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

        setupAdapter()
        checkAppLock()
        setupTabs()
        setupUrlSync()
        setupObservers()
        setupClickListeners()
    }

    private fun setupAdapter() {
        generalLinkAdapter = GeneralLinkAdapter { position, isChecked ->
            viewModel.toggleLink(position, isChecked)
        }
        binding.rvGeneralLinks.adapter = generalLinkAdapter
    }

    private fun checkAppLock() {
        if (viewModel.isAppUnlocked) return

        viewLifecycleOwner.lifecycleScope.launch {
            val isEnabled = settingsRepository.lockEnabled.first()
            if (isEnabled) {
                val pin = settingsRepository.lockPin.first()
                showAppLockDialog(pin)
            } else {
                viewModel.setUnlocked()
            }
        }
    }

    private fun showAppLockDialog(correctPin: String?) {
        val editText = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf(InputFilter.LengthFilter(4))
            hint = "1234"
        }
        val container = FrameLayout(requireContext())
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(48, 20, 48, 20)
        editText.layoutParams = params
        container.addView(editText)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.lock_screen_title)
            .setView(container)
            .setPositiveButton("確定", null)
            .setCancelable(false)
            .setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    requireActivity().finish()
                    true
                } else false
            }
            .create()
            .apply {
                show()
                getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val enteredPin = editText.text.toString()
                    if (enteredPin == correctPin) {
                        viewModel.setUnlocked()
                        dismiss()
                    } else {
                        dismiss()
                        showLockErrorAndExit()
                    }
                }
            }
    }

    private fun showLockErrorAndExit() {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.lock_error_wrong_pin)
            .setPositiveButton("離開") { _, _ -> requireActivity().finish() }
            .setCancelable(false)
            .show()
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        binding.layoutGeneral.visibility = View.VISIBLE
                        binding.layoutAdvanced.visibility = View.GONE
                    }
                    1 -> {
                        binding.layoutGeneral.visibility = View.GONE
                        binding.layoutAdvanced.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupUrlSync() {
        binding.etGeneralUrl.doAfterTextChanged { text ->
            if (!isSyncingUrl) {
                isSyncingUrl = true
                if (binding.etAdvancedUrl.text.toString() != text.toString()) {
                    binding.etAdvancedUrl.setText(text)
                }
                isSyncingUrl = false
            }
        }

        binding.etAdvancedUrl.doAfterTextChanged { text ->
            if (!isSyncingUrl) {
                isSyncingUrl = true
                if (binding.etGeneralUrl.text.toString() != text.toString()) {
                    binding.etGeneralUrl.setText(text)
                }
                isSyncingUrl = false
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.crawlState.collect { state ->
                        when (state) {
                            is CrawlState.Idle -> showIdleState()
                            is CrawlState.Loading -> showLoadingState()
                            is CrawlState.Success -> showSuccessState(state.content)
                            is CrawlState.Error -> showErrorState(state.message)
                        }
                    }
                }

                launch {
                    viewModel.homeLinks.collect { links ->
                        generalLinkAdapter.submitList(links)
                        if (links.isNotEmpty()) {
                            binding.layoutLinkList.visibility = View.VISIBLE
                            val allChecked = links.all { it.isChecked }
                            isUpdatingSelectAll = true
                            binding.cbSelectAll.isChecked = allChecked
                            isUpdatingSelectAll = false

                            val selectedCount = links.count { it.isChecked }
                            binding.tvSelectedCount.text = "已選擇 $selectedCount / ${links.size} 個"
                            binding.btnGeneralCrawl.isEnabled = selectedCount > 0
                        } else {
                            binding.layoutLinkList.visibility = View.GONE
                        }
                    }
                }

                launch {
                    viewModel.lastUrl.collect { url ->
                        if (url.isNotBlank()) {
                            if (binding.etGeneralUrl.text.isNullOrBlank()) {
                                binding.etGeneralUrl.setText(url)
                            }
                            if (binding.etAdvancedUrl.text.isNullOrBlank()) {
                                binding.etAdvancedUrl.setText(url)
                            }
                        }
                    }
                }

                launch {
                    viewModel.lastPattern.collect { pattern ->
                        if (binding.etPattern.text.isNullOrBlank()) {
                            binding.etPattern.setText(pattern)
                        }
                    }
                }

                launch {
                    viewModel.lastRule.collect { rule ->
                        if (binding.etRule.text.isNullOrBlank()) {
                            binding.etRule.setText(rule)
                        }
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingSelectAll) {
                viewModel.toggleAllLinks(isChecked)
            }
        }

        binding.btnFetchLinks.setOnClickListener {
            val url = binding.etGeneralUrl.text?.toString()?.trim() ?: ""
            viewModel.fetchHomeLinks(url)
        }

        binding.btnGeneralCrawl.setOnClickListener {
            val url = binding.etGeneralUrl.text?.toString()?.trim() ?: ""
            viewModel.crawlSelectedGeneralLinks(url)
        }

        binding.btnAdvancedCrawl.setOnClickListener {
            val url = binding.etAdvancedUrl.text?.toString()?.trim() ?: ""
            val pattern = binding.etPattern.text?.toString()?.trim() ?: ""
            val rule = binding.etRule.text?.toString()?.trim() ?: ""
            viewModel.crawlAdvanced(url, pattern, rule)
        }

        binding.btnRetry.setOnClickListener {
            if (binding.tabLayout.selectedTabPosition == 1) {
                val url = binding.etAdvancedUrl.text?.toString()?.trim() ?: ""
                val pattern = binding.etPattern.text?.toString()?.trim() ?: ""
                val rule = binding.etRule.text?.toString()?.trim() ?: ""
                viewModel.crawlAdvanced(url, pattern, rule)
            } else {
                val url = binding.etGeneralUrl.text?.toString()?.trim() ?: ""
                if (viewModel.homeLinks.value.isEmpty()) {
                    viewModel.fetchHomeLinks(url)
                } else {
                    viewModel.crawlSelectedGeneralLinks(url)
                }
            }
        }
    }

    private fun showIdleState() {
        binding.progressBar.visibility = View.GONE
        binding.btnFetchLinks.isEnabled = true
        binding.btnGeneralCrawl.isEnabled = viewModel.homeLinks.value.any { it.isChecked }
        binding.btnAdvancedCrawl.isEnabled = true
        binding.tvError.visibility = View.GONE
        binding.btnRetry.visibility = View.GONE
    }

    private fun showLoadingState() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnFetchLinks.isEnabled = false
        binding.btnGeneralCrawl.isEnabled = false
        binding.btnAdvancedCrawl.isEnabled = false
        binding.tvError.visibility = View.GONE
        binding.btnRetry.visibility = View.GONE
    }

    private fun showSuccessState(result: CrawlResult) {
        binding.progressBar.visibility = View.GONE
        binding.btnFetchLinks.isEnabled = true
        binding.btnGeneralCrawl.isEnabled = true
        binding.btnAdvancedCrawl.isEnabled = true
        binding.tvError.visibility = View.GONE
        binding.btnRetry.visibility = View.GONE

        val totalEntries = result.totalEntries
        Toast.makeText(
            requireContext(),
            "爬取完成：找到 $totalEntries 個連結",
            Toast.LENGTH_SHORT
        ).show()

        val action = MainFragmentDirections.actionMainFragmentToBrowseFragment(result.linkIndexId)
        findNavController().navigate(action)
        viewModel.resetCrawlState()
    }

    private fun showErrorState(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.btnFetchLinks.isEnabled = true
        binding.btnGeneralCrawl.isEnabled = viewModel.homeLinks.value.any { it.isChecked }
        binding.btnAdvancedCrawl.isEnabled = true
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
        binding.btnRetry.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
