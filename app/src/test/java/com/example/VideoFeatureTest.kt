package com.example

import com.example.data.model.Video
import com.example.util.MediaHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoFeatureTest {

    @Test
    fun `test video view count and like count formatting`() {
        val lowVideo = Video(viewsCount = 350L, likesCount = 85L)
        assertEquals("350", lowVideo.formattedViews)
        assertEquals("85", lowVideo.formattedLikes)

        val thousandVideo = Video(viewsCount = 1500L, likesCount = 2300L)
        assertEquals("1.5K", thousandVideo.formattedViews)
        assertEquals("2.3K", thousandVideo.formattedLikes)

        val millionVideo = Video(viewsCount = 2_500_000L, likesCount = 1_200_000L)
        assertEquals("2.5M", millionVideo.formattedViews)
        assertEquals("1.2M", millionVideo.formattedLikes)
    }

    @Test
    fun `test video max size constraint is 100MB`() {
        val maxAllowed = MediaHelper.MAX_VIDEO_SIZE_BYTES
        val expected100Mb = 100L * 1024L * 1024L
        assertEquals(expected100Mb, maxAllowed)

        val validSize = 50L * 1024L * 1024L
        val oversized = 105L * 1024L * 1024L

        assertTrue(validSize <= MediaHelper.MAX_VIDEO_SIZE_BYTES)
        assertFalse(oversized <= MediaHelper.MAX_VIDEO_SIZE_BYTES)
    }

    @Test
    fun `test view threshold seconds is 3 seconds minimum`() {
        assertEquals(3L, MediaHelper.VIEW_THRESHOLD_SECONDS)
    }

    @Test
    fun `test categories list contains required categories`() {
        val required = listOf(
            "Divertissement",
            "Musique",
            "Sport",
            "Éducation",
            "Humour",
            "Actualités",
            "Mode",
            "Cuisine",
            "Technologie",
            "Autre"
        )
        for (cat in required) {
            assertTrue(Video.CATEGORIES.contains(cat))
        }
    }
}
