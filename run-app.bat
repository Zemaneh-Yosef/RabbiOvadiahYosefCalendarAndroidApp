@echo off
rem Builds the app, lets you pick devices/emulators, installs and launches it. See run-app.ps1.
set "PS=powershell"
where pwsh >nul 2>nul
if not errorlevel 1 set "PS=pwsh"
%PS% -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-app.ps1" %*
pause
