$ErrorActionPreference = "Stop"
. "$PSScriptRoot\common.ps1"

Invoke-MavenInEachModule `
    -Arguments @("spotbugs:check") `
    -Label "SpotBugs check"
