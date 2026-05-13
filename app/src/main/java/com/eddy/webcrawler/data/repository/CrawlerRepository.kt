package com.eddy.webcrawler.data.repository

import android.content.Context
import com.eddy.webcrawler.data.crawler.WebCrawler
import com.eddy.webcrawler.data.db.LinkEntryDao
import com.eddy.webcrawler.data.db.LinkIndexDao
import com.eddy.webcrawler.data.model.ContentItem
import com.eddy.webcrawler.data.model.DownloadStatus
import com.eddy.webcrawler.data.model.LinkEntry
import com.eddy.webcrawler.data.model.LinkIndex
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class CrawlResult(
    val linkIndexId: Long,
    val totalEntries: Int
)

@Singleton
class CrawlerRepository @Inject constructor(
    private val webCrawler: WebCrawler,
    private val linkIndexDao: LinkIndexDao,
    private val linkEntryDao: LinkEntryDao,
    private val client: OkHttpClient,
    @ApplicationContext private val context: Context
) {

    suspend fun downloadImage(indexId: Long, entryId: Long, url: String): String? = withContext(Dispatchers.IO) {
        val imagesDir = context.filesDir.resolve("images/$indexId")
        if (!imagesDir.exists()) imagesDir.mkdirs()

        val filename = url.substringAfterLast('/').substringBefore('?')
        val file = File(imagesDir, filename)

        if (file.exists()) return@withContext file.absolutePath

        try {
            val request = okhttp3.Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            val entry = linkEntryDao.getEntryById(entryId) ?: return@withContext file.absolutePath
            linkEntryDao.updateEntry(entry.copy(localPath = file.absolutePath))

            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    suspend fun crawl(url: String, pattern: String): Result<CrawlResult> = runCatching {
        val crawlData = webCrawler.fetchAndParse(url, pattern)

        if (crawlData.aEntries.isNotEmpty()) {
            // 首頁邏輯：不儲存首頁本身的內容，改為爬取所有子連結的內容
            var linkIndexId: Long = 0
            var totalEntries = 0
            crawlData.aEntries.forEach { entryData ->
                runCatching {
                    val subData = webCrawler.fetchAndParse(entryData.url, pattern)
                    val allEntries = subData.aEntries + subData.imgEntries

                    val linkIndex = LinkIndex(
                        sourceUrl = entryData.url,
                        filterPattern = pattern,
                        title = subData.title,
                        crawlTimestamp = System.currentTimeMillis(),
                        status = if (allEntries.isEmpty()) "EMPTY" else "SUCCESS"
                    )
                    linkIndexId = linkIndexDao.insertIndex(linkIndex)

                    val subA = subData.aEntries.map { subEntry ->
                        LinkEntry(
                            linkIndexId = linkIndexId,
                            displayName = subEntry.displayName,
                            url = subEntry.url,
                            type = subEntry.type,
                            fileExtension = subEntry.fileExtension
                        )
                    }
                    val subImg = subData.imgEntries.map { subEntry ->
                        LinkEntry(
                            linkIndexId = linkIndexId,
                            displayName = subEntry.displayName,
                            url = subEntry.url,
                            type = subEntry.type,
                            fileExtension = subEntry.fileExtension
                        )
                    }

                    if (subA.isNotEmpty() || subImg.isNotEmpty()) {
                        linkEntryDao.insertEntries(subA + subImg)
                        totalEntries += subA.size + subImg.size
                    }
                }
            }

            CrawlResult(
                linkIndexId = linkIndexId,
                totalEntries
            )
        } else {
            // 原本的邏輯：不是首頁，直接儲存目前的內容
            val allEntries = crawlData.aEntries + crawlData.imgEntries

            val linkIndex = LinkIndex(
                sourceUrl = url,
                filterPattern = pattern,
                title = crawlData.title,
                crawlTimestamp = System.currentTimeMillis(),
                status = if (allEntries.isEmpty()) "EMPTY" else "SUCCESS"
            )

            val linkIndexId = linkIndexDao.insertIndex(linkIndex)

            val aEntries = crawlData.aEntries.map { entryData ->
                LinkEntry(
                    linkIndexId = linkIndexId,
                    displayName = entryData.displayName,
                    url = entryData.url,
                    type = entryData.type,
                    fileExtension = entryData.fileExtension
                )
            }

            val imgEntries = crawlData.imgEntries.map { entryData ->
                LinkEntry(
                    linkIndexId = linkIndexId,
                    displayName = entryData.displayName,
                    url = entryData.url,
                    type = entryData.type,
                    fileExtension = entryData.fileExtension
                )
            }

            if (aEntries.isNotEmpty() || imgEntries.isNotEmpty()) {
                linkEntryDao.insertEntries(aEntries + imgEntries)
            }

            val savedIndex = linkIndexDao.getIndexById(linkIndexId)
                ?: throw IllegalStateException("Failed to retrieve saved link index")

            CrawlResult(
                linkIndexId = linkIndexId,
                allEntries.size
            )
        }
    }

    fun getAllIndices(): Flow<List<LinkIndex>> = linkIndexDao.getAllIndices()

    fun getEntriesByIndexId(indexId: Long): Flow<List<LinkEntry>> =
        linkEntryDao.getEntriesByIndexId(indexId)

    fun getImagesByIndexId(indexId: Long): Flow<List<LinkEntry>> =
        linkEntryDao.getImagesByIndexId(indexId)

    fun getDownloadsByIndexId(indexId: Long): Flow<List<LinkEntry>> =
        linkEntryDao.getDownloadsByIndexId(indexId)

    suspend fun updateEntry(entry: LinkEntry) {
        linkEntryDao.updateEntry(entry)
    }

    suspend fun deleteIndex(index: LinkIndex) {
        linkIndexDao.deleteIndex(index)
    }

    fun getEntriesAsContentItems(indexId: Long): Flow<List<ContentItem>> {
        return linkEntryDao.getEntriesByIndexId(indexId).map { entries ->
            entries.map { entry ->
                when (entry.type) {
                    "IMAGE" -> ContentItem.ImageItem(
                        stableId = entry.id.toString(),
                        url = entry.url,
                        displayName = entry.displayName,
                        fileExtension = entry.fileExtension,
                        localPath = entry.localPath
                    )
                    "DOWNLOAD" -> ContentItem.DownloadItem(
                        stableId = entry.id.toString(),
                        url = entry.url,
                        displayName = entry.displayName,
                        fileExtension = entry.fileExtension ?: "",
                        downloadStatus = parseDownloadStatus(entry.downloadStatus),
                        localPath = entry.localPath
                    )
                    else -> ContentItem.LinkItem(
                        stableId = entry.id.toString(),
                        url = entry.url,
                        displayName = entry.displayName
                    )
                }
            }
        }
    }

    private fun parseDownloadStatus(status: String?): DownloadStatus {
        return try {
            status?.let { DownloadStatus.valueOf(it) } ?: DownloadStatus.NOT_DOWNLOADED
        } catch (e: IllegalArgumentException) {
            DownloadStatus.NOT_DOWNLOADED
        }
    }
}
