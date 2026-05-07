package com.example.airdroid.model

import android.net.wifi.aware.PeerHandle

data class AwarePeer(
    val handle: PeerHandle,
    val deviceName: String = "Dispositivo cercano",
    val distanceMm: Int? = null
)
