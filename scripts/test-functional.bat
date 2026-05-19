@echo off
setlocal enabledelayedexpansion

set "ROOT_DIR=%~dp0.."
set "BUILD_DIR=%ROOT_DIR%\build\classes"
set "SOURCES_FILE=%ROOT_DIR%\build\functional-sources.txt"

if exist "%ROOT_DIR%\build" rmdir /s /q "%ROOT_DIR%\build"
mkdir "%BUILD_DIR%"

if exist "%SOURCES_FILE%" del "%SOURCES_FILE%"
for /r "%ROOT_DIR%\src\main\java" %%f in (*.java) do echo "%%f" >> "%SOURCES_FILE%"
for /r "%ROOT_DIR%\src\test\functional\java" %%f in (*.java) do echo "%%f" >> "%SOURCES_FILE%"

javac --release 21 -d "%BUILD_DIR%" @"%SOURCES_FILE%"
if errorlevel 1 exit /b 1
xcopy "%ROOT_DIR%\src\main\resources\*" "%BUILD_DIR%\" /E /I /Y >nul

java -ea -cp "%BUILD_DIR%" com.example.purchasefx.PurchaseApplicationFunctionalTest
if errorlevel 1 exit /b 1

endlocal
