$ErrorActionPreference = "Stop"
. "$PSScriptRoot\common.ps1"

Invoke-MavenInEachService `
    -Arguments @("clean", "verify") `
    -Label "Maven verify"