@echo off
chcp 65001 >nul
mode con: cols=110 lines=38 >nul 2>&1
title Notium - Build System
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build-tools\build-engine.ps1"
echo.
echo Press any key to close...
pause >nul
