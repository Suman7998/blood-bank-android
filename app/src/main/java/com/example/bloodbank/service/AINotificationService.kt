package com.example.bloodbank.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.bloodbank.MainActivity
import com.example.bloodbank.R
import com.example.bloodbank.data.entity.*
import com.example.bloodbank.data.repository.DonorRepository
import com.example.bloodbank.util.NotificationPreferenceManager
import com.example.bloodbank.ai.BloodDemandPredictor
import kotlinx.coroutines.*
import java.util.*
import kotlin.random.Random

class AINotificationService(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID_EMERGENCY = "emergency_alerts"
        private const val CHANNEL_ID_REMINDERS = "donation_reminders"
        private const val CHANNEL_ID_GENERAL = "general_notifications"
        private const val CHANNEL_ID_HEALTH = "health_tips"
        
        private const val NOTIFICATION_ID_BASE = 1000
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    private val preferenceManager = NotificationPreferenceManager(context)
    private val bloodDemandPredictor = BloodDemandPredictor()
    private var notificationIdCounter = NOTIFICATION_ID_BASE
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_EMERGENCY,
                    "Emergency Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Critical blood shortage and emergency requests"
                    enableVibration(true)
                    setShowBadge(true)
                },
                NotificationChannel(
                    CHANNEL_ID_REMINDERS,
                    "Donation Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminders for blood donation appointments"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_ID_GENERAL,
                    "General Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "General blood bank notifications"
                },
                NotificationChannel(
                    CHANNEL_ID_HEALTH,
                    "Health Tips",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Health tips and donation guidelines"
                }
            )
            
            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { systemNotificationManager.createNotificationChannel(it) }
        }
    }
    
    fun generateIntelligentAlert(donorRepository: DonorRepository): BloodAlert? {
        return runBlocking {
            val donors = donorRepository.getAllLocal()
            analyzeAndGenerateAlert(donors)
        }
    }
    
    private fun analyzeAndGenerateAlert(donors: List<Donor>): BloodAlert? {
        if (donors.isEmpty()) {
            return createWelcomeAlert()
        }
        
        // Use AI predictor for advanced analysis
        val analysis = bloodDemandPredictor.analyzeBloodInventory(donors)
        val smartAlert = bloodDemandPredictor.generateSmartAlert(analysis)
        
        // Return smart alert if available, otherwise generate other types
        return smartAlert ?: when {
            shouldSendHospitalEmergencyAlert() -> createHospitalEmergencyAlert()
            shouldSendHospitalPartnershipAlert() -> createHospitalPartnershipAlert()
            shouldSendBloodDriveCampaignAlert() -> createBloodDriveCampaignAlert()
            shouldSendSeasonalCampaignAlert() -> createSeasonalCampaignAlert()
            shouldSendMobileBloodCampAlert() -> createMobileBloodCampAlert()
            shouldSendDonationReminder() -> createDonationReminderAlert()
            shouldSendHealthTip() -> createHealthTipAlert()
            shouldSendNearbyDonationAlert() -> createNearbyDonationAlert()
            else -> null
        }
    }
    
    private fun findCriticalBloodGroups(bloodGroupCounts: Map<String, Int>, totalDonors: Int): List<String> {
        val expectedPercentages = mapOf(
            "O+" to 0.38, "A+" to 0.34, "B+" to 0.09, "AB+" to 0.03,
            "O-" to 0.07, "A-" to 0.06, "B-" to 0.02, "AB-" to 0.01
        )
        
        return expectedPercentages.filter { (bloodGroup, expectedPercent) ->
            val actualCount = bloodGroupCounts[bloodGroup] ?: 0
            val actualPercent = actualCount.toDouble() / totalDonors
            actualPercent < expectedPercent * 0.5 // Less than 50% of expected
        }.keys.toList()
    }
    
    private fun createShortageAlert(bloodGroup: String): BloodAlert {
        val messages = listOf(
            "Critical shortage of $bloodGroup blood detected!",
            "Urgent: $bloodGroup donors needed immediately",
            "Emergency: Low $bloodGroup blood inventory"
        )
        
        return BloodAlert(
            title = "🚨 Blood Shortage Alert",
            message = messages.random(),
            bloodGroup = bloodGroup,
            priority = AlertPriority.CRITICAL,
            type = AlertType.BLOOD_SHORTAGE,
            location = "All Centers",
            requiredUnits = Random.nextInt(5, 20),
            expiryTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24 hours
        )
    }
    
    private fun createDonationReminderAlert(): BloodAlert {
        val messages = listOf(
            "It's been a while since your last donation. Ready to save lives?",
            "Your donation can help up to 3 people. Schedule your appointment today!",
            "Be a hero - donate blood and make a difference in someone's life"
        )
        
        return BloodAlert(
            title = "💝 Donation Reminder",
            message = messages.random(),
            bloodGroup = "All",
            priority = AlertPriority.MEDIUM,
            type = AlertType.DONATION_REMINDER,
            location = "Nearby Centers"
        )
    }
    
    private fun createHealthTipAlert(): BloodAlert {
        val tips = listOf(
            "Stay hydrated! Drink plenty of water before and after donating blood.",
            "Eat iron-rich foods like spinach, red meat, and beans to maintain healthy blood levels.",
            "Get enough sleep before donating - it helps your body recover faster.",
            "Avoid alcohol 24 hours before donating blood for the best donation experience.",
            "Regular exercise improves blood circulation and overall health."
        )
        
        return BloodAlert(
            title = "💡 Health Tip",
            message = tips.random(),
            bloodGroup = "All",
            priority = AlertPriority.LOW,
            type = AlertType.HEALTH_TIP,
            location = null
        )
    }
    
    private fun createNearbyDonationAlert(): BloodAlert {
        val locations = listOf("City Hospital", "Red Cross Center", "Community Health Center", "Medical College")
        
        return BloodAlert(
            title = "📍 Nearby Donation Drive",
            message = "Blood donation camp happening near you! Join us and save lives.",
            bloodGroup = "All",
            priority = AlertPriority.MEDIUM,
            type = AlertType.NEARBY_DONATION,
            location = locations.random(),
            expiryTime = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000) // 7 days
        )
    }
    
    private fun createWelcomeAlert(): BloodAlert {
        return BloodAlert(
            title = "🩸 Welcome to Blood Bank",
            message = "Thank you for joining our life-saving community! Explore features and help save lives.",
            bloodGroup = "All",
            priority = AlertPriority.LOW,
            type = AlertType.HEALTH_TIP,
            location = null
        )
    }
    
    // Hospital and Blood Campaign Alert Functions
    
    private fun createHospitalEmergencyAlert(): BloodAlert {
        val hospitals = listOf("City General Hospital", "Metro Medical Center", "Central Hospital", "Emergency Care Hospital", "Regional Trauma Center")
        val bloodGroups = listOf("O-", "AB+", "A-", "B+", "O+")
        val selectedHospital = hospitals.random()
        val selectedBloodGroup = bloodGroups.random()
        
        val emergencyMessages = listOf(
            "⚡ CRITICAL: $selectedHospital facing severe $selectedBloodGroup shortage! Multiple patients in ICU need immediate transfusion. Every minute counts - respond now!",
            "🆘 EMERGENCY ALERT: $selectedBloodGroup blood stock depleted at $selectedHospital! Accident victims require urgent care. Your donation saves lives TODAY!",
            "🚨 RED ALERT: $selectedHospital needs $selectedBloodGroup donors ASAP! Emergency surgery in progress. Be the hero someone desperately needs!",
            "⏰ URGENT CALL: $selectedBloodGroup blood critically needed at $selectedHospital! Life-threatening situation. Immediate donor response required!",
            "💔 LIFE OR DEATH: $selectedHospital running out of $selectedBloodGroup blood! Critical patients waiting. Rush to donate - time is running out!"
        )
        
        return BloodAlert(
            title = "🚨 HOSPITAL EMERGENCY",
            message = emergencyMessages.random(),
            bloodGroup = selectedBloodGroup,
            priority = AlertPriority.EMERGENCY,
            type = AlertType.HOSPITAL_EMERGENCY,
            location = selectedHospital,
            requiredUnits = Random.nextInt(3, 15),
            expiryTime = System.currentTimeMillis() + (2 * 60 * 60 * 1000), // 2 hours
            contactInfo = "Emergency Hotline: +91-911-BLOOD"
        )
    }
    
    private fun createBloodDriveCampaignAlert(): BloodAlert {
        val locations = listOf("Central Community Center", "City Mall", "University Campus", "Sports Complex", "Town Hall", "Shopping Plaza", "Corporate Office Complex")
        val selectedLocation = locations.random()
        
        val campaignMessages = listOf(
            "🎯 MEGA BLOOD DRIVE at $selectedLocation! Join 500+ donors this weekend. Free health screening + refreshments. Book your 15-minute slot now!",
            "🌟 COMMUNITY HEROES NEEDED! Blood donation camp at $selectedLocation. Help us reach our goal of 200 units. Every donor gets a thank-you kit!",
            "🏆 BLOOD DONATION CHAMPIONSHIP at $selectedLocation! Compete with friends to save lives. Prizes for top donors + certificates for all participants!",
            "💪 BE THE CHANGE! Mobile blood unit stationed at $selectedLocation. Quick, safe, and convenient. Walk-ins welcome - no appointment needed!",
            "🎪 BLOOD DONATION FESTIVAL at $selectedLocation! Live music, food stalls, and life-saving donations. Make it a family day out while helping others!"
        )
        
        return BloodAlert(
            title = "🩸 BLOOD DRIVE CAMPAIGN",
            message = campaignMessages.random(),
            bloodGroup = "All",
            priority = AlertPriority.MEDIUM,
            type = AlertType.BLOOD_DRIVE_CAMPAIGN,
            location = selectedLocation,
            expiryTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000), // 24 hours
            contactInfo = "Register: blooddrive@hospital.com"
        )
    }
    
    private fun createSeasonalCampaignAlert(): BloodAlert {
        val seasonalMessages = listOf(
            "🎊 NEW YEAR, NEW HOPE! Start 2024 by saving lives. Join our resolution campaign - donate blood and inspire others. First 100 donors get special gifts!",
            "🌺 VALENTINE'S SPECIAL: Share love, donate blood! Couples who donate together get matching certificates. Spread love beyond hearts - save lives!",
            "🎃 HALLOWEEN BLOOD DRIVE: Don't let blood supplies get scary low! Dress up and donate. Best costume wins prizes while saving real lives!",
            "🎆 INDEPENDENCE DAY BLOOD MARATHON: Celebrate freedom by freeing someone from illness. 48-hour non-stop donation drive. Be a true patriot!",
            "🌙 RAMADAN BLOOD APPEAL: During this holy month, give the gift of life. Special evening donation slots available. Break your fast knowing you saved lives!"
        )
        
        val locations = listOf("Seasonal Care Center", "Community Health Hub", "Festival Grounds Medical", "Holiday Donation Center", "Special Events Clinic")
        
        return BloodAlert(
            title = "🎉 SEASONAL BLOOD CAMPAIGN",
            message = seasonalMessages.random(),
            bloodGroup = "All",
            priority = AlertPriority.MEDIUM,
            type = AlertType.SEASONAL_CAMPAIGN,
            location = locations.random(),
            expiryTime = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000), // 7 days
            contactInfo = "Campaign Info: seasonal@bloodbank.org"
        )
    }
    
    private fun createHospitalPartnershipAlert(): BloodAlert {
        val hospitals = listOf("Metro Hospital", "City Medical Center", "Regional Health Center", "University Hospital", "Children's Specialty Hospital")
        val bloodGroups = listOf("AB+", "O-", "A-", "B-", "AB-")
        val selectedHospital = hospitals.random()
        val selectedBloodGroup = bloodGroups.random()
        val daysLeft = Random.nextInt(1, 4)
        
        val partnershipMessages = listOf(
            "🤝 PARTNERSHIP ALERT: $selectedHospital reports $selectedBloodGroup shortage! Our trusted partner needs your help. 15 surgeries on hold - act fast!",
            "⚠️ PARTNER HOSPITAL SOS: $selectedBloodGroup inventory at $selectedHospital down to $daysLeft days! Cancer patients need your support. Respond immediately!",
            "🔔 NETWORK ALERT: $selectedHospital (our key partner) facing $selectedBloodGroup crisis! Pediatric ward affected. Your donation saves children's lives!",
            "📢 ALLIANCE EMERGENCY: $selectedBloodGroup stocks depleted at partner $selectedHospital! Organ transplant surgeries at risk. Urgent donor mobilization needed!",
            "🚨 PARTNER CRISIS: $selectedHospital's $selectedBloodGroup supply critically low! Maternity ward needs immediate support. Help mothers and babies!"
        )
        
        return BloodAlert(
            title = "🏥 PARTNERSHIP ALERT",
            message = partnershipMessages.random(),
            bloodGroup = selectedBloodGroup,
            priority = AlertPriority.HIGH,
            type = AlertType.HOSPITAL_PARTNERSHIP,
            location = selectedHospital,
            requiredUnits = Random.nextInt(5, 25),
            expiryTime = System.currentTimeMillis() + (daysLeft * 24 * 60 * 60 * 1000),
            contactInfo = "Partnership Coordinator: +91-800-PARTNER"
        )
    }
    
    private fun createMobileBloodCampAlert(): BloodAlert {
        val areas = listOf("Sector 15 Market", "Downtown Plaza", "Residential Complex", "Shopping District", "Business Park", "Metro Station", "College Campus")
        val selectedArea = areas.random()
        val dayOfWeek = listOf("Saturday", "Sunday", "Friday").random()
        
        val mobileMessages = listOf(
            "🚐 MOBILE BLOOD UNIT ALERT! Our van is parked at $selectedArea right now! Quick 10-minute donation process. Free snacks and juice for all donors!",
            "📱 BLOOD-ON-WHEELS at $selectedArea! State-of-the-art mobile lab with AC and comfortable seating. Donate in luxury while saving lives!",
            "🎪 MOBILE DONATION CARNIVAL at $selectedArea! Games, music, and blood donation all in one place. Bring family and friends for a fun day out!",
            "🏃‍♂️ EXPRESS BLOOD DONATION at $selectedArea! No queues, no waiting. Our mobile unit processes donors in under 15 minutes. Perfect for busy schedules!",
            "🌟 NEIGHBORHOOD HEROES WANTED at $selectedArea! Mobile blood camp exclusively for local residents. Help your community - donate at your doorstep!"
        )
        
        return BloodAlert(
            title = "🚐 MOBILE BLOOD CAMP",
            message = mobileMessages.random(),
            bloodGroup = "All",
            priority = AlertPriority.MEDIUM,
            type = AlertType.MOBILE_BLOOD_CAMP,
            location = selectedArea,
            expiryTime = System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000), // 3 days
            contactInfo = "Mobile Unit: +91-900-MOBILE"
        )
    }
    
    private fun shouldSendDonationReminder(): Boolean {
        // AI logic: Send reminder based on time patterns
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        // Send reminders on weekdays between 9 AM and 6 PM
        return dayOfWeek in 2..6 && hour in 9..18 && Random.nextFloat() < 0.3f
    }
    
    private fun shouldSendHealthTip(): Boolean {
        // Send health tips randomly, but more likely in the morning
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val morningBoost = if (hour in 7..10) 0.4f else 0.1f
        
        return Random.nextFloat() < morningBoost
    }
    
    private fun shouldSendNearbyDonationAlert(): Boolean {
        // Send nearby donation alerts occasionally
        return Random.nextFloat() < 0.2f
    }
    
    // New AI logic functions for hospital and campaign alerts
    
    private fun shouldSendHospitalEmergencyAlert(): Boolean {
        // Send hospital emergency alerts during critical times
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        // More likely during day hours when hospitals are busier
        val timeBoost = if (hour in 8..20) 0.15f else 0.05f
        return Random.nextFloat() < timeBoost
    }
    
    private fun shouldSendBloodDriveCampaignAlert(): Boolean {
        // Send campaign alerts more frequently on weekdays
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        // Send during business hours on weekdays
        return dayOfWeek in 2..6 && hour in 9..17 && Random.nextFloat() < 0.25f
    }
    
    private fun shouldSendSeasonalCampaignAlert(): Boolean {
        // Send seasonal campaigns based on time of year and day
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        // More likely during holiday seasons (Nov-Jan) and weekends
        val seasonalBoost = if (month in 10..0) 0.3f else 0.1f
        val weekendBoost = if (dayOfWeek in 1..7) 0.2f else 0.1f
        
        return Random.nextFloat() < (seasonalBoost + weekendBoost)
    }
    
    private fun shouldSendHospitalPartnershipAlert(): Boolean {
        // Send partnership alerts during critical shortage periods
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        // More likely during morning and evening hours
        val criticalTimeBoost = if (hour in 7..10 || hour in 17..20) 0.2f else 0.1f
        return Random.nextFloat() < criticalTimeBoost
    }
    
    private fun shouldSendMobileBloodCampAlert(): Boolean {
        // Send mobile camp alerts more on weekends
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        // More likely on weekends when mobile camps are active
        val weekendBoost = if (dayOfWeek == 1 || dayOfWeek == 7) 0.4f else 0.15f
        return Random.nextFloat() < weekendBoost
    }
    
    fun showNotification(alert: BloodAlert) {
        if (!notificationManager.areNotificationsEnabled()) {
            return
        }
        
        // Check user preferences
        val isEmergency = alert.priority >= AlertPriority.CRITICAL
        if (!preferenceManager.shouldShowNotification(alert.bloodGroup, isEmergency)) {
            return
        }
        
        val channelId = when (alert.priority) {
            AlertPriority.CRITICAL, AlertPriority.EMERGENCY -> CHANNEL_ID_EMERGENCY
            AlertPriority.HIGH, AlertPriority.MEDIUM -> CHANNEL_ID_REMINDERS
            AlertPriority.LOW -> if (alert.type == AlertType.HEALTH_TIP) CHANNEL_ID_HEALTH else CHANNEL_ID_GENERAL
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(alert.title)
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setPriority(getNotificationPriority(alert.priority))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .apply {
                if (alert.priority >= AlertPriority.HIGH) {
                    setDefaults(NotificationCompat.DEFAULT_ALL)
                }
                
                alert.location?.let { location ->
                    setSubText("📍 $location")
                }
                
                when (alert.type) {
                    AlertType.BLOOD_SHORTAGE, AlertType.HOSPITAL_EMERGENCY -> {
                        setColor(0xFFD32F2F.toInt()) // Red color for emergency
                        setCategory(NotificationCompat.CATEGORY_ALARM)
                    }
                    AlertType.HOSPITAL_PARTNERSHIP -> {
                        setColor(0xFFFF5722.toInt()) // Orange color for partnership alerts
                        setCategory(NotificationCompat.CATEGORY_REMINDER)
                    }
                    AlertType.BLOOD_DRIVE_CAMPAIGN, AlertType.SEASONAL_CAMPAIGN -> {
                        setColor(0xFF2196F3.toInt()) // Blue color for campaigns
                        setCategory(NotificationCompat.CATEGORY_EVENT)
                    }
                    AlertType.MOBILE_BLOOD_CAMP -> {
                        setColor(0xFF4CAF50.toInt()) // Green color for mobile camps
                        setCategory(NotificationCompat.CATEGORY_EVENT)
                    }
                    else -> {
                        // Default handling for other types
                    }
                }
            }
            .build()
        
        try {
            notificationManager.notify(getNextNotificationId(), notification)
        } catch (e: SecurityException) {
            // Handle case where notification permission is not granted
        }
    }
    
    private fun getNotificationPriority(alertPriority: AlertPriority): Int {
        return when (alertPriority) {
            AlertPriority.LOW -> NotificationCompat.PRIORITY_LOW
            AlertPriority.MEDIUM -> NotificationCompat.PRIORITY_DEFAULT
            AlertPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
            AlertPriority.CRITICAL, AlertPriority.EMERGENCY -> NotificationCompat.PRIORITY_MAX
        }
    }
    
    private fun getNextNotificationId(): Int {
        return notificationIdCounter++
    }
}
