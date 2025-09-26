package com.example.securep2pchat

import android.util.Base64
import java.security.KeyPairGenerator
import java.security.PublicKey
import javax.crypto.Cipher

class KeyExchangeManager {
    private var keyPair = generateKeyPair()
    private var sessionKey: String? = null
    private var peerPublicKey: PublicKey? = null
    
    data class KeyExchangeData(
        val publicKey: String,
        val encryptedSessionKey: String? = null
    )
    
    fun getPublicKey(): String {
        return Base64.encodeToString(keyPair.public.encoded, Base64.DEFAULT)
    }
    
    fun setPeerPublicKey(publicKeyBase64: String) {
        try {
            val keyBytes = Base64.decode(publicKeyBase64, Base64.DEFAULT)
            val keyFactory = java.security.KeyFactory.getInstance("RSA")
            val keySpec = java.security.spec.X509EncodedKeySpec(keyBytes)
            peerPublicKey = keyFactory.generatePublic(keySpec)
        } catch (e: Exception) {
            throw RuntimeException("Failed to set peer public key: ${e.message}")
        }
    }
    
    fun generateSessionKey(): String {
        sessionKey = CryptoManager().generateAESKey()
        return sessionKey!!
    }
    
    fun encryptSessionKey(): String {
        if (peerPublicKey == null || sessionKey == null) {
            throw IllegalStateException("Peer public key or session key not set")
        }
        
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, peerPublicKey)
        val encrypted = cipher.doFinal(sessionKey!!.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }
    
    fun decryptSessionKey(encryptedSessionKey: String): String {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, keyPair.private)
        val decrypted = cipher.doFinal(Base64.decode(encryptedSessionKey, Base64.DEFAULT))
        sessionKey = String(decrypted, Charsets.UTF_8)
        return sessionKey!!
    }
    
    fun getSessionKey(): String {
        return sessionKey ?: throw IllegalStateException("Session key not established")
    }
    
    private fun generateKeyPair(): java.security.KeyPair {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        return keyGen.generateKeyPair()
    }
}
