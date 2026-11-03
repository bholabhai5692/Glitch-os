package com.example.location

import android.content.Context
import android.app.AppOpsManager
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import android.os.Process
import android.util.Log

class MockLocationController(context: Context) {
    private val appContext = context.applicationContext
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var providerInstalled = false
    var isRunning: Boolean = false
        private set

    fun isSelectedAsMockApp(): Boolean {
        return try {
            val appOpsManager = appContext.getSystemService(AppOpsManager::class.java)
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_MOCK_LOCATION,
                Process.myUid(),
                appContext.packageName
            ) == AppOpsManager.MODE_ALLOWED
        } catch (exception: SecurityException) {
            Log.e("MockLocationController", "Could not check mock-location app permission.", exception)
            false
        }
    }

    fun start(coordinates: LocationCoordinates) {
        if (!providerInstalled) {
            locationManager.addTestProvider(
                LocationManager.GPS_PROVIDER,
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                Criteria.POWER_LOW,
                Criteria.ACCURACY_FINE
            )
            providerInstalled = true
        }

        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
        update(coordinates)
        isRunning = true
    }

    fun update(coordinates: LocationCoordinates) {
        if (!providerInstalled) {
            throw IllegalStateException("Start the mock location provider before updating it.")
        }

        val location = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = coordinates.latitude
            longitude = coordinates.longitude
            accuracy = 5f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, location)
    }

    fun stop() {
        if (!providerInstalled) return

        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false)
        locationManager.removeTestProvider(LocationManager.GPS_PROVIDER)
        providerInstalled = false
        isRunning = false
    }
}
