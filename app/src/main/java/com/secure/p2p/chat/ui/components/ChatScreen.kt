package com.secure.p2p.chat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.secure.p2p.chat.data.model.Message
import com.secure.p2p.chat.ui.viewmodels.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Автоскролл к новым сообщениям
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                scrollState.animateScrollToItem(messages.size)
            }
        }
    }
    
    Scaffold(
        topBar = {
            ChatTopAppBar(
                connectionState = uiState.connectionState,
                onBack = onBack
            )
        },
        bottomBar = {
            MessageInputField(
                onSendMessage = { message ->
                    viewModel.sendMessage(message)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Индикатор соединения
            ConnectionStatusIndicator(uiState.connectionState)
            
            // Список сообщений
            if (messages.isEmpty()) {
                EmptyChatPlaceholder()
            } else {
                MessagesList(
                    messages = messages,
                    scrollState = scrollState
                )
            }
        }
    }
}

@Composable
fun ChatTopAppBar(
    connectionState: WebRTCManager.ConnectionState,
    onBack: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Secure",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Secure Chat")
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }
    )
}

@Composable
fun ConnectionStatusIndicator(connectionState: WebRTCManager.ConnectionState) {
    val (text, color) = when (connectionState) {
        WebRTCManager.ConnectionState.CONNECTED -> "🟢 Secure Connection" to Color(0xFF4CAF50)
        WebRTCManager.ConnectionState.CONNECTING -> "🟡 Connecting..." to Color(0xFFFFC107)
        WebRTCManager.ConnectionState.DISCONNECTED -> "🔴 Disconnected" to Color(0xFFF44336)
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.1f))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun MessagesList(
    messages: List<Message>,
    scrollState: androidx.compose.foundation.lazy.LazyListState
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .weight(1f),
        state = scrollState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages) { message ->
            MessageBubble(message = message)
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    val isOwnMessage = message.senderName == "You"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isOwnMessage) MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(12.dp),
            horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
        ) {
            if (!isOwnMessage) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isOwnMessage) MaterialTheme.colorScheme.onPrimary 
                       else MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = formatTime(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = if (isOwnMessage) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun MessageInputField(onSendMessage: (String) -> Unit) {
    var messageText by remember { mutableStateOf("") }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = messageText,
            onValueChange = { messageText = it },
            placeholder = { Text("Type a secure message...") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        IconButton(
            onClick = {
                if (messageText.isNotBlank()) {
                    onSendMessage(messageText)
                    messageText = ""
                }
            },
            modifier = Modifier
                .size(48.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(24.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun EmptyChatPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "🔒 Secure Chat",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Your messages are end-to-end encrypted",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Start a conversation by sending a message",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

private fun formatTime(timestamp: Long): String {
    // Простое форматирование времени
    return android.text.format.DateFormat.format("HH:mm", java.util.Date(timestamp)).toString()
}

@Preview
@Composable
fun PreviewMessageBubble() {
    val message = Message(
        content = "Hello, this is a secure message!",
        senderName = "Test User",
        timestamp = System.currentTimeMillis()
    )
    MessageBubble(message = message)
}
