package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.location.LocationCoordinates
import com.example.location.MockLocationController
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private const val TAG = "LocationScreen"
private const val INITIAL_LATITUDE = "28.6139"
private const val INITIAL_LONGITUDE = "77.2090"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val mockLocationController = remember(context) { MockLocationController(context) }
    var latitude by remember { mutableStateOf(INITIAL_LATITUDE) }
    var longitude by remember { mutableStateOf(INITIAL_LONGITUDE) }
    var isRunning by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showStopConfirmation by remember { mutableStateOf(false) }

    fun selectedCoordinates(): LocationCoordinates? {
        val coordinates = LocationCoordinates.parse(latitude, longitude)
        if (coordinates == null) {
            statusMessage = "Enter a valid latitude (-90 to 90) and longitude (-180 to 180)."
        }
        return coordinates
    }

    fun applySelectedLocation() {
        val coordinates = selectedCoordinates() ?: return
        try {
            if (isRunning) {
                mockLocationController.update(coordinates)
            } else {
                mockLocationController.start(coordinates)
                isRunning = true
            }
            statusMessage = "Mock location set to ${coordinates.latitude}, ${coordinates.longitude}."
        } catch (exception: SecurityException) {
            statusMessage =
                "Android blocked the mock provider. In Developer options, select Glitch OS as the mock location app, then try again."
            Log.e(TAG, "Mock provider permission was denied.", exception)
        } catch (exception: IllegalArgumentException) {
            statusMessage =
                "Android rejected the mock provider. Re-select Glitch OS in Developer options and try again."
            Log.e(TAG, "Android rejected the mock provider.", exception)
        } catch (exception: IllegalStateException) {
            statusMessage = exception.message ?: "The mock location provider is not ready."
            Log.e(TAG, "Mock provider is not ready.", exception)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            applySelectedLocation()
        } else {
            statusMessage =
                "Precise location permission is required for Android's mock-location provider."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Location", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "For app development and QA testing. Android marks these coordinates as a mock location; this tool does not hide that signal.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )

            LocationPickerMap(
                latitude = LocationCoordinates.parse(latitude, longitude)?.latitude ?: 28.6139,
                longitude = LocationCoordinates.parse(latitude, longitude)?.longitude ?: 77.2090,
                onLocationSelected = { selectedLatitude, selectedLongitude ->
                    latitude = "%.6f".format(java.util.Locale.US, selectedLatitude)
                    longitude = "%.6f".format(java.util.Locale.US, selectedLongitude)
                }
            )
            TextButton(
                onClick = {
                    try {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.openstreetmap.org/copyright")
                            )
                        )
                    } catch (exception: android.content.ActivityNotFoundException) {
                        statusMessage = "Could not open the OpenStreetMap copyright page."
                        Log.e(TAG, "No app is available to open the map attribution link.", exception)
                    } catch (exception: SecurityException) {
                        statusMessage = "Access to the OpenStreetMap copyright page was denied."
                        Log.e(TAG, "Could not open the map attribution link.", exception)
                    }
                }
            ) {
                Text(
                    "© OpenStreetMap contributors",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text("Latitude") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text("Longitude") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Card(colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("One-time setup", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Grant precise location permission, then open Developer options → Select mock location app → Glitch OS. Android requires you to choose the app yourself.",
                        color = TextSecondary
                    )
                    Button(
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                            } catch (exception: android.content.ActivityNotFoundException) {
                                statusMessage = "Could not open Developer options. Open Settings and enable them manually."
                                Log.e(TAG, "Developer options activity is unavailable.", exception)
                            } catch (exception: SecurityException) {
                                statusMessage = "Could not open Developer options. Open Settings and enable them manually."
                                Log.e(TAG, "Access to Developer options was denied.", exception)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Developer options")
                    }
                }
            }

            Button(
                onClick = {
                    val hasFineLocation = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasFineLocation) {
                        applySelectedLocation()
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isRunning) "Update mock location" else "Set mock location")
            }

            if (isRunning) {
                Text("Mock location is active while this screen is open.", color = TextSecondary)
                Button(
                    onClick = { showStopConfirmation = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Stop mock location")
                }
            }

            statusMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showStopConfirmation) {
        AlertDialog(
            onDismissRequest = { showStopConfirmation = false },
            title = { Text("Stop mock location?") },
            text = { Text("Apps will stop receiving this test location.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStopConfirmation = false
                        try {
                            mockLocationController.stop()
                            isRunning = false
                            statusMessage = "Mock location stopped."
                        } catch (exception: SecurityException) {
                            statusMessage = "Android did not allow the mock provider to stop. Re-select Glitch OS in Developer options."
                            Log.e(TAG, "Could not stop mock provider.", exception)
                        } catch (exception: IllegalArgumentException) {
                            statusMessage = "Android could not remove the mock provider. Restart the app and check Developer options."
                            Log.e(TAG, "Could not remove mock provider.", exception)
                        }
                    }
                ) {
                    Text("Stop")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    DisposableEffect(mockLocationController) {
        onDispose {
            if (mockLocationController.isRunning) {
                try {
                    mockLocationController.stop()
                } catch (exception: SecurityException) {
                    Log.e(TAG, "Android denied cleanup of the mock location provider.", exception)
                } catch (exception: IllegalArgumentException) {
                    Log.e(TAG, "Android could not remove the mock location provider during cleanup.", exception)
                } catch (exception: IllegalStateException) {
                    Log.e(TAG, "Could not clean up mock location after leaving the screen.", exception)
                }
            }
        }
    }
}

@Composable
private fun LocationPickerMap(
    latitude: Double,
    longitude: Double,
    onLocationSelected: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnLocationSelected by rememberUpdatedState(onLocationSelected)
    val mapView = remember(context) {
        org.maplibre.android.MapLibre.getInstance(context)
        org.maplibre.android.module.http.HttpRequestUtil.setOkHttpClient(
            okhttp3.OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", "GlitchOS/1.1 (+https://github.com/mafiyacoding001-beep/Glitch-os)")
                        .build()
                    chain.proceed(request)
                }
                .build()
        )
        org.maplibre.android.maps.MapView(context).also { it.onCreate(null) }
    }
    var mapLibreMap by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }
    var marker by remember { mutableStateOf<org.maplibre.android.annotations.Marker?>(null) }

    DisposableEffect(mapView, lifecycleOwner) {
        var started = false
        var resumed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!started) {
                    mapView.onStart()
                    started = true
                }
                Lifecycle.Event.ON_RESUME -> if (!resumed) {
                    mapView.onResume()
                    resumed = true
                }
                Lifecycle.Event.ON_PAUSE -> if (resumed) {
                    mapView.onPause()
                    resumed = false
                }
                Lifecycle.Event.ON_STOP -> if (started) {
                    mapView.onStop()
                    started = false
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (resumed) mapView.onPause()
            if (started) mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    )

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            mapLibreMap = map
            map.setStyle(
                org.maplibre.android.maps.Style.Builder().fromJson(OSM_STYLE)
            ) {
                map.addOnMapClickListener { point ->
                    latestOnLocationSelected(point.latitude, point.longitude)
                    true
                }
            }
        }
    }

    LaunchedEffect(mapLibreMap, latitude, longitude) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val position = org.maplibre.android.geometry.LatLng(latitude, longitude)
        map.animateCamera(
            org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(position, 12.0)
        )
        marker?.remove()
        marker = map.addMarker(
            org.maplibre.android.annotations.MarkerOptions()
                .position(position)
                .title("Mock location")
        )
    }
}

private val OSM_STYLE = """
    {
      "version": 8,
      "sources": {
        "openstreetmap": {
          "type": "raster",
          "tiles": ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
          "tileSize": 256,
          "attribution": "© OpenStreetMap contributors"
        }
      },
      "layers": [
        {
          "id": "openstreetmap-raster",
          "type": "raster",
          "source": "openstreetmap"
        }
      ]
    }
""".trimIndent()
