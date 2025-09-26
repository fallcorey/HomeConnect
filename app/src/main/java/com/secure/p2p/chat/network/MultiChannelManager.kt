package com.secure.p2p.chat.network

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MultiChannelManager(private val context: Context) {
    private val webRTCManager = AdvancedWebRTCManager(context)
    private val wifiDirectManager = WifiDirectManager(context)
    private val bluetoothManager = BluetoothManager(context)
    
    private val _activeChannel = MutableStateFlow(NetworkChannel.WEBRTC)
    val activeChannel: StateFlow<NetworkChannel> = _activeChannel
    
    private val _connectionQuality = MutableStateFlow(ConnectionQuality.UNKNOWN)
    val connectionQuality: StateFlow<ConnectionQuality> = _connectionQuality
    
    // Приоритет каналов (от высшего к низшему)
    private val channelPriority = listOf(
        NetworkChannel.WEBRTC,
        NetworkChannel.WIFI_DIRECT, 
        NetworkChannel.BLUETOOTH
    )
    
    init {
        initializeAllChannels()
        startChannelMonitoring()
    }
    
    /**
     * Инициализирует все сетевые каналы
     */
    private fun initializeAllChannels() {
        webRTCManager // Уже инициализирован в конструкторе
        wifiDirectManager.initialize()
        bluetoothManager.initialize()
    }
    
    /**
     * Запускает мониторинг качества каналов
     */
    private fun startChannelMonitoring() {
        // В реальном приложении здесь будет периодическая проверка
        // скорости, задержки и стабильности каждого канала
    }
    
    /**
     * Автоматически выбирает лучший доступный канал
     */
    suspend fun selectBestChannel(): NetworkChannel {
        for (channel in channelPriority) {
            if (isChannelAvailable(channel)) {
                _activeChannel.value = channel
                return channel
            }
        }
        
        _activeChannel.value = NetworkChannel.DISCONNECTED
        return NetworkChannel.DISCONNECTED
    }
    
    /**
     * Проверяет доступность канала
     */
    fun isChannelAvailable(channel: NetworkChannel): Boolean {
        return when (channel) {
            NetworkChannel.WEBRTC -> checkWebRTCAvailability()
            NetworkChannel.WIFI_DIRECT -> checkWifiDirectAvailability()
            NetworkChannel.BLUETOOTH -> checkBluetoothAvailability()
            NetworkChannel.DISCONNECTED -> true
        }
    }
    
    /**
     * Отправляет сообщение через лучший доступный канал
     */
    fun sendMessage(peerId: String, message: NetworkMessage) {
        when (_activeChannel.value) {
            NetworkChannel.WEBRTC -> webRTCManager.sendMessage(peerId, message)
            NetworkChannel.WIFI_DIRECT -> {
                val messageJson = convertMessageToJson(message)
                wifiDirectManager.sendMessage(messageJson)
            }
            NetworkChannel.BLUETOOTH -> {
                val messageJson = convertMessageToJson(message)
                bluetoothManager.sendMessage(messageJson)
            }
            NetworkChannel.DISCONNECTED -> {
                // Сообщение будет сохранено и отправлено при появлении соединения
                queueMessageForLater(peerId, message)
            }
        }
    }
    
    /**
     * Отправляет сообщение всем подключенным пирам
     */
    fun broadcastMessage(message: NetworkMessage) {
        when (_activeChannel.value) {
            NetworkChannel.WEBRTC -> webRTCManager.sendMessageToAllPeers(message)
            NetworkChannel.WIFI_DIRECT -> {
                val messageJson = convertMessageToJson(message)
                wifiDirectManager.sendMessage(messageJson) // WiFi Direct обычно point-to-point
            }
            NetworkChannel.BLUETOOTH -> {
                val messageJson = convertMessageToJson(message)
                bluetoothManager.sendMessage(messageJson) // Bluetooth обычно point-to-point
            }
            NetworkChannel.DISCONNECTED -> {
                // Сообщение будет сохранено для будущей рассылки
            }
        }
    }
    
    /**
     * Подключается к пиру через указанный канал
     */
    fun connectToPeer(peerId: String, channel: NetworkChannel, connectionData: String? = null) {
        when (channel) {
            NetworkChannel.WEBRTC -> {
                // WebRTC требует обмена SDP и ICE кандидатами
                connectionData?.let { 
                    handleWebRTCConnectionData(peerId, it) 
                }
            }
            NetworkChannel.WIFI_DIRECT -> {
                // Поиск и подключение через WiFi Direct
                wifiDirectManager.discoverPeers()
                // Подключение произойдет когда пир будет обнаружен
            }
            NetworkChannel.BLUETOOTH -> {
                // Поиск и подключение через Bluetooth
                bluetoothManager.startDiscovery()
                // Подключение произойдет когда устройство будет обнаружено
            }
            NetworkChannel.DISCONNECTED -> {
                // Нет активного соединения
            }
        }
    }
    
    /**
     * Возвращает статистику по всем каналам
     */
    fun getChannelStatistics(): Map<NetworkChannel, ChannelStats> {
        return mapOf(
            NetworkChannel.WEBRTC to getWebRTCStats(),
            NetworkChannel.WIFI_DIRECT to getWifiDirectStats(),
            NetworkChannel.BLUETOOTH to getBluetoothStats()
        )
    }
    
    fun cleanup() {
        webRTCManager.disconnectAll()
        wifiDirectManager.cleanup()
        bluetoothManager.cleanup()
    }
    
    private fun checkWebRTCAvailability(): Boolean {
        // WebRTC доступен если есть интернет (через STUN)
        return true // Упрощенная проверка
    }
    
    private fun checkWifiDirectAvailability(): Boolean {
        return wifiDirectManager.connectionState.value != WifiDirectState.DISABLED
    }
    
    private fun checkBluetoothAvailability(): Boolean {
        return bluetoothManager.isBluetoothEnabled()
    }
    
    private fun convertMessageToJson(message: NetworkMessage): String {
        // Простая конвертация в JSON
        return """
            {
                "id": "${message.id}",
                "chatId": "${message.chatId}", 
                "senderId": "${message.senderId}",
                "encryptedContent": "${message.encryptedContent}",
                "timestamp": ${message.timestamp}
            }
        """.trimIndent()
    }
    
    private fun queueMessageForLater(peerId: String, message: NetworkMessage) {
        // В реальном приложении здесь будет сохранение в локальную базу
        // и отправка при восстановлении соединения
    }
    
    private fun handleWebRTCConnectionData(peerId: String, connectionData: String) {
        // Обработка SDP offer/answer и ICE кандидатов для WebRTC
    }
    
    private fun getWebRTCStats(): ChannelStats {
        return ChannelStats(
            isAvailable = checkWebRTCAvailability(),
            latency = 50, // мс (примерно)
            bandwidth = 10000, // kbps (примерно)
            stability = 0.95 // 95% стабильности
        )
    }
    
    private fun getWifiDirectStats(): ChannelStats {
        return ChannelStats(
            isAvailable = checkWifiDirectAvailability(),
            latency = 10, // мс (низкая задержка)
            bandwidth = 25000, // kbps (высокая скорость)
            stability = 0.98 // 98% стабильности
        )
    }
    
    private fun getBluetoothStats(): ChannelStats {
        return ChannelStats(
            isAvailable = checkBluetoothAvailability(),
            latency = 100, // мс (высокая задержка)
            bandwidth = 1000, // kbps (низкая скорость)
            stability = 0.90 // 90% стабильности
        )
    }
}

enum class NetworkChannel {
    WEBRTC, WIFI_DIRECT, BLUETOOTH, DISCONNECTED
}

data class ChannelStats(
    val isAvailable: Boolean,
    val latency: Int, // milliseconds
    val bandwidth: Int, // kbps
    val stability: Double // 0.0 - 1.0
)

enum class ConnectionQuality {
    EXCELLENT, GOOD, FAIR, POOR, UNKNOWN
}
