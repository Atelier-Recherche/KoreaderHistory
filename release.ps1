#Requires -Version 5.1
<#
.SYNOPSIS
  Bump de version, build release, APK a cote + zip plugin seul, tag + gh release.
#>
param(
    [ValidateSet('Patch', 'Minor', 'Major')][string]$BumpKind = 'Patch',
    [switch]$DryRun,
    [switch]$SkipGit
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot '_common.ps1')
$Root = Get-ProjectRoot
Set-Location $Root

$product = 'KOReaderHistory'
$pluginSlug = 'athistory.koplugin'

$current = Get-AppVersionName -Root $Root
$code = Get-AppVersionCode -Root $Root
if ($current -notmatch '^(\d+)\.(\d+)\.(\d+)$') { throw "versionName non semver: $current" }
$maj = [int]$Matches[1]; $min = [int]$Matches[2]; $pat = [int]$Matches[3]
switch ($BumpKind) {
    'Major' { $new = "$($maj + 1).0.0" }
    'Minor' { $new = "$maj.$($min + 1).0" }
    default { $new = "$maj.$min.$($pat + 1)" }
}
$newCode = $code + 1

Write-Host "==> Release $product $current ($code) -> $new ($newCode)" -ForegroundColor Cyan
if ($DryRun) { return }

if (-not $SkipGit) {
    foreach ($c in @('git', 'gh')) {
        if (-not (Get-Command $c -ErrorAction SilentlyContinue)) { throw "Commande introuvable: $c" }
    }
    $dirty = (git status --porcelain | Out-String).Trim()
    if ($dirty) {
        throw "Arbre Git non propre.`n$dirty"
    }
}

# Bump versionName / versionCode in app/build.gradle.kts
$gradlePath = Join-Path $Root 'app\build.gradle.kts'
$gradle = Get-Content $gradlePath -Raw
$gradle = [regex]::Replace($gradle, 'versionCode\s*=\s*\d+', "versionCode = $newCode")
$gradle = [regex]::Replace($gradle, 'versionName\s*=\s*"[^"]+"', "versionName = `"$new`"")
Set-Content -Path $gradlePath -Value $gradle -NoNewline -Encoding utf8

# VERSION file for convenience
Set-Content -Path (Join-Path $Root 'VERSION') -Value $new -NoNewline -Encoding utf8

& (Join-Path $Root 'build.ps1') -Variant Release
$apk = Get-ReleaseApkPath -Root $Root

# APK a cote du zip (pas dedans)
$apkOut = Join-Path $Root "$product-$new.apk"
Copy-Item $apk $apkOut -Force
Write-Host "==> APK: $apkOut" -ForegroundColor Green

# Zip propre = uniquement le plugin (dossier athistory.koplugin/)
$stageRoot = Join-Path $Root '_stage_release'
$stagePlugin = Join-Path $stageRoot $pluginSlug
if (Test-Path $stageRoot) { Remove-Item $stageRoot -Recurse -Force }
New-Item -ItemType Directory -Path $stagePlugin -Force | Out-Null
Copy-Item (Join-Path $Root "$pluginSlug\*") $stagePlugin -Recurse -Force

$zipPath = Join-Path $Root "$pluginSlug-$new.zip"
if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
Compress-Archive -Path $stagePlugin -DestinationPath $zipPath -Force
Remove-Item $stageRoot -Recurse -Force
Write-Host "==> Zip plugin: $zipPath" -ForegroundColor Green

if (-not $SkipGit) {
    git add app/build.gradle.kts VERSION
    git commit -m "release: $new"
    git tag "v$new"
    git push origin HEAD
    git push origin "v$new"
    gh release create "v$new" $apkOut $zipPath --title "$product $new" --generate-notes
    Write-Host "==> Release GitHub v$new publiee (APK + zip plugin)" -ForegroundColor Green
} else {
    Write-Host '==> SkipGit: artefacts locaux prets, pas de push/tag' -ForegroundColor Yellow
}
