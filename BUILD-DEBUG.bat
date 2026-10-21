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
if not exist "DEBUG-OUTPUT" mkdir "DEBUG-OUTPUT"
copy /Y "app\build\outputs\apk\debug\app-debug.apk" "DEBUG-OUTPUT\NODE-Player.apk" >nul
if errorlevel 1 (
  echo Could not copy APK to DEBUG-OUTPUT.
  pause
  exit /b 1
)
echo DEBUG APK: DEBUG-OUTPUT\NODE-Player.apk
echo This DEBUG APK cannot update a separately signed RELEASE installation.
pause
