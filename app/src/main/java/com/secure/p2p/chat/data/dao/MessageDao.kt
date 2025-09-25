package com.secure.p2p.chat.data.dao

import androidx.room.*
import com.secure.p2p.chat.data.model.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesByChat(chatId: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteChatMessages(chatId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE chatId = :chatId AND isRead = 0")
    suspend fun getUnreadCount(chatId: String): Int

    @Update
    suspend fun updateMessage(message: Message)
}
