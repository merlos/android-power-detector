package com.merlos.powerdetector.ui

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.merlos.powerdetector.R
import com.merlos.powerdetector.data.PowerActionEntity
import com.merlos.powerdetector.databinding.DialogActionFormBinding
import com.merlos.powerdetector.domain.ActionType
import com.merlos.powerdetector.domain.PowerTrigger
import com.merlos.powerdetector.execution.ActionExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActionFormDialogFragment : DialogFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: DialogActionFormBinding? = null
    private val binding: DialogActionFormBinding
        get() = checkNotNull(_binding)
    private val qrScanner = registerForActivityResult(ScanContract()) { result ->
        val rawValue = result.contents?.trim().orEmpty()
        if (rawValue.isBlank()) {
            return@registerForActivityResult
        }

        applyTelegramQrPayload(TelegramQrParser.parse(rawValue))
    }
    private val cameraPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchTelegramQrScanner()
        } else {
            Snackbar.make(requireActivity().findViewById(android.R.id.content), R.string.camera_permission_denied, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogActionFormBinding.inflate(LayoutInflater.from(requireContext()))
        val existingAction = arguments?.getSerializable(ARG_ACTION) as? PowerActionEntity
        val actionType = existingAction?.actionType?.let(ActionType::valueOf)
            ?: ActionType.valueOf(requireArguments().getString(ARG_ACTION_TYPE).orEmpty())
        setupForm(actionType, existingAction)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existingAction == null) getDialogTitle(actionType) else getDialogTitle(actionType))
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .apply {
                if (existingAction != null) {
                    setNeutralButton(R.string.delete) { _, _ ->
                        viewModel.deleteAction(existingAction)
                        Snackbar.make(requireActivity().findViewById(android.R.id.content), R.string.action_deleted, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (validateAndSave(actionType, existingAction)) {
                    Snackbar.make(requireActivity().findViewById(android.R.id.content), R.string.action_saved, Snackbar.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        }

        return dialog
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setupForm(actionType: ActionType, existingAction: PowerActionEntity?) {
        val triggerItems = listOf(
            getString(R.string.trigger_both),
            getString(R.string.trigger_ac),
            getString(R.string.trigger_battery)
        )
        binding.triggerDropdown.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, triggerItems)
        )
        val currentTrigger = existingAction?.trigger?.let(PowerTrigger::valueOf) ?: PowerTrigger.BOTH
        binding.triggerDropdown.setText(
            when (currentTrigger) {
                PowerTrigger.BOTH -> triggerItems[0]
                PowerTrigger.ON_AC_POWER -> triggerItems[1]
                PowerTrigger.ON_BATTERY -> triggerItems[2]
            },
            false
        )
        binding.enabledSwitch.isChecked = existingAction?.enabled ?: true
        binding.messageLayout.hint = getString(R.string.message_label)
        binding.messageEditText.setText(existingAction?.message.orEmpty())

        if (actionType == ActionType.SMS) {
            binding.recipientLayout.hint = getString(R.string.recipient_phone)
            binding.botTokenLayout.visibility = android.view.View.GONE
            binding.importQrButton.visibility = android.view.View.GONE
            binding.importQrHelperText.visibility = android.view.View.GONE
            binding.recipientEditText.inputType = android.text.InputType.TYPE_CLASS_PHONE
        } else {
            binding.recipientLayout.hint = getString(R.string.recipient_chat_id)
            binding.botTokenLayout.visibility = android.view.View.VISIBLE
            binding.importQrButton.visibility = android.view.View.VISIBLE
            binding.importQrHelperText.visibility = android.view.View.VISIBLE
            binding.botTokenLayout.hint = getString(R.string.bot_token_label)
            binding.botTokenEditText.setText(existingAction?.botToken.orEmpty())
            binding.recipientEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT
            binding.importQrButton.setOnClickListener {
                startTelegramQrScan()
            }
        }
        binding.recipientEditText.setText(existingAction?.recipient.orEmpty())

        binding.testActionButton.setOnClickListener {
            runTestAction(actionType, existingAction)
        }
    }

    private fun validateAndSave(actionType: ActionType, existingAction: PowerActionEntity?): Boolean {
        val formState = readFormState(actionType)
        if (formState == null) {
            Snackbar.make(requireActivity().findViewById(android.R.id.content), R.string.validation_required, Snackbar.LENGTH_SHORT).show()
            return false
        }

        viewModel.saveAction(
            actionId = existingAction?.id ?: 0,
            actionType = actionType,
            trigger = formState.trigger,
            recipient = formState.recipient,
            botToken = formState.botToken,
            message = formState.message,
            enabled = binding.enabledSwitch.isChecked
        )
        return true
    }

    private fun runTestAction(actionType: ActionType, existingAction: PowerActionEntity?) {
        val formState = readFormState(actionType) ?: run {
            Snackbar.make(requireActivity().findViewById(android.R.id.content), R.string.validation_required, Snackbar.LENGTH_SHORT).show()
            return
        }
        val appContext = requireContext().applicationContext

        val draftAction = PowerActionEntity(
            id = existingAction?.id ?: 0,
            actionType = actionType.name,
            trigger = formState.trigger.name,
            recipient = formState.recipient,
            botToken = formState.botToken,
            message = formState.message,
            enabled = binding.enabledSwitch.isChecked,
            createdAt = existingAction?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        binding.testActionButton.isEnabled = false
        binding.testResultText.isVisible = true
        binding.testResultText.text = getString(R.string.action_test_running)

        lifecycleScope.launch(Dispatchers.IO) {
            val result = ActionExecutor(appContext).execute(
                draftAction,
                resolveTestChargingState(formState.trigger)
            )
            if (draftAction.id > 0) {
                viewModel.recordExecutionResult(draftAction.id, result.message)
            }
            withContext(Dispatchers.Main) {
                binding.testActionButton.isEnabled = true
                binding.testResultText.text = if (result.success) {
                    getString(R.string.action_test_success, result.message)
                } else {
                    getString(R.string.action_test_failure, result.message)
                }
            }
        }
    }

    private fun startTelegramQrScan() {
        val permissionState = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        if (permissionState == PackageManager.PERMISSION_GRANTED) {
            launchTelegramQrScanner()
        } else {
            cameraPermissionRequest.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchTelegramQrScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt(getString(R.string.qr_scan_prompt))
            setBeepEnabled(false)
            setOrientationLocked(true)
        }
        qrScanner.launch(options)
    }

    private fun applyTelegramQrPayload(payload: TelegramQrPayload?) {
        if (payload == null) {
            Snackbar.make(requireActivity().findViewById(android.R.id.content), R.string.qr_scan_failed, Snackbar.LENGTH_SHORT).show()
            return
        }

        binding.botTokenEditText.setText(payload.botId)
        binding.recipientEditText.setText(payload.chatId)
        binding.botTokenLayout.error = null
        binding.recipientLayout.error = null
        Snackbar.make(requireActivity().findViewById(android.R.id.content), R.string.qr_scan_success, Snackbar.LENGTH_SHORT).show()
    }

    private fun readFormState(actionType: ActionType): FormState? {
        val recipient = binding.recipientEditText.text?.toString()?.trim().orEmpty()
        val botToken = binding.botTokenEditText.text?.toString()?.trim().orEmpty()
        val message = binding.messageEditText.text?.toString()?.trim().orEmpty()
        val trigger = when (binding.triggerDropdown.text.toString()) {
            getString(R.string.trigger_ac) -> PowerTrigger.ON_AC_POWER
            getString(R.string.trigger_battery) -> PowerTrigger.ON_BATTERY
            else -> PowerTrigger.BOTH
        }

        binding.recipientLayout.error = null
        binding.botTokenLayout.error = null
        binding.messageLayout.error = null

        var valid = true
        if (recipient.isBlank()) {
            binding.recipientLayout.error = if (actionType == ActionType.SMS) {
                getString(R.string.error_phone_required)
            } else {
                getString(R.string.error_chat_required)
            }
            valid = false
        }
        if (actionType == ActionType.TELEGRAM && botToken.isBlank()) {
            binding.botTokenLayout.error = getString(R.string.error_bot_token_required)
            valid = false
        }
        if (message.isBlank()) {
            binding.messageLayout.error = getString(R.string.error_message_required)
            valid = false
        }

        if (!valid) {
            return null
        }

        return FormState(
            trigger = trigger,
            recipient = recipient,
            botToken = botToken.ifBlank { null },
            message = message
        )
    }

    private fun resolveTestChargingState(trigger: PowerTrigger): Boolean {
        return when (trigger) {
            PowerTrigger.ON_AC_POWER -> true
            PowerTrigger.ON_BATTERY -> false
            PowerTrigger.BOTH -> readCurrentChargingState()
        }
    }

    private fun readCurrentChargingState(): Boolean {
        val context = requireContext().applicationContext
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        return plugged == BatteryManager.BATTERY_PLUGGED_AC ||
            plugged == BatteryManager.BATTERY_PLUGGED_USB ||
            plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
    }

    private fun getDialogTitle(actionType: ActionType): String {
        return if (actionType == ActionType.SMS) {
            getString(R.string.action_type_sms)
        } else {
            getString(R.string.action_type_telegram)
        }
    }

    companion object {
        private const val ARG_ACTION_TYPE = "arg_action_type"
        private const val ARG_ACTION = "arg_action"

        fun newInstance(actionType: ActionType, action: PowerActionEntity? = null): ActionFormDialogFragment {
            return ActionFormDialogFragment().apply {
                arguments = bundleOf(
                    ARG_ACTION_TYPE to actionType.name,
                    ARG_ACTION to action
                )
            }
        }
    }

    private data class FormState(
        val trigger: PowerTrigger,
        val recipient: String,
        val botToken: String?,
        val message: String
    )
}
