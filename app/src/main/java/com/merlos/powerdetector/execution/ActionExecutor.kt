package com.merlos.powerdetector.execution

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.merlos.powerdetector.data.PowerActionEntity
import com.merlos.powerdetector.domain.ActionType

class ActionExecutor(
    private val context: Context,
    private val telegramApiClient: TelegramApiClient = TelegramApiClient(),
    private val messageRenderer: ActionMessageRenderer = ActionMessageRenderer()
) {
    data class ExecutionResult(val success: Boolean, val message: String)

    fun execute(action: PowerActionEntity, isCharging: Boolean): ExecutionResult {
        val renderedMessage = messageRenderer.render(action.message, isCharging)
        Log.d(TAG, "Executing action id=${action.id} type=${action.actionType} recipient=${action.recipient} renderedMessage=\"$renderedMessage\"")
        return when (ActionType.valueOf(action.actionType)) {
            ActionType.SMS -> sendSms(action.recipient, renderedMessage)
            ActionType.TELEGRAM -> sendTelegram(action.botToken, action.recipient, renderedMessage)
        }
    }

    private fun sendSms(phoneNumber: String, message: String): ExecutionResult {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            Log.w(TAG, "SMS permission not granted - cannot send to $phoneNumber")
            return ExecutionResult(false, "SMS permission missing")
        }
        Log.d(TAG, "Sending SMS to $phoneNumber")

        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
        } else {
            SmsManager.getDefault()
        }

        return try {
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d(TAG, "SMS sent successfully to $phoneNumber")
            ExecutionResult(true, "Executed successfully")
        } catch (error: Exception) {
            Log.e(TAG, "SMS send failed to $phoneNumber: ${error.message}", error)
            ExecutionResult(false, error.message ?: "Unable to send SMS")
        }
    }

    private fun sendTelegram(botToken: String?, chatId: String, message: String): ExecutionResult {
        if (botToken.isNullOrBlank()) {
            Log.w(TAG, "Telegram bot token is blank - cannot send to chatId=$chatId")
            return ExecutionResult(false, "Telegram bot token is missing")
        }
        Log.d(TAG, "Sending Telegram message to chatId=$chatId")

        return try {
            telegramApiClient.sendMessage(botToken, chatId, message)
            Log.d(TAG, "Telegram message sent successfully to chatId=$chatId")
            ExecutionResult(true, "Executed successfully")
        } catch (error: Exception) {
            Log.e(TAG, "Telegram send failed to chatId=$chatId: ${error.message}", error)
            ExecutionResult(false, error.message ?: "Unable to send Telegram message")
        }
    }

    companion object {
        private const val TAG = "ActionExecutor"
    }
}
