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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SecureP2PTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SimpleWelcomeScreen()
                }
            }
        }
    }
}

@Composable
fun SimpleWelcomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Secure P2P Chat",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "Building secure messaging...",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Button(
            onClick = { /* TODO */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Started")
        }
    }
}

@Composable
fun SecureP2PTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        content = content
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewWelcomeScreen() {
    SecureP2PTheme {
        SimpleWelcomeScreen()
    }
}
