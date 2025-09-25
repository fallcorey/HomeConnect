package com.secure.p2p.chat.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
                    Greeting("Secure P2P Chat")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to $name!",
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.headlineMedium
        )
        
        Button(
            onClick = { /* TODO: Implement chat creation */ },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Start Secure Chat")
        }
        
        Button(
            onClick = { /* TODO: Implement join chat */ },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Join Existing Chat")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SecureP2PTheme {
        Greeting("Android")
    }
}
