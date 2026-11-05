param([switch]$NoPause)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
. (Join-Path $PSScriptRoot 'java-tools.ps1')
$javaTools = Get-JavaTools
$buildRoot = Join-Path $PSScriptRoot 'build'
New-Item -ItemType Directory -Path $buildRoot -Force | Out-Null
$classes = Join-Path $buildRoot ('classes_' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $classes | Out-Null
$sourceRoot = Join-Path $PSScriptRoot 'src'
$sourceFiles = @(Get-ChildItem -LiteralPath $sourceRoot -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
# Explicit package directories also work in restricted Windows environments.
Get-ChildItem -LiteralPath $sourceRoot -Recurse -Directory | ForEach-Object {
    New-Item -ItemType Directory -Path (Join-Path $classes $_.FullName.Substring($sourceRoot.Length + 1)) -Force | Out-Null
}
$compilerVersion = (& $javaTools.Compiler -version 2>&1 | Out-String)
$targetArgs = @('--release','8')
if ($compilerVersion -match 'javac 1\.8\.') { $targetArgs = @('-source','8','-target','8') }
$compilerArgs = @('-encoding','UTF-8') + $targetArgs + @('-Xlint:all,-options','-d',$classes) + $sourceFiles
& $javaTools.Compiler @compilerArgs
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed. Existing bin was preserved.' }
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'resources\db.properties') -Destination $classes
$binaryRoot = Join-Path $PSScriptRoot 'bin'
$projectPrefix = (Resolve-Path -LiteralPath $PSScriptRoot).Path.TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
foreach ($directory in @($buildRoot, $classes, $binaryRoot)) {
    if (Test-Path -LiteralPath $directory) {
        $item = Get-Item -LiteralPath $directory -Force
        if (!$item.FullName.StartsWith($projectPrefix, [StringComparison]::OrdinalIgnoreCase) -or
            ($item.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
            throw 'Build directory must be inside the project and cannot be a link.'
        }
    }
}
if (Test-Path -LiteralPath $binaryRoot) {
    Move-Item -LiteralPath $binaryRoot -Destination (Join-Path $buildRoot ('bin_previous_' + [guid]::NewGuid().ToString('N')))
}
Move-Item -LiteralPath $classes -Destination $binaryRoot
Write-Output ('BUILD PASS: ' + $sourceFiles.Count + ' Java files, Java 8 bytecode, strict compiler checks.')
