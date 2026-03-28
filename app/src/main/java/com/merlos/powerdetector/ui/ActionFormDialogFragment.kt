package com.merlos.powerdetector.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.merlos.powerdetector.R
import com.merlos.powerdetector.data.PowerActionEntity
import com.merlos.powerdetector.databinding.DialogActionFormBinding
import com.merlos.powerdetector.domain.ActionType
import com.merlos.powerdetector.domain.PowerTrigger

class ActionFormDialogFragment : DialogFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: DialogActionFormBinding? = null
    private val binding: DialogActionFormBinding
        get() = checkNotNull(_binding)

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
            getString(R.string.trigger_ac),
            getString(R.string.trigger_battery)
        )
        binding.triggerDropdown.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, triggerItems)
        )
        val currentTrigger = existingAction?.trigger?.let(PowerTrigger::valueOf) ?: PowerTrigger.ON_AC_POWER
        binding.triggerDropdown.setText(
            if (currentTrigger == PowerTrigger.ON_AC_POWER) triggerItems[0] else triggerItems[1],
            false
        )
        binding.enabledSwitch.isChecked = existingAction?.enabled ?: true
        binding.messageLayout.hint = getString(R.string.message_label)
        binding.messageEditText.setText(existingAction?.message.orEmpty())

        if (actionType == ActionType.SMS) {
            binding.recipientLayout.hint = getString(R.string.recipient_phone)
            binding.botTokenLayout.visibility = android.view.View.GONE
            binding.recipientEditText.inputType = android.text.InputType.TYPE_CLASS_PHONE
        } else {
            binding.recipientLayout.hint = getString(R.string.recipient_chat_id)
            binding.botTokenLayout.visibility = android.view.View.VISIBLE
            binding.botTokenLayout.hint = getString(R.string.bot_token_label)
            binding.botTokenEditText.setText(existingAction?.botToken.orEmpty())
            binding.recipientEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        binding.recipientEditText.setText(existingAction?.recipient.orEmpty())
    }

    private fun validateAndSave(actionType: ActionType, existingAction: PowerActionEntity?): Boolean {
        val recipient = binding.recipientEditText.text?.toString()?.trim().orEmpty()
        val botToken = binding.botTokenEditText.text?.toString()?.trim().orEmpty()
        val message = binding.messageEditText.text?.toString()?.trim().orEmpty()
        val trigger = if (binding.triggerDropdown.text.toString() == getString(R.string.trigger_battery)) {
            PowerTrigger.ON_BATTERY
        } else {
            PowerTrigger.ON_AC_POWER
        }

        val isInvalid = recipient.isBlank() ||
            message.isBlank() ||
            (actionType == ActionType.TELEGRAM && botToken.isBlank())
        if (isInvalid) {
            Snackbar.make(requireActivity().findViewById(android.R.id.content), R.string.validation_required, Snackbar.LENGTH_SHORT).show()
            return false
        }

        viewModel.saveAction(
            actionId = existingAction?.id ?: 0,
            actionType = actionType,
            trigger = trigger,
            recipient = recipient,
            botToken = botToken.ifBlank { null },
            message = message,
            enabled = binding.enabledSwitch.isChecked
        )
        return true
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
}
