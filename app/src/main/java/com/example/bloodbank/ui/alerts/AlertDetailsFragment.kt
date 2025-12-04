package com.example.bloodbank.ui.alerts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.bloodbank.databinding.FragmentAlertDetailsBinding
import com.example.bloodbank.data.entity.BloodAlert
import com.example.bloodbank.data.entity.AlertPriority
import com.example.bloodbank.data.repository.AlertRepository
import com.example.bloodbank.data.repository.DonorRepository
import com.example.bloodbank.ai.BloodDemandPredictor
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AlertDetailsFragment : Fragment() {
    
    private var _binding: FragmentAlertDetailsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var alertRepository: AlertRepository
    private lateinit var donorRepository: DonorRepository
    private lateinit var bloodDemandPredictor: BloodDemandPredictor
    
    companion object {
        private const val ARG_ALERT_ID = "alert_id"
        
        fun newInstance(alertId: Long): AlertDetailsFragment {
            val fragment = AlertDetailsFragment()
            val args = Bundle()
            args.putLong(ARG_ALERT_ID, alertId)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        alertRepository = AlertRepository(requireContext())
        donorRepository = DonorRepository(requireContext())
        bloodDemandPredictor = BloodDemandPredictor()
        
        val alertId = arguments?.getLong(ARG_ALERT_ID) ?: -1
        if (alertId != -1L) {
            loadAlertDetails(alertId)
        }
        
        setupClickListeners()
    }
    
    private fun loadAlertDetails(alertId: Long) {
        lifecycleScope.launch {
            val alerts = alertRepository.getActiveAlerts()
            val alert = alerts.find { it.id == alertId }
            
            if (alert != null) {
                displayAlertDetails(alert)
                loadAIInsights(alert)
            } else {
                Snackbar.make(binding.root, "Alert not found", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun displayAlertDetails(alert: BloodAlert) {
        binding.apply {
            tvAlertTitle.text = alert.title
            tvAlertMessage.text = alert.message
            tvBloodGroup.text = alert.bloodGroup
            tvPriority.text = alert.priority.displayName
            tvType.text = alert.type.displayName
            
            // Format timestamp
            val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
            tvTimestamp.text = dateFormat.format(Date(alert.timestamp))
            
            // Set priority color
            val priorityColor = when (alert.priority) {
                AlertPriority.EMERGENCY -> android.graphics.Color.parseColor("#D32F2F")
                AlertPriority.CRITICAL -> android.graphics.Color.parseColor("#F57C00")
                AlertPriority.HIGH -> android.graphics.Color.parseColor("#FF9800")
                AlertPriority.MEDIUM -> android.graphics.Color.parseColor("#2196F3")
                AlertPriority.LOW -> android.graphics.Color.parseColor("#4CAF50")
            }
            viewPriorityIndicator.setBackgroundColor(priorityColor)
            
            // Show optional fields
            alert.location?.let {
                tvLocation.text = "📍 $it"
                tvLocation.visibility = View.VISIBLE
            } ?: run {
                tvLocation.visibility = View.GONE
            }
            
            if (alert.requiredUnits > 1) {
                tvRequiredUnits.text = "Required Units: ${alert.requiredUnits}"
                tvRequiredUnits.visibility = View.VISIBLE
            } else {
                tvRequiredUnits.visibility = View.GONE
            }
            
            alert.contactInfo?.let {
                tvContactInfo.text = "Contact: $it"
                tvContactInfo.visibility = View.VISIBLE
            } ?: run {
                tvContactInfo.visibility = View.GONE
            }
            
            alert.expiryTime?.let {
                val expiryDate = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                tvExpiryTime.text = "Expires: ${expiryDate.format(Date(it))}"
                tvExpiryTime.visibility = View.VISIBLE
            } ?: run {
                tvExpiryTime.visibility = View.GONE
            }
        }
    }
    
    private fun loadAIInsights(alert: BloodAlert) {
        lifecycleScope.launch {
            try {
                val donors = donorRepository.getAllLocal()
                val analysis = bloodDemandPredictor.analyzeBloodInventory(donors)
                
                binding.apply {
                    // Show AI insights section
                    layoutAiInsights.visibility = View.VISIBLE
                    
                    // Display total donors
                    tvTotalDonors.text = "Total Donors: ${analysis.totalDonors}"
                    
                    // Display critical shortages
                    if (analysis.criticalShortages.isNotEmpty()) {
                        tvCriticalShortages.text = "Critical Shortages: ${analysis.criticalShortages.joinToString(", ")}"
                        tvCriticalShortages.visibility = View.VISIBLE
                    } else {
                        tvCriticalShortages.visibility = View.GONE
                    }
                    
                    // Display recommendations
                    val recommendationsText = analysis.recommendedActions.joinToString("\n") { "• $it" }
                    tvRecommendations.text = recommendationsText
                    
                    // Show specific blood group prediction if relevant
                    if (alert.bloodGroup != "All") {
                        analysis.predictions[alert.bloodGroup]?.let { prediction ->
                            val predictionText = buildString {
                                append("${alert.bloodGroup} Analysis:\n")
                                append("Current: ${prediction.currentCount} donors (${String.format("%.1f", prediction.currentPercent)}%)\n")
                                append("Expected: ${String.format("%.1f", prediction.expectedPercent)}%\n")
                                if (prediction.shortage > 0) {
                                    append("Shortage: ${String.format("%.1f", prediction.shortage)}%")
                                }
                            }
                            tvBloodGroupPrediction.text = predictionText
                            tvBloodGroupPrediction.visibility = View.VISIBLE
                        }
                    } else {
                        tvBloodGroupPrediction.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                binding.layoutAiInsights.visibility = View.GONE
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnMarkAsRead.setOnClickListener {
            val alertId = arguments?.getLong(ARG_ALERT_ID) ?: -1
            if (alertId != -1L) {
                lifecycleScope.launch {
                    alertRepository.markAsRead(alertId)
                    Snackbar.make(binding.root, "Alert marked as read", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        
        binding.btnDismiss.setOnClickListener {
            val alertId = arguments?.getLong(ARG_ALERT_ID) ?: -1
            if (alertId != -1L) {
                lifecycleScope.launch {
                    alertRepository.markAsInactive(alertId)
                    Snackbar.make(binding.root, "Alert dismissed", Snackbar.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
