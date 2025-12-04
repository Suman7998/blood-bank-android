package com.example.bloodbank.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.bloodbank.data.entity.*
import com.example.bloodbank.data.repository.AlertRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BloodBankMessagingService : FirebaseMessagingService() {
    
    companion object {
        private const val TAG = "BloodBankMessaging"
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "From: ${remoteMessage.from}")
        
        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }
        
        // Handle notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            handleNotificationMessage(it)
        }
    }
    
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        
        // Send token to server if needed
        sendRegistrationToServer(token)
    }
    
    private fun handleDataMessage(data: Map<String, String>) {
        try {
            val title = data["title"] ?: "Blood Bank Alert"
            val message = data["message"] ?: "New notification"
            val bloodGroup = data["blood_group"] ?: "All"
            val priority = data["priority"]?.let { 
                try { AlertPriority.valueOf(it) } catch (e: Exception) { AlertPriority.MEDIUM }
            } ?: AlertPriority.MEDIUM
            val type = data["type"]?.let {
                try { AlertType.valueOf(it) } catch (e: Exception) { AlertType.BLOOD_SHORTAGE }
            } ?: AlertType.BLOOD_SHORTAGE
            val location = data["location"]
            val requiredUnits = data["required_units"]?.toIntOrNull() ?: 1
            val contactInfo = data["contact_info"]
            
            val alert = BloodAlert(
                title = title,
                message = message,
                bloodGroup = bloodGroup,
                priority = priority,
                type = type,
                location = location,
                requiredUnits = requiredUnits,
                contactInfo = contactInfo
            )
            
            // Save to local database and show notification
            CoroutineScope(Dispatchers.IO).launch {
                val alertRepository = AlertRepository(applicationContext)
                alertRepository.addAlert(alert)
                
                val aiNotificationService = AINotificationService(applicationContext)
                aiNotificationService.showNotification(alert)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling data message", e)
        }
    }
    
    private fun handleNotificationMessage(notification: RemoteMessage.Notification) {
        val title = notification.title ?: "Blood Bank"
        val body = notification.body ?: "New notification"
        
        // Create a general alert from notification
        val alert = BloodAlert(
            title = title,
            message = body,
            bloodGroup = "All",
            priority = AlertPriority.MEDIUM,
            type = AlertType.HEALTH_TIP
        )
        
        CoroutineScope(Dispatchers.IO).launch {
            val alertRepository = AlertRepository(applicationContext)
            alertRepository.addAlert(alert)
        }
    }
    
    private fun sendRegistrationToServer(token: String) {
        // TODO: Send token to your server for targeted notifications
        Log.d(TAG, "Token sent to server: $token")
    }
}
