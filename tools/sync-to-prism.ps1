<#
.SYNOPSIS
    Собирает мод и копирует свежий jar в папку mods инстанса Prism Launcher.

.DESCRIPTION
    Нужен, чтобы проверять мод в обычном лаунчере, а не в дев-клиенте.
    Путь к инстансу запоминается в tools/prism-instance.txt, поэтому имя
    инстанса достаточно указать один раз.

.EXAMPLE
    .\tools\sync-to-prism.ps1 -Instance "Silverleos 26.1.2"
    .\tools\sync-to-prism.ps1
#>
[CmdletBinding()]
param(
    # Имя инстанса в Prism Launcher. Если не указано - берётся из prism-instance.txt.
    [string]$Instance,

    # Пропустить сборку и скопировать уже собранный jar.
    [switch]$NoBuild
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$memo = Join-Path $PSScriptRoot 'prism-instance.txt'

if (-not $Instance) {
    if (Test-Path $memo) {
        $Instance = (Get-Content $memo -Raw).Trim()
    } else {
        Write-Host "Не указан инстанс. Запусти один раз с именем:" -ForegroundColor Yellow
        Write-Host '  .\tools\sync-to-prism.ps1 -Instance "Silverleos 26.1.2"' -ForegroundColor Yellow
        exit 1
    }
}

$instanceRoot = Join-Path $env:APPDATA "PrismLauncher\instances\$Instance"
if (-not (Test-Path $instanceRoot)) {
    Write-Host "Инстанс '$Instance' не найден в $env:APPDATA\PrismLauncher\instances" -ForegroundColor Red
    Write-Host "Доступные инстансы:" -ForegroundColor Yellow
    Get-ChildItem (Join-Path $env:APPDATA 'PrismLauncher\instances') -Directory |
        ForEach-Object { "  $($_.Name)" }
    exit 1
}

# Prism хранит игру в .minecraft или minecraft - зависит от версии и настроек.
$mods = $null
foreach ($candidate in @('.minecraft', 'minecraft')) {
    $path = Join-Path $instanceRoot "$candidate\mods"
    if (Test-Path (Join-Path $instanceRoot $candidate)) { $mods = $path; break }
}
if (-not $mods) { throw "Внутри инстанса не найдена папка .minecraft или minecraft" }
New-Item -ItemType Directory -Force -Path $mods | Out-Null

if (-not $NoBuild) {
    Write-Host "Собираю мод..." -ForegroundColor Cyan
    Push-Location $repo
    try {
        & (Join-Path $repo 'gradlew.bat') build
        if ($LASTEXITCODE -ne 0) { throw "Сборка упала (код $LASTEXITCODE)" }
    } finally { Pop-Location }
}

$jar = Get-ChildItem (Join-Path $repo 'build\libs') -Filter '*.jar' |
    Where-Object { $_.Name -notlike '*-sources.jar' } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if (-not $jar) { throw "jar не найден в build\libs - сначала собери мод" }

# Старые версии мода убираем, иначе Fabric упадёт на дубликате мода.
Get-ChildItem $mods -Filter 'silverleos*.jar' -ErrorAction SilentlyContinue | Remove-Item -Force
Copy-Item $jar.FullName (Join-Path $mods $jar.Name) -Force

Set-Content -Path $memo -Value $Instance -NoNewline -Encoding UTF8

Write-Host ""
Write-Host "Готово: $($jar.Name) -> $Instance" -ForegroundColor Green
Write-Host "Запускай инстанс в Prism. Мод обновлён." -ForegroundColor Green
