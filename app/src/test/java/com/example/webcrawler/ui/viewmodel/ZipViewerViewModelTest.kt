package com.example.webcrawler.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ZipViewerViewModelTest {

    @Test
    fun `givenInitialState_whenCreated_thenZipViewerStateIsIdle`() {
        val viewModel = ZipViewerViewModel()
        assertEquals(ZipViewerState.Idle, viewModel.zipViewerState.value)
    }

    @Test
    fun `givenNonExistentDirectory_whenScanMedia_thenReturnsErrorState`() {
        val viewModel = ZipViewerViewModel()
        viewModel.scanMedia("/nonexistent/path")

        val state = viewModel.zipViewerState.value
        assertTrue(state is ZipViewerState.Error)
    }

    @Test
    fun `givenEmptyDirectory_whenScanMedia_thenReturnsErrorState`() {
        val tempDir = File.createTempFile("test_empty_", "").apply { delete() }.also { it.mkdirs() }
        try {
            val viewModel = ZipViewerViewModel()
            viewModel.scanMedia(tempDir.absolutePath)

            val state = viewModel.zipViewerState.value
            assertTrue(state is ZipViewerState.Error)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `givenDirectoryWithImages_whenScanMedia_thenReturnsSuccessWithImageItems`() {
        val tempDir = File.createTempFile("test_images_", "").apply { delete() }.also { it.mkdirs() }
        try {
            File(tempDir, "photo.jpg").createNewFile()
            File(tempDir, "icon.png").createNewFile()

            val viewModel = ZipViewerViewModel()
            viewModel.scanMedia(tempDir.absolutePath)

            val state = viewModel.zipViewerState.value
            assertTrue(state is ZipViewerState.Success)
            assertEquals(2, (state as ZipViewerState.Success).mediaItems.size)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `givenDirectoryWithVideos_whenScanMedia_thenReturnsSuccessWithVideoItems`() {
        val tempDir = File.createTempFile("test_videos_", "").apply { delete() }.also { it.mkdirs() }
        try {
            File(tempDir, "video.mp4").createNewFile()
            File(tempDir, "clip.webm").createNewFile()

            val viewModel = ZipViewerViewModel()
            viewModel.scanMedia(tempDir.absolutePath)

            val state = viewModel.zipViewerState.value
            assertTrue(state is ZipViewerState.Success)
            assertEquals(2, (state as ZipViewerState.Success).mediaItems.size)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `givenMixedDirectory_whenScanMedia_thenReturnsBothImageAndVideoItems`() {
        val tempDir = File.createTempFile("test_mixed_", "").apply { delete() }.also { it.mkdirs() }
        try {
            File(tempDir, "photo.jpg").createNewFile()
            File(tempDir, "video.mp4").createNewFile()
            File(tempDir, "readme.txt").createNewFile()

            val viewModel = ZipViewerViewModel()
            viewModel.scanMedia(tempDir.absolutePath)

            val state = viewModel.zipViewerState.value
            assertTrue(state is ZipViewerState.Success)
            assertEquals(2, (state as ZipViewerState.Success).mediaItems.size)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
