package com.example.airdroid.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.aware.*
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import com.example.airdroid.model.AwarePeer

class AwareViewModel : ViewModel() {
    var isAvailable by mutableStateOf(false)
    var isAttached by mutableStateOf(false)
    val discoveredPeers = mutableStateListOf<AwarePeer>()
    var errorMessage by mutableStateOf<String?>(null)
    var isPublishing by mutableStateOf(false)
    var isSubscribing by mutableStateOf(false)

    private var wifiAwareManager: WifiAwareManager? = null
    private var awareSession: WifiAwareSession? = null
    private val serviceName = "_airdroid_share._tcp"

    fun initialize(context: Context) {
        wifiAwareManager = context.getSystemService(Context.WIFI_AWARE_SERVICE) as WifiAwareManager?
        checkAvailability(context)
    }

    fun checkAvailability(context: Context) {
        val hasFeature = context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)
        val isEnabled = wifiAwareManager?.isAvailable == true
        isAvailable = hasFeature && isEnabled

        errorMessage = when {
            !hasFeature -> "Tu dispositivo no soporta Wi-Fi Aware"
            !isEnabled -> "Wi-Fi o Ubicación están desactivados"
            else -> null
        }
    }

    fun startSharing(context: Context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        wifiAwareManager?.attach(object : AttachCallback() {
            override fun onAttached(session: WifiAwareSession) {
                awareSession = session
                isAttached = true
                publishService()
                subscribeToService()
            }

            override fun onAttachFailed() {
                errorMessage = "Error al iniciar sesión Wi-Fi Aware"
            }
        }, Handler(Looper.getMainLooper()))
    }

    fun stopSharing() {
        awareSession?.close()
        awareSession = null
        isAttached = false
        isPublishing = false
        isSubscribing = false
        discoveredPeers.clear()
    }

    private fun publishService() {
        val config = PublishConfig.Builder()
            .setServiceName(serviceName)
            .build()

        awareSession?.publish(config, object : DiscoverySessionCallback() {
            override fun onPublishStarted(session: PublishDiscoverySession) {
                isPublishing = true
            }

            override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                // Notificar mensaje recibido
            }
        }, Handler(Looper.getMainLooper()))
    }

    private fun subscribeToService() {
        val config = SubscribeConfig.Builder()
            .setServiceName(serviceName)
            .build()

        awareSession?.subscribe(config, object : DiscoverySessionCallback() {
            override fun onSubscribeStarted(session: SubscribeDiscoverySession) {
                isSubscribing = true
            }

            override fun onServiceDiscovered(
                peerHandle: PeerHandle,
                serviceSpecificInfo: ByteArray,
                matchFilter: List<ByteArray>
            ) {
                if (discoveredPeers.none { it.handle == peerHandle }) {
                    discoveredPeers.add(AwarePeer(peerHandle))
                }
            }

            override fun onServiceLost(peerHandle: PeerHandle, reason: Int) {
                discoveredPeers.removeAll { it.handle == peerHandle }
            }
        }, Handler(Looper.getMainLooper()))
    }

    override fun onCleared() {
        super.onCleared()
        stopSharing()
    }
}
