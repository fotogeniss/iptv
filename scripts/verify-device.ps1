[CmdletBinding()]
param(
    [string]$Serial = $env:ANDROID_SERIAL,
    [ValidateSet("Current", "Portrait", "Landscape")]
    [string]$Orientation = "Current",
    [switch]$SkipInstrumentation,
    [string]$OutputRoot = "validation/device-runs"
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

function Invoke-Adb {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)
    $output = & $script:adbPath -s $script:deviceSerial @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "adb $($Arguments -join ' ') failed:`n$($output -join [Environment]::NewLine)"
    }
    return $output
}

function Save-AdbText {
    param([string]$Path, [Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)
    $text = Invoke-Adb @Arguments
    $text | Out-File -LiteralPath $Path -Encoding utf8
}

$adbCommand = Get-Command adb -ErrorAction SilentlyContinue
if ($null -eq $adbCommand) {
    throw "adb is not available. Install Android platform-tools or add it to PATH."
}
$script:adbPath = $adbCommand.Source

$readyDevices = @(
    & $script:adbPath devices |
        Select-Object -Skip 1 |
        ForEach-Object {
            if ($_ -match '^([^\s]+)\s+device$') { $Matches[1] }
        }
)
if ($readyDevices.Count -eq 0) {
    throw "No ready Android device/emulator is visible to adb."
}
if ([string]::IsNullOrWhiteSpace($Serial)) {
    if ($readyDevices.Count -ne 1) {
        throw "Multiple devices are attached. Pass -Serial or set ANDROID_SERIAL."
    }
    $Serial = $readyDevices[0]
}
if ($Serial -notin $readyDevices) {
    throw "Device '$Serial' is not in adb's ready-device list."
}
$script:deviceSerial = $Serial

$safeSerial = $Serial -replace '[^A-Za-z0-9._-]', '_'
$runStamp = Get-Date -Format "yyyyMMdd-HHmmss"
$runDirectory = Join-Path $repoRoot (Join-Path $OutputRoot "$runStamp-$safeSerial-$($Orientation.ToLowerInvariant())")
New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null

$deviceProperties = @{
    serial = $Serial
    manufacturer = ((Invoke-Adb shell getprop ro.product.manufacturer) -join "").Trim()
    model = ((Invoke-Adb shell getprop ro.product.model) -join "").Trim()
    device = ((Invoke-Adb shell getprop ro.product.device) -join "").Trim()
    sdk = ((Invoke-Adb shell getprop ro.build.version.sdk) -join "").Trim()
    release = ((Invoke-Adb shell getprop ro.build.version.release) -join "").Trim()
    abi = ((Invoke-Adb shell getprop ro.product.cpu.abi) -join "").Trim()
    density = ((Invoke-Adb shell wm density) -join " ").Trim()
    size = ((Invoke-Adb shell wm size) -join " ").Trim()
    orientation = $Orientation
    startedAt = (Get-Date).ToString("o")
}
$deviceProperties | ConvertTo-Json | Out-File -LiteralPath (Join-Path $runDirectory "device.json") -Encoding utf8

$originalAutoRotation = $null
$originalUserRotation = $null
try {
    if ($Orientation -ne "Current") {
        $originalAutoRotation = ((Invoke-Adb shell settings get system accelerometer_rotation) -join "").Trim()
        $originalUserRotation = ((Invoke-Adb shell settings get system user_rotation) -join "").Trim()
        Invoke-Adb shell settings put system accelerometer_rotation 0 | Out-Null
        $rotation = if ($Orientation -eq "Landscape") { "1" } else { "0" }
        Invoke-Adb shell settings put system user_rotation $rotation | Out-Null
        Start-Sleep -Seconds 1
    }

    if (-not $SkipInstrumentation) {
        & "$repoRoot\gradlew.bat" :app:connectedDebugAndroidTest --stacktrace --no-daemon --console=plain 2>&1 |
            Tee-Object -FilePath (Join-Path $runDirectory "instrumentation.txt")
        if ($LASTEXITCODE -ne 0) {
            throw "connectedDebugAndroidTest failed. See instrumentation.txt."
        }
    }

    Invoke-Adb logcat -c | Out-Null
    Invoke-Adb shell am force-stop com.prelude.iptv | Out-Null
    Save-AdbText (Join-Path $runDirectory "launch.txt") shell am start -W -n com.prelude.iptv/.StartupActivity
    Start-Sleep -Seconds 4

    $appPid = ((Invoke-Adb shell pidof com.prelude.iptv) -join "").Trim()
    if ([string]::IsNullOrWhiteSpace($appPid)) {
        throw "Prelude+ did not remain alive after launch."
    }

    Save-AdbText (Join-Path $runDirectory "logcat.txt") logcat -d -v threadtime
    Save-AdbText (Join-Path $runDirectory "meminfo.txt") shell dumpsys meminfo com.prelude.iptv
    Save-AdbText (Join-Path $runDirectory "activity.txt") shell dumpsys activity activities

    $remoteScreenshot = "/sdcard/prelude-device-qa-$runStamp.png"
    Invoke-Adb shell screencap -p $remoteScreenshot | Out-Null
    Invoke-Adb pull $remoteScreenshot (Join-Path $runDirectory "launch.png") | Out-Null
    Invoke-Adb shell rm $remoteScreenshot | Out-Null

    $logText = Get-Content -LiteralPath (Join-Path $runDirectory "logcat.txt") -Raw
    $fatalPatterns = @(
        'FATAL EXCEPTION',
        'ANR in com\.prelude\.iptv',
        'Force finishing activity com\.prelude\.iptv'
    )
    $fatalMatches = @($fatalPatterns | Where-Object { $logText -match $_ })
    if ($fatalMatches.Count -gt 0) {
        throw "Crash/ANR signature detected in logcat: $($fatalMatches -join ', ')"
    }

    $deviceProperties.completedAt = (Get-Date).ToString("o")
    $deviceProperties.status = "PASS"
    $deviceProperties.pid = $appPid
    $deviceProperties | ConvertTo-Json | Out-File -LiteralPath (Join-Path $runDirectory "result.json") -Encoding utf8
    Write-Host "DEVICE QA PASS: $Serial ($($deviceProperties.model), API $($deviceProperties.sdk))"
    Write-Host "Artifacts: $runDirectory"
}
catch {
    $deviceProperties.completedAt = (Get-Date).ToString("o")
    $deviceProperties.status = "FAIL"
    $deviceProperties.failure = $_.Exception.Message
    $deviceProperties | ConvertTo-Json | Out-File -LiteralPath (Join-Path $runDirectory "result.json") -Encoding utf8
    throw
}
finally {
    if ($Orientation -ne "Current" -and $null -ne $originalAutoRotation) {
        Invoke-Adb shell settings put system accelerometer_rotation $originalAutoRotation | Out-Null
        if (-not [string]::IsNullOrWhiteSpace($originalUserRotation)) {
            Invoke-Adb shell settings put system user_rotation $originalUserRotation | Out-Null
        }
    }
}
