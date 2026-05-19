$ErrorActionPreference = "Stop"
$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..")
$BuildDir = Join-Path $RootDir "build/classes"
$SourcesFile = Join-Path $RootDir "build/unit-sources.txt"

Remove-Item -Recurse -Force (Join-Path $RootDir "build") -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $BuildDir | Out-Null

Get-ChildItem -Path @(
    (Join-Path $RootDir "src/main/java"),
    (Join-Path $RootDir "src/test/unit/java")
) -Recurse -Filter *.java | ForEach-Object { '"{0}"' -f $_.FullName } | Set-Content -Encoding ascii $SourcesFile

javac --release 21 -d $BuildDir "@$SourcesFile"
Copy-Item -Path (Join-Path $RootDir "src/main/resources/*") -Destination $BuildDir -Recurse -Force
java -ea -cp $BuildDir com.example.purchasefx.AppConfigTest
java -ea -cp $BuildDir com.example.purchasefx.PurchaseServiceTest
java -ea -cp $BuildDir com.example.purchasefx.TreasuryFiscalDataClientTest
