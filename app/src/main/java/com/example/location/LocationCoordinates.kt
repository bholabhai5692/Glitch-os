package com.example.location

data class LocationCoordinates(val latitude: Double, val longitude: Double) {
    companion object {
        fun parse(latitude: String, longitude: String): LocationCoordinates? {
            val lat = latitude.trim().toDoubleOrNull() ?: return null
            val lon = longitude.trim().toDoubleOrNull() ?: return null
            if (!lat.isFinite() || !lon.isFinite()) return null
            if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
            return LocationCoordinates(lat, lon)
        }
    }
}
