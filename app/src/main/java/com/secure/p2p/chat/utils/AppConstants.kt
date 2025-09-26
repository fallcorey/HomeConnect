package com.secure.p2p.chat.utils

object AppConstants {
    const val APP_TAG = "SecureP2PChat"
    const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024 // 10MB
    const val MAX_GROUP_PARTICIPANTS = 8
    const val MESSAGE_EPHEMERAL_TIMEOUT_MS = 24 * 60 * 60 * 1000L // 24 hours
    
    // WebRTC configuration
    const val STUN_SERVER_URL = "stun:stun.l.google.com:19302"
    const val DATA_CHANNEL_LABEL = "secure-chat"
    
    // Network timeouts
    const val CONNECTION_TIMEOUT_MS = 10000L
    const val MESSAGE_TIMEOUT_MS = 5000L
}
