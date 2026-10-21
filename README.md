# NODE Player Mobile — v0.1.2

**Android music player by NODE Development**. Local music playback remains offline-first with no online database.

## v0.1.2: automatic update flow + NODE-Player.apk filename

- Version `0.1.2`, `versionCode=3`; SAME application ID `com.nodedevelopment.nodeplayermobile`.
- GitHub Actions test build output **inside its downloadable artifact ZIP** is `NODE-Player.apk` (not `app-debug.apk`).
- New GitHub Actions **signed official release** workflow also outputs `NODE-Player.apk` plus its matching `NODE-Player.apk.sha256`. It **requires private GitHub Secrets** configured by the developer; it will refuse to build without them.
- GitHub Releases: on opening online, check for newer versions; show **UPDATE NOW** or **LATER (24H)**. Download official APK with progress, validate HTTPS source, SHA-256 checksum, package ID, newer version, and signing certificate compatibility; hand off to Android's system installer for user approval. If Android asks for app-install permission, resume the already-downloaded file after the user returns from Settings.
- All library scanning, import music, playback, playlists/favorites, v0.1.1 playing-animation and song-title customization are preserved.

## Start here (NO Android Studio required on your PC)

Read **`START-HERE-BUILD-APK-WITHOUT-ANDROID-STUDIO.md`**, then **`docs/RELEASE-GUIDE.md`**.

### Important: debug APKs versus official APKs

GitHub's debug test builds use **temporary signing certificates**. Even if both APKs are named `NODE-Player.apk`, Android may NOT let one install over the other. A new official signing key will also NOT update an already-installed debug build. **Do not uninstall an existing test build without understanding that local favorites, playlists and custom song names may be removed** (ordinary MP3s in the shared Music folder remain outside the app). Establish ONE long-term private signing key BEFORE public distribution. Keep it forever.

The first publicly released signed version is the permanent foundation for future in-app updates. A future higher `versionCode` APK must be signed with the SAME key.

### No server/database service required

Music, local playlists/favorites, and per-song rename aliases stay on the phone; the GitHub Releases API is only used for optional update checks/downloads. No subscription or account.

### Status

Source packages for v0.1.2 have been prepared. This exact version is **not yet Android-compiled or tested**; verify the green GitHub Actions test build and an actual device before publishing. User reported their previous version working, but that does not independently validate v0.1.2.
