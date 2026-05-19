$ErrorActionPreference = "Stop"
& (Join-Path $PSScriptRoot "test-unit.ps1")
& (Join-Path $PSScriptRoot "test-functional.ps1")
