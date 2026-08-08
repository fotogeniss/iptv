#!/bin/sh
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
JAVA_CMD=${JAVA_HOME:+$JAVA_HOME/bin/}java
cd "$APP_HOME"
if ! command -v "$JAVA_CMD" >/dev/null 2>&1; then
  echo "ERROR: Java 17 or newer is required. Set JAVA_HOME or add java to PATH." >&2
  exit 1
fi
exec "$JAVA_CMD" ${JAVA_OPTS:-} ${GRADLE_OPTS:-} \
  -Dorg.gradle.appname=gradlew \
  -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
  org.gradle.wrapper.GradleWrapperMain "$@"
