#Requires -Version 5.1
<#
.SYNOPSIS
  Build + install les 2 apps (widget History + launcher e-ink), pousse le plugin KOReader.
#>
param(
    [string]$AdbDevice = '',
    [string]$KoreaderPath = '/storage/emulated/0/koreader',
    [string]$RemoteApkDir = '/sdcard/Download/AtelierKOReaderHistory',
    [ValidateSet('Debug', 'Release')][string]$Variant = 'Debug',
    [switch]$Force,
    [switch]$SkipPlugin,
    [switch]$SkipInstall
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot '_common.ps1')
$Root = Get-ProjectRoot

function Invoke-AdbQuiet {
    param([Parameter(Mandatory)][string[]]$AdbCmd)
    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = & adb @AdbCmd 2>&1
        $code = $LASTEXITCODE
        foreach ($line in @($output)) {
            if ($null -ne $line -and "$line".Trim().Length -gt 0) {
                Write-Host "$line"
            }
        }
        return $code
    } finally {
        $ErrorActionPreference = $prevEap
    }
}

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    throw 'adb introuvable dans le PATH'
}

$prevEap = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
$devices = @(& adb devices 2>$null | Select-Object -Skip 1 | Where-Object { $_ -match '\tdevice$' })
$ErrorActionPreference = $prevEap
if (-not $devices -or $devices.Count -eq 0) {
    throw 'Aucun appareil ADB en etat "device". Branchez un telephone / activez le debogage USB.'
}

if ($AdbDevice) {
    $prefix = @('-s', $AdbDevice)
} elseif ($devices.Count -gt 1) {
    throw "Plusieurs appareils ADB. Precisez -AdbDevice. Liste:`n$($devices -join "`n")"
} else {
    $prefix = @()
}

$synced = [bool](Sync-BridgeAssets -Root $Root)
$needWidget = $Force -or $synced -or (Test-ApkNeedsRebuild -Root $Root -Module app -Variant $Variant)
$needLauncher = $Force -or (Test-ApkNeedsRebuild -Root $Root -Module launcher -Variant $Variant)

if ($needWidget -or $needLauncher) {
    Write-Host "==> Build :app + :launcher ($Variant)" -ForegroundColor Cyan
    $tasks = if ($Variant -eq 'Release') {
        @(':app:assembleRelease', ':launcher:assembleRelease')
    } else {
        @(':app:assembleDebug', ':launcher:assembleDebug')
    }
    Invoke-Gradle -Root $Root -GradleArgs $tasks
} else {
    Write-Host "==> APKs a jour, skip build (utilise -Force pour reconstruire)" -ForegroundColor DarkGray
}

$widgetApk = Get-ModuleApkPath -Root $Root -Module app -Variant $Variant
$launcherApk = Get-ModuleApkPath -Root $Root -Module launcher -Variant $Variant
$widgetVer = Get-ModuleVersionName -Root $Root -Module app
$launcherVer = Get-ModuleVersionName -Root $Root -Module launcher
$suffix = $Variant.ToLowerInvariant()

$remoteWidget = ($RemoteApkDir.TrimEnd('/') + "/KOReaderHistory-$widgetVer-$suffix.apk")
$remoteLauncher = ($RemoteApkDir.TrimEnd('/') + "/EinkLauncher-$launcherVer-$suffix.apk")

Write-Host "==> adb mkdir $RemoteApkDir" -ForegroundColor Cyan
$code = Invoke-AdbQuiet -AdbCmd ($prefix + @('shell', 'mkdir', '-p', $RemoteApkDir))
if ($code -ne 0) { throw "adb mkdir a echoue (code $code)" }

Write-Host "==> adb push widget -> $remoteWidget" -ForegroundColor Cyan
$code = Invoke-AdbQuiet -AdbCmd ($prefix + @('push', $widgetApk, $remoteWidget))
if ($code -ne 0) { throw "adb push widget a echoue (code $code)" }

Write-Host "==> adb push launcher -> $remoteLauncher" -ForegroundColor Cyan
$code = Invoke-AdbQuiet -AdbCmd ($prefix + @('push', $launcherApk, $remoteLauncher))
if ($code -ne 0) { throw "adb push launcher a echoue (code $code)" }

if (-not $SkipInstall) {
    Write-Host "==> adb install -r widget $widgetApk" -ForegroundColor Cyan
    $code = Invoke-AdbQuiet -AdbCmd ($prefix + @('install', '-r', $widgetApk))
    if ($code -ne 0) { throw "adb install widget a echoue (code $code)" }

    Write-Host "==> adb install -r launcher $launcherApk" -ForegroundColor Cyan
    $code = Invoke-AdbQuiet -AdbCmd ($prefix + @('install', '-r', $launcherApk))
    if ($code -ne 0) { throw "adb install launcher a echoue (code $code)" }
}

if (-not $SkipPlugin) {
    Push-KoreaderPlugin -Root $Root -AdbArgs $prefix -KoreaderPath $KoreaderPath
}

Write-Host '==> Deploy OK — 2 APKs (widget + launcher)' -ForegroundColor Green
Write-Host "    $remoteWidget" -ForegroundColor DarkGray
Write-Host "    $remoteLauncher" -ForegroundColor DarkGray
