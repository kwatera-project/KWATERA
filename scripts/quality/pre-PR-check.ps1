$ErrorActionPreference = "Stop"

$steps = @(
    "Invoke-SpotlessCheck.ps1",
    "Invoke-MavenVerify.ps1",
    "Invoke-SpotBugsCheck.ps1",
    "Invoke-OcrCheck.ps1",
    "Invoke-FrontendCheck.ps1"
)

Write-Host ""
Write-Host "Starting local pre-PR quality checks..." -ForegroundColor Yellow

foreach ($step in $steps) {
    $scriptPath = Join-Path $PSScriptRoot $step
    & $scriptPath
}

Write-Host ""
Write-Host "All local pre-PR quality checks passed." -ForegroundColor Green
