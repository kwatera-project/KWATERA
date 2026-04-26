Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$script:RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$script:ServiceDirs = @(
    "services/config-server",
    "services/service-registry",
    "services/reservation-service"
)
$script:FrontendDir = "frontend"

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

function Invoke-FrontendBunChecks {
    param(
        [string]$Label = "Frontend checks"
    )

    $frontendPath = Join-Path $script:RepoRoot $script:FrontendDir

    if (-not (Test-Path $frontendPath)) {
        throw "Frontend directory not found: $frontendPath"
    }

    Write-Step "$Label -> $script:FrontendDir"

    Push-Location $frontendPath
    try {
        bun install --frozen-lockfile
        $installExitCode = $LASTEXITCODE
        if ($installExitCode -ne 0) {
            throw "bun install failed in $script:FrontendDir with exit code $installExitCode."
        }

        bun run lint
        $lintExitCode = $LASTEXITCODE
        if ($lintExitCode -ne 0) {
            throw "Frontend lint failed in $script:FrontendDir with exit code $lintExitCode."
        }

        bun run build
        $buildExitCode = $LASTEXITCODE
        if ($buildExitCode -ne 0) {
            throw "Frontend build failed in $script:FrontendDir with exit code $buildExitCode."
        }
    }
    finally {
        Pop-Location
    }
}