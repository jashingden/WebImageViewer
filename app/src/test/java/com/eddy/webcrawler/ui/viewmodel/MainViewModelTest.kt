package com.eddy.webcrawler.ui.viewmodel

import com.eddy.webcrawler.data.repository.CrawlResult
import com.eddy.webcrawler.data.repository.CrawlerRepository
import com.eddy.webcrawler.data.model.LinkIndex
import com.eddy.webcrawler.data.model.LinkEntry
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: MainViewModel
    private val repository: CrawlerRepository = mockk()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MainViewModel(repository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `givenInitialState_whenCreated_thenCrawlStateIsIdle`() {
        assertEquals(CrawlState.Idle, viewModel.crawlState.value)
    }

    @Test
    fun `givenEmptyUrl_whenCrawl_thenReturnsErrorState`() = runTest {
        viewModel.crawl("", ".*")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.crawlState.value is CrawlState.Error)
    }

    @Test
    fun `givenInvalidUrl_whenCrawl_thenReturnsErrorState`() = runTest {
        viewModel.crawl("not-a-url", ".*")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.crawlState.value is CrawlState.Error)
    }

    @Test
    fun `givenValidUrl_whenCrawlSucceeds_thenReturnsSuccessState`() = runTest {
        val crawlResult = CrawlResult(
            linkIndexId = 1L,
            linkIndex = LinkIndex(
                id = 1L,
                sourceUrl = "https://example.com",
                filterPattern = ".*",
                title = "Test",
                crawlTimestamp = System.currentTimeMillis(),
                status = "SUCCESS"
            ),
            entries = listOf(
                LinkEntry(
                    id = 1L,
                    linkIndexId = 1L,
                    displayName = "Image",
                    url = "https://example.com/image.jpg",
                    type = "IMAGE"
                )
            )
        )
        coEvery { repository.crawl(any(), any()) } returns Result.success(crawlResult)

        viewModel.crawl("https://example.com", ".*")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.crawlState.value is CrawlState.Success)
    }

    @Test
    fun `givenValidUrl_whenCrawlFails_thenReturnsErrorState`() = runTest {
        coEvery { repository.crawl(any(), any()) } returns Result.failure(RuntimeException("Network error"))

        viewModel.crawl("https://example.com", ".*")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.crawlState.value is CrawlState.Error)
    }

    @Test
    fun `givenCrawlAction_whenCrawl_thenStateTransitionsIdleToLoadingToSuccess`() = runTest {
        val crawlResult = CrawlResult(
            linkIndexId = 1L,
            linkIndex = LinkIndex(
                id = 1L,
                sourceUrl = "https://example.com",
                filterPattern = ".*",
                title = "Test",
                crawlTimestamp = System.currentTimeMillis(),
                status = "SUCCESS"
            ),
            entries = emptyList()
        )
        coEvery { repository.crawl(any(), any()) } returns Result.success(crawlResult)

        val states = mutableListOf<CrawlState>()
        val job = kotlinx.coroutines.launch {
            viewModel.crawlState.collect { states.add(it) }
        }

        viewModel.crawl("https://example.com", ".*")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(states.contains(CrawlState.Loading))
        assertTrue(states.any { it is CrawlState.Success })

        job.cancel()
    }
}
