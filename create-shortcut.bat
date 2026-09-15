@echo off
setlocal
set "ROOT=%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%ROOT%create-ecshop-shortcut.ps1"
if errorlevel 1 (
  echo ERROR: shortcut creation failed.
  pause
  exit /b 1
)
pause
endlocal
