@echo off
setlocal
cd /d "%~dp0"
if not exist "gradle\wrapper\gradle-wrapper.jar" call "SETUP-GRADLE-WRAPPER.bat"
if not exist "gradle\wrapper\gradle-wrapper.jar" exit /b 1
call "gradlew.bat" --no-daemon assembleDebug
if errorlevel 1 (
  echo.
  echo BUILD FAILED. Copy the full error text when reporting a problem.
  pause
  exit /b 1
)
echo.
echo DEBUG APK: app\build\outputs\apk\debug\app-debug.apk
echo This DEBUG APK cannot update a separately signed RELEASE installation.
pause
