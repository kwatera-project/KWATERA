Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$script:RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$script:FrontendDir = "frontend"
$script:OcrServiceDir = "services/ocr-service"

function Write-Step {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    Write-Host ""
    Write-Host "=== $Message ===" -ForegroundColor Cyan
}

function Get-MavenModuleDirs {
    $rootPomPath = Join-Path $script:RepoRoot "pom.xml"

    if (-not (Test-Path $rootPomPath)) {
        throw "Root Maven pom not found: $rootPomPath"
    }

    [xml]$rootPom = Get-Content -Raw $rootPomPath
    $moduleDirs = @($rootPom.project.modules.module | ForEach-Object { [string]$_ })

    if ($moduleDirs.Count -eq 0) {
        throw "No Maven modules are declared in $rootPomPath."
    }

    foreach ($relativeDir in $moduleDirs) {
        $modulePath = Join-Path $script:RepoRoot $relativeDir
        $modulePomPath = Join-Path $modulePath "pom.xml"

        if (-not (Test-Path $modulePath -PathType Container)) {
            throw "Maven module directory not found: $modulePath"
        }

        if (-not (Test-Path $modulePomPath -PathType Leaf)) {
            throw "Maven module pom not found: $modulePomPath"
        }
    }

    return $moduleDirs
}

function Get-MavenCommand {
    $mavenCommand = Get-Command "mvn" -ErrorAction SilentlyContinue

    if ($null -eq $mavenCommand) {
        throw "Maven 3.9 or newer is required for Java quality checks. Install Maven and expose 'mvn' on PATH."
    }

    return $mavenCommand.Source
}

function Invoke-MavenInEachModule {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,

        [Parameter(Mandatory = $true)]
        [string]$Label
    )

    $mavenCommand = Get-MavenCommand

    foreach ($relativeDir in (Get-MavenModuleDirs)) {
        $modulePath = Join-Path $script:RepoRoot $relativeDir

        Write-Step "$Label -> $relativeDir"

        Push-Location $modulePath
        try {
            Invoke-CheckedCommand `
                -Command $mavenCommand `
                -Arguments (@("-B", "-ntp") + $Arguments) `
                -FailureMessage "$Label failed in $relativeDir."
        }
        finally {
            Pop-Location
        }
    }
}

function Invoke-CheckedCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Command,

        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,

        [Parameter(Mandatory = $true)]
        [string]$FailureMessage
    )

    & $Command @Arguments
    $exitCode = $LASTEXITCODE

    if ($exitCode -ne 0) {
        throw "$FailureMessage Exit code: $exitCode."
    }
}

function Get-PythonInvocation {
    $candidates = @()

    $pythonCommand = Get-Command "python" -ErrorAction SilentlyContinue
    if ($null -ne $pythonCommand) {
        $candidates += [pscustomobject]@{
            Command = $pythonCommand.Source
            PrefixArguments = @()
        }
    }

    $pythonLauncher = Get-Command "py" -ErrorAction SilentlyContinue
    if ($null -ne $pythonLauncher) {
        $candidates += [pscustomobject]@{
            Command = $pythonLauncher.Source
            PrefixArguments = @("-3.12")
        }
    }

    foreach ($candidate in $candidates) {
        $versionArguments = @(
            $candidate.PrefixArguments + @(
                "-c",
                "import sys; raise SystemExit(0 if sys.version_info[:2] == (3, 12) else 1)"
            )
        )

        & $candidate.Command @versionArguments *> $null
        if ($LASTEXITCODE -eq 0) {
            return $candidate
        }
    }

    throw "Python 3.12 is required for OCR checks. Install it and expose either 'python' or the Windows 'py' launcher on PATH."
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
        Invoke-CheckedCommand `
            -Command "bun" `
            -Arguments @("install", "--frozen-lockfile") `
            -FailureMessage "bun install failed in $script:FrontendDir."

        Invoke-CheckedCommand `
            -Command "bun" `
            -Arguments @("run", "lint") `
            -FailureMessage "Frontend lint failed in $script:FrontendDir."

        Invoke-CheckedCommand `
            -Command "bun" `
            -Arguments @("run", "build") `
            -FailureMessage "Frontend build failed in $script:FrontendDir."
    }
    finally {
        Pop-Location
    }
}

function Invoke-OcrChecks {
    param(
        [string]$Label = "OCR quality checks"
    )

    $ocrServicePath = Join-Path $script:RepoRoot $script:OcrServiceDir
    $requirementsPath = Join-Path $ocrServicePath "requirements-dev.txt"
    $dockerfilePath = Join-Path $ocrServicePath "Dockerfile"

    if (-not (Test-Path $ocrServicePath -PathType Container)) {
        throw "OCR service directory not found: $ocrServicePath"
    }

    if (-not (Test-Path $requirementsPath -PathType Leaf)) {
        throw "OCR development requirements not found: $requirementsPath"
    }

    if (-not (Test-Path $dockerfilePath -PathType Leaf)) {
        throw "OCR Dockerfile not found: $dockerfilePath"
    }

    Write-Step "$Label -> $script:OcrServiceDir"
    $python = Get-PythonInvocation

    Push-Location $ocrServicePath
    try {
        Invoke-CheckedCommand `
            -Command $python.Command `
            -Arguments @($python.PrefixArguments + @("-m", "pip", "install", "--upgrade", "pip")) `
            -FailureMessage "pip upgrade failed in $script:OcrServiceDir."

        Invoke-CheckedCommand `
            -Command $python.Command `
            -Arguments @($python.PrefixArguments + @("-m", "pip", "install", "-r", "requirements-dev.txt")) `
            -FailureMessage "OCR dependency installation failed in $script:OcrServiceDir."

        Invoke-CheckedCommand `
            -Command $python.Command `
            -Arguments @($python.PrefixArguments + @("-m", "ruff", "check", ".")) `
            -FailureMessage "Ruff lint failed in $script:OcrServiceDir."

        Invoke-CheckedCommand `
            -Command $python.Command `
            -Arguments @($python.PrefixArguments + @("-m", "ruff", "format", "--check", ".")) `
            -FailureMessage "Ruff formatting check failed in $script:OcrServiceDir."

        Invoke-CheckedCommand `
            -Command $python.Command `
            -Arguments @(
                $python.PrefixArguments + @(
                    "-m",
                    "pytest",
                    "--cov=app",
                    "--cov-report=xml:coverage.xml",
                    "--cov-report=term-missing",
                    "-q"
                )
            ) `
            -FailureMessage "OCR pytest with coverage failed in $script:OcrServiceDir."
    }
    finally {
        Pop-Location
    }

    Write-Step "OCR Docker image build"

    Push-Location $script:RepoRoot
    try {
        Invoke-CheckedCommand `
            -Command "docker" `
            -Arguments @(
                "build",
                "-f",
                "services/ocr-service/Dockerfile",
                "-t",
                "kwatera-ocr-service:pre-pr",
                "."
            ) `
            -FailureMessage "OCR Docker image build failed."
    }
    finally {
        Pop-Location
    }
}
