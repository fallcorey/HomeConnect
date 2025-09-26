package com.example.securep2pchat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var chatHistory: TextView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var connectButton: Button
    private lateinit var startServerButton: Button
    private lateinit var ipInput: EditText
    private lateinit var portInput: EditText
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var chatService: SimpleChatService? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupClickListeners()
    }
    
    private fun initializeViews() {
        chatHistory = findViewById(R.id.chatHistory)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        connectButton = findViewById(R.id.connectButton)
        startServerButton = findViewById(R.id.startServerButton)
        ipInput = findViewById(R.id.ipInput)
        portInput = findViewById(R.id.portInput)
        
        // Set default values for testing
        ipInput.setText("127.0.0.1")
        portInput.setText("8080")
    }
    
    private fun setupClickListeners() {
        connectButton.setOnClickListener {
            val ip = ipInput.text.toString()
            val port = portInput.text.toString().toIntOrNull() ?: 8080
            connectToServer(ip, port)
        }
        
        startServerButton.setOnClickListener {
            val port = portInput.text.toString().toIntOrNull() ?: 8080
            startServer(port)
        }
        
        sendButton.setOnClickListener {
            sendMessage()
        }
    }
    
    private fun connectToServer(ip: String, port: Int) {
        coroutineScope.launch {
            try {
                chatService = SimpleChatService()
                withContext(Dispatchers.IO) {
                    chatService!!.connectToServer(ip, port) { message ->
                        coroutineScope.launch {
                            showMessage("Server: $message")
                        }
                    }
                }
                showMessage("Connected to $ip:$port")
                updateUI(true)
            } catch (e: Exception) {
                showMessage("Connection failed: ${e.message}")
            }
        }
    }
    
    private fun startServer(port: Int) {
        coroutineScope.launch {
            try {
                chatService = SimpleChatService()
                withContext(Dispatchers.IO) {
                    chatService!!.startServer(port) { message ->
                        coroutineScope.launch {
                            showMessage("Client: $message")
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
        
        val service = chatService
        if (service == null || !service.isConnected()) {
            showMessage("Not connected to any
