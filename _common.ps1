#Requires -Version 5.1
# Shared helpers for build / deploy / release scripts.

Set-StrictMode -Version Latest

function Get-ProjectRoot {
    $PSScriptRoot
}

function Resolve-ProjectJavaHome {
    <#
    .SYNOPSIS
      Prefer a JDK whose version string Gradle can parse (no 4-part versions like 25.0.5.1 / 17.0.21.1).
    #>
    $candidates = @(
        $env:ANDROID_STUDIO_JDK,
        'C:\Program Files\Android\Android Studio\jbr',
        "${env:LOCALAPPDATA}\Programs\Android Studio\jbr",
        $env:JAVA_HOME
    ) | Where-Object { $_ -and (Test-Path (Join-Path $_ 'bin\java.exe')) }

    foreach ($jdkCandidate in $candidates) {
        $java = Join-Path $jdkCandidate 'bin\java.exe'
        # java -version writes to stderr; do not let ErrorActionPreference=Stop abort
        $verLine = cmd /c "`"$java`" -version 2>&1" | Select-Object -First 1
        if ("$verLine" -match 'version\s+"(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:\.(\d+))?"') {
            $parts = @($Matches[1], $Matches[2], $Matches[3], $Matches[4]) | Where-Object { $_ }
            # Gradle VersionNumber rejects 4+ segments (e.g. Temurin 25.0.5.1)
            if ($parts.Count -le 3) {
                $major = [int]$Matches[1]
                if ($major -ge 17) { return (Resolve-Path $jdkCandidate).Path }
            }
        }
    }
    throw @"
Aucun JDK compatible Gradle trouve (17+ avec version a 3 segments max).
Installez Android Studio (JBR) ou definissez ANDROID_STUDIO_JDK.
JAVA_HOME actuel: $($env:JAVA_HOME)
"@
}

function Use-ProjectJava {
    $jdk = Resolve-ProjectJavaHome
    $env:JAVA_HOME = $jdk
    $env:PATH = "$jdk\bin;$env:PATH"
    Write-Host "==> JAVA_HOME=$jdk" -ForegroundColor DarkGray
    return $jdk
}

function Get-AppVersionName {
    param([string]$Root = (Get-ProjectRoot))
    $gradle = Join-Path $Root 'app\build.gradle.kts'
    $text = Get-Content $gradle -Raw
    if ($text -match 'versionName\s*=\s*"([^"]+)"') { return $Matches[1] }
    return '0.0.0'
}

function Get-AppVersionCode {
    param([string]$Root = (Get-ProjectRoot))
    $gradle = Join-Path $Root 'app\build.gradle.kts'
    $text = Get-Content $gradle -Raw
    if ($text -match 'versionCode\s*=\s*(\d+)') { return [int]$Matches[1] }
    return 1
}

function Invoke-Gradle {
    param(
        [Parameter(Mandatory)][string]$Root,
        [Parameter(Mandatory)][string[]]$GradleArgs
    )
    Use-ProjectJava | Out-Null
    $gradlew = Join-Path $Root 'gradlew.bat'
    if (-not (Test-Path $gradlew)) { throw "gradlew.bat introuvable: $gradlew" }
    Push-Location $Root
    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & $gradlew @GradleArgs
        if ($LASTEXITCODE -ne 0) { throw "Gradle a echoue (code $LASTEXITCODE): $($GradleArgs -join ' ')" }
    } finally {
        $ErrorActionPreference = $prevEap
        Pop-Location
    }
}

function Get-DebugApkPath {
    param([string]$Root = (Get-ProjectRoot))
    $apk = Join-Path $Root 'app\build\outputs\apk\debug\app-debug.apk'
    if (-not (Test-Path $apk)) { throw "APK debug introuvable: $apk — lancez d'abord build.ps1" }
    return $apk
}

function Get-ReleaseApkPath {
    param([string]$Root = (Get-ProjectRoot))
    $signed = Join-Path $Root 'app\build\outputs\apk\release\app-release.apk'
    $unsigned = Join-Path $Root 'app\build\outputs\apk\release\app-release-unsigned.apk'
    if (Test-Path $signed) { return $signed }
    if (Test-Path $unsigned) { return $unsigned }
    throw "APK release introuvable sous app\build\outputs\apk\release\"
}

function Get-ApkOutputPath {
    param(
        [Parameter(Mandatory)][string]$Root,
        [ValidateSet('Debug', 'Release')][string]$Variant = 'Debug'
    )
    if ($Variant -eq 'Release') {
        $signed = Join-Path $Root 'app\build\outputs\apk\release\app-release.apk'
        $unsigned = Join-Path $Root 'app\build\outputs\apk\release\app-release-unsigned.apk'
        if (Test-Path $signed) { return $signed }
        if (Test-Path $unsigned) { return $unsigned }
        return $signed
    }
    return (Join-Path $Root 'app\build\outputs\apk\debug\app-debug.apk')
}

function Test-ApkNeedsRebuild {
    <#
    .SYNOPSIS
      True si l'APK manque ou si une source (app, gradle, plugin) est plus recente.
    #>
    param(
        [Parameter(Mandatory)][string]$Root,
        [ValidateSet('Debug', 'Release')][string]$Variant = 'Debug'
    )
    $apk = Get-ApkOutputPath -Root $Root -Variant $Variant
    if (-not (Test-Path $apk)) { return $true }

    $apkTime = (Get-Item $apk).LastWriteTimeUtc
    $watch = @(
        (Join-Path $Root 'app\src'),
        (Join-Path $Root 'app\build.gradle.kts'),
        (Join-Path $Root 'build.gradle.kts'),
        (Join-Path $Root 'settings.gradle.kts'),
        (Join-Path $Root 'gradle.properties'),
        (Join-Path $Root 'athistory.koplugin')
    )
    foreach ($path in $watch) {
        if (-not (Test-Path $path)) { continue }
        $item = Get-Item $path
        if ($item.PSIsContainer) {
            $newer = Get-ChildItem $path -Recurse -File -ErrorAction SilentlyContinue |
                Where-Object { $_.LastWriteTimeUtc -gt $apkTime } |
                Select-Object -First 1
            if ($newer) { return $true }
        } elseif ($item.LastWriteTimeUtc -gt $apkTime) {
            return $true
        }
    }
    return $false
}

function Sync-BridgeAssets {
    param([Parameter(Mandatory)][string]$Root)
    $src = Join-Path $Root 'athistory.koplugin'
    $dst = Join-Path $Root 'app\src\main\assets\athistory.koplugin'
    if (-not (Test-Path $src)) { throw "Plugin introuvable: $src" }

    $needsSync = $false
    if (-not (Test-Path $dst)) {
        $needsSync = $true
    } else {
        $srcFiles = Get-ChildItem $src -File
        foreach ($f in $srcFiles) {
            $destFile = Join-Path $dst $f.Name
            if (-not (Test-Path $destFile)) { $needsSync = $true; break }
            if ((Get-FileHash $f.FullName -Algorithm SHA256).Hash -ne (Get-FileHash $destFile -Algorithm SHA256).Hash) {
                $needsSync = $true
                break
            }
        }
    }
    if (-not $needsSync) { return $false }

    if (Test-Path $dst) { Remove-Item $dst -Recurse -Force }
    New-Item -ItemType Directory -Path $dst -Force | Out-Null
    Copy-Item (Join-Path $src '*') $dst -Recurse -Force
    Write-Host '==> Bridge synchronise -> assets/athistory.koplugin' -ForegroundColor DarkGray
    return $true
}

function Push-KoreaderPlugin {
    <#
    .SYNOPSIS
      Copie athistory.koplugin vers le dossier plugins KOReader de l'appareil via adb.
    #>
    param(
        [Parameter(Mandatory)][string]$Root,
        [string[]]$AdbArgs = @(),
        [string]$KoreaderPath = '/storage/emulated/0/koreader'
    )
    $pluginSlug = 'athistory.koplugin'
    $localPlugin = Join-Path $Root $pluginSlug
    if (-not (Test-Path $localPlugin)) { throw "Plugin introuvable: $localPlugin" }

    $remotePlugins = ($KoreaderPath.TrimEnd('/') + '/plugins')
    $remote = "$remotePlugins/$pluginSlug"
    Write-Host "==> adb push plugin -> $remote" -ForegroundColor Cyan
    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $mkdirOut = & adb @AdbArgs shell "mkdir -p '$remote' '$KoreaderPath/settings/athistory'" 2>&1
        if ($LASTEXITCODE -ne 0) { throw "adb mkdir plugins a echoue (code $LASTEXITCODE): $mkdirOut" }

        Get-ChildItem $localPlugin -File | ForEach-Object {
            $pushOut = & adb @AdbArgs push $_.FullName "$remote/" 2>&1
            if ($LASTEXITCODE -ne 0) { throw "adb push echoue: $($_.Name) — $pushOut" }
            Write-Host "    $($_.Name)" -ForegroundColor DarkGray
        }
    } finally {
        $ErrorActionPreference = $prevEap
    }
}
