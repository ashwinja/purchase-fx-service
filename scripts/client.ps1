$ErrorActionPreference = "Stop"
$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..")
$BuildDir = Join-Path $RootDir "build/classes"
$SourcesFile = Join-Path $RootDir "build/main-sources.txt"

New-Item -ItemType Directory -Force $BuildDir | Out-Null
Get-ChildItem -Path @(
    (Join-Path $RootDir "src/main/java")
) -Recurse -Filter *.java | ForEach-Object { '"{0}"' -f $_.FullName } | Set-Content -Encoding ascii $SourcesFile

javac --release 21 -d $BuildDir "@$SourcesFile"
Copy-Item -Path (Join-Path $RootDir "src/main/resources/*") -Destination $BuildDir -Recurse -Force
java -cp $BuildDir com.example.purchasefx.client.PurchaseFxClient @args
