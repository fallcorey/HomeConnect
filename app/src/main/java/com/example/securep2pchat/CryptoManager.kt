package com.example.securep2pchat

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoManager {
    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH = 128
        private const val AES_KEY_SIZE = 256
    }
    
    fun generateAESKey(): String {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(AES_KEY_SIZE, SecureRandom())
        val secretKey = keyGenerator.generateKey()
        return Base64.encodeToString(secretKey.encoded, Base64.DEFAULT)
    }
    
    fun encrypt(data: String, key: String): String {
        try {
            val secretKey = createKeyFromString(key)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            
            // Combine IV + encrypted data
            val combined = iv + encrypted
            
            return Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            throw RuntimeException("Encryption failed", e)
        }
    }
    
    fun decrypt(encryptedData: String, key: String): String {
        try {
            val secretKey = createKeyFromString(key)
            val combined = Base64.decode(encryptedData, Base64.DEFAULT)
            
            // Extract IV (first 12 bytes) and encrypted data
            val iv = combined.copyOfRange(0, IV_LENGTH)
            val encrypted = combined.copyOfRange(IV_LENGTH, combined.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            val decrypted = cipher.doFinal(encrypted)
            return String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            throw RuntimeException("Decryption failed: ${e.message}")
        }
    }
    
    private fun createKeyFromString(key: String): SecretKey {
        val keyBytes = Base64.decode(key, Base64.DEFAULT)
        return SecretKeySpec(keyBytes, "AES")
    }
}
