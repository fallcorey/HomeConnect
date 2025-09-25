package com.secure.p2p.chat.utils

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    
    fun formatMessageTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }
    
    fun formatMessageDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }
    
    fun isToday(timestamp: Long): Boolean {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        return timestamp >= today
    }
}
