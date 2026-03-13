@echo off
REM Minimal wrapper that delegates to repository-level gradlew
setlocal enabledelayedexpansion
set DIR=%~dp0
REM resolve to repo root (two levels up)
for %%I in ("%DIR%\..\..") do set REPO_ROOT=%%~fI
"%REPO_ROOT%\gradlew.bat" -p "%DIR%" %*
