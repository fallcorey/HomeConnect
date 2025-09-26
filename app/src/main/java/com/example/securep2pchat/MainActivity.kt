package com.example.securep2pchat

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var chatService: SecureChatService
    private lateinit var cryptoManager: CryptoManager
    private lateinit var messageInput: EditText
    private lateinit var chatHistory: TextView
    private lateinit var connectButton: Button
    private lateinit var startServerButton: Button
    private lateinit var ipInput: EditText
    private lateinit var portInput: EditText
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        initializeServices()
        setupClickListeners()
    }
    
    private fun initializeViews() {
        messageInput = findViewById(R.id.messageInput)
        chatHistory = findViewById(R.id.chatHistory)
        connectButton = findViewById(R.id.connectButton)
        startServerButton = findViewById(R.id.startServerButton)
        ipInput = findViewById(R.id.ipInput)
        portInput = findViewById(R.id.portInput)
        
        // Set default values for testing
        ipInput.setText("192.168.1.1")
        portInput.setText("8080")
    }
    
    private fun initializeServices() {
        cryptoManager = CryptoManager()
        chatService = SecureChatService(cryptoManager)
    }
    
    private fun setupClickListeners() {
        connectButton.setOnClickListener {
            val ip = ipInput.text.toString()
            val port = portInput.text.toString().toIntOrNull() ?: 8080
            
            if (ip.isBlank()) {
                showMessage("Please enter server IP")
                return@setOnClickListener
            }
            
            connectToServer(ip, port)
        }
        
        startServerButton.setOnClickListener {
            val port = portInput.text.toString().toIntOrNull() ?: 8080
            startServer(port)
        }
        
        findViewById<Button>(R.id.sendButton).setOnClickListener {
            sendMessage()
        }
    }
    
    private fun connectToServer(ip: String, port: Int) {
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    chatService.connectToServer(ip, port) { message ->
                        coroutineScope.launch {
                            showMessage("Peer: $message")
                        }
                    }
                }
                showMessage("Connected to server $ip:$port")
                updateUI(true)
            } catch (e: Exception) {
                showMessage("Connection failed: ${e.message}")
            }
        }
    }
    
    private fun startServer(port: Int) {
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    chatService.startServer(port) { message ->
                        coroutineScope.launch {
                            showMessage("Peer: $message")
                        }
                    }
                }
                showMessage("Server started on port $port")
                updateUI(true)
            } catch (e: Exception) {
                showMessage("Server error: ${e.message}")
            }
        }
    }
    
    private fun sendMessage() {
        val message = messageInput.text.toString()
        if (message.isBlank()) return
        
        if (!chatService.isConnected()) {
            showMessage("Not connected to any peer")
            return
        }
        
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    chatService.sendMessage(message)
                }
                showMessage("You: $message")
                messageInput.setText("")
            } catch (e: Exception) {
                showMessage("Send failed: ${e.message}")
            }
        }
    }
    
    private fun showMessage(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val formattedMessage = "[$timestamp] $message\n"
        
        runOnUiThread {
            chatHistory.append(formattedMessage)
            
            // Auto-scroll to bottom
            val scrollView = findViewById<ScrollView>(R.id.scrollView)
            scrollView.post {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }
    
    private fun updateUI(connected: Boolean) {
        runOnUiThread {
            connectButton.isEnabled = !connected
            startServerButton.isEnabled = !connected
            findViewById<Button>(R.id.sendButton).isEnabled = connected
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        chatService.stop()
    }
}
