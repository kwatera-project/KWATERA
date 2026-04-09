$ErrorActionPreference = "Stop"
. "$PSScriptRoot\common.ps1"

Invoke-MavenInEachService `
    -Arguments @("spotbugs:check") `
    -Label "SpotBugs check"