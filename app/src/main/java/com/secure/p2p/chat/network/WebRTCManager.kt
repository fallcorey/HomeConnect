package com.secure.p2p.chat.network

import org.webrtc.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WebRTCManager {
    private val peerConnectionFactory: PeerConnectionFactory
    private val peerConnections = mutableMapOf<String, PeerConnection>()
    private val dataChannels = mutableMapOf<String, DataChannel>()
    
    // Состояние соединения
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState
    
    // Входящие сообщения
    private val _incomingMessages = MutableStateFlow<List<NetworkMessage>>(emptyList())
    val incomingMessages: StateFlow<List<NetworkMessage>> = _incomingMessages

    init {
        // Инициализация WebRTC
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(ApplicationContextProvider.getApplicationContext())
                .createInitializationOptions()
        )
        
        val options = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory()
    }

    fun createPeerConnection(peerId: String): PeerConnection {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            keyType = PeerConnection.KeyType.ECDSA
        }
        
        val peerConnection = peerConnectionFactory.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                    // Обработка изменения состояния сигналинга
                }
                
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    when (state) {
                        PeerConnection.IceConnectionState.CONNECTED -> {
                            _connectionState.value = ConnectionState.CONNECTED
                        }
                        PeerConnection.IceConnectionState.DISCONNECTED -> {
                            _connectionState.value = ConnectionState.DISCONNECTED
                        }
                        else -> {}
                    }
                }
                
                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let {
                        // Отправляем ICE кандидата другому пиру
                        onIceCandidateGenerated(peerId, it)
                    }
                }
                
                override fun onDataChannel(dataChannel: DataChannel?) {
                    dataChannel?.let {
                        setupDataChannel(peerId, it)
                    }
                }
                
                // Остальные методы оставляем пустыми
                override fun onIceConnectionReceivingChange(p0: Boolean) {}
                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
                override fun onAddStream(p0: MediaStream?) {}
                override fun onRemoveStream(p0: MediaStream?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
            }
        ) ?: throw IllegalStateException("Failed to create peer connection")
        
        peerConnections[peerId] = peerConnection
        return peerConnection
    }
    
    fun createDataChannel(peerId: String, label: String): DataChannel {
        val init = DataChannel.Init().apply {
            ordered = true
            maxRetransmits = 3
        }
        
        val dataChannel = peerConnections[peerId]?.createDataChannel(label, init)
            ?: throw IllegalStateException("Peer connection not found")
        
        setupDataChannel(peerId, dataChannel)
        dataChannels[peerId] = dataChannel
        
        return dataChannel
    }
    
    private fun setupDataChannel(peerId: String, dataChannel: DataChannel) {
        dataChannel.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(amount: Long) {
                // Изменение буферизованного количества данных
            }
            
            override fun onStateChange() {
                when (dataChannel.state()) {
                    DataChannel.State.OPEN -> {
                        _connectionState.value = ConnectionState.CONNECTED
                    }
                    DataChannel.State.CLOSED -> {
                        _connectionState.value = ConnectionState.DISCONNECTED
                    }
                    else -> {}
                }
            }
            
            override fun onMessage(message: DataChannel.Buffer?) {
                message?.let {
                    if (it.binary) {
                        // Бинарные данные (шифрованные сообщения)
                        val data = ByteArray(it.data.remaining())
                        it.data.get(data)
                        processIncomingMessage(peerId, data)
                    }
                }
            }
        })
    }
    
    fun sendMessage(peerId: String, message: NetworkMessage) {
        val dataChannel = dataChannels[peerId]
        if (dataChannel?.state() == DataChannel.State.OPEN) {
            // Конвертируем сообщение в JSON и шифруем
            val jsonMessage = """
                {
                    "id": "${message.id}",
                    "chatId": "${message.chatId}",
                    "senderId": "${message.senderId}",
                    "encryptedContent": "${message.encryptedContent}",
                    "iv": "${message.iv}",
                    "timestamp": ${message.timestamp}
                }
            """.trimIndent()
            
            val data = jsonMessage.toByteArray(Charsets.UTF_8)
            val buffer = ByteBuffer.wrap(data)
            dataChannel.send(DataChannel.Buffer(buffer, false))
        }
    }
    
    private fun processIncomingMessage(peerId: String, data: ByteArray) {
        // Обработка входящего сообщения
        try {
            val jsonString = String(data, Charsets.UTF_8)
            // Парсим JSON и добавляем в incomingMessages
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun onIceCandidateGenerated(peerId: String, candidate: IceCandidate) {
        // Отправляем ICE кандидата другому участнику
        // В реальном приложении это будет через сигнальный сервер или QR-код
    }
}

enum class ConnectionState {
    CONNECTED, CONNECTING, DISCONNECTED
}

// Простой провайдер контекста (заглушка)
object ApplicationContextProvider {
    fun getApplicationContext(): android.content.Context {
        // В реальном приложении это будет предоставлено через Dependency Injection
        throw IllegalStateException("Context not provided")
    }
}
