$ErrorActionPreference = "Stop"
. "$PSScriptRoot\common.ps1"

Invoke-MavenInEachModule `
    -Arguments @("spotless:check") `
    -Label "Spotless check"
