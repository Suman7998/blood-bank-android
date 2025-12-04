package com.example.bloodbank.data.entity

data class Donor(
    val id: Long = 0,
    val name: String,
    val bloodGroup: String,
    val phone: String,
    val city: String,
    val age: Int = 25,
    val gender: String = "Male",
    val location: String = city,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val profileImageUrl: String? = null,
    val idDocumentUrl: String? = null,
    val medicalReportsUrl: String? = null,
    val isAvailable: Boolean = true,
    val lastDonationDate: Long = 0,
    val emergencyContact: String? = null,
    val bloodPressure: String? = null,
    val weight: String? = null,
    val specialNotes: String? = null,
    val voiceNoteUrl: String? = null,
    val videoNoteUrl: String? = null,
    val registrationDate: Long = System.currentTimeMillis(),
    val verificationStatus: String = "Pending" // Pending, Verified, Rejected
)
