package com.secure.p2p.chat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "group_chats")
data class GroupChat(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val participants: List<ChatParticipant>,
    val maxParticipants: Int = 8,
    val isEphemeral: Boolean = true,
    val sessionKey: String, // Base64 encoded group session key
    val lastActivity: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    val participantCount: Int
        get() = participants.size
    
    val canAddMoreParticipants: Boolean
        get() = participantCount < maxParticipants
    
    fun addParticipant(participant: ChatParticipant): GroupChat {
        if (participants.any { it.userId == participant.userId }) {
            return this // Участник уже есть
        }
        
        if (!canAddMoreParticipants) {
            throw IllegalStateException("Group is full")
        }
        
        return copy(
            participants = participants + participant,
            lastActivity = System.currentTimeMillis()
        )
    }
    
    fun removeParticipant(userId: String): GroupChat {
        return copy(
            participants = participants.filter { it.userId != userId },
            lastActivity = System.currentTimeMillis()
        )
    }
    
    fun updateParticipantStatus(userId: String, isOnline: Boolean): GroupChat {
        return copy(
            participants = participants.map { 
                if (it.userId == userId) it.copy(isOnline = isOnline) else it 
            },
            lastActivity = System.currentTimeMillis()
        )
    }
}

data class ChatParticipant(
    val userId: String,
    val userName: String,
    val deviceId: String,
    val publicKey: String, // Base64 encoded RSA public key
    val joinedAt: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
) {
    fun toUser(): User {
        return User(
            id = userId,
            nickname = userName,
            publicKey = publicKey,
            deviceId = deviceId
        )
    }
}

// События группового чата
data class GroupChatEvent(
    val id: String = UUID.randomUUID().toString(),
    val chatId: String,
    val type: GroupEventType,
    val userId: String,
    val userName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val data: String? = null // Дополнительные данные события
) {
    fun toMessage(): Message {
        return Message(
            chatId = chatId,
            senderId = userId,
            senderName = userName,
            content = when (type) {
                GroupEventType.USER_JOINED -> "$userName joined the group"
                GroupEventType.USER_LEFT -> "$userName left the group"
                GroupEventType.GROUP_CREATED -> "Group created by $userName"
            },
            encryptedContent = "",
            iv = "",
            messageType = MessageType.SYSTEM,
            isEncrypted = false
        )
    }
}

enum class GroupEventType {
    GROUP_CREATED, USER_JOINED, USER_LEFT, USER_KICKED, SESSION_KEY_ROTATED
}

// Приглашение в группу
data class GroupInvite(
    val inviteId: String = UUID.randomUUID().toString(),
    val groupId: String,
    val groupName: String,
    val inviterId: String,
    val inviterName: String,
    val sessionKey: String, // Base64 encoded group session key
    val participantCount: Int,
    val maxParticipants: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24 hours
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt
    
    fun toQrCodeData(): String {
        return "GROUP_INVITE|$groupId|$inviterId|$sessionKey|$maxParticipants|$expiresAt"
    }
    
    companion object {
        fun fromQrCodeData(qrData: String): GroupInvite? {
            return try {
                val parts = qrData.split("|")
                if (parts.size >= 6 && parts[0] == "GROUP_INVITE") {
                    GroupInvite(
                        groupId = parts[1],
                        groupName = "Group Chat", // Можно передавать в следующих версиях
                        inviterId = parts[2],
                        inviterName = "User", // Можно передавать в следующих версиях
                        sessionKey = parts[3],
                        participantCount = 1, // Будет обновлено при подключении
                        maxParticipants = parts[4].toInt(),
                        expiresAt = parts[5].toLong()
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
