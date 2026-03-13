@echo off
REM Minimal wrapper that delegates to repository-level gradlew
setlocal enabledelayedexpansion
set DIR=%~dp0
REM Remove trailing backslash if present to avoid issues when quoting the path
if "%DIR:~-1%"=="\" set DIR=%DIR:~0,-1%
REM resolve to repo root (two levels up)
for %%I in ("%DIR%\..\..") do set REPO_ROOT=%%~fI
"%REPO_ROOT%\gradlew.bat" -p "%DIR%" %*
