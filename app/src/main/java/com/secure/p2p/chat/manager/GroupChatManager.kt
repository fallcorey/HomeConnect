package com.secure.p2p.chat.manager

import com.secure.p2p.chat.crypto.CryptoManager
import com.secure.p2p.chat.data.model.*
import com.secure.p2p.chat.network.MultiChannelManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*
import javax.crypto.SecretKey

class GroupChatManager(
    private val cryptoManager: CryptoManager,
    private val networkManager: MultiChannelManager
) {
    private val _activeGroups = MutableStateFlow<List<GroupChat>>(emptyList())
    val activeGroups: StateFlow<List<GroupChat>> = _activeGroups
    
    private val _groupEvents = MutableStateFlow<List<GroupChatEvent>>(emptyList())
    val groupEvents: StateFlow<List<GroupChatEvent>> = _groupEvents
    
    private val currentUser: ChatParticipant by lazy {
        createCurrentUser()
    }
    
    /**
     * Создает новую группу
     */
    fun createGroup(groupName: String, description: String? = null): GroupChat {
        val groupId = UUID.randomUUID().toString()
        val sessionKey = cryptoManager.generateSessionKey()
        val sessionKeyBase64 = cryptoManager.keyToBase64(sessionKey)
        
        val group = GroupChat(
            id = groupId,
            name = groupName,
            description = description,
            createdBy = currentUser.userId,
            participants = listOf(currentUser),
            sessionKey = sessionKeyBase64
        )
        
        // Добавляем группу в активные
        _activeGroups.value = _activeGroups.value + group
        
        // Создаем событие создания группы
        val creationEvent = GroupChatEvent(
            chatId = groupId,
            type = GroupEventType.GROUP_CREATED,
            userId = currentUser.userId,
            userName = currentUser.userName
        )
        _groupEvents.value = _groupEvents.value + creationEvent
        
        return group
    }
    
    /**
     * Создает приглашение в группу
     */
    fun createGroupInvite(groupId: String): GroupInvite? {
        val group = _activeGroups.value.find { it.id == groupId } ?: return null
        
        return GroupInvite(
            groupId = groupId,
            groupName = group.name,
            inviterId = currentUser.userId,
            inviterName = currentUser.userName,
            sessionKey = group.sessionKey,
            participantCount = group.participantCount,
            maxParticipants = group.maxParticipants
        )
    }
    
    /**
     * Принимает приглашение в группу
     */
    fun joinGroup(invite: GroupInvite): GroupChat? {
        if (invite.isExpired) {
            return null
        }
        
        val existingGroup = _activeGroups.value.find { it.id == invite.groupId }
        return if (existingGroup != null) {
            // Группа уже существует - присоединяемся
            addParticipantToGroup(existingGroup, currentUser)
        } else {
            // Создаем новую группу на основе приглашения
            createGroupFromInvite(invite)
        }
    }
    
    /**
     * Отправляет сообщение в группу
     */
    fun sendGroupMessage(groupId: String, content: String, messageType: MessageType = MessageType.TEXT) {
        val group = _activeGroups.value.find { it.id == groupId } ?: return
        
        // Шифруем сообщение групповым ключом
        val sessionKey = cryptoManager.keyFromBase64(group.sessionKey)
        val encryptedData = cryptoManager.encryptMessage(content, sessionKey)
        
        val message = Message(
            chatId = groupId,
            senderId = currentUser.userId,
            senderName = currentUser.userName,
            content = content,
            encryptedContent = cryptoManager.keyToBase64(
                cryptoManager.keyFromBase64(encryptedData.encrypted.toString())
            ),
            iv = cryptoManager.keyToBase64(encryptedData.iv),
            messageType = messageType
        )
        
        // Отправляем через все активные каналы
        group.participants.forEach { participant ->
            if (participant.userId != currentUser.userId) {
                networkManager.sendMessage(participant.deviceId, message.toNetworkFormat())
            }
        }
    }
    
    /**
     * Отправляет файл в группу
     */
    fun sendGroupFile(groupId: String, fileData: FileTransferData) {
        val group = _activeGroups.value.find { it.id == groupId } ?: return
        
        // Шифруем файл групповым ключом
        val sessionKey = cryptoManager.keyFromBase64(group.sessionKey)
        val fileContent = cryptoManager.keyToBase64(fileData.data)
        val encryptedData = cryptoManager.encryptMessage(fileContent, sessionKey)
        
        val message = Message(
            chatId = groupId,
            senderId = currentUser.userId,
            senderName = currentUser.userName,
            content = "File: ${fileData.fileName}",
            encryptedContent = cryptoManager.keyToBase64(
                cryptoManager.keyFromBase64(encryptedData.encrypted.toString())
            ),
            iv = cryptoManager.keyToBase64(encryptedData.iv),
            messageType = when (fileData.fileType) {
                FileType.IMAGE -> MessageType.IMAGE
                else -> MessageType.FILE
            },
            fileId = fileData.id,
            fileName = fileData.fileName,
            fileSize = fileData.size,
            fileType = fileData.fileType,
            fileData = fileContent // Уже зашифрованные данные
        )
        
        // Отправляем через все активные каналы
        group.participants.forEach { participant ->
            if (participant.userId != currentUser.userId) {
                networkManager.sendMessage(participant.deviceId, message.toNetworkFormat())
            }
        }
    }
    
    /**
     * Обновляет статус участника
     */
    fun updateParticipantStatus(groupId: String, userId: String, isOnline: Boolean) {
        val group = _activeGroups.value.find { it.id == groupId } ?: return
        val updatedGroup = group.updateParticipantStatus(userId, isOnline)
        
        _activeGroups.value = _activeGroups.value.map { 
            if (it.id == groupId) updatedGroup else it 
        }
    }
    
    /**
     * Вращает групповой ключ (для безопасности)
     */
    fun rotateGroupKey(groupId: String) {
        val group = _activeGroups.value.find { it.id == groupId } ?: return
        val newSessionKey = cryptoManager.generateSessionKey()
        val newSessionKeyBase64 = cryptoManager.keyToBase64(newSessionKey)
        
        val updatedGroup = group.copy(sessionKey = newSessionKeyBase64)
        
        _activeGroups.value = _activeGroups.value.map { 
            if (it.id == groupId) updatedGroup else it 
        }
        
        // Уведомляем участников о смене ключа
        // (в реальном приложении здесь будет рассылка нового ключа)
    }
    
    private fun createCurrentUser(): ChatParticipant {
        // Генерируем пользователя на основе устройства
        val userId = UUID.randomUUID().toString()
        val deviceId = android.provider.Settings.Secure.getString(
            android.content.Context.getContentResolver(),
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: UUID.randomUUID().toString()
        
        // Генерируем пару ключей для пользователя
        // (в реальном приложении здесь будет генерация RSA ключей)
        val publicKey = "public_key_${userId}" // Заглушка
        
        return ChatParticipant(
            userId = userId,
            userName = "User_${deviceId.takeLast(4)}",
            deviceId = deviceId,
            publicKey = publicKey
        )
    }
    
    private fun addParticipantToGroup(group: GroupChat, participant: ChatParticipant): GroupChat {
        val updatedGroup = group.addParticipant(participant)
        
        _activeGroups.value = _activeGroups.value.map { 
            if (it.id == group.id) updatedGroup else it 
        }
        
        // Создаем событие присоединения
        val joinEvent = GroupChatEvent(
            chatId = group.id,
            type = GroupEventType.USER_JOINED,
            userId = participant.userId,
            userName = participant.userName
        )
        _groupEvents.value = _groupEvents.value + joinEvent
        
        return updatedGroup
    }
    
    private fun createGroupFromInvite(invite: GroupInvite): GroupChat {
        val group = GroupChat(
            id = invite.groupId,
            name = invite.groupName,
            createdBy = invite.inviterId,
            participants = listOf(currentUser),
            sessionKey = invite.sessionKey,
            maxParticipants = invite.maxParticipants
        )
        
        _activeGroups.value = _activeGroups.value + group
        
        return group
    }
}
