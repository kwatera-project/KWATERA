$ErrorActionPreference = "Stop"
. "$PSScriptRoot\common.ps1"

Invoke-MavenInEachService `
    -Arguments @("spotless:check") `
    -Label "Spotless check"