package com.example.openrouteservice.model

//MODELO PARA RESPRESNTAR EL GEOJSON RESPONSE DE LA API ===========================================

data class GeoJsonResponse(
    val features: List<GeoFeature>, //arreglo[]
    val metadata: Metadata //objeto{}
)

data class GeoFeature(
    val geometry: GeoGeometry,    //contiene los puntos para el mapa
    val properties: GeoProperties, //contiene distancia, tiempo, etc
)

data class GeoGeometry(
    val type: String,
    val coordinates: List<List<Double>> // Lista de [Longitud, Latitud]
)
data class GeoProperties(
    val summary: RouteSummary,
    val segments: List<RouteSegment> //instrucciones paso a paso
)

data class RouteSummary(
    val distance: Double, //distancia total
    val duration: Double  //tiempo total en segundos
)

data class RouteSegment(
    val distance: Double,
    val duration: Double,
    val steps: List<RouteStep>
)
data class RouteStep(
    val distance: Double,
    val duration: Double,
    val instruction: String,
    val name: String
)


data class Metadata(
    val attribution: String,
    val service: String
)

