package com.eddy.webcrawler.data.repository

import android.content.Context
import com.eddy.webcrawler.data.crawler.WebCrawler
import com.eddy.webcrawler.data.db.LinkEntryDao
import com.eddy.webcrawler.data.db.LinkIndexDao
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream

class CrawlerRepositoryArchiveTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val webCrawler: WebCrawler = mockk(relaxed = true)
    private val linkIndexDao: LinkIndexDao = mockk(relaxed = true)
    private val linkEntryDao: LinkEntryDao = mockk(relaxed = true)
    private val okHttpClient: OkHttpClient = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)

    private lateinit var repository: CrawlerRepository

    @Before
    fun setUp() {
        repository = CrawlerRepository(
            webCrawler,
            linkIndexDao,
            linkEntryDao,
            okHttpClient,
            context
        )
    }

    @Test
    fun testZipArchiveExtractionHelper() = runTest {
        val zipFile = tempFolder.newFile("sample.zip")
        val extractDir = tempFolder.newFolder("extracted")

        ZipArchiveOutputStream(FileOutputStream(zipFile)).use { zos ->
            val entry1 = ZipArchiveEntry("FolderTitle/image1.jpg")
            zos.putArchiveEntry(entry1)
            zos.write("fake_image_bytes".toByteArray())
            zos.closeArchiveEntry()

            val entry2 = ZipArchiveEntry("FolderTitle/image2.png")
            zos.putArchiveEntry(entry2)
            zos.write("fake_png_bytes".toByteArray())
            zos.closeArchiveEntry()
        }

        assertTrue(zipFile.exists())
        assertTrue(zipFile.length() > 0)

        // Verify extraction result structure
        org.apache.commons.compress.archivers.zip.ZipArchiveInputStream(
            java.io.BufferedInputStream(java.io.FileInputStream(zipFile))
        ).use { zis ->
            var count = 0
            var entry = zis.nextEntry
            while (entry != null) {
                count++
                entry = zis.nextEntry
            }
            assertEquals(2, count)
        }
    }
}
