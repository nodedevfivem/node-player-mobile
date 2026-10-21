@echo off
setlocal
set "APP_HOME=%~dp0"
if not exist "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" (
  echo Missing Gradle wrapper. Run SETUP-GRADLE-WRAPPER.bat first.
  exit /b 1
)
set "JAVA_EXE=java.exe"
if defined JAVA_HOME set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
"%JAVA_EXE%" -Xmx64m -Xms64m -classpath "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
