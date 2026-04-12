package com.example.p2p_fishare.ui

import android.net.wifi.p2p.WifiP2pDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainScreen(
    peers: List<WifiP2pDevice>,
    isConnected: Boolean,
    status: String,
    messages: List<String>,
    onDiscoverClick: () -> Unit,
    onConnectClick: (WifiP2pDevice) -> Unit,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textToSend by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Estado: $status",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (!isConnected) {
                Button(onClick = onDiscoverClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Buscar Dispositivos")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Dispositivos detectados:", style = MaterialTheme.typography.titleMedium)
                LazyColumn {
                    items(peers) { device ->
                        Text(
                            text = "${device.deviceName}\n${device.deviceAddress}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onConnectClick(device) }
                                .padding(vertical = 12.dp)
                        )
                        HorizontalDivider()
                    }
                }
            } else {
                Text("¡Conectado!", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(messages) { msg ->
                        val isMe = msg.startsWith("Yo:")
                        // Limpiamos el prefijo para mostrar solo el contenido en la burbuja
                        val displayMsg = msg.removePrefix("Yo: ").removePrefix("Recibido: ")
                        ChatBubble(message = displayMsg, isMe = isMe)
                    }
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = textToSend,
                        onValueChange = { textToSend = it },
                        placeholder = { Text("Escribe un mensaje") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 4
                    )
                    IconButton(
                        onClick = {
                            if (textToSend.isNotBlank()) {
                                onSendMessage(textToSend)
                                textToSend = ""
                            }
                        },
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: String, isMe: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isMe) Color(0xFFDCF8C6) else Color.White,
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isMe) 12.dp else 0.dp,
                bottomEnd = if (isMe) 0.dp else 12.dp
            ),
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                fontSize = 16.sp,
                color = Color.Black
            )
        }
    }
}
