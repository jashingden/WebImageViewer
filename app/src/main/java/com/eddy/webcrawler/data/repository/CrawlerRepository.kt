package com.eddy.webcrawler.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.eddy.webcrawler.data.crawler.WebCrawler
import com.eddy.webcrawler.data.db.LinkEntryDao
import com.eddy.webcrawler.data.db.LinkIndexDao
import com.eddy.webcrawler.data.model.ContentItem
import com.eddy.webcrawler.data.model.DownloadStatus
import com.eddy.webcrawler.data.model.IndexWithThumbnail
import com.eddy.webcrawler.data.model.LinkEntry
import com.eddy.webcrawler.data.model.LinkIndex
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
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

    suspend fun crawl(url: String, pattern: String, rule: String): Result<CrawlResult> = runCatching {
        val crawlData = webCrawler.fetchAndParse(url, pattern)

        if (crawlData.aEntries.isNotEmpty()) {
            // 首頁邏輯：不儲存首頁本身的內容，改為爬取所有子連結的內容
            var linkIndexId: Long = 0
            var totalEntries = 0
            crawlData.aEntries.forEach { entryData ->
                runCatching {
                    val subData = webCrawler.fetchAndParse(entryData.url, if (rule.isNotBlank()) rule else pattern)
                    val allEntries = subData.aEntries + subData.imgEntries

                    val linkIndex = LinkIndex(
                        sourceUrl = entryData.url,
                        filterPattern = if (rule.isNotBlank()) rule else pattern,
                        title = subData.title,
                        crawlTimestamp = System.currentTimeMillis(),
                        status = if (allEntries.isEmpty()) "EMPTY" else "SUCCESS"
                    )
                    linkIndexId = linkIndexDao.insertIndex(linkIndex)
                    if (linkIndexId == -1L) {
                        linkIndexId = linkIndexDao.getIndexByUrl(entryData.url)?.id ?: 0L
                    }

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

            var linkIndexId = linkIndexDao.insertIndex(linkIndex)
            if (linkIndexId == -1L) {
                linkIndexId = linkIndexDao.getIndexByUrl(url)?.id
                    ?: throw IllegalStateException("Failed to insert or retrieve link index")
            }

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

    suspend fun getIndexById(id: Long): LinkIndex? = linkIndexDao.getIndexById(id)

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

    suspend fun deleteIndexWithFiles(indexId: Long) = withContext(Dispatchers.IO) {
        val index = linkIndexDao.getIndexById(indexId) ?: return@withContext
        val entries = linkEntryDao.getEntriesByIndexIdList(indexId)
        
        entries.forEach { entry ->
            entry.localPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
        
        // Delete the images directory for this index if it exists
        val imagesDir = context.filesDir.resolve("images/$indexId")
        if (imagesDir.exists()) {
            imagesDir.deleteRecursively()
        }

        val indexDir = File(context.filesDir, indexId.toString())
        if (indexDir.exists()) {
            indexDir.deleteRecursively()
        }

        linkEntryDao.deleteEntriesByIndexId(indexId)
        linkIndexDao.deleteIndex(index)
    }

    suspend fun deleteEntry(entryId: Long) = withContext(Dispatchers.IO) {
        val entry = linkEntryDao.getEntryById(entryId) ?: return@withContext
        entry.localPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        linkEntryDao.deleteEntryById(entryId)
    }

    suspend fun deleteEntries(entryIds: List<Long>) = withContext(Dispatchers.IO) {
        entryIds.forEach { entryId ->
            val entry = linkEntryDao.getEntryById(entryId)
            entry?.localPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
        linkEntryDao.deleteEntriesByIds(entryIds)
    }

    fun getIndicesWithThumbnails(): Flow<List<IndexWithThumbnail>> {
        return linkIndexDao.getIndicesWithThumbnails()
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
                    "HTML" -> ContentItem.HtmlItem(
                        stableId = entry.id.toString(),
                        url = entry.url,
                        displayName = entry.displayName
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

    suspend fun importArchive(uri: android.net.Uri): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val archiveFileName = getFileNameFromUri(context, uri)
            val tempArchiveFile = File(context.cacheDir, "temp_import_${System.currentTimeMillis()}")

            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempArchiveFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalStateException("無法讀取選擇的檔案")

                val format = detectArchiveFormat(tempArchiveFile, archiveFileName)
                if (format == ArchiveFormat.UNKNOWN) {
                    throw IllegalArgumentException("不支援的檔案格式，請選擇 zip、7z 或 tar.gz 格式的壓縮檔")
                }

                val tempExtractDir = File(context.filesDir, "temp_extract_${System.currentTimeMillis()}")
                if (!tempExtractDir.exists()) tempExtractDir.mkdirs()

                try {
                    when (format) {
                        ArchiveFormat.ZIP -> extractZip(tempArchiveFile, tempExtractDir)
                        ArchiveFormat.SEVEN_Z -> extract7z(tempArchiveFile, tempExtractDir)
                        ArchiveFormat.TAR_GZ -> extractTarGz(tempArchiveFile, tempExtractDir)
                        ArchiveFormat.UNKNOWN -> throw IllegalArgumentException("不支援的檔案格式")
                    }

                    val topLevelItems = tempExtractDir.listFiles()?.filter {
                        !it.name.startsWith(".") && !it.name.equals("__MACOSX", ignoreCase = true)
                    } ?: emptyList()

                    val (extractedFolder, title) = if (topLevelItems.size == 1 && topLevelItems[0].isDirectory) {
                        val folder = topLevelItems[0]
                        Pair(folder, folder.name)
                    } else {
                        val baseName = archiveFileName
                            .substringBeforeLast('.')
                            .let { if (it.lowercase().endsWith(".tar")) it.substringBeforeLast('.') else it }
                            .ifBlank { "Archive" }
                        Pair(tempExtractDir, baseName)
                    }

                    val linkIndex = LinkIndex(
                        sourceUrl = "file://$archiveFileName-${System.currentTimeMillis()}",
                        filterPattern = ".*",
                        title = title,
                        crawlTimestamp = System.currentTimeMillis(),
                        status = "SUCCESS"
                    )

                    var linkIndexId = linkIndexDao.insertIndex(linkIndex)
                    if (linkIndexId == -1L) {
                        linkIndexId = linkIndexDao.getIndexByUrl(linkIndex.sourceUrl)?.id
                            ?: throw IllegalStateException("無法建立索引紀錄")
                    }

                    val targetFolder = File(context.filesDir, linkIndexId.toString())
                    if (targetFolder.exists()) {
                        targetFolder.deleteRecursively()
                    }

                    if (extractedFolder != tempExtractDir) {
                        if (!extractedFolder.renameTo(targetFolder)) {
                            extractedFolder.copyRecursively(targetFolder, overwrite = true)
                            extractedFolder.deleteRecursively()
                        }
                    } else {
                        if (!tempExtractDir.renameTo(targetFolder)) {
                            tempExtractDir.copyRecursively(targetFolder, overwrite = true)
                            tempExtractDir.deleteRecursively()
                        }
                    }

                    val imageFiles = findImageFiles(targetFolder)
                    if (imageFiles.isEmpty()) {
                        linkIndexDao.deleteIndex(linkIndex.copy(id = linkIndexId))
                        targetFolder.deleteRecursively()
                        throw IllegalStateException("壓縮檔內找不到任何圖片檔案")
                    }

                    val entries = imageFiles.map { imgFile ->
                        LinkEntry(
                            linkIndexId = linkIndexId,
                            displayName = imgFile.name,
                            url = "file://${imgFile.absolutePath}",
                            type = "IMAGE",
                            fileExtension = imgFile.extension.lowercase(),
                            downloadStatus = "EXTRACTED",
                            localPath = imgFile.absolutePath
                        )
                    }

                    linkEntryDao.insertEntries(entries)

                    linkIndexId
                } finally {
                    if (tempExtractDir.exists()) {
                        tempExtractDir.deleteRecursively()
                    }
                }
            } finally {
                if (tempArchiveFile.exists()) {
                    tempArchiveFile.delete()
                }
            }
        }
    }

    private fun getFileNameFromUri(context: Context, uri: android.net.Uri): String {
        var name = ""
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex) ?: ""
                }
            }
        }
        if (name.isBlank()) {
            name = uri.lastPathSegment ?: "archive.zip"
        }
        return name
    }

    private enum class ArchiveFormat {
        ZIP, SEVEN_Z, TAR_GZ, UNKNOWN
    }

    private fun detectArchiveFormat(file: File, fileName: String): ArchiveFormat {
        val lowerName = fileName.lowercase()
        if (lowerName.endsWith(".7z")) return ArchiveFormat.SEVEN_Z
        if (lowerName.endsWith(".tar.gz") || lowerName.endsWith(".tgz")) return ArchiveFormat.TAR_GZ
        if (lowerName.endsWith(".zip")) return ArchiveFormat.ZIP

        if (file.length() >= 6) {
            val bytes = ByteArray(6)
            java.io.FileInputStream(file).use { it.read(bytes) }
            // 7z: 37 7A BC AF 27 1C
            if (bytes[0] == 0x37.toByte() && bytes[1] == 0x7A.toByte() && bytes[2] == 0xBC.toByte() &&
                bytes[3] == 0xAF.toByte() && bytes[4] == 0x27.toByte() && bytes[5] == 0x1C.toByte()) {
                return ArchiveFormat.SEVEN_Z
            }
            // Gzip: 1F 8B
            if (bytes[0] == 0x1F.toByte() && bytes[1] == 0x8B.toByte()) {
                return ArchiveFormat.TAR_GZ
            }
            // Zip: PK (50 4B)
            if (bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
                return ArchiveFormat.ZIP
            }
        }
        return ArchiveFormat.UNKNOWN
    }

    private fun extractZip(zipFile: File, destDir: File) {
        ZipArchiveInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val targetFile = File(destDir, entry.name)
                validateZipSlip(destDir, targetFile)
                if (entry.isDirectory) {
                    targetFile.mkdirs()
                } else {
                    targetFile.parentFile?.mkdirs()
                    FileOutputStream(targetFile).use { output ->
                        zis.copyTo(output)
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun extractTarGz(tarGzFile: File, destDir: File) {
        val lowerName = tarGzFile.name.lowercase()
        val isGzip = lowerName.endsWith(".gz") || lowerName.endsWith(".tgz")
        val inputStream: InputStream = if (isGzip) {
            GzipCompressorInputStream(BufferedInputStream(FileInputStream(tarGzFile)))
        } else {
            BufferedInputStream(FileInputStream(tarGzFile))
        }

        inputStream.use { inStream ->
            TarArchiveInputStream(inStream).use { tis ->
                var entry = tis.nextEntry
                while (entry != null) {
                    val targetFile = File(destDir, entry.name)
                    validateZipSlip(destDir, targetFile)
                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()
                        FileOutputStream(targetFile).use { output ->
                            tis.copyTo(output)
                        }
                    }
                    entry = tis.nextEntry
                }
            }
        }
    }

    private fun extract7z(sevenZFile: File, destDir: File) {
        SevenZFile(sevenZFile).use { szf ->
            var entry = szf.nextEntry
            while (entry != null) {
                val targetFile = File(destDir, entry.name)
                validateZipSlip(destDir, targetFile)
                if (entry.isDirectory) {
                    targetFile.mkdirs()
                } else {
                    targetFile.parentFile?.mkdirs()
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(8192)
                        var count: Int
                        while (szf.read(buffer).also { count = it } != -1) {
                            output.write(buffer, 0, count)
                        }
                    }
                }
                entry = szf.nextEntry
            }
        }
    }

    private fun validateZipSlip(destDir: File, targetFile: File) {
        val canonicalDest = destDir.canonicalPath
        val canonicalTarget = targetFile.canonicalPath
        if (!canonicalTarget.startsWith(canonicalDest + File.separator) && canonicalTarget != canonicalDest) {
            throw SecurityException("Illegal archive entry path traversal: ${targetFile.name}")
        }
    }

    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic", "heif")

    private fun findImageFiles(dir: File): List<File> {
        val result = mutableListOf<File>()
        val files = dir.listFiles() ?: return result
        for (file in files) {
            if (file.isDirectory) {
                if (!file.name.startsWith(".") && !file.name.equals("__MACOSX", ignoreCase = true)) {
                    result.addAll(findImageFiles(file))
                }
            } else if (file.isFile) {
                if (file.extension.lowercase() in imageExtensions) {
                    result.add(file)
                }
            }
        }
        result.sortBy { it.name }
        return result
    }
}
