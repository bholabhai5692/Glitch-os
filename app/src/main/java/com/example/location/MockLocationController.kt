package com.example.location

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock

class MockLocationController(context: Context) {
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var providerInstalled = false
    var isRunning: Boolean = false
        private set

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
