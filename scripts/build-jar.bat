@echo off
setlocal enabledelayedexpansion

set "ROOT_DIR=%~dp0.."
set "BUILD_DIR=%ROOT_DIR%\build\classes"
set "SOURCES_FILE=%ROOT_DIR%\build\main-sources.txt"
set "MANIFEST_FILE=%ROOT_DIR%\build\manifest.mf"
set "JAR_DIR=%ROOT_DIR%\release"
set "JAR_FILE=%JAR_DIR%\purchase-fx-service.jar"

if exist "%ROOT_DIR%\build" rmdir /s /q "%ROOT_DIR%\build"
if exist "%JAR_DIR%" rmdir /s /q "%JAR_DIR%"
mkdir "%BUILD_DIR%"
mkdir "%JAR_DIR%"

for /r "%ROOT_DIR%\src\main\java" %%f in (*.java) do echo "%%f" >> "%SOURCES_FILE%"

javac --release 21 -d "%BUILD_DIR%" @"%SOURCES_FILE%"
if errorlevel 1 exit /b 1
xcopy "%ROOT_DIR%\src\main\resources\*" "%BUILD_DIR%\" /E /I /Y >nul

> "%MANIFEST_FILE%" echo Manifest-Version: 1.0
>> "%MANIFEST_FILE%" echo Main-Class: com.example.purchasefx.Application
>> "%MANIFEST_FILE%" echo.

jar cfm "%JAR_FILE%" "%MANIFEST_FILE%" -C "%BUILD_DIR%" .
if errorlevel 1 exit /b 1

echo Built "%JAR_FILE%"

endlocal
