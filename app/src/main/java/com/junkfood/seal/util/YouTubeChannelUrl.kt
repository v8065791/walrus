package com.junkfood.seal.util

import java.net.URI

enum class YouTubeChannelTab(val pathSegment: String) {
    Videos("videos"),
    Shorts("shorts"),
    Live("streams"),
}

data class YouTubeChannelSource(val baseUrl: String, val initialTab: YouTubeChannelTab) {
    fun urlFor(tab: YouTubeChannelTab): String = "$baseUrl/${tab.pathSegment}"
}

/**
 * Recognises YouTube channel roots and their media tabs without treating watch or playlist links as
 * channels. A bare channel URL defaults to the Videos tab so yt-dlp returns selectable media rather
 * than a list of channel-tab playlists.
 */
fun String.toYouTubeChannelSource(): YouTubeChannelSource? {
    val input = trim()
    val uri =
        runCatching { URI(if ("://" in input) input else "https://$input") }.getOrNull()
            ?: return null
    val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
    if (host != "youtube.com" && !host.endsWith(".youtube.com")) return null

    val segments = uri.path.orEmpty().split('/').filter(String::isNotBlank)
    if (segments.isEmpty()) return null

    val rootLength =
        when {
            segments.first().startsWith("@") && segments.first().length > 1 -> 1
            segments.first() in channelPrefixes && segments.size >= 2 -> 2
            else -> return null
        }
    if (segments.size > rootLength + 1) return null

    val requestedTab = segments.getOrNull(rootLength)
    val initialTab =
        when (requestedTab) {
            null,
            "featured",
            "videos" -> YouTubeChannelTab.Videos
            "shorts" -> YouTubeChannelTab.Shorts
            "streams",
            "live" -> YouTubeChannelTab.Live
            else -> return null
        }

    val rootPath = segments.take(rootLength).joinToString("/")
    return YouTubeChannelSource(
        baseUrl = "https://www.youtube.com/$rootPath",
        initialTab = initialTab,
    )
}

private val channelPrefixes = setOf("channel", "c", "user")
