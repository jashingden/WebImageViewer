package com.eddy.webcrawler.ui.fragment

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.eddy.webcrawler.R
import com.eddy.webcrawler.databinding.FragmentSettingsBinding
import com.eddy.webcrawler.ui.viewmodel.SettingsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.lockEnabled.collect { enabled ->
                    if (binding.switchLock.isChecked != enabled) {
                        binding.switchLock.isChecked = enabled
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.switchLock.setOnClickListener {
            val isChecked = binding.switchLock.isChecked
            if (isChecked) {
                // If turning ON, need to set PIN
                showPinInputDialog(true)
            } else {
                // If turning OFF, just update
                viewModel.setLockEnabled(false)
            }
        }
    }

    private fun showPinInputDialog(isFirstTime: Boolean, firstPin: String? = null) {
        val title = if (isFirstTime) getString(R.string.settings_lock_prompt) else getString(R.string.settings_lock_confirm_prompt)
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

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(container)
            .setPositiveButton("下一步", null)
            .setNegativeButton("取消") { _, _ ->
                // Revert switch if cancelled during setup
                binding.switchLock.isChecked = viewModel.lockEnabled.value
            }
            .setCancelable(false)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val pin = editText.text.toString()
            if (pin.length == 4) {
                if (isFirstTime) {
                    dialog.dismiss()
                    showPinInputDialog(false, pin)
                } else {
                    if (pin == firstPin) {
                        viewModel.setLockPin(pin)
                        Toast.makeText(requireContext(), R.string.settings_lock_success, Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(requireContext(), R.string.settings_lock_mismatch, Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        showPinInputDialog(true) // Start over
                    }
                }
            } else {
                editText.error = "請輸入 4 位數"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
