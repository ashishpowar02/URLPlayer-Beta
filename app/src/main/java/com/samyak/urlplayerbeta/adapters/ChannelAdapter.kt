package com.samyak.urlplayerbeta.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.samyak.urlplayerbeta.databinding.ItemChannelsBinding
import com.samyak.urlplayerbeta.models.Videos

class ChannelAdapter(
    private val onPlayClick: (Videos) -> Unit,
    private val onEditClick: (Videos) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    private val items = mutableListOf<Videos>()

    inner class ChannelViewHolder(private val binding: ItemChannelsBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Videos) {
            binding.tvChannelName.text = item.name
            binding.tvChannelLink.text = item.url

            binding.editButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onEditClick(item)
                }
            }

            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onPlayClick(item)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemChannelsBinding.inflate(inflater, parent, false)
        return ChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun addItem(newItem: Videos) {
        items.add(newItem)
        notifyItemInserted(items.lastIndex)
    }

    fun updateItems(newItems: List<Videos>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
} 