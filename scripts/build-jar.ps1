$ErrorActionPreference = "Stop"
$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..")
$BuildDir = Join-Path $RootDir "build/classes"
$SourcesFile = Join-Path $RootDir "build/main-sources.txt"
$ManifestFile = Join-Path $RootDir "build/manifest.mf"
$JarDir = Join-Path $RootDir "release"
$JarFile = Join-Path $JarDir "purchase-fx-service.jar"

Remove-Item -Recurse -Force (Join-Path $RootDir "build") -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force $JarDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $BuildDir | Out-Null
New-Item -ItemType Directory -Force $JarDir | Out-Null

Get-ChildItem -Path @(
    (Join-Path $RootDir "src/main/java")
) -Recurse -Filter *.java | ForEach-Object { '"{0}"' -f $_.FullName } | Set-Content -Encoding ascii $SourcesFile

javac --release 21 -d $BuildDir "@$SourcesFile"
Copy-Item -Path (Join-Path $RootDir "src/main/resources/*") -Destination $BuildDir -Recurse -Force
@"
Manifest-Version: 1.0
Main-Class: com.example.purchasefx.Application

"@ | Set-Content -Encoding ascii $ManifestFile
jar cfm $JarFile $ManifestFile -C $BuildDir .
Write-Host "Built $JarFile"
