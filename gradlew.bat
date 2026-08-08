@echo off
setlocal
set APP_HOME=%~dp0
cd /d "%APP_HOME%"
if defined JAVA_HOME (
  set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_EXE=java.exe
)
"%JAVA_EXE%" -version >NUL 2>&1
if errorlevel 1 (
  echo ERROR: Java 17 or newer is required. Set JAVA_HOME or add java to PATH. 1>&2
  exit /b 1
)
"%JAVA_EXE%" %JAVA_OPTS% %GRADLE_OPTS% -Dorg.gradle.appname=gradlew -classpath "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
