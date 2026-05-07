package com.example.airdroid.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.aware.WifiAwareManager

class AwareStateReceiver(private val onStateChanged: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED) {
            onStateChanged()
        }
    }
}
