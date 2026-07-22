package com.junkfood.seal.util

import java.io.File

private val SIDECAR_EXTENSIONS =
    setOf(
        "aria2",
        "ass",
        "avif",
        "jpeg",
        "jpg",
        "json3",
        "lrc",
        "part",
        "png",
        "srv1",
        "srv2",
        "srv3",
        "srt",
        "ttml",
        "vtt",
        "webp",
        "ytdl",
    )

private val SIDECAR_SUFFIXES =
    setOf("annotations.xml", "comments.json", "description", "info.json", "live_chat.json")

/** Returns true only for known yt-dlp sidecars that share the primary media filename stem. */
internal fun isAssociatedDownloadSidecar(
    primaryFileName: String,
    candidateFileName: String,
): Boolean {
    if (primaryFileName == candidateFileName) return false

    val primaryStem = primaryFileName.substringBeforeLast('.', primaryFileName)
    val prefix = "$primaryStem."
    if (!candidateFileName.startsWith(prefix)) return false

    val suffix = candidateFileName.removePrefix(prefix).lowercase()
    val extension = suffix.substringAfterLast('.')
    return extension in SIDECAR_EXTENSIONS ||
        suffix in SIDECAR_SUFFIXES ||
        suffix.contains(".part-frag") ||
        suffix.startsWith("part-frag") ||
        suffix.startsWith("temp.")
}

internal fun deleteLocalDownloadWithSidecars(primaryFile: File): List<String> =
    buildList {
            add(primaryFile)
            primaryFile.parentFile
                ?.listFiles()
                ?.filter { it.isFile && isAssociatedDownloadSidecar(primaryFile.name, it.name) }
                ?.let(::addAll)
        }
        .distinctBy { it.absolutePath }
        .mapNotNull { file -> if (file.exists() && file.delete()) file.absolutePath else null }
