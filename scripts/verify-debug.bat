@echo off
setlocal
cd /d "%~dp0\.."
call gradlew.bat --version || exit /b 1
call gradlew.bat clean :app:compileDebugKotlin --stacktrace || exit /b 1
call gradlew.bat :app:testDebugUnitTest --stacktrace || exit /b 1
call gradlew.bat :app:lintDebug --stacktrace || exit /b 1
call gradlew.bat :app:assembleDebug --stacktrace || exit /b 1
