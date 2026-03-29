package com.merlos.powerdetector.execution

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

object PowerActionScheduler {
    private const val KEY_IS_CHARGING = "key_is_charging"
    private const val TAG = "PowerActionScheduler"

    fun schedule(context: Context, isCharging: Boolean) {
        val request = OneTimeWorkRequestBuilder<PowerChangeWorker>()
            .setInputData(Data.Builder().putBoolean(KEY_IS_CHARGING, isCharging).build())
            .build()

        WorkManager.getInstance(context).enqueue(request)
        Log.d(TAG, "PowerChangeWorker enqueued for isCharging=$isCharging")
    }

    fun isCharging(inputData: androidx.work.Data): Boolean {
        return inputData.getBoolean(KEY_IS_CHARGING, false)
    }
}
