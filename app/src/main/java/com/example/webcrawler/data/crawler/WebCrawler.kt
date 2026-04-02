package com.example.webcrawler.data.crawler

import com.example.webcrawler.data.model.LinkEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

data class CrawlData(
    val title: String,
    val entries: List<CrawlEntryData>
)

data class CrawlEntryData(
    val displayName: String,
    val url: String,
    val type: String,
    val fileExtension: String?
)

@Singleton
class WebCrawler @Inject constructor(
    private val client: OkHttpClient
) {

    companion object {
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg")
        private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mkv", "avi")
        private val DOWNLOAD_EXTENSIONS = setOf("zip", "rar", "7z", "tar", "gz")
    }

    suspend fun fetchAndParse(url: String, pattern: String): CrawlData = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("HTTP ${response.code}: ${response.message}")
            }

            val html = response.body?.string() ?: ""
            val document = Jsoup.parse(html, url)
            val title = document.title()

            val regex = pattern.toRegex()
            val entries = mutableListOf<CrawlEntryData>()

            document.select("a[href]").forEach { element ->
                val absHref = element.attr("abs:href")
                if (absHref.isNotBlank() && absHref.matches(regex)) {
                    val text = element.text().ifBlank { extractFilename(absHref) }
                    val ext = extractExtension(absHref)
                    val type = determineType(absHref, ext)
                    entries.add(CrawlEntryData(text, absHref, type, ext))
                }
            }

            document.select("img[src]").forEach { element ->
                val absSrc = element.attr("abs:src")
                if (absSrc.isNotBlank() && absSrc.matches(regex)) {
                    val alt = element.attr("alt").ifBlank { extractFilename(absSrc) }
                    val ext = extractExtension(absSrc)
                    entries.add(CrawlEntryData(alt, absSrc, "IMAGE", ext))
                }
            }

            CrawlData(title, entries)
        }
    }

    private fun determineType(url: String, extension: String?): String {
        val ext = extension?.lowercase()
        return when {
            ext in IMAGE_EXTENSIONS -> "IMAGE"
            ext in DOWNLOAD_EXTENSIONS -> "DOWNLOAD"
            else -> "LINK"
        }
    }

    private fun extractFilename(url: String): String {
        return url.substringAfterLast('/').substringBefore('?').ifBlank { url }
    }

    private fun extractExtension(url: String): String? {
        val filename = url.substringBefore('?').substringAfterLast('.')
        return filename.takeIf { it.isNotEmpty() && it != url }
    }
}
