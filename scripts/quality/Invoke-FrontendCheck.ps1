$ErrorActionPreference = "Stop"
. "$PSScriptRoot\common.ps1"

Invoke-FrontendBunChecks -Label "Frontend lint/build"