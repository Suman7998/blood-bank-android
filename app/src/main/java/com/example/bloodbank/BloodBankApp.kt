package com.example.bloodbank

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager

class BloodBankApp : Application() {

    override fun onCreate() {
        super.onCreate()
        
        try {
            // Initialize WorkManager with error handling
            initializeWorkManager()
            
            Log.d("BloodBankApp", "Application initialized successfully")
        } catch (e: Exception) {
            Log.e("BloodBankApp", "Error during application initialization", e)
            // Continue without crashing - let the app start even if some features fail
        }
    }
    
    private fun initializeWorkManager() {
        try {
            val config = Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build()
            
            WorkManager.initialize(this, config)
            Log.d("BloodBankApp", "WorkManager initialized successfully")
        } catch (e: Exception) {
            Log.e("BloodBankApp", "Failed to initialize WorkManager", e)
            // Don't crash the app if WorkManager fails to initialize
        }
    }
}
