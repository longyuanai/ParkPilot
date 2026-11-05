@echo off
setlocal
set "PSModulePath="
chcp 65001 >nul
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0zhixingyuebo-java\package.ps1" %*
