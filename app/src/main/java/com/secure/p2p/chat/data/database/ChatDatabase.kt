package com.secure.p2p.chat.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.secure.p2p.chat.data.dao.MessageDao
import com.secure.p2p.chat.data.model.Message

@Database(
    entities = [Message::class],
    version = 1,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        fun getInstance(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "secure_chat_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
