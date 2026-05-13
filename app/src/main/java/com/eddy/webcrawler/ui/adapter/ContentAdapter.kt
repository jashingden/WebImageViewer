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
    private val onViewZipClick: (ContentItem.DownloadItem) -> Unit = {},
    private val onHtmlClick: (ContentItem.HtmlItem) -> Unit = {}
) : ListAdapter<ContentItem, RecyclerView.ViewHolder>(ContentItemDiffCallback()) {

    companion object {
        const val TYPE_IMAGE = 0
        const val TYPE_LINK = 1
        const val TYPE_DOWNLOAD = 2
        const val TYPE_HTML = 3
    }

    override fun submitList(list: List<ContentItem>?) {
        val sortedList = list?.sortedBy { item ->
            when (item) {
                is ContentItem.ImageItem -> 0
                is ContentItem.HtmlItem -> 1
                is ContentItem.LinkItem -> 2
                is ContentItem.DownloadItem -> 3
            }
        }
        val filteredList = sortedList?.filter { item ->
            if (item is ContentItem.ImageItem) {
                val ext = item.fileExtension?.lowercase() ?: ""
                ext != "png" && ext != "svg"
            } else true
        }
        super.submitList(filteredList)
    }

    override fun submitList(list: List<ContentItem>?, commitCallback: Runnable?) {
        val sortedList = list?.sortedBy { item ->
            when (item) {
                is ContentItem.ImageItem -> 0
                is ContentItem.HtmlItem -> 1
                is ContentItem.LinkItem -> 2
                is ContentItem.DownloadItem -> 3
            }
        }
        val filteredList = sortedList?.filter { item ->
            if (item is ContentItem.ImageItem) {
                val ext = item.fileExtension?.lowercase() ?: ""
                ext != "png" && ext != "svg"
            } else true
        }
        super.submitList(filteredList, commitCallback)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ContentItem.ImageItem -> TYPE_IMAGE
            is ContentItem.LinkItem -> TYPE_LINK
            is ContentItem.DownloadItem -> TYPE_DOWNLOAD
            is ContentItem.HtmlItem -> TYPE_HTML
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_IMAGE -> ImageViewHolder(ItemImageBinding.inflate(inflater, parent, false))
            TYPE_LINK -> LinkViewHolder(ItemLinkBinding.inflate(inflater, parent, false))
            TYPE_DOWNLOAD -> DownloadViewHolder(ItemZipDownloadBinding.inflate(inflater, parent, false))
            TYPE_HTML -> HtmlViewHolder(ItemLinkBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ContentItem.ImageItem -> (holder as ImageViewHolder).bind(item)
            is ContentItem.LinkItem -> (holder as LinkViewHolder).bind(item)
            is ContentItem.DownloadItem -> (holder as DownloadViewHolder).bind(item, onDownloadClick, onViewZipClick)
            is ContentItem.HtmlItem -> (holder as HtmlViewHolder).bind(item, onHtmlClick)
        }
    }

    class ImageViewHolder(private val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ContentItem.ImageItem) {
            val imageSource = if (item.localPath != null) {
                java.io.File(item.localPath)
            } else {
                item.url
            }
            binding.ivImage.load(imageSource) {
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

    class HtmlViewHolder(private val binding: ItemLinkBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ContentItem.HtmlItem, onHtmlClick: (ContentItem.HtmlItem) -> Unit) {
            binding.tvLink.text = item.displayName
            binding.root.setOnClickListener { onHtmlClick(item) }
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
