# NODE Player Mobile v0.1.1 — build APK without Android Studio

You do not need Android Studio, Java, or the Android SDK installed on your PC.
GitHub Actions builds the APK on a hosted build runner and provides it as a downloadable artifact.

## One-time upload (easiest with the ChatGPT GitHub connection)

1. Make a GitHub repository named `NODE-Player-Mobile` under the account `nodedevfivem`.
2. Publish the **contents** of this project's source directory into the repository's main branch. Preserve all nested folder paths, including `.github/workflows/debug-build.yml`. Do NOT upload the ZIP as a single repository file; GitHub needs to see the source files.
3. If available, connect GitHub to ChatGPT, so that future source changes can be prepared or published with fewer manual steps, subject to your approval.

## Download the test APK

1. Open your `NODE-Player-Mobile` GitHub repository.
2. Select **Actions** at the top.
3. Select **Build Android APK (No Android Studio)** from the left.
4. Click **Run workflow** and run it on `main` (or select the newest run automatically started after an upload).
5. Wait for a green check (if a red X appears, open the job log and send the error to ChatGPT for a code fix).
6. Open that run and scroll to **Artifacts**.
7. Download `NODE-Player-Mobile-v0.1.1-TEST-APK.zip`.
8. Extract the APK (`app-debug.apk`) and open it on your Android device. You may need to allow installing apps from that source.

## IMPORTANT: test APK versus official release APK

- The GitHub Actions output is a **debug/testing APK**. It is installable on Android 10+ but isn't the long-term signing identity used for public updates.
- Each fresh cloud runner can use a different ephemeral Android debug signing key. A subsequent debug APK may not update over the old one; you may have to uninstall the earlier test build. Uninstalling may remove the app's local database. Music files in your regular `Music/` folder are not stored inside the app and aren't deleted by an uninstall.
- To distribute **official in-place auto-updates**, we'll need one permanent, privately controlled Android release signing key. Use the release guide to create/configure it. Never put keystore or passwords in the source ZIP or public repository.
- Do not publish a GitHub v0.1.0 Release until the application has been successfully compiled and tested on a real phone.

## No internet is needed while listening to your local music

GitHub Actions needs internet to download Android build dependencies, and app updates require internet. Actual local song playback and music library management do not.

## Common misconception

The source ZIP supplied in ChatGPT is NOT an APK. It must be built first. If ChatGPT cannot access an Android build environment and your repository, it cannot truthfully claim to have created a working APK.
