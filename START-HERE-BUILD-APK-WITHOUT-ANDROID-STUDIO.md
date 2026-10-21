# NODE Player Mobile v0.1.2 — build NODE-Player.apk with GitHub (no Android Studio)

## A. Quick test APK (for your phone)

1. Copy the **v0.1.2 UPDATE ONLY** ZIP's contents into `D:\NODE_APPS\NODE-Player-Mobile` and overwrite matching files. Make sure hidden `.github/workflows` files are included. Or use the FULL SOURCE ZIP.
2. Upload the changed files to your existing GitHub repository `nodedevfivem/node-player-mobile` at **the same paths** (commit to `main`). Do **not** upload any private `.jks`, `signing.properties`, backup key or password.
3. GitHub Actions runs **Build Android APK (No Android Studio)** on pushes to `main`; alternatively run it manually.
4. After a green build, open the workflow's Summary and scroll to **Artifacts**. Download `NODE-Player-v0.1.2-TEST-APK`.
5. Extract the ZIP. The actual APK inside it is **`NODE-Player.apk`**. Install/test on Android 10+.

The TEST APK is debug-signed on GitHub's short-lived build runner. It is NOT a public signed release. **Do not uninstall an existing test app just to make it install**—that can erase local playlists/favorites/names.

## B. Official permanently signed APK (for GitHub Releases and in-app updates)

1. **Once only:** Set up a permanent release signing key privately. You can run `CREATE-PRIVATE-SIGNING-KEY.bat` on Windows if a JDK `keytool` is available (JDK, NOT Android Studio), or create a private key using another secure JDK installation. Back up the private keystore and passwords offline.
2. On GitHub repository **Settings → Secrets and variables → Actions**, add these repository secrets:
   - `NODE_PLAYER_KEYSTORE_BASE64` — full Base64 encoded keystore, no line breaks
   - `NODE_PLAYER_STORE_PASSWORD` — private keystore password
   - `NODE_PLAYER_KEY_ALIAS` — e.g. `nodeplayer`
   - `NODE_PLAYER_KEY_PASSWORD` — key password (for standard PKCS12 same as keystore password)
3. Go to **Actions → Build SIGNED NODE-Player APK (Official Release)**, select `main`, then **Run workflow**. This action intentionally will NOT run automatically on every push.
4. A successful action has artifact `NODE-Player-v0.1.2-SIGNED-RELEASE`. Download and extract its ZIP to get **TWO files**: `NODE-Player.apk` and `NODE-Player.apk.sha256`.
5. Create a **normal public GitHub Release**, tag `v0.1.2` (not draft / not pre-release), attach **BOTH files** directly as release assets. Paste the supplied release description. Do not attach the debug APK by accident.
6. Future users who originally installed the permanently signed public APK can use **UPDATE NOW** inside NODE Player after you publish the next higher-version signed release with the SAME private key. Android still asks for installation approval.

## C. Critical one-time transition from v0.1.0/v0.1.1 testing APKs

The GitHub-hosted debug APK is signed with a different certificate from your new official release key. Android will reject a direct in-place update between those signers, even with the same name or package ID. This limitation cannot be safely bypassed. **Before uninstalling a debug build, understand that app-private playlists, favorites and custom song names may be lost.** User music files in shared Music storage do not normally disappear when uninstalling.

## D. Common mistakes

- `app-debug.apk` in `app/build/outputs` is Gradle's temporary output name; the **downloadable file** from the workflows is renamed `NODE-Player.apk`.
- ZIP artifacts shown in GitHub Actions are **not** GitHub Releases. To trigger in-app checks, upload the two official files to a new public GitHub Release.
- Naming a debug APK `NODE-Player.apk` does NOT make it release-signed or upgrade-compatible.
- Never share private signing keys/passwords with ChatGPT or post them in GitHub source.
- Publishing v0.1.2 cannot trigger an update alert in v0.1.2 itself. Test end-to-end **0.1.2 → 0.1.3** after publishing your next signed release.
