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
copy /Y "%APK%" "RELEASE-OUTPUT\NODE-Player-Mobile-v0.1.0.apk" >nul
powershell -NoProfile -ExecutionPolicy Bypass -Command "$f='RELEASE-OUTPUT\NODE-Player-Mobile-v0.1.0.apk'; $h=(Get-FileHash -Algorithm SHA256 $f).Hash.ToLower(); [IO.File]::WriteAllText(($f+'.sha256'), ($h+'  '+[IO.Path]::GetFileName($f)+[Environment]::NewLine), [Text.Encoding]::ASCII); Write-Host 'SHA-256:' $h"
if errorlevel 1 (
  echo Failed to create SHA-256 checksum.
  pause
  exit /b 1
)
echo.
echo SUCCESS - GitHub Release assets are in RELEASE-OUTPUT:
echo   NODE-Player-Mobile-v0.1.0.apk
echo   NODE-Player-Mobile-v0.1.0.apk.sha256
echo Sign ALL future versions with the SAME signing keystore and applicationId.
pause
