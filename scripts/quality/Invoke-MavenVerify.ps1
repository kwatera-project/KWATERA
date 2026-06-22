$ErrorActionPreference = "Stop"
. "$PSScriptRoot\common.ps1"

Invoke-MavenInEachModule `
    -Arguments @("clean", "verify") `
    -Label "Maven verify"
