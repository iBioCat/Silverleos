<#
.SYNOPSIS
    Собирает мод и упаковывает его в silverleos.mrpack для Prism Launcher.

.DESCRIPTION
    Пак самодостаточный: мод, Fabric API и GeckoLib лежат внутри, в overrides/mods.
    Никаких внешних загрузок при установке — Prism ставит игру, загрузчик и кладёт моды.

    Имена jar-ов внутри пака намеренно без версий. Prism при обновлении перезаписывает
    одноимённые файлы, но версионное имя каждый раз новое — старый jar остался бы в папке,
    и игра упала бы на дубликате мода.

.PARAMETER SkipBuild
    Не пересобирать мод, взять готовый jar из build/libs.

.EXAMPLE
    .\tools\build-mrpack.ps1
#>

[CmdletBinding()]
param(
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$root = Split-Path -Parent $PSScriptRoot
Push-Location $root

try {
    # --- версии берём из gradle.properties, чтобы не расходились с сборкой ---
    $props = @{}
    Get-Content (Join-Path $root 'gradle.properties') | ForEach-Object {
        if ($_ -match '^\s*([^#=\s]+)\s*=\s*(.+?)\s*$') { $props[$Matches[1]] = $Matches[2] }
    }

    foreach ($key in 'version', 'minecraft_version', 'loader_version', 'fabric_api_version', 'geckolib_version') {
        if (-not $props.ContainsKey($key)) { throw "В gradle.properties нет ключа '$key'" }
    }

    $modVersion    = $props['version']
    $mcVersion     = $props['minecraft_version']
    $loaderVersion = $props['loader_version']
    $fapiVersion   = $props['fabric_api_version']
    $geckoVersion  = $props['geckolib_version']

    Write-Host "Silverleos $modVersion  (Minecraft $mcVersion, Fabric Loader $loaderVersion)" -ForegroundColor Cyan

    # --- сборка мода ---
    if (-not $SkipBuild) {
        Write-Host "`n[1/4] Собираю мод..." -ForegroundColor Yellow
        & (Join-Path $root 'gradlew.bat') build --quiet
        if ($LASTEXITCODE -ne 0) { throw "gradlew build завершился с кодом $LASTEXITCODE" }
    } else {
        Write-Host "`n[1/4] Сборка пропущена (-SkipBuild)" -ForegroundColor DarkGray
    }

    $modJar = Join-Path $root "build\libs\silverleos-$modVersion.jar"
    if (-not (Test-Path $modJar)) { throw "Не найден собранный мод: $modJar" }

    # --- зависимости, с кешом чтобы не качать каждый раз ---
    Write-Host "`n[2/4] Готовлю зависимости..." -ForegroundColor Yellow

    $cacheDir = Join-Path $root 'build\mrpack-cache'
    New-Item -ItemType Directory -Force -Path $cacheDir | Out-Null

    $deps = @(
        @{
            Name = 'fabric-api.jar'
            Cache = "fabric-api-$fapiVersion.jar"
            Url = "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$fapiVersion/fabric-api-$fapiVersion.jar"
        },
        @{
            Name = 'geckolib.jar'
            Cache = "geckolib-$mcVersion-$geckoVersion.jar"
            Url = "https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/com/geckolib/geckolib-fabric-$mcVersion/$geckoVersion/geckolib-fabric-$mcVersion-$geckoVersion.jar"
        }
    )

    foreach ($dep in $deps) {
        $cached = Join-Path $cacheDir $dep.Cache
        if (Test-Path $cached) {
            Write-Host "      из кеша: $($dep.Cache)" -ForegroundColor DarkGray
        } else {
            Write-Host "      качаю:   $($dep.Cache)"
            Invoke-WebRequest -Uri $dep.Url -OutFile $cached -UseBasicParsing
        }
        $dep.Path = $cached
    }

    # --- раскладка пака ---
    Write-Host "`n[3/4] Собираю структуру пака..." -ForegroundColor Yellow

    $stage = Join-Path $root 'build\mrpack-stage'
    if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
    $modsDir = Join-Path $stage 'overrides\mods'
    New-Item -ItemType Directory -Force -Path $modsDir | Out-Null

    Copy-Item $modJar (Join-Path $modsDir 'silverleos.jar')
    foreach ($dep in $deps) { Copy-Item $dep.Path (Join-Path $modsDir $dep.Name) }

    # files пустой намеренно: всё содержимое лежит в overrides, внешних загрузок нет
    $index = [ordered]@{
        formatVersion = 1
        game          = 'minecraft'
        versionId     = $modVersion
        name          = 'Silverleos'
        summary       = 'Ancient cave chameleon-like creature for ModJam 2026 - Echoes of the Past'
        files         = @()
        dependencies  = [ordered]@{
            'minecraft'     = $mcVersion
            'fabric-loader' = $loaderVersion
        }
    }

    $indexPath = Join-Path $stage 'modrinth.index.json'
    $json = $index | ConvertTo-Json -Depth 10
    [System.IO.File]::WriteAllText($indexPath, $json, (New-Object System.Text.UTF8Encoding($false)))

    # --- упаковка ---
    Write-Host "`n[4/4] Упаковываю..." -ForegroundColor Yellow

    $distDir = Join-Path $root 'build\dist'
    New-Item -ItemType Directory -Force -Path $distDir | Out-Null
    $outFile = Join-Path $distDir 'silverleos.mrpack'
    if (Test-Path $outFile) { Remove-Item $outFile -Force }

    # Пишем zip вручную: Compress-Archive может положить пути с обратными слешами,
    # а Prism ждёт разделители в стиле zip-спецификации.
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem

    $zip = [System.IO.Compression.ZipFile]::Open($outFile, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        Get-ChildItem $stage -Recurse -File | ForEach-Object {
            $entryName = $_.FullName.Substring($stage.Length + 1).Replace('\', '/')
            [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
                $zip, $_.FullName, $entryName,
                [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
            Write-Host "      + $entryName" -ForegroundColor DarkGray
        }
    } finally {
        $zip.Dispose()
    }

    $sizeMb = [math]::Round((Get-Item $outFile).Length / 1MB, 2)
    Write-Host "`nГотово: $outFile  ($sizeMb МБ)" -ForegroundColor Green
    Write-Host "Выпустить релиз:" -ForegroundColor Cyan
    Write-Host "  gh release create v$modVersion `"$outFile`" --title `"Silverleos $modVersion`" --notes `"...`"" -ForegroundColor Gray
}
finally {
    Pop-Location
}
