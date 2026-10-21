# NODE Player Mobile — v0.1.2 and future signed GitHub Releases

## Source and APK naming

- Application name: **NODE Player Mobile**.
- Stable package ID: `com.nodedevelopment.nodeplayermobile` (do not change).
- Artifact/test APK filename: **`NODE-Player.apk`**.
- Official GitHub Release assets: **`NODE-Player.apk`** and **`NODE-Player.apk.sha256`**.
- Version in Gradle: `versionCode=3`, `versionName=0.1.2`, tag `v0.1.2`.

## ONE-TIME PRIVATE SIGNING IDENTITY — keep forever

You need a JDK's `keytool` on Windows **only for key generation**, not Android Studio. Run `CREATE-PRIVATE-SIGNING-KEY.bat` in this folder. It creates:

- `../NODE-Player-SIGNING-PRIVATE/node-player-release.jks` (private keystore)
- `../NODE-Player-SIGNING-PRIVATE/KEYSTORE-BASE64-PRIVATE.txt` (temporary copy for uploading the private GitHub Secret)

The folder is OUTSIDE the project root, so dragging project files onto GitHub cannot include it. Back up `.jks` and the passwords privately; delete the temporary Base64 file after storing the secret. **Do not upload to GitHub Code, paste in chat, or publish with the release.** The script will refuse to overwrite an existing key. The private file is NOT in the provided full-source ZIP.

If the JDK isn't installed, get a reputable JDK 17+; no Android Studio needed. The script asks for a password; for default PKCS12 both store and key use that password. Use the same key for **every** future official version.

### GitHub encrypted Secrets (4, one-time)

In repository `nodedevfivem/node-player-mobile` go to **Settings → Secrets and variables → Actions → New repository secret**.

1. `NODE_PLAYER_KEYSTORE_BASE64`: the full Base64 string from the private local text file.
2. `NODE_PLAYER_STORE_PASSWORD`: the private keystore password.
3. `NODE_PLAYER_KEY_ALIAS`: `nodeplayer` (or your chosen key alias).
4. `NODE_PLAYER_KEY_PASSWORD`: the private key password (normally the same as store for PKCS12).

Do not add these to a source file. GitHub Actions reconstructs the key temporarily on its secure build runner and deletes it when the job ends.

## Build official release in GitHub Actions

1. Commit the updated source, including `.github/workflows/release-build.yml`, to `main`.
2. GitHub Actions → **Build SIGNED NODE-Player APK (Official Release)** → **Run workflow** on `main`.
3. Ensure green success. Download `NODE-Player-v0.1.2-SIGNED-RELEASE` artifact and extract it.
4. Confirm `NODE-Player.apk` and `NODE-Player.apk.sha256` are present. The workflow checks the Android signing certificate before producing the artifact.
5. GitHub Releases → **Draft a new release** → tag **`v0.1.2`**, release title **NODE Player Mobile v0.1.2 — In-App Updates & Signed APK**. Attach BOTH files as release assets (not ZIP). Use the provided full description, then publish a normal (not pre-release) release after testing.

### Future releases

Bump `versionCode` and `versionName`, update workflow artifact descriptions, reuse the **same GitHub Secrets** and key, create a matching public release with `NODE-Player.apk` and `NODE-Player.apk.sha256`. GitHub Releases `/latest` returns the newest published stable release, which NODE Player checks when opened online. `UPDATE NOW` verifies and downloads the APK then hands it to Android's installer; `LATER` snoozes that version for 24h. No automatic silent installation is possible for ordinary sideloaded apps.

## Important limitations

- The v0.1.0/v0.1.1 GitHub Actions debug APK has an ephemeral debug signature. A new official release key cannot update that debug APK in place. Android's signature checks are mandatory. Back up data before uninstalling; app-private playlists/favorites/names can be lost on uninstall. Original MP3 files in shared storage normally remain.
- A GitHub Actions **test** artifact is NOT an official GitHub Release. Only a published non-prerelease Release containing both assets is discoverable by the in-app updater.
- The `.sha256` file is integrity protection; Android's signer validation is an independent essential security step.
- Public music playback does not need Wi-Fi or mobile data. Only GitHub update checking and downloading do.
