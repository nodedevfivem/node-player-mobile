@echo off
setlocal
cd /d "%~dp0"
echo =========================================================
echo  NODE PLAYER MOBILE - ONE-TIME PRIVATE SIGNING KEY SETUP
echo =========================================================
echo.
echo Your signing key is PRIVATE and must be saved forever.
echo The key will be stored OUTSIDE the app source folder. NEVER upload it.
echo.
where keytool >nul 2>nul
if errorlevel 1 (
 echo Java JDK keytool was not found. Install a trusted JDK 17+ for Windows first.
 echo Android Studio is NOT required. Then reopen this script.
  pause
  exit /b 1
)
if not exist "..\NODE-Player-SIGNING-PRIVATE" mkdir "..\NODE-Player-SIGNING-PRIVATE"
if exist "..\NODE-Player-SIGNING-PRIVATE\node-player-release.jks" (
 echo A signing identity ALREADY EXISTS. Refusing to overwrite it.
 echo Use your existing key for all future releases.
  pause
  exit /b 1
)
echo The next step will ask you to choose and confirm a strong keystore password.
echo Remember it! For PKCS12, use the SAME password for store and key.
echo.
keytool -genkeypair -v -storetype PKCS12 -keystore "..\NODE-Player-SIGNING-PRIVATE\node-player-release.jks" -alias nodeplayer -keyalg RSA -keysize 3072 -validity 10000 -dname "CN=NODE Development, OU=Android, O=NODE Development, C=PH"
if errorlevel 1 (
 echo Key generation did not succeed. Do NOT continue to publish.
  pause
  exit /b 1
)
powershell -NoProfile -ExecutionPolicy Bypass -Command "$b=[Convert]::ToBase64String([IO.File]::ReadAllBytes('..\NODE-Player-SIGNING-PRIVATE\node-player-release.jks')); [IO.File]::WriteAllText('..\NODE-Player-SIGNING-PRIVATE\KEYSTORE-BASE64-PRIVATE.txt',$b,[Text.Encoding]::ASCII)"
if errorlevel 1 (
 echo Base64 conversion failed.
  pause
  exit /b 1
)
echo.
echo KEY CREATED OUTSIDE PROJECT - MAKE AN OFFLINE BACKUP!
echo.
echo In GitHub: repository Settings - Secrets and variables - Actions.
echo Add the following FOUR repository secrets:
echo   NODE_PLAYER_KEYSTORE_BASE64 : exact contents of ..\NODE-Player-SIGNING-PRIVATE\KEYSTORE-BASE64-PRIVATE.txt
echo   NODE_PLAYER_STORE_PASSWORD : your keystore password
echo   NODE_PLAYER_KEY_ALIAS      : nodeplayer
echo   NODE_PLAYER_KEY_PASSWORD   : SAME password (PKCS12)
echo.
echo After secrets are saved, securely delete the temporary KEYSTORE-BASE64-PRIVATE.txt.
echo KEEP the .jks backed up and NEVER send it to anyone.
echo.
pause
