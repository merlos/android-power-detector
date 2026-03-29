package com.merlos.powerdetector.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.merlos.powerdetector.MainActivity
import com.merlos.powerdetector.R
import com.merlos.powerdetector.receiver.PowerStateReceiver

class PowerMonitorService : Service() {

    private val powerReceiver = PowerStateReceiver()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: foreground service starting")
        startForeground(NOTIFICATION_ID, buildNotification())
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        ContextCompat.registerReceiver(this, powerReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        Log.d(TAG, "onStartCommand: power receiver registered dynamically")
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(powerReceiver)
        Log.d(TAG, "onDestroy: power receiver unregistered")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.monitor_notification_title))
            .setContentText(getString(R.string.monitor_notification_text))
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "PowerMonitorService"
        const val CHANNEL_ID = "power_monitor"
        const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            Log.d(TAG, "start: requesting foreground service")
            context.startForegroundService(Intent(context, PowerMonitorService::class.java))
        }
    }
}
