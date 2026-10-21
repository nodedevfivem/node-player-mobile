# NODE Player Mobile — signed APK GitHub Releases

## ONE-TIME SETUP

1. Install Android Studio with Android SDK Platform 35 and Java/JDK 17+.
2. Create a GitHub repository under `nodedevfivem` named `NODE-Player-Mobile` (or change GITHUB_REPO/GITHUB_OWNER in `app/build.gradle.kts` before publishing).
3. Run `SETUP-GRADLE-WRAPPER.bat` on a Windows PC **with internet access**. The script downloads Gradle's official wrapper JAR and validates its published SHA-256. Subsequent Gradle dependency downloads also require internet; actual music playback does not.
4. Make a **private, permanent signing key** (never share this key with ChatGPT, GitHub, or end users):

   `keytool -genkeypair -v -keystore node-player-release.jks -alias nodeplayer -keyalg RSA -keysize 3072 -validity 10000`

   Store the file safely outside the public source repository and make backup copies. Use an alias/password you can keep securely.
5. Copy `signing.properties.example` to `signing.properties`. Put your real `storeFile` path, passwords, and `keyAlias` there. Neither this file nor the `.jks` is ever included in distributed source ZIPs.

## FIRST OFFICIAL RELEASE

1. Test on a real Android 10+ phone via `BUILD-DEBUG.bat` or Android Studio. Debug and release APKs are signed with DIFFERENT keys.
2. Run `SIGN-RELEASE.bat`. It builds a signed APK and generates its `.apk.sha256` companion in `RELEASE-OUTPUT`.
3. Create a GitHub Release with tag `v0.1.0`; attach BOTH assets from `RELEASE-OUTPUT`.
4. Install that release APK on devices. Later updates can be installed over it using the app's updater if the exact same application ID and release keystore are used.

## EVERY UPDATE

1. Increase `versionCode` (integer; MUST increase), and `versionName` (e.g. `0.1.1`) in `app/build.gradle.kts`.
2. Also update the version embedded in the output name in `SIGN-RELEASE.bat`, e.g. replace `v0.1.0` with `v0.1.1` for that release.
3. Run the release script and check the signed APK and `.sha256` exist.
4. Publish GitHub Release tag `v0.1.1` (not a draft or prerelease), upload matching APK and `.apk.sha256` assets.
5. The app requests the latest release metadata from the public GitHub Releases API when opened online, shows Update Now / Maybe Later, validates the new APK's SHA-256, and hands it to Android's installer. Users may need to allow installation from this app in system settings. Android enforces signature compatibility.

### Security / integrity

* NEVER commit `signing.properties`, `.jks`, passwords, or signing key material.
* `.apk.sha256` verifies the downloaded file's integrity. SHA-256 assets are not a substitute for the Android signing certificate: Android's package installer checks installed app signature continuity.
* Since the checksum itself is hosted in the same GitHub release, repository account security is important (enable GitHub 2FA).
* Release downloads require internet, but no internet is needed for local music library/playback.
* App intentionally does NOT force upgrades or silently install APKs.
* Do not change `applicationId` after release or you will break in-place upgrades.
