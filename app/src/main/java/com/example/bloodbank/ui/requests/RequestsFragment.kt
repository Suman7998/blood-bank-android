package com.example.bloodbank.ui.requests

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bloodbank.databinding.FragmentRequestsBinding
import com.example.bloodbank.data.SampleDataGenerator
import com.example.bloodbank.data.entity.Donor
import com.example.bloodbank.ui.adapter.DonorAdapter
import com.example.bloodbank.util.MultimediaManager
import com.example.bloodbank.service.LocationService
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class RequestsFragment : Fragment(), TextToSpeech.OnInitListener {
    
    private var _binding: FragmentRequestsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var multimediaManager: MultimediaManager
    private lateinit var donorAdapter: DonorAdapter
    private lateinit var locationService: LocationService
    private var donors: List<Donor> = emptyList()
    private var allDonors: List<Donor> = emptyList()
    private var currentLocation: LocationService.LocationData? = null
    
    private var textToSpeech: TextToSpeech? = null
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentAudioFile: File? = null
    private var isRecording = false
    private var isPlaying = false
    
    // Camera and file handling
    private var imageCapture: ImageCapture? = null
    private var currentPhotoFile: File? = null
    
    // Activity result launchers
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoFile?.let { file ->
                lifecycleScope.launch {
                    try {
                        val compressedFile = multimediaManager.compressImage(file)
                        val thumbnailFile = multimediaManager.createThumbnail(compressedFile)
                        
                        // Upload to Firebase Storage
                        val imageUrl = multimediaManager.uploadFile(
                            compressedFile, 
                            "donor_images/${System.currentTimeMillis()}.jpg"
                        )
                        
                        Toast.makeText(context, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            initializeMultimedia()
        } else {
            Toast.makeText(context, "Permissions required for full functionality", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        multimediaManager = MultimediaManager(requireContext())
        locationService = LocationService(requireContext())
        
        setupUI()
        checkPermissions()
        loadSampleData()
        setupVoicePrompts()
        getCurrentLocation()
    }
    
    private fun setupUI() {
        // Setup RecyclerView for donors
        donorAdapter = DonorAdapter(
            onCallClick = { donor -> makePhoneCall(donor.phone) },
            onImageClick = { donor -> showDonorImages(donor) },
            onVoiceClick = { donor -> playVoiceNote(donor) },
            onVideoClick = { donor -> playVideoNote(donor) }
        )
        
        binding.recyclerViewDonors.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = donorAdapter
        }
        
        // Setup multimedia buttons
        binding.btnCapturePhoto.setOnClickListener { capturePhoto() }
        binding.btnRecordAudio.setOnClickListener { toggleAudioRecording() }
        binding.btnRecordVideo.setOnClickListener { recordVideo() }
        binding.btnPlayVoicePrompt.setOnClickListener { playRandomVoicePrompt() }
        binding.btnUploadDocument.setOnClickListener { uploadDocument() }
        
        // Setup location button
        binding.btnFindNearby.setOnClickListener { findNearbyDonors() }
        
        // Setup blood group filter
        binding.spinnerBloodGroup.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterDonorsByBloodGroup(position)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
    }
    
    private fun checkPermissions() {
        val requiredPermissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.CAMERA)
        }
        
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.RECORD_AUDIO)
        }
        
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) 
            != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.CALL_PHONE)
        }
        
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) 
                != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        
        if (requiredPermissions.isNotEmpty()) {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        } else {
            initializeMultimedia()
        }
    }
    
    private fun initializeMultimedia() {
        // Initialize Text-to-Speech
        textToSpeech = TextToSpeech(requireContext(), this)
        
        // Initialize multimedia manager
        multimediaManager.initializeTextToSpeech { success ->
            if (success) {
                binding.btnPlayVoicePrompt.isEnabled = true
            }
        }
    }
    
    private fun loadSampleData() {
        allDonors = SampleDataGenerator.generateSampleDonors(120)
        donors = allDonors
        donorAdapter.updateDonors(donors)
        
        // Update UI with statistics
        binding.tvTotalDonors.text = "Total Donors: ${donors.size}"
        binding.tvAvailableDonors.text = "Available: ${donors.count { it.isAvailable }}"
    }
    
    private fun setupVoicePrompts() {
        val voicePrompts = SampleDataGenerator.getVoicePrompts()
        
        // Play welcome message
        lifecycleScope.launch {
            kotlinx.coroutines.delay(1000)
            multimediaManager.speakText("Welcome to Blood Request Center. ${donors.size} donors available.")
        }
    }
    
    private fun capturePhoto() {
        currentPhotoFile = multimediaManager.createImageFile()
        
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, multimediaManager.getFileUri(currentPhotoFile!!))
        }
        
        cameraLauncher.launch(intent)
    }
    
    private fun toggleAudioRecording() {
        if (isRecording) {
            stopAudioRecording()
        } else {
            startAudioRecording()
        }
    }
    
    private fun startAudioRecording() {
        currentAudioFile = multimediaManager.createAudioFile()
        
        if (multimediaManager.startAudioRecording(currentAudioFile!!)) {
            isRecording = true
            binding.btnRecordAudio.text = "Stop Recording"
            binding.btnRecordAudio.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
            
            multimediaManager.speakText("Recording started")
        } else {
            Toast.makeText(context, "Failed to start recording", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun stopAudioRecording() {
        multimediaManager.stopAudioRecording()
        isRecording = false
        binding.btnRecordAudio.text = "Record Audio"
        binding.btnRecordAudio.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light))
        
        multimediaManager.speakText("Recording stopped")
        
        // Upload audio file
        currentAudioFile?.let { file ->
            lifecycleScope.launch {
                try {
                    val audioUrl = multimediaManager.uploadFile(
                        file, 
                        "audio_notes/${System.currentTimeMillis()}.3gp"
                    )
                    Toast.makeText(context, "Audio uploaded successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Audio upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun recordVideo() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30) // 30 seconds limit
            putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1) // High quality
        }
        
        startActivity(intent)
    }
    
    private fun playRandomVoicePrompt() {
        val prompts = SampleDataGenerator.getVoicePrompts()
        val randomPrompt = prompts.random()
        multimediaManager.speakText(randomPrompt)
        
        Toast.makeText(context, "Playing voice prompt", Toast.LENGTH_SHORT).show()
    }
    
    private fun uploadDocument() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        
        startActivity(Intent.createChooser(intent, "Select Document"))
    }
    
    private fun makePhoneCall(phoneNumber: String) {
        multimediaManager.makePhoneCall(requireActivity(), phoneNumber)
    }
    
    private fun showDonorImages(donor: Donor) {
        // Show donor profile image, ID document, and medical reports
        Toast.makeText(context, "Showing images for ${donor.name}", Toast.LENGTH_SHORT).show()
        
        // You can implement an image viewer dialog here
        multimediaManager.speakText("Showing profile images for ${donor.name}")
    }
    
    private fun playVoiceNote(donor: Donor) {
        donor.voiceNoteUrl?.let { url ->
            // Play voice note (you can implement actual audio streaming here)
            multimediaManager.speakText("Playing voice note for ${donor.name}")
            Toast.makeText(context, "Playing voice note", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(context, "No voice note available", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun playVideoNote(donor: Donor) {
        donor.videoNoteUrl?.let { url ->
            // Play video note (you can implement video player here)
            multimediaManager.speakText("Playing video note for ${donor.name}")
            Toast.makeText(context, "Playing video note", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(context, "No video note available", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun filterDonorsByBloodGroup(position: Int) {
        val bloodGroups = listOf("All", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        
        val filteredDonors = if (position == 0) {
            allDonors
        } else {
            val selectedBloodGroup = bloodGroups[position]
            allDonors.filter { it.bloodGroup == selectedBloodGroup }
        }
        
        donors = filteredDonors
        donorAdapter.updateDonors(donors)
        binding.tvAvailableDonors.text = "Available: ${donors.count { it.isAvailable }}"
        
        multimediaManager.speakText("Showing ${donors.size} donors")
    }
    
    private fun getCurrentLocation() {
        if (!locationService.hasLocationPermission()) {
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!locationService.isLocationEnabled()) {
            Toast.makeText(context, "Please enable location services", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                currentLocation = locationService.getCurrentLocation()
                currentLocation?.let { location ->
                    binding.tvCurrentLocation.text = "📍 Current: ${locationService.getLocationString(location.latitude, location.longitude)}"
                    multimediaManager.speakText("Location acquired successfully")
                }
            } catch (e: Exception) {
                // Try to get last known location as fallback
                val lastLocation = locationService.getLastKnownLocation()
                if (lastLocation != null) {
                    currentLocation = lastLocation
                    binding.tvCurrentLocation.text = "📍 Last Known: ${locationService.getLocationString(lastLocation.latitude, lastLocation.longitude)}"
                } else {
                    binding.tvCurrentLocation.text = "📍 Location unavailable"
                    Toast.makeText(context, "Unable to get location: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun findNearbyDonors() {
        currentLocation?.let { location ->
            val nearbyDonors = locationService.findNearbyDonors(location, allDonors, 10000f) // 10km radius
            
            if (nearbyDonors.isNotEmpty()) {
                donors = nearbyDonors.map { it.first }
                donorAdapter.updateDonors(donors)
                binding.tvAvailableDonors.text = "Nearby: ${donors.count { it.isAvailable }}"
                
                multimediaManager.speakText("Found ${nearbyDonors.size} donors within 10 kilometers")
                Toast.makeText(context, "Found ${nearbyDonors.size} nearby donors", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "No donors found within 10km", Toast.LENGTH_SHORT).show()
                multimediaManager.speakText("No nearby donors found")
            }
        } ?: run {
            Toast.makeText(context, "Location not available. Please wait for GPS", Toast.LENGTH_SHORT).show()
            getCurrentLocation()
        }
    }
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.getDefault()
            binding.btnPlayVoicePrompt.isEnabled = true
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        
        // Clean up multimedia resources
        multimediaManager.cleanup()
        textToSpeech?.shutdown()
        mediaPlayer?.release()
        mediaRecorder?.release()
        
        _binding = null
    }
}
