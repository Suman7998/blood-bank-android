package com.example.bloodbank.util

import android.content.Context
import android.content.SharedPreferences
import com.example.bloodbank.data.entity.NotificationPreference
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class NotificationPreferenceManager(context: Context) {
    
    companion object {
        private const val PREF_NAME = "notification_preferences"
        private const val KEY_PREFERENCES = "preferences"
        private const val DEFAULT_USER_ID = "default_user"
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun savePreferences(preferences: NotificationPreference) {
        val json = gson.toJson(preferences)
        sharedPreferences.edit()
            .putString(KEY_PREFERENCES, json)
            .apply()
    }
    
    fun getPreferences(): NotificationPreference {
        val json = sharedPreferences.getString(KEY_PREFERENCES, null)
        return if (json != null) {
            try {
                gson.fromJson(json, NotificationPreference::class.java)
            } catch (e: Exception) {
                getDefaultPreferences()
            }
        } else {
            getDefaultPreferences()
        }
    }
    
    private fun getDefaultPreferences(): NotificationPreference {
        return NotificationPreference(
            userId = DEFAULT_USER_ID,
            enableBloodShortageAlerts = true,
            enableDonationReminders = true,
            enableEmergencyAlerts = true,
            enableNearbyDonations = true,
            enableHealthTips = true,
            preferredBloodGroups = listOf("O+", "A+", "B+", "AB+", "O-", "A-", "B-", "AB-"),
            maxDistanceKm = 50,
            quietHoursStart = 22,
            quietHoursEnd = 8,
            enableQuietHours = true,
            notificationSound = true,
            vibration = true
        )
    }
    
    fun isQuietHoursActive(): Boolean {
        val preferences = getPreferences()
        if (!preferences.enableQuietHours) return false
        
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        
        return if (preferences.quietHoursStart <= preferences.quietHoursEnd) {
            currentHour >= preferences.quietHoursStart && currentHour < preferences.quietHoursEnd
        } else {
            currentHour >= preferences.quietHoursStart || currentHour < preferences.quietHoursEnd
        }
    }
    
    fun shouldShowNotification(bloodGroup: String, isEmergency: Boolean = false): Boolean {
        val preferences = getPreferences()
        
        // Always show emergency alerts
        if (isEmergency && preferences.enableEmergencyAlerts) {
            return true
        }
        
        // Check quiet hours for non-emergency alerts
        if (isQuietHoursActive() && !isEmergency) {
            return false
        }
        
        // Check blood group preferences
        return preferences.preferredBloodGroups.contains(bloodGroup) || bloodGroup == "All"
    }
}
