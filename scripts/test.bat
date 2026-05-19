@echo off
call "%~dp0test-unit.bat"
if errorlevel 1 exit /b 1
call "%~dp0test-functional.bat"
if errorlevel 1 exit /b 1
