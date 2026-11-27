#Requires -Version 5.1
<#
.SYNOPSIS
  Build local des 2 APKs (widget History + launcher e-ink) + zip plugin dans dist/.
  Saute Gradle si les APKs sont a jour (sauf -Force).
#>
param(
    [ValidateSet('Debug', 'Release')][string]$Variant = 'Debug',
    [string]$Version = '',
    [switch]$Force,
    [switch]$SkipPluginZip
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot '_common.ps1')
$Root = Get-ProjectRoot

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
    Write-Host '==> APKs a jour, skip Gradle (utilise -Force pour reconstruire)' -ForegroundColor DarkGray
}

$widgetApk = Get-ModuleApkPath -Root $Root -Module app -Variant $Variant
$launcherApk = Get-ModuleApkPath -Root $Root -Module launcher -Variant $Variant
$ver = Get-ProductVersion -Root $Root -Override $Version
$suffix = $Variant.ToLowerInvariant()

$dist = Join-Path $Root 'dist'
if (-not (Test-Path $dist)) {
    New-Item -ItemType Directory -Path $dist -Force | Out-Null
}

$widgetOut = Join-Path $dist "$script:ProductName-$ver-$suffix.apk"
$launcherOut = Join-Path $dist "$script:LauncherProductName-$ver-$suffix.apk"
Copy-Item $widgetApk $widgetOut -Force
Copy-Item $launcherApk $launcherOut -Force
Write-Host "==> APK widget:   $widgetOut" -ForegroundColor Green
Write-Host "==> APK launcher: $launcherOut" -ForegroundColor Green

$zipPath = $null
if (-not $SkipPluginZip) {
    $zipPath = New-PluginZip -Root $Root -Version $ver -DistDir $dist
}

# Sortie machine-readable (derniere ligne = zip, ou widget APK si skip zip)
if ($zipPath) {
    Write-Output $zipPath
} else {
    Write-Output $widgetOut
}
