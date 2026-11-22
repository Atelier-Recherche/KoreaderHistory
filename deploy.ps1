#Requires -Version 5.1
<#
.SYNOPSIS
  Installe l'APK debug + pousse athistory.koplugin via ADB.
  Ne rebuild que si l'APK est absent, ou avec -Force.
#>
param(
    [string]$AdbDevice = '',
    [string]$KoreaderPath = '/storage/emulated/0/koreader',
    [switch]$Force,
    [switch]$SkipPlugin
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

$apkPath = Get-ApkOutputPath -Root $Root -Variant Debug
$mustBuild = $Force -or -not (Test-Path $apkPath)
if ($mustBuild) {
    $buildArgs = @{ Variant = 'Debug'; Force = $true }
    & (Join-Path $Root 'build.ps1') @buildArgs
} else {
    Write-Host "==> APK present, skip build (utilise -Force pour reconstruire)" -ForegroundColor DarkGray
}

$apk = Get-DebugApkPath -Root $Root
Write-Host "==> adb install -r $apk" -ForegroundColor Cyan
$code = Invoke-AdbQuiet -AdbCmd ($prefix + @('install', '-r', $apk))
if ($code -ne 0) { throw "adb install a echoue (code $code)" }

if (-not $SkipPlugin) {
    Push-KoreaderPlugin -Root $Root -AdbArgs $prefix -KoreaderPath $KoreaderPath
}

Write-Host '==> Deploy OK (APK + plugin KOReader)' -ForegroundColor Green
