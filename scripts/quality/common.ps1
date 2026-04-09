Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$script:RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$script:ServiceDirs = @(
    "services/config-server",
    "services/service-registry",
    "services/reservation-service"
)

function Write-Step {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    Write-Host ""
    Write-Host "=== $Message ===" -ForegroundColor Cyan
}

function Invoke-MavenInEachService {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,

        [Parameter(Mandatory = $true)]
        [string]$Label
    )

    foreach ($relativeDir in $script:ServiceDirs) {
        $servicePath = Join-Path $script:RepoRoot $relativeDir

        if (-not (Test-Path $servicePath)) {
            throw "Service directory not found: $servicePath"
        }

        Write-Step "$Label -> $relativeDir"

        Push-Location $servicePath
        try {
            & .\mvnw @Arguments
            $exitCode = $LASTEXITCODE
        }
        finally {
            Pop-Location
        }

        if ($exitCode -ne 0) {
            throw "$Label failed in $relativeDir with exit code $exitCode."
        }
    }
}