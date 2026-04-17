package com.example.fcmapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    private lateinit var tvToken: TextView
    private lateinit var tvLastMessage: TextView

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("FCM_UI", "Broadcast recibido en MainActivity")
            val title = intent?.getStringExtra("title") ?: "Sin título"
            val body = intent?.getStringExtra("body") ?: ""
            
            // Forzar actualización en el hilo principal por seguridad
            runOnUiThread {
                tvLastMessage.text = "📩 $title\n$body"
                Log.d("FCM_UI", "UI Actualizada: $title")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvToken = findViewById(R.id.tvToken)
        tvLastMessage = findViewById(R.id.tvLastMessage)

        // Solicitar permiso de notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                    Log.d("FCM", "Permiso otorgado: $isGranted")
                }.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Obtener Token de FCM
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result ?: "Token nulo"
                tvToken.text = token
                Log.d("FCM", "Token actual: $token")
            } else {
                tvToken.text = "Error al obtener token"
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("com.example.fcmapp.FCM_MESSAGE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            // Para versiones anteriores a Android 13
            registerReceiver(receiver, filter)
        }
        Log.d("FCM_UI", "Receiver registrado en onResume")
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(receiver)
            Log.d("FCM_UI", "Receiver desregistrado en onPause")
        } catch (e: Exception) {
            Log.e("FCM_UI", "Error al desregistrar receiver", e)
        }
    }
}
