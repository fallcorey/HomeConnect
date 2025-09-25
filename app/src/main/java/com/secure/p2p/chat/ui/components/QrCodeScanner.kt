package com.secure.p2p.chat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeScannerScreen(
    onQrCodeScanned: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Scan QR Code") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Placeholder для настоящего сканера QR-кодов
            // В реальном приложении здесь будет CameraX с ML Kit
            
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "QR Scanner",
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "QR Code Scanner",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Point your camera at the QR code to join the secure chat",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Кнопка для тестирования (временная)
            Button(
                onClick = {
                    // Временная заглушка для тестирования
                    showTestQrDialog(context, onQrCodeScanned)
                }
            ) {
                Text("Test with Sample QR Code")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(onClick = onBack) {
                Text("Enter Code Manually")
            }
        }
    }
}

private fun showTestQrDialog(context: Context, onQrCodeScanned: (String) -> Unit) {
    // Временная функция для тестирования
    val testQrData = "SECURE_CHAT_INVITE|test-chat-123|test-session-key|TestUser|${System.currentTimeMillis()}"
    
    Toast.makeText(
        context, 
        "Simulating QR scan with test data", 
        Toast.LENGTH_LONG
    ).show()
    
    onQrCodeScanned(testQrData)
}

@Composable
fun QrCodeDisplayScreen(
    qrData: String,
    onBack: () -> Unit
) {
    var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    LaunchedEffect(qrData) {
        try {
            // Генерируем QR-код из данных
            qrBitmap = QrCodeGenerator.generateQrCode(qrData, 400, 400)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Your QR Code") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Share this QR code to invite others",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            if (qrBitmap != null) {
                // Здесь будет Android-специфичный код для отображения Bitmap
                // В реальном приложении используйте `ImageBitmap` и `asImageBitmap()`
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "QR Code Preview\n(300x300)",
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                CircularProgressIndicator()
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Scan this code with another device to join the secure chat",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Информация о безопасности
            SecurityNotice()
        }
    }
}

@Composable
fun SecurityNotice() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🔒 Security Information",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "• QR code contains encrypted session key\n• Connection is end-to-end encrypted\n• No data is stored on servers",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
