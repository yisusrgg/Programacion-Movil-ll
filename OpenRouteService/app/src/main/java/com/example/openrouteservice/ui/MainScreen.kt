package com.example.openrouteservice.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.openrouteservice.viewmodel.RouteViewModel
import com.google.android.gms.location.*
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun MainScreen(routeViewModel: RouteViewModel = viewModel()) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val sharedPrefs = remember { context.getSharedPreferences("open_route_prefs", Context.MODE_PRIVATE) }

    // --- Ubicación de casa con persistencia ---
    var homeLocation by remember {
        val lat = sharedPrefs.getFloat("home_lat", 20.143672f).toDouble()
        val lon = sharedPrefs.getFloat("home_lon", -101.165705f).toDouble()
        mutableStateOf(GeoPoint(lat, lon))
    }

    val saveHomeLocation = { point: GeoPoint ->
        homeLocation = point
        sharedPrefs.edit()
            .putFloat("home_lat", point.latitude.toFloat())
            .putFloat("home_lon", point.longitude.toFloat())
            .apply()
        Toast.makeText(context, "Ubicación de casa guardada", Toast.LENGTH_SHORT).show()
    }

    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasPermission = isGranted }
    )

    // Configuración para actualizaciones en tiempo real
    DisposableEffect(hasPermission) {
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    currentLocation = GeoPoint(it.latitude, it.longitude)
                }
            }
        }

        if (hasPermission) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setMinUpdateIntervalMillis(1000)
                .build()

            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) { e.printStackTrace() }
        } else {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapViewCompose(
            currentLocation = currentLocation,
            destination = homeLocation,
            routePoints = routeViewModel.routePoints,
            onMapLongClick = { newHome ->
                saveHomeLocation(newHome)
            }
        )

        // Botón principal: Trazar Ruta
        Button(
            onClick = {
                currentLocation?.let { start ->
                    routeViewModel.fetchRoute(start, homeLocation)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp),
            enabled = currentLocation != null
        ) {
            Text("Trazar Ruta a Casa")
        }
        
        // Indicador de ayuda
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
        ) {
            Text(
                text = "Mantén presionado el mapa para fijar tu casa",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = Color.White
            )
        }
    }
}

@Composable
fun MapViewCompose(
    currentLocation: GeoPoint?, 
    destination: GeoPoint, 
    routePoints: List<GeoPoint>,
    onMapLongClick: (GeoPoint) -> Unit
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(true)
            controller.setZoom(15.0)
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            view.overlays.clear()

            // Eventos del mapa (Long Click para cambiar destino)
            val eventsReceiver = object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean = false
                override fun longPressHelper(p: GeoPoint): Boolean {
                    onMapLongClick(p)
                    return true
                }
            }
            view.overlays.add(MapEventsOverlay(eventsReceiver))

            // Marcador de Destino (Casa)
            val homeMarker = Marker(view)
            homeMarker.position = destination
            homeMarker.title = "Mi Casa"
            view.overlays.add(homeMarker)

            // Marcador de Ubicación Actual
            currentLocation?.let {
                val startMarker = Marker(view)
                startMarker.position = it
                startMarker.title = "Mi Ubicación"
                startMarker.icon = ContextCompat.getDrawable(context, org.osmdroid.library.R.drawable.person)
                view.overlays.add(startMarker)
            }

            if (routePoints.isNotEmpty()) {
                val line = Polyline()
                line.setPoints(routePoints)
                line.outlinePaint.color = AndroidColor.BLUE
                line.outlinePaint.strokeWidth = 12f
                view.overlays.add(line)
            }
            view.invalidate()
        }
    )
}
