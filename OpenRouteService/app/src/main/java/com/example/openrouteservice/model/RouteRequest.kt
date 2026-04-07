package com.example.openrouteservice.model

data class RouteRequest(
    val coordinates: List<List<Double>>,
    val instructions: Boolean = true,
    val language: String = "es"
)