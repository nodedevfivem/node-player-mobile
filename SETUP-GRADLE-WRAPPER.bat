@echo off
setlocal
cd /d "%~dp0"
if exist "gradle\wrapper\gradle-wrapper.jar" (
  echo Gradle Wrapper exists. Checking checksum...
) else (
  echo Downloading official Gradle v8.11.1 Wrapper from Gradle...
)
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $f='gradle\wrapper\gradle-wrapper.jar'; $expected='2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046'; if (!(Test-Path $f)) { Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.11.1-wrapper.jar' -OutFile $f }; $actual=(Get-FileHash -Algorithm SHA256 $f).Hash.ToLower(); if ($actual -ne $expected) { Remove-Item $f -ErrorAction SilentlyContinue; throw 'Wrapper checksum mismatch' }; Write-Host 'Verified official Gradle wrapper.'"
if errorlevel 1 (
  echo Wrapper setup failed. Please check your internet connection.
  pause
  exit /b 1
)
echo You can now use BUILD-DEBUG.bat or SIGN-RELEASE.bat.
pause
