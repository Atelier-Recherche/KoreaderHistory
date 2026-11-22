#Requires -Version 5.1
<#
.SYNOPSIS
  Build local de l'APK Android.
  Saute Gradle si l'APK existe deja et qu'aucune source n'est plus recente (sauf -Force).
#>
param(
    [ValidateSet('Debug', 'Release')][string]$Variant = 'Debug',
    [switch]$Force
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot '_common.ps1')
$Root = Get-ProjectRoot

$synced = [bool](Sync-BridgeAssets -Root $Root)
$apkPath = Get-ApkOutputPath -Root $Root -Variant $Variant
$needsBuild = $Force -or $synced -or -not (Test-Path $apkPath) -or (Test-ApkNeedsRebuild -Root $Root -Variant $Variant)

if (-not $needsBuild) {
    Write-Host "==> APK deja a jour, skip build: $apkPath" -ForegroundColor DarkGray
    exit 0
}

Write-Host "==> Build $Variant" -ForegroundColor Cyan
$task = if ($Variant -eq 'Release') { 'assembleRelease' } else { 'assembleDebug' }
Invoke-Gradle -Root $Root -GradleArgs @($task)

$apk = if ($Variant -eq 'Release') { Get-ReleaseApkPath -Root $Root } else { Get-DebugApkPath -Root $Root }
Write-Host "==> OK: $apk" -ForegroundColor Green
