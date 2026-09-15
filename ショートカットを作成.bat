@echo off
setlocal
set "ROOT=%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%ROOT%create-ecshop-shortcut.ps1"
if errorlevel 1 (
  echo ERROR: Shortcut creation failed.
  pause
  exit /b 1
)
echo Created: %LINK%
echo.
echo Note: the .lnk file is specific to this PC's current path.
echo If the project is copied to another PC, run this batch again there.
pause
endlocal
