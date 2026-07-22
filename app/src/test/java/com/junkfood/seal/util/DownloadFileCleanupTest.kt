package com.junkfood.seal.util

import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadFileCleanupTest {
    @Test
    fun `matches subtitles thumbnails and metadata with the same stem`() {
        val primary = "Episode.01 [abc123].mp4"

        listOf(
                "Episode.01 [abc123].en.vtt",
                "Episode.01 [abc123].info.json",
                "Episode.01 [abc123].description",
                "Episode.01 [abc123].webp",
                "Episode.01 [abc123].comments.json",
            )
            .forEach { assertTrue(it, isAssociatedDownloadSidecar(primary, it)) }
    }

    @Test
    fun `matches partial and downloader control files`() {
        val primary = "Video.mp4"

        listOf("Video.f137.mp4.part", "Video.f137.mp4.part-Frag12", "Video.mp4.ytdl").forEach {
            assertTrue(it, isAssociatedDownloadSidecar(primary, it))
        }
    }

    @Test
    fun `does not match other media or nearby downloads`() {
        val primary = "Video.mp4"

        listOf("Video.mp4", "Video.mp3", "Video 2.jpg", "Other.info.json", "video.en.vtt").forEach {
            assertFalse(it, isAssociatedDownloadSidecar(primary, it))
        }
    }

    @Test
    fun `deletes primary and sidecars but preserves other media`() {
        val directory = Files.createTempDirectory("walrus-cleanup").toFile()
        try {
            val primary = directory.resolve("Video.mp4").apply { writeText("video") }
            val subtitle = directory.resolve("Video.en.vtt").apply { writeText("subtitle") }
            val metadata = directory.resolve("Video.info.json").apply { writeText("metadata") }
            val otherMedia = directory.resolve("Video.mp3").apply { writeText("audio") }
            val nearbyThumbnail = directory.resolve("Video 2.jpg").apply { writeText("image") }

            val deletedPaths = deleteLocalDownloadWithSidecars(primary).toSet()

            assertTrue(primary.absolutePath in deletedPaths)
            assertTrue(subtitle.absolutePath in deletedPaths)
            assertTrue(metadata.absolutePath in deletedPaths)
            assertFalse(primary.exists())
            assertFalse(subtitle.exists())
            assertFalse(metadata.exists())
            assertTrue(otherMedia.exists())
            assertTrue(nearbyThumbnail.exists())
        } finally {
            directory.deleteRecursively()
        }
    }
}
