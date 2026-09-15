@echo off
setlocal EnableExtensions
set "ROOT=%~dp0"
set "BACKEND=%ROOT%backend"
set "FRONTEND=%ROOT%frontend"

echo [1/4] Checking Java, Node.js, and npm...
where java >nul 2>&1 || goto :missing_java
where node >nul 2>&1 || goto :missing_node
where npm >nul 2>&1 || goto :missing_npm
set "MVN_CMD=mvnw.cmd"
where mvn >nul 2>&1 && set "MVN_CMD=mvn"

echo [2/4] Installing frontend packages when needed...
if not exist "%FRONTEND%\node_modules" (
  pushd "%FRONTEND%"
  call npm.cmd install
  if errorlevel 1 goto :npm_failed
  popd
)

echo [3/4] Starting backend and frontend...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%ROOT%launch-ecshop-services.ps1"
if errorlevel 1 goto :service_failed

echo [4/4] Waiting for services...
for /L %%N in (1,1,60) do (
  powershell.exe -NoProfile -Command "$b=Test-NetConnection 127.0.0.1 -Port 8080 -InformationLevel Quiet; $f=Test-NetConnection 127.0.0.1 -Port 5173 -InformationLevel Quiet; if($b -and $f){exit 0}else{exit 1}" >nul 2>&1
  if not errorlevel 1 goto :ready
  powershell.exe -NoProfile -Command "Start-Sleep -Seconds 1" >nul 2>&1
)
echo WARNING: services did not become ready within 60 seconds.
echo Check the backend and frontend windows.
goto :done

:ready
echo Services are ready: http://127.0.0.1:5173
start "" "http://127.0.0.1:5173"
goto :done

:missing_java
echo ERROR: Java 21+ is required and must be available in PATH.
goto :fail
:missing_node
echo ERROR: Node.js 24+ is required and must be available in PATH.
goto :fail
:missing_npm
echo ERROR: npm is required and must be available in PATH.
goto :fail
:service_failed
echo ERROR: service process startup failed.
goto :fail
:npm_failed
popd
echo ERROR: npm install failed.
:fail
pause
exit /b 1
:done
pause
endlocal
