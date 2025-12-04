package com.example.bloodbank.ui.centers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.bloodbank.databinding.FragmentDonationCentersBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class DonationCentersFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentDonationCentersBinding? = null
    private val binding get() = _binding!!
    private var googleMap: GoogleMap? = null
    private val fusedLocationProvider by lazy { LocationServices.getFusedLocationProviderClient(requireContext()) }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                    || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
            if (granted) {
                enableMyLocationAndLoadNearby()
            } else {
                // Load all default markers without filtering if permission denied
                addMarkersAndMoveCamera(null)
                Snackbar.make(binding.root, "Location permission denied", Snackbar.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDonationCentersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(com.example.bloodbank.R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        // Load markers on camera idle as well
        map.setOnCameraIdleListener {
            val target = map.cameraPosition?.target
            if (target != null) {
                loadNearbyHospitals(target)
            }
        }
        // Always start focused on Mumbai
        val mumbai = LatLng(19.0760, 72.8777)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(mumbai, 12f))
        // Trigger initial load for Mumbai
        loadNearbyHospitals(mumbai)
    }

    private fun checkLocationPermissionAndProceed() {
        val fine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            enableMyLocationAndLoadNearby()
        } else {
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationAndLoadNearby() {
        val map = googleMap ?: return
        map.isMyLocationEnabled = true

        fusedLocationProvider.lastLocation
            .addOnSuccessListener { location ->
                addMarkersAndMoveCamera(location)
            }
            .addOnFailureListener {
                addMarkersAndMoveCamera(null)
            }
    }

    private fun addMarkersAndMoveCamera(current: Location?) {
        val map = googleMap ?: return

        // If we have current location, move camera there and trigger dynamic load via camera idle listener
        // Otherwise, show India bounds fallback and allow user to pan to load markers

        when {
            current != null -> {
                val here = LatLng(current.latitude, current.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(here, 15f))
                // Nearby markers will load via camera idle callback
            }
            else -> {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(20.5937, 78.9629), 4.5f)) // India
            }
        }
    }

    // Simple debounce job to avoid flooding requests
    private var nearbyJob: Job? = null
    private val http by lazy { OkHttpClient() }

    private fun loadNearbyHospitals(center: LatLng) {
        val map = googleMap ?: return
        val key = getString(com.example.bloodbank.R.string.google_maps_key)
        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=${center.latitude},${center.longitude}&radius=50000&type=hospital&key=$key"

        nearbyJob?.cancel()
        nearbyJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val req = Request.Builder().url(url).build()
                val resp = http.newCall(req).execute()
                val body = resp.body?.string()
                if (!resp.isSuccessful || body.isNullOrEmpty()) return@launch
                val json = JSONObject(body)
                val results = json.optJSONArray("results") ?: return@launch

                val markers = mutableListOf<Triple<LatLng, String, String?>>()
                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val name = item.optString("name")
                    val vicinity = item.optString("vicinity")
                    val geometry = item.optJSONObject("geometry") ?: continue
                    val location = geometry.optJSONObject("location") ?: continue
                    val lat = location.optDouble("lat")
                    val lng = location.optDouble("lng")
                    if (!lat.isNaN() && !lng.isNaN()) {
                        markers.add(Triple(LatLng(lat, lng), name, vicinity))
                    }
                }

                withContext(Dispatchers.Main) {
                    map.clear()
                    if (markers.isEmpty()) {
                        // Fallback to static Mumbai hospitals if API returns none
                        addStaticMumbaiMarkers(map)
                    } else {
                        markers.forEach { (latLng, title, snippet) ->
                            map.addMarker(
                                MarkerOptions()
                                    .position(latLng)
                                    .title(title)
                                    .snippet(snippet ?: "")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // On error: ensure user still sees Mumbai hospitals
                withContext(Dispatchers.Main) {
                    val mapNow = googleMap ?: return@withContext
                    mapNow.clear()
                    addStaticMumbaiMarkers(mapNow)
                }
            }
        }
    }

    private fun addStaticMumbaiMarkers(map: GoogleMap) {
        val staticMumbai = listOf(
            Triple(LatLng(19.07283, 72.88261), "KEM Hospital", "Parel, Mumbai"),
            Triple(LatLng(19.0968, 72.8517), "Cooper Hospital", "JVPD Scheme, Mumbai"),
            Triple(LatLng(19.2183, 72.9781), "Fortis Hospital Mulund", "Mulund, Mumbai"),
            Triple(LatLng(19.0669, 72.8355), "Lilavati Hospital", "Bandra West, Mumbai"),
            Triple(LatLng(19.1180, 72.8465), "Nanavati Max Hospital", "Vile Parle West, Mumbai"),
            Triple(LatLng(18.9930, 72.8170), "Jaslok Hospital", "Pedder Rd, Mumbai"),
            Triple(LatLng(19.2270, 72.8567), "SevenHills Hospital", "Marol, Andheri East"),
            Triple(LatLng(19.0622, 72.8249), "Bhabha Hospital", "Bandra West, Mumbai")
        )
        staticMumbai.forEach { (latLng, title, snippet) ->
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(title)
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
