@echo off
setlocal
set "PSModulePath="
chcp 65001 >nul
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0verify_tests.ps1" -NoPause
set RESULT=%ERRORLEVEL%
if /I not "%~1"=="--nopause" pause
exit /b %RESULT%
