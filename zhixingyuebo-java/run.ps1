param([switch]$NoPause)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if (-not (Test-Path -LiteralPath (Join-Path $PSScriptRoot 'bin\com\zhixingyuebo\view\MainApp.class'))) { & (Join-Path $PSScriptRoot 'build.ps1') -NoPause }
. (Join-Path $PSScriptRoot 'java-tools.ps1')
$javaTools = Get-JavaTools
$runArgs = @('-Dfile.encoding=UTF-8','-Duser.timezone=Asia/Shanghai','-cp',((Join-Path $PSScriptRoot 'bin') + ';' + (Join-Path $PSScriptRoot 'lib\mysql-connector-j-8.1.0.jar')),'com.zhixingyuebo.view.MainApp')
& $javaTools.Runtime @runArgs
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
