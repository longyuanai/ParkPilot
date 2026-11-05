param([switch]$Evidence,[switch]$ExportEvidence,[switch]$ForceFailure,[switch]$NoPause)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
. (Join-Path $PSScriptRoot 'verification-tools.ps1')
$fingerprint = Get-ProjectFingerprint $PSScriptRoot
& (Join-Path $PSScriptRoot 'build.ps1') -NoPause
. (Join-Path $PSScriptRoot 'java-tools.ps1')
$javaTools = Get-JavaTools
$classPath = (Join-Path $PSScriptRoot 'bin') + ';' + (Join-Path $PSScriptRoot 'lib\mysql-connector-j-8.1.0.jar')
$runArgs = @('-Dfile.encoding=UTF-8','-Duser.timezone=Asia/Shanghai','-Dzhixing.monthlyCard.price=100.00')
if ($ForceFailure) { $runArgs += @('-Dzhixing.forceFailure=true','-Dzhixing.acceptance.dir=build/negative-acceptance') }
$runArgs += @('-cp',$classPath,'com.zhixingyuebo.test.TestDatabaseRunner')
if ($Evidence) { $runArgs += '--evidence' }
if ($ExportEvidence) { $runArgs += @('--export-evidence') }
& $javaTools.Runtime @runArgs
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if ((Get-ProjectFingerprint $PSScriptRoot) -ne $fingerprint) {
    throw 'Project files changed during verification. Run verification again.'
}
if (-not $ForceFailure) {
    [IO.File]::WriteAllText((Join-Path $PSScriptRoot 'build\acceptance\source.sha256'), $fingerprint, [Text.Encoding]::ASCII)
}
