package com.example.p2p_fishare

import android.Manifest
elimiimport android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.p2p_fishare.ui.MainScreen
import com.example.p2p_fishare.ui.theme.P2PFiShareTheme
import com.example.p2p_fishare.viewmodels.WifiAwareViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: WifiAwareViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            viewModel.startAwareSession(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            P2PFiShareTheme {
                val filePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    uri?.let { viewModel.sendFile(contentResolver, it) }
                }

                LaunchedEffect(Unit) {
                    viewModel.checkSupport(this@MainActivity)
                    checkPermissionsAndStart()
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    MainScreen(
                        peers = emptyList(),
                        isConnected = viewModel.isConnected,
                        status = viewModel.connectionStatus,
                        messages = viewModel.chatMessages,
                        onDiscoverClick = { checkPermissionsAndStart() },
                        onConnectClick = { /* Wi-Fi Aware logic */ },
                        onSendMessage = { message -> viewModel.sendMessage(message) },
                        onSendFileClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )

                    if (!viewModel.isConnected) {
                        val peers = viewModel.discoveredPeers.value
                        if (peers.isNotEmpty()) {
                            val firstPeer = peers.keys.first()
                            viewModel.connectToPeer(firstPeer)
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            viewModel.startAwareSession(this)
        } else {
            requestPermissionLauncher.launch(missing.toTypedArray())
        }
    }
}
