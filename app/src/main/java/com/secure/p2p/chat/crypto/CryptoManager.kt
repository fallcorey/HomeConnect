package com.secure.p2p.chat.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoManager {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    // Генерация AES ключа для сессии
    fun generateSessionKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }

    // Шифрование сообщения
    fun encryptMessage(message: String, secretKey: SecretKey): EncryptedData {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val iv = cipher.iv
        val encrypted = cipher.doFinal(message.toByteArray(Charsets.UTF_8))
        
        return EncryptedData(
            encrypted = encrypted,
            iv = iv
        )
    }

    // Дешифрование сообщения
    fun decryptMessage(encryptedData: EncryptedData, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, encryptedData.iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        
        return String(cipher.doFinal(encryptedData.encrypted), Charsets.UTF_8)
    }

    // Конвертация ключа в Base64 для передачи
    fun keyToBase64(key: SecretKey): String {
        return Base64.encodeToString(key.encoded, Base64.NO_WRAP)
    }

    // Восстановление ключа из Base64
    fun keyFromBase64(base64Key: String): SecretKey {
        val keyBytes = Base64.decode(base64Key, Base64.NO_WRAP)
        return SecretKeySpec(keyBytes, "AES")
    }
}

data class EncryptedData(
    val encrypted: ByteArray,
    val iv: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedData
        return encrypted.contentEquals(other.encrypted) && iv.contentEquals(other.iv)
    }

    override fun hashCode(): Int {
        var result = encrypted.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        return result
    }
}
