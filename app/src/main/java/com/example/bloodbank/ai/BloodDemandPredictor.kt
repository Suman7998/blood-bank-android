package com.example.bloodbank.ai

import com.example.bloodbank.data.entity.Donor
import com.example.bloodbank.data.entity.BloodAlert
import com.example.bloodbank.data.entity.AlertPriority
import com.example.bloodbank.data.entity.AlertType
import java.util.*
import kotlin.math.*
import kotlin.random.Random

class BloodDemandPredictor {
    
    companion object {
        // Real-world blood group distribution percentages
        private val GLOBAL_DISTRIBUTION = mapOf(
            "O+" to 38.0, "A+" to 34.0, "B+" to 9.0, "AB+" to 3.0,
            "O-" to 7.0, "A-" to 6.0, "B-" to 2.0, "AB-" to 1.0
        )
        
        // Seasonal demand factors (higher in winter, lower in summer)
        private val SEASONAL_FACTORS = mapOf(
            Calendar.JANUARY to 1.2,
            Calendar.FEBRUARY to 1.3,
            Calendar.MARCH to 1.1,
            Calendar.APRIL to 1.0,
            Calendar.MAY to 0.9,
            Calendar.JUNE to 0.8,
            Calendar.JULY to 0.8,
            Calendar.AUGUST to 0.9,
            Calendar.SEPTEMBER to 1.0,
            Calendar.OCTOBER to 1.1,
            Calendar.NOVEMBER to 1.2,
            Calendar.DECEMBER to 1.3
        )
    }
    
    fun analyzeBloodInventory(donors: List<Donor>): InventoryAnalysis {
        if (donors.isEmpty()) {
            return InventoryAnalysis(
                totalDonors = 0,
                criticalShortages = GLOBAL_DISTRIBUTION.keys.toList(),
                predictions = emptyMap(),
                recommendedActions = listOf("Add sample donors to get started with blood inventory management")
            )
        }
        
        val bloodGroupCounts = donors.groupBy { it.bloodGroup }.mapValues { it.value.size }
        val totalDonors = donors.size
        
        // Calculate current distribution
        val currentDistribution = bloodGroupCounts.mapValues { (_, count) ->
            (count.toDouble() / totalDonors) * 100
        }
        
        // Identify critical shortages
        val criticalShortages = mutableListOf<String>()
        val predictions = mutableMapOf<String, BloodGroupPrediction>()
        
        GLOBAL_DISTRIBUTION.forEach { (bloodGroup, expectedPercent) ->
            val currentPercent = currentDistribution[bloodGroup] ?: 0.0
            val shortage = expectedPercent - currentPercent
            val seasonalFactor = getCurrentSeasonalFactor()
            val adjustedDemand = expectedPercent * seasonalFactor
            
            if (currentPercent < expectedPercent * 0.5) { // Less than 50% of expected
                criticalShortages.add(bloodGroup)
            }
            
            predictions[bloodGroup] = BloodGroupPrediction(
                bloodGroup = bloodGroup,
                currentCount = bloodGroupCounts[bloodGroup] ?: 0,
                currentPercent = currentPercent,
                expectedPercent = expectedPercent,
                adjustedDemand = adjustedDemand,
                shortage = max(0.0, shortage),
                priority = calculatePriority(shortage, seasonalFactor)
            )
        }
        
        val recommendedActions = generateRecommendations(predictions, criticalShortages)
        
        return InventoryAnalysis(
            totalDonors = totalDonors,
            criticalShortages = criticalShortages,
            predictions = predictions,
            recommendedActions = recommendedActions
        )
    }
    
    fun generateSmartAlert(analysis: InventoryAnalysis): BloodAlert? {
        if (analysis.criticalShortages.isEmpty()) {
            return generatePositiveAlert(analysis)
        }
        
        val mostCritical = analysis.predictions.values
            .filter { it.bloodGroup in analysis.criticalShortages }
            .maxByOrNull { it.shortage }
            ?: return null
        
        return BloodAlert(
            title = "🚨 Critical Blood Shortage Detected",
            message = generateShortageMessage(mostCritical),
            bloodGroup = mostCritical.bloodGroup,
            priority = mostCritical.priority,
            type = AlertType.BLOOD_SHORTAGE,
            location = "All Centers",
            requiredUnits = calculateRequiredUnits(mostCritical),
            expiryTime = System.currentTimeMillis() + (48 * 60 * 60 * 1000) // 48 hours
        )
    }
    
    private fun generateShortageMessage(prediction: BloodGroupPrediction): String {
        val severity = when {
            prediction.shortage > 20 -> "CRITICAL"
            prediction.shortage > 10 -> "HIGH"
            else -> "MODERATE"
        }
        
        return buildString {
            append("$severity shortage of ${prediction.bloodGroup} blood detected!\n\n")
            append("Current: ${prediction.currentCount} donors (${String.format("%.1f", prediction.currentPercent)}%)\n")
            append("Expected: ${String.format("%.1f", prediction.expectedPercent)}%\n")
            append("Shortage: ${String.format("%.1f", prediction.shortage)}%\n\n")
            append("Immediate action required to maintain adequate blood supply.")
        }
    }
    
    private fun generatePositiveAlert(analysis: InventoryAnalysis): BloodAlert? {
        val randomTips = listOf(
            "Great job! Your blood inventory is well-balanced. Consider organizing a community awareness event.",
            "Excellent blood distribution! Share health tips with donors to maintain their eligibility.",
            "Well-maintained inventory! Consider setting up donation reminders for regular donors.",
            "Good work! Your AI system is keeping blood supplies optimal. Keep monitoring trends."
        )
        
        return if (Random.nextFloat() < 0.3f) { // 30% chance for positive feedback
            BloodAlert(
                title = "✅ Inventory Status: Excellent",
                message = randomTips.random(),
                bloodGroup = "All",
                priority = AlertPriority.LOW,
                type = AlertType.HEALTH_TIP
            )
        } else null
    }
    
    private fun calculateRequiredUnits(prediction: BloodGroupPrediction): Int {
        return max(1, (prediction.shortage * 0.5).roundToInt())
    }
    
    private fun calculatePriority(shortage: Double, seasonalFactor: Double): AlertPriority {
        val adjustedShortage = shortage * seasonalFactor
        
        return when {
            adjustedShortage > 25 -> AlertPriority.EMERGENCY
            adjustedShortage > 15 -> AlertPriority.CRITICAL
            adjustedShortage > 10 -> AlertPriority.HIGH
            adjustedShortage > 5 -> AlertPriority.MEDIUM
            else -> AlertPriority.LOW
        }
    }
    
    private fun getCurrentSeasonalFactor(): Double {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH)
        return SEASONAL_FACTORS[month] ?: 1.0
    }
    
    private fun generateRecommendations(
        predictions: Map<String, BloodGroupPrediction>,
        criticalShortages: List<String>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (criticalShortages.isNotEmpty()) {
            recommendations.add("🎯 Focus recruitment on ${criticalShortages.joinToString(", ")} donors")
            recommendations.add("📱 Send targeted notifications to ${criticalShortages.joinToString(", ")} blood group donors")
            recommendations.add("🏥 Contact nearby hospitals about ${criticalShortages.first()} blood availability")
        }
        
        val seasonalFactor = getCurrentSeasonalFactor()
        if (seasonalFactor > 1.1) {
            recommendations.add("❄️ Winter season: Increase donation drives due to higher demand")
        } else if (seasonalFactor < 0.9) {
            recommendations.add("☀️ Summer season: Maintain regular donation schedules")
        }
        
        val totalShortage = predictions.values.sumOf { it.shortage }
        if (totalShortage > 50) {
            recommendations.add("🚨 Consider emergency blood drive campaign")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("✅ Inventory levels are optimal. Continue regular monitoring.")
        }
        
        return recommendations
    }
}

data class InventoryAnalysis(
    val totalDonors: Int,
    val criticalShortages: List<String>,
    val predictions: Map<String, BloodGroupPrediction>,
    val recommendedActions: List<String>
)

data class BloodGroupPrediction(
    val bloodGroup: String,
    val currentCount: Int,
    val currentPercent: Double,
    val expectedPercent: Double,
    val adjustedDemand: Double,
    val shortage: Double,
    val priority: AlertPriority
)
