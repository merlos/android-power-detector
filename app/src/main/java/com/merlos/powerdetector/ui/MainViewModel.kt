package com.merlos.powerdetector.ui

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.merlos.powerdetector.data.AppDatabase
import com.merlos.powerdetector.data.PowerActionEntity
import com.merlos.powerdetector.domain.ActionType
import com.merlos.powerdetector.domain.PowerTrigger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).powerActionDao()

    val actions: LiveData<List<PowerActionEntity>> = dao.observeAll().asLiveData()

    private val _isCharging = MutableLiveData(readChargingState())
    val isCharging: LiveData<Boolean> = _isCharging

    fun updatePowerState(isCharging: Boolean) {
        _isCharging.value = isCharging
    }

    fun refreshPowerState() {
        _isCharging.value = readChargingState()
    }

    fun saveAction(
        actionId: Long,
        actionType: ActionType,
        trigger: PowerTrigger,
        recipient: String,
        botToken: String?,
        message: String,
        enabled: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = if (actionId > 0) dao.findById(actionId) else null
            val now = System.currentTimeMillis()
            val action = PowerActionEntity(
                id = actionId,
                actionType = actionType.name,
                trigger = trigger.name,
                recipient = recipient,
                botToken = botToken,
                message = message,
                enabled = enabled,
                lastResult = existing?.lastResult,
                lastExecutedAt = existing?.lastExecutedAt,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            dao.upsert(action)
        }
    }

    fun deleteAction(action: PowerActionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(action)
        }
    }

    fun recordExecutionResult(actionId: Long, message: String) {
        if (actionId <= 0) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateExecution(
                actionId = actionId,
                result = message,
                executedAt = System.currentTimeMillis()
            )
        }
    }

    fun hasEnabledSmsAction(actions: List<PowerActionEntity>): Boolean {
        return actions.any { it.enabled && it.actionType == ActionType.SMS.name }
    }

    private fun readChargingState(): Boolean {
        val context = getApplication<Application>()
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        return plugged == BatteryManager.BATTERY_PLUGGED_AC ||
            plugged == BatteryManager.BATTERY_PLUGGED_USB ||
            plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
    }
}
