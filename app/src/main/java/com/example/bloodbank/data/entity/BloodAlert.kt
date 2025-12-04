package com.example.bloodbank.data.entity

data class BloodAlert(
    val id: Long = 0,
    val title: String,
    val message: String,
    val bloodGroup: String,
    val priority: AlertPriority,
    val type: AlertType,
    val location: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isActive: Boolean = true,
    val expiryTime: Long? = null,
    val requiredUnits: Int = 1,
    val contactInfo: String? = null
)

enum class AlertPriority(val level: Int, val displayName: String) {
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High"),
    CRITICAL(4, "Critical"),
    EMERGENCY(5, "Emergency")
}

enum class AlertType(val displayName: String) {
    BLOOD_SHORTAGE("Blood Shortage"),
    DONATION_REMINDER("Donation Reminder"),
    EMERGENCY_REQUEST("Emergency Request"),
    NEARBY_DONATION("Nearby Donation"),
    INVENTORY_LOW("Inventory Low"),
    DONOR_MATCH("Donor Match"),
    APPOINTMENT_REMINDER("Appointment Reminder"),
    HEALTH_TIP("Health Tip"),
    HOSPITAL_EMERGENCY("Hospital Emergency"),
    BLOOD_DRIVE_CAMPAIGN("Blood Drive Campaign"),
    SEASONAL_CAMPAIGN("Seasonal Campaign"),
    HOSPITAL_PARTNERSHIP("Hospital Partnership"),
    MOBILE_BLOOD_CAMP("Mobile Blood Camp")
}
