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
    val isRead: Boolean = false,
    
    // Поля для файлов
    val fileId: String? = null,
    val fileName: String? = null,
    val fileSize: Long = 0,
    val fileType: FileType? = null,
    val fileData: String? = null, // Base64 encoded file data or chunk reference
    val chunkIndex: Int = -1,
    val totalChunks: Int = 1
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
            messageType = messageType,
            fileId = fileId,
            fileName = fileName,
            fileSize = fileSize,
            fileType = fileType,
            fileData = fileData,
            chunkIndex = chunkIndex,
            totalChunks = totalChunks
        )
    }
    
    val isFileMessage: Boolean
        get() = messageType == MessageType.IMAGE || messageType == MessageType.FILE
    
    val isChunkedFile: Boolean
        get() = chunkIndex != -1 && totalChunks > 1
}

enum class MessageType {
    TEXT, IMAGE, FILE, SYSTEM, LOCATION
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
    val messageType: MessageType,
    val fileId: String? = null,
    val fileName: String? = null,
    val fileSize: Long = 0,
    val fileType: FileType? = null,
    val fileData: String? = null,
    val chunkIndex: Int = -1,
    val totalChunks: Int = 1
)
