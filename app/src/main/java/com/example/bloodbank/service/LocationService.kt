package com.example.bloodbank.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationService(private val context: Context) {
    
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    companion object {
        private const val MIN_TIME_BETWEEN_UPDATES = 1000L // 1 second
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES = 1f // 1 meter
    }
    
    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val timestamp: Long
    )
    
    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationData = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            continuation.resumeWithException(SecurityException("Location permission not granted"))
            return@suspendCancellableCoroutine
        }
        
        if (!isLocationEnabled()) {
            continuation.resumeWithException(IllegalStateException("Location services are disabled"))
            return@suspendCancellableCoroutine
        }
        
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
                val locationData = LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    timestamp = location.time
                )
                continuation.resume(locationData)
            }
            
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        
        continuation.invokeOnCancellation {
            locationManager.removeUpdates(locationListener)
        }
        
        try {
            // Try GPS first
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BETWEEN_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    locationListener
                )
            }
            // Fallback to network provider
            else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BETWEEN_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    locationListener
                )
            } else {
                continuation.resumeWithException(IllegalStateException("No location providers available"))
            }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
    
    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(): LocationData? {
        if (!hasLocationPermission()) return null
        
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        var bestLocation: Location? = null
        
        for (provider in providers) {
            try {
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null && (bestLocation == null || location.accuracy < bestLocation.accuracy)) {
                    bestLocation = location
                }
            } catch (e: Exception) {
                // Continue to next provider
            }
        }
        
        return bestLocation?.let {
            LocationData(
                latitude = it.latitude,
                longitude = it.longitude,
                accuracy = it.accuracy,
                timestamp = it.time
            )
        }
    }
    
    fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
    
    fun findNearbyDonors(
        userLocation: LocationData,
        donors: List<com.example.bloodbank.data.entity.Donor>,
        radiusInMeters: Float = 5000f // 5km default
    ): List<Pair<com.example.bloodbank.data.entity.Donor, Float>> {
        return donors.mapNotNull { donor ->
            if (donor.latitude != 0.0 && donor.longitude != 0.0) {
                val distance = calculateDistance(
                    userLocation.latitude, userLocation.longitude,
                    donor.latitude, donor.longitude
                )
                if (distance <= radiusInMeters) {
                    Pair(donor, distance)
                } else null
            } else null
        }.sortedBy { it.second } // Sort by distance
    }
    
    fun getLocationString(latitude: Double, longitude: Double): String {
        return "Lat: ${String.format("%.6f", latitude)}, Lng: ${String.format("%.6f", longitude)}"
    }
    
    fun isLocationAccurate(location: LocationData, minAccuracy: Float = 100f): Boolean {
        return location.accuracy <= minAccuracy
    }
}
