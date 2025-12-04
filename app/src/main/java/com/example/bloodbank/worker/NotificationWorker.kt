package com.example.bloodbank.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.bloodbank.data.repository.DonorRepository
import com.example.bloodbank.service.AINotificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val aiService = AINotificationService(applicationContext)
            val donorRepository = DonorRepository(applicationContext)
            
            // Generate intelligent alert based on current data
            val alert = aiService.generateIntelligentAlert(donorRepository)
            
            alert?.let { 
                aiService.showNotification(it)
            }
            
            Result.success()
        } catch (e: Exception) {
            // Log error but don't fail the work to avoid breaking the app
            Result.success() // Return success to prevent retries
        }
    }
}
