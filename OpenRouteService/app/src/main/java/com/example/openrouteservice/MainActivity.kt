package com.example.openrouteservice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.openrouteservice.model.RouteRequest
import com.example.openrouteservice.network.RetrofitClient
import com.example.openrouteservice.ui.theme.OpenRouteServiceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenRouteServiceTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    Greeting()
                }
            }
        }
    }
}

@Composable
fun Greeting() {
    // Prueba rápida en un bloque que se ejecute al iniciar para ver si funciona la api
    LaunchedEffect(Unit) {
        val request = RouteRequest(
            coordinates = listOf(
                listOf(-101.18, 20.12), // Inicio (Moroleón)
                listOf(-101.15, 20.15)  // Fin (Tu casa)
            )
        )
        //val apiKey = BuildConfig.ORS_KEY

        try {
            val response = RetrofitClient.instance.getRoutePost(
                profile = "driving-car",
                apiKey = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImFlYzViOGY5ZTNmNDRkOWNhMzFmYzgxNWE2NjU1Y2FhIiwiaCI6Im11cm11cjY0In0=",
                request = request
            )

            if (response.isSuccessful) {
                val dist = response.body()?.features?.get(0)?.properties?.summary?.distance
                println("¡CONECTADO! Distancia: $dist metros")
            } else {
                println("Error de API: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            println("Error de red: ${e.message}")
        }
    }
}
