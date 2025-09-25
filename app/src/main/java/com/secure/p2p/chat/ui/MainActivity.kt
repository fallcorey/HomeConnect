package com.secure.p2p.chat.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.secure.p2p.chat.ui.components.*
import com.secure.p2p.chat.ui.theme.SecureP2PTheme
import com.secure.p2p.chat.ui.viewmodels.ChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SecureP2PTheme {
                SecureP2PApp()
            }
        }
    }
}

@Composable
fun SecureP2PApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.ChatList) }
    var chatInviteData by remember { mutableStateOf<String?>(null) }
    
    val viewModel = ChatViewModel()
    
    when (currentScreen) {
        Screen.ChatList -> ChatListScreen(
            onCreateChat = { currentScreen = Screen.CreateChat },
            onJoinChat = { currentScreen = Screen.JoinChat }
        )
        
        Screen.CreateChat -> CreateChatScreen(
            viewModel = viewModel,
            onBack = { currentScreen = Screen.ChatList },
            onChatCreated = { qrData ->
                chatInviteData = qrData
                currentScreen = Screen.QrCodeDisplay
            }
        )
        
        Screen.JoinChat -> QrCodeScannerScreen(
            onQrCodeScanned = { qrData ->
                chatInviteData = qrData
                currentScreen = Screen.Chat
                viewModel.joinChat(qrData, "User${System.currentTimeMillis() % 1000}")
            },
            onBack = { currentScreen = Screen.ChatList }
        )
        
        Screen.QrCodeDisplay -> {
            chatInviteData?.let { qrData ->
                QrCodeDisplayScreen(
                    qrData = qrData,
                    onBack = { currentScreen = Screen.ChatList },
                    onChatJoined = {
                        currentScreen = Screen.Chat
                        viewModel.createNewChat("Creator")
                    }
                )
            }
        }
        
        Screen.Chat -> ChatScreen(
            viewModel = viewModel,
            onBack = { currentScreen = Screen.ChatList }
        )
    }
}

@Composable
fun CreateChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onChatCreated: (String) -> Unit
) {
    var userName by remember { mutableStateOf("") }
    
    // Упрощенный экран создания чата
    Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Create Secure Chat",
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
        
        androidx.compose.material3.OutlinedTextField(
            value = userName,
            onValueChange = { userName = it },
            label = { Text("Your Name") },
            modifier = androidx.compose.ui.Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
        
        androidx.compose.material3.Button(
            onClick = {
                if (userName.isNotBlank()) {
                    viewModel.createNewChat(userName)
                    onChatCreated("test-qr-data-${System.currentTimeMillis()}")
                }
            },
            modifier = androidx.compose.ui.Modifier.fillMaxWidth()
        ) {
            Text("Create Chat & Generate QR Code")
        }
        
        Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
        
        androidx.compose.material3.TextButton(
            onClick = onBack,
            modifier = androidx.compose.ui.Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}

sealed class Screen {
    object ChatList : Screen()
    object CreateChat : Screen()
    object JoinChat : Screen()
    object QrCodeDisplay : Screen()
    object Chat : Screen()
}

@Preview(showBackground = true)
@Composable
fun PreviewSecureP2PApp() {
    SecureP2PTheme {
        SecureP2PApp()
    }
}
