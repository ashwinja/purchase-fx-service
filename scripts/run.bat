@echo off
setlocal enabledelayedexpansion

set "ROOT_DIR=%~dp0.."
set "BUILD_DIR=%ROOT_DIR%\build\classes"
set "SOURCES_FILE=%ROOT_DIR%\build\main-sources.txt"

mkdir "%BUILD_DIR%" 2>nul
if exist "%SOURCES_FILE%" del "%SOURCES_FILE%"
for /r "%ROOT_DIR%\src\main\java" %%f in (*.java) do echo "%%f" >> "%SOURCES_FILE%"

javac --release 21 -d "%BUILD_DIR%" @"%SOURCES_FILE%"
if errorlevel 1 exit /b 1
xcopy "%ROOT_DIR%\src\main\resources\*" "%BUILD_DIR%\" /E /I /Y >nul

java -cp "%BUILD_DIR%" com.example.purchasefx.Application

endlocal
