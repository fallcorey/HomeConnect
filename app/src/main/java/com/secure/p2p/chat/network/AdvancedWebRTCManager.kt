package com.secure.p2p.chat.network

import android.content.Context
import org.webrtc.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

class AdvancedWebRTCManager(private val context: Context) {
    private val peerConnectionFactory: PeerConnectionFactory
    private val peerConnections = mutableMapOf<String, PeerConnection>()
    private val dataChannels = mutableMapOf<String, DataChannel>()
    
    // STUN сервера для установки P2P соединений
    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
    )
    
    // Состояния соединений
    private val _connectionStates = MutableStateFlow<Map<String, ConnectionState>>(emptyMap())
    val connectionStates: StateFlow<Map<String, ConnectionState>> = _connectionStates
    
    private val _incomingMessages = MutableStateFlow<List<NetworkMessage>>(emptyList())
    val incomingMessages: StateFlow<List<NetworkMessage>> = _incomingMessages
    
    // Сигнальные данные для обмена между пирами
    private val _localIceCandidates = MutableStateFlow<List<IceCandidate>>(emptyList())
    val localIceCandidates: StateFlow<List<IceCandidate>> = _localIceCandidates
    
    private val _localSessionDescription = MutableStateFlow<SessionDescription?>(null)
    val localSessionDescription: StateFlow<SessionDescription?> = _localSessionDescription

    init {
        // Инициализация WebRTC
        val initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        
        PeerConnectionFactory.initialize(initializationOptions)
        
        // Создаем фабрику peer connection
        val options = PeerConnectionFactory.Options().apply {
            disableEncryption = false
            disableNetworkMonitor = false
        }
        
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory()
    }
    
    fun createOffer(peerId: String): SessionDescription? {
        val peerConnection = createPeerConnection(peerId)
        peerConnections[peerId] = peerConnection
        
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
        }
        
        var offer: SessionDescription? = null
        peerConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let {
                    peerConnection.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            _localSessionDescription.value = it
                        }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    }, it)
                    offer = it
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                println("Create offer failed: $error")
            }
            override fun onSetFailure(error: String?) {
                println("Set local description failed: $error")
            }
        }, constraints)
        
        return offer
    }
    
    fun createAnswer(peerId: String, offer: SessionDescription): SessionDescription? {
        val peerConnection = createPeerConnection(peerId)
        peerConnections[peerId] = peerConnection
        
        peerConnection.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                val constraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
                }
                
                peerConnection.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(desc: SessionDescription?) {
                        desc?.let {
                            peerConnection.setLocalDescription(object : SdpObserver {
                                override fun onCreateSuccess(p0: SessionDescription?) {}
                                override fun onSetSuccess() {
                                    _localSessionDescription.value = it
                                }
                                override fun onCreateFailure(p0: String?) {}
                                override fun onSetFailure(p0: String?) {}
                            }, it)
                        }
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(error: String?) {
                        println("Create answer failed: $error")
                    }
                    override fun onSetFailure(error: String?) {
                        println("Set local description failed: $error")
                    }
                }, constraints)
            }
            override fun onSetFailure(error: String?) {
                println("Set remote description failed: $error")
            }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, offer)
        
        return _localSessionDescription.value
    }
    
    fun setRemoteDescription(peerId: String, answer: SessionDescription) {
        peerConnections[peerId]?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                println("Remote description set successfully")
            }
            override fun onSetFailure(error: String?) {
                println("Set remote description failed: $error")
            }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, answer)
    }
    
    fun addIceCandidate(peerId: String, candidate: IceCandidate) {
        peerConnections[peerId]?.addIceCandidate(candidate)
    }
    
    fun sendMessage(peerId: String, message: NetworkMessage) {
        val dataChannel = dataChannels[peerId]
        if (dataChannel?.state() == DataChannel.State.OPEN) {
            val jsonData = """
                {
                    "id": "${message.id}",
                    "chatId": "${message.chatId}",
                    "senderId": "${message.senderId}",
                    "encryptedContent": "${message.encryptedContent}",
                    "iv": "${message.iv}",
                    "timestamp": ${message.timestamp},
                    "messageType": "${message.messageType}"
                }
            """.trimIndent()
            
            val buffer = ByteBuffer.wrap(jsonData.toByteArray(Charsets.UTF_8))
            dataChannel.send(DataChannel.Buffer(buffer, false))
        }
    }
    
    fun sendMessageToAllPeers(message: NetworkMessage) {
        dataChannels.keys.forEach { peerId ->
            sendMessage(peerId, message)
        }
    }
    
    fun disconnectFromPeer(peerId: String) {
        peerConnections[peerId]?.close()
        peerConnections.remove(peerId)
        dataChannels.remove(peerId)
        
        updateConnectionState(peerId, ConnectionState.DISCONNECTED)
    }
    
    fun disconnectAll() {
        peerConnections.keys.forEach { peerId ->
            disconnectFromPeer(peerId)
        }
    }
    
    private fun createPeerConnection(peerId: String): PeerConnection {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            keyType = PeerConnection.KeyType.ECDSA
        }
        
        return peerConnectionFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                println("Signaling state changed: $state")
            }
            
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                val connectionState = when (state) {
                    PeerConnection.IceConnectionState.CONNECTED -> ConnectionState.CONNECTED
                    PeerConnection.IceConnectionState.CONNECTING -> ConnectionState.CONNECTING
                    PeerConnection.IceConnectionState.DISCONNECTED -> ConnectionState.DISCONNECTED
                    else -> ConnectionState.DISCONNECTED
                }
                updateConnectionState(peerId, connectionState)
            }
            
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    _localIceCandidates.value = _localIceCandidates.value + it
                }
            }
            
            override fun onDataChannel(dataChannel: DataChannel?) {
                dataChannel?.let {
                    setupDataChannel(peerId, it)
                }
            }
            
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
        }) ?: throw IllegalStateException("Failed to create peer connection")
    }
    
    private fun setupDataChannel(peerId: String, dataChannel: DataChannel) {
        dataChannels[peerId] = dataChannel
        
        dataChannel.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(amount: Long) {
                // Можно отслеживать буферизацию данных
            }
            
            override fun onStateChange() {
                when (dataChannel.state()) {
                    DataChannel.State.OPEN -> {
                        updateConnectionState(peerId, ConnectionState.CONNECTED)
                        println("Data channel opened for peer: $peerId")
                    }
                    DataChannel.State.CLOSED -> {
                        updateConnectionState(peerId, ConnectionState.DISCONNECTED)
                        println("Data channel closed for peer: $peerId")
                    }
                    else -> {}
                }
            }
            
            override fun onMessage(message: DataChannel.Buffer?) {
                message?.let { buffer ->
                    if (!buffer.binary) {
                        val data = ByteArray(buffer.data.remaining())
                        buffer.data.get(data)
                        val jsonString = String(data, Charsets.UTF_8)
                        processIncomingMessage(jsonString)
                    }
                }
            }
        })
    }
    
    private fun processIncomingMessage(jsonString: String) {
        try {
            // Простой парсинг JSON (в реальном приложении используйте Gson/Moshi)
            val message = parseNetworkMessage(jsonString)
            _incomingMessages.value = _incomingMessages.value + message
        } catch (e: Exception) {
            println("Failed to parse incoming message: ${e.message}")
        }
    }
    
    private fun parseNetworkMessage(jsonString: String): NetworkMessage {
        // Упрощенный парсинг JSON
        return NetworkMessage(
            id = extractJsonField(jsonString, "id"),
            chatId = extractJsonField(jsonString, "chatId"),
            senderId = extractJsonField(jsonString, "senderId"),
            encryptedContent = extractJsonField(jsonString, "encryptedContent"),
            iv = extractJsonField(jsonString, "iv"),
            timestamp = extractJsonField(jsonString, "timestamp").toLongOrNull() ?: 0L,
            messageType = MessageType.valueOf(extractJsonField(jsonString, "messageType"))
        )
    }
    
    private fun extractJsonField(jsonString: String, fieldName: String): String {
        val pattern = "\"$fieldName\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        return pattern.find(jsonString)?.groupValues?.get(1) ?: ""
    }
    
    private fun updateConnectionState(peerId: String, state: ConnectionState) {
        val currentStates = _connectionStates.value.toMutableMap()
        currentStates[peerId] = state
        _connectionStates.value = currentStates
    }
}

enum class ConnectionState {
    CONNECTED, CONNECTING, DISCONNECTED, FAILED
}
