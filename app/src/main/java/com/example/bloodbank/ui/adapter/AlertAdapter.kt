package com.example.bloodbank.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bloodbank.R
import com.example.bloodbank.data.entity.BloodAlert
import com.example.bloodbank.data.entity.AlertPriority
import java.text.SimpleDateFormat
import java.util.*

class AlertAdapter(
    private val onAlertClick: (BloodAlert) -> Unit
) : ListAdapter<BloodAlert, AlertAdapter.AlertViewHolder>(AlertDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alert, parent, false)
        return AlertViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.tv_alert_title)
        private val messageText: TextView = itemView.findViewById(R.id.tv_alert_message)
        private val timeText: TextView = itemView.findViewById(R.id.tv_alert_time)
        private val priorityIndicator: View = itemView.findViewById(R.id.view_priority_indicator)
        private val bloodGroupText: TextView = itemView.findViewById(R.id.tv_blood_group)
        private val locationText: TextView = itemView.findViewById(R.id.tv_location)
        
        fun bind(alert: BloodAlert) {
            titleText.text = alert.title
            messageText.text = alert.message
            bloodGroupText.text = alert.bloodGroup
            
            // Format timestamp
            val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            timeText.text = dateFormat.format(Date(alert.timestamp))
            
            // Set location or hide if null
            if (alert.location != null) {
                locationText.text = "📍 ${alert.location}"
                locationText.visibility = View.VISIBLE
            } else {
                locationText.visibility = View.GONE
            }
            
            // Set priority indicator color
            val priorityColor = when (alert.priority) {
                AlertPriority.EMERGENCY -> ContextCompat.getColor(itemView.context, R.color.priority_emergency)
                AlertPriority.CRITICAL -> ContextCompat.getColor(itemView.context, R.color.priority_critical)
                AlertPriority.HIGH -> ContextCompat.getColor(itemView.context, R.color.priority_high)
                AlertPriority.MEDIUM -> ContextCompat.getColor(itemView.context, R.color.priority_medium)
                AlertPriority.LOW -> ContextCompat.getColor(itemView.context, R.color.priority_low)
            }
            priorityIndicator.setBackgroundColor(priorityColor)
            
            // Set read/unread appearance
            val alpha = if (alert.isRead) 0.7f else 1.0f
            itemView.alpha = alpha
            
            itemView.setOnClickListener {
                onAlertClick(alert)
            }
        }
    }
    
    class AlertDiffCallback : DiffUtil.ItemCallback<BloodAlert>() {
        override fun areItemsTheSame(oldItem: BloodAlert, newItem: BloodAlert): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: BloodAlert, newItem: BloodAlert): Boolean {
            return oldItem == newItem
        }
    }
}
