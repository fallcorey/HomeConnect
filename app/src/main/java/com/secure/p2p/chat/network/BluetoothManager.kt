package com.secure.p2p.chat.network

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.*
import java.util.*

class BluetoothManager(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }
    
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices
    
    private val _connectionState = MutableStateFlow(BluetoothState.DISCONNECTED)
    val connectionState: StateFlow<BluetoothState> = _connectionState
    
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (!_discoveredDevices.value.contains(it)) {
                            _discoveredDevices.value = _discoveredDevices.value + it
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _connectionState.value = BluetoothState.DISCOVERY_FINISHED
                }
            }
        }
    }
    
    fun initialize() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(receiver, filter)
        
        startBluetoothServer()
    }
    
    fun enableBluetooth(): Boolean {
        return bluetoothAdapter?.enable() ?: false
    }
    
    fun disableBluetooth(): Boolean {
        return bluetoothAdapter?.disable() ?: false
    }
    
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    fun startDiscovery() {
        _discoveredDevices.value = emptyList()
        bluetoothAdapter?.startDiscovery()
        _connectionState.value = BluetoothState.DISCOVERING
    }
    
    fun stopDiscovery() {
        bluetoothAdapter?.cancelDiscovery()
    }
    
    fun connectToDevice(device: BluetoothDevice) {
        Thread {
            try {
                _connectionState.value = BluetoothState.CONNECTING
                
                // UUID для SPP (Serial Port Profile)
                val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                clientSocket = device.createRfcommSocketToServiceRecord(uuid)
                
                bluetoothAdapter?.cancelDiscovery() // Важно отменить discovery перед подключением
                
                clientSocket?.connect()
                setupDataStreams(clientSocket)
                
                _connectionState.value = BluetoothState.CONNECTED
                startReadingMessages()
                
            } catch (e: IOException) {
                Log.e("Bluetooth", "Connection failed", e)
                _connectionState.value = BluetoothState.FAILED
            }
        }.start()
    }
    
    fun sendMessage(message: String) {
        try {
            outputStream?.write(message.toByteArray())
            outputStream?.flush()
        } catch (e: IOException) {
            Log.e("Bluetooth", "Failed to send message", e)
        }
    }
    
    fun disconnect() {
        try {
            clientSocket?.close()
            serverSocket?.close()
            outputStream?.close()
            inputStream?.close()
            _connectionState.value = BluetoothState.DISCONNECTED
        } catch (e: IOException) {
            Log.e("Bluetooth", "Disconnect error", e)
        }
    }
    
    private fun startBluetoothServer() {
        Thread {
            try {
                // UUID для чат-приложения
                val uuid = UUID.fromString("a5a5a5a5-1111-2222-3333-444444444444")
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("SecureP2PChat", uuid)
                
                _connectionState.value = BluetoothState.LISTENING
                
                while (true) {
                    val socket = serverSocket?.accept()
                    socket?.let {
                        clientSocket = it
                        setupDataStreams(it)
                        _connectionState.value = BluetoothState.CONNECTED
                        startReadingMessages()
                        break
                    }
                }
            } catch (e: IOException) {
                Log.e("Bluetooth", "Server error", e)
                _connectionState.value = BluetoothState.FAILED
            }
        }.start()
    }
    
    private fun setupDataStreams(socket: BluetoothSocket) {
        outputStream = socket.outputStream
        inputStream = socket.inputStream
    }
    
    private fun startReadingMessages() {
        Thread {
            val buffer = ByteArray(1024)
            var bytes: Int
            
            try {
                while (true) {
                    bytes = inputStream?.read(buffer) ?: break
                    if (bytes > 0) {
                        val message = String(buffer, 0, bytes)
                        Log.d("Bluetooth", "Received: $message")
                        // Обрабатываем входящее сообщение
                    }
                }
            } catch (e: IOException) {
                Log.e("Bluetooth", "Message reading error", e)
                _connectionState.value = BluetoothState.DISCONNECTED
            }
        }.start()
    }
    
    fun cleanup() {
        context.unregisterReceiver(receiver)
        disconnect()
    }
}

enum class BluetoothState {
    DISABLED, ENABLED, DISCOVERING, DISCOVERY_FINISHED, LISTENING, CONNECTING, CONNECTED, DISCONNECTED, FAILED
}
