package com.example.securep2pchat

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.*
import java.net.ServerSocket
import java.net.Socket

@Serializable
data class SecureMessage(
    val type: String,
    val data: String,
    val timestamp: Long = System.currentTimeMillis()
)

class SecureChatService {
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var keyExchangeManager = KeyExchangeManager()
    private var cryptoManager = CryptoManager()
    private var currentSessionKey: String? = null
    private var isRunning = true
    
    suspend fun startServer(port: Int, onMessage: (String) -> Unit) {
        try {
            serverSocket = ServerSocket(port)
            
            CoroutineScope(Dispatchers.IO).launch {
                while (isRunning) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        handleClientConnection(socket, onMessage)
                    } catch (e: Exception) {
                        if (isRunning) {
                            withContext(Dispatchers.Main) {
                                onMessage("Server error: ${e.message}")
                            }
                        }
                        break
                    }
                }
            }
            
            withContext(Dispatchers.Main) {
                onMessage("Server started on port $port")
            }
        } catch (e: Exception) {
            throw IOException("Server error: ${e.message}")
        }
    }
    
    suspend fun connectToServer(host: String, port: Int, onMessage: (String) -> Unit) {
        try {
            clientSocket = Socket(host, port)
            startMessageReceiver(onMessage)
            performKeyExchange()
            
            withContext(Dispatchers.Main) {
                onMessage("Connected to server $host:$port")
            }
        } catch (e: Exception) {
            throw IOException("Connection failed: ${e.message}")
        }
    }
    
    private suspend fun handleClientConnection(socket: Socket, onMessage: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                performKeyExchangeAsServer(socket)
                startMessageReceiver(socket, onMessage)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onMessage("Client connection error: ${e.message}")
                }
            }
        }
    }
    
    private suspend fun performKeyExchangeAsServer(socket: Socket) {
        val input = BufferedReader(InputStreamReader(socket.getInputStream()))
        val output = PrintWriter(socket.getOutputStream(), true)
        
        // Generate session key for this chat
        currentSessionKey = keyExchangeManager.generateSessionKey()
        
        // Send our public key and encrypted session key
        val keyData = KeyExchangeManager.KeyExchangeData(
            publicKey = keyExchangeManager.getPublicKey(),
            encryptedSessionKey = keyExchangeManager.encryptSessionKey()
        )
        
        val message = SecureMessage("key_exchange", Json.encodeToString(KeyExchangeManager.KeyExchangeData.serializer(), keyData))
        output.println(Json.encodeToString(SecureMessage.serializer(), message))
        
        // Wait for client's public key
        val response = input.readLine()
        val responseMessage = Json.decodeFromString<SecureMessage>(response)
        
        if (responseMessage.type == "key_exchange") {
            val clientKeyData = Json.decodeFromString<KeyExchangeManager.KeyExchangeData>(responseMessage.data)
            keyExchangeManager.setPeerPublicKey(clientKeyData.publicKey)
        }
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
    
    private fun startMessageReceiver(socket: Socket, onMessage: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val input = BufferedReader(InputStreamReader(socket.getInputStream()))
            
            while (isRunning) {
                try {
                    val message = input.readLine() ?: break
                    val secureMessage = Json.decodeFromString<SecureMessage>(message)
                    
                    when (secureMessage.type) {
                        "message" -> {
                            val decrypted = cryptoManager.decrypt(secureMessage.data, currentSessionKey!!)
                            withContext(Dispatchers.Main) {
                                onMessage("Peer: $decrypted")
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (isRunning) {
                        withContext(Dispatchers.Main) {
                            onMessage("Error receiving message: ${e.message}")
                        }
                    }
                    break
                }
            }
        }
    }
    
    private fun startMessageReceiver(onMessage: (String) -> Unit) {
        val socket = clientSocket ?: return
        startMessageReceiver(socket, onMessage)
    }
    
    suspend fun sendMessage(message: String) {
        val socket = clientSocket ?: throw IllegalStateException("Not connected")
        val output = PrintWriter(socket.getOutputStream(), true)
        
        val encrypted = cryptoManager.encrypt(message, currentSessionKey!!)
        val secureMessage = SecureMessage("message", encrypted)
        
        withContext(Dispatchers.IO) {
            output.println(Json.encodeToString(SecureMessage.serializer(), secureMessage))
        }
    }
    
    fun stop() {
        isRunning = false
        serverSocket?.close()
        clientSocket?.close()
    }
    
    fun isConnected(): Boolean {
        return clientSocket?.isConnected == true || serverSocket != null
    }
}
