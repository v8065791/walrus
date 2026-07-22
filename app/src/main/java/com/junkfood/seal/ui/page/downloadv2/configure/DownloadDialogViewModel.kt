package com.junkfood.seal.ui.page.downloadv2.configure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junkfood.seal.database.objects.CommandTemplate
import com.junkfood.seal.download.DownloaderV2
import com.junkfood.seal.download.Task
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.PlaylistResult
import com.junkfood.seal.util.VideoInfo
import com.junkfood.seal.util.YouTubeChannelSource
import com.junkfood.seal.util.YouTubeChannelTab
import com.junkfood.seal.util.toYouTubeChannelSource
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "DownloadDialogViewModel"

class DownloadDialogViewModel(private val downloader: DownloaderV2) : ViewModel() {

    sealed interface SelectionState {
        data object Idle : SelectionState

        data class PlaylistSelection(
            val result: PlaylistResult,
            val channelSource: YouTubeChannelSource? = null,
            val selectedChannelTab: YouTubeChannelTab? = null,
            val loadingChannelTab: YouTubeChannelTab? = null,
            val channelTabError: String? = null,
            val preferences: DownloadUtil.DownloadPreferences =
                DownloadUtil.DownloadPreferences.EMPTY,
        ) : SelectionState

        data class FormatSelection(val info: VideoInfo) : SelectionState
    }

    sealed interface SheetState {
        data object InputUrl : SheetState

        data class Configure(val urlList: List<String>) : SheetState

        data class Loading(val taskKey: String, val job: Job) : SheetState

        data class Error(val action: Action, val throwable: Throwable) : SheetState
    }

    sealed interface SheetValue {
        data object Expanded : SheetValue

        data object Hidden : SheetValue
    }

    sealed interface Action {
        data object HideSheet : Action

        data class ShowSheet(val urlList: List<String>? = null) : Action

        data class ProceedWithURLs(val urlList: List<String>) : Action

        data object Reset : Action

        data class FetchPlaylist(
            val url: String,
            val preferences: DownloadUtil.DownloadPreferences,
        ) : Action

        data class FetchChannelTab(val tab: YouTubeChannelTab) : Action

        data class FetchFormats(
            val url: String,
            val audioOnly: Boolean,
            val preferences: DownloadUtil.DownloadPreferences,
        ) : Action

        data class DownloadWithPreset(
            val urlList: List<String>,
            val preferences: DownloadUtil.DownloadPreferences,
        ) : Action

        data class RunCommand(
            val url: String,
            val template: CommandTemplate,
            val preferences: DownloadUtil.DownloadPreferences,
        ) : Action

        data object Cancel : Action
    }

    private val mSelectionStateFlow: MutableStateFlow<SelectionState> =
        MutableStateFlow(SelectionState.Idle)
    private val mSheetStateFlow: MutableStateFlow<SheetState> =
        MutableStateFlow(SheetState.InputUrl)
    private val mSheetValueFlow: MutableStateFlow<SheetValue> = MutableStateFlow(SheetValue.Hidden)
    private var channelTabJob: Job? = null
    private var channelTabTaskKey: String? = null

    val selectionStateFlow = mSelectionStateFlow.asStateFlow()
    val sheetStateFlow = mSheetStateFlow.asStateFlow()
    val sheetValueFlow = mSheetValueFlow.asStateFlow()

    private val sheetState
        get() = sheetStateFlow.value

    fun postAction(action: Action) {
        with(action) {
            when (this) {
                is Action.ProceedWithURLs -> proceedWithUrls(this)
                is Action.FetchFormats -> fetchFormat(this)
                is Action.FetchPlaylist -> fetchPlaylist(this)
                is Action.FetchChannelTab -> fetchChannelTab(tab)
                is Action.DownloadWithPreset -> downloadWithPreset(urlList, preferences)
                is Action.RunCommand -> runCommand(url, template, preferences)
                Action.HideSheet -> hideDialog()
                is Action.ShowSheet -> showDialog(this)
                Action.Cancel -> cancel()
                Action.Reset -> resetSelectionState()
            }
        }
    }

    private fun proceedWithUrls(action: Action.ProceedWithURLs) {
        mSheetStateFlow.update { SheetState.Configure(action.urlList) }
    }

    private fun fetchPlaylist(action: Action.FetchPlaylist) {
        val (url, preferences) = action
        val channelSource = url.toYouTubeChannelSource()
        val resolvedUrl = channelSource?.urlFor(channelSource.initialTab) ?: url
        val taskKey = "FetchPlaylist_$resolvedUrl"

        val job =
            viewModelScope.launch(Dispatchers.IO) {
                DownloadUtil.getPlaylistOrVideoInfo(
                        playlistURL = resolvedUrl,
                        downloadPreferences = preferences,
                        taskKey = taskKey,
                    )
                    .onSuccess { info ->
                        withContext(Dispatchers.Main) {
                            when (info) {
                                is PlaylistResult -> {
                                    mSelectionStateFlow.update {
                                        SelectionState.PlaylistSelection(
                                            result = info,
                                            channelSource = channelSource,
                                            selectedChannelTab = channelSource?.initialTab,
                                            preferences = preferences,
                                        )
                                    }
                                }

                                is VideoInfo -> {
                                    mSelectionStateFlow.update {
                                        SelectionState.FormatSelection(info = info)
                                    }
                                }
                            }
                            hideDialog()
                        }
                    }
                    .onFailure { th ->
                        mSheetStateFlow.update { SheetState.Error(action = action, throwable = th) }
                    }
            }
        mSheetStateFlow.update { SheetState.Loading(taskKey = taskKey, job = job) }
    }

    private fun fetchChannelTab(tab: YouTubeChannelTab) {
        val selection = mSelectionStateFlow.value as? SelectionState.PlaylistSelection ?: return
        val source = selection.channelSource ?: return
        if (selection.loadingChannelTab != null || selection.selectedChannelTab == tab) return

        mSelectionStateFlow.update {
            selection.copy(loadingChannelTab = tab, channelTabError = null)
        }
        val taskKey = "FetchChannelTab_${source.urlFor(tab)}"
        channelTabTaskKey = taskKey
        channelTabJob =
            viewModelScope.launch(Dispatchers.IO) {
                DownloadUtil.getPlaylistOrVideoInfo(
                        playlistURL = source.urlFor(tab),
                        downloadPreferences = selection.preferences,
                        taskKey = taskKey,
                    )
                    .onSuccess { info ->
                        withContext(Dispatchers.Main) {
                            val current =
                                mSelectionStateFlow.value as? SelectionState.PlaylistSelection
                                    ?: return@withContext
                            if (
                                current.channelSource != source || current.loadingChannelTab != tab
                            ) {
                                return@withContext
                            }
                            channelTabTaskKey = null
                            channelTabJob = null
                            if (info is PlaylistResult) {
                                mSelectionStateFlow.update {
                                    current.copy(
                                        result = info,
                                        selectedChannelTab = tab,
                                        loadingChannelTab = null,
                                        channelTabError = null,
                                    )
                                }
                            } else {
                                mSelectionStateFlow.update {
                                    current.copy(
                                        loadingChannelTab = null,
                                        channelTabError =
                                            "The selected channel tab contains no list",
                                    )
                                }
                            }
                        }
                    }
                    .onFailure { throwable ->
                        withContext(Dispatchers.Main) {
                            val current =
                                mSelectionStateFlow.value as? SelectionState.PlaylistSelection
                                    ?: return@withContext
                            if (
                                current.channelSource == source && current.loadingChannelTab == tab
                            ) {
                                channelTabTaskKey = null
                                channelTabJob = null
                                mSelectionStateFlow.update {
                                    current.copy(
                                        loadingChannelTab = null,
                                        channelTabError =
                                            throwable.message ?: "Unable to load the channel tab",
                                    )
                                }
                            }
                        }
                    }
            }
    }

    private fun fetchFormat(action: Action.FetchFormats) {
        val (url, audioOnly, preferences) = action

        val job =
            viewModelScope.launch(Dispatchers.IO) {
                DownloadUtil.fetchVideoInfoFromUrl(
                        url = url,
                        preferences = preferences.copy(extractAudio = audioOnly),
                        taskKey = "FetchFormat_$url",
                    )
                    .onSuccess { info ->
                        withContext(Dispatchers.Main) {
                            mSelectionStateFlow.update {
                                SelectionState.FormatSelection(info = info)
                            }
                            hideDialog()
                        }
                    }
                    .onFailure { th ->
                        withContext(Dispatchers.Main) {
                            mSheetStateFlow.update { SheetState.Error(action, throwable = th) }
                        }
                    }
            }

        mSheetStateFlow.update { SheetState.Loading(taskKey = "FetchFormat_$url", job = job) }
    }

    private fun downloadWithPreset(
        urlList: List<String>,
        preferences: DownloadUtil.DownloadPreferences,
    ) {
        urlList.forEach { downloader.enqueue(Task(url = it, preferences = preferences)) }
        hideDialog()
    }

    private fun runCommand(
        url: String,
        template: CommandTemplate,
        preferences: DownloadUtil.DownloadPreferences,
    ) {
        val task =
            Task(
                url = url,
                type = Task.TypeInfo.CustomCommand(template = template),
                preferences = preferences,
            )
        downloader.enqueue(task)
    }

    private fun hideDialog() {
        mSheetValueFlow.update { SheetValue.Hidden }
        when (sheetState) {
            is SheetState.Loading -> {
                cancel()
            }

            else -> {}
        }
    }

    private fun showDialog(action: Action.ShowSheet) {
        val urlList = action.urlList
        if (!urlList.isNullOrEmpty()) {
            mSheetStateFlow.update { SheetState.Configure(urlList) }
        } else {
            mSheetStateFlow.update { SheetState.InputUrl }
        }
        mSheetValueFlow.update { SheetValue.Expanded }
    }

    private fun cancel(): Boolean {
        return when (val state = sheetState) {
            is SheetState.Loading -> {
                val res = YoutubeDL.destroyProcessById(id = state.taskKey)
                if (res) {
                    state.job.cancel()
                }
                return res
            }
            else -> false
        }
    }

    private fun resetSelectionState() {
        cancelChannelTab()
        mSelectionStateFlow.update { SelectionState.Idle }
    }

    private fun cancelChannelTab() {
        channelTabTaskKey?.let { YoutubeDL.destroyProcessById(it) }
        channelTabJob?.cancel()
        channelTabTaskKey = null
        channelTabJob = null
    }

    override fun onCleared() {
        cancelChannelTab()
        super.onCleared()
    }
}
