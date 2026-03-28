package com.merlos.powerdetector.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.merlos.powerdetector.execution.PowerActionScheduler

class PowerStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> PowerActionScheduler.schedule(context, true)
            Intent.ACTION_POWER_DISCONNECTED -> PowerActionScheduler.schedule(context, false)
        }
    }
}
