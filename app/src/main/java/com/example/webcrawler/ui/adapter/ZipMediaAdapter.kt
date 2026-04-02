package com.example.webcrawler.ui.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.webcrawler.R
import com.example.webcrawler.data.model.MediaType
import com.example.webcrawler.data.model.ZipMediaItem
import com.example.webcrawler.databinding.ItemZipMediaBinding

class ZipMediaAdapter(
    private val player: ExoPlayer
) : ListAdapter<ZipMediaItem, RecyclerView.ViewHolder>(ZipMediaItemDiffCallback()) {

    companion object {
        const val TYPE_IMAGE = 0
        const val TYPE_VIDEO = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).mediaType) {
            MediaType.IMAGE -> TYPE_IMAGE
            MediaType.VIDEO -> TYPE_VIDEO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_IMAGE -> ImageViewHolder(ItemZipMediaBinding.inflate(inflater, parent, false))
            TYPE_VIDEO -> VideoViewHolder(ItemZipMediaBinding.inflate(inflater, parent, false), player)
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is ImageViewHolder -> holder.bind(item)
            is VideoViewHolder -> holder.bind(item)
        }
    }

    class ImageViewHolder(private val binding: ItemZipMediaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ZipMediaItem) {
            binding.ivMedia.visibility = android.view.View.VISIBLE
            binding.playerView.visibility = android.view.View.GONE
            binding.ivMedia.load(item.localPath) {
                crossfade(true)
                placeholder(R.drawable.ic_placeholder)
                error(R.drawable.ic_error)
            }
        }
    }

    class VideoViewHolder(
        private val binding: ItemZipMediaBinding,
        private val player: ExoPlayer
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ZipMediaItem) {
            binding.ivMedia.visibility = android.view.View.GONE
            binding.playerView.visibility = android.view.View.VISIBLE
            binding.playerView.player = player
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(java.io.File(item.localPath))))
            player.prepare()
        }
    }

    class ZipMediaItemDiffCallback : DiffUtil.ItemCallback<ZipMediaItem>() {
        override fun areItemsTheSame(oldItem: ZipMediaItem, newItem: ZipMediaItem): Boolean {
            return oldItem.localPath == newItem.localPath
        }

        override fun areContentsTheSame(oldItem: ZipMediaItem, newItem: ZipMediaItem): Boolean {
            return oldItem == newItem
        }
    }
}
