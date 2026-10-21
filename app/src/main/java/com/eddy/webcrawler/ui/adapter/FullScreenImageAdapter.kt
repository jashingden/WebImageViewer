package com.eddy.webcrawler.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.eddy.webcrawler.data.model.ContentItem
import com.eddy.webcrawler.databinding.ItemFullScreenImageBinding

class FullScreenImageAdapter(
    private val onRegionClick: (x: Float, width: Float) -> Unit
) : ListAdapter<ContentItem.ImageItem, FullScreenImageAdapter.ImageViewHolder>(ImageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemFullScreenImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ImageViewHolder(private val binding: ItemFullScreenImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ContentItem.ImageItem) {
            binding.progressBar.visibility = View.VISIBLE
            binding.imageView.load(item.localPath ?: item.url) {
                listener(
                    onSuccess = { _, _ -> binding.progressBar.visibility = View.GONE },
                    onError = { _, _ -> binding.progressBar.visibility = View.GONE }
                )
            }
            binding.imageView.onRegionClick = { x, width ->
                onRegionClick(x, width)
            }
        }
    }

    class ImageDiffCallback : DiffUtil.ItemCallback<ContentItem.ImageItem>() {
        override fun areItemsTheSame(oldItem: ContentItem.ImageItem, newItem: ContentItem.ImageItem): Boolean {
            return oldItem.stableId == newItem.stableId
        }

        override fun areContentsTheSame(oldItem: ContentItem.ImageItem, newItem: ContentItem.ImageItem): Boolean {
            return oldItem == newItem
        }
    }
}
