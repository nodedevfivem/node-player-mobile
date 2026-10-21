# NODE Player Mobile — v0.1.0 source starter

A dark cyan-and-chrome themed, 100% offline-first Android music player created for NODE Development. Official NODE Player logo included under `app/src/main/res/drawable-nodpi`.

## Status — SOURCE ONLY

Complete **initial Android Studio source project**; it has not been compiled or exercised on an Android device in the generation environment (no Android SDK/Gradle/network present). If Android Studio reports a build error, provide the exact output to make a targeted fix. Do not publish before testing.

## Android / stack

- Android 10+ (`minSdk=29`), Kotlin 2.0.21, Jetpack Compose, Android Media3 player and MediaSessionService.
- Android Gradle Plugin 8.9.2, Gradle 8.11.1, JDK 17+, compileSdk 35.
- No internet account, no cloud DB, no Supabase, no Firebase. Local SQLite for favorites/playlists/recent plays; SharedPreferences for folder choice.
- Offline playback and import; internet is only for optional GitHub release checks/downloads.

## Implemented in the v0.1.0 foundation

- Native home dashboard, official logo, first-run default/custom music folder selection.
- Scan ONLY `Internal Storage/Music/` (including subfolders) via MediaStore, or an explicitly chosen SAF folder. Safe manual Refresh and change-triggered refresh while the app is foregrounded.
- Import one/multiple local audio files through the Android file picker; copy into the active music folder, not move/delete the originals. Formats considered: MP3, M4A/MP4 audio, FLAC, AAC, OGG/Opus, WAV (codec support varies by device).
- All Songs, Artists, Albums, Favorites; search, sorting; artwork extraction from embedded tags with lazy disk caching.
- Favorite tracks and create/delete playlists, add/remove tracks, local SQLite persistence.
- Playback, seek, next/previous, shuffle, repeat one/all, restore last position (without auto-playing), expanded Now Playing and bottom mini-player.
- Media3 background session for system notification/media controls, lock screen, headset and Bluetooth media commands.
- GitHub release check with Update Now/Maybe Later, SHA-256 validation, Android installer handoff. Releases are **not yet live**, so the initial check may return no update/repository unavailable until a GitHub repo and first release exist.

## Planned after the foundation (not implemented in this zip)

Equalizer/presets, sleep timer, visualizer, lyrics/LRC, playlist reordering, richer metadata editing, advanced gapless/crossfade, themes, Android Auto. These were approved for phased updates and should not be represented as already working.

The repository also includes a GitHub Actions workflow (`.github/workflows/debug-build.yml`) for Android debug CI builds (Android signs debug APKs with a temporary debug key; these are NOT unsigned). It does **not** have any signing secrets and it does not automatically publish signed updates.

## Recommended: get your APK with GitHub Actions (Android Studio NOT required)

Read `START-HERE-BUILD-APK-WITHOUT-ANDROID-STUDIO.md`. This repository includes `.github/workflows/debug-build.yml` to build a test APK on GitHub using Gradle directly (no wrapper jar necessary on GitHub). This workflow has NOT run yet, so compilation is not verified. Publishing to GitHub and running Actions requires repository access.

## Alternative: open in Android Studio

1. Extract this ZIP to a normal folder, e.g. `C:\NODE-Player-Mobile`.
2. Open the **folder containing `settings.gradle.kts`** in Android Studio.
3. Run `SETUP-GRADLE-WRAPPER.bat` once to obtain the **authentic, checksum-verified** Gradle wrapper jar (not bundled because this environment cannot download its bytes). Internet required once.
4. Wait for Gradle sync and SDK components to download.
5. Connect your Android device with developer options + USB debugging, select the `app` configuration, then Run; or run `BUILD-DEBUG.bat` and install `app/build/outputs/apk/debug/app-debug.apk`.
6. Grant Music permission when choosing the default Music folder. Your music files stay on your phone. No song files are included in this source.

For signed APKs, see `docs/RELEASE-GUIDE.md` and `SIGN-RELEASE.bat`.

## Privacy and device behavior

Audio is read from `Music/` unless the user chooses another folder. Imported audio stays on-device; import **copies** rather than moving files. Android's user-approved file/folder pickers are used; a custom SAF folder must be writable for imports. Library metadata does not leave the device. GitHub check accesses the public GitHub API to see whether a new app version exists.

## Repo and version conventions

Source config currently points to owner `nodedevfivem`, repository `NODE-Player-Mobile` as the proposed project name. Create that repo or update the config before releasing. Version 0.1.0 / versionCode 1. Do not rename app ID or lose release signing key after publishing.
