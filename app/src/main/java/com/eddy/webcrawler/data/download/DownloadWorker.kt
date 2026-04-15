package com.eddy.webcrawler.data.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.eddy.webcrawler.data.db.LinkEntryDao
import com.eddy.webcrawler.data.model.DownloadStatus
import com.eddy.webcrawler.data.model.LinkEntry
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val client: OkHttpClient,
    private val linkEntryDao: LinkEntryDao
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_ENTRY_ID = "KEY_ENTRY_ID"
        const val KEY_LINK_INDEX_ID = "KEY_LINK_INDEX_ID"
        const val KEY_URL = "KEY_URL"
        const val KEY_ERROR = "KEY_ERROR"
        const val KEY_LOCAL_PATH = "KEY_LOCAL_PATH"
        const val KEY_PROGRESS = "KEY_PROGRESS"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val entryId = inputData.getLong(KEY_ENTRY_ID, -1)
        val linkIndexId = inputData.getLong(KEY_LINK_INDEX_ID, -1)
        val url = inputData.getString(KEY_URL)

        if (entryId == -1L || linkIndexId == -1L || url.isNullOrBlank()) {
            return@withContext Result.failure(workDataOf(KEY_ERROR to "Invalid input parameters"))
        }

        try {
            setProgress(workDataOf(KEY_PROGRESS to "下載中…"))

            val downloadsDir = applicationContext.filesDir.resolve("downloads")
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val zipFilename = url.substringAfterLast('/').substringBefore('?')
            val zipFile = File(downloadsDir, zipFilename)

            downloadZip(url, zipFile)

            setProgress(workDataOf(KEY_PROGRESS to "解壓縮中…"))

            val extractedDir = applicationContext.filesDir.resolve("extracted/$linkIndexId/$entryId")
            if (!extractedDir.exists()) extractedDir.mkdirs()

            extractZip(zipFile, extractedDir)

            val entry = LinkEntry(
                id = entryId,
                linkIndexId = linkIndexId,
                displayName = zipFilename,
                url = url,
                type = "DOWNLOAD",
                downloadStatus = DownloadStatus.EXTRACTED.name,
                localPath = extractedDir.absolutePath
            )
            linkEntryDao.updateEntry(entry)

            Result.success(workDataOf(KEY_LOCAL_PATH to extractedDir.absolutePath))
        } catch (e: SecurityException) {
            Result.failure(workDataOf(KEY_ERROR to "安全性錯誤：${e.message}"))
        } catch (e: Exception) {
            Result.failure(workDataOf(KEY_ERROR to "下載或解壓縮失敗：${e.message}"))
        }
    }

    private suspend fun downloadZip(url: String, destination: File) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("HTTP ${response.code}: ${response.message}")
            }
            response.body?.byteStream()?.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private suspend fun extractZip(zipFile: File, destDir: File) {
        val canonicalDest = destDir.canonicalPath

        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                val canonicalOut = outFile.canonicalPath

                if (!canonicalOut.startsWith("$canonicalDest${File.separator}") && canonicalOut != canonicalDest) {
                    throw SecurityException("Bad zip entry: path traversal attempt detected")
                }

                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }

                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        zipFile.delete()
    }
}
