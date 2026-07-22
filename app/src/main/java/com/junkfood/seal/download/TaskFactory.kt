package com.junkfood.seal.download

import androidx.annotation.CheckResult
import com.junkfood.seal.download.Task.DownloadState.Idle
import com.junkfood.seal.download.Task.DownloadState.ReadyWithInfo
import com.junkfood.seal.util.DownloadUtil.DownloadPreferences
import com.junkfood.seal.util.Format
import com.junkfood.seal.util.PlaylistEntry
import com.junkfood.seal.util.PlaylistResult
import com.junkfood.seal.util.VideoClip
import com.junkfood.seal.util.VideoInfo
import kotlin.math.roundToInt

object TaskFactory {
    /**
     * @return A [TaskWithState] with extra configurations made by user in the custom format
     *   selection page
     */
    @CheckResult
    fun createWithConfigurations(
        videoInfo: VideoInfo,
        formatList: List<Format>,
        videoClips: List<VideoClip>,
        splitByChapter: Boolean,
        newTitle: String,
        selectedSubtitles: List<String>,
        selectedAutoCaptions: List<String>,
    ): TaskWithState {
        val fileSize =
            formatList.fold(.0) { acc, format ->
                acc + (format.fileSize ?: format.fileSizeApprox ?: .0)
            }

        val info =
            videoInfo
                .run { if (fileSize != .0) copy(fileSize = fileSize) else this }
                .run { if (newTitle.isNotEmpty()) copy(title = newTitle) else this }

        val audioOnlyFormats = formatList.filter { it.isAudioOnly() }
        val videoFormats = formatList.filter { it.containsVideo() }
        val audioOnly = audioOnlyFormats.isNotEmpty() && videoFormats.isEmpty()
        val mergeAudioStream = audioOnlyFormats.size > 1
        val formatId = formatList.joinToString(separator = "+") { it.formatId.toString() }

        val subtitleLanguage =
            (selectedSubtitles + selectedAutoCaptions).joinToString(separator = ",")

        val preferences =
            DownloadPreferences.createFromPreferences()
                .run {
                    copy(
                        formatIdString = formatId,
                        videoClips = videoClips,
                        splitByChapter = splitByChapter,
                        newTitle = newTitle,
                        mergeAudioStream = mergeAudioStream,
                        extractAudio = extractAudio || audioOnly,
                    )
                }
                .run {
                    if (subtitleLanguage.isNotEmpty()) {
                        copy(
                            downloadSubtitle = true,
                            autoSubtitle = selectedAutoCaptions.isNotEmpty(),
                            subtitleLanguage = subtitleLanguage,
                        )
                    } else {
                        this
                    }
                }

        val task = Task(url = info.originalUrl.toString(), preferences = preferences)
        val state =
            Task.State(
                downloadState = ReadyWithInfo,
                videoInfo = info,
                viewState =
                    Task.ViewState.fromVideoInfo(info = info)
                        .copy(videoFormats = videoFormats, audioOnlyFormats = audioOnlyFormats),
            )

        return TaskWithState(task, state)
    }

    /** @return List of [TaskWithState]s created from playlist items */
    @CheckResult
    fun createWithPlaylistResult(
        playlistUrl: String,
        indexList: List<Int>,
        playlistResult: PlaylistResult,
        preferences: DownloadPreferences,
    ): List<TaskWithState> {
        checkNotNull(playlistResult.entries)
        val indexEntryMap = indexList.associateWith { index -> playlistResult.entries[index - 1] }

        val taskList =
            indexEntryMap.map { (index, entry) ->
                val viewState = entry.toViewState(playlistResult, index)
                val task =
                    Task(
                        url = playlistUrl,
                        preferences = preferences,
                        type = Task.TypeInfo.Playlist(index),
                    )
                val state =
                    Task.State(downloadState = Idle, videoInfo = null, viewState = viewState)
                TaskWithState(task, state)
            }

        return taskList
    }

    /**
     * Creates one independent URL task for each selected channel video. Reusing the channel URL
     * here can make yt-dlp expand the selection as a grouped playlist download.
     */
    @CheckResult
    fun createWithChannelResult(
        indexList: List<Int>,
        playlistResult: PlaylistResult,
        preferences: DownloadPreferences,
    ): List<TaskWithState> {
        checkNotNull(playlistResult.entries)
        val indexEntryMap = indexList.associateWith { index -> playlistResult.entries[index - 1] }

        return indexEntryMap.map { (index, entry) ->
            val videoUrl = entry.toYouTubeVideoUrl()
            val viewState = entry.toViewState(playlistResult, index).copy(url = videoUrl)
            val task = Task(url = videoUrl, preferences = preferences)
            val state = Task.State(downloadState = Idle, videoInfo = null, viewState = viewState)
            TaskWithState(task, state)
        }
    }

    private fun PlaylistEntry.toViewState(
        playlistResult: PlaylistResult,
        index: Int,
    ): Task.ViewState =
        Task.ViewState(
            url = url ?: "",
            title = title ?: "${playlistResult.title} - $index",
            duration = duration?.roundToInt() ?: 0,
            uploader = uploader ?: channel ?: playlistResult.channel ?: "",
            thumbnailUrl = thumbnails?.lastOrNull()?.url ?: "",
        )

    private fun PlaylistEntry.toYouTubeVideoUrl(): String {
        val extractedUrl = url?.takeIf(String::isNotBlank)
        return when {
            extractedUrl?.startsWith("https://") == true ||
                extractedUrl?.startsWith("http://") == true -> extractedUrl
            extractedUrl?.startsWith("/") == true -> "https://www.youtube.com$extractedUrl"
            extractedUrl != null -> "https://www.youtube.com/watch?v=$extractedUrl"
            !id.isNullOrBlank() -> "https://www.youtube.com/watch?v=$id"
            else -> error("Selected channel entry has no video URL")
        }
    }

    data class TaskWithState(val task: Task, val state: Task.State)
}
