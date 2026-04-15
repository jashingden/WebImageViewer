package com.eddy.webcrawler.data.download

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.eddy.webcrawler.data.db.LinkEntryDao
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DownloadWorkerTest {

    private lateinit var server: MockWebServer
    private lateinit var worker: DownloadWorker
    private val linkEntryDao: LinkEntryDao = mockk()
    private lateinit var tempDir: File

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        tempDir = File.createTempFile("test_worker_", "").apply { delete() }.also { it.mkdirs() }
    }

    @After
    fun teardown() {
        server.shutdown()
        tempDir.deleteRecursively()
    }

    private fun createWorker(inputData: androidx.work.Data): DownloadWorker {
        val context = mockk<Context>(relaxed = true)
        val params = mockk<WorkerParameters>(relaxed = true)
        val client = OkHttpClient.Builder().build()
        
        coEvery { context.filesDir } returns tempDir
        coEvery { context.applicationContext } returns context
        coEvery { params.inputData } returns inputData
        
        return DownloadWorker(context, params, client, linkEntryDao)
    }

    @Test
    fun `givenValidZipUrl_whenDoWork_thenReturnsSuccess`() = runBlocking {
        val zipContent = createTestZip()
        server.enqueue(MockResponse().setBody(zipContent).setHeader("Content-Type", "application/zip"))

        val inputData = workDataOf(
            DownloadWorker.KEY_ENTRY_ID to 1L,
            DownloadWorker.KEY_LINK_INDEX_ID to 10L,
            DownloadWorker.KEY_URL to server.url("/test.zip").toString()
        )

        val worker = createWorker(inputData)
        coEvery { linkEntryDao.updateEntry(any()) } returns Unit

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
    }

    @Test
    fun `givenInvalidInput_whenDoWork_thenReturnsFailure`() = runBlocking {
        val inputData = workDataOf(
            DownloadWorker.KEY_ENTRY_ID to -1L,
            DownloadWorker.KEY_LINK_INDEX_ID to -1L,
            DownloadWorker.KEY_URL to ""
        )

        val worker = createWorker(inputData)
        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
    }

    @Test
    fun `givenMaliciousZipEntry_whenExtract_thenThrowsSecurityException`() = runBlocking {
        val maliciousZip = createMaliciousZip()
        server.enqueue(MockResponse().setBody(maliciousZip).setHeader("Content-Type", "application/zip"))

        val inputData = workDataOf(
            DownloadWorker.KEY_ENTRY_ID to 1L,
            DownloadWorker.KEY_LINK_INDEX_ID to 10L,
            DownloadWorker.KEY_URL to server.url("/malicious.zip").toString()
        )

        val worker = createWorker(inputData)
        coEvery { linkEntryDao.updateEntry(any()) } returns Unit

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
    }

    @Test
    fun `givenNetworkError_whenDoWork_thenReturnsFailure`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))

        val inputData = workDataOf(
            DownloadWorker.KEY_ENTRY_ID to 1L,
            DownloadWorker.KEY_LINK_INDEX_ID to 10L,
            DownloadWorker.KEY_URL to server.url("/error.zip").toString()
        )

        val worker = createWorker(inputData)
        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
    }

    private fun createTestZip(): ByteArray {
        val baos = java.io.ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("file1.txt"))
            zos.write("Hello World".toByteArray())
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("file2.txt"))
            zos.write("Test Content".toByteArray())
            zos.closeEntry()
        }
        return baos.toByteArray()
    }

    private fun createMaliciousZip(): ByteArray {
        val baos = java.io.ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("../escape.txt"))
            zos.write("Malicious content".toByteArray())
            zos.closeEntry()
        }
        return baos.toByteArray()
    }
}
