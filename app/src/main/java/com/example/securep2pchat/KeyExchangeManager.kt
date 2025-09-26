package com.example.securep2pchat

import android.util.Base64
import java.security.KeyPairGenerator
import java.security.PrivateKey
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
        val keyBytes = Base64.decode(publicKeyBase64, Base64.DEFAULT)
        val keyFactory = java.security.KeyFactory.getInstance("RSA")
        val keySpec = java.security.spec.X509EncodedKeySpec(keyBytes)
        peerPublicKey = keyFactory.generatePublic(keySpec)
    }
    
    fun generateSessionKey(): String {
        sessionKey = CryptoManager().generateSessionKey()
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
    
    fun performKeyExchange(peerKeyData: KeyExchangeData): KeyExchangeData {
        setPeerPublicKey(peerKeyData.publicKey)
        
        return if (peerKeyData.encryptedSessionKey != null) {
            // We are client, decrypt their session key
            decryptSessionKey(peerKeyData.encryptedSessionKey)
            KeyExchangeData(getPublicKey())
        } else {
            // We are server, generate and encrypt session key
            generateSessionKey()
            KeyExchangeData(getPublicKey(), encryptSessionKey())
        }
    }
    
    private fun generateKeyPair(): java.security.KeyPair {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        return keyGen.generateKeyPair()
    }
}
