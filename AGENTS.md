# Repository Guidelines

## Project Structure & Module Organization

Walrus is a multi-module Android project. The main application lives in `app/`; Kotlin sources are under `app/src/main/java/com/junkfood/seal`, Android resources under `app/src/main/res`, and JVM tests under `app/src/test`. The `color/` module contains reusable color utilities. Version logic is centralized in `buildSrc/src/main/kotlin/Version.kt`, while release metadata lives in `fastlane/metadata/android/`.

The source namespace remains `com.junkfood.seal` for compatibility with the upstream codebase; the shipped application ID is `com.v8065791.walrus`.

## Build, Test, and Development Commands

- `./gradlew :app:assembleGenericDebug` builds an installable generic debug APK.
- `./gradlew :app:testGenericDebugUnitTest` runs local JVM unit tests.
- `./gradlew :app:lintGenericDebug` runs Android lint checks.
- `./gradlew ktfmtCheck` verifies Kotlin formatting; use `./gradlew ktfmtFormat` to apply it.

Use JDK 21 and an Android SDK compatible with compile SDK 35. Keep machine-specific SDK paths in the ignored `local.properties` file.

## Coding Style & Naming Conventions

Follow Kotlin style with four-space indentation and trailing commas in multiline declarations. Compose functions and types use `PascalCase`; functions, properties, and test methods use `camelCase`; constants use `UPPER_SNAKE_CASE`. Keep UI state in view models and extraction/download behavior in utility or downloader layers. Run ktfmt before committing.

## Testing Guidelines

Add focused JVM tests under the matching package in `app/src/test`. Name test classes after the subject, such as `YouTubeChannelUrlTest`, and use descriptive backtick test names. Cover URL normalization, state transitions, and failure/cancellation paths for downloader changes.

## Commit & Pull Request Guidelines

Use concise imperative commits, preferably Conventional Commit prefixes such as `feat:`, `fix:`, `refactor:`, or `docs:`. Pull requests should explain user-visible behavior, list validation commands, and link relevant issues. Include screenshots or a short recording for Compose UI changes. Avoid committing secrets, keystores, generated APKs, or `local.properties`.
