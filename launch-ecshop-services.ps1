$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$backend = Join-Path $root 'backend'
$frontend = Join-Path $root 'frontend'
$backendOut = Join-Path $backend 'ecshop-backend.out.log'
$backendErr = Join-Path $backend 'ecshop-backend.err.log'
$frontendOut = Join-Path $frontend 'ecshop-frontend.out.log'
$frontendErr = Join-Path $frontend 'ecshop-frontend.err.log'

function Start-LoggedCommand($workingDirectory, $command, $outputFile) {
  $info = New-Object System.Diagnostics.ProcessStartInfo
  $info.FileName = 'cmd.exe'
  $info.Arguments = '/d /c "' + $command + ' > "' + $outputFile + '" 2>&1"'
  $info.WorkingDirectory = $workingDirectory
  $info.UseShellExecute = $false
  $info.CreateNoWindow = $true
  $process = New-Object System.Diagnostics.Process
  $process.StartInfo = $info
  [void]$process.Start()
}

if (Get-Command mvn.cmd -ErrorAction SilentlyContinue) {
  Start-LoggedCommand $backend 'mvn.cmd spring-boot:run' $backendOut
} else {
  Start-LoggedCommand $backend 'mvnw.cmd spring-boot:run' $backendOut
}
Start-LoggedCommand $frontend 'npm.cmd run dev -- --host 127.0.0.1' $frontendOut
Write-Host 'Service processes started.'
