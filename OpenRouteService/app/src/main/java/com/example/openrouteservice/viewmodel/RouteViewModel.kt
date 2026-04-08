package com.example.openrouteservice.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.openrouteservice.model.RouteRequest
import com.example.openrouteservice.network.RetrofitClient
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

class RouteViewModel : ViewModel() {
    var routePoints by mutableStateOf<List<GeoPoint>>(emptyList())
        private set

    private val apiKey = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjA0ZTk4ZGFmNmFiMTQyMjBiZWIwNGNmYTg5ZTQ5OWNmIiwiaCI6Im11cm11cjY0In0="

    fun fetchRoute(start: GeoPoint, end: GeoPoint) {
        viewModelScope.launch {
            val request = RouteRequest(
                coordinates = listOf(
                    listOf(start.longitude, start.latitude),
                    listOf(end.longitude, end.latitude)
                )
            )

            try {
                val response = RetrofitClient.instance.getRoutePost("driving-car", apiKey, request)
                if (response.isSuccessful) {
                    val coords = response.body()?.features?.get(0)?.geometry?.coordinates
                    routePoints = coords?.map { GeoPoint(it[1], it[0]) } ?: emptyList()
                    Log.d("RouteViewModel", "Ruta obtenida con éxito")
                } else {
                    Log.e("RouteViewModel", "Error en la respuesta: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("RouteViewModel", "Error de red: ${e.message}")
            }
        }
    }
}
