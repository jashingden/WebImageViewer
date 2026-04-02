package com.example.webcrawler.data.crawler

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WebCrawlerTest {

    private lateinit var server: MockWebServer
    private lateinit var webCrawler: WebCrawler

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        val client = OkHttpClient.Builder().build()
        webCrawler = WebCrawler(client)
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `givenValidHtmlPage_whenCrawl_thenReturnsFilteredLinks`() = runBlocking {
        val html = """
            <html>
                <head><title>Test Page</title></head>
                <body>
                    <a href="/page1">Page 1</a>
                    <a href="/page2">Page 2</a>
                    <a href="https://external.com">External</a>
                    <img src="/image.jpg" alt="Test Image"/>
                </body>
            </html>
        """.trimIndent()

        server.enqueue(MockResponse().setBody(html).setHeader("Content-Type", "text/html"))

        val result = webCrawler.fetchAndParse(server.url("/").toString(), ".*")

        assertEquals("Test Page", result.title)
        assertTrue(result.entries.isNotEmpty())
    }

    @Test
    fun `givenInvalidUrl_whenCrawl_thenThrowsException`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))

        try {
            webCrawler.fetchAndParse(server.url("/").toString(), ".*")
            assert(false) { "Expected exception for 404 response" }
        } catch (e: RuntimeException) {
            assert(true)
        }
    }

    @Test
    fun `givenEmptyPage_whenCrawl_thenReturnsEmptyEntries`() = runBlocking {
        val html = "<html><head><title>Empty</title></head><body></body></html>"
        server.enqueue(MockResponse().setBody(html).setHeader("Content-Type", "text/html"))

        val result = webCrawler.fetchAndParse(server.url("/").toString(), ".*")

        assertEquals("Empty", result.title)
        assertEquals(0, result.entries.size)
    }

    @Test
    fun `givenPatternFilter_whenCrawl_thenReturnsOnlyMatchingLinks`() = runBlocking {
        val html = """
            <html>
                <body>
                    <a href="/images/photo.jpg">Photo</a>
                    <a href="/docs/readme.txt">Readme</a>
                    <a href="/images/logo.png">Logo</a>
                </body>
            </html>
        """.trimIndent()

        server.enqueue(MockResponse().setBody(html).setHeader("Content-Type", "text/html"))

        val result = webCrawler.fetchAndParse(server.url("/").toString(), ".*\\.jpg$")

        assertEquals(1, result.entries.size)
        assertTrue(result.entries[0].url.endsWith("photo.jpg"))
    }

    @Test
    fun `givenImageLinks_whenCrawl_thenClassifiedAsImageType`() = runBlocking {
        val html = """
            <html>
                <body>
                    <img src="/photo.jpg"/>
                    <img src="/icon.png"/>
                </body>
            </html>
        """.trimIndent()

        server.enqueue(MockResponse().setBody(html).setHeader("Content-Type", "text/html"))

        val result = webCrawler.fetchAndParse(server.url("/").toString(), ".*")

        assertEquals(2, result.entries.size)
        assertTrue(result.entries.all { it.type == "IMAGE" })
    }

    @Test
    fun `givenZipLinks_whenCrawl_thenClassifiedAsDownloadType`() = runBlocking {
        val html = """
            <html>
                <body>
                    <a href="/files/archive.zip">Archive</a>
                    <a href="/files/data.rar">Data</a>
                </body>
            </html>
        """.trimIndent()

        server.enqueue(MockResponse().setBody(html).setHeader("Content-Type", "text/html"))

        val result = webCrawler.fetchAndParse(server.url("/").toString(), ".*")

        assertEquals(2, result.entries.size)
        assertTrue(result.entries.all { it.type == "DOWNLOAD" })
    }
}
