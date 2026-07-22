package com.junkfood.seal.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubeChannelUrlTest {
    @Test
    fun `bare handle defaults to videos`() {
        val source = "https://youtube.com/@WalrusProject".toYouTubeChannelSource()

        assertEquals("https://www.youtube.com/@WalrusProject", source?.baseUrl)
        assertEquals(YouTubeChannelTab.Videos, source?.initialTab)
        assertEquals(
            "https://www.youtube.com/@WalrusProject/shorts",
            source?.urlFor(YouTubeChannelTab.Shorts),
        )
    }

    @Test
    fun `channel tab and query are normalised`() {
        val source = "https://m.youtube.com/channel/UC123/streams?view=2".toYouTubeChannelSource()

        assertEquals("https://www.youtube.com/channel/UC123", source?.baseUrl)
        assertEquals(YouTubeChannelTab.Live, source?.initialTab)
    }

    @Test
    fun `legacy channel forms remain supported`() {
        assertEquals(
            YouTubeChannelTab.Shorts,
            "https://www.youtube.com/c/Creator/shorts".toYouTubeChannelSource()?.initialTab,
        )
        assertEquals(
            YouTubeChannelTab.Videos,
            "https://www.youtube.com/user/Creator/featured".toYouTubeChannelSource()?.initialTab,
        )
        assertEquals(
            YouTubeChannelTab.Videos,
            "youtube.com/@Creator".toYouTubeChannelSource()?.initialTab,
        )
    }

    @Test
    fun `video and playlist links are not channels`() {
        assertNull("https://youtu.be/id".toYouTubeChannelSource())
        assertNull("https://www.youtube.com/watch?v=id".toYouTubeChannelSource())
        assertNull("https://www.youtube.com/playlist?list=id".toYouTubeChannelSource())
        assertNull("https://example.com/@creator".toYouTubeChannelSource())
    }
}
