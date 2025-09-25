package com.secure.p2p.chat.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.secure.p2p.chat.ui.theme.SecureP2PTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SecureP2PTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatApp()
                }
            }
        }
    }
}

@Composable
fun ChatApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.ChatList) }
    
    when (currentScreen) {
        Screen.ChatList -> ChatListScreen(
            onCreateChat = { currentScreen = Screen.CreateChat },
            onJoinChat = { currentScreen = Screen.JoinChat }
        )
        Screen.CreateChat -> CreateChatScreen(
            onBack = { currentScreen = Screen.ChatList }
        )
        Screen.JoinChat -> JoinChatScreen(
            onBack = { currentScreen = Screen.ChatList }
        )
    }
}

@Composable
fun ChatListScreen(
    onCreateChat: () -> Unit,
    onJoinChat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Secure P2P Chat",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Button(
            onClick = onCreateChat,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Create New Secure Chat")
        }
        
        Button(
            onClick = onJoinChat,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Join Existing Chat")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Информация о безопасности
        SecurityInfoCard()
    }
}

@Composable
fun SecurityInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🔒 End-to-End Encryption",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "• Messages encrypted with AES-256\n• No server storage\n• Direct P2P connections",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun CreateChatScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Create Secure Chat",
            style = MaterialTheme.typography.headlineSmall
        )
        // Здесь будет форма создания чата
    }
}

@Composable
fun JoinChatScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Join Chat with QR Code",
            style = MaterialTheme.typography.headlineSmall
        )
        // Здесь будет сканер QR-кода
    }
}

sealed class Screen {
    object ChatList : Screen()
    object CreateChat : Screen()
    object JoinChat : Screen()
}

@Preview(showBackground = true)
@Composable
fun PreviewChatList() {
    SecureP2PTheme {
        ChatListScreen(
            onCreateChat = {},
            onJoinChat = {}
        )
    }
}
