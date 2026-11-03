package com.example.ui.screens

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.location.LocationCoordinates
import com.example.location.MockLocationController
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "LocationScreen"
private const val INITIAL_LATITUDE = 28.6139
private const val INITIAL_LONGITUDE = 77.2090
private const val LOCATION_TIMEOUT_MS = 20_000L

@Composable
fun LocationScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val mockLocationController = remember(context) { MockLocationController(context) }
    var latitude by remember { mutableStateOf(INITIAL_LATITUDE) }
    var longitude by remember { mutableStateOf(INITIAL_LONGITUDE) }
    var isRunning by remember { mutableStateOf(mockLocationController.isRunning) }
    var mockAppSelected by remember { mutableStateOf(mockLocationController.isSelectedAsMockApp()) }
    var developerOptionsEnabled by remember { mutableStateOf(isDeveloperOptionsEnabled(context)) }
    var isLocating by remember { mutableStateOf(false) }

    fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun applySelectedLocation() {
        val coordinates = LocationCoordinates(latitude, longitude)
        try {
            if (mockLocationController.isRunning) {
                mockLocationController.update(coordinates)
            } else {
                mockLocationController.start(coordinates)
            }
            isRunning = true
            mockAppSelected = mockLocationController.isSelectedAsMockApp()
            showMessage("Mock location set to ${coordinates.latitude}, ${coordinates.longitude}.")
        } catch (exception: SecurityException) {
            mockAppSelected = false
            showMessage("Select Glitch OS as the mock location app in Developer options first.")
            Log.e(TAG, "Android denied the mock location provider.", exception)
        } catch (exception: IllegalArgumentException) {
            mockAppSelected = mockLocationController.isSelectedAsMockApp()
            showMessage("Android rejected the mock provider. Re-select Glitch OS in Developer options.")
            Log.e(TAG, "Android rejected the mock location provider.", exception)
        } catch (exception: IllegalStateException) {
            showMessage(exception.message ?: "The mock location provider is not ready.")
            Log.e(TAG, "Mock location provider is not ready.", exception)
        }
    }

    suspend fun moveToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            showMessage("Allow precise location to find your current position.")
            return
        }
        isLocating = true
        try {
            val location = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                requestCurrentLocation(context)
            }
            if (location == null) {
                showMessage("Could not get a location fix. Check that Location is turned on and try again.")
            } else {
                latitude = location.latitude
                longitude = location.longitude
            }
        } catch (exception: SecurityException) {
            showMessage("Allow precise location to find your current position.")
            Log.e(TAG, "Android denied access to the current location.", exception)
        } catch (exception: IllegalArgumentException) {
            showMessage("No location provider is available. Turn on device Location and try again.")
            Log.e(TAG, "No usable current-location provider is available.", exception)
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            showMessage("Could not get the current location. Check device Location settings.")
            Log.e(TAG, "Could not retrieve the current location.", exception)
        } finally {
            isLocating = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            if (isLocating) {
                scope.launch { moveToCurrentLocation() }
            } else {
                applySelectedLocation()
            }
        } else {
            isLocating = false
            showMessage("Precise location permission is required for this action.")
        }
    }

    DisposableEffect(lifecycleOwner, mockLocationController) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                mockAppSelected = mockLocationController.isSelectedAsMockApp()
                developerOptionsEnabled = isDeveloperOptionsEnabled(context)
                isRunning = mockLocationController.isRunning
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (mockLocationController.isRunning) {
                try {
                    mockLocationController.stop()
                } catch (exception: SecurityException) {
                    Log.e(TAG, "Android denied cleanup of the mock location provider.", exception)
                } catch (exception: IllegalArgumentException) {
                    Log.e(TAG, "Android could not remove the mock location provider.", exception)
                } catch (exception: IllegalStateException) {
                    Log.e(TAG, "Could not clean up the mock location provider.", exception)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LocationPickerMap(
            latitude = latitude,
            longitude = longitude,
            onLocationSelected = { selectedLatitude, selectedLongitude ->
                latitude = selectedLatitude
                longitude = selectedLongitude
            }
        )

        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            shape = CircleShape,
            color = DarkSurface.copy(alpha = 0.92f)
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            shape = CircleShape,
            color = DarkSurface.copy(alpha = 0.92f)
        ) {
            IconButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        scope.launch { moveToCurrentLocation() }
                    } else {
                        isLocating = true
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    }
                },
                enabled = !isLocating
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = "Go to current location",
                    tint = if (isLocating) TextSecondary else CyberCyan
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!mockAppSelected) {
                OutlinedButton(
                    onClick = {
                        val action = if (developerOptionsEnabled) {
                            Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
                        } else {
                            Settings.ACTION_DEVICE_INFO_SETTINGS
                        }
                        try {
                            context.startActivity(Intent(action))
                            showMessage(
                                if (developerOptionsEnabled) {
                                    "In Developer options, choose Glitch OS under Select mock location app."
                                } else {
                                    "Open Build number and tap it 7 times to enable Developer options, then return here."
                                }
                            )
                        } catch (exception: Exception) {
                            showMessage("Could not open Settings. Open Settings and enable Developer options manually.")
                            Log.e(TAG, "Could not open the required Android settings page.", exception)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = DarkSurface.copy(alpha = 0.94f),
                        contentColor = TextPrimary
                    )
                ) {
                    Text(
                        if (developerOptionsEnabled) {
                            "Set mock app in Developer options"
                        } else {
                            "Enable Developer options"
                        }
                    )
                }
            }

            Button(
                onClick = {
                    if (mockAppSelected) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            applySelectedLocation()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        }
                    } else {
                        showMessage("Set Glitch OS as the mock location app before setting a location.")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = DarkBackground
                )
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Text(if (isRunning) "Update location" else "Set location", modifier = Modifier.padding(start = 8.dp))
            }

            if (isRunning) {
                OutlinedButton(
                    onClick = {
                        try {
                            mockLocationController.stop()
                            isRunning = false
                            showMessage("Mock location stopped.")
                        } catch (exception: SecurityException) {
                            showMessage("Android denied stopping the mock provider.")
                            Log.e(TAG, "Could not stop the mock provider.", exception)
                        } catch (exception: IllegalArgumentException) {
                            showMessage("Android could not remove the mock provider. Re-select Glitch OS in Developer options.")
                            Log.e(TAG, "Could not remove the mock provider.", exception)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = DarkSurface.copy(alpha = 0.94f),
                        contentColor = TextPrimary
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Stop mock location", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        Text(
            "© OpenStreetMap contributors",
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = if (isRunning) 190.dp else 140.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
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
                        .header("User-Agent", "GlitchOS/1.2 (+https://github.com/bholabhai5692/Glitch-os)")
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
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            mapView.onStart()
            started = true
        }
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
            resumed = true
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (resumed) mapView.onPause()
            if (started) mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    )

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            mapLibreMap = map
            map.setStyle(org.maplibre.android.maps.Style.Builder().fromJson(OSM_STYLE)) {
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
            org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(position, 15.0)
        )
        marker?.remove()
        marker = map.addMarker(
            org.maplibre.android.annotations.MarkerOptions()
                .position(position)
                .title("Mock location")
        )
    }
}

private suspend fun requestCurrentLocation(context: Context): Location? =
    suspendCancellableCoroutine { continuation ->
        val locationManager = context.getSystemService(LocationManager::class.java)
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { provider -> locationManager.isProviderEnabled(provider) }
        if (providers.isEmpty()) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val handler = Handler(Looper.getMainLooper())
        lateinit var listener: LocationListener
        fun finish(location: Location?) {
            if (!continuation.isActive) return
            handler.removeCallbacksAndMessages(null)
            locationManager.removeUpdates(listener)
            continuation.resume(location)
        }
        listener = LocationListener { location -> finish(location) }
        val timeout = Runnable {
            if (continuation.isActive) {
                locationManager.removeUpdates(listener)
                continuation.resumeWithException(TimeoutException("Timed out waiting for a location fix."))
            }
        }
        continuation.invokeOnCancellation {
            handler.removeCallbacks(timeout)
            locationManager.removeUpdates(listener)
        }
        handler.postDelayed(timeout, LOCATION_TIMEOUT_MS)
        try {
            providers.forEach { provider ->
                locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            }
        } catch (exception: SecurityException) {
            handler.removeCallbacks(timeout)
            locationManager.removeUpdates(listener)
            continuation.resumeWithException(exception)
        } catch (exception: IllegalArgumentException) {
            handler.removeCallbacks(timeout)
            locationManager.removeUpdates(listener)
            continuation.resumeWithException(exception)
        }
    }

private fun isDeveloperOptionsEnabled(context: Context): Boolean =
    Settings.Global.getInt(
        context.contentResolver,
        Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
        0
    ) == 1

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
