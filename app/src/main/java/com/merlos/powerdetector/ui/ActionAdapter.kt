package com.merlos.powerdetector.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.merlos.powerdetector.R
import com.merlos.powerdetector.data.PowerActionEntity
import com.merlos.powerdetector.databinding.ItemActionBinding
import com.merlos.powerdetector.domain.ActionType
import com.merlos.powerdetector.domain.PowerTrigger
import java.text.DateFormat
import java.util.Date

class ActionAdapter(
    private val onActionTapped: (PowerActionEntity) -> Unit
) : ListAdapter<PowerActionEntity, ActionAdapter.ActionViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemActionBinding.inflate(inflater, parent, false)
        return ActionViewHolder(binding, onActionTapped)
    }

    override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ActionViewHolder(
        private val binding: ItemActionBinding,
        private val onActionTapped: (PowerActionEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(action: PowerActionEntity) {
            val context = binding.root.context
            val actionType = ActionType.valueOf(action.actionType)
            val trigger = PowerTrigger.valueOf(action.trigger)
            binding.root.setOnClickListener { onActionTapped(action) }
            binding.typeIndicator.setBackgroundResource(
                if (actionType == ActionType.SMS) R.drawable.bg_indicator_sms else R.drawable.bg_indicator_telegram
            )
            binding.titleText.text = if (actionType == ActionType.SMS) {
                context.getString(R.string.action_sms_title, action.recipient)
            } else {
                context.getString(R.string.action_telegram_title, action.recipient)
            }
            val triggerLabel = if (trigger == PowerTrigger.ON_AC_POWER) {
                context.getString(R.string.trigger_ac)
            } else {
                context.getString(R.string.trigger_battery)
            }
            val stateLabel = if (action.enabled) {
                context.getString(R.string.action_enabled_short)
            } else {
                context.getString(R.string.action_disabled)
            }
            binding.subtitleText.text = context.getString(R.string.action_subtitle, triggerLabel, stateLabel)
            binding.resultText.text = when {
                action.lastExecutedAt == null -> context.getString(R.string.action_last_never)
                action.lastResult == context.getString(R.string.execution_success) -> {
                    context.getString(
                        R.string.action_last_success,
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                            .format(Date(action.lastExecutedAt))
                    )
                }
                else -> {
                    context.getString(
                        R.string.action_last_failure,
                        action.lastResult ?: context.getString(R.string.action_last_never)
                    )
                }
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<PowerActionEntity>() {
        override fun areItemsTheSame(oldItem: PowerActionEntity, newItem: PowerActionEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PowerActionEntity, newItem: PowerActionEntity): Boolean {
            return oldItem == newItem
        }
    }
}
