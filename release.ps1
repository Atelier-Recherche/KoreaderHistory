#Requires -Version 5.1
<#
.SYNOPSIS
  Si l'arbre git est propre: bump VERSION (+ gradle app/launcher), build release,
  artefacts dans dist/, tag et gh release (2 APKs + zip plugin).
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

$plugin = Get-PluginFolderName
$current = Get-ProductVersion -Root $Root
$codeApp = Get-ModuleVersionCode -Root $Root -Module app
$codeLauncher = Get-ModuleVersionCode -Root $Root -Module launcher

if ($current -notmatch '^(\d+)\.(\d+)\.(\d+)$') { throw "Version non semver: $current" }
$maj = [int]$Matches[1]; $min = [int]$Matches[2]; $pat = [int]$Matches[3]
switch ($BumpKind) {
    'Major' { $new = "$($maj + 1).0.0" }
    'Minor' { $new = "$maj.$($min + 1).0" }
    default { $new = "$maj.$min.$($pat + 1)" }
}
$newCodeApp = $codeApp + 1
$newCodeLauncher = $codeLauncher + 1

Write-Host "==> Release $script:ProductName $current -> $new (app code $codeApp->$newCodeApp, launcher $codeLauncher->$newCodeLauncher)" -ForegroundColor Cyan
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

Set-ModuleVersion -Root $Root -Module app -VersionName $new -VersionCode $newCodeApp
Set-ModuleVersion -Root $Root -Module launcher -VersionName $new -VersionCode $newCodeLauncher
Set-Content -Path (Join-Path $Root 'VERSION') -Value $new -NoNewline -Encoding utf8

Write-Host '==> Build Release…' -ForegroundColor Cyan
& (Join-Path $Root 'build.ps1') -Variant Release -Version $new -Force | Out-Null

$dist = Join-Path $Root 'dist'
$widgetApk = Join-Path $dist "$script:ProductName-$new-release.apk"
$launcherApk = Join-Path $dist "$script:LauncherProductName-$new-release.apk"
$zipPath = Join-Path $dist "$plugin-$new.zip"

foreach ($p in @($widgetApk, $launcherApk, $zipPath)) {
    if (-not (Test-Path $p)) { throw "Artefact manquant apres build: $p" }
}

if ($SkipGit) {
    Write-Host '==> SkipGit: artefacts locaux prets dans dist/' -ForegroundColor Yellow
    Write-Host "    $widgetApk"
    Write-Host "    $launcherApk"
    Write-Host "    $zipPath"
    return
}

$remoteUrl = (git remote get-url origin).Trim()
if ($remoteUrl -notmatch 'github\.com[:/](?<owner>[^/]+)/(?<repo>[^/]+?)(?:\.git)?/?$') {
    throw "Impossible de deduire owner/repo depuis origin: $remoteUrl"
}
$ghRepo = "$($Matches['owner'])/$($Matches['repo'])"
Write-Host "==> GitHub repo: $ghRepo"

git add app/build.gradle.kts launcher/build.gradle.kts VERSION
git commit -m $new
if ($LASTEXITCODE -ne 0) { throw 'git commit a echoue' }

git tag "v$new"
if ($LASTEXITCODE -ne 0) { throw "git tag v$new a echoue" }

git push origin HEAD
if ($LASTEXITCODE -ne 0) { throw 'git push HEAD a echoue' }

git push origin "v$new"
if ($LASTEXITCODE -ne 0) { throw "git push tag v$new a echoue" }

gh release create "v$new" $widgetApk $launcherApk $zipPath `
    --repo $ghRepo `
    --title "$script:ProductName $new" `
    --generate-notes
if ($LASTEXITCODE -ne 0) {
    throw "gh release create a echoue (repo=$ghRepo). Tag v$new est deja pousse — cree la release a la main si besoin."
}

Write-Host "==> Release v$new publiee" -ForegroundColor Green
Write-Host "    $widgetApk"
Write-Host "    $launcherApk"
Write-Host "    $zipPath"
