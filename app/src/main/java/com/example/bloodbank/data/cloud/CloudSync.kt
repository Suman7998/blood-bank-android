package com.example.bloodbank.data.cloud

import com.example.bloodbank.data.entity.Donor
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CloudSync {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val donorsCol get() = db.collection("donors")

    suspend fun pushAll(donors: List<Donor>) {
        // Use batched writes in chunks to be safe
        val chunks = donors.chunked(400)
        for (chunk in chunks) {
            val batch = db.batch()
            chunk.forEach { donor ->
                // Use phone as an id if present to reduce duplicates, else auto-id
                val docRef = if (donor.phone.isNotBlank()) donorsCol.document(donor.phone) else donorsCol.document()
                val data = mapOf(
                    "name" to donor.name,
                    "blood_group" to donor.bloodGroup,
                    "phone" to donor.phone,
                    "city" to donor.city
                )
                batch.set(docRef, data)
            }
            batch.commit().await()
        }
    }

    suspend fun pullAll(): List<Donor> {
        val snap = donorsCol.get().await()
        return snap.documents.mapNotNull { d ->
            val name = d.getString("name") ?: return@mapNotNull null
            val bg = d.getString("blood_group") ?: return@mapNotNull null
            val phone = d.getString("phone") ?: ""
            val city = d.getString("city") ?: ""
            Donor(
                id = 0,
                name = name,
                bloodGroup = bg,
                phone = phone,
                city = city
            )
        }
    }
}
