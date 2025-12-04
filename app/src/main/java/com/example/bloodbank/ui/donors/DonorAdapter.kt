package com.example.bloodbank.ui.donors

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bloodbank.data.entity.Donor
import com.example.bloodbank.databinding.ItemDonorBinding

class DonorAdapter : ListAdapter<Donor, DonorAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Donor>() {
            override fun areItemsTheSame(oldItem: Donor, newItem: Donor): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Donor, newItem: Donor): Boolean = oldItem == newItem
        }
    }

    inner class VH(private val binding: ItemDonorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Donor) {
            binding.tvName.text = item.name
            binding.tvBloodGroup.text = item.bloodGroup
            binding.tvLocation.text = item.city
            binding.tvPhone.text = item.phone
            binding.tvAge.text = "${item.age} years"
            binding.tvGender.text = item.gender
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemDonorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}
