package com.eddy.webcrawler.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.eddy.webcrawler.data.model.IndexWithThumbnail
import com.eddy.webcrawler.databinding.ItemSelectIndexBinding
import java.io.File

class SelectIndexAdapter(
    private val onDisplayClick: (Long) -> Unit,
    private val onDeleteClick: (Long) -> Unit
) : ListAdapter<IndexWithThumbnail, SelectIndexAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSelectIndexBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemSelectIndexBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: IndexWithThumbnail) {
            binding.tvTitle.text = item.linkIndex.title
            
            val imageSource = item.localThumbnailPath?.let { File(it) } ?: item.thumbnailUrl
            binding.ivThumbnail.load(imageSource) {
                crossfade(true)
                // Optionally add placeholder/error
            }

            binding.btnDisplay.setOnClickListener {
                onDisplayClick(item.linkIndex.id)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(item.linkIndex.id)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<IndexWithThumbnail>() {
            override fun areItemsTheSame(oldItem: IndexWithThumbnail, newItem: IndexWithThumbnail): Boolean {
                return oldItem.linkIndex.id == newItem.linkIndex.id
            }

            override fun areContentsTheSame(oldItem: IndexWithThumbnail, newItem: IndexWithThumbnail): Boolean {
                return oldItem == newItem
            }
        }
    }
}
