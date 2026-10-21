# NODE Player Mobile v0.1.1 — Changes from user-tested v0.1.0

## Current playing song
- The active track is highlighted cyan and marked with a slim cyan stripe in Home recent list, Library lists and Playlists.
- Three small equalizer-style bars bounce only when playback is active. When paused they stop and display `PAUSED`; this is a cosmetic playback animation, **not a true audio-spectrum equalizer**. It consumes no mic or network access.
- No playback engine changes; state comes from the existing Media3 PlaybackHub.

## Edit song names
- Open any song's **⋮** menu in Home, Library or a Playlist → **Edit song name**.
- Enter a title (1–120 characters) and select **Save name**. The current track name changes in player UI as well.
- Reopen **Edit song name** → **Restore original title** to undo.
- Only the library display title changes: MP3 files, paths and embedded metadata remain untouched.
- Overrides use `song_uri` keys in the local SQLite `song_titles` table. `LibraryStore` automatically migrates schema **1 → 2** without dropping existing favorites, playlists or history; rescan uses these overrides.

## Packaging / testing
- Android app version `0.1.1`, version code `2`; application ID unchanged.
- Upload the modified/new files in UPDATE ONLY to the same folder paths on GitHub, or replace with FULL SOURCE contents.
- GitHub Actions creates a test APK artifact `NODE-Player-Mobile-v0.1.1-TEST-APK` after successful compilation; the APK is not compiled in this environment.
- **Signing warning:** GitHub's ephemeral debug keys can prevent installation over the old v0.1.0 debug APK. Do not uninstall while you need existing app local data. Permanent private release signing is required for reliable official in-place updates.
