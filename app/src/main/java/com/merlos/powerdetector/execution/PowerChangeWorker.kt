package com.merlos.powerdetector.execution

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class PowerChangeWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val isCharging = PowerActionScheduler.isCharging(inputData)
        Log.d(TAG, "doWork started: isCharging=$isCharging runAttemptCount=$runAttemptCount")
        return try {
            PowerChangeProcessor.process(applicationContext, isCharging)
            Log.d(TAG, "doWork completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "doWork failed: ${e.message}", e)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "PowerChangeWorker"
    }
}
