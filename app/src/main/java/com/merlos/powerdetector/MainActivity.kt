package com.merlos.powerdetector

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.merlos.powerdetector.data.PowerActionEntity
import com.merlos.powerdetector.databinding.ActivityMainBinding
import com.merlos.powerdetector.domain.ActionType
import com.merlos.powerdetector.ui.ActionAdapter
import com.merlos.powerdetector.ui.ActionFormDialogFragment
import com.merlos.powerdetector.ui.MainViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: ActionAdapter
    private var requestedSmsPermission = false

    private val smsPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Snackbar.make(binding.root, R.string.sms_permission_denied, Snackbar.LENGTH_LONG).show()
        }
        refreshPermissionHint(adapter.currentList)
    }

    private val powerStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> viewModel.updatePowerState(true)
                Intent.ACTION_POWER_DISCONNECTED -> viewModel.updatePowerState(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ActionAdapter(::showEditActionDialog)
        binding.actionsRecyclerView.adapter = adapter

        binding.addActionButton.setOnClickListener { showActionTypeChooser() }
        binding.addActionInlineButton.setOnClickListener { showActionTypeChooser() }
        binding.addActionEmptyButton.setOnClickListener { showActionTypeChooser() }

        observeViewModel()
        viewModel.refreshPowerState()
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(
            powerStateReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }
        )
    }

    override fun onStop() {
        unregisterReceiver(powerStateReceiver)
        super.onStop()
    }

    private fun observeViewModel() {
        viewModel.isCharging.observe(this) { isCharging ->
            binding.powerStatusText.text = getString(if (isCharging) R.string.status_ac else R.string.status_battery)
            binding.powerSummaryText.text = getString(
                if (isCharging) R.string.status_ac_summary else R.string.status_battery_summary
            )
        }

        viewModel.actions.observe(this) { actions ->
            adapter.submitList(actions)
            val empty = actions.isEmpty()
            binding.emptyStateLayout.visibility = if (empty) View.VISIBLE else View.GONE
            binding.actionsRecyclerView.visibility = if (empty) View.GONE else View.VISIBLE
            refreshPermissionHint(actions)
        }
    }

    private fun showActionTypeChooser() {
        val labels = arrayOf(
            getString(R.string.action_type_sms),
            getString(R.string.action_type_telegram)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.chooser_title)
            .setItems(labels) { _, which ->
                val selectedType = if (which == 0) ActionType.SMS else ActionType.TELEGRAM
                ActionFormDialogFragment.newInstance(selectedType)
                    .show(supportFragmentManager, "create-action")
            }
            .show()
    }

    private fun showEditActionDialog(action: PowerActionEntity) {
        ActionFormDialogFragment.newInstance(ActionType.valueOf(action.actionType), action)
            .show(supportFragmentManager, "edit-action-${action.id}")
    }

    private fun refreshPermissionHint(actions: List<PowerActionEntity>) {
        val hasSmsAction = viewModel.hasEnabledSmsAction(actions)
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
        binding.permissionHintText.visibility = if (hasSmsAction && !granted) View.VISIBLE else View.GONE
        if (hasSmsAction && !granted && !requestedSmsPermission) {
            requestedSmsPermission = true
            smsPermissionRequest.launch(Manifest.permission.SEND_SMS)
        }
    }
}
