# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Jaskier is a single-module Android app (`:app`, package `com.example.jaskier`) freshly generated from the Android Studio template. There is no application code yet — the manifest declares no activities and `app/src/main/java/` is empty; only the template unit/instrumented tests exist.

- minSdk 33, targetSdk/compileSdk 37, Java 11 compatibility
- AGP 9.3.1 with built-in Kotlin support (no separate Kotlin plugin is applied; Kotlin sources such as the tests compile via AGP itself)
- Dependencies are declared through the version catalog at `gradle/libs.versions.toml` — add new libraries there and reference them as `libs.*` in `app/build.gradle.kts`
- Gradle configuration cache is enabled (`gradle.properties`)

## Commands

```bash
./gradlew assembleDebug                 # build debug APK
./gradlew test                          # JVM unit tests (app/src/test)
./gradlew connectedAndroidTest          # instrumented tests (app/src/androidTest; needs a device/emulator)
./gradlew lint                          # Android Lint
./gradlew :app:testDebugUnitTest --tests "com.example.jaskier.ExampleUnitTest"   # single unit test class
```

## Notes

- R8 keep rules use the AGP 9 convention: files under `app/src/main/keepRules/` (e.g. `rules.keep`) are combined automatically — there is no `proguard-rules.pro`.
- The release build type currently has `optimization { enable = false }` (no minification).
