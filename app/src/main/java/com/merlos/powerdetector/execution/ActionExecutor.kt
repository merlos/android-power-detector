package com.merlos.powerdetector.execution

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
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
            return ExecutionResult(false, "SMS permission missing")
        }

        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
        } else {
            SmsManager.getDefault()
        }

        return try {
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            ExecutionResult(true, "Executed successfully")
        } catch (error: Exception) {
            ExecutionResult(false, error.message ?: "Unable to send SMS")
        }
    }

    private fun sendTelegram(botToken: String?, chatId: String, message: String): ExecutionResult {
        if (botToken.isNullOrBlank()) {
            return ExecutionResult(false, "Telegram bot token is missing")
        }

        return try {
            telegramApiClient.sendMessage(botToken, chatId, message)
            ExecutionResult(true, "Executed successfully")
        } catch (error: Exception) {
            ExecutionResult(false, error.message ?: "Unable to send Telegram message")
        }
    }
}
