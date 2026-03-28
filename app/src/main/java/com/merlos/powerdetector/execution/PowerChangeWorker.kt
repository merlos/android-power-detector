package com.merlos.powerdetector.execution

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.merlos.powerdetector.data.AppDatabase
import com.merlos.powerdetector.domain.PowerTrigger

class PowerChangeWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val isCharging = PowerActionScheduler.isCharging(inputData)
        val trigger = if (isCharging) {
            PowerTrigger.ON_AC_POWER.name
        } else {
            PowerTrigger.ON_BATTERY.name
        }

        val database = AppDatabase.getInstance(applicationContext)
        val actions = database.powerActionDao().findEnabledForTrigger(trigger)
        val executor = ActionExecutor(applicationContext)
        val executedAt = System.currentTimeMillis()

        actions.forEach { action ->
            val result = executor.execute(action, isCharging)
            database.powerActionDao().updateExecution(
                actionId = action.id,
                result = result.message,
                executedAt = executedAt
            )
        }

        return Result.success()
    }
}
