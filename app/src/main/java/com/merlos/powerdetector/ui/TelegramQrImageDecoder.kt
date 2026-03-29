package com.merlos.powerdetector.ui

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

object TelegramQrImageDecoder {
    fun decode(contentResolver: ContentResolver, uri: Uri): TelegramQrPayload? {
        val bitmap = contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: return null

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        bitmap.recycle()

        val binaryBitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(width, height, pixels)))
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(com.google.zxing.BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to true
        )

        val rawText = runCatching {
            QRCodeReader().decode(binaryBitmap, hints).text
        }.recoverCatching {
            MultiFormatReader().decode(binaryBitmap, hints).text
        }.getOrNull() ?: return null

        return TelegramQrParser.parse(rawText)
    }
}
