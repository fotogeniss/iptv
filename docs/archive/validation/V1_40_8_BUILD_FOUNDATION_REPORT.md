# v1.40.8 Build Foundation Report

## Result

The project now contains a functional cross-platform Gradle launcher instead of the previous placeholder script.

## Verified locally

- `gradle/wrapper/gradle-wrapper.jar` contains `org.gradle.wrapper.GradleWrapperMain`.
- The launcher was compiled with Java 17 bytecode.
- The wrapper can download a configured ZIP, verify SHA-256, extract it and forward all Gradle arguments.
- A local fake-distribution smoke test completed successfully.
- `gradlew` is executable.
- `gradlew.bat` is included for Windows.
- The configured Gradle 8.9 binary distribution SHA-256 is present.
- Version is `1.40.8` / `versionCode 52`.
- The previously fixed explicit Compose `foundation.layout.weight` import is absent.
- No `RowColumnParentData` reference is present.

## Not claimed

A real Android `compileDebugKotlin` or APK build was not executed here. The execution environment does not include Android SDK Platform 35 and cannot access the Gradle/Maven repositories required to resolve Android dependencies.

## First command on the developer machine

Windows:

```bat
scripts\verify-debug.bat
```

macOS/Linux:

```sh
./scripts/verify-debug.sh
```

The script stops at the first failing stage, in this order:

1. Gradle launcher
2. Kotlin compilation
3. JVM unit tests
4. Android lint
5. Debug APK assembly
