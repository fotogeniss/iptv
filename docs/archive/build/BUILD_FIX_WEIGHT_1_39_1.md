# Build Fix 1.39.1 — Compose Modifier.weight

## Failure

`Cannot access 'val RowColumnParentData?.weight: Float': it is internal in file.`

## Root cause

Ten premium Details/Live UI files explicitly imported:

```kotlin
import androidx.compose.foundation.layout.weight
```

With the project's Compose/Kotlin versions this resolves to an internal implementation symbol. `Modifier.weight(...)` is a public member extension supplied by `RowScope` or `ColumnScope` and does not need that import.

## Fix

Removed the invalid explicit import from all affected files. No layout behavior or `Modifier.weight(...)` usage was changed.

## Version

- versionName: `1.39.1`
- versionCode: `43`
