package com.example.p2p_fishare.viewmodels

import android.annotation.SuppressLint
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.*
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class WiFiDirectViewModel : ViewModel() {

    var peersList by mutableStateOf(listOf<WifiP2pDevice>())
    var isConnected by mutableStateOf(false)
    var chatMessages by mutableStateOf(listOf<String>())
    var connectionStatus by mutableStateOf("Desconectado")
    
    private var targetIp: String? = null
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null

    @SuppressLint("MissingPermission")
    fun startDiscovery(manager: WifiP2pManager, channel: WifiP2pManager.Channel) {
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                connectionStatus = "Buscando..."
            }
            override fun onFailure(reason: Int) {
                connectionStatus = "Error búsqueda: $reason"
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(manager: WifiP2pManager, channel: WifiP2pManager.Channel, device: WifiP2pDevice) {
        connectionStatus = "Preparando conexión..."

        // Limpiar grupo previo antes de conectar (vital para que funcione la invitación)
        manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                proceedToConnect(manager, channel, device)
            }
            override fun onFailure(reason: Int) {
                proceedToConnect(manager, channel, device)
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun proceedToConnect(manager: WifiP2pManager, channel: WifiP2pManager.Channel, device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }
        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                connectionStatus = "Conectando a ${device.deviceName}..."
                Log.d("WiFiDirect", "Invitación enviada correctamente")
            }
            override fun onFailure(reason: Int) {
                connectionStatus = "Fallo al conectar: $reason"
            }
        })
    }

    fun onConnectionInfoAvailable(info: WifiP2pInfo) {
        if (info.groupFormed) {
            isConnected = true
            connectionStatus = if (info.isGroupOwner) "Conectado (Servidor)" else "Conectado (Cliente)"
            startServer()
            if (!info.isGroupOwner && info.groupOwnerAddress != null) {
                targetIp = info.groupOwnerAddress.hostAddress
                viewModelScope.launch {
                    delay(1500)
                    sendMessage("Conectado")
                }
            }
        }
    }

    fun onPeersAvailable(peers: WifiP2pDeviceList) {
        peersList = peers.deviceList.toList()
        if (!isConnected) {
            connectionStatus = "Dispositivos encontrados: ${peersList.size}"
        }
    }

    fun onDisconnected() {
        isConnected = false
        connectionStatus = "Desconectado"
        targetIp = null
        stopServer()
        chatMessages = emptyList()
    }

    private fun startServer() {
        if (serverJob != null) return
        serverJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(8888)
                while (true) {
                    val client = serverSocket?.accept() ?: break
                    val clientIp = client.inetAddress.hostAddress
                    if (targetIp == null) targetIp = clientIp

                    val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                    val message = reader.readLine()
                    if (message != null && message != "Conectado") {
                        withContext(Dispatchers.Main) {
                            chatMessages = chatMessages + "Recibido: $message"
                        }
                    }
                    client.close()
                }
            } catch (e: Exception) {
                Log.e("WiFiDirect", "Server error", e)
            }
        }
    }

    private fun stopServer() {
        serverJob?.cancel()
        serverJob = null
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        serverSocket = null
    }

    fun sendMessage(message: String) {
        val host = targetIp ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(host, 8888), 5000)
                val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
                writer.println(message)
                socket.close()
                if (message != "Conectado") {
                    withContext(Dispatchers.Main) {
                        chatMessages = chatMessages + "Yo: $message"
                    }
                }
            } catch (e: Exception) {
                Log.e("WiFiDirect", "Send error", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopServer()
    }
}