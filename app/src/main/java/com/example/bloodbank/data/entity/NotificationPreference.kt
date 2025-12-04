package com.example.bloodbank.data.entity

data class NotificationPreference(
    val userId: String,
    val enableBloodShortageAlerts: Boolean = true,
    val enableDonationReminders: Boolean = true,
    val enableEmergencyAlerts: Boolean = true,
    val enableNearbyDonations: Boolean = true,
    val enableHealthTips: Boolean = true,
    val preferredBloodGroups: List<String> = emptyList(),
    val maxDistanceKm: Int = 50,
    val quietHoursStart: Int = 22, // 10 PM
    val quietHoursEnd: Int = 8,    // 8 AM
    val enableQuietHours: Boolean = true,
    val notificationSound: Boolean = true,
    val vibration: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)
