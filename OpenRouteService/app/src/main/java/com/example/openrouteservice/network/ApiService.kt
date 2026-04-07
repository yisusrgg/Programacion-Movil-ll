package com.example.openrouteservice.network

import com.example.openrouteservice.model.GeoJsonResponse
import com.example.openrouteservice.model.RouteRequest
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.Response


interface ApiService {
    @POST("v2/directions/{profile}/geojson")
    suspend fun getRoutePost(
        @Path("profile") profile: String,
        @Header("Authorization") apiKey: String,
        @Body request: RouteRequest
    ): Response<GeoJsonResponse>
}