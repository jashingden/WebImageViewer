package com.eddy.webcrawler.ui.viewmodel

import com.eddy.webcrawler.data.model.ContentItem
import com.eddy.webcrawler.data.model.DownloadStatus
import com.eddy.webcrawler.data.model.LinkEntry
import com.eddy.webcrawler.data.repository.CrawlerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class BrowseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: BrowseViewModel
    private val repository: CrawlerRepository = mockk()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = BrowseViewModel(repository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `givenInitialState_whenCreated_thenPageStateIsIdle`() {
        assertEquals(PageState.Idle, viewModel.pageState.value)
    }

    @Test
    fun `givenValidIndexId_whenLoadContent_thenEmitsSuccessState`() = runTest {
        val contentItems = listOf(
            ContentItem.ImageItem("1", "https://example.com/image.jpg", "Image 1"),
            ContentItem.LinkItem("2", "https://example.com/page", "Page Link")
        )
        coEvery { repository.getEntriesAsContentItems(1L) } returns flowOf(contentItems)

        viewModel.loadContent(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.pageState.value
        assertTrue(state is PageState.Success)
        assertEquals(2, (state as PageState.Success).content.size)
    }

    @Test
    fun `givenRepositoryError_whenLoadContent_thenEmitsErrorState`() = runTest {
        coEvery { repository.getEntriesAsContentItems(1L) } returns flowOf { throw RuntimeException("DB error") }

        viewModel.loadContent(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.pageState.value is PageState.Error)
    }

    @Test
    fun `givenDownloadItem_whenStartDownload_thenUpdatesEntryStatus`() = runTest {
        coEvery { repository.updateEntry(any()) } returns Unit

        viewModel.startDownload(1L, 10L, "https://example.com/file.zip")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.updateEntry(any()) }
    }

    @Test
    fun `givenCompletedDownload_whenMarkDownloadComplete_thenUpdatesEntryWithExtractedStatus`() = runTest {
        coEvery { repository.updateEntry(any()) } returns Unit

        viewModel.markDownloadComplete(1L, 10L, "/data/extracted/1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.updateEntry(any()) }
    }

    @Test
    fun `givenFailedDownload_whenMarkDownloadFailed_thenUpdatesEntryWithFailedStatus`() = runTest {
        coEvery { repository.updateEntry(any()) } returns Unit

        viewModel.markDownloadFailed(1L, 10L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.updateEntry(any()) }
    }
}
