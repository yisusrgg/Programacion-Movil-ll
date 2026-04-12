package com.example.reconocimientodewi_fi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import com.example.reconocimientodewi_fi.ui.theme.ReconocimientoDeWIFITheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val signalLevel: Int,
    val frequency: Int,
    val capabilities: String
)

class WifiViewModel : ViewModel() {
    val scanResults = mutableStateListOf<WifiNetwork>()
    var isConnected by mutableStateOf(false)
    var activeNetworkName by mutableStateOf("Desconectado")
}

class MainActivity : ComponentActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var connectivityManager: ConnectivityManager
    private val viewModel: WifiViewModel by viewModels()
    
    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success) {
                scanSuccess()
            } else {
                scanFailure()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        setupNetworkCallback()

        setContent {
            ReconocimientoDeWIFITheme {
                WifiAppScreen(
                    scanResults = viewModel.scanResults,
                    isConnected = viewModel.isConnected,
                    activeNetworkName = viewModel.activeNetworkName,
                    onScanRequest = { startWifiScan() }
                )
            }
        }
    }

    private fun setupNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                viewModel.isConnected = true
                val wifiInfo = wifiManager.connectionInfo
                viewModel.activeNetworkName = wifiInfo.ssid.replace("\"", "")
            }

            override fun onLost(network: Network) {
                viewModel.isConnected = false
                viewModel.activeNetworkName = "Desconectado"
            }
        })
    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(wifiScanReceiver)
    }

    private fun startWifiScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        wifiManager.startScan()
    }

    private fun scanSuccess() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        
        val results = wifiManager.scanResults
        updateResults(results)
    }

    private fun scanFailure() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        
        val results = wifiManager.scanResults
        updateResults(results)
    }

    private fun updateResults(results: List<ScanResult>) {
        viewModel.scanResults.clear()
        
        // Agrupamos por SSID para evitar duplicados y mostramos el de mejor señal
        val uniqueNetworks = results
            .filter { !it.SSID.isNullOrEmpty() }
            .groupBy { it.SSID }
            .map { group -> group.value.maxByOrNull { it.level }!! }
            .sortedByDescending { it.level }

        uniqueNetworks.forEach { result ->
            viewModel.scanResults.add(
                WifiNetwork(
                    ssid = result.SSID,
                    bssid = result.BSSID,
                    signalLevel = WifiManager.calculateSignalLevel(result.level, 5),
                    frequency = result.frequency,
                    capabilities = result.capabilities
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiAppScreen(
    scanResults: List<WifiNetwork>,
    isConnected: Boolean,
    activeNetworkName: String,
    onScanRequest: () -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            onScanRequest()
        } else {
            Toast.makeText(context, "Permisos de ubicación necesarios para escanear", Toast.LENGTH_SHORT).show()
        }
    }

    // Automatic scanning every 10 seconds
    LaunchedEffect(Unit) {
        while (isActive) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                onScanRequest()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            delay(10000) // Scan every 10 seconds
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reconocimiento Wi-Fi") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                onScanRequest()
                Toast.makeText(context, "Escaneando...", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Escanear ahora")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Status Card with Theme-aware colors
            val isDark = isSystemInDarkTheme()
            val cardBg = if (isConnected) {
                if (isDark) Color(0xFF1B5E20) else Color(0xFFE8F5E9)
            } else {
                if (isDark) Color(0xFFB71C1C) else Color(0xFFFFEBEE)
            }
            val iconColor = if (isConnected) {
                if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
            } else {
                if (isDark) Color(0xFFE57373) else Color(0xFFC62828)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = iconColor
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (isConnected) "Conectado a:" else "Desconectado",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = activeNetworkName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Redes Disponibles (${scanResults.size})",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.onBackground
            )

            if (scanResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Buscando redes automáticamente...",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(scanResults) { network ->
                        WifiNetworkItem(network)
                    }
                }
            }
        }
    }
}

@Composable
fun WifiNetworkItem(network: WifiNetwork) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = network.ssid,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${network.signalLevel}/4",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "BSSID: ${network.bssid}",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            val band = if (network.frequency > 4000) "5 GHz" else "2.4 GHz"
            Text(
                text = "Banda: $band (${network.frequency} MHz)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Seguridad: ${network.capabilities}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
