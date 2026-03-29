package com.merlos.powerdetector.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.merlos.powerdetector.execution.PowerActionScheduler

class PowerStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isCharging = when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> true
            Intent.ACTION_POWER_DISCONNECTED -> false
            else -> return
        }
        Log.d(TAG, "Power state broadcast received: isCharging=$isCharging")
        PowerActionScheduler.schedule(context, isCharging)
    }

    companion object {
        private const val TAG = "PowerStateReceiver"
    }
}
