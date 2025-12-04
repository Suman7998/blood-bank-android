package com.example.bloodbank.manager

import android.content.Context
import androidx.work.*
import com.example.bloodbank.worker.NotificationWorker
import java.util.concurrent.TimeUnit

class NotificationManager(private val context: Context) {
    
    companion object {
        private const val PERIODIC_NOTIFICATION_WORK = "periodic_notification_work"
        private const val IMMEDIATE_NOTIFICATION_WORK = "immediate_notification_work"
    }
    
    private val workManager = WorkManager.getInstance(context)
    
    fun startPeriodicNotifications() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val periodicWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            repeatInterval = 4, // Every 4 hours
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 1, // 1 hour flex
            flexTimeIntervalUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag("ai_notifications")
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_NOTIFICATION_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }
    
    fun triggerImmediateNotification() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        
        val immediateWorkRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setConstraints(constraints)
            .addTag("immediate_notification")
            .build()
        
        workManager.enqueueUniqueWork(
            IMMEDIATE_NOTIFICATION_WORK,
            ExistingWorkPolicy.REPLACE,
            immediateWorkRequest
        )
    }
    
    fun stopPeriodicNotifications() {
        workManager.cancelUniqueWork(PERIODIC_NOTIFICATION_WORK)
    }
    
    fun cancelAllNotifications() {
        workManager.cancelAllWorkByTag("ai_notifications")
        workManager.cancelAllWorkByTag("immediate_notification")
    }
}
