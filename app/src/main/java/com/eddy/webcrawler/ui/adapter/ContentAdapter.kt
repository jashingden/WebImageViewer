package com.eddy.webcrawler.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.eddy.webcrawler.R
import com.eddy.webcrawler.data.model.ContentItem
import com.eddy.webcrawler.data.model.DownloadStatus
import com.eddy.webcrawler.databinding.ItemImageBinding
import com.eddy.webcrawler.databinding.ItemLinkBinding
import com.eddy.webcrawler.databinding.ItemZipDownloadBinding

class ContentAdapter(
    private val onDownloadClick: (ContentItem.DownloadItem) -> Unit = {},
    private val onViewZipClick: (ContentItem.DownloadItem) -> Unit = {}
) : ListAdapter<ContentItem, RecyclerView.ViewHolder>(ContentItemDiffCallback()) {

    companion object {
        const val TYPE_IMAGE = 0
        const val TYPE_LINK = 1
        const val TYPE_DOWNLOAD = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ContentItem.ImageItem -> TYPE_IMAGE
            is ContentItem.LinkItem -> TYPE_LINK
            is ContentItem.DownloadItem -> TYPE_DOWNLOAD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_IMAGE -> ImageViewHolder(ItemImageBinding.inflate(inflater, parent, false))
            TYPE_LINK -> LinkViewHolder(ItemLinkBinding.inflate(inflater, parent, false))
            TYPE_DOWNLOAD -> DownloadViewHolder(ItemZipDownloadBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ContentItem.ImageItem -> (holder as ImageViewHolder).bind(item)
            is ContentItem.LinkItem -> (holder as LinkViewHolder).bind(item)
            is ContentItem.DownloadItem -> (holder as DownloadViewHolder).bind(item, onDownloadClick, onViewZipClick)
        }
    }

    class ImageViewHolder(private val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ContentItem.ImageItem) {
            binding.ivImage.load(item.url) {
                crossfade(true)
                placeholder(R.drawable.ic_placeholder)
                error(R.drawable.ic_error)
            }
        }
    }

    class LinkViewHolder(private val binding: ItemLinkBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ContentItem.LinkItem) {
            binding.tvLink.text = item.displayName
        }
    }

    class DownloadViewHolder(private val binding: ItemZipDownloadBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: ContentItem.DownloadItem,
            onDownloadClick: (ContentItem.DownloadItem) -> Unit,
            onViewZipClick: (ContentItem.DownloadItem) -> Unit
        ) {
            binding.tvDisplayName.text = item.displayName

            when (item.downloadStatus) {
                DownloadStatus.NOT_DOWNLOADED -> {
                    binding.tvStatus.text = "尚未下載"
                    binding.btnAction.text = "下載 ZIP"
                    binding.btnAction.isEnabled = true
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnAction.setOnClickListener { onDownloadClick(item) }
                }
                DownloadStatus.DOWNLOADING -> {
                    binding.tvStatus.text = "下載中…"
                    binding.btnAction.isEnabled = false
                    binding.progressBar.visibility = android.view.View.VISIBLE
                    binding.progressBar.isIndeterminate = true
                }
                DownloadStatus.DOWNLOADED -> {
                    binding.tvStatus.text = "下載完成，解壓縮中…"
                    binding.btnAction.isEnabled = false
                    binding.progressBar.visibility = android.view.View.VISIBLE
                    binding.progressBar.isIndeterminate = true
                }
                DownloadStatus.EXTRACTED -> {
                    binding.tvStatus.text = "已解壓縮"
                    binding.btnAction.text = "檢視 ZIP"
                    binding.btnAction.isEnabled = true
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnAction.setOnClickListener { onViewZipClick(item) }
                }
                DownloadStatus.FAILED -> {
                    binding.tvStatus.text = "下載失敗"
                    binding.btnAction.text = "重試"
                    binding.btnAction.isEnabled = true
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnAction.setOnClickListener { onDownloadClick(item) }
                }
            }
        }
    }

    class ContentItemDiffCallback : DiffUtil.ItemCallback<ContentItem>() {
        override fun areItemsTheSame(oldItem: ContentItem, newItem: ContentItem): Boolean {
            return oldItem.stableId == newItem.stableId
        }

        override fun areContentsTheSame(oldItem: ContentItem, newItem: ContentItem): Boolean {
            return oldItem == newItem
        }
    }
}
