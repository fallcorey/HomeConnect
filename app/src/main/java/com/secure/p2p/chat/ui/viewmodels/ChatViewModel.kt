package com.secure.p2p.chat.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secure.p2p.chat.crypto.CryptoManager
import com.secure.p2p.chat.data.model.Message
import com.secure.p2p.chat.network.WebRTCManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

class ChatViewModel : ViewModel() {
    private val cryptoManager = CryptoManager()
    private val webRTCManager = WebRTCManager()
    
    // Состояние UI
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState
    
    // Сообщения текущего чата
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages
    
    // Генерация ID для чата и пользователя
    private val currentUserId = UUID.randomUUID().toString()
    private var currentChatId: String? = null
    
    fun createNewChat(userName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Генерируем ID чата
                val chatId = UUID.randomUUID().toString()
                currentChatId = chatId
                
                // Генерируем сессионный ключ
                val sessionKey = cryptoManager.generateSessionKey()
                val sessionKeyBase64 = cryptoManager.keyToBase64(sessionKey)
                
                // Создаем данные для QR-кода
                val qrData = ChatInviteData(
                    chatId = chatId,
                    sessionKey = sessionKeyBase64,
                    creatorName = userName
                )
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentChatId = chatId,
                    qrCodeData = qrData.toJson(),
                    connectionState = WebRTCManager.ConnectionState.CONNECTING
                )
                
                // Начинаем слушать входящие соединения
                startListeningForConnections()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to create chat: ${e.message}"
                )
            }
        }
    }
    
    fun joinChat(qrData: String, userName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val inviteData = ChatInviteData.fromJson(qrData)
                currentChatId = inviteData.chatId
                
                // Восстанавливаем сессионный ключ из QR-кода
                val sessionKey = cryptoManager.keyFromBase64(inviteData.sessionKey)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentChatId = inviteData.chatId,
                    connectionState = WebRTCManager.ConnectionState.CONNECTING
                )
                
                // Подключаемся к существующему чату
                connectToExistingChat(inviteData)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to join chat: ${e.message}"
                )
            }
        }
    }
    
    fun sendMessage(content: String) {
        viewModelScope.launch {
            try {
                val chatId = currentChatId ?: return@launch
                
                // Шифруем сообщение
                val sessionKey = cryptoManager.keyFromBase64(
                    _uiState.value.qrCodeData?.let { 
                        ChatInviteData.fromJson(it).sessionKey 
                    } ?: return@launch
                )
                
                val encryptedData = cryptoManager.encryptMessage(content, sessionKey)
                
                val message = Message(
                    chatId = chatId,
                    senderId = currentUserId,
                    senderName = "You", // В реальном приложении будет имя пользователя
                    content = content,
                    encryptedContent = cryptoManager.keyToBase64(
                        cryptoManager.keyFromBase64(encryptedData.encrypted.toString())
                    ),
                    iv = cryptoManager.keyToBase64(encryptedData.iv),
                    isEncrypted = true
                )
                
                // Добавляем сообщение в локальный список
                _messages.value = _messages.value + message
                
                // Отправляем через P2P
                webRTCManager.sendMessageToAllPeers(message.toNetworkFormat())
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to send message: ${e.message}"
                )
            }
        }
    }
    
    private fun startListeningForConnections() {
        // Запускаем WebRTC сервер для принятия входящих соединений
        // В реальной реализации здесь будет настройка STUN/TURN серверов
    }
    
    private fun connectToExistingChat(inviteData: ChatInviteData) {
        // Подключаемся к существующему чату через WebRTC
        // Создаем peer connection и подключаемся к создателю чата
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class ChatUiState(
    val isLoading: Boolean = false,
    val currentChatId: String? = null,
    val qrCodeData: String? = null,
    val connectionState: WebRTCManager.ConnectionState = WebRTCManager.ConnectionState.DISCONNECTED,
    val error: String? = null
)

data class ChatInviteData(
    val chatId: String,
    val sessionKey: String, // Base64 encoded AES key
    val creatorName: String
) {
    fun toJson(): String {
        return """{"chatId":"$chatId","sessionKey":"$sessionKey","creatorName":"$creatorName"}"""
    }
    
    companion object {
        fun fromJson(json: String): ChatInviteData {
            // Простой парсинг JSON (в реальном приложении используйте библиотеку)
            val chatId = json.substringAfter("\"chatId\":\"").substringBefore("\"")
            val sessionKey = json.substringAfter("\"sessionKey\":\"").substringBefore("\"")
            val creatorName = json.substringAfter("\"creatorName\":\"").substringBefore("\"")
            return ChatInviteData(chatId, sessionKey, creatorName)
        }
    }
}
