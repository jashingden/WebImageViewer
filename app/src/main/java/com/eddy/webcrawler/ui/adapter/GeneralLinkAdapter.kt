package com.eddy.webcrawler.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eddy.webcrawler.databinding.ItemGeneralLinkBinding
import com.eddy.webcrawler.ui.viewmodel.SelectableLink

class GeneralLinkAdapter(
    private val onItemCheckChanged: (position: Int, isChecked: Boolean) -> Unit
) : ListAdapter<SelectableLink, GeneralLinkAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGeneralLinkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(private val binding: ItemGeneralLinkBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SelectableLink, position: Int) {
            binding.tvDisplayName.text = item.displayName
            binding.tvUrl.text = item.url
            binding.cbLink.isChecked = item.isChecked

            binding.root.setOnClickListener {
                val newChecked = !item.isChecked
                binding.cbLink.isChecked = newChecked
                onItemCheckChanged(adapterPosition, newChecked)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<SelectableLink>() {
            override fun areItemsTheSame(oldItem: SelectableLink, newItem: SelectableLink): Boolean {
                return oldItem.url == newItem.url
            }

            override fun areContentsTheSame(oldItem: SelectableLink, newItem: SelectableLink): Boolean {
                return oldItem == newItem
            }
        }
    }
}
