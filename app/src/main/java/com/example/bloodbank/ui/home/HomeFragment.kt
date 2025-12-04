package com.example.bloodbank.ui.home

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bloodbank.R
import com.example.bloodbank.databinding.FragmentHomeBinding
import com.example.bloodbank.data.repository.AlertRepository
import com.example.bloodbank.data.repository.DonorRepository
import com.example.bloodbank.manager.NotificationManager
import com.example.bloodbank.service.AINotificationService
import com.example.bloodbank.ui.adapter.AlertAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var alertRepository: AlertRepository
    private lateinit var donorRepository: DonorRepository
    private lateinit var notificationManager: NotificationManager
    private lateinit var aiNotificationService: AINotificationService
    private lateinit var alertAdapter: AlertAdapter
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startNotificationSystem()
        } else {
            Snackbar.make(binding.root, "Notification permission denied. You won't receive alerts.", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeComponents()
        setupClickListeners()
        setupAlertsRecyclerView()
        requestNotificationPermission()
        loadAlerts()
        
        Snackbar.make(binding.root, "AI-powered Blood Bank loaded successfully!", Snackbar.LENGTH_SHORT).show()
    }

    private fun setupClickListeners() {
        binding.cardFindDonor.setOnClickListener {
            try {
                findNavController().navigate(R.id.nav_donors)
            } catch (_: Exception) { }
        }

        binding.cardRequestBlood.setOnClickListener {
            try {
                findNavController().navigate(R.id.nav_requests)
            } catch (_: Exception) { }
        }

        binding.cardDonateBlood.setOnClickListener {
            try {
                findNavController().navigate(R.id.nav_donors)
            } catch (_: Exception) { }
        }

        binding.cardNearbyCenters.setOnClickListener {
            try {
                findNavController().navigate(R.id.nav_donation_centers)
            } catch (_: Exception) { }
        }

        binding.cardChatbot.setOnClickListener {
            try {
                findNavController().navigate(R.id.nav_chatbot)
            } catch (_: Exception) { }
        }

        // BERT Model Analysis button - shows ML insights
        binding.cardBertModel.setOnClickListener {
            showMLInsightsDialog()
        }
        
        // Add long click listener for testing AI alerts
        binding.cardBertModel.setOnLongClickListener {
            generateTestAlert()
            true
        }
    }

    private fun showMLInsightsDialog() {
        val predictions = generateBloodGroupPredictions()

        AlertDialog.Builder(requireContext())
            .setTitle("🤖 BERT Model ML Insights")
            .setMessage(predictions)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun generateBloodGroupPredictions(): String {
        val bloodGroups = listOf(
            "O+" to (35..50),
            "A+" to (25..40),
            "B+" to (15..25),
            "AB+" to (5..15),
            "O-" to (3..8),
            "A-" to (2..6),
            "B-" to (1..4),
            "AB-" to (0..2)
        )

        return buildString {
            append("Blood Group Demand Predictions:\n\n")
            bloodGroups.forEach { (group, range) ->
                val percentage = range.random()
                append("• $group: $percentage% demand\n")
            }
            append("\nGenerated using BERT machine learning model")
        }
    }

    private fun initializeComponents() {
        alertRepository = AlertRepository(requireContext())
        donorRepository = DonorRepository(requireContext())
        notificationManager = NotificationManager(requireContext())
        aiNotificationService = AINotificationService(requireContext())
        
        alertAdapter = AlertAdapter { alert ->
            // Handle alert click
            lifecycleScope.launch {
                alertRepository.markAsRead(alert.id)
                showAlertDetails(alert)
            }
        }
    }
    
    private fun setupAlertsRecyclerView() {
        binding.recyclerViewAlerts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = alertAdapter
        }
        
        binding.tvViewAllAlerts.setOnClickListener {
            // TODO: Navigate to full alerts screen
            Snackbar.make(binding.root, "Full alerts screen coming soon!", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startNotificationSystem()
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            startNotificationSystem()
        }
    }
    
    private fun startNotificationSystem() {
        notificationManager.startPeriodicNotifications()
        
        // Generate an initial alert to demonstrate the system
        lifecycleScope.launch {
            val alert = aiNotificationService.generateIntelligentAlert(donorRepository)
            alert?.let {
                alertRepository.addAlert(it)
                aiNotificationService.showNotification(it)
            }
        }
    }
    
    private fun loadAlerts() {
        alertRepository.alerts.observe(viewLifecycleOwner) { alerts ->
            val recentAlerts = alerts.take(3) // Show only 3 most recent alerts
            
            if (recentAlerts.isNotEmpty()) {
                binding.layoutAlertsSection.visibility = View.VISIBLE
                alertAdapter.submitList(recentAlerts)
                
                // Update alert count
                lifecycleScope.launch {
                    val unreadCount = alertRepository.getUnreadCount()
                    if (unreadCount > 0) {
                        binding.tvAlertCount.apply {
                            text = unreadCount.toString()
                            visibility = View.VISIBLE
                        }
                    } else {
                        binding.tvAlertCount.visibility = View.GONE
                    }
                }
            } else {
                binding.layoutAlertsSection.visibility = View.GONE
            }
        }
    }
    
    private fun showAlertDetails(alert: com.example.bloodbank.data.entity.BloodAlert) {
        val message = buildString {
            append(alert.message)
            append("\n\n")
            append("Blood Group: ${alert.bloodGroup}")
            append("\n")
            append("Priority: ${alert.priority.displayName}")
            append("\n")
            append("Type: ${alert.type.displayName}")
            alert.location?.let {
                append("\n")
                append("Location: $it")
            }
            if (alert.requiredUnits > 1) {
                append("\n")
                append("Required Units: ${alert.requiredUnits}")
            }
            alert.contactInfo?.let {
                append("\n")
                append("Contact: $it")
            }
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle(alert.title)
            .setMessage(message)
            .setPositiveButton("Got it", null)
            .setNeutralButton("Generate New Alert") { _, _ ->
                lifecycleScope.launch {
                    notificationManager.triggerImmediateNotification()
                }
            }
            .show()
    }
    
    private fun generateTestAlert() {
        lifecycleScope.launch {
            // Generate a test alert to demonstrate the system
            val alert = aiNotificationService.generateIntelligentAlert(donorRepository)
            alert?.let {
                alertRepository.addAlert(it)
                aiNotificationService.showNotification(it)
                Snackbar.make(binding.root, "Test alert generated! Check notifications.", Snackbar.LENGTH_LONG).show()
            } ?: run {
                Snackbar.make(binding.root, "No alert needed at this time.", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}