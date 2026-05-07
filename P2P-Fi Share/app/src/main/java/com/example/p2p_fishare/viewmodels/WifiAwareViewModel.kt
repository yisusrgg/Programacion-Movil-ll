package com.example.p2p_fishare.viewmodels

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.aware.*
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.net.Inet6Address
import java.net.ServerSocket
import java.net.Socket

class WifiAwareViewModel : ViewModel() {

    var isSupported by mutableStateOf(true)
    var isAvailable by mutableStateOf(false)
    var isConnected by mutableStateOf(false)
    var connectionStatus by mutableStateOf("Iniciando...")
    var chatMessages by mutableStateOf(listOf<String>())
    var discoveredPeers = mutableStateOf(mapOf<PeerHandle, String>())

    private var wifiAwareSession: WifiAwareSession? = null
    private var discoverySession: DiscoverySession? = null
    private var connectivityManager: ConnectivityManager? = null
    
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null
    private var targetPeer: PeerHandle? = null
    private var targetAddress: Inet6Address? = null
    private var targetPort: Int? = null

    private val SERVICE_NAME = "P2P_FiShare_Service"

    fun checkSupport(context: Context) {
        isSupported = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_WIFI_AWARE)
        val manager = if (isSupported) {
            context.getSystemService(Context.WIFI_AWARE_SERVICE) as? WifiAwareManager
        } else {
            null
        }
        
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        isAvailable = manager?.isAvailable == true
        
        if (!isSupported || manager == null) {
            isSupported = false
            connectionStatus = "Hardware Aware no soportado"
        } else if (!isAvailable) {
            connectionStatus = "Activa Wi-Fi y Ubicación"
        } else {
            connectionStatus = "Listo para buscar"
        }
    }

    @SuppressLint("MissingPermission")
    fun startAwareSession(context: Context) {
        if (!isSupported) {
            connectionStatus = "Hardware Aware no soportado"
            return
        }

        val manager = context.getSystemService(Context.WIFI_AWARE_SERVICE) as? WifiAwareManager
        if (manager == null) {
            connectionStatus = "Servicio Aware no disponible"
            return
        }

        manager.attach(object : AttachCallback() {
            override fun onAttached(session: WifiAwareSession) {
                wifiAwareSession = session
                publishAndSubscribe()
            }
            override fun onAttachFailed() {
                connectionStatus = "Error de sesión Aware"
            }
        }, Handler(Looper.getMainLooper()))
    }

    private fun publishAndSubscribe() {
        val session = wifiAwareSession ?: return
        
        val pubConfig = PublishConfig.Builder().setServiceName(SERVICE_NAME).build()
        session.publish(pubConfig, object : DiscoverySessionCallback() {
            override fun onPublishStarted(session: PublishDiscoverySession) {
                discoverySession = session
                connectionStatus = "Buscando dispositivos..."
                startServer()
            }
            override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                val msgStr = String(message)
                if (msgStr.startsWith("PORT:")) {
                    val port = msgStr.substring(5).toIntOrNull() ?: return
                    targetPort = port
                    requestNetwork(peerHandle, true)
                }
            }
        }, null)

        val subConfig = SubscribeConfig.Builder().setServiceName(SERVICE_NAME).build()
        session.subscribe(subConfig, object : DiscoverySessionCallback() {
            override fun onServiceDiscovered(peerHandle: PeerHandle, serviceInfo: ByteArray, matchFilter: List<ByteArray>) {
                val newMap = discoveredPeers.value.toMutableMap()
                newMap[peerHandle] = "Dispositivo Cercano"
                discoveredPeers.value = newMap
            }
        }, null)
    }

    @SuppressLint("MissingPermission")
    fun connectToPeer(peerHandle: PeerHandle) {
        targetPeer = peerHandle
        connectionStatus = "Solicitando conexión..."
        val port = serverSocket?.localPort ?: 0
        discoverySession?.sendMessage(peerHandle, 0, "PORT:$port".toByteArray())
        requestNetwork(peerHandle, false)
    }

    @SuppressLint("MissingPermission")
    private fun requestNetwork(peerHandle: PeerHandle, isServer: Boolean) {
        val session = discoverySession ?: return
        val networkSpecifier = WifiAwareNetworkSpecifier.Builder(session, peerHandle)
            .setPskPassphrase("fishare123")
            .build()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
            .setNetworkSpecifier(networkSpecifier)
            .build()

        connectivityManager?.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isConnected = true
                connectionStatus = "¡Conexión establecida!"
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val awareInfo = caps.transportInfo as? WifiAwareNetworkInfo
                targetAddress = awareInfo?.peerIpv6Addr
                awareInfo?.port?.let { if (it > 0) targetPort = it }
            }

            override fun onLost(network: Network) {
                isConnected = false
                connectionStatus = "Conexión perdida"
            }
        })
    }

    private fun startServer() {
        if (serverJob != null) return
        serverJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(0)
                while (true) {
                    val client = serverSocket?.accept() ?: break
                    handleClient(client)
                }
            } catch (e: Exception) { Log.e("Aware", "Server error", e) }
        }
    }

    private suspend fun handleClient(client: Socket) {
        withContext(Dispatchers.IO) {
            try {
                val dataInput = DataInputStream(client.getInputStream())
                val type = dataInput.readInt()
                if (type == 1) {
                    val text = dataInput.readUTF()
                    withContext(Dispatchers.Main) { chatMessages = chatMessages + "Recibido: $text" }
                } else if (type == 2) {
                    val name = dataInput.readUTF()
                    val size = dataInput.readLong()
                    val buffer = ByteArray(4096)
                    var totalRead = 0L
                    while (totalRead < size) {
                        val read = client.getInputStream().read(buffer)
                        if (read == -1) break
                        totalRead += read
                    }
                    withContext(Dispatchers.Main) { chatMessages = chatMessages + "Archivo recibido: $name" }
                }
            } catch (e: Exception) { Log.e("Aware", "Client error", e) }
            finally { client.close() }
        }
    }

    fun sendMessage(text: String) {
        val host = targetAddress ?: return
        val port = targetPort ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val socket = Socket(host, port)
                val dataOutput = DataOutputStream(socket.getOutputStream())
                dataOutput.writeInt(1)
                dataOutput.writeUTF(text)
                socket.close()
                withContext(Dispatchers.Main) { chatMessages = chatMessages + "Yo: $text" }
            } catch (e: Exception) { Log.e("Aware", "Send error", e) }
        }
    }

    fun sendFile(contentResolver: ContentResolver, uri: Uri) {
        val host = targetAddress ?: return
        val port = targetPort ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var name = "archivo"
                var size = 0L
                contentResolver.query(uri, null, null, null, null)?.use {
                    val nameIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = it.getColumnIndex(OpenableColumns.SIZE)
                    if (it.moveToFirst()) {
                        name = it.getString(nameIdx)
                        size = it.getLong(sizeIdx)
                    }
                }
                val socket = Socket(host, port)
                val out = DataOutputStream(socket.getOutputStream())
                out.writeInt(2)
                out.writeUTF(name)
                out.writeLong(size)
                contentResolver.openInputStream(uri)?.use { input ->
                    val buffer = ByteArray(4096)
                    var len: Int
                    while (input.read(buffer).also { len = it } != -1) {
                        out.write(buffer, 0, len)
                    }
                }
                socket.close()
                withContext(Dispatchers.Main) { chatMessages = chatMessages + "Yo envié: $name" }
            } catch (e: Exception) { Log.e("Aware", "File error", e) }
        }
    }
}
