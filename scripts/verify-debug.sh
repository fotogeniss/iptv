#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
./gradlew --version
./gradlew clean :app:compileDebugKotlin --stacktrace
./gradlew :app:testDebugUnitTest --stacktrace
./gradlew :app:lintDebug --stacktrace
./gradlew :app:assembleDebug --stacktrace
