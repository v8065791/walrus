# Walrus

Walrus is an Android video and audio downloader powered by [yt-dlp](https://github.com/yt-dlp/yt-dlp). It is a fork of [Seal](https://github.com/JunkFood02/Seal) that keeps Seal's flexible custom-command workflow while adding channel-oriented selection inspired by YTDLnis.

## Highlights

- Paste a YouTube channel URL and browse separate Videos, Shorts, and Live tabs.
- Select individual channel entries before downloading instead of treating the channel as one opaque URL.
- Download video, audio, playlists, subtitles, and metadata through a Compose-based interface.
- Create, save, and run custom yt-dlp command templates.
- Keep download history and update the bundled yt-dlp runtime.
- Optionally delete media and its related yt-dlp sidecar files together from history.

## Build

Requirements:

- JDK 21
- Android SDK with compile SDK 35

Create `local.properties` with your local SDK path, then run:

```bash
./gradlew :app:testGenericDebugUnitTest
./gradlew :app:assembleGenericDebug
```

The universal debug APK is written under `app/build/outputs/apk/generic/debug/`. Run `./gradlew ktfmtCheck` before submitting Kotlin changes.

## Project Status

Version 3.5 focuses on a faster channel-download workflow and clearer download-mode tabs without removing Seal's custom scripts or downloader. The Kotlin source namespace remains `com.junkfood.seal` to minimize divergence from upstream; Walrus uses the distinct Android application ID `com.v8065791.walrus`.

## Contributing

Read [AGENTS.md](AGENTS.md) for repository layout, validation commands, and contribution conventions. Bug reports should include a reproducible URL when safe to share, the selected options or custom command, Android version, and relevant logs.

## Credits and License

Walrus is based on [Seal](https://github.com/JunkFood02/Seal), created by JunkFood02, and uses [youtubedl-android](https://github.com/yausername/youtubedl-android). Upstream copyright and contributor history are retained.

This project is licensed under GPL-3.0. See [LICENSE](LICENSE).
