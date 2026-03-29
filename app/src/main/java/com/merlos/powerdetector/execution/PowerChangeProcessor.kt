package com.merlos.powerdetector.execution

import android.content.Context
import android.util.Log
import com.merlos.powerdetector.data.AppDatabase
import com.merlos.powerdetector.domain.PowerTrigger

object PowerChangeProcessor {
    private const val TAG = "PowerChangeProcessor"

    suspend fun process(context: Context, isCharging: Boolean) {
        val trigger = if (isCharging) {
            PowerTrigger.ON_AC_POWER.name
        } else {
            PowerTrigger.ON_BATTERY.name
        }
        Log.d(TAG, "Processing power change: isCharging=$isCharging trigger=$trigger")

        val database = AppDatabase.getInstance(context)
        val actions = database.powerActionDao().findEnabledForTriggers(
            listOf(PowerTrigger.BOTH.name, trigger)
        )
        Log.d(TAG, "Found ${actions.size} matching enabled action(s)")

        val executor = ActionExecutor(context)
        val executedAt = System.currentTimeMillis()

        actions.forEach { action ->
            Log.d(TAG, "Executing action id=${action.id} type=${action.actionType}")
            val result = executor.execute(action, isCharging)
            Log.d(TAG, "Action id=${action.id} result: success=${result.success} msg=${result.message}")
            database.powerActionDao().updateExecution(
                actionId = action.id,
                result = result.message,
                executedAt = executedAt
            )
        }
    }
}