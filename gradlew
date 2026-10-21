#!/bin/sh
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ ! -f "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" ]; then
  echo 'Missing wrapper jar. Run SETUP-GRADLE-WRAPPER.bat on Windows or download the official Gradle 8.11.1 wrapper jar and verify its SHA-256.' >&2
  exit 1
fi
exec java -Xmx64m -Xms64m -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
