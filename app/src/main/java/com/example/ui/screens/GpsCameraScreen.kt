package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Environment
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.location.LocationCompat
import androidx.exifinterface.media.ExifInterface
import coil.compose.AsyncImage
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class GpsPhoto(val file: File, val location: String?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsCameraScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val photos = remember { mutableStateListOf<GpsPhoto>() }
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var locationGranted by remember {
        mutableStateOf(hasGpsPermission(context))
    }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var caption by remember { mutableStateOf("") }
    var showGallery by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Allow camera and location access to capture GPS photos.") }

    fun refreshPhotos() {
        val directory = photoDirectory(context)
        val files = directory.listFiles()
            ?.filter { it.isFile && it.extension.equals("jpg", ignoreCase = true) }
            ?.sortedByDescending(File::lastModified)
            .orEmpty()
        scope.launch {
            val entries = withContext(kotlinx.coroutines.Dispatchers.IO) {
                files.map { file ->
                val exif = runCatching { ExifInterface(file.absolutePath) }.getOrNull()
                val coords = FloatArray(2)
                val gps = if (exif?.getLatLong(coords) == true) {
                    "${"%.5f".format(Locale.US, coords[0])}, ${"%.5f".format(Locale.US, coords[1])}"
                } else {
                    null
                }
                GpsPhoto(file, gps)
                }
            }
            photos.clear()
            photos.addAll(entries)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        cameraGranted = permissions[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        locationGranted = hasGpsPermission(context)
        status = when {
            !cameraGranted -> "Camera permission is needed to take photos."
            !locationGranted -> "Camera is ready. Allow location to attach real GPS coordinates."
            else -> "Camera and real GPS are ready."
        }
    }

    LaunchedEffect(Unit) {
        refreshPhotos()
        if (!cameraGranted || !locationGranted) {
            status = "Grant camera and location access to use GPS Camera."
        }
    }

    DisposableEffect(locationGranted) {
        if (!locationGranted) {
            currentLocation = null
            onDispose { }
        } else {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val fix = result.lastLocation
                    if (fix != null && LocationCompat.isMock(fix)) {
                        currentLocation = null
                        status = "Mock location detected. GPS metadata is disabled until a real location fix is available."
                    } else {
                        currentLocation = fix
                    }
                }
            }
            try {
                client.requestLocationUpdates(
                    LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
                        .setMinUpdateIntervalMillis(2_500L)
                        .build(),
                    callback,
                    Looper.getMainLooper()
                )
            } catch (_: SecurityException) {
                locationGranted = false
                status = "Location permission was revoked. Allow it to attach GPS data."
            }
            onDispose { client.removeLocationUpdates(callback) }
        }
    }

    DisposableEffect(cameraGranted, cameraFacing, lifecycleOwner) {
        if (!cameraGranted) {
            capture = null
            camera = null
            onDispose { }
        } else {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            val mainExecutor = ContextCompat.getMainExecutor(context)
            var disposed = false
            val listener = Runnable {
                if (disposed) return@Runnable
                try {
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    provider.unbindAll()
                    camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.Builder().requireLensFacing(cameraFacing).build(),
                        preview,
                        imageCapture
                    )
                    capture = imageCapture
                    status = if (locationGranted) "Camera and real GPS are ready." else
                        "Camera ready. Photos will not include GPS until location is allowed."
                } catch (error: Exception) {
                    status = "Could not start camera: ${error.localizedMessage ?: "camera unavailable"}"
                }
            }
            providerFuture.addListener(listener, mainExecutor)
            onDispose {
                disposed = true
                if (providerFuture.isDone) {
                    runCatching { providerFuture.get().unbindAll() }
                }
            }
        }
    }

    fun capturePhoto() {
        val imageCapture = capture
        if (imageCapture == null) {
            status = "Camera is still starting. Try again in a moment."
            return
        }
        val directory = photoDirectory(context)
        if (!directory.exists() && !directory.mkdirs()) {
            status = "Could not create the private GPS Camera folder."
            return
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val photoFile = File(directory, "GPS_$timestamp.jpg")
        val options = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        status = "Capturing photo…"
        imageCapture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val location = currentLocation
                    try {
                        ExifInterface(photoFile.absolutePath).apply {
                            if (location != null) setGpsInfo(location)
                            setAttribute(
                                ExifInterface.TAG_DATETIME_ORIGINAL,
                                SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date())
                            )
                            if (caption.isNotBlank()) {
                                setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, caption.trim())
                            }
                            saveAttributes()
                        }
                        status = if (location != null) {
                            "Photo saved with real GPS coordinates."
                        } else {
                            "Photo saved without GPS. Allow location or wait for a fix to add coordinates."
                        }
                        refreshPhotos()
                    } catch (error: Exception) {
                        status = "Photo saved, but metadata could not be written: ${error.localizedMessage ?: "unknown error"}"
                        refreshPhotos()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    status = "Photo capture failed: ${exception.localizedMessage ?: "camera error"}"
                    photoFile.delete()
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showGallery) "GPS Photos" else "GPS Camera") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showGallery = !showGallery
                        if (showGallery) refreshPhotos()
                    }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Open photo gallery")
                    }
                }
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        if (showGallery) {
            if (photos.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No GPS Camera photos yet", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("Photos are saved in this app's private GlitchGPSCamera folder.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(photos, key = { it.file.absolutePath }) { photo ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = photo.file,
                                    contentDescription = "Captured photo",
                                    modifier = Modifier.size(78.dp).clip(RoundedCornerShape(14.dp))
                                )
                                Column(
                                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(photo.file.name, color = TextPrimary, maxLines = 1)
                                    Text(
                                        photo.location?.let { "GPS $it" } ?: "No GPS metadata",
                                        color = if (photo.location == null) TextSecondary else CyberCyan,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
                                            .format(Date(photo.file.lastModified())),
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (cameraGranted) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    ) {
                        AndroidView(
                            factory = { previewView },
                            modifier = Modifier.fillMaxSize()
                        )
                        Card(
                            modifier = Modifier.align(Alignment.TopCenter).padding(12.dp).fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.62f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    currentLocation?.let { "GPS  ${"%.6f".format(Locale.US, it.latitude)}, ${"%.6f".format(Locale.US, it.longitude)}" }
                                        ?: "Waiting for a real GPS fix…",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                currentLocation?.let {
                                    Text(
                                        "Accuracy ±${it.accuracy.toInt()} m",
                                        color = Color.White.copy(alpha = 0.8f),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                if (caption.isNotBlank()) {
                                    Text(caption, color = Color.White, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Photo note (optional)") },
                        placeholder = { Text("Add a short caption") },
                        singleLine = true,
                        supportingText = { Text("Saved in photo metadata; captured GPS coordinates stay unchanged.") }
                    )
                    Text(status, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    if (!locationGranted) {
                        TextButton(onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        }) {
                            Text("Enable location for real GPS metadata")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                cameraFacing = if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                            }
                        ) {
                            Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Switch camera", tint = CyberCyan)
                        }
                        IconButton(
                            enabled = camera?.cameraInfo?.hasFlashUnit() == true,
                            onClick = {
                            camera?.cameraControl?.enableTorch(camera?.cameraInfo?.torchState?.value != 1)
                            }
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = "Toggle flash", tint = CyberCyan)
                        }
                        Button(
                            onClick = ::capturePhoto,
                            enabled = capture != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Take photo")
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Real GPS Camera", color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Camera access takes photos. Location access adds the device's actual coordinates to photo metadata. No mock or sample coordinates are used.",
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        }) {
                            Text("Allow camera and location")
                        }
                        Text(status, color = TextSecondary, modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }
        }
    }
}

private fun hasGpsPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

private fun photoDirectory(context: android.content.Context): File =
    File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir, "GlitchGPSCamera")
