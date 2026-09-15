$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$link = Join-Path $root 'ECShop-MVP.lnk'
$target = Join-Path $root 'start-ecshop.bat'

$shell = New-Object -ComObject WScript.Shell
$shortcut = $shell.CreateShortcut($link)
$shortcut.TargetPath = $target
$shortcut.WorkingDirectory = $root
$shortcut.Description = "EC Shop MVP"
$shortcut.Save()

Write-Host "Created: $link"
Write-Host "The shortcut points to this folder. Re-run create-shortcut.bat after copying the project."
