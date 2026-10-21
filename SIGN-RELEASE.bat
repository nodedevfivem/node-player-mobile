@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
if not exist "signing.properties" (
  echo Missing signing.properties. Read docs\RELEASE-GUIDE.md.
  echo A release MUST use your permanent private keystore to preserve upgrade compatibility.
  pause
  exit /b 1
)
if not exist "gradle\wrapper\gradle-wrapper.jar" call "SETUP-GRADLE-WRAPPER.bat"
if not exist "gradle\wrapper\gradle-wrapper.jar" exit /b 1
call "gradlew.bat" --no-daemon clean assembleRelease
if errorlevel 1 (
  echo RELEASE BUILD FAILED.
  pause
  exit /b 1
)
set "APK=app\build\outputs\apk\release\app-release.apk"
if not exist "%APK%" (
  echo Missing signed APK.
  pause
  exit /b 1
)
if not exist "RELEASE-OUTPUT" mkdir "RELEASE-OUTPUT"
set "APKSIGNER=%LOCALAPPDATA%\Android\Sdk\build-tools\35.0.0\apksigner.bat"
if defined ANDROID_HOME if exist "%ANDROID_HOME%\build-tools\35.0.0\apksigner.bat" set "APKSIGNER=%ANDROID_HOME%\build-tools\35.0.0\apksigner.bat"
if not exist "%APKSIGNER%" (
  echo ERROR: Cannot find apksigner.bat in Android SDK 35.
  echo Cannot verify release APK signature. Refusing to publish.
  pause
  exit /b 1
)
call "%APKSIGNER%" verify "%APK%"
if errorlevel 1 (
  echo ERROR: Release APK signature verification failed.
  pause
  exit /b 1
)
copy /Y "%APK%" "RELEASE-OUTPUT\NODE-Player.apk" >nul
powershell -NoProfile -ExecutionPolicy Bypass -Command "$f='RELEASE-OUTPUT\NODE-Player.apk'; $h=(Get-FileHash -Algorithm SHA256 $f).Hash.ToLower(); [IO.File]::WriteAllText(($f+'.sha256'), ($h+'  '+[IO.Path]::GetFileName($f)+[Environment]::NewLine), [Text.Encoding]::ASCII); Write-Host 'SHA-256:' $h"
if errorlevel 1 (
  echo Failed to create SHA-256 checksum.
  pause
  exit /b 1
)
echo.
echo SUCCESS - GitHub Release assets are in RELEASE-OUTPUT:
echo   NODE-Player.apk
echo   NODE-Player.apk.sha256
echo Sign ALL future versions with the SAME signing keystore and applicationId.
pause
