package com.example.p2p_fishare

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.p2p_fishare.receivers.WiFiDirectBroadcastReceiver
import com.example.p2p_fishare.ui.MainScreen
import com.example.p2p_fishare.ui.theme.P2PFiShareTheme
import com.example.p2p_fishare.viewmodels.WiFiDirectViewModel

class MainActivity : ComponentActivity() {

    private val manager by lazy { getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager }
    private var channel: WifiP2pManager.Channel? = null
    private var receiver: WiFiDirectBroadcastReceiver? = null
    private val viewModel: WiFiDirectViewModel by viewModels()

    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            channel?.let { viewModel.startDiscovery(manager, it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        channel = manager.initialize(this, mainLooper, null)
        receiver = channel?.let { WiFiDirectBroadcastReceiver(manager, it, viewModel) }

        setContent {
            P2PFiShareTheme {
                // Configuramos el Scaffold para que no añada insets automáticos
                // y así evitar que el chat "flote" cuando se abre el teclado
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    // Pasamos el modifier directamente sin el padding del Scaffold
                    MainScreen(
                        peers = viewModel.peersList,
                        isConnected = viewModel.isConnected,
                        status = viewModel.connectionStatus,
                        messages = viewModel.chatMessages,
                        onDiscoverClick = { checkPermissionsAndDiscover() },
                        onConnectClick = { device -> channel?.let { viewModel.connectToDevice(manager, it, device) } },
                        onSendMessage = { message -> viewModel.sendMessage(message) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    private fun checkPermissionsAndDiscover() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            channel?.let { viewModel.startDiscovery(manager, it) }
        } else {
            requestPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    override fun onResume() {
        super.onResume()
        receiver?.let { registerReceiver(it, intentFilter) }
    }

    override fun onPause() {
        super.onPause()
        receiver?.let { unregisterReceiver(it) }
    }
}
