package com.example.bloodbank.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.bloodbank.R
import com.example.bloodbank.data.entity.Donor

class DonorAdapter(
    private val onCallClick: (Donor) -> Unit,
    private val onImageClick: (Donor) -> Unit,
    private val onVoiceClick: (Donor) -> Unit,
    private val onVideoClick: (Donor) -> Unit
) : RecyclerView.Adapter<DonorAdapter.DonorViewHolder>() {
    
    private var donors: List<Donor> = emptyList()
    
    fun updateDonors(newDonors: List<Donor>) {
        donors = newDonors
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DonorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_donor, parent, false)
        return DonorViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: DonorViewHolder, position: Int) {
        holder.bind(donors[position])
    }
    
    override fun getItemCount(): Int = donors.size
    
    inner class DonorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProfile: ImageView = itemView.findViewById(R.id.ivProfile)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvBloodGroup: TextView = itemView.findViewById(R.id.tvBloodGroup)
        private val tvAge: TextView = itemView.findViewById(R.id.tvAge)
        private val tvGender: TextView = itemView.findViewById(R.id.tvGender)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        private val tvPhone: TextView = itemView.findViewById(R.id.tvPhone)
        private val tvAvailability: TextView = itemView.findViewById(R.id.tvAvailability)
        private val tvSpecialNotes: TextView = itemView.findViewById(R.id.tvSpecialNotes)
        private val btnCall: Button = itemView.findViewById(R.id.btnCall)
        private val btnViewImages: Button = itemView.findViewById(R.id.btnViewImages)
        private val btnPlayVoice: Button = itemView.findViewById(R.id.btnPlayVoice)
        private val btnPlayVideo: Button = itemView.findViewById(R.id.btnPlayVideo)
        private val ivVerified: ImageView = itemView.findViewById(R.id.ivVerified)
        
        fun bind(donor: Donor) {
            tvName.text = donor.name
            tvBloodGroup.text = donor.bloodGroup
            tvAge.text = "${donor.age} years"
            tvGender.text = donor.gender
            tvLocation.text = donor.location
            tvPhone.text = donor.phone
            tvSpecialNotes.text = donor.specialNotes ?: "No special notes"
            
            // Set availability status
            tvAvailability.text = if (donor.isAvailable) "Available" else "Not Available"
            tvAvailability.setTextColor(
                if (donor.isAvailable) 
                    itemView.context.getColor(android.R.color.holo_green_dark)
                else 
                    itemView.context.getColor(android.R.color.holo_red_dark)
            )
            
            // Load profile image with Glide
            Glide.with(itemView.context)
                .load(donor.profileImageUrl)
                .placeholder(R.drawable.ic_person_placeholder)
                .error(R.drawable.ic_person_placeholder)
                .transform(CircleCrop())
                .into(ivProfile)
            
            // Set verification status
            when (donor.verificationStatus) {
                "Verified" -> {
                    ivVerified.visibility = View.VISIBLE
                    ivVerified.setImageResource(R.drawable.ic_verified)
                }
                "Pending" -> {
                    ivVerified.visibility = View.VISIBLE
                    ivVerified.setImageResource(R.drawable.ic_pending)
                }
                else -> {
                    ivVerified.visibility = View.GONE
                }
            }
            
            // Set click listeners
            btnCall.setOnClickListener { onCallClick(donor) }
            btnViewImages.setOnClickListener { onImageClick(donor) }
            
            // Enable/disable voice and video buttons based on availability
            btnPlayVoice.isEnabled = !donor.voiceNoteUrl.isNullOrEmpty()
            btnPlayVoice.setOnClickListener { onVoiceClick(donor) }
            
            btnPlayVideo.isEnabled = !donor.videoNoteUrl.isNullOrEmpty()
            btnPlayVideo.setOnClickListener { onVideoClick(donor) }
            
            // Set button colors based on availability
            btnPlayVoice.alpha = if (btnPlayVoice.isEnabled) 1.0f else 0.5f
            btnPlayVideo.alpha = if (btnPlayVideo.isEnabled) 1.0f else 0.5f
            
            // Set blood group background color
            val bloodGroupColor = when (donor.bloodGroup) {
                "A+" -> R.color.blood_a_positive
                "A-" -> R.color.blood_a_negative
                "B+" -> R.color.blood_b_positive
                "B-" -> R.color.blood_b_negative
                "AB+" -> R.color.blood_ab_positive
                "AB-" -> R.color.blood_ab_negative
                "O+" -> R.color.blood_o_positive
                "O-" -> R.color.blood_o_negative
                else -> R.color.blood_default
            }
            
            tvBloodGroup.setBackgroundResource(bloodGroupColor)
        }
    }
}
