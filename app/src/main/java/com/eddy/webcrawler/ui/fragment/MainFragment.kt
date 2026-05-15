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
import com.eddy.webcrawler.ui.viewmodel.CrawlState
import com.eddy.webcrawler.ui.viewmodel.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

        checkAppLock()
        setupObservers()
        setupClickListeners()
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
                    viewModel.lastUrl.collect { url ->
                        if (binding.etUrl.text.isNullOrBlank() && url.isNotBlank()) {
                            binding.etUrl.setText(url)
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
        binding.btnCrawl.setOnClickListener {
            val url = binding.etUrl.text?.toString()?.trim() ?: ""
            val pattern = binding.etPattern.text?.toString()?.trim() ?: ""
            val rule = binding.etRule.text?.toString()?.trim() ?: ""
            viewModel.crawl(url, pattern, rule)
        }

        binding.btnRetry.setOnClickListener {
            val url = binding.etUrl.text?.toString()?.trim() ?: ""
            val pattern = binding.etPattern.text?.toString()?.trim() ?: ""
            val rule = binding.etRule.text?.toString()?.trim() ?: ""
            viewModel.crawl(url, pattern, rule)
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

    private fun showSuccessState(result: CrawlResult) {
        binding.progressBar.visibility = View.GONE
        binding.btnCrawl.isEnabled = true
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
