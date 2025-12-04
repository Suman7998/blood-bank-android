package com.example.bloodbank.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bloodbank.databinding.ItemChatUserBinding
import com.example.bloodbank.databinding.ItemChatBotBinding

class ChatAdapter(private val items: MutableList<ChatMessage> = mutableListOf()) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_BOT = 2
    }

    fun addMessage(message: ChatMessage) {
        items.add(message)
        notifyItemInserted(items.size - 1)
    }

    override fun getItemViewType(position: Int): Int = if (items[position].isUser) TYPE_USER else TYPE_BOT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_USER) {
            val binding = ItemChatUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            UserVH(binding)
        } else {
            val binding = ItemChatBotBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            BotVH(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder is UserVH) holder.bind(item) else if (holder is BotVH) holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    inner class UserVH(private val binding: ItemChatUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessage) {
            binding.tvText.text = item.text
        }
    }

    inner class BotVH(private val binding: ItemChatBotBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessage) {
            binding.tvText.text = item.text
        }
    }
}
