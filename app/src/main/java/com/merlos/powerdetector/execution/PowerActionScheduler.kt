package com.merlos.powerdetector.execution

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager

object PowerActionScheduler {
    private const val KEY_IS_CHARGING = "key_is_charging"

    fun schedule(context: Context, isCharging: Boolean) {
        val request = OneTimeWorkRequestBuilder<PowerChangeWorker>()
            .setInputData(Data.Builder().putBoolean(KEY_IS_CHARGING, isCharging).build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    fun isCharging(inputData: androidx.work.Data): Boolean {
        return inputData.getBoolean(KEY_IS_CHARGING, false)
    }
}
