package com.example.securep2pchat

import kotlinx.cor.*
import kotlinx.coroutines.channels.Channel
import java.io.*
import java.net.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SecureMessage(
    val type: String, // "key_exchange", "message", "handshake"
    val data: String,
    val signature: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class SecureChatService(private val cryptoManager: CryptoManager) {
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var keyExchangeManager = KeyExchangeManager()
    
    private val messageChannel = Channel<String>()
    private var currentSessionKey: String? = null
    
    suspend fun startServer(port: Int, onMessage: (String) -> Unit) {
        try {
            serverSocket = ServerSocket(port)
            
            // Generate session key for this chat room
            currentSessionKey = keyExchangeManager.generateSessionKey()
            
            while (true) {
                val socket = serverSocket?.accept() ?: break
                handleClientConnection(socket, onMessage)
            }
        } catch (e: Exception) {
            throw IOException("Server error: ${e.message}")
        }
    }
    
    suspend fun connectToServer(host: String, port: Int, onMessage: (String) -> Unit) {
        try {
            clientSocket = Socket(host, port)
            startMessageReceiver(onMessage)
            
            // Initiate key exchange
            performKeyExchange()
        } catch (e: Exception) {
            throw IOException("Connection failed: ${e.message}")
        }
    }
    
    private suspend fun handleClientConnection(socket: Socket, onMessage: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Perform key exchange first
                performKeyExchangeAsServer(socket)
                
                // Start receiving messages
                startMessageReceiver(socket, onMessage)
            } catch (e: Exception) {
                onMessage("Connection error: ${e.message}")
            }
        }
    }
    
    private suspend fun performKeyExchangeAsServer(socket: Socket) {
        val input = BufferedReader(InputStreamReader(socket.getInputStream()))
        val output = PrintWriter(socket.getOutputStream(), true)
        
        // Send our public key and encrypted session key
        val keyData = KeyExchangeManager.KeyExchangeData(
            publicKey = keyExchangeManager.getPublicKey(),
            encryptedSessionKey = keyExchangeManager.encryptSessionKey()
        )
        
        val message = SecureMessage("key_exchange", Json.encodeToString(KeyExchangeManager.KeyExchangeData.serializer(), keyData))
        output.println(Json.encodeToString(SecureMessage.serializer(), message))
        
        // Wait for client's public key (acknowledgement)
        val response = input.readLine()
        val responseMessage = Json.decodeFromString<SecureMessage>(response)
        
        if (responseMessage.type == "key_exchange") {
            val clientKeyData = Json.decodeFromString<KeyExchangeManager.KeyExchangeData>(responseMessage.data)
            keyExchangeManager.setPeerPublicKey(clientKeyData.publicKey)
        }
        
        currentSessionKey = keyExchangeManager.getSessionKey()
    }
    
    private suspend fun performKeyExchange() {
        val socket = clientSocket ?: return
        val input = BufferedReader(InputStreamReader(socket.getInputStream()))
        val output = PrintWriter(socket.getOutputStream(), true)
        
        // Receive server's key data
        val serverMessage = input.readLine()
        val keyMessage = Json.decodeFromString<SecureMessage>(serverMessage)
        
        if (keyMessage.type == "key_exchange") {
            val serverKeyData = Json.decodeFromString<KeyExchangeManager.KeyExchangeData>(keyMessage.data)
            
            // Set server's public key and decrypt session key
            keyExchangeManager.setPeerPublicKey(serverKeyData.publicKey)
            currentSessionKey = keyExchangeManager.decryptSessionKey(serverKeyData.encryptedSessionKey!!)
            
            // Send our public key back
            val clientKeyData = KeyExchangeManager.KeyExchangeData(
                publicKey = keyExchangeManager.getPublicKey()
            )
            
            val response = SecureMessage("key_exchange", Json.encodeToString(KeyExchangeManager.KeyExchangeData.serializer(), clientKeyData))
            output.println(Json.encodeToString(SecureMessage.serializer(), response))
        }
    }
    
    private suspend fun startMessageReceiver(socket: Socket, onMessage: (String) -> Unit) {
        val input = BufferedReader(InputStreamReader(socket.getInputStream()))
        
        while (true) {
            try {
                val message = input.readLine() ?: break
                val secureMessage = Json.decodeFromString<SecureMessage>(message)
                
                when (secureMessage.type) {
                    "message" -> {
                        val decrypted = cryptoManager.decryptWithKey(secureMessage.data, currentSessionKey!!)
                        onMessage(decrypted)
                    }
                    else -> {
                        // Handle other message types
                    }
                }
            } catch (e: Exception) {
                onMessage("Error receiving message: ${e.message}")
                break
            }
        }
    }
    
    private suspend fun startMessageReceiver(onMessage: (String) -> Unit) {
        val socket = clientSocket ?: return
        startMessageReceiver(socket, onMessage)
    }
    
    suspend fun sendMessage(message: String) {
        val socket = clientSocket ?: throw IllegalStateException("Not connected")
        val output = PrintWriter(socket.getOutputStream(), true)
        
        val encrypted = cryptoManager.encryptWithKey(message, currentSessionKey!!)
        val secureMessage = SecureMessage("message", encrypted)
        
        output.println(Json.encodeToString(SecureMessage.serializer(), secureMessage))
    }
    
    fun stop() {
        serverSocket?.close()
        clientSocket?.close()
        messageChannel.close()
    }
    
    fun isConnected(): Boolean {
        return clientSocket?.isConnected == true || serverSocket != null
    }
}
