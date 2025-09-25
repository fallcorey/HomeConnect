package com.secure.p2p.chat.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import java.util.EnumMap

object QrCodeGenerator {
    
    /**
     * Генерирует QR-код из текстовых данных
     */
    @Throws(WriterException::class)
    fun generateQrCode(data: String, width: Int, height: Int): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        hints[EncodeHintType.MARGIN] = 1
        
        val bitMatrix = MultiFormatWriter().encode(
            data,
            BarcodeFormat.QR_CODE,
            width,
            height,
            hints
        )
        
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
    
    /**
     * Генерирует безопасные данные для QR-кода приглашения в чат
     */
    fun generateChatInviteQrData(chatId: String, sessionKey: String, creatorName: String): String {
        return "SECURE_CHAT_INVITE|$chatId|$sessionKey|$creatorName|${System.currentTimeMillis()}"
    }
    
    /**
     * Парсит данные из QR-кода приглашения
     */
    fun parseChatInviteQrData(qrData: String): ChatInviteQrData? {
        return try {
            val parts = qrData.split("|")
            if (parts.size >= 5 && parts[0] == "SECURE_CHAT_INVITE") {
                ChatInviteQrData(
                    chatId = parts[1],
                    sessionKey = parts[2],
                    creatorName = parts[3],
                    timestamp = parts[4].toLong()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

data class ChatInviteQrData(
    val chatId: String,
    val sessionKey: String,
    val creatorName: String,
    val timestamp: Long
) {
    fun isValid(): Boolean {
        // Проверяем что приглашение не старше 10 минут
        return System.currentTimeMillis() - timestamp < 10 * 60 * 1000
    }
}
