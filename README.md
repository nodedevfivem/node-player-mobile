# NODE Player Mobile — v0.1.1 source update

A dark cyan-and-chrome themed, 100% offline-first Android music player created for NODE Development. Official NODE Player logo included under `app/src/main/res/drawable-nodpi`.

## Status — v0.1.1 SOURCE ONLY (build not yet verified)

User confirmed v0.1.0 compiles on GitHub Actions and plays music on Android; screenshot confirms songs detected and playback working. The v0.1.1 modifications have **not yet** been Android-compiled or tested on the phone. If GitHub Actions reports an error, use the build logs for a targeted fix. Do not publish this update before testing.

### What's new in v0.1.1

- Cyan row highlight and left-edge marker show the current track in All Songs/filtered Library, Home recent songs and Playlists.
- Three short animated equalizer-style bars indicate playing status and stop moving on pause (cosmetic, not an actual frequency analyzer).
- Clear `NOW PLAYING` / `PAUSED` labels; follows automatic next-song transitions.
- Library/Home/playlist song menus now offer **Edit song name**. Rename and restore the original title; the custom name persists across rescan and app restarts.
- User-given song titles are stored as local, URI-keyed overrides in a new SQLite table. Existing library data survives an additive database migration from schema 1 to 2; source audio files/tags are never modified.
- Same Android package ID and permissions; `versionCode=2`, `versionName=0.1.1`.
- GitHub Actions debug artifact is now `NODE-Player-Mobile-v0.1.1-TEST-APK`.
- NOTE: GitHub-hosted debug APKs may have different signing keys across builds. A new debug APK might not install over v0.1.0. Prefer setting up permanent private release signing before relying on in-place updates; uninstalling deletes local preferences/favorites/playlists.


## Android / stack

- Android 10+ (`minSdk=29`), Kotlin 2.0.21, Jetpack Compose, Android Media3 player and MediaSessionService.
- Android Gradle Plugin 8.9.2, Gradle 8.11.1, JDK 17+, compileSdk 35.
- No internet account, no cloud DB, no Supabase, no Firebase. Local SQLite for favorites/playlists/recent plays/custom song names; SharedPreferences for folder choice.
- Offline playback and import; internet is only for optional GitHub release checks/downloads.

## Implemented in the v0.1.0 foundation (preserved in v0.1.1)

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

Read `START-HERE-BUILD-APK-WITHOUT-ANDROID-STUDIO.md`. This repository includes `.github/workflows/debug-build.yml` to build a test APK on GitHub using Gradle directly (no wrapper jar necessary on GitHub). GitHub Actions successfully compiled v0.1.0 in the user repository; v0.1.1 has not run yet. Publishing to GitHub and running Actions requires repository access.

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

Source config currently points to owner `nodedevfivem`, repository `NODE-Player-Mobile` as the proposed project name. Create that repo or update the config before releasing. Version 0.1.1 / versionCode 2. Do not rename app ID or lose release signing key after publishing.
