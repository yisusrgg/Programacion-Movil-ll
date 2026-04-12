package com.example.p2p_fishare.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import com.example.p2p_fishare.viewmodels.WiFiDirectViewModel

class WiFiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val viewModel: WiFiDirectViewModel
) : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                if (state != WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    viewModel.onDisconnected()
                }
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                manager.requestPeers(channel) { peers ->
                    viewModel.onPeersAvailable(peers)
                }
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val networkInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO, NetworkInfo::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                }

                if (networkInfo?.isConnected == true) {
                    manager.requestConnectionInfo(channel) { info ->
                        viewModel.onConnectionInfoAvailable(info)
                    }
                } else {
                    viewModel.onDisconnected()
                }
            }
        }
    }
}