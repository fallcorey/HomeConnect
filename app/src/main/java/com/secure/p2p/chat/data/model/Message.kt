package com.secure.p2p.chat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val encryptedContent: String, // Base64 encoded encrypted data
    val iv: String, // Base64 encoded IV
    val timestamp: Long = System.currentTimeMillis(),
    val messageType: MessageType = MessageType.TEXT,
    val isEncrypted: Boolean = true,
    val isDelivered: Boolean = false,
    val isRead: Boolean = false
) {
    fun toNetworkFormat(): NetworkMessage {
        return NetworkMessage(
            id = id,
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            encryptedContent = encryptedContent,
            iv = iv,
            timestamp = timestamp,
            messageType = messageType
        )
    }
}

enum class MessageType {
    TEXT, IMAGE, SYSTEM
}

// Для передачи по сети
data class NetworkMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val encryptedContent: String,
    val iv: String,
    val timestamp: Long,
    val messageType: MessageType
)
