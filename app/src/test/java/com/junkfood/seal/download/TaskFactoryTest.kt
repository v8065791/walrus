package com.junkfood.seal.download

import com.junkfood.seal.util.DownloadUtil.DownloadPreferences
import com.junkfood.seal.util.PlaylistEntry
import com.junkfood.seal.util.PlaylistResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskFactoryTest {
    @Test
    fun `channel selections create independent video tasks`() {
        val tasks =
            TaskFactory.createWithChannelResult(
                indexList = listOf(1, 2),
                playlistResult =
                    PlaylistResult(
                        title = "Channel videos",
                        entries =
                            listOf(
                                PlaylistEntry(
                                    id = "first",
                                    url = "https://www.youtube.com/watch?v=first",
                                    title = "First",
                                ),
                                PlaylistEntry(id = "second", url = "second", title = "Second"),
                            ),
                    ),
                preferences = DownloadPreferences.EMPTY,
            )

        assertEquals(
            listOf(
                "https://www.youtube.com/watch?v=first",
                "https://www.youtube.com/watch?v=second",
            ),
            tasks.map { it.task.url },
        )
        assertTrue(tasks.all { it.task.type == Task.TypeInfo.URL })
        assertNotEquals(tasks[0].task.id, tasks[1].task.id)
    }

    @Test
    fun `channel entry id is used when flat playlist omits url`() {
        val task =
            TaskFactory.createWithChannelResult(
                    indexList = listOf(1),
                    playlistResult =
                        PlaylistResult(entries = listOf(PlaylistEntry(id = "video-id"))),
                    preferences = DownloadPreferences.EMPTY,
                )
                .single()

        assertEquals("https://www.youtube.com/watch?v=video-id", task.task.url)
        assertEquals(task.task.url, task.state.viewState.url)
    }

    @Test
    fun `ordinary playlists retain indexed playlist tasks`() {
        val task =
            TaskFactory.createWithPlaylistResult(
                    playlistUrl = "https://example.com/playlist",
                    indexList = listOf(2),
                    playlistResult =
                        PlaylistResult(
                            entries =
                                listOf(
                                    PlaylistEntry(url = "https://example.com/one"),
                                    PlaylistEntry(url = "https://example.com/two"),
                                )
                        ),
                    preferences = DownloadPreferences.EMPTY,
                )
                .single()

        assertEquals("https://example.com/playlist", task.task.url)
        assertEquals(Task.TypeInfo.Playlist(2), task.task.type)
    }
}
