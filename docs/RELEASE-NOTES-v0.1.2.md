# NODE Player Mobile v0.1.2 changes (from v0.1.1)

- Builds `NODE-Player.apk` rather than distributing `app-debug.apk`.
- `versionCode` 2 → 3, `versionName` 0.1.1 → 0.1.2; app ID unchanged.
- New GitHub Actions signed release workflow backed by the developer's encrypted signing secrets; signature verified with Android's `apksigner` before publishing artifact.
- Test GitHub workflow now uploads renamed `NODE-Player.apk` inside its `NODE-Player-v0.1.2-TEST-APK` artifact ZIP.
- Local Windows build scripts also copy to `DEBUG-OUTPUT/NODE-Player.apk` and `RELEASE-OUTPUT/NODE-Player.apk` with matching SHA-256.
- In-app GitHub update source now selects EXACTLY matching release assets `NODE-Player.apk` and `NODE-Player.apk.sha256`.
- Update downloads validate SHA-256 checksum, correct app package ID, newer APK version, and signing certificate compatibility before opening the Android installer.
- "Later" snoozes update prompts for 24 hours; manual Settings → Check for Updates still bypasses the delay.
- Android unknown-app installation permission settings now resume the cached downloaded APK when the user returns (no second download).
- All music playback, imported songs, favorites, playlists, song-name aliases, and v0.1.1 moving equalizer bars preserved.

**Source only; Android compilation and real-device testing of this version pending.**
