package com.example.openrouteservice.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.openrouteservice.viewmodel.RouteViewModel
import com.google.android.gms.location.*
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun MainScreen(routeViewModel: RouteViewModel = viewModel()) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // --- Ubicación de casa ---
    val homeLocation = remember { GeoPoint(20.143672, -101.165705) }

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
            routePoints = routeViewModel.routePoints
        )

        Button(
            onClick = {
                currentLocation?.let { start ->
                    routeViewModel.fetchRoute(start, homeLocation)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            enabled = currentLocation != null
        ) {
            Text("Trazar Ruta a Casa")
        }
    }
}

@Composable
fun MapViewCompose(currentLocation: GeoPoint?, destination: GeoPoint, routePoints: List<GeoPoint>) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            controller.setZoom(15.0)
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            view.overlays.clear()

            // Marcador de Casa
            val homeMarker = Marker(view)
            homeMarker.position = destination
            homeMarker.title = "Mi Casa"
            view.overlays.add(homeMarker)

            // Marcador de Ubicación Actual
            currentLocation?.let {
                val startMarker = Marker(view)
                startMarker.position = it
                startMarker.title = "Yo"
                startMarker.icon = ContextCompat.getDrawable(context, org.osmdroid.library.R.drawable.person)
                view.overlays.add(startMarker)

                if (routePoints.isEmpty()) {
                    view.controller.setCenter(it)
                }
            }

            if (routePoints.isNotEmpty()) {
                val line = Polyline()
                line.setPoints(routePoints)
                line.outlinePaint.color = Color.BLUE
                line.outlinePaint.strokeWidth = 10f
                view.overlays.add(line)
            }
            view.invalidate()
        }
    )
}
