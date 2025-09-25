package com.secure.p2p.chat.network

import android.content.*
import android.net.wifi.p2p.*
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.*
import java.net.ServerSocket
import java.net.Socket

class WifiDirectManager(private val context: Context) : WifiP2pManager.ConnectionInfoListener {
    private val manager: WifiP2pManager? by lazy {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    }
    private var channel: Channel? = null
    
    private val _discoveredPeers = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val discoveredPeers: StateFlow<List<WifiP2pDevice>> = _discoveredPeers
    
    private val _connectionState = MutableStateFlow(WifiDirectState.DISCONNECTED)
    val connectionState: StateFlow<WifiDirectState> = _connectionState
    
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        _connectionState.value = WifiDirectState.ENABLED
                    } else {
                        _connectionState.value = WifiDirectState.DISABLED
                    }
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    manager?.requestPeers(channel) { peers ->
                        _discoveredPeers.value = peers.deviceList.toList()
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    manager?.requestConnectionInfo(channel, this@WifiDirectManager)
                }
            }
        }
    }
    
    fun initialize() {
        channel = manager?.initialize(context, context.mainLooper, null)
        
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }
        
        context.registerReceiver(receiver, intentFilter)
        
        // Запускаем серверный сокет для принятия подключений
        startServerSocket()
    }
    
    fun discoverPeers() {
        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("WifiDirect", "Discovery started")
            }
            override fun onFailure(reasonCode: Int) {
                Log.e("WifiDirect", "Discovery failed: $reasonCode")
            }
        })
    }
    
    fun connectToDevice(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WifiP2pConfig.WpsDisplay(device.primaryDeviceType)
        }
        
        manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("WifiDirect", "Connection initiated")
                _connectionState.value = WifiDirectState.CONNECTING
            }
            override fun onFailure(reason: Int) {
                Log.e("WifiDirect", "Connection failed: $reason")
                _connectionState.value = WifiDirectState.FAILED
            }
        })
    }
    
    fun sendMessage(message: String) {
        try {
            outputStream?.writeUTF(message)
            outputStream?.flush()
        } catch (e: IOException) {
            Log.e("WifiDirect", "Failed to send message", e)
        }
    }
    
    fun disconnect() {
        manager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _connectionState.value = WifiDirectState.DISCONNECTED
                cleanupSockets()
            }
            override fun onFailure(reason: Int) {
                Log.e("WifiDirect", "Disconnect failed: $reason")
            }
        })
    }
    
    override fun onConnectionInfoReady(info: WifiP2pInfo?) {
        info?.let {
            if (it.groupFormed) {
                if (it.isGroupOwner) {
                    // Мы владелец группы - ждем подключений
                    _connectionState.value = WifiDirectState.CONNECTED_AS_OWNER
                } else {
                    // Мы клиент - подключаемся к владельцу
                    connectToGroupOwner(it.groupOwnerAddress.hostAddress)
                    _connectionState.value = WifiDirectState.CONNECTED_AS_CLIENT
                }
            }
        }
    }
    
    private fun startServerSocket() {
        Thread {
            try {
                serverSocket = ServerSocket(8888)
                while (true) {
                    val client = serverSocket?.accept()
                    client?.let {
                        handleClientConnection(it)
                    }
                }
            } catch (e: IOException) {
                Log.e("WifiDirect", "Server socket error", e)
            }
        }.start()
    }
    
    private fun connectToGroupOwner(hostAddress: String) {
        Thread {
            try {
                clientSocket = Socket(hostAddress, 8888)
                outputStream = DataOutputStream(clientSocket?.getOutputStream())
                inputStream = DataInputStream(clientSocket?.getInputStream())
                
                // Запускаем чтение сообщений
                startReadingMessages()
            } catch (e: IOException) {
                Log.e("WifiDirect", "Failed to connect to group owner", e)
            }
        }.start()
    }
    
    private fun handleClientConnection(socket: Socket) {
        Thread {
            try {
                outputStream = DataOutputStream(socket.getOutputStream())
                inputStream = DataInputStream(socket.getInputStream())
                startReadingMessages()
            } catch (e: IOException) {
                Log.e("WifiDirect", "Client connection error", e)
            }
        }.start()
    }
    
    private fun startReadingMessages() {
        Thread {
            try {
                while (true) {
                    val message = inputStream?.readUTF()
                    message?.let {
                        // Обрабатываем входящее сообщение
                        Log.d("WifiDirect", "Received: $it")
                    }
                }
            } catch (e: IOException) {
                Log.e("WifiDirect", "Message reading error", e)
            }
        }.start()
    }
    
    private fun cleanupSockets() {
        try {
            serverSocket?.close()
            clientSocket?.close()
            outputStream?.close()
            inputStream?.close()
        } catch (e: IOException) {
            Log.e("WifiDirect", "Cleanup error", e)
        }
    }
    
    fun cleanup() {
        context.unregisterReceiver(receiver)
        cleanupSockets()
    }
}

enum class WifiDirectState {
    DISABLED, ENABLED, CONNECTING, CONNECTED_AS_OWNER, CONNECTED_AS_CLIENT, DISCONNECTED, FAILED
}
