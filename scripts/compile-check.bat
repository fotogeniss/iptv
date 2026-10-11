@echo off
rem Ελέγχει μόνο ότι ο κώδικας κάνει compile. Δεν φτιάχνει APK.
rem Γράφει το αποτέλεσμα στο validation\compile-check.txt για να διαβάζεται απευθείας.
setlocal
cd /d "%~dp0\.."
if not defined JAVA_HOME if exist "C:\Program Files\Android\Android Studio3\jbr\bin\java.exe" set "JAVA_HOME=C:\Program Files\Android\Android Studio3\jbr"
if not defined JAVA_HOME if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
if not exist validation mkdir validation
echo START %DATE% %TIME% > validation\compile-check.txt
call gradlew.bat :app:compileDebugKotlin --console=plain >> validation\compile-check.txt 2>&1
echo EXIT=%ERRORLEVEL% %DATE% %TIME% >> validation\compile-check.txt
